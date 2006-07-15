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
// Modifications:
//
// 2003 Jan 31: Cleaned up some unused imports.
// 2003 Jan 08: Added code to associate the IP address in traps with nodes
//              and added the option to discover nodes based on traps.
//
// Original code base Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
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

package org.opennms.netmgt.trapd;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.sql.SQLException;

import org.apache.log4j.Category;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.opennms.core.fiber.PausableFiber;
import org.opennms.core.utils.ThreadCategory;
import org.opennms.netmgt.config.TrapdConfigFactory;
import org.opennms.netmgt.daemon.ServiceDaemon;
import org.opennms.netmgt.dao.EventDao;
import org.opennms.netmgt.eventd.EventIpcManager;
import org.opennms.netmgt.eventd.EventIpcManagerFactory;

/**
 * <p>
 * The Trapd listens for SNMP traps on the standard port(162). Creates a
 * SnmpTrapSession and implements the SnmpTrapHandler to get callbacks when
 * traps are received
 * </p>
 * 
 * <p>
 * The received traps are converted into XML and sent to eventd
 * </p>
 * 
 * <p>
 * <strong>Note: </strong>Trapd is a PausableFiber so as to receive control
 * events. However, a 'pause' on Trapd has no impact on the receiving and
 * processing of traps
 * </p>
 * 
 * @author <A HREF="mailto:weave@oculan.com">Brian Weaver </A>
 * @author <A HREF="mailto:sowmya@opennms.org">Sowmya Nataraj </A>
 * @author <A HREF="mailto:larry@opennms.org">Lawrence Karnowski </A>
 * @author <A HREF="mailto:mike@opennms.org">Mike Davidson </A>
 * @author <A HREF="mailto:tarus@opennms.org">Tarus Balog </A>
 * @author <A HREF="http://www.opennms.org">OpenNMS.org </A>
 * 
 */
public class Trapd extends ServiceDaemon {
	/**
	 * The name of the logging category for Trapd.
	 */
	private static final String LOG4J_CATEGORY = "OpenNMS.Trapd";

	/**
	 * The singlton instance.
	 */
	private static final Trapd m_singleton = new Trapd();

	private String m_name;
    
    private EventDao m_eventDao;

	/**
	 * <P>
	 * Constructs a new Trapd object that receives and forwards trap messages
	 * via JSDT. The session is initialized with the default client name of <EM>
	 * OpenNMS.trapd</EM>. The trap session is started on the default port, as
	 * defined by the SNMP libarary.
	 * </P>
	 * 
	 * @see org.opennms.protocols.snmp.SnmpTrapSession
	 */
	public Trapd() {
		m_name = LOG4J_CATEGORY;
	}

	public synchronized void init() {
		ThreadCategory.setPrefix(LOG4J_CATEGORY);

		Category log = ThreadCategory.getInstance();

		try {
			log.debug("start: Initializing the trapd config factory");
			TrapdConfigFactory.init();
		} catch (MarshalException e) {
			log.error("Failed to load configuration", e);
			throw new UndeclaredThrowableException(e);
		} catch (ValidationException e) {
			log.error("Failed to load configuration", e);
			throw new UndeclaredThrowableException(e);
		} catch (IOException e) {
			log.error("Failed to load configuration", e);
			throw new UndeclaredThrowableException(e);
		}

		try {
			// clear out the known nodes
			TrapdIPMgr.dataSourceSync();
		} catch (SQLException e) {
			log.error("Failed to load known IP address list", e);
			throw new UndeclaredThrowableException(e);
		}

		TrapHandler trapHandler = getTrapHandler();
		trapHandler.setTrapdConfig(TrapdConfigFactory.getInstance());

		EventIpcManagerFactory.init();
		EventIpcManager mgr = EventIpcManagerFactory.getIpcManager();
		trapHandler.setEventManager(mgr);

		trapHandler.init();
	}

	/**
	 * Create the SNMP trap session and create the JSDT communication channel to
	 * communicate with eventd.
	 * 
	 * @exception java.lang.reflect.UndeclaredThrowableException
	 *                if an unexpected database, or IO exception occurs.
	 * 
	 * @see org.opennms.protocols.snmp.SnmpTrapSession
	 * @see org.opennms.protocols.snmp.SnmpTrapHandler
	 */
	public synchronized void start() {
		// Set the category prefix
		ThreadCategory.setPrefix(LOG4J_CATEGORY);

		getTrapHandler().start();
		
	}

	/**
	 * Pauses Trapd
	 */
	public void pause() {
		if (!isRunning()) {
			return;
		}
		// Set the category prefix
		ThreadCategory.setPrefix(LOG4J_CATEGORY);

		setStatus(PAUSE_PENDING);

		getTrapHandler().pause();
		
		setStatus(PAUSED);
	}

	/**
	 * Resumes Trapd
	 */
	public void resume() {
		// Set the category prefix
		ThreadCategory.setPrefix(LOG4J_CATEGORY);

		getTrapHandler().resume();
	}

	/**
	 * Stops the currently running service. If the service is not running then
	 * the command is silently discarded.
	 */
	public synchronized void stop() {
		// Set the category prefix
		ThreadCategory.setPrefix(LOG4J_CATEGORY);

		getTrapHandler().stop();
	}

	/**
	 * Returns the current status of the service.
	 * 
	 * @return The service's status.
	 */
	public synchronized int getStatus() {
		return getTrapHandler().getStatus();
	}

	/**
	 * Returns the name of the service.
	 * 
	 * @return The service's name.
	 */
	public String getName() {
		return m_name;
	}

	/**
	 * Returns the singular instance of the trapd daemon. There can be only one
	 * instance of this service per virtual machine.
	 */
	public static Trapd getInstance() {
		return m_singleton;
	}

	private TrapHandler getTrapHandler() {
		// Set the category prefix
		ThreadCategory.setPrefix(LOG4J_CATEGORY);

		return TrapHandler.getInstance();
	}

    public EventDao getEventDao() {
        return m_eventDao;
    }

    public void setEventDao(EventDao eventDao) {
        m_eventDao = eventDao;
    }
}