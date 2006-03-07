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
// 2002 Oct 22: Added threshold rearm events.  
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

package org.opennms.netmgt.threshd;

import java.lang.reflect.UndeclaredThrowableException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Category;
import org.apache.log4j.Priority;
import org.opennms.core.utils.ThreadCategory;
import org.opennms.netmgt.config.DatabaseConnectionFactory;
import org.opennms.netmgt.config.ThresholdingConfigFactory;
import org.opennms.netmgt.config.threshd.Threshold;
import org.opennms.netmgt.poller.NetworkInterface;
import org.opennms.netmgt.rrd.RrdException;
import org.opennms.netmgt.rrd.RrdUtils;
import org.opennms.netmgt.utils.EventProxy;
import org.opennms.netmgt.utils.EventProxyException;
import org.opennms.netmgt.utils.ParameterMap;
import org.opennms.netmgt.xml.event.Events;
import org.opennms.netmgt.xml.event.Log;

/**
 * <P>
 * The LatencyThresholder class ...
 * </P>
 * 
 * @author <A HREF="mailto:mike@opennms.org">Mike Davidson </A>
 * @author <A HREF="http://www.opennms.org/">OpenNMS </A>
 * 
 * FIXME: This thresholder does not support ranges yet.
 * 
 */
final class LatencyThresholder implements ServiceThresholder {
    /**
     * SQL statement to retrieve interface's 'ipinterface' table information.
     */
    private static final String SQL_GET_NODEID = "SELECT nodeid FROM ipinterface WHERE ipAddr=? AND ismanaged!='D'";

    /**
     * Default thresholding interval (in milliseconds).
     * 
     */
    static final int DEFAULT_INTERVAL = 300000; // 300s or 5m

    /**
     * Default age before which a data point is considered "out of date"
     */
    
    static final int DEFAULT_RANGE = 0; 


    /**
     * Interface attribute key used to store the interface's node id
     */
    static final String RRD_REPOSITORY_KEY = "org.opennms.netmgt.collectd.LatencyThresholder.RrdRepository";

    /**
     * Interface attribute key used to store configured thresholds
     */
    static final String THRESHOLD_MAP_KEY = "org.opennms.netmgt.collectd.LatencyThresholder.ThresholdMap";

    /**
     * Interface attribute key used to store the interface's node id
     */
    static final String NODE_ID_KEY = "org.opennms.netmgt.collectd.SnmpThresholder.NodeId";

    /**
     * Specific service that this thresholder is responsible for latency
     * threshold checking.
     */
    private String m_svcName;

    /**
     * Local host name
     */
    private String m_host;

    /**
     * <P>
     * Returns the name of the service that the plug-in threshold checks.
     * </P>
     * 
     * @return The service that the plug-in collects.
     */
    public String serviceName() {
        return m_svcName;
    }

    /**
     * <P>
     * Initialize the service thresholder.
     * </P>
     * 
     * @param parameters
     *            Parameter map which contains (currently) a single entry, the
     *            name of the service which this thresholder is responsible for
     *            latency threshold checking keyed by the String "svcName"
     * 
     * @exception RuntimeException
     *                Thrown if an unrecoverable error occurs that prevents the
     *                plug-in from functioning.
     * 
     */
    public void initialize(Map parameters) {
        // Log4j category
        //
        Category log = log();

        // Service name
        //
        m_svcName = (String) parameters.get("svcName");
        if (log.isDebugEnabled())
            log.debug("initialize: latency thresholder for service '" + m_svcName + "'");

        // Get local host name (used when generating threshold events)
        //
        try {
            m_host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            if (log.isEnabledFor(Priority.WARN))
                log.warn("initialize: Unable to resolve local host name.", e);
            m_host = "unresolved.host";
        }

        try {
            RrdUtils.initialize();
        } catch (RrdException e) {
            if (log.isEnabledFor(Priority.ERROR))
                log.error("initialize: Unable to initialize RrdUtils", e);
            throw new RuntimeException("Unable to initialize RrdUtils", e);
        }

        if (log.isDebugEnabled())
            log.debug("initialize: successfully instantiated JNI interface to RRD...");

        return;
    }

    /**
     * Responsible for freeing up any resources held by the thresholder.
     */
    public void release() {
        // Nothing to release...
    }

