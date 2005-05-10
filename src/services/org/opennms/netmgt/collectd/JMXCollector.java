//
//This file is part of the OpenNMS(R) Application.
//
//OpenNMS(R) is Copyright (C) 2002-2003 The OpenNMS Group, Inc.  All rights reserved.
//OpenNMS(R) is a derivative work, containing both original code, included code and modified
//code that was published under the GNU General Public License. Copyrights for modified 
//and included code are below.
//
//OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
//This program is free software; you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation; either version 2 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.                                                            
//
//You should have received a copy of the GNU General Public License
//along with this program; if not, write to the Free Software
//Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//  
//For more information contact: 
// OpenNMS Licensing       <license@opennms.org>
// http://www.opennms.org/
// http://www.opennms.com/
//

package org.opennms.netmgt.collectd;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.*;
import java.util.Set;
import java.util.StringTokenizer;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.apache.log4j.Category;
import org.apache.log4j.Priority;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.opennms.core.utils.ThreadCategory;
import org.opennms.netmgt.EventConstants;
import org.opennms.netmgt.config.BeanInfo;
import org.opennms.netmgt.config.DatabaseConnectionFactory;
import org.opennms.netmgt.config.JMXDataCollectionConfigFactory;
import org.opennms.netmgt.config.collectd.Attrib;
import org.opennms.netmgt.poller.monitors.NetworkInterface;
import org.opennms.netmgt.rrd.RRDDataSource;
import org.opennms.netmgt.rrd.RrdException;
import org.opennms.netmgt.rrd.RrdUtils;
import org.opennms.netmgt.utils.EventProxy;
import org.opennms.netmgt.utils.ParameterMap;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.protocols.jmx.connectors.ConnectionWrapper;

/*
 * This class performs the collection and storage of data.  The derived class manages the connection 
 * and configuration.  The SNMPCollector class was used as the starting point for this class so anyone 
 * familiar with it should be able to easily understand it.
 * 
 * The jmx-datacollection-config.xml defines a list of MBeans and attributes that may be monitored.  
 * This class retrieves the list of MBeans for the specified service name (currently jboss and jsr160) 
 * and queries the remote server for the attributes.  The values are then stored in RRD files.  
 * 
 * Two types of MBeans may be specified in the jmx-datacollection-config.xml file.  Standard MBeans which 
 * consist of and ObjectName and their attributes, and WildCard MBeans which performs a query to retieve 
 * MBeans based on a criteria.  The current implementation looks like:  jboss:a=b,c=d,*  Future versions 
 * may permit enhanced queries.  In either case multiple MBeans may be returned and these MBeans would then 
 * be queried to obtain their attributes.  There are some important issues then using the wild card appraoch:
 * 
 * 1. Since multiple MBeans will have the same attribute name there needs to be a way to differentiate them.
 *    To handle this situation you need to specify which field in the ObjectName should be used.  This is defined
 *    as the key-field.
 * 
 * 2. The version of RRD that is used is limited to 19 characters.  If this limit is exceeded then the data will
 *    not be saved.  The name is defined as:  keyField_attributeName.rrd  Since the keyfield is defined in the 
 *    Object Name and may be too long, you may define an alias for it.  The key-alias parameter permit you to 
 *    define a list of names to be substituted.  Only exact matches are handled.  An example is:
 * 
 *    key-alias="this-name-is-long|thisIsNot,name-way-2-long,goodName"
 * 
 * 3. If there are keyfields that you want to exclude (exact matches) you may use a comma separated list like: 
 * 
 *    exclude="name1,name2,name3"
 * 
 * 4. Unlike the Standard MBeans there is no way (currently) to pre-define graphs for them in the snmp-graph.properties file.
 *    The only way you can create graphs is to create a custom graph in the Report section.
 * 
 * The wild card approach needs to be carefully considered before using it but it can cut down on the amount of
 * work necessary to define what to save.  
 * 
 * 
 * @author <A HREF="mailto:mike@opennms.org">Mike Jamison </A>
 * @author <A HREF="http://www.opennms.org/">OpenNMS </A>
 */

