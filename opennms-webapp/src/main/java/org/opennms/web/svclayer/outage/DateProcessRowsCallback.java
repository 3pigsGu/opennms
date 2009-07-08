/*******************************************************************************
 * This file is part of the OpenNMS(R) Application.
 *
 * Copyright (C) 2006-2009 The OpenNMS Group, Inc.  All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc.:
 *
 *      51 Franklin Street
 *      5th Floor
 *      Boston, MA 02110-1301
 *      USA
 *
 * For more information contact:
 *
 *      OpenNMS Licensing <license@opennms.org>
 *      http://www.opennms.org/
 *      http://www.opennms.com/
 *
 *******************************************************************************/

package org.opennms.web.svclayer.outage;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.extremecomponents.table.callback.ProcessRowsCallback;
import org.extremecomponents.table.core.TableModel;
import java.util.ArrayList;
import java.util.Collection;

/**
 * 
 * Overwrite Default ProcessRowsCallback to enable date filtering.
 * Example code from Nathan MA
 * 
 * @author <a href="mailto:joed@opennms.org">Johan Edstrom</a>
 */
public class DateProcessRowsCallback extends ProcessRowsCallback
{
	/** {@inheritDoc} **/
    @SuppressWarnings("unchecked")
    public Collection filterRows(TableModel model, Collection rows)
        throws Exception
    {
        boolean filtered = model.getLimit().isFiltered();
        boolean cleared = model.getLimit().isCleared();

        if (!filtered || cleared)
        {
             return rows;
        }

        if (filtered)
        {
            Collection collection = new ArrayList();
            Predicate filterPredicate = new DateFilterPredicate(model);
            CollectionUtils.select(rows, filterPredicate, collection);

            return collection;
        }

        return rows;
    }
}