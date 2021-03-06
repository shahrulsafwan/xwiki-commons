/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.velocity.introspection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.util.RuntimeServicesAware;
import org.apache.velocity.util.introspection.Info;
import org.apache.velocity.util.introspection.VelMethod;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.properties.ConverterManager;

/**
 * Chainable Velocity Uberspector that tries to convert method arguments to formal parameter types when the passed
 * arguments don't match the method signature. In other words, it looks for a method matching the passed arguments and
 * if none is found then it tries the convert the arguments to match the available method signatures (the available
 * methods with the same name and the same number of parameters but with different parameter types). E.g.:
 *
 * <pre>
 * {@code $obj.someMethod('VALUE')
 * // will forward to
 * obj.someMethod(SomeEnum.VALUE)
 * // if obj has someMethod(SomeEnum) and not someMethod(String)}
 * </pre>
 *
 * @since 4.1M2
 * @version $Id$
 * @see ChainableUberspector
 */
public class MethodArgumentsUberspector extends AbstractChainableUberspector implements RuntimeServicesAware
{
    /**
     * The component used to convert method arguments to formal parameter types.
     */
    private ConverterManager converterManager;

    @Override
    public void setRuntimeServices(RuntimeServices runtimeServices)
    {
        ComponentManager componentManager =
            (ComponentManager) runtimeServices.getApplicationAttribute(ComponentManager.class.getName());
        try {
            this.converterManager = componentManager.getInstance(ConverterManager.class);
        } catch (ComponentLookupException e) {
            this.log.warn("Failed to initialize " + this.getClass().getSimpleName(), e);
        }
    }

    @Override
    public VelMethod getMethod(Object obj, String methodName, Object[] args, Info i) throws Exception
    {
        // Let Velocity find a matching method. However, Velocity finds the closest matching method.
        // According to the JavaDoc of MethodMap:
        // "Attempts to find the most specific applicable method using the algorithm described in the JLS section
        // 15.12.2 (with the exception that it can't distinguish a primitive type argument from an object type
        // argument, since in reflection primitive type arguments are represented by their object counterparts, so for
        // an argument of type (say) java.lang.Integer, it will not be able to decide between a method that takes int
        // and a method that takes java.lang.Integer as a parameter."
        // Thus we need to apply the following logic:
        // - if the returned VelMethod has a different number of parameters than the signature asked for, then go into
        //   our conversion code
        // - if our conversion code doesn't find any match, then return the VelMethod found by Velocity.

        VelMethod initialVelMethod = super.getMethod(obj, methodName, args, i);
        VelMethod velMethod = initialVelMethod;

        boolean shouldConvert = false;
        if (this.converterManager != null) {
            if (velMethod == null) {
                shouldConvert = true;
            } else {
                Method method = getPrivateMethod(velMethod);
                boolean sameParameterNumbers = method.getParameterTypes().length == args.length;
                if (!sameParameterNumbers) {
                    shouldConvert = true;
                }
            }
        }

        if (shouldConvert) {
            // Try to convert method arguments to formal parameter types.
            Object[] convertedArguments = this.convertArguments(obj, methodName, args);
            if (convertedArguments != null) {
                velMethod = super.getMethod(obj, methodName, convertedArguments, i);
                if (velMethod != null) {
                    velMethod = new ConvertingVelMethod(velMethod);
                } else {
                    velMethod = initialVelMethod;
                }
            }
        }

        return velMethod;
    }

    /**
     * This is hackish but there's no way in Velocity to get access to the underlying Method from a VelMethod instance.
     */
    private Method getPrivateMethod(VelMethod velMethod) throws Exception
    {
        Field methodField = velMethod.getClass().getDeclaredField("method");
        boolean isAccessible = methodField.isAccessible();
        try {
            methodField.setAccessible(true);
            return (Method) methodField.get(velMethod);
        } finally {
            methodField.setAccessible(isAccessible);
        }
    }

    /**
     * Converts the given arguments to match a method with the specified name and the same number of formal parameters
     * as the number of arguments.
     *
     * @param obj the object the method is invoked on, used to retrieve the list of available methods
     * @param methodName the method we're looking for
     * @param args the method arguments
     * @return a new array of arguments where some values have been converted to match the formal method parameter
     *         types, {@code null} if no such method is found
     */
    private Object[] convertArguments(Object obj, String methodName, Object[] args)
    {
        for (Method method : obj.getClass().getMethods()) {
            if (method.getName().equalsIgnoreCase(methodName) && method.getParameterTypes().length == args.length) {
                try {
                    return convertArguments(args, method.getParameterTypes());
                } catch (Exception e) {
                    // Ignore and try the next method.
                }
            }
        }
        return null;
    }

    /**
     * Tries to convert the given arguments to match the specified formal parameters types.
     * <p>
     * Throws a runtime exception if the conversion fails.
     *
     * @param arguments the method actual arguments
     * @param parameterTypes the method formal parameter types
     * @return a new array of arguments where some values have been converted to match the formal method parameter types
     */
    private Object[] convertArguments(Object[] arguments, Class<?>[] parameterTypes)
    {
        Object[] convertedArguments = Arrays.copyOf(arguments, arguments.length);
        for (int i = 0; i < parameterTypes.length; i++) {
            // Try to convert the argument if it's not null and if it doesn't match the parameter type.
            if (arguments[i] != null && !parameterTypes[i].isInstance(arguments[i])) {
                convertedArguments[i] = this.converterManager.convert(parameterTypes[i], arguments[i]);
            }
        }
        return convertedArguments;
    }

    /**
     * Wrapper for a real VelMethod that converts the passed arguments to the real arguments expected by the method.
     *
     * @version $Id$
     */
    private class ConvertingVelMethod implements VelMethod
    {
        /** The real method that performs the actual call. */
        private VelMethod innerMethod;

        /**
         * Constructor.
         *
         * @param realMethod the real method to wrap
         */
        public ConvertingVelMethod(VelMethod realMethod)
        {
            this.innerMethod = realMethod;
        }

        @Override
        public Object invoke(Object o, Object[] params) throws Exception
        {
            return this.innerMethod.invoke(o, convertArguments(o, this.innerMethod.getMethodName(), params));
        }

        @Override
        public boolean isCacheable()
        {
            return this.innerMethod.isCacheable();
        }

        @Override
        public String getMethodName()
        {
            return this.innerMethod.getMethodName();
        }

        @Override
        public Class<?> getReturnType()
        {
            return this.innerMethod.getReturnType();
        }
    }
}
