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
// 02 Oct 2005: Use File.separator to join file path components instead of "/". -- DJ Gregor
// 12 Nov 2002: Added response time reports to webUI.
//
// Original coda base Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
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

package org.opennms.web.response;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.opennms.core.resource.Vault;
import org.opennms.core.utils.IntSet;
import org.opennms.netmgt.utils.IfLabel;
import org.opennms.netmgt.utils.RrdFileConstants;
import org.opennms.web.Util;
import org.opennms.web.graph.PrefabGraph;
import org.opennms.web.graph.GraphModel;
import org.opennms.web.graph.GraphModelAbstract;
import org.opennms.web.performance.GraphAttribute;
import org.opennms.web.performance.GraphResource;
import org.opennms.web.performance.GraphResourceType;
import org.opennms.web.performance.ResponseTimeGraphResourceType;

/**
 * Encapsulates all SNMP performance reporting for the web user interface.
 * 
 * @author <a href="mailto:tarus@opennms.org">Tarus Balog </a>
 * @author <a href="mailto:seth@opennms.org">Seth Leger </a>
 * @author <a href="mailto:larry@opennms.org">Lawrence Karnowski </a>
 * @author <a href="http://www.opennms.org">OpenNMS </a>
 */
public class ResponseTimeModel extends GraphModelAbstract {
    public static final String RRDTOOL_GRAPH_PROPERTIES_FILENAME =
		File.separator + "etc" + File.separator + "response-graph.properties";
    private GraphResourceType m_resourceType =
        new ResponseTimeGraphResourceType(this);


    /**
     * Create a new instance.
     * 
     * @param homeDir
     *            the OpenNMS home directory, see {@link Vault#getHomeDir
     *            Vault.getHomeDir}.
     */
    public ResponseTimeModel(String homeDir) throws IOException {
        //loadProperties(homeDir, RRDTOOL_GRAPH_PROPERTIES_FILENAME);
    }
    
    public ResponseTimeModel() throws IOException {
    }

    // XXX parameters nodeId and includeNodeQueries are not used
    public List getDataSourceList(String nodeId, String intf,
				  boolean includeNodeQueries) {
        if (nodeId == null || intf == null) {
            throw new IllegalArgumentException("Cannot take null parameters.");
        }

        File intfDir = new File(getRrdDirectory(), intf);

	return getDataSourcesInDirectory(intfDir);
    }

    /**
     * Returns a list of data structures representing the nodes that have SNMP
     * performance data collected.
     * 
     * <p>
     * First the list of RRD files is collected. From those filenames, the IP
     * address is extracted from each. A list of unique IP addresses is created,
     * discarding the duplicates. At the same time, a mapping of unique IP
     * address to RRD files is created. Then a database call is made to
     * determine the node identifier and human-readable label for each node
     * containing the IP addresses. From that list, an array of data structures,
     * <code>QueryableNode</code>s, are created.
     * </p>
     */
    public QueryableNode[] getQueryableNodes() throws SQLException {
        // Get all of the numeric directory names in the RRD directory; these
        // are the nodeids of the nodes that have performance data
        File[] intDirs = getRrdDirectory().listFiles(RrdFileConstants.INTERFACE_DIRECTORY_FILTER);

        if (intDirs == null || intDirs.length == 0) {
	    return new QueryableNode[0];
	}

	List nodeList = new LinkedList();

	// create a set to test ipAddrs against.
	Set queryableIpAddrs = new HashSet(intDirs.length);
	for (int i = 0; i < intDirs.length; i++) {
	    String ipAddr = intDirs[i].getName();
	    queryableIpAddrs.add(ipAddr);
	}

	// create the main stem of the select statement
	StringBuffer select = new StringBuffer("SELECT DISTINCT ipinterface.ipAddr, ipinterface.nodeid, node.nodeLabel FROM node, ipinterface WHERE node.nodetype != 'D' AND ipinterface.nodeid=node.nodeid AND ipinterface.ismanaged != 'D' ORDER BY node.nodeLabel");

	Connection conn = Vault.getDbConnection();

	Statement stmt = null;
	ResultSet rs = null;
	try {
	    stmt = conn.createStatement();
	    rs = stmt.executeQuery(select.toString());

	    IntSet nodesAdded = new IntSet();
	    while (rs.next()) {
		String ipAddr = rs.getString("ipAddr");
		int nodeId = rs.getInt("nodeid");

		if (queryableIpAddrs.contains(ipAddr)
		    && !nodesAdded.contains(nodeId)) {
		    String nodeLabel = rs.getString("nodeLabel");
		    nodeList.add(new QueryableNode(nodeId, nodeLabel));
		    nodesAdded.add(nodeId);
		}
	    }
	} finally {
	    if (rs != null)
		rs.close();
	    if (stmt != null)
		stmt.close();
	    Vault.releaseDbConnection(conn);
	}

	return (QueryableNode[])
	    nodeList.toArray(new QueryableNode[nodeList.size()]);
    }

