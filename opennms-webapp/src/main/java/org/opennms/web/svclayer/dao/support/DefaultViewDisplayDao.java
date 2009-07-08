/*******************************************************************************
 * This file is part of the OpenNMS(R) Application.
 *
 * Copyright (C) 2006-2008 The OpenNMS Group, Inc.  All rights reserved.
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

package org.opennms.web.svclayer.dao.support;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.opennms.netmgt.config.ViewsDisplayFactory;
import org.opennms.netmgt.config.viewsdisplay.View;
import org.opennms.web.svclayer.dao.ViewDisplayDao;
import org.springframework.dao.DataRetrievalFailureException;

/**
 * 
 * @author <a href="mailto:jason.aras@opennms.org">Jason Aras</a>
 * @author <a href="mailto:brozow@opennms.org">Mathew Brozowski</a>
 * @author <a href="mailto:david@opennms.org">David Hustace</a>
 */
public class DefaultViewDisplayDao implements ViewDisplayDao {
	
	public DefaultViewDisplayDao() {
		try {
			ViewsDisplayFactory.init();
		} catch (MarshalException e) {
			throw new DataRetrievalFailureException("Syntax error in viewsDisplay file", e);
		} catch (ValidationException e) {
			throw new DataRetrievalFailureException("Syntax error in viewsDisplay file", e);
		} catch (FileNotFoundException e) {
			throw new DataRetrievalFailureException("Unable to locate viewsDisplaly file", e);
		} catch (IOException e) {
			throw new DataRetrievalFailureException("Error load viewsDisplay file", e);
		}
	}
	
	public View getView() {
		try {
			return ViewsDisplayFactory.getInstance().getView("WebConsoleView");
		} catch (MarshalException e) {
			throw new DataRetrievalFailureException("Syntax error in viewsDisplay file", e);
		} catch (ValidationException e) {
			throw new DataRetrievalFailureException("Syntax error in viewsDisplay file", e);
		} catch (FileNotFoundException e) {
			throw new DataRetrievalFailureException("Unable to locate viewsDisplaly file", e);
		} catch (IOException e) {
			throw new DataRetrievalFailureException("Error load viewsDisplay file", e);
		}
		
	}

}
