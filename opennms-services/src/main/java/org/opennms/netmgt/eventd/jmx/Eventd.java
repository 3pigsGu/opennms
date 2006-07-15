//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2002-2003 The OpenNMS Group, Inc.  All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified 
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.                                                            
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//       
// For more information contact: 
//      OpenNMS Licensing       <license@opennms.org>
//      http://www.opennms.org/
//      http://www.opennms.com/
//
// Tab Size = 8
//

package org.opennms.netmgt.eventd.jmx;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.sql.SQLException;

import org.apache.log4j.Category;
import org.apache.log4j.Priority;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.opennms.core.utils.ThreadCategory;
import org.opennms.netmgt.ConfigFileConstants;
import org.opennms.netmgt.config.DataSourceFactory;
import org.opennms.netmgt.config.EventdConfigFactory;
import org.opennms.netmgt.eventd.EventConfigurationManager;
import org.opennms.netmgt.eventd.EventIpcManager;
import org.opennms.netmgt.eventd.EventIpcManagerDefaultImpl;
import org.opennms.netmgt.eventd.EventIpcManagerFactory;


public class Eventd implements EventdMBean {
    /**
     * The log4j category used to log debug messsages and statements.
     */
    public static final String LOG4J_CATEGORY = "OpenNMS.Eventd";

    public void init() {
        ThreadCategory.setPrefix(LOG4J_CATEGORY);
        Category log = ThreadCategory.getInstance();

        org.opennms.netmgt.eventd.Eventd e = org.opennms.netmgt.eventd.Eventd.getInstance();

        try {
            EventdConfigFactory.reload();
            DataSourceFactory.init();
        } catch (FileNotFoundException ex) {
            log.error("Failed to load eventd configuration. File Not Found:", ex);
            throw new UndeclaredThrowableException(ex);
        } catch (MarshalException ex) {
            log.error("Failed to load eventd configuration", ex);
            throw new UndeclaredThrowableException(ex);
        } catch (ValidationException ex) {
            log.error("Failed to load eventd configuration", ex);
            throw new UndeclaredThrowableException(ex);
        } catch (IOException ex) {
            log.error("Failed to load eventd configuration", ex);
            throw new UndeclaredThrowableException(ex);
        } catch (ClassNotFoundException ex) {
            log.error("Failed to init database connection factory", ex);
            throw new UndeclaredThrowableException(ex);
        } catch (SQLException ex) {
            log.error("Failed to init database connection factory", ex);
            throw new UndeclaredThrowableException(ex);
        } catch (PropertyVetoException ex) {
            log.error("Failed to init database connection factory", ex);
            throw new UndeclaredThrowableException(ex);
        }

        // load configuration(eventconf)
        //
        try {
            File configFile = ConfigFileConstants.getFile(ConfigFileConstants.EVENT_CONF_FILE_NAME);
            EventConfigurationManager.loadConfiguration(configFile.getPath());
        } catch (MarshalException ex) {
            log.error("Failed to load eventd configuration", ex);
            throw new UndeclaredThrowableException(ex);
        } catch (ValidationException ex) {
            log.error("Failed to load eventd configuration", ex);
            throw new UndeclaredThrowableException(ex);
        } catch (IOException ex) {
            log.error("Failed to load events configuration", ex);
            throw new UndeclaredThrowableException(ex);
        }
        
        e.setConfigManager(EventdConfigFactory.getInstance());
        
        EventIpcManager ipcMgr = new EventIpcManagerDefaultImpl(EventdConfigFactory.getInstance());
        EventIpcManagerFactory.setIpcManager(ipcMgr);
        EventIpcManagerFactory.init();
        
        e.setDataSource(DataSourceFactory.getDataSource());
        e.setEventIpcManager(ipcMgr);
        e.init();
        
    }

    public void start() {
        org.opennms.netmgt.eventd.Eventd.getInstance().start();
    }

    public void stop() {
        org.opennms.netmgt.eventd.Eventd.getInstance().stop();
    }

    public int getStatus() {
        return org.opennms.netmgt.eventd.Eventd.getInstance().getStatus();
    }

    public String status() {
        return org.opennms.core.fiber.Fiber.STATUS_NAMES[getStatus()];
    }

    public String getStatusText() {
        return org.opennms.core.fiber.Fiber.STATUS_NAMES[getStatus()];
    }
}