public abstract class JMXCollector implements ServiceCollector {

    /**
     * SQL statement to retrieve interface's 'ipinterface' table information.
     */

    private static final String SQL_GET_NODEID = "SELECT nodeid FROM ipinterface WHERE ipaddr=? AND ismanaged!='D'";

    /**
     * Interface attribute key used to store the map of IfInfo objects which
     * hold data about each interface on a particular node.
     */
    static String IF_MAP_KEY = "org.opennms.netmgt.collectd.JBossCollector.ifMap";

    /**
     * RRD data source name max length.
     */
    private static final int MAX_DS_NAME_LENGTH = 19;

    /**
     * Path to JMX RRD file repository.
     */
    private String m_rrdPath = null;

    /**
     * Local host name
     */
    private String m_host;

    /*
     * In some circumstances there may be many instances of a given service but running on
     * different ports.  Rather than using the port as the identfier users may define a more 
     * meaninful name. 
     */
    private boolean useFriendlyName = false;

    /* -------------------------------------------------------------- */
    /* Attr key names */
    /* -------------------------------------------------------------- */

    /**
     * Interface attribute key used to store a JMXNodeInfo object which holds
     * data about the node being polled.
     */
    static String NODE_INFO_KEY = "org.opennms.netmgt.collectd.JMXCollector.nodeInfo";

    static String MAX_VARS_PER_PDU_STORAGE_KEY = "org.opennms.netmgt.collectd.JMXCollector.maxVarsPerPdu";
    
    /*
     * The service name is provided by the derived class
     */
    private String serviceName = null;

    /**
     * <P>
     * Returns the name of the service that the plug-in collects ("JMX").
     * </P>
     * 
     * @return The service that the plug-in collects.
     */
    public String serviceName() {
        return serviceName.toUpperCase();
    }
    
    public void setServiceName(String name) {
        serviceName = name;
    }
    
    /**
     * <P>
     * Initialize the service collector.
     * </P>
     * 
     * <P>
     * During initialization the JMX collector: - Initializes various
     * configuration factories. - Verifies access to the database - Verifies
     * access to RRD file repository - Verifies access to JNI RRD shared library -
     * Determines if JMX to be stored for only the node'sprimary interface or
     * for all interfaces.
     * </P>
     * 
     * @param parameters
     *            Not currently used.
     * 
     * @exception RuntimeException
     *                Thrown if an unrecoverable error occurs that prevents the
     *                plug-in from functioning.
     *  
     */

