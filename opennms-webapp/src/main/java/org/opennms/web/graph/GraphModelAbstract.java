package org.opennms.web.graph;

import org.apache.log4j.Category;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.opennms.core.utils.ThreadCategory;
import org.opennms.netmgt.utils.IfLabel;
import org.opennms.netmgt.utils.RrdFileConstants;
import org.opennms.web.Util;
import org.opennms.web.performance.GraphAttribute;
import org.opennms.web.performance.GraphResource;
import org.opennms.web.performance.GraphResourceType;
import org.springframework.orm.ObjectRetrievalFailureException;
import org.springframework.util.StringUtils;

public abstract class GraphModelAbstract implements GraphModel {
    private GraphDao m_dao;
    
    public void setPrefabGraphDao(GraphDao dao) {
        m_dao = dao;
    }
    
    public GraphDao getPrefabGraphDao() {
        return m_dao;
    }
    
    private void assertDaoSet() {
        if (m_dao == null) {
            throw new IllegalStateException("prefabGraphDao has not been set");
        }
    }
    
    public PrefabGraphType getPrefabGraphType() {
        assertDaoSet();
        PrefabGraphType type = m_dao.findByName(getType());
        if (type == null) {
            throw new IllegalArgumentException("Cannot find PrefabGraphType for "
                    + getType());
        }
        return type;
    }

    public PrefabGraph[] getQueries() {
        return getPrefabGraphType().getQueries();
    }
    
    public PrefabGraph getQuery(String name) {
        return getPrefabGraphType().getQuery(name);
    }

    public File getRrdDirectory() {
        return getPrefabGraphType().getRrdDirectory();
    }

    public String getDefaultReport() {
        return getPrefabGraphType().getDefaultReport();
    }
    

    public PrefabGraph[] getQueriesByResourceTypeAttributes(String resourceType,
            Set<GraphAttribute> attributes) {
        return getQueriesByResourceTypeAttributes(resourceType, attributes,
                                                  getQueries());
        
    }

    public PrefabGraph[] getQueriesByResourceTypeAttributes(String resourceType,
            Set<GraphAttribute> attributes, PrefabGraph[] availableQueries) {

        List<PrefabGraph> returnList = new LinkedList<PrefabGraph>();

        Set<String> availDataSourceList = new HashSet<String>(attributes.size());
        for (GraphAttribute attribute : attributes) {
            availDataSourceList.add(attribute.getName());
        }

        for (PrefabGraph query : availableQueries) {
            if (resourceType != null &&
                    !resourceType.equals(query.getType())) {
                if (log().isDebugEnabled()) {
                    log().debug("skipping " + query.getName() + " because its type \"" + query.getType() + "\" does not equal resourceType \"" + resourceType + "\"");
                }
                continue;
            }
            
            List<String> requiredList = Arrays.asList(query.getColumns());

            if (availDataSourceList.containsAll(requiredList)) {
                if (log().isDebugEnabled()) {
                    log().debug("adding " + query.getName() + " to query list");
                }
                returnList.add(query);
            } else {
                if (log().isDebugEnabled()) {
                    log().debug("not adding " + query.getName() + " to query list because required list of attributes (" + StringUtils.collectionToDelimitedString(requiredList, ", ") + ") is not in the list of attributes on the resource (" + StringUtils.collectionToDelimitedString(availDataSourceList, ", ")+ ")");
                }
            }
        }

        PrefabGraph[] availQueries = (PrefabGraph[])
        returnList.toArray(new PrefabGraph[returnList.size()]);

        return availQueries;
    }


    public PrefabGraph[] getQueries(int nodeId) {
        return getQueries(String.valueOf(nodeId));
    }

    public PrefabGraph[] getQueries(String nodeId) {
        if (nodeId == null) {
            throw new IllegalArgumentException("Cannot take null parameters.");
        }

        // create a temporary list of queries to return
        List returnList = new LinkedList();

        // get the full list of all possible queries
        PrefabGraph[] queries = getQueries();

        // get all the data sources supported by node
        List availDataSourceList = getDataSourceList(nodeId);

        // for each query, see if all the required data sources are available
        // in the available data source list, if so, add that query to the
        // returnList
        for (int i = 0; i < queries.length; i++) {
            List requiredList = Arrays.asList(queries[i].getColumns());

            if (availDataSourceList.containsAll(requiredList)) {
                returnList.add(queries[i]);
            }
        }

        // put the queries in returnList into an array
        PrefabGraph[] availQueries = (PrefabGraph[])
	    returnList.toArray(new PrefabGraph[returnList.size()]);

        return availQueries;
    }

    public PrefabGraph[] getQueries(int nodeId, String intf,
				    boolean includeNodeQueries) {
        boolean isNode = true;	    
        return getQueries(String.valueOf(nodeId), intf, includeNodeQueries, isNode);
    }

