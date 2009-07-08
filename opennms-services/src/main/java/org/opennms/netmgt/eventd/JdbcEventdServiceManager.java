/*******************************************************************************
 * This file is part of the OpenNMS(R) Application.
 *
 * Copyright (C) 2008 The OpenNMS Group, Inc.  All rights reserved.
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

package org.opennms.netmgt.eventd;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.log4j.Category;
import org.opennms.core.utils.ThreadCategory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.util.Assert;

public class JdbcEventdServiceManager implements InitializingBean, EventdServiceManager {
    private DataSource m_dataSource;

    /**
     * Cache of service names to service IDs.
     */
    private Map<String, Integer> m_serviceMap = new HashMap<String, Integer>();

    public JdbcEventdServiceManager() {
    }

    /* (non-Javadoc)
     * @see org.opennms.netmgt.eventd.EventdServiceManager#getServiceId(java.lang.String)
     */
    public synchronized int getServiceId(String serviceName) throws DataAccessException {
        Assert.notNull(serviceName, "The serviceName argument must not be null");

        if (m_serviceMap.containsKey(serviceName)) {
            return m_serviceMap.get(serviceName).intValue();
        } else {
            log().debug("Could not find entry for '" + serviceName + "' in service name cache.  Looking up in database.");
            
            int serviceId;
            try {
                serviceId = new JdbcTemplate(m_dataSource).queryForInt("SELECT serviceID FROM service WHERE serviceName = ?", new Object[] { serviceName });
            } catch (IncorrectResultSizeDataAccessException e) {
                if (e.getActualSize() == 0) {
                    log().debug("Did not find entry for '" + serviceName + "' in database.");
                    return -1; // not found
                } else {
                    throw e; // more than one found... WTF?!?!
                }
            }
            
            m_serviceMap.put(serviceName, new Integer(serviceId));
            
            log().debug("Found entry for '" + serviceName + "' (ID " + serviceId + ") in database.  Adding to service name cache.");
            
            return serviceId;
        }
    }

    /* (non-Javadoc)
     * @see org.opennms.netmgt.eventd.EventdServiceManager#dataSourceSync()
     */
    public synchronized void dataSourceSync() {
        m_serviceMap.clear();
        
        new JdbcTemplate(m_dataSource).query(EventdConstants.SQL_DB_SVC_TABLE_READ, new RowCallbackHandler() {
            public void processRow(ResultSet resultSet) throws SQLException {
                m_serviceMap.put(resultSet.getString(2), new Integer(resultSet.getInt(1)));
            }
        });
    }

    private Category log() {
        return ThreadCategory.getInstance(getClass());
    }

    public void afterPropertiesSet() throws Exception {
        Assert.state(m_dataSource != null, "property dataSource must be set");
    }

    public DataSource getDataSource() {
        return m_dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        m_dataSource = dataSource;
    }
}
