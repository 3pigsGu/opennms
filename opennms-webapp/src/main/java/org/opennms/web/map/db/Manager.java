//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2006 The OpenNMS Group, Inc.  All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
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
/*
 * Created on 4-gen-2005
 *
 */
package org.opennms.web.map.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import org.opennms.core.resource.Vault;
import org.opennms.web.asset.AssetModel;
import org.opennms.web.map.view.VElement;

/**
 * @author micmas
 * 
 */
public class Manager {
    private Connection connection = null;

    private boolean isStartedSession() {
        return (connection != null);
    }

    public void startSession() throws SQLException {
        connection = Vault.getDbConnection();
        connection.setAutoCommit(false);
        connection
                .setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
    }

    synchronized public void endSession() throws SQLException {
        if (!isStartedSession())
            throw new IllegalStateException("Call startSession() first.");
    	connection.commit();
        Vault.releaseDbConnection(connection);
        connection = null;
    }

    public synchronized void saveMaps(Map[] m) throws SQLException {
        if (!isStartedSession())
            throw new IllegalStateException("Call startSession() first.");

        for (int i = 0, n = m.length; i < n; i++) {
            saveMap(m[i]);
        }
    }

    /**
     * This use DB table Asset to get default icons on Db 
     * other implementation are possble
     * geticon from Sysoid as an example
     */
    
    public String getIconName(int elementId,String type) throws SQLException {
    	if (type.equals(VElement.MAP_TYPE )) return "map";
    	AssetModel model = new AssetModel();
    	String iconName = model.getAsset(elementId).getDisplayCategory().toLowerCase();
    	if(iconName==null || iconName.equals("")){
    		return "unspecified";
    	}
    	return iconName;
    }

    public synchronized void saveElements(Element[] e) throws SQLException {
        if (!isStartedSession())
            throw new IllegalStateException("Call startSession() first.");
        if(e!=null){
        	for (int i = 0, n = e.length; i < n; i++) {
        		saveElement(e[i]);
        	}
        }
    }