    public void initialize(Map parameters) {
        
        // Log4j category
        //
        Category log = ThreadCategory.getInstance(getClass());
        
        // Get local host name (used when generating threshold events)
        try {            
            m_host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {            
            if (log.isEnabledFor(Priority.WARN))            
              log.warn("initialize: Unable to resolve local host name.", e);            
            m_host = "unresolved.host";
        }
        
        // Initialize the JMXDataCollectionConfigFactory
        try {
            JMXDataCollectionConfigFactory.reload();

        } catch (MarshalException ex) {
            if (log.isEnabledFor(Priority.FATAL))
                log.fatal("initialize: Failed to load data collection configuration",ex);
            throw new UndeclaredThrowableException(ex);
        } catch (ValidationException ex) {
            if (log.isEnabledFor(Priority.FATAL))
                log.fatal("initialize: Failed to load data collection configuration",ex);
            throw new UndeclaredThrowableException(ex);
        } catch (IOException ex) {
            if (log.isEnabledFor(Priority.FATAL))
                log.fatal("initialize: Failed to load data collection configuration",ex);
            throw new UndeclaredThrowableException(ex);
        }

        // Make sure we can connect to the database
        //
        java.sql.Connection ctest = null;
        try {
            DatabaseConnectionFactory.init();
            ctest = DatabaseConnectionFactory.getInstance().getConnection();
        } catch (IOException ie) {
            if (log.isEnabledFor(Priority.FATAL))
                log.fatal("initialize: IOException getting database connection", ie);
            throw new UndeclaredThrowableException(ie);
        } catch (MarshalException me) {
            if (log.isEnabledFor(Priority.FATAL))
                log.fatal("initialize: Marshall Exception getting database connection",me);
            throw new UndeclaredThrowableException(me);
        } catch (ValidationException ve) {
            if (log.isEnabledFor(Priority.FATAL))
                log.fatal("initialize: Validation Exception getting database connection",ve);
            throw new UndeclaredThrowableException(ve);
        } catch (SQLException sqlE) {
            if (log.isEnabledFor(Priority.FATAL))
                log.fatal("initialize: Failed getting connection to the database.",sqlE);
            throw new UndeclaredThrowableException(sqlE);
        } catch (ClassNotFoundException cnfE) {
            if (log.isEnabledFor(Priority.FATAL))
                log.fatal("initialize: Failed loading database driver.", cnfE);
            throw new UndeclaredThrowableException(cnfE);

        } finally {
            if (ctest != null) {
                try {
                    ctest.close();
                } catch (Throwable t) {
                    if (log.isEnabledFor(Priority.WARN))
                        log.warn("initialize: an exception occured while closing the JDBC connection",t);
                }
            }
        }
        
        // Save local reference to singleton instance
        //
        // m_rrdInterface = org.opennms.netmgt.rrd.Interface.getInstance();

        if (log.isDebugEnabled())
            log.debug("initialize: successfully instantiated JNI interface to RRD...");
        return;
    }
    
    private void initRRD() {
        Category log = ThreadCategory.getInstance(getClass());
        
        // Get path to RRD repository
        m_rrdPath = JMXDataCollectionConfigFactory.getInstance().getRrdRepository();

        if (m_rrdPath == null)
            throw new RuntimeException(
                    "Configuration error, failed to retrieve path to RRD repository.");

        // Strip the File.separator char off of the end of the path

        if (m_rrdPath.endsWith(File.separator)) {
            m_rrdPath = m_rrdPath.substring(0,(m_rrdPath.length() - File.separator.length()));
        }

        if (log.isDebugEnabled())
            log.debug("initialize: " + serviceName + " RRD file repository path: " + m_rrdPath);

        // If the RRD file repository directory does NOT already exist, create
        // it.
        //

        File f = new File(m_rrdPath);
        if (!f.isDirectory())
            if (!f.mkdirs())
                throw new RuntimeException("Unable to create RRD file repository, path: " + m_rrdPath);

        try {
            RrdUtils.initialize();
        } catch (RrdException e) {
            if (log.isEnabledFor(Priority.ERROR))
                log.error("initialize: Unable to initialize RrdUtils", e);
            throw new RuntimeException("Unable to initialize RrdUtils", e);
        }
    }

    /**
     * 
     * Responsible for freeing up any resources held by the collector.
     */

    public void release() {
        // Nothing to release...
    }

    /**
     * Responsible for performing all necessary initialization for the specified
     * interface in preparation for data collection.
     * 
     * @param iface
     *            Network interface to be prepped for collection.
     * @param parameters
     *            Key/value pairs associated with the package to which the
     *            interface belongs..
     *  
     */

    public void initialize(NetworkInterface iface, Map parameters) {
        Category    log    = ThreadCategory.getInstance(getClass());
        InetAddress ipAddr = (InetAddress) iface.getAddress();
        
        log.debug("initialize: " + m_rrdPath);
        if (m_rrdPath == null) {
            initRRD();
        }
        
        if (log.isDebugEnabled())
            log.debug("initialize: InetAddress=" + ipAddr.getHostAddress());
        
        // Retrieve the name of the JMX data collector
        String collectionName = ParameterMap.getKeyedString(parameters,"collection", serviceName);

        if (log.isDebugEnabled())
            log.debug("initialize: collectionName=" + collectionName);
        java.sql.Connection dbConn = null;
        try {
            dbConn = DatabaseConnectionFactory.getInstance().getConnection();
        } catch (SQLException sqlE) {
            if (log.isEnabledFor(Priority.ERROR))
                log.error("initialize: Failed getting connection to the database.",sqlE);
            throw new UndeclaredThrowableException(sqlE);
        }

        int nodeID = -1;

        // Prepare & execute the SQL statement to get the 'nodeid' from the
        // ipInterface table 'nodeid' will be used to retrieve the node's
        // system object id from the node table.
        // In addition to nodeid, the interface's ifIndex
        // fields are also retrieved.
        //

        PreparedStatement stmt = null;

        try {

            stmt = dbConn.prepareStatement(SQL_GET_NODEID);
            stmt.setString(1, ipAddr.getHostAddress()); // interface address
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                nodeID = rs.getInt(1);
                if (rs.wasNull())
                    nodeID = -1;
            } else {
                nodeID = -1;
            }
            rs.close();

        } catch (SQLException sqle) {
            if (log.isDebugEnabled())
                log.debug("initialize: SQL exception!!", sqle);
            throw new RuntimeException(
                    "SQL exception while attempting to retrieve node id for interface "
                            + ipAddr.getHostAddress());
        } finally {
            try {
                stmt.close();
            } catch (Exception e) {
                // Ignore
            }
        }

        JMXNodeInfo nodeInfo = new JMXNodeInfo(nodeID);
        log.debug("nodeInfo: " + ipAddr.getHostAddress() + " " + nodeID + " " + iface);

        // Retrieve list of MBean objects to be collected from the
        // remote agent which are to be stored in the node-level RRD file.
        // These objects pertain to the node itself not any individual
        // interfaces.

        List attrList = JMXDataCollectionConfigFactory.getInstance().getAttributeList(collectionName, serviceName, ipAddr.getHostAddress());

        nodeInfo.setAttributeList(attrList);
        HashMap dsList = buildDataSourceList(collectionName, attrList);
        nodeInfo.setDsMap(dsList);
        nodeInfo.setMBeans(JMXDataCollectionConfigFactory.getInstance().getMBeanInfo(collectionName));

        // Add the JMXNodeInfo object as an attribute of the interface
        //
        iface.setAttribute(NODE_INFO_KEY, nodeInfo);
        iface.setAttribute("collectionName", collectionName);
    }