    public PrefabGraph[] getQueries(String nodeId, String intf,
				    boolean includeNodeQueries) {
        boolean isNode = true;
        return getQueries(nodeId, intf, includeNodeQueries, isNode);
    }

    public PrefabGraph[] getQueries(String nodeOrDomain, String intf,
                                    boolean includeNodeQueries, boolean isNode) { 
        if (nodeOrDomain == null || intf == null) {
            throw new IllegalArgumentException("Cannot take null parameters.");
        }

        // create a temporary list of queries to return
        List returnList = new LinkedList();

        // get the full list of all possible queries
        PrefabGraph[] queries = getQueries();

        // get all the data sources supported by this interface (and possibly
        // node)
        List availDataSourceList = getDataSourceList(nodeOrDomain, intf,
						     includeNodeQueries, isNode);

        // for each query, see if all the required data sources are available
        // in the available data source list, if so, add that query to the
        // returnList
        for (int i = 0; i < queries.length; i++) {
            List requiredList = Arrays.asList(queries[i].getColumns());

            if (availDataSourceList.containsAll(requiredList)) {
                if(isNode || queries[i].getExternalValues().length == 0) {
                    returnList.add(queries[i]);
                }
            }
        }

        // put the queries in returnList into an array
        PrefabGraph[] availQueries = (PrefabGraph[])
	    returnList.toArray(new PrefabGraph[returnList.size()]);

        return availQueries;
    }

    public PrefabGraph[] getQueriesForDomain(String domain, String intf) {
        boolean includeNodeQueries = false;
        boolean isNode = false;
        return getQueries(domain, intf, includeNodeQueries, isNode);
    }
    
    public PrefabGraph[] getAllQueries(String nodeOrDomain, boolean includeNodeQueries, boolean isNode) {
        if (nodeOrDomain == null) {
            throw new IllegalArgumentException("Cannot take null parameters.");
        }
        Category log = ThreadCategory.getInstance(this.getClass());
        HashMap queryCount = new HashMap();
        String mostFreqQuery = "none";
        int mostFreqCount = 0;

        // get the full list of all possible queries
        PrefabGraph[] queries = getQueries();

        File nodeOrDomainDir = new File(getRrdDirectory(), nodeOrDomain);

        // get each interface directory
        File[] intfDir = nodeOrDomainDir.listFiles(RrdFileConstants.INTERFACE_DIRECTORY_FILTER);

        // for each interface directory, get all available data sources
	if(intfDir != null) {
            for (int j = 0; j < intfDir.length; j++) {
                String dirName = intfDir[j].getName();

                //

                List availDataSourceList = getDataSourceList(nodeOrDomain, dirName, includeNodeQueries, isNode);

                // for each query, see if all the required data sources are available
                // in the available data source list, if so, add that query to the
                // queryCount HashMap
                //
                for (int i = 0; i < queries.length; i++) {
	            String qname = queries[i].getName();
                    List requiredList = Arrays.asList(queries[i].getColumns());

                    if (availDataSourceList.containsAll(requiredList)) {
                        if(isNode || queries[i].getExternalValues().length == 0) {
                            if(queryCount.containsKey(queries[i])) {
                                int x = ( (Integer) queryCount.get(queries[i])).intValue();
                                queryCount.put(queries[i], new Integer(x++));
                            } else {
                                queryCount.put(queries[i], new Integer(1));
                            }
                            if(( (Integer) queryCount.get(queries[i])).intValue() > mostFreqCount) {
                                mostFreqCount = ( (Integer) queryCount.get(queries[i])).intValue();
                                mostFreqQuery = qname;
                            }
                        }
                    }
                }
            }	
        }	

        // put the queries in queryCount keySet into an array
        PrefabGraph[] availQueries = (PrefabGraph[]) queryCount.keySet().toArray(new PrefabGraph[queryCount.size() + 1]);

        // determine working default graph and copy to end of array. It will be pulled
        // off again by the calling method.
        for(int i = 0; i < queryCount.size(); i++ ) {
            if(availQueries[i].getName().equals(getDefaultReport())) {
                availQueries[queryCount.size()] = availQueries[i];
                break;
            }
            if(availQueries[i].getName().equals(mostFreqQuery)) {
                availQueries[queryCount.size()] = availQueries[i];
            }
        }
        if (log.isDebugEnabled() && queryCount.size() > 0) {
            if(availQueries[queryCount.size()].getName().equals(getDefaultReport())) {
                log.debug("Found default report " + getDefaultReport() + " in list of available queries");
            } else {
                log.debug("Default report " + getDefaultReport() + " not found in list of available queries. Using most frequent query " + mostFreqQuery + " as the default.");
            }
        }

        return availQueries;
    }	

    // Check to see whether any of the public String[] getDataSources listed below are used
    // Remove them if not.
    //
    public String[] getDataSources(int nodeId) {
        return getDataSources(String.valueOf(nodeId));
    }

    public String[] getDataSources(String nodeId) {
        List dataSourceList = getDataSourceList(String.valueOf(nodeId));
        String[] dataSources = (String[])
	    dataSourceList.toArray(new String[dataSourceList.size()]);

        return dataSources;
    }

    public String[] getDataSources(int nodeId, String intf,
				   boolean includeNodeQueries) {
        return getDataSources(String.valueOf(nodeId), intf,
			      includeNodeQueries);
    }

    public String[] getDataSources(String nodeId, String intf,
				   boolean includeNodeQueries) {
        List dataSourceList = getDataSourceList(String.valueOf(nodeId), intf,
						includeNodeQueries);
        String[] dataSources = (String[])
	    dataSourceList.toArray(new String[dataSourceList.size()]);

        return dataSources;
    }

    public List getDataSourceList(int nodeId) {
        return getDataSourceList(String.valueOf(nodeId));
    }


    public List<String> getDataSourceList(String nodeId) {
        if (nodeId == null) {
            throw new IllegalArgumentException("Cannot take null parameters.");
        }

        List<String> dataSources = new LinkedList<String>();
        File nodeDir = new File(getRrdDirectory(), nodeId);
        int suffixLength = RrdFileConstants.getRrdSuffix().length();

        // get the node data sources
        File[] nodeFiles =
	    nodeDir.listFiles(RrdFileConstants.RRD_FILENAME_FILTER);
        if (nodeFiles != null) {
            for (int i = 0; i < nodeFiles.length; i++) {
                String fileName = nodeFiles[i].getName();
                String dsName =
		    fileName.substring(0, fileName.length() - suffixLength);

                dataSources.add(dsName);
            }
        }

        return dataSources;
    }

    public List getDataSourceList(int nodeId, String intf,
                                  boolean includeNodeQueries) {
        boolean isNode = true;
        return getDataSourceList(String.valueOf(nodeId), intf, includeNodeQueries, isNode);
    }

    public List getDataSourceList(String nodeIdOrDomain, String intf, boolean includeNodeQueries, boolean isNode) {
        Category log = ThreadCategory.getInstance(this.getClass());
        if (nodeIdOrDomain == null || intf == null) {
            throw new IllegalArgumentException("Cannot take null parameters.");
        }

        List dataSources = new ArrayList();

        File nodeOrDomainDir = new File(getRrdDirectory(), nodeIdOrDomain);
        File intfDir = new File(nodeOrDomainDir, intf);

        int suffixLength = RrdFileConstants.getRrdSuffix().length();

        // get the node data sources
        if (includeNodeQueries && isNode) {
            dataSources.addAll(this.getDataSourceList(nodeIdOrDomain));
        }

        // get the interface data sources
        File[] intfFiles = intfDir.listFiles(RrdFileConstants.RRD_FILENAME_FILTER);
	if (intfFiles == null) {
            // See if perhaps this is a response graph rather than a performance graph
	    // TODO - Do this a better way. Should distinguish response from performance
	    // coming in.
	    log.debug("getDataSourceList: No interface files. Looking for performance data");
            intfDir = new File(getRrdDirectory(), intf);
	    intfFiles = intfDir.listFiles(RrdFileConstants.RRD_FILENAME_FILTER);
        }

        if (intfFiles != null) {
            for (int i = 0; i < intfFiles.length; i++) {
                String fileName = intfFiles[i].getName();
                String dsName = fileName.substring(0, fileName.length() - suffixLength);

                dataSources.add(dsName);
	        log.debug("getDataSourceList: adding " + dsName);
            }
        }

        return dataSources;
    }

