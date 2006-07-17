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
// 2002 Nov 14: Used non-blocking socket class to speed up capsd and pollerd.
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

package org.opennms.netmgt.eventd;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.log4j.Category;
import org.opennms.core.utils.ThreadCategory;
import org.opennms.netmgt.config.EventdConfigManager;
import org.opennms.netmgt.daemon.AbstractServiceDaemon;
import org.opennms.netmgt.dao.EventDao;
import org.opennms.netmgt.eventd.adaptors.EventReceiver;
import org.opennms.netmgt.eventd.adaptors.tcp.TcpEventReceiver;
import org.opennms.netmgt.eventd.adaptors.udp.UdpEventReceiver;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.netmgt.xml.event.EventReceipt;

/**
 * <p>
 * Eventd listens for events from the discovery, capsd, trapd processes and
 * sends events to the Master Station when queried for.
 * </p>
 * 
 * <p>
 * Eventd receives events sent in as XML, looks up the event.conf and adds
 * information to these events and stores them to the db. It also reconverts
 * them back to XML to be sent to other processes like 'actiond'
 * </p>
 * 
 * <p>
 * Process like trapd, capsd etc. that are local to the distributed poller send
 * events to the eventd. Events can also be sent via TCP or UDP to eventd.
 * </p>
 * 
 * <p>
 * Eventd listens for incoming events, loads info from the 'event.conf', adds
 * events to the database and sends the events added to the database to
 * subscribed listeners. It also maintains a servicename to serviceid mapping
 * from the services table so as to prevent a database lookup for each incoming
 * event
 * </P>
 * 
 * <P>
 * The number of threads that processes events is configurable via the eventd
 * configuration xml
 * </P>
 * 
 * @author <A HREF="mailto:sowmya@opennms.org">Sowmya Nataraj </A>
 * @author <A HREF="http://www.opennms.org">OpenNMS.org </A>
 */
public final class Eventd extends AbstractServiceDaemon implements org.opennms.netmgt.eventd.adaptors.EventHandler {
    private static EventIpcManager m_eventIpcManager;
    /**
     * The log4j category used to log debug messsages and statements.
     */
    public static final String LOG4J_CATEGORY = "OpenNMS.Eventd";

    /**
     * Singleton instance of this class
     */
    private static final Eventd m_singleton = new Eventd();

    /**
     * The service table map
     */
    private static Map m_serviceTableMap;

    /**
     * The handler for events coming in through TCP
     */
    private EventReceiver m_tcpReceiver;

    /**
     * The handler for events coming in through UDP
     */
    private EventReceiver m_udpReceiver;

    /**
     * Contains dotted-decimal representation of the IP address where Eventd is
     * running. Used when eventd broadcasts events.
     */
    private String m_address = null;

    private EventdConfigManager m_eFactory;
    private EventDao m_eventDao;
    
    private DataSource m_dataSource;

//    private DbConnectionFactory m_dbConnectionFactory;

    static {
        // map of service names to service identifer
        m_serviceTableMap = Collections.synchronizedMap(new HashMap());
    }

    /**
     * Constuctor creates the localhost address(to be used eventually when
     * eventd originates events during correlation) and the broadcast queue
     */
    public Eventd() {
    	super("OpenNMS.Eventd");
        try {
            m_address = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException uhE) {
            Category log = ThreadCategory.getInstance(getClass());

            m_address = "localhost";
            log.warn("Could not lookup the host name for the local host machine, address set to localhost", uhE);
        }

    }

    protected void onStop() {
		// Stop listener threads
        if (log().isDebugEnabled()) {
            log().debug("calling shutdown on tcp/udp listener threads");
        }

        m_tcpReceiver.stop();
        m_udpReceiver.stop();

        if (log().isDebugEnabled()) {
            log().debug("shutdown on tcp/udp listener threads returned");
        }
	}

    protected void onInit() {

    	if (m_dataSource == null) {
            throw new IllegalStateException("dataSource not initialized");
        }

        // Get a database connection and create the service table map
        java.sql.Connection tempConn = null;
        try {
            tempConn = m_dataSource.getConnection();

            // create the service table map
            PreparedStatement stmt = tempConn.prepareStatement(EventdConstants.SQL_DB_SVC_TABLE_READ);
            ResultSet rset = stmt.executeQuery();
            while (rset.next()) {
                int svcid = rset.getInt(1);
                String svcname = rset.getString(2);

                m_serviceTableMap.put(svcname, new Integer(svcid));
            }

            rset.close();
            stmt.close();
        } catch (SQLException sqlE) {
            throw new UndeclaredThrowableException(sqlE);
        } finally {
            try {
                if (tempConn != null) {
                    tempConn.close();
                }
            } catch (SQLException sqlE) {
                log().warn("An error occured closing the database connection, ignoring", sqlE);
            }
        }

        // Create all the threads

        m_tcpReceiver = null;
        m_udpReceiver = null;
        try {
            String timeoutReq = m_eFactory.getSocketSoTimeoutRequired();
            m_tcpReceiver = new TcpEventReceiver(m_eFactory.getTCPPort());
            m_udpReceiver = new UdpEventReceiver(m_eFactory.getUDPPort());

            m_tcpReceiver.addEventHandler(this);
            m_udpReceiver.addEventHandler(this);

        } catch (IOException e) {
            log().error("Error starting up the TCP/UDP threads of eventd", e);
            throw new UndeclaredThrowableException(e);
        }


    }

    protected void onStart() {
		m_tcpReceiver.start();
        m_udpReceiver.start();

        if (log().isDebugEnabled()) {
            log().debug("Listener threads started");
        }

        if (log().isDebugEnabled()) {
            log().debug("Eventd running");
        }
	}

	/**
     * Used to retrieve the local host address. The address of the machine on
     * which Eventd is running.
     * 
     * @return The local machines hostname.
     */
    public String getLocalHostAddress() {
        return m_address;
    }

    /**
     * Return the service id for the name passed
     * 
     * @param svcname
     *            the service name whose service id is required
     * 
     * @return the service id for the name passed, -1 if not found
     */
    public static synchronized int getServiceID(String svcname) {
        Integer i = (Integer) m_serviceTableMap.get(svcname);
        if (i != null) {
            return i.intValue();
        } else {
            return -1;
        }
    }

    /**
     * Add the svcname/svcid mapping to the servicetable map
     */
    public static synchronized void addServiceMapping(String svcname, int serviceid) {
        m_serviceTableMap.put(svcname, new Integer(serviceid));
    }

    public static Eventd getInstance() {
        return m_singleton;
    }

    public boolean processEvent(Event event) {
        m_eventIpcManager.sendNow(event);
        return true;
    }

    public void receiptSent(EventReceipt event) {
        // do nothing
    }
    
    public void setDataSource(DataSource dataSource) {
    	m_dataSource = dataSource;
    }

    public void setConfigManager(EventdConfigManager configManager) {
        
        m_eFactory = configManager;
        
    }
    
    public EventIpcManager getEventIpcManager() {
        return m_eventIpcManager;
    }
    
    public void setEventIpcManager(EventIpcManager manager) {
        m_eventIpcManager = manager;
    }
    
    public void setEventDao(EventDao dao) {
        m_eventDao = dao;
    }

    public EventDao getEventDao() {
        return m_eventDao;
    }
}