    /**
     * 
     * Responsible for releasing any resources associated with the specified
     * interface.
     * 
     * @param iface
     *            Network interface to be released.
     */

    public void release(NetworkInterface iface) {
        // Nothing to release...
    }
    
    public abstract ConnectionWrapper getMBeanServerConnection(Map parameterMap, InetAddress address);

    /**
     * Perform data collection.
     * 
     * @param iface
     *            Network interface to be data collected
     * @param eproxy
     *            Eventy proxy for sending events.
     * @param parameters
     *            Key/value pairs from the package to which the interface
     *            belongs.
     */

    public int collect(NetworkInterface iface, EventProxy eproxy, Map map) {
        Category    log            = ThreadCategory.getInstance(getClass());
        InetAddress ipaddr         = (InetAddress) iface.getAddress();
        String      collectionName = (String) iface.getAttribute("collectionName");
        JMXNodeInfo nodeInfo       = (JMXNodeInfo) iface.getAttribute(NODE_INFO_KEY);
        HashMap     list           = nodeInfo.getDsMap();
        HashMap     mbeans         = nodeInfo.getMBeans();
        String      collDir        = serviceName;
        
        ConnectionWrapper connection = null;

        log.debug("collect " + ipaddr.getHostAddress() + " " + nodeInfo.getNodeId() + " " + m_rrdPath);

        try {

            String rrdPath      = ParameterMap.getKeyedString( map, "rrdRepository",  null);
            int    retry        = ParameterMap.getKeyedInteger(map, "retry",          3);
            String port         = ParameterMap.getKeyedString( map, "port",           null);
            String friendlyName = ParameterMap.getKeyedString( map, "friendly-name",  port);
            
            InetAddress ipv4Addr = (InetAddress)iface.getAddress();
            
            connection = getMBeanServerConnection(map, ipaddr);
            
            if (connection == null) {
                return COLLECTION_FAILED;
            }
            
            MBeanServerConnection mbeanServer = connection.getMBeanServer();
            
            int serviceStatus = COLLECTION_FAILED;
            
            if (useFriendlyName) {
                collDir = friendlyName;
            }

            for (int attempts = 0; attempts <= retry; attempts++) {
                try {

                    /*
                     * Iterate over the mbeans, for each object name perform a
                     * getAttributes, the update the RRD.
                     */

                    for (Iterator iter = mbeans.values().iterator(); iter.hasNext();) {
                        BeanInfo beanInfo    = (BeanInfo) iter.next();
                        String   objectName  = beanInfo.getObjectName();
                        String   excludeList = beanInfo.getExcludes();
                        
                        String[] attrNames = beanInfo.getAttributeNames();
                        
                        if (objectName.indexOf("*") == -1) {
                            log.debug(serviceName + " Collector - getAttributes: " + objectName + " #attributes: " + attrNames.length);
 
                            try {
                                AttributeList attrList = (AttributeList) mbeanServer.getAttributes(new ObjectName(objectName),attrNames);
                                updateRRDs(collectionName, iface, attrList, collDir, null, null);
                            } catch (InstanceNotFoundException e2) {
                                log.error("Unable to retrieve attributes from " + objectName);
                            }
                        }
                        else {
                            Set mbeanSet = mbeanServer.queryNames(new ObjectName(objectName),null);
                            for (Iterator objectNameIter = mbeanSet.iterator(); objectNameIter.hasNext();) {
                                ObjectName oName = (ObjectName)objectNameIter.next();
                                log.debug(serviceName + " Collector - getAttributesWC: " + oName + " #attributes: " + attrNames.length + " " + beanInfo.getKeyAlias());
  
                                try {
                                    if (excludeList == null) {
                                        /*
                                         * the exclude list doesn't apply
                                         */
                                        AttributeList attrList = (AttributeList) mbeanServer.getAttributes(oName, attrNames);
                                        updateRRDs(collectionName, 
                                                   iface, 
                                                   attrList, 
                                                   collDir,
                                                   oName.getKeyProperty(beanInfo.getKeyField()), 
                                                   beanInfo.getKeyAlias());
                                    }
                                    else {
                                        /*
                                         * filter out calls if the key field matches an entry in the exclude list
                                         */
                                        String keyName = oName.getKeyProperty(beanInfo.getKeyField());
                                        boolean found = false;
                                        StringTokenizer st = new StringTokenizer(excludeList, ",");
                                        while (st.hasMoreTokens()) {
                                            if (keyName.equals(st.nextToken())) {
                                                found = true;
                                                break;
                                            }
                                        }
                                        if (!found) {
                                            AttributeList attrList = (AttributeList) mbeanServer.getAttributes(oName, attrNames);
                                            updateRRDs(collectionName, 
                                                       iface, 
                                                       attrList, 
                                                       collDir,
                                                       oName.getKeyProperty(beanInfo.getKeyField()), 
                                                       beanInfo.getKeyAlias());
                                        }
                                    }
                                } catch (InstanceNotFoundException e2) {
                                    log.error("Error retrieving attributes for " + oName);
                                }
                            }
                        }
                        serviceStatus = COLLECTION_SUCCEEDED;
                    }
                    break;
                }
                catch (Exception e) {
                    e.fillInStackTrace();
                    log.debug(serviceName + " Collector.collect: IOException while collect address: " + iface.getAddress(), e);
                }
            } // of for
        } catch (Exception e1) {
            log.error("Error getting MBeanServer");
       } finally {
           if (connection != null) {
               connection.close();
           }
       }
        //
        // return the status of the collection
        //
        return COLLECTION_SUCCEEDED;
    }

