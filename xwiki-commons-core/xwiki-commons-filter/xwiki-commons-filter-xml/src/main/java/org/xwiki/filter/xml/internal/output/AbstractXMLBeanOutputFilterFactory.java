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
package org.xwiki.filter.xml.internal.output;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Result;

import org.xwiki.filter.FilterException;
import org.xwiki.filter.internal.output.AbstractBeanOutputFilterStreamFactory;
import org.xwiki.filter.internal.output.BeanOutputFilter;
import org.xwiki.filter.type.FilterStreamType;
import org.xwiki.filter.xml.output.XMLOutputProperties;

/**
 * A generic xml output filter implementation. This class can be used as a test bench to validate various
 * XMLInputStream wiki parsers.
 * 
 * @version $Id$
 * @since 6.2M1
 */
public abstract class AbstractXMLBeanOutputFilterFactory<P extends XMLOutputProperties, F> extends
    AbstractBeanOutputFilterStreamFactory<P, F>
{
    public AbstractXMLBeanOutputFilterFactory(FilterStreamType type)
    {
        super(type);
    }

    @Override
    public BeanOutputFilter<P> createOutputFilter(P properties) throws FilterException
    {
        try {
            return new DefaultXMLOutputFilter<P, F>(this, properties);
        } catch (Exception e) {
            throw new FilterException("Failed to create output filter stream", e);
        }
    }

    protected abstract Object createListener(Result result, P parameters) throws XMLStreamException,
        FactoryConfigurationError, FilterException;
}