    /**
     * Responsible for performing all necessary initialization for the specified
     * interface in preparation for thresholding.
     * 
     * @param iface
     *            Network interface to be prepped for thresholding.
     * @param parameters
     *            Key/value pairs associated with the package to which the
     *            interface belongs..
     * 
     */
    public void initialize(NetworkInterface iface, Map parameters) {
        Category log = log();

        // Get interface address from NetworkInterface
        //
        if (iface.getType() != NetworkInterface.TYPE_IPV4)
            throw new RuntimeException("Unsupported interface type, only TYPE_IPV4 currently supported");
        InetAddress ipAddr = (InetAddress) iface.getAddress();
        String groupName = ParameterMap.getKeyedString(parameters, "thresholding-group", "default");

        // Get the threshold group's RRD repository path
        // 
        String repository = null;
        try {
            repository = ThresholdingConfigFactory.getInstance().getRrdRepository(groupName);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Thresholding group '" + groupName + "' does not exist.");
        }

        // Add RRD repository as an attribute of the interface for retrieval
        // by the check() method.
        //
        iface.setAttribute(RRD_REPOSITORY_KEY, repository);

        // Get database connection in order to retrieve the nodeid and
        // ifIndex from the database for this interface.
        //
        java.sql.Connection dbConn = null;
        try {
            dbConn = DatabaseConnectionFactory.getInstance().getConnection();
        } catch (SQLException sqlE) {
            if (log.isEnabledFor(Priority.ERROR))
                log.error("initialize: Failed getting connection to the database.", sqlE);
            throw new UndeclaredThrowableException(sqlE);
        }

        // Use IP address to lookup the node id
        //
        // NOTE: All database calls wrapped in try/finally block so we make
        // certain that the connection will be closed when we are
        // finished.
        //
        int nodeId = -1;

        try {
            // Prepare & execute the SQL statement to get the 'nodeid',
            // 'ifIndex' and 'isSnmpPrimary' fields from the ipInterface table.
            //
            PreparedStatement stmt = null;
            try {
                stmt = dbConn.prepareStatement(SQL_GET_NODEID);
                stmt.setString(1, ipAddr.getHostAddress()); // interface address
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    nodeId = rs.getInt(1);
                    if (rs.wasNull())
                        nodeId = -1;
                }
                rs.close();
            } catch (SQLException sqle) {
                if (log.isDebugEnabled())
                    log.debug("initialize: SQL exception!!", sqle);
                throw new RuntimeException("SQL exception while attempting to retrieve node id for interface " + ipAddr.getHostAddress());
            } finally {
                try {
                    stmt.close();
                } catch (Exception e) {
                    // Ignore
                }
            }

            if (log.isDebugEnabled())
                log.debug("initialize: db retrieval info: nodeid = " + nodeId + ", address = " + ipAddr.getHostAddress());

            if (nodeId == -1)
                throw new RuntimeException("Unable to retrieve node id for interface " + ipAddr.getHostAddress());
        } finally {
            // Done with the database so close the connection
            try {
                dbConn.close();
            } catch (SQLException sqle) {
                if (log.isEnabledFor(Priority.INFO))
                    log.info("initialize: SQLException while closing database connection", sqle);
            }
        }

        // Add nodeId as an attribute of the interface for retrieval
        // by the check() method.
        //
        iface.setAttribute(NODE_ID_KEY, new Integer(nodeId));

        // Retrieve the collection of Threshold objects associated with
        // the defined thresholding group and build maps of
        // ThresholdEntity objects keyed by datasource name. The
        // datasource type of the threshold determines which
        // map the threshold entity is added to.
        //
        // Each ThresholdEntity can wrap one high Threshold and one low
        // Threshold castor-generated object for a single datasource.
        // If more than one high or more than one low threshold is defined
        // for a single datasource a warning messages is generated. Only
        // the first threshold in such a scenario will be used for thresholding.
        //

        // Create empty map for storing threshold entities
        Map thresholdMap = new HashMap();

        try {
            Iterator iter = ThresholdingConfigFactory.getInstance().getThresholds(groupName).iterator();
            while (iter.hasNext()) {
                Threshold thresh = (Threshold) iter.next();

                // See if map entry already exists for this datasource
                // If not, create a new one.
                boolean newEntity = false;
                ThresholdEntity thresholdEntity = null;

                // All latency thresholds are per interface so confirm that
                // the datasource type is set to "if"
                //
                if (!thresh.getDsType().equals("if") && !thresh.getDsType().equals("expr")) {
                    log.warn("initialize: invalid datasource type, latency thresholder only supports interface level datasources.");
                    continue; // continue with the next threshold...
                }

                // First attempt to lookup the entry in the map
                thresholdEntity = (ThresholdEntity) thresholdMap.get(thresh.getDsName());

                // Found entry?
                if (thresholdEntity == null) {
                    // Nope, create a new one
                    newEntity = true;
                    thresholdEntity = new ThresholdEntity();
                }

                try {
                    // Set high/low threshold
                    if (thresh.getType().equals(ThresholdEntity.HIGH_THRESHOLD))
                        thresholdEntity.setHighThreshold(thresh);
                    else if (thresh.getType().equals(ThresholdEntity.LOW_THRESHOLD))
                        thresholdEntity.setLowThreshold(thresh);
                } catch (IllegalStateException e) {
                    log.warn("Encountered duplicate " + thresh.getType() + " for datasource " + thresh.getDsName(), e);
                }

                // Add new entity to the map
                if (newEntity) {
                    thresholdMap.put(thresh.getDsName(), thresholdEntity);
                }
            }
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Thresholding group '" + groupName + "' does not exist.");
        }

