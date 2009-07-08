/*******************************************************************************
 * This file is part of the OpenNMS(R) Application.
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * Copyright (C) 2009 The OpenNMS Group, Inc.  All rights reserved.
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

package org.opennms.web.notification.filter;

import org.opennms.web.filter.EqualsFilter;
import org.opennms.web.filter.SQLType;

/** Encapsulates all node filtering functionality. */
public class NodeFilter extends EqualsFilter<Integer> {
    public static final String TYPE = "node";

    //protected int nodeId;

    public NodeFilter(int nodeId) {
        super(TYPE, SQLType.INT, "NODEID", "node.id", nodeId);
        //this.nodeId = nodeId;
    }

//    public String getSql() {
//        return (" NODEID=" + this.nodeId);
//    }
//    
//    public String getParamSql() {
//        return (" NODEID=?");
//    }
//    
//    public int bindParams(PreparedStatement ps, int parameterIndex) throws SQLException {
//    	ps.setInt(parameterIndex, this.nodeId);
//    	return 1;
//    }
//
//    public String getDescription() {
//        return (TYPE + "=" + this.nodeId);
//    }
//
//    public String getTextDescription() {
//        String nodeName = Integer.toString(this.nodeId);
//        try {
//            nodeName = NetworkElementFactory.getNodeLabel(this.nodeId);
//        } catch (SQLException e) {
//        }
//
//        return (TYPE + "=" + nodeName);
//    }
//
//    public String toString() {
//        return ("<NoticeFactory.NodeFilter: " + this.getDescription() + ">");
//    }
//
//    public int getNodeId() {
//        return (this.nodeId);
//    }
//
//    public boolean equals(Object obj) {
//        return (this.toString().equals(obj.toString()));
//    }
}