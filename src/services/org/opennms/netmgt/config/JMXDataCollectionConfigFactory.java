/*
 * Created on Mar 1, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.opennms.netmgt.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
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
import org.opennms.netmgt.config.collectd.*;
import org.opennms.netmgt.config.collectd.JmxDatacollectionConfig;
import org.opennms.netmgt.config.collectd.Mbean;
import org.opennms.netmgt.config.collectd.Mbeans;

/**
 * This class is the main respository for SNMP data collection configuration
 * information used by the SNMP service monitor. When this class is loaded it
 * reads the snmp data collection configuration into memory.
 * 
 * <strong>Note: </strong>Users of this class should make sure the
 * <em>init()</em> is called before calling any other method to ensure the
 * config is loaded before accessing other convenience methods.
 * 
 * @author <a href="mailto:weave@oculan.com">Weave </a>
 * @author <a href="http://www.opennms.org/">OpenNMS </a>
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
            m_singleton = new JMXDataCollectionConfigFactory("/Users/mjamison/Desktop/OpenNMS-3/share/rrd/jmx/");
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
     * Converts the internet address to a long value so that it can be compared
     * using simple opertions. The address is converted in network byte order
     * (big endin) and allows for comparisions like &lt;, &gt;, &lt;=, &gt;=,
     * ==, and !=.
     * 
     * @param addr
     *            The address to convert to a long
     * 
     * @return The address as a long value.
     * 
     */
    private static long toLong(InetAddress addr) {
        byte[] baddr = addr.getAddress();
        long result = ((long) baddr[0] & 0xffL) << 24 | ((long) baddr[1] & 0xffL) << 16 | ((long) baddr[2] & 0xffL) << 8 | ((long) baddr[3] & 0xffL);

        return result;
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
        Enumeration enum = beans.enumerateMbean();
        while (enum.hasMoreElements()) {
            Mbean mbean = (Mbean)enum.nextElement();
            System.out.println("MBean: " + mbean.getName() + " " + mbean.getObjectname());
            Attrib[] attributes = mbean.getAttrib();
            for (int i = 0; i < attributes.length; i++) {
                list.add(attributes[i]);
                System.out.println("addAttribute: " + attributes[i].getName());
            }
        }
        return list;
    }
    
    public HashMap getMBeanInfo(String cName) {
        HashMap map = new HashMap();
        
        // Retrieve the appropriate Collection object
        // 
        org.opennms.netmgt.config.collectd.JmxCollection collection = (org.opennms.netmgt.config.collectd.JmxCollection) m_collectionMap.get(cName);
        
        ArrayList list = new ArrayList();
        Mbeans beans = collection.getMbeans();
        Enumeration enum = beans.enumerateMbean();
        while (enum.hasMoreElements()) {
            Mbean mbean = (Mbean)enum.nextElement();
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
     * Private utility method used by the getMibObjectList() method. This method
     * takes a group name and a list of MibObject objects as arguments and adds
     * all of the MibObjects associated with the group to the object list. If
     * the passed group consists of any additional sub-groups, then this method
     * will be called recursively for each sub-group until the entire
     * log.debug("processGroupName: adding MIB objects from group: " +
     * groupName); group is processed.
     * 
     * @param cName
     *            Collection name
     * @param groupName
     *            Name of the group to process
     * @param ifType
     *            Interface type
     * @param mibObjectList
     *            List of MibObject objects being built.
     */
    private void processGroupName(String cName, String groupName, List mibObjectList) {
        Category log = ThreadCategory.getInstance(getClass());

        // Using the collector name retrieve the group map
        Map groupMap = (Map) m_collectionGroupMap.get(cName);

        // Next use the groupName to access the Group object
        Mbean mbean = (Mbean) groupMap.get(groupName);

        // Verify that we have a valid Group object...generate
        // warning message if not...
        if (mbean == null) {
            log.warn("JmxDataCollectionConfigFactory.processGroupName: unable to retrieve group information for group name '" + groupName + "': check DataCollection.xml file.");
            return;
        }

        if (log.isDebugEnabled())
            log.debug("processGroupName:  processing group: " + groupName);

        // Process any sub-groups contained within this group
/*        
        List groupNameList = (List) mbean.getIncludeMbeanCollection();
        Iterator i = groupNameList.iterator();
        while (i.hasNext()) {
            processGroupName(cName, (String) i.next(), mibObjectList); // Recursive
                                                                       // call
                                                                       // to
                                                                       // process
                                                                       // sub-groups
        }
*/        
        boolean addGroupObjects = true;  // not sure about this...
        if (addGroupObjects) {
            if (log.isDebugEnabled())
                log.debug("processGroupName: OIDs from group '" + mbean.getName());
            List objectList = (List) mbean.getAttribCollection();
            processObjectList(objectList, mibObjectList);
        } else {
            if (log.isDebugEnabled())
                log.debug("processGroupName: OIDs from group '" + mbean.getName());
        }
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