    /**
     * Return a human-readable description (usually an IP address or hostname)
     * for the interface given.
     */
    protected String
	getHumanReadableNameForIfLabel(int nodeId,
				       String ifLabel,
				       boolean isPerformanceModel)
		throws SQLException {
        if (nodeId < 1) {
            throw new IllegalArgumentException("Illegal nodeid encountered "
					       + "when looking for performance "
					       + "information: \"" +
					       String.valueOf(nodeId) + "\"");
        }
        if (ifLabel == null) {
            throw new IllegalArgumentException("Cannot take null parameters.");
        }

        // Retrieve the extended information for this nodeid/ifLabel pair
        Map intfMap = IfLabel.getInterfaceInfoFromIfLabel(nodeId, ifLabel);

        StringBuffer descr = new StringBuffer();

        /*
	 * If there is no extended information, the ifLabel is not associated
         *  with a current SNMP interface.
	 */
        if (intfMap.size() < 1) {
            descr.append(ifLabel);
	    if (isPerformanceModel) {
		descr.append(" (Not Currently Updated)");
	    }
        } else {
	    // Otherwise, add the extended information to the description
            StringBuffer parenString = new StringBuffer();
            
            if (isPerformanceModel && intfMap.get("snmpifalias") != null) {
                parenString.append((String) intfMap.get("snmpifalias"));
            }
            if ((intfMap.get("ipaddr") != null)
		&& !((String) intfMap.get("ipaddr")).equals("0.0.0.0")) {
                String ipaddr = (String) intfMap.get("ipaddr");
		if (parenString.length() > 0) {
		    parenString.append(", ");
		}
		parenString.append(ipaddr);
            }
            if ((intfMap.get("snmpifspeed") != null)
		&& (Long.parseLong((String) intfMap.get("snmpifspeed"))
		    != 0)) {
		long ifSpeed =
		    Long.parseLong((String) intfMap.get("snmpifspeed"));
		String speed = Util.getHumanReadableIfSpeed(ifSpeed);
		if (parenString.length() > 0) {
		    parenString.append(", ");
		}
		parenString.append(speed);
            }

            if (intfMap.get("snmpifname") != null) {
                descr.append((String) intfMap.get("snmpifname"));
            } else if (intfMap.get("snmpifdescr") != null) {
                descr.append((String) intfMap.get("snmpifdescr"));
            } else {
                /*
		 * Should never reach this point, since ifLabel is based on
		 * the values of ifName and ifDescr but better safe than sorry.
		 */
                descr.append(ifLabel);
            }

            /* Add the extended information in parenthesis after the ifLabel,
	     * if such information was found.
	     */
	    if (parenString.length() > 0) {
		descr.append(" (");
		descr.append(parenString);
		descr.append(")");
	    }
        }

        return Util.htmlify(descr.toString());
    }


    public abstract List getDataSourceList(String nodeId, String intf,
					   boolean includeNodeQueries);

    public List<String> getDataSourcesInDirectory(File directory) {
        int suffixLength = RrdFileConstants.getRrdSuffix().length();

        // get the interface data sources
        File[] files =
	    directory.listFiles(RrdFileConstants.RRD_FILENAME_FILTER);

	ArrayList<String> dataSources = new ArrayList<String>(files.length);
        for (int i = 0; i < files.length; i++) {
            String fileName = files[i].getName();
            String dsName =
		fileName.substring(0, fileName.length() - suffixLength);

            dataSources.add(dsName);
        }

	return dataSources;
    }
    
    
    public List<GraphResource> getResourcesForNodeResourceType(int nodeId, String resourceTypeName) {
        GraphResourceType resourceType = getResourceTypeByName(resourceTypeName);

        return resourceType.getResourcesForNode(nodeId);
    }
    
    public List<GraphResource> getResourcesForDomainResourceType(String domain, String resourceTypeName) {
        GraphResourceType resourceType = getResourceTypeByName(resourceTypeName);

        return resourceType.getResourcesForDomain(domain);
    }
    
    public GraphResource getResourceForNodeResourceResourceType(int nodeId, String resourceName, String resourceTypeName) {
        List<GraphResource> resources = getResourcesForNodeResourceType(nodeId, resourceTypeName);
        
        GraphResource resource = null;
        for (GraphResource a : resources) {
            if (resourceName.equals(a.getName())) {
                resource = a;
            }
        }
        
        if (resource == null) {
            String message = "Resource '" + resourceName + "' is not on node " + nodeId + " for resource type " + resourceTypeName;
            log().info(message); 
            throw new ObjectRetrievalFailureException(GraphResource.class, resourceName, message, null);
        }
        
        return resource;
    }
    
    public GraphResource getResourceForDomainResourceResourceType(String domain, String resourceName, String resourceTypeName) {
        List<GraphResource> resources = getResourcesForDomainResourceType(domain, resourceTypeName);
        
        GraphResource resource = null;
        for (GraphResource a : resources) {
            if (resourceName.equals(a.getName())) {
                resource = a;
            }
        }
        
        if (resource == null) {
            log().info("Resource of resource type '" + resourceTypeName + "' is not on domain " + domain);
            throw new ObjectRetrievalFailureException(GraphResourceType.class, resourceName, "Resource of resource type '" + resourceTypeName + "' is not on domain " + domain, null);
        }
        
        return resource;
    }
    

    public Category log() {
        return ThreadCategory.getInstance();
    }

    /** Convenient data structure for storing nodes with RRDs available. */
    public static class QueryableNode {
        private int m_nodeId;

        private String m_nodeLabel;

	public QueryableNode(int nodeId, String nodeLabel) {
	    m_nodeId = nodeId;
	    m_nodeLabel = nodeLabel;
	}

	public int getNodeId() {
	    return m_nodeId;
	}

	public String getNodeLabel() {
	    return m_nodeLabel;
	}
    }

}