        // Add threshold maps as attributes for retrieval by the check() method.
        //
        iface.setAttribute(THRESHOLD_MAP_KEY, thresholdMap);

        // Debug
        //
        if (log.isDebugEnabled()) {
            log.debug("initialize: dumping interface thresholds defined for " + ipAddr.getHostAddress() + "/" + groupName + ":");
            Iterator iter = thresholdMap.values().iterator();
            while (iter.hasNext())
                log.debug((ThresholdEntity) iter.next());
        }

        if (log.isDebugEnabled())
            log.debug("initialize: initialization completed for " + ipAddr.getHostAddress());
        return;
    }

    /**
     * Responsible for releasing any resources associated with the specified
     * interface.
     * 
     * @param iface
     *            Network interface to be released.
     */
    public void release(NetworkInterface iface) {
        // Nothing to release...
    }

    /**
     * Perform threshold checking.
     * 
     * @param iface
     *            Network interface to be data collected.
     * @param eproxy
     *            Eventy proxy for sending events.
     * @param parameters
     *            Key/value pairs from the package to which the interface
     *            belongs.
     */
    public int check(NetworkInterface iface, EventProxy eproxy, Map parameters) {
		LatencyInterface latIface = new LatencyInterface(iface, m_svcName);
		LatencyParameters latParms = new LatencyParameters(parameters, m_svcName);
        
        try {
            
            // Get configuration parameters
            //
            // NodeId attribute
            if (log().isDebugEnabled())
                log().debug("check: service= " + m_svcName + " interface= " + latIface.getHostName() + " nodeId= " + latIface.getNodeId() + " thresholding-group=" + latParms.getGroupName() + " interval=" + latParms.getInterval() + "ms");
            
            // RRD Repository attribute
            //
            // Create empty Events object to hold any threshold
            // events generated during the thresholding check...
            Events events = checkRrdDir(latIface, latParms);
            
            // Send created events
            //
            sendEvents(eproxy, events);
            
            // return the status of the threshold check
            //
            return THRESHOLDING_SUCCEEDED;
            
        } catch(ThresholdingException e) {
            log().error(e.getMessage());
            return e.getFailureCode();
        } catch (EventProxyException e) {
            log().error("check: Failed sending threshold events via event proxy...", e);
            return THRESHOLDING_FAILED;
        }
    }

	private void sendEvents(EventProxy eproxy, Events events) throws EventProxyException {
        if (events != null && events.getEventCount() > 0) {
            Log eventLog = new Log();
            eventLog.setEvents(events);
            eproxy.send(eventLog);
        }
    }

    /**
     * Performs threshold checking on an directory which contains one or more
     * RRD files containing latency/response time information. ThresholdEntity
     * objects are stored for performing threshold checking.
     * @param latIface TODO
     * @param latParms TODO
     * @param parameters 
     * @param iface 
     * @param directory
     *            RRD repository directory
     * @param nodeId
     *            Node identifier of interface being checked
     * @param ipAddr
     *            IP address of the interface being checked
     * @param interval
     *            Configured thresholding interval
     * @param date
     *            Source for timestamp to be used for all generated events
     * @param thresholdMap
     *            Map of configured interface level ThresholdEntity objects
     *            keyed by datasource name.
     * @param events
     *            Castor events object containing any events to be generated as
     *            a result of threshold checking.
     * @throws IllegalArgumentException
     *             if path parameter is not a directory.
     * @throws ThresholdingException 
     */
    Events checkRrdDir(LatencyInterface latIface, LatencyParameters latParms) throws IllegalArgumentException, ThresholdingException {
		Map thresholdMap = latIface.getThresholdMap();

        // Sanity Check
        if (latIface.getInetAddress() == null || thresholdMap == null) {
            throw new ThresholdingException("check: Threshold checking failed for " + m_svcName + "/" + latIface.getHostName(), THRESHOLDING_FAILED);
        }
        
        Events events = new Events();
        Date date = new Date();

        for (Iterator it = thresholdMap.keySet().iterator(); it.hasNext();) {
            String datasource = (String) it.next();
            ThresholdEntity threshold = (ThresholdEntity) thresholdMap.get(datasource);
            if (threshold != null) {
                Double dsValue = threshold.fetchLastValue(latIface, latParms);

                threshold.evaluateThreshold(dsValue, events, date, latIface);
            }
        }

        return events;
    }

    public final Category log() {
        return ThreadCategory.getInstance(LatencyThresholder.class);
    }
}