    public boolean createRRD(String         collectionName, 
                             InetAddress    ipaddr, 
                             String         directory, 
                             RRDDataSource  ds,
                             String         collectionDir,
                             String         keyField) 
    throws RrdException {
        String creator = "primary " + serviceName + " interface " + ipaddr.getHostAddress();
        int    step    = JMXDataCollectionConfigFactory.getInstance().getStep(collectionName);
        List   rraList = JMXDataCollectionConfigFactory.getInstance().getRRAList(collectionName);
        

        File repos = new File(directory + "/" + collectionDir);
        if (!repos.exists()) {
            repos.mkdir();
        }

        if (keyField == null) {
            return RrdUtils.createRRD(creator, 
                                     directory + "/" + collectionDir,
                                     ds.getName(), 
                                     step, 
                                     ds.getType(), 
                                     ds.getHeartbeat(), 
                                     ds.getMin(), 
                                     ds.getMax(), 
                                     rraList);
        }
        else {
           if (keyField.equals("")) {
               return RrdUtils.createRRD(creator, 
                       directory + "/" + collectionDir,
                       ds.getName(), 
                       step, 
                       ds.getType(), 
                       ds.getHeartbeat(), 
                       ds.getMin(), 
                       ds.getMax(), 
                       rraList);
           }
           else {
               return RrdUtils.createRRD(creator, 
                       directory + "/" + collectionDir,
                       keyField + "_" + ds.getName(), 
                       step, 
                       ds.getType(), 
                       ds.getHeartbeat(), 
                       ds.getMin(), 
                       ds.getMax(), 
                       rraList);
           }
        }
    }