    public synchronized void saveElement(Element e) throws SQLException {
        if (!isStartedSession())
            throw new IllegalStateException("Call startSession() first.");

        final String sqlSelectQuery = "SELECT COUNT(*) FROM element WHERE elementid = ? AND MAPID = ? AND elementtype = ?";
        final String sqlInsertQuery = "INSERT INTO element (mapid, elementid, elementtype, elementlabel, elementicon, elementx, elementy) VALUES (?, ?, ?, ?, ?, ?, ?)";
        final String sqlUpdateQuery = "UPDATE element SET mapid = ?, elementid = ?, elementtype = ?, elementlabel = ?, elementicon = ?, elementx = ?, elementy = ? WHERE elementid = ? AND mapid = ? AND elementtype = ?";
        try {
            PreparedStatement statement = connection
                    .prepareStatement(sqlSelectQuery);
            statement.setInt(1, e.getId());
            statement.setInt(2, e.getMapId());
            statement.setString(3, e.getType());
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                statement.close();
                if (count == 0) {
                    statement = connection.prepareStatement(sqlInsertQuery);
                    statement.setInt(1, e.getMapId());
                    statement.setInt(2, e.getId());
                    statement.setString(3, e.getType());
                    statement.setString(4, e.getLabel());
                    statement.setString(5, e.getIcon());
                    statement.setInt(6, e.getX());
                    statement.setInt(7, e.getY());
                } else {
                    statement = connection.prepareStatement(sqlUpdateQuery);
                    statement.setInt(1, e.getMapId());
                    statement.setInt(2, e.getId());
                    statement.setString(3, e.getType());
                    statement.setString(4, e.getLabel());
                    statement.setString(5, e.getIcon());
                    statement.setInt(6, e.getX());
                    statement.setInt(7, e.getY());
                    statement.setInt(8, e.getId());
                    statement.setInt(9, e.getMapId());
                    statement.setString(10, e.getType());
                }
                // now count counts number of modified record
                count = statement.executeUpdate();
                rs.close();
                statement.close();
            }
        } catch (SQLException ex) {
            try {
                connection.rollback();
                Vault.releaseDbConnection(connection);
                throw ex;
            } catch (SQLException eex) {
                throw eex;
            }
        }
    }

    public synchronized void deleteElement(Element e) throws SQLException {
        if(e!=null){
        	deleteElement(e.getId(), e.getMapId(),e.getType());
        }
    }
    
    public synchronized void deleteElements(Element[] elems) throws SQLException {
    	if(elems!=null){
    		for(int i=0;i<elems.length;i++){
    			deleteElement(elems[i]);
    		}
    	}
    }

    public synchronized void deleteElementsOfMap(int id) throws SQLException {
        if (!isStartedSession())
            throw new IllegalStateException("Call startSession() first.");
        final String sqlDelete = "DELETE FROM element WHERE mapid = ?";

        try {
            PreparedStatement statement = connection
                    .prepareStatement(sqlDelete);
            statement.setInt(1, id);
            statement.execute();
            statement.close();
        } catch (SQLException e) {
            try {
                connection.rollback();
                Vault.releaseDbConnection(connection);
                throw e;
            } catch (SQLException ex) {
                throw ex;
            }
        }
    }    
    
    public synchronized void deleteElement(int id, int mapid, String type)
            throws SQLException {
        if (!isStartedSession())
            throw new IllegalStateException("Call startSession() first.");
        final String sqlDelete = "DELETE FROM element WHERE elementid = ? AND mapid = ? AND elementtype = ?";

        try {
            PreparedStatement statement = connection
                    .prepareStatement(sqlDelete);
            statement.setInt(1, id);
            statement.setInt(2, mapid);
            statement.setString(3, type);
            statement.execute();
            statement.close();
        } catch (SQLException e) {
            try {
                connection.rollback();
                Vault.releaseDbConnection(connection);
                throw e;
            } catch (SQLException ex) {
                throw ex;
            }
        }
    }

    public synchronized void deleteMap(Map m) throws SQLException {
        deleteMap(m.getId());
    }

    public synchronized int deleteMap(int id) throws SQLException {
        if (!isStartedSession())
            throw new IllegalStateException("Call startSession() first.");
        final String sqlDeleteMap = "DELETE FROM map WHERE mapid = ?";
        final String sqlDeleteElemMap = "DELETE FROM element WHERE elementid = ? AND elementtype = ?";
        int countDelete = 0;
        try {
            PreparedStatement statement = connection
                    .prepareStatement(sqlDeleteMap);
            statement.setInt(1, id);
            countDelete = statement.executeUpdate();
            statement.close();
            statement = connection.prepareStatement(sqlDeleteElemMap);
            statement.setInt(1, id);
            statement.setString(2, Element.MAP_TYPE);
            statement.executeUpdate();
            statement.close();
            return countDelete;
        } catch (SQLException e) {
            try {
                connection.rollback();
                Vault.releaseDbConnection(connection);
                throw e;
            } catch (SQLException ex) {
                throw ex;
            }
        } 
    }

    public synchronized void saveMap(Map m) throws SQLException {
        if (!isStartedSession())
            throw new IllegalStateException("Call startSession() first.");
        
        final String sqlGetCurrentTimestamp = "SELECT CURRENT_TIMESTAMP";
        final String sqlGetMapNxtId = "SELECT nextval('mapnxtid')";
        final String sqlInsertQuery = "INSERT INTO map (mapid, mapname, mapbackground, mapowner, mapcreatetime, mapaccess, userlastmodifies, lastmodifiedtime, mapscale, mapxoffset, mapyoffset, maptype, mapwidth, mapheight) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        final String sqlUpdateQuery = "UPDATE map SET mapname = ?, mapbackground = ?, mapowner = ?, mapaccess = ?, userlastmodifies = ?, lastmodifiedtime = ?, mapscale = ?, mapxoffset = ?, mapyoffset = ?, maptype = ? , mapwidth = ?, mapheight = ? WHERE mapid = ?";
        Timestamp currentTimestamp = null;
        int nxtid = 0;
        
        int count = -1;
        
        try {
            Statement stmtCT = connection.createStatement();
            ResultSet rs = stmtCT.executeQuery(sqlGetCurrentTimestamp);
            if (rs.next()) {
                currentTimestamp = rs.getTimestamp(1);
                PreparedStatement statement;
                if (m.isNew()) {
                    Statement stmtID = connection.createStatement();
                    ResultSet rsStmt = stmtID.executeQuery(sqlGetMapNxtId);
                    if (rsStmt.next()) {
                        nxtid = rsStmt.getInt(1);
                    }
                    rsStmt.close();
                    stmtID.close();
                    
                    statement = connection.prepareStatement(sqlInsertQuery);
                    statement.setInt(1, nxtid);
                    statement.setString(2, m.getName());
                    statement.setString(3, m.getBackground());
                    statement.setString(4, m.getOwner());
                    statement.setTimestamp(5, currentTimestamp);
                    statement.setString(6, m.getAccessMode());
                    statement.setString(7, m.getUserLastModifies());
                    statement.setTimestamp(8, currentTimestamp);
                    statement.setDouble(9, m.getScale());
                    statement.setInt(10, m.getOffsetX());
                    statement.setInt(11, m.getOffsetY());
                    statement.setString(12, m.getType());
                    statement.setInt(13,m.getWidth());
                    statement.setInt(14,m.getHeight());
                } else {
                    statement = connection.prepareStatement(sqlUpdateQuery);
                    statement.setString(1, m.getName());
                    statement.setString(2, m.getBackground());
                    statement.setString(3, m.getOwner());
                    statement.setString(4, m.getAccessMode());
                    statement.setString(5, m.getUserLastModifies());
                    statement.setTimestamp(6, currentTimestamp);
                    statement.setDouble(7, m.getScale());
                    statement.setInt(8, m.getOffsetX());
                    statement.setInt(9, m.getOffsetY());
                    statement.setString(10, m.getType());
                    statement.setInt(11,m.getWidth());
                    statement.setInt(12,m.getHeight());
                    statement.setInt(13, m.getId());
                }
                count = statement.executeUpdate();

                statement.close();
            }
            rs.close();
            stmtCT.close();
        } catch (SQLException ex) {
            try {
                connection.rollback();
                Vault.releaseDbConnection(connection);
            } catch (SQLException eex) {
                throw eex;
            }
            throw ex;
        }
        
        if (count == 0)
            throw new SQLException("Called saveMap() on deleted map;");        
        
        if (m.isNew()) {
            m.setId(nxtid);
            m.setCreateTime(currentTimestamp);
            m.setAsNew(false);
        }
        m.setLastModifiedTime(currentTimestamp);
    }
}