    public ArrayList getQueryableInterfacesForNode(int nodeId)
	throws SQLException {
        return getQueryableInterfacesForNode(String.valueOf(nodeId));
    }

    public ArrayList<String> getQueryableInterfacesForNode(String nodeId)
	throws SQLException {
        if (nodeId == null) {
            throw new IllegalArgumentException("Cannot take null parameters.");
        }

        // create the main stem of the select statement
        StringBuffer select = new StringBuffer("SELECT DISTINCT ipaddr FROM ipinterface WHERE nodeid=");

        select.append(nodeId);

        // close the select
        select.append(" ORDER BY ipaddr");

        Connection conn = Vault.getDbConnection();
        ArrayList<String> intfs = new ArrayList<String>();

        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(select.toString());

            while (rs.next()) {
                String ipAddr = rs.getString("ipaddr");
                File intfDir = new File(getRrdDirectory(), ipAddr);

                if (intfDir.isDirectory()) {
                    intfs.add(ipAddr);
                }
            }
            rs.close();
            stmt.close();
        } finally {
            Vault.releaseDbConnection(conn);
        }

        return intfs;
    }

    public boolean isQueryableNode(int nodeId) throws SQLException {
        return isQueryableNode(String.valueOf(nodeId));
    }

    public boolean isQueryableNode(String nodeId) throws SQLException {
        if (nodeId == null) {
            throw new IllegalArgumentException("Cannot take null parameters.");
        }
        ArrayList intfList = getQueryableInterfacesForNode(nodeId);
        if (intfList != null && !intfList.isEmpty()) {
            return true;
        }
        return false;
    }

    public boolean isQueryableInterface(String ipAddr) {
        if (ipAddr == null) {
            throw new IllegalArgumentException("Cannot take null parameters.");
        }

	File intfDir = new File(getRrdDirectory(), ipAddr);

	return RrdFileConstants.isValidRRDLatencyDir(intfDir);
    }

    public boolean encodeNodeIdInRRDParm() {
        return false;
    }

    public String getType() {
        return "response";
    }

    /**
     * Return a human-readable description (usually an IP address or hostname)
     * for the interface given.
     */
    public String getHumanReadableNameForIfLabel(int nodeId, String ifLabel)
		throws SQLException {
	return getHumanReadableNameForIfLabel(nodeId, ifLabel, false);
    }

    public String getRelativePathForAttribute(String resourceType, String resourceParent, String resource, String attribute) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(resource);
        buffer.append(File.separator);
        buffer.append(attribute);
        buffer.append(RrdFileConstants.RRD_SUFFIX);
        return buffer.toString();

    }
    
    public PrefabGraph getQuery(String resourceType, String name) {
        return getQuery(name);
    }
    
    public GraphResourceType getResourceTypeByName(String name) {
        return m_resourceType;
    }
}