    /**
     * This method is responsible for building an RRDTool style 'update' command
     * which is issued via the RRD JNI interface in order to push the latest
     * JMX-collected values into the interface's RRD database.
     * 
     * @param collectionName
     *            JMX data Collection name from 'jmx-datacollection-config.xml'
     * @param iface
     *            NetworkInterface object of the interface currently being
     *            polled
     * @param nodeCollector
     *            Node level MBean data collected via JMX for the polled
     *            interface
     * @param ifCollector
     *            Interface level MBean data collected via JMX for the polled
     *            interface
     * 
     * @exception RuntimeException
     *                Thrown if the data source list for the interface is null.
     */

    private boolean updateRRDs(String           collectionName, 
                               NetworkInterface iface,
                               AttributeList    attributeList,
                               String           collectionDir,
                               String           keyField,
                               String           substitutions) {

        Category log = ThreadCategory.getInstance(getClass());
        InetAddress ipaddr = (InetAddress) iface.getAddress();

        JMXNodeInfo nodeInfo = (JMXNodeInfo) iface.getAttribute(NODE_INFO_KEY);

        boolean rrdError = false;
        
        // -----------------------------------------------------------
        // Node data
        // -----------------------------------------------------------

        log.debug("updateRRDs: processing node-level collection...");

        // Build path to node RRD repository. createRRD() will make the
        // appropriate directories if they do not already exist.
        //

        String nodeRepository = m_rrdPath + File.separator + String.valueOf(nodeInfo.getNodeId());
        
        // Iterate over the node datasource list and issue RRD update
        // commands to update each datasource which has a corresponding
        // value in the collected JMX data
        //
        HashMap dsMap = nodeInfo.getDsMap();

        try {
            for (int i = 0; i < attributeList.size(); i++) {
                Attribute attribute = (Attribute) attributeList.get(i);
                RRDDataSource ds = (RRDDataSource) dsMap.get(attribute.getName());
                if (keyField == null) {
                    try {
                        createRRD(collectionName, ipaddr, nodeRepository, ds, collectionDir, null);
                        RrdUtils.updateRRD(ipaddr.getHostAddress(), 
                                           nodeRepository + "/" + collectionDir, 
                                           ds.getName(), 
                                           "" + attribute.getValue());
                    } catch (Throwable e1) {
                    }
                }
                else {
                    try {
                        String key = fixKey(keyField, ds.getName(), substitutions);
                        createRRD(collectionName, ipaddr, nodeRepository, ds, collectionDir, key);
                        if (key.equals("")) {
                            RrdUtils.updateRRD(ipaddr.getHostAddress(), 
                                    nodeRepository + "/" + collectionDir, 
                                    ds.getName(), 
                                    "" + attribute.getValue());
                        }
                        else {
                            RrdUtils.updateRRD(ipaddr.getHostAddress(), 
                                    nodeRepository + "/" + collectionDir, 
                                    key + "_" + ds.getName(), 
                                    "" + attribute.getValue());
                        }
                    } catch (Throwable e1) {
                        //log.debug("Error updating: " ds.getName());
                    }
                }
                try {
                    Thread.currentThread().sleep(500);
                }catch (Exception te) {
                    
                }
            }
        } catch (Throwable e) {
            log.error("RRD Error", e);
            rrdError = true;
        }
        return rrdError;
    }
    
