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

package org.opennms.netmgt.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Category;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.ValidationException;
import org.opennms.core.utils.ThreadCategory;
import org.opennms.netmgt.ConfigFileConstants;
import org.opennms.netmgt.collectd.Attr;
import org.opennms.netmgt.config.collectd.Attrib;
import org.opennms.netmgt.config.collectd.JmxDatacollectionConfig;
import org.opennms.netmgt.config.collectd.Mbean;
import org.opennms.netmgt.config.collectd.Mbeans;

/**
 * This class is the main respository for JMX data collection configuration
 * information used by the an instance of the JMX service monitor. When this class is loaded it
 * reads the jmx data collection configuration into memory.
 * 
 * <strong>Note: </strong>Users of this class should make sure the
 * <em>init()</em> is called before calling any other method to ensure the
 * config is loaded before accessing other convenience methods.
 * 
 * 
 * @author <A HREF="mailto:mike@opennms.org">Mike Jamison </A>
 * @author <A HREF="http://www.opennms.org/">OpenNMS </A>
 *
 */

public final class JMXDataCollectionConfigFactory {
    /**
     * The singleton instance of this factory
     */
    private static JMXDataCollectionConfigFactory m_singleton = null;

    /**
     * The config class loaded from the config file
     */
    private JmxDatacollectionConfig m_config;
     

    /**
     * This member is set to true if the configuration file has been loaded.
     */
    private static boolean m_loaded = false;

    /**
     * Map of group maps indexed by SNMP collection name.
     */
    private Map m_collectionGroupMap;

    /**
     * Map of JmxCollection objects indexed by data collection name
     */
    private Map m_collectionMap;

    /**
     * Private constructor
     * 
     * @exception java.io.IOException
     *                Thrown if the specified config file cannot be read
     * @exception org.exolab.castor.xml.MarshalException
     *                Thrown if the file does not conform to the schema.
     * @exception org.exolab.castor.xml.ValidationException
     *                Thrown if the contents do not match the required schema.
     */
    private JMXDataCollectionConfigFactory(String configFile) throws IOException, MarshalException, ValidationException {
        InputStream cfgIn = new FileInputStream(configFile);

        m_config = (JmxDatacollectionConfig) Unmarshaller.unmarshal(JmxDatacollectionConfig.class, new InputStreamReader(cfgIn));
        cfgIn.close();

        // Build collection map which is a hash map of Collection
        // objects indexed by collection name...also build
        // collection group map which is a hash map indexed
        // by collection name with a hash map as the value
        // containing a map of the collections's group names
        // to the Group object containing all the information
        // for that group. So the associations are:
        //
        // CollectionMap
        // collectionName -> Collection
        //
        // CollectionGroupMap
        // collectionName -> groupMap
        // 
        // GroupMap
        // groupMapName -> Group
        //
        // This is parsed and built at initialization for
        // faster processing at run-timne.
        // 
        m_collectionMap = new HashMap();
        m_collectionGroupMap = new HashMap();
        
        // BOZO isn't the collection name defined in the jmx-datacollection.xml file and
        // global to all the mbeans?
        java.util.Collection collections = m_config.getJmxCollectionCollection();
        Iterator citer = collections.iterator();
        while (citer.hasNext()) {
            org.opennms.netmgt.config.collectd.JmxCollection collection = (org.opennms.netmgt.config.collectd.JmxCollection) citer.next();

            // Build group map for this collection
            Map groupMap = new HashMap();

            Mbeans mbeans = collection.getMbeans();
            java.util.Collection groupList = mbeans.getMbeanCollection();
            Iterator giter = groupList.iterator();
            while (giter.hasNext()) {
                Mbean mbean = (Mbean) giter.next();
                groupMap.put(mbean.getName(), mbean);
            }

            m_collectionGroupMap.put(collection.getName(), groupMap);
            m_collectionMap.put(collection.getName(), collection);
        }
    }

