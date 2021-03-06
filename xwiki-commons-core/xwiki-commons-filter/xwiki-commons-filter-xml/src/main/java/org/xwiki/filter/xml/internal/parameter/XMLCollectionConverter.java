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
package org.xwiki.filter.xml.internal.parameter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.mapper.Mapper;

/**
 * Customize {@link CollectionConverter}.
 *
 * @version $Id$
 * @since 5.2M1
 */
public class XMLCollectionConverter extends CollectionConverter
{
    /**
     * @param mapper the mapper
     */
    public XMLCollectionConverter(Mapper mapper)
    {
        super(mapper);
    }

    @Override
    public boolean canConvert(Class type)
    {
        return type.equals(Collection.class) || type.equals(List.class);
    }

    @Override
    protected Object createCollection(Class type)
    {
        return new ArrayList<Object>();
    }
}