    /*
     * This method strips out the illegal character '/' and attempts to keep the length 
     * of the key plus ds name to 19 or less characters.  The slash character cannot be in
     * the name since it is an illegal character in file names.
     */
    private String fixKey(String key, String attrName, String substitutions) {
        String newKey = key;
        if (key.startsWith("/")) {
            newKey = key.substring(1);
        }
        if (substitutions != null && substitutions.length() > 0) {
            StringTokenizer st = new StringTokenizer(substitutions, ",");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                int index = token.indexOf("|");
                if (newKey.equals(token.substring(0,index))) {
                    newKey = token.substring(index+1);
                }
            }
        }
        return newKey;
    }

    /**
     * @param ds
     * @param collectorEntry
     * @param
     * @param dsVal
     * @return
     * @throws Exception
     */

    public String getRRDValue(RRDDataSource ds, JMXCollectorEntry collectorEntry)
     throws IllegalArgumentException {
        Category log = ThreadCategory.getInstance(getClass());
        String dsVal = null;

        // Make sure we have an actual object id value.
        if (ds.getOid() == null)
            return null;

        return (String) collectorEntry.get(ds.getOid());
    }

    /**
     * This method is responsible for building a list of RRDDataSource objects
     * from the provided list of MBeanObject objects.
     * 
     * @param collectionName
     *            Collection name
     * @param oidList
     *            List of MBeanObject objects defining the oid's to be collected
     *            via JMX.
     * 
     * @return list of RRDDataSource objects
     */

    private HashMap buildDataSourceList(String collectionName, List attributeList) {

        // Log4j category
        //
        Category log = ThreadCategory.getInstance(getClass());

        // Retrieve the RRD expansion data source list which contains all
        // the expansion data source's. Use this list as a basis
        // for building a data source list for the current interface.
        //

        HashMap dsList = new HashMap();

        // Loop through the MBean object list to be collected for this interface
        // and add a corresponding RRD data source object. In this manner
        // each interface will have RRD files create which reflect only the data
        // sources pertinent to it.
        //

        Iterator o = attributeList.iterator();
        while (o.hasNext()) {
            Attrib attr = (Attrib) o.next();
            RRDDataSource ds = null;

            // Verify that this object has an appropriate "integer" data type
            // which can be stored in an RRD database file (must map to one of
            // the supported RRD data source types: COUNTER or GAUGE).
            String ds_type = RRDDataSource.mapType(attr.getType());
            if (ds_type != null) {
                // Passed!! Create new data source instance for this MBean
                // object
                // Assign heartbeat using formula (2 * step) and hard code
                // min & max values to "U" ("unknown").
                ds = new RRDDataSource();
                ds.setHeartbeat(2 * JMXDataCollectionConfigFactory
                        .getInstance().getStep(collectionName));
                // For completeness, adding a minval option to the variable.
                String ds_minval = attr.getMinval();
                if (ds_minval == null) {
                    ds_minval = "U";
                }
                ds.setMax(ds_minval);

                // In order to handle counter wraps, we need to set a max
                // value for the variable.
                String ds_maxval = attr.getMaxval();
                if (ds_maxval == null) {
                    ds_maxval = "U";
                }

                ds.setMax(ds_maxval);
                ds.setInstance(collectionName);

                // Truncate MBean object name/alias if it exceeds 19 char max
                // for
                // RRD data source names.
                String ds_name = attr.getAlias();
                if (ds_name.length() > MAX_DS_NAME_LENGTH) {
                    if (log.isEnabledFor(Priority.WARN))
                        log.warn("buildDataSourceList: alias '"
                                        + attr.getAlias()
                                        + "' exceeds 19 char maximum for RRD data source names, truncating.");
                    char[] temp = ds_name.toCharArray();
                    ds_name = String.copyValueOf(temp, 0, MAX_DS_NAME_LENGTH);
                }
                ds.setName(ds_name);

                // Map MBean object data type to RRD data type
                ds.setType(ds_type);

                // Assign the data source object identifier and instance
                //ds.setName(attr.getName());
                ds.setOid(attr.getName());

                if (log.isDebugEnabled())
                    log.debug("buildDataSourceList: ds_name: " + ds.getName()
                            + " ds_oid: " + ds.getOid() + "."
                            + ds.getInstance() + " ds_max: " + ds.getMax()
                            + " ds_min: " + ds.getMin());

                // Add the new data source to the list
                dsList.put(attr.getName(), ds);
            } else if (log.isEnabledFor(Priority.WARN)) {
                log.warn("buildDataSourceList: Data type '"
                                + attr.getType()
                                + "' not supported.  Only integer-type data may be stored in RRD.");
                log.warn("buildDataSourceList: MBean object '"
                        + attr.getAlias()
                        + "' will not be mapped to RRD data source.");
            }
        }

        return dsList;
    }

    /**
     * This method is responsible for building a Capsd forceRescan event object
     * and sending it out over the EventProxy.
     * 
     * @param ifAddress
     *            interface address to which this event pertains
     * @param eventProxy
     *            proxy over which an event may be sent to eventd
     */

    private void generateForceRescanEvent(String ifAddress, EventProxy eventProxy) {
       
        // Log4j category
        //
        Category log = ThreadCategory.getInstance(getClass());
        if (log.isDebugEnabled())
            log.debug("generateForceRescanEvent: interface = " + ifAddress + " serviceName: " + serviceName);

        // create the event to be sent
        Event newEvent = new Event();
        newEvent.setUei(EventConstants.FORCE_RESCAN_EVENT_UEI);
        newEvent.setSource(serviceName.toUpperCase() + "ServiceMonitor");
        newEvent.setInterface(ifAddress);
        newEvent.setService(serviceName.toUpperCase());

        if (m_host != null)
            newEvent.setHost(m_host);

        newEvent.setTime(EventConstants.formatToString(new java.util.Date()));

        // Send event via EventProxy
        try {
            eventProxy.send(newEvent);
        } catch (Exception e) {
            if (log.isEnabledFor(Priority.ERROR))
                log.error("generateForceRescanEvent: Unable to send forceRescan event.", e);
        }
    }
    /**
     * @param useFriedlyName The useFriedlyName to set.
     */
    public void setUseFriendlyName(boolean useFriendlyName) {
        this.useFriendlyName = useFriendlyName;
    }
}