    /**
     * Load the config from the default config file and create the singleton
     * instance of this factory.
     * 
     * @exception java.io.IOException
     *                Thrown if the specified config file cannot be read
     * @exception org.exolab.castor.xml.MarshalException
     *                Thrown if the file does not conform to the schema.
     * @exception org.exolab.castor.xml.ValidationException
     *                Thrown if the contents do not match the required schema.
     */
    public static synchronized void init() throws IOException, MarshalException, ValidationException {
        if (m_loaded) {
            // init already called - return
            // to reload, reload() will need to be called
            return;
        }


        try {
            File cfgFile = ConfigFileConstants.getFile(ConfigFileConstants.JMX_DATA_COLLECTION_CONF_FILE_NAME);

            ThreadCategory.getInstance(JMXDataCollectionConfigFactory.class).debug("init: config file path: " + cfgFile.getPath());
            m_singleton = new JMXDataCollectionConfigFactory(cfgFile.getPath());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            m_singleton = new JMXDataCollectionConfigFactory("/opt/opennms/rrd/response/jmx/");
        }

        m_loaded = true;
    }

    /**
     * Reload the config from the default config file
     * 
     * @exception java.io.IOException
     *                Thrown if the specified config file cannot be read/loaded
     * @exception org.exolab.castor.xml.MarshalException
     *                Thrown if the file does not conform to the schema.
     * @exception org.exolab.castor.xml.ValidationException
     *                Thrown if the contents do not match the required schema.
     */
    public static synchronized void reload() throws IOException, MarshalException, ValidationException {
        m_singleton = null;
        m_loaded = false;

        init();
    }

    /**
     * Return the singleton instance of this factory.
     * 
     * @return The current factory instance.
     * 
     * @throws java.lang.IllegalStateException
     *             Thrown if the factory has not yet been initialized.
     */
    public static synchronized JMXDataCollectionConfigFactory getInstance() {
        if (!m_loaded)
            throw new IllegalStateException("The factory has not been initialized");

        return m_singleton;
    }

    /**
     * This method returns the list of MIB objects associated with a particular
     * system object id, IP address, and ifType for the specified collection.
     * 
     * @param cName
     *            name of the data collection from which to retrieve oid
     *            information.
     * @param aSysoid
     *            system object id to look up in the collection
     * @param anAddress
     *            IP address to look up in the collection
     * @param ifType
     *            Interface type (-1 indicates that only node-level objects
     *            should be retrieved.
     * 
     * @return a list of MIB objects
     */
    public List getAttributeList(String cName, String aSysoid, String anAddress) {
        Category log = ThreadCategory.getInstance(getClass());

        if (log.isDebugEnabled())
            log.debug("getMibObjectList: collection: " + cName + " sysoid: " + aSysoid + " address: " + anAddress);

        if (aSysoid == null) {
            if (log.isDebugEnabled())
                log.debug("getMibObjectList: aSysoid parameter is NULL...");
            return new ArrayList();
        }

        // Retrieve the appropriate Collection object
        // 
        org.opennms.netmgt.config.collectd.JmxCollection collection = (org.opennms.netmgt.config.collectd.JmxCollection) m_collectionMap.get(cName);
        if (collection == null) {
            return new ArrayList();
        }
        ArrayList list = new ArrayList();
        Mbeans beans = collection.getMbeans();
        Enumeration en = beans.enumerateMbean();
        while (en.hasMoreElements()) {
            Mbean mbean = (Mbean)en.nextElement();
            Attrib[] attributes = mbean.getAttrib();
            for (int i = 0; i < attributes.length; i++) {
                list.add(attributes[i]);
            }
        }
        return list;
    }
    
    public HashMap getMBeanInfo(String cName) {
        HashMap map = new HashMap();
        
        // Retrieve the appropriate Collection object
        // 
        org.opennms.netmgt.config.collectd.JmxCollection collection = (org.opennms.netmgt.config.collectd.JmxCollection) m_collectionMap.get(cName);
        
        Mbeans beans = collection.getMbeans();
        Enumeration en = beans.enumerateMbean();
        while (en.hasMoreElements()) {
        	BeanInfo beanInfo = new BeanInfo();
        	
            Mbean mbean = (Mbean)en.nextElement();
            beanInfo.setMbeanName(mbean.getName());
            beanInfo.setObjectName(mbean.getObjectname());
            beanInfo.setKeyField(mbean.getKeyfield());
            beanInfo.setExcludes(mbean.getExclude());
            beanInfo.setKeyAlias(mbean.getKeyAlias());
            int count = mbean.getAttribCount();
            String[] attribs = new String[count];
            Attrib[] attributes = mbean.getAttrib();
            for (int i = 0; i < attributes.length; i++) {
                attribs[i] = attributes[i].getName();
            }
            
            beanInfo.setAttributes(attribs);
            map.put(mbean.getObjectname(), beanInfo);
        }
        return map;
    }

    public HashMap getMBeanInfo_save(String cName) {
        HashMap map = new HashMap();
        
        // Retrieve the appropriate Collection object
        // 
        org.opennms.netmgt.config.collectd.JmxCollection collection = (org.opennms.netmgt.config.collectd.JmxCollection) m_collectionMap.get(cName);
        
        Mbeans beans = collection.getMbeans();
        Enumeration en = beans.enumerateMbean();
        while (en.hasMoreElements()) {
            Mbean mbean = (Mbean)en.nextElement();
            int count = mbean.getAttribCount();
            String[] attribs = new String[count];
            Attrib[] attributes = mbean.getAttrib();
            for (int i = 0; i < attributes.length; i++) {
                attribs[i] = attributes[i].getName();
            }
            map.put(mbean.getObjectname(), attribs);
        }
        return map;
    }

    /**
     * Takes a list of castor generated MibObj objects iterates over them
     * creating corresponding MibObject objects and adding them to the supplied
     * MibObject list.
     * 
     * @param objectList
     *            List of MibObject objects parsed from
     *            'datacollection-config.xml'
     * @param mibObjectList
     *            List of MibObject objects currently being built 
     */ 
    static void processObjectList(List objectList, List mibObjectList) { 
        Iterator i = objectList.iterator();
        while (i.hasNext()) {
            Attrib mibObj = (Attrib) i.next();

            // Create a MibObject from the castor MibObj
            Attr aMibObject = new Attr();
            aMibObject.setName(mibObj.getName());
            aMibObject.setAlias(mibObj.getAlias());
            aMibObject.setType(mibObj.getType());
            aMibObject.setMaxval(mibObj.getMaxval());
            aMibObject.setMinval(mibObj.getMinval());

            // Add the MIB object provided it isn't already in the list
            if (!mibObjectList.contains(aMibObject)) {
                mibObjectList.add(aMibObject);
            }
        }
    }

    /**
     * Retrieves configured RRD step size.
     * 
     * @param cName
     *            Name of the data collection
     * 
     * @return RRD step size for the specified collection
     */
    public int getStep(String cName) {
        org.opennms.netmgt.config.collectd.JmxCollection collection = (org.opennms.netmgt.config.collectd.JmxCollection) m_collectionMap.get(cName);
        if (collection != null)
            return collection.getRrd().getStep();
        else
            return -1;
    }

    /**
     * Retrieves configured list of RoundRobin Archive statements.
     * 
     * @param cName
     *            Name of the data collection
     * 
     * @return list of RRA strings.
     */
    public List getRRAList(String cName) {
        org.opennms.netmgt.config.collectd.JmxCollection collection = (org.opennms.netmgt.config.collectd.JmxCollection) m_collectionMap.get(cName);
        if (collection != null)
            return (List) collection.getRrd().getRraCollection();
        else
            return null;

    }

    /**
     * Retrieves the configured value for the maximum number of variables (oids)
     * which can be encoded into a single outgoing SNMP PDU request..
     * 
     * @param cName
     *            Name of the data collection
     * 
     * @return max number of variables per pdu or -1 upon error
     */
    public int getMaxVarsPerPdu(String cName) {
        org.opennms.netmgt.config.collectd.JmxCollection collection = (org.opennms.netmgt.config.collectd.JmxCollection) m_collectionMap.get(cName);
        if (collection != null)
            return collection.getMaxVarsPerPdu();
        else
            return -1;
    }

    /**
     * Retrieves the configured path to the RRD file repository.
     * 
     * @return RRD repository path.
     */
    public String getRrdRepository() {
        return m_config.getRrdRepository();
    }

}
