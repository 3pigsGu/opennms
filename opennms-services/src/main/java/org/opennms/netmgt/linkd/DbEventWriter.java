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
 * Created on 8-lug-2005
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

package org.opennms.netmgt.linkd;

import java.lang.reflect.UndeclaredThrowableException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.log4j.Category;
import org.opennms.core.utils.ThreadCategory;

import org.opennms.netmgt.capsd.snmp.SnmpTableEntry;
import org.opennms.netmgt.config.DataSourceFactory;

import org.opennms.netmgt.linkd.SnmpCollection.Vlan;
import org.opennms.netmgt.linkd.snmp.CdpCacheTableEntry;
import org.opennms.netmgt.linkd.snmp.Dot1dBaseGroup;
import org.opennms.netmgt.linkd.snmp.Dot1dBasePortTableEntry;
import org.opennms.netmgt.linkd.snmp.Dot1dStpGroup;
import org.opennms.netmgt.linkd.snmp.Dot1dStpPortTableEntry;
import org.opennms.netmgt.linkd.snmp.Dot1dTpFdbTableEntry;
import org.opennms.netmgt.linkd.snmp.IpNetToMediaTableEntry;
import org.opennms.netmgt.linkd.snmp.IpRouteTableEntry;
import org.opennms.netmgt.linkd.snmp.QBridgeDot1dTpFdbTableEntry;
import org.opennms.netmgt.linkd.snmp.VlanCollectorEntry;

/**
 * <P>
 * This class is used to store informations owned by SnmpCollection
 * and DiscoveryLink Classes in DB. 
 * When saving Snmp Collection it populate Bean LinkableNode
 * with information for DiscoveryLink.
 * It performes data test for DiscoveryLink.
 * Also take correct action on DB tables in case node is deleted
 * service SNMP is discovered, service SNMP is Lost and Regained
 * </P>
 *
 * @author antonio
 * 
 *  
 * */

public class DbEventWriter implements Runnable {

	static final char ACTION_UPTODATE = 'N';

	static final char ACTION_DELETE = 'D';

	static final char ACTION_STORE = 'S';
	
	static final char ACTION_STORE_LINKS = 'A';

	/**
	 * The status of the info in FDB table entry The meanings of the value is
	 * other(1): none of the following. This would include the case where some
	 * other MIB object (not the corresponding instance of dot1dTpFdbPort, nor
	 * an entry in the dot1dStaticTable) is being used to determine if and how
	 * frames addressed to the value of the corresponding instance of
	 * dot1dTpFdbAddress are being forwarded.
	 */
	private static final int SNMP_DOT1D_FDB_STATUS_OTHER = 1;

	/**
	 * The status of the info in FDB table entry The status of this entry. The
	 * meanings of the values are: invalid(2) : this entry is not longer valid
	 * (e.g., it was learned but has since aged-out), but has not yet been
	 * flushed from the table.
	 */
	private static final int SNMP_DOT1D_FDB_STATUS_INVALID = 2;

	/**
	 * The status of the info in FDB table entry The status of this entry. The
	 * meanings of the values are: learned(3) : the value of the corresponding
	 * instance of dot1dTpFdbPort was learned, and is being used.
	 */
	private static final int SNMP_DOT1D_FDB_STATUS_LEARNED = 3;

	/**
	 * The status of the info in FDB table entry The status of this entry. The
	 * meanings of the values are: self(4) : the value of the corresponding
	 * instance of dot1dTpFdbAddress represents one of the bridge's addresses.
	 * The corresponding instance of dot1dTpFdbPort indicates which of the
	 * bridge's ports has this address.
	 */
	private static final int SNMP_DOT1D_FDB_STATUS_SELF = 4;

	/**
	 * mgmt(5) : the value of the corresponding instance of dot1dTpFdbAddress is
	 * also the value of an existing instance of dot1dStaticAddress.
	 */
	private static final int SNMP_DOT1D_FDB_STATUS_MGMT = 5;

	/**
	 * the int that indicated cdp address type
	 * 
	 */
	
	private static final int CDP_ADDRESS_TYPE_IP_ADDRESS = 1;
	LinkableNode m_node;
	
	SnmpCollection m_snmpcoll;

	DiscoveryLink m_discovery;

	int m_nodeId;

	private static final String SQL_GET_NODEID = "SELECT node.nodeid FROM node LEFT JOIN ipinterface ON node.nodeid = ipinterface.nodeid WHERE nodetype = 'A' AND ipaddr = ?";

	private static final String SQL_GET_NODEID__IFINDEX_MASK = "SELECT node.nodeid,snmpinterface.snmpifindex,snmpinterface.snmpipadentnetmask FROM node LEFT JOIN snmpinterface ON node.nodeid = snmpinterface.nodeid WHERE nodetype = 'A' AND ipaddr = ?";

	private static final String SQL_GET_NODEID_IFINDEX_IPINT = "SELECT node.nodeid,ipinterface.ifindex FROM node LEFT JOIN ipinterface ON node.nodeid = ipinterface.nodeid WHERE nodetype = 'A' AND ipaddr = ?";

	private static final String SQL_UPDATE_DATAINTERFACE = "UPDATE datalinkinterface set status = 'N'  WHERE lastpolltime < ? AND status = 'A'";
	
	private static final String SQL_UPDATE_ATINTERFACE = "UPDATE atinterface set status = 'N'  WHERE sourcenodeid = ? AND lastpolltime < ? AND status = 'A'";

	private static final String SQL_UPDATE_IPROUTEINTERFACE = "UPDATE iprouteinterface set status = 'N'  WHERE nodeid = ? AND lastpolltime < ? AND status = 'A'";

	private static final String SQL_UPDATE_STPNODE = "UPDATE stpnode set status = 'N'  WHERE nodeid = ? AND lastpolltime < ? AND status = 'A'";

	private static final String SQL_UPDATE_STPINTERFACE = "UPDATE stpinterface set status = 'N'  WHERE nodeid = ? AND lastpolltime < ? AND status = 'A'";

	private static final String SQL_UPDATE_ATINTERFACE_STATUS = "UPDATE atinterface set status = ?  WHERE sourcenodeid = ? OR nodeid = ?";

	private static final String SQL_UPDATE_IPROUTEINTERFACE_STATUS = "UPDATE iprouteinterface set status = ? WHERE nodeid = ? ";

	private static final String SQL_UPDATE_STPNODE_STATUS = "UPDATE stpnode set status = ?  WHERE nodeid = ? ";

	private static final String SQL_UPDATE_STPINTERFACE_STATUS = "UPDATE stpinterface set status = ? WHERE nodeid = ? ";

	private static final String SQL_UPDATE_DATALINKINTERFACE_STATUS = "UPDATE datalinkinterface set status = ? WHERE nodeid = ? OR nodeparentid = ? ";

	private static final String SQL_GET_NODEID_IFINDEX = "SELECT atinterface.nodeid, snmpinterface.snmpifindex from atinterface left JOIN snmpinterface ON atinterface.nodeid = snmpinterface.nodeid AND atinterface.ipaddr = snmpinterface.ipaddr WHERE atphysaddr = ? AND status = 'A'";
	
	private static final String SQL_GET_SNMPIFTYPE = "SELECT snmpiftype FROM snmpinterface WHERE nodeid = ? AND snmpifindex = ?";

	private static final String SQL_GET_IFINDEX_SNMPINTERFACE_NAME = "SELECT snmpifindex FROM snmpinterface WHERE nodeid = ? AND (snmpifname = ? OR snmpifdescr = ?) ";
	
	private static final String SQL_GET_SNMPPHYSADDR_SNMPINTERFACE = "SELECT snmpphysaddr FROM snmpinterface WHERE nodeid = ? AND  snmpphysaddr <> ''";

	private char action = ACTION_STORE;
	
	/**
	 * A boolean used to decide if performs autodiscovery
	 */

	private boolean autoDiscovery = false;

	/**
	 * @param m_snmpcoll
	 */

	public DbEventWriter(int nodeid, SnmpCollection m_snmpcoll) {

		super();
		this.m_nodeId = nodeid;
		this.m_node = new LinkableNode(nodeid,m_snmpcoll.getTarget().getHostAddress());
		this.m_snmpcoll = m_snmpcoll;

	}

	/**
	 * 
	 * @param nodeid
	 * @param action
	 */
	public DbEventWriter(int nodeid, char action) {

		super();
		m_node = null;
		this.m_nodeId = nodeid;
		this.action = action;
	}

	public DbEventWriter (DiscoveryLink discoverLink) {
		m_node = null;
		m_discovery = discoverLink;
		this.action = ACTION_STORE_LINKS;
	}

	public void run() {

		Category log = ThreadCategory.getInstance(getClass());
		Connection dbConn = null;
		Timestamp now = new Timestamp(System.currentTimeMillis());

		try {
			dbConn = DataSourceFactory.getInstance().getConnection();
			if (log.isDebugEnabled()) {
				log.debug("run: Storing information into database");
			}
			if (action == ACTION_STORE)
				storeSnmpCollection(dbConn, now);
			else if (action == ACTION_UPTODATE || action == ACTION_DELETE)
				update(dbConn, action);
			else if (action == ACTION_STORE_LINKS)
				storeDiscoveryLink(dbConn,now);
			else
				log.warn("Unknown action: " + action + " . Exiting");
		} catch (SQLException sqlE) {
			log
					.fatal(
							"SQL Exception while syncing node object with database information.",
							sqlE);
			throw new UndeclaredThrowableException(sqlE);
		} catch (Throwable t) {
			log
					.fatal(
							"Unknown error while syncing node object with database information.",
							t);
			throw new UndeclaredThrowableException(t);
		} finally {
			try {
				if (dbConn != null) {
					dbConn.close();
				}
			} catch (Exception e) {
				log
						.fatal(
								"Unknown error while closing database connection.",
								e);
			}
		}
	}

	/**
	 * 
	 * @param dbConn
	 * @param now
	 * @throws SQLException
	 */
	private void storeDiscoveryLink(Connection dbConn, Timestamp now) throws SQLException,
			UnknownHostException {

		Category log = ThreadCategory.getInstance(getClass());
		
		PreparedStatement stmt = null;
		ResultSet rs = null;

		NodeToNodeLink[] links = m_discovery.getLinks();
		
		if (log.isDebugEnabled()) {
			log.debug("storelink: Storing " + links.length + " NodeToNodeLink information into database");
		}
		for (int i=0; i <links.length; i++) {
			NodeToNodeLink lk = links[i];
			int nodeid = lk.getNodeId();
			int ifindex = lk.getIfindex();
			int nodeparentid = lk.getNodeparentid();
			int parentifindex = lk.getParentifindex();
			
			DbDataLinkInterfaceEntry dbentry = DbDataLinkInterfaceEntry.get(dbConn,
					nodeid, ifindex);
			if (dbentry == null) {
				// Create a new entry
				dbentry = DbDataLinkInterfaceEntry.create(nodeid, ifindex);
			}
			dbentry.updateNodeParentId(nodeparentid);
			dbentry.updateParentIfIndex(parentifindex);
			dbentry.updateStatus(DbDataLinkInterfaceEntry.STATUS_ACTIVE);
			dbentry.set_lastpolltime(now);

			dbentry.store(dbConn);

			// now parsing simmetrical and setting to D if necessary

			dbentry = DbDataLinkInterfaceEntry.get(dbConn, nodeparentid,
						parentifindex);

			if (dbentry != null) {
				if (dbentry.get_nodeparentid() == nodeid
						&& dbentry.get_parentifindex() == ifindex
						&& dbentry.get_status() != DbDataLinkInterfaceEntry.STATUS_DELETE) {
					dbentry.updateStatus(DbDataLinkInterfaceEntry.STATUS_DELETE);
					dbentry.store(dbConn);
				}
			}
		}
		
		MacToNodeLink[] linkmacs = m_discovery.getMacLinks();
		
		if (log.isDebugEnabled()) {
			log.debug("storelink: Storing " + linkmacs.length + " MacToNodeLink information into database");
		}
		for (int i = 0; i < linkmacs.length; i++) {
			MacToNodeLink lkm = linkmacs[i];
			String macaddr = lkm.getMacAddress();
			int nodeparentid = lkm.getNodeparentid();
			int parentifindex = lkm.getParentifindex();

			stmt = dbConn.prepareStatement(SQL_GET_NODEID_IFINDEX);

			stmt.setString(1, macaddr);

			
			rs = stmt.executeQuery();
			
			if (log.isDebugEnabled())
				log.debug("storelink: finding nodeid,ifindex on DB. Sql Statement "
					+ SQL_GET_NODEID_IFINDEX + " with mac address " + macaddr);

			if (!rs.next()) {
				rs.close();
				stmt.close();
				if (log.isDebugEnabled())
					log.debug("storelink: no nodeid found on DB for mac address "
						+ macaddr
						+ " on link. .... Skipping");
				continue;
			}

			// extract the values.
			//
			int ndx = 1;

			int nodeid = rs.getInt(ndx++);
			if (rs.wasNull()) {
				rs.close();
				stmt.close();
				if (log.isDebugEnabled())
					log.debug("storelink: no nodeid found on DB for mac address "
						+ macaddr
						+ " on link. .... Skipping");
				continue;
			}

			int ifindex = rs.getInt(ndx++);
			if (rs.wasNull()) {
				if (log.isDebugEnabled())
					log.debug("storelink: no ifindex found on DB for mac address "
						+ macaddr
						+ " on link.");
				ifindex = -1;
			}

			rs.close();
			stmt.close();

			DbDataLinkInterfaceEntry dbentry = DbDataLinkInterfaceEntry.get(dbConn,
					nodeid, ifindex);
			if (dbentry == null) {
				// Create a new entry
				dbentry = DbDataLinkInterfaceEntry.create(nodeid, ifindex);
			}
			dbentry.updateNodeParentId(nodeparentid);
			dbentry.updateParentIfIndex(parentifindex);
			dbentry.updateStatus(DbDataLinkInterfaceEntry.STATUS_ACTIVE);
			dbentry.set_lastpolltime(now);

			dbentry.store(dbConn);

			
		}
		
		stmt = dbConn.prepareStatement(SQL_UPDATE_DATAINTERFACE);
		stmt.setTimestamp(1, now);

		int i = stmt.executeUpdate();
		stmt.close();
		if (log.isDebugEnabled())
			log
					.debug("storelink: datalinkinterface - updated to NOT ACTIVE status "
							+ i + " rows ");



	}
			
	/**
	 * 
	 * @param dbConn
	 * @param now
	 * @throws SQLException
	 */
	private void storeSnmpCollection(Connection dbConn, Timestamp now) throws SQLException,
			UnknownHostException {

		Category log = ThreadCategory.getInstance(getClass());
//		Iterator ite = null;

		int nodeid = m_node.getNodeId();

		if (m_snmpcoll.hasIpNetToMediaTable()) {
			Iterator ite1 = m_snmpcoll.getIpNetToMediaTable().getEntries().iterator();
			if (log.isDebugEnabled())
				log
						.debug("store: saving IpNetToMediaTable to atinterface table in DB");
			// the AtInterfaces used by LinkableNode where to save info
			java.util.List<AtInterface> atInterfaces = new java.util.ArrayList<AtInterface>();
			while (ite1.hasNext()) {

				IpNetToMediaTableEntry ent = (IpNetToMediaTableEntry) ite1
						.next();
				
				int ifindex = ent.getIpNetToMediaIfIndex();
				
				if ( ifindex < 0 ) {
					log.warn("store: invalid ifindex " + ifindex);
					continue;
				}
				
				InetAddress ipaddress = ent.getIpNetToMediaNetAddress();

				if (ipaddress == null || ipaddress.isLoopbackAddress() || ipaddress.getHostAddress().equals("0.0.0.0")) {
					log.warn("store: ipNetToMedia invalid ip " + ipaddress.getHostAddress());
					continue;
				}

				String physAddr = ent.getIpNetToMediaPhysAddress();
				
				if ( physAddr == null || physAddr.equals("000000000000") || physAddr.equalsIgnoreCase("ffffffffffff")) {
					log.warn("store: ipNetToMedia invalid mac address " + physAddr
							+ " for ip " + ipaddress.getHostAddress());
					continue;
				}


				if (log.isDebugEnabled())
					log.debug("store: trying save ipNetToMedia info: ipaddr " + ipaddress.getHostName()
							+ " mac address " + physAddr + " ifindex "
							+ ifindex);

				// get an At interface but without setting mac address
				AtInterface at = getNodeidIfindexFromIp(dbConn, ipaddress);
				if (at == null) {
						log.warn("getNodeidIfindexFromIp: no nodeid found for ipaddress "
								+ ipaddress + ".");
					sendNewSuspectEvent(ipaddress);
					continue;
				}
				//set the mac address
				at.setMacAddress(physAddr);
				// add At Inteface to list of valid interfaces
				atInterfaces.add(at);

				// Save in DB
				DbAtInterfaceEntry atInterfaceEntry = DbAtInterfaceEntry.get(
						dbConn, at.getNodeId(), ipaddress.getHostAddress());
				
				if (atInterfaceEntry == null) {
					atInterfaceEntry = DbAtInterfaceEntry.create(at.getNodeId(),
							ipaddress.getHostAddress());
				}

				// update object
				atInterfaceEntry.updateAtPhysAddr(physAddr);
				atInterfaceEntry.updateSourceNodeId(nodeid);
				atInterfaceEntry.updateIfIndex(ifindex);
				atInterfaceEntry.updateStatus(DbAtInterfaceEntry.STATUS_ACTIVE);
				atInterfaceEntry.set_lastpolltime(now);

				// store object in database
				atInterfaceEntry.store(dbConn);
			}
			// set AtInterfaces in LinkableNode
			m_node.setAtInterfaces(atInterfaces);
		}

		if (m_snmpcoll.hasCdpCacheTable()) {
			if (log.isDebugEnabled())
				log
						.debug("store: saving CdpCacheTable into SnmpLinkableNode");
			java.util.List<CdpInterface> cdpInterfaces = new java.util.ArrayList<CdpInterface>();
			Iterator ite2 = m_snmpcoll.getCdpCacheTable()
					.getEntries().iterator();
			while (ite2.hasNext()) {
				CdpCacheTableEntry cdpEntry = (CdpCacheTableEntry) ite2.next();
				int cdpAddrType = cdpEntry.getCdpCacheAddressType();

				if (cdpAddrType != CDP_ADDRESS_TYPE_IP_ADDRESS) {
					log.warn(" cdp Address Type not valid " + cdpAddrType);
					continue;
				}

				InetAddress cdpTargetIpAddr = cdpEntry.getCdpCacheAddress();
				
				if (cdpTargetIpAddr == null || cdpTargetIpAddr.isLoopbackAddress() || cdpTargetIpAddr.getHostAddress().equals("0.0.0.0")) {
					log.warn(" cdp Ip Address is not valid " + cdpTargetIpAddr);
					continue;
				}
				
				if (log.isDebugEnabled())	log.debug(" cdp ip address found " + cdpTargetIpAddr.getHostAddress());
				
				int cdpIfIndex = cdpEntry.getCdpCacheIfIndex();
				
				if (cdpIfIndex < 0 ) {
					log.warn(" cdpIfIndex not valid " + cdpIfIndex);
					continue;
				}

				if (log.isDebugEnabled())	log.debug(" cdp ifindex found " + cdpIfIndex);

				String cdpTargetDevicePort = cdpEntry.getCdpCacheDevicePort();
				
				if (cdpTargetDevicePort == null ) {
					log.warn(" cdpTargetDevicePort null. Skipping. " );
					continue;
				}

				if (log.isDebugEnabled())	log.debug(" cdp Target device port name found " + cdpTargetDevicePort);

				int targetCdpNodeId = getNodeidFromIp(dbConn, cdpTargetIpAddr);

				if (targetCdpNodeId == -1 ) {
					log.warn("No nodeid found: cdp interface not added to Linkable Snmp Node. Skipping");
					sendNewSuspectEvent(cdpTargetIpAddr);
					continue;
				}

				int cdpTargetIfindex = getIfIndexByName(
						dbConn, targetCdpNodeId, cdpTargetDevicePort);

				if (log.isDebugEnabled()) log.debug("No valid if target index found: " + cdpTargetIfindex + "cdp interface not added to Linkable Snmp Node. Skipping");
				
				CdpInterface cdpIface = new CdpInterface(cdpIfIndex);
				cdpIface.setCdpTargetNodeId(targetCdpNodeId);
				cdpIface.setCdpTargetIpAddr(cdpTargetIpAddr);
				cdpIface.setCdpTargetIfIndex(cdpTargetIfindex);
				
				cdpInterfaces.add(cdpIface);
			}
			m_node.setCdpInterfaces(cdpInterfaces);
		}

		if (m_snmpcoll.hasRouteTable()) {
			java.util.List<RouterInterface> routeInterfaces = new java.util.ArrayList<RouterInterface>();
			
			Iterator ite3 = m_snmpcoll.getIpRouteTable().getEntries()
					.iterator();
			if (log.isDebugEnabled())
				log
						.debug("store: saving ipRouteTable to iprouteinterface table in DB");
			while (ite3.hasNext()) {
				IpRouteTableEntry ent = (IpRouteTableEntry) ite3.next();

				Integer ifindex = ent.getInt32(IpRouteTableEntry.IP_ROUTE_IFINDEX);

				if (ifindex == null || ifindex < 0) {
					log.warn("store: Not valid ifindex" + ifindex 
							+ " Skipping...");
					continue;
				}

				InetAddress routedest = ent.getIPAddress(IpRouteTableEntry.IP_ROUTE_DEST);
				InetAddress routemask = ent.getIPAddress(IpRouteTableEntry.IP_ROUTE_MASK);
				InetAddress nexthop = ent.getIPAddress(IpRouteTableEntry.IP_ROUTE_NXTHOP);

				if (log.isDebugEnabled()) {
					log.debug("storeSnmpCollection: parsing routedest/routemask/nexthop: " 
							+ routedest + "/" 
							+ routemask + "/"
							+ nexthop + " ifindex " + (ifindex < 1 ? "less than 1" : ifindex));
					
				}
				
				if (nexthop.isLoopbackAddress()) {
					if (log.isInfoEnabled()) 
						log.info("storeSnmpCollection: loopbackaddress found skipping.");
					continue;
				}
				
				if (nexthop.getHostAddress().equals("0.0.0.0")) {
					if (log.isInfoEnabled()) 
						log.info("storeSnmpCollection: broadcast address found skipping.");
					continue;
				}
				
				if (nexthop.isMulticastAddress()) {
					if (log.isInfoEnabled()) 
						log.info("storeSnmpCollection: multicast ddress found skipping.");
					continue;
				}
				
				Integer routemetric1 = ent.getInt32(IpRouteTableEntry.IP_ROUTE_METRIC1);
				Integer routemetric2 = ent.getInt32(IpRouteTableEntry.IP_ROUTE_METRIC2);
				Integer routemetric3  =ent.getInt32(IpRouteTableEntry.IP_ROUTE_METRIC3);
				Integer routemetric4 = ent.getInt32(IpRouteTableEntry.IP_ROUTE_METRIC4);
				Integer routemetric5 = ent.getInt32(IpRouteTableEntry.IP_ROUTE_METRIC5);
				Integer routetype = ent.getInt32(IpRouteTableEntry.IP_ROUTE_TYPE);
				Integer routeproto = ent.getInt32(IpRouteTableEntry.IP_ROUTE_PROTO);

				/**
				 *  FIXME: send routedest 0.0.0.0 to discoverylink  
				 *  remeber that now nexthop 0.0.0.0 is not 
				 *  parsed, anyway we should analize this case in link discovery
				 *  so here is the place where you can have this info saved for
				 * now is discarded. See DiscoveryLink for more details......
				 * 
				**/
				
				// the routerinterface constructor set nodeid, ifindex, netmask for nexthop address
				// try to find on snmpinterface table
				RouterInterface routeIface = getNodeidMaskFromIp(dbConn,nexthop);

				// if target node is not snmp node always try to find info
				// on ipinterface table
				if (routeIface == null) {
					routeIface = getNodeFromIp(dbConn, nexthop);
				}
					
				if (routeIface == null) {
					log.warn("store: No nodeid found for next hop ip" + nexthop 
							+ " Skipping ip route interface add to Linkable Snmp Node");
					// try to find it in ipinterface
					sendNewSuspectEvent(nexthop);
				} else {
					int snmpiftype = -2;
                    
                    //Okay to autobox here, we checked for null
					if (ifindex != null && ifindex > 0) snmpiftype = getSnmpIfType(dbConn, nodeid, ifindex);

					if (snmpiftype == -1) {
						log.warn("store: interface has wrong or null snmpiftype "
								+ snmpiftype + " . Skip adding to DiscoverLink ");
					} else {
						if (log.isDebugEnabled())
							log.debug("store: interface has snmpiftype "
										+ snmpiftype + " . Adding to DiscoverLink ");

						routeIface.setRouteDest(routedest);
						routeIface.setRoutemask(routemask);
						routeIface.setSnmpiftype(snmpiftype);
						routeIface.setIfindex(ifindex);
						routeIface.setMetric(routemetric1);
						routeIface.setNextHop(nexthop);
						routeInterfaces.add(routeIface);
						
					}
				}

				// always save info to DB
				DbIpRouteInterfaceEntry iprouteInterfaceEntry = DbIpRouteInterfaceEntry
						.get(dbConn, nodeid, routedest.getHostAddress());
				if (iprouteInterfaceEntry == null) {
					// Create a new entry
					iprouteInterfaceEntry = DbIpRouteInterfaceEntry.create(
							m_node.getNodeId(), routedest.getHostAddress());
				}
				// update object
				iprouteInterfaceEntry.updateRouteMask(routemask.getHostAddress());
				iprouteInterfaceEntry.updateRouteNextHop(nexthop.getHostAddress());
				iprouteInterfaceEntry.updateIfIndex(ifindex);
                
                //okay to autobox these since were checking for null
				if (routemetric1 != null)
					iprouteInterfaceEntry.updateRouteMetric1(routemetric1);
				if (routemetric2 != null)
					iprouteInterfaceEntry.updateRouteMetric2(routemetric2);
				if (routemetric3 != null)
					iprouteInterfaceEntry.updateRouteMetric3(routemetric3);
				if (routemetric4 != null)
					iprouteInterfaceEntry.updateRouteMetric4(routemetric4);
				if (routemetric5 != null)
					iprouteInterfaceEntry.updateRouteMetric5(routemetric5);
				if (routetype != null)
					iprouteInterfaceEntry.updateRouteType(routetype);
				if (routeproto != null)
					iprouteInterfaceEntry.updateRouteProto(routeproto);
				iprouteInterfaceEntry
						.updateStatus(DbAtInterfaceEntry.STATUS_ACTIVE);
				iprouteInterfaceEntry.set_lastpolltime(now);

				// store object in database
				iprouteInterfaceEntry.store(dbConn);
			}
			m_node.setRouteInterfaces(routeInterfaces);
		}

		if (log.isDebugEnabled())
			log.debug("store: saving VlanTable in DB");

		if (m_snmpcoll.hasVlanTable()) {
						
			Iterator ite3 = m_snmpcoll.getVlanTable().getEntries()
					.iterator();
			if (log.isDebugEnabled())
				log
						.debug("store: saving Snmp Vlan Table to vlan table in DB");
			while (ite3.hasNext()) {
				SnmpTableEntry ent = (SnmpTableEntry) ite3.next();

				Integer vlanindex = ent.getInt32(VlanCollectorEntry.VLAN_INDEX);

				if (vlanindex == null || vlanindex < 0) {
					log.warn("store: Not valid vlan ifindex" + vlanindex 
							+ " Skipping...");
					continue;
				}
				
				String vlanName = ent.getDisplayString(VlanCollectorEntry.VLAN_NAME);
				if (vlanName == null ) {
					log.warn("store: Null vlan name. forcing to default...");
					vlanName = "default";
				}

				Integer vlantype = ent.getInt32(VlanCollectorEntry.VLAN_TYPE);
				Integer vlanstatus = ent.getInt32(VlanCollectorEntry.VLAN_STATUS);
				
				// always save info to DB
				DbVlanEntry vlanEntry = DbVlanEntry
						.get(dbConn, nodeid, vlanindex);
				if (vlanEntry == null) {
					// Create a new entry
					vlanEntry = DbVlanEntry.create(
							m_node.getNodeId(), vlanindex);
				}
				
				vlanEntry.updateVlanName(vlanName);
                //okay to autobox these since were checking for null
				if (vlantype != null)
					vlanEntry.updateVlanType(vlantype);
				if (vlanstatus != null)
					vlanEntry.updateVlanStatus(vlanstatus);
				vlanEntry
						.updateStatus(DbAtInterfaceEntry.STATUS_ACTIVE);
				vlanEntry.set_lastpolltime(now);

				// store object in database
				vlanEntry.store(dbConn);
			}
		}

		if (log.isDebugEnabled())
			log.debug("store: saving SnmpVlanCollection's in DB");
		
		Iterator<Entry<Vlan,SnmpVlanCollection>> ite4 = m_snmpcoll.getSnmpVlanCollections().entrySet().iterator();
		
		SnmpVlanCollection snmpVlanColl = null;
		Vlan vlan = null;
		while (ite4.hasNext()) {
			
			Entry<Vlan,SnmpVlanCollection> entry = ite4.next();

			vlan = entry.getKey();

			
			int vlanid = vlan.getVlanindex();
			String vlanname = vlan.getVlanname();
			String vlanindex = Integer.toString(vlanid);
			if (log.isDebugEnabled())
				log
						.debug("store: parsing VLAN "
								+ vlanindex + " VLAN_NAME " + vlanname);

			snmpVlanColl = entry.getValue();

			if (snmpVlanColl.hasDot1dBase()) {
				if (log.isDebugEnabled())
					log
							.debug("store: saving Dot1dBaseGroup in stpnode table");

				Dot1dBaseGroup dod1db = (Dot1dBaseGroup) snmpVlanColl.getDot1dBase();
				
				String baseBridgeAddress = dod1db.getBridgeAddress();
				if (baseBridgeAddress == null || baseBridgeAddress == "000000000000") {
					log.warn("store: invalid base bridge address " + baseBridgeAddress);
				} else {
					m_node.addBridgeIdentifier(baseBridgeAddress,vlanindex);
					int basenumports = dod1db.getNumberOfPorts();

					
					int bridgetype = dod1db.getBridgeType();
					
					DbStpNodeEntry dbStpNodeEntry = DbStpNodeEntry.get(dbConn,
						m_node.getNodeId(), vlanid);
					if (dbStpNodeEntry == null) {
						// Create a new entry
						dbStpNodeEntry = DbStpNodeEntry.create(m_node
							.getNodeId(), vlanid);
					}
					// update object

					dbStpNodeEntry.updateBaseBridgeAddress(baseBridgeAddress);
					dbStpNodeEntry.updateBaseNumPorts(basenumports);
					dbStpNodeEntry.updateBaseType(bridgetype);
					dbStpNodeEntry.updateBaseVlanName(vlanname);
				
					if (snmpVlanColl.hasDot1dStp()) {
						if (log.isDebugEnabled())
							log
								.debug("store: adding Dot1dStpGroup in stpnode table");

						Dot1dStpGroup dod1stp = (Dot1dStpGroup) snmpVlanColl
							.getDot1dStp();
						int protospec = dod1stp.getStpProtocolSpecification();
						int stppriority = dod1stp.getStpPriority();
						int stprootcost = dod1stp.getStpRootCost();
						int stprootport = dod1stp.getStpRootPort();
						String stpDesignatedRoot = dod1stp.getStpDesignatedRoot();

						if (stpDesignatedRoot == null || stpDesignatedRoot == "0000000000000000") {
							if (log.isDebugEnabled()) log.debug("store: Dot1dStpGroup found stpDesignatedRoot " + stpDesignatedRoot + " not adding to Linkable node");
							stpDesignatedRoot = "0000000000000000";
						} else {
							m_node.setVlanStpRoot(vlanindex,stpDesignatedRoot);
						}
						
						dbStpNodeEntry.updateStpProtocolSpecification(protospec);
						dbStpNodeEntry.updateStpPriority(stppriority);
						dbStpNodeEntry.updateStpDesignatedRoot(stpDesignatedRoot);
						dbStpNodeEntry.updateStpRootCost(stprootcost);
						dbStpNodeEntry.updateStpRootPort(stprootport);
					}
					// store object in database
					dbStpNodeEntry.updateStatus(DbStpNodeEntry.STATUS_ACTIVE);
					dbStpNodeEntry.set_lastpolltime(now);
					dbStpNodeEntry.store(dbConn);
				
					// FIXME implement vlan table.....
					// so you can store vlan tables properly
					// depending on vlan collection entry set
					
					if (snmpVlanColl.hasDot1dBasePortTable()) {
						Iterator sub_ite = snmpVlanColl.getDot1dBasePortTable()
							.getEntries().iterator();
						if (log.isDebugEnabled())
							log
								.debug("store: saving Dot1dBasePortTable in stpinterface table");
						while (sub_ite.hasNext()) {
							Dot1dBasePortTableEntry dot1dbaseptentry = (Dot1dBasePortTableEntry) sub_ite
								.next();
							
							int baseport = dot1dbaseptentry.getBaseBridgePort();
							int ifindex = dot1dbaseptentry.getBaseBridgePortIfindex();
							
							if (baseport == -1 || ifindex == -1 ) {
								log.warn("store: Dot1dBasePortTable invalid baseport or ifindex " + baseport + " / " + ifindex);
								continue;
							}
						
							m_node.setIfIndexBridgePort(ifindex,baseport);
						
							DbStpInterfaceEntry dbStpIntEntry = DbStpInterfaceEntry
								.get(dbConn, m_node.getNodeId(),
										baseport, vlanid);
							if (dbStpIntEntry == null) {
							// Create a new entry
								dbStpIntEntry = DbStpInterfaceEntry.create(
									m_node.getNodeId(), baseport, vlanid);
							}
							
							dbStpIntEntry.updateIfIndex(ifindex);
							dbStpIntEntry.updateStatus(DbStpNodeEntry.STATUS_ACTIVE);
							dbStpIntEntry.set_lastpolltime(now);
							dbStpIntEntry.store(dbConn);
						}
					}

					if (snmpVlanColl.hasDot1dStpPortTable()) {
						if (log.isDebugEnabled())
							log
								.debug(" store: adding Dot1dStpPortTable in stpinterface table");
						Iterator sub_ite = snmpVlanColl.getDot1dStpPortTable()
							.getEntries().iterator();
						while (sub_ite.hasNext()) {
							Dot1dStpPortTableEntry dot1dstpptentry = (Dot1dStpPortTableEntry) sub_ite
								.next();
							int stpport = dot1dstpptentry.getDot1dStpPort();
							
							if (stpport == -1 ) {
								log.warn("store: Dot1dStpPortTable found invalid stp port. Skipping");
								continue;
							}
							
							DbStpInterfaceEntry dbStpIntEntry = DbStpInterfaceEntry
								.get(dbConn, m_node.getNodeId(),
										stpport, vlanid);
							if (dbStpIntEntry == null) {
							// Cannot create the object becouse must exists the dot1dbase
							// object!!!!!
								log
										.warn("store: StpInterface not found in database when storing STP info"
												+ " for bridge node with nodeid "
												+ m_node.getNodeId()
												+ " bridgeport number "
												+ stpport
												+ " and vlan index "
												+ vlanindex
												+ " skipping.");
							} else {
								
								String stpPortDesignatedBridge = dot1dstpptentry.getDot1dStpPortDesignatedBridge();
								String stpPortDesignatedPort = dot1dstpptentry.getDot1dStpPortDesignatedPort();

								if (stpPortDesignatedBridge == null || stpPortDesignatedBridge.equals("0000000000000000")) {
									log.warn("store: "+ stpPortDesignatedBridge + " designated bridge is invalid not adding to discoveryLink");
									stpPortDesignatedBridge = "0000000000000000";
								} else if (stpPortDesignatedPort == null || stpPortDesignatedPort.equals("0000")) {
									log.warn("store: " + stpPortDesignatedPort + " designated port is invalid not adding to discoveryLink");
									stpPortDesignatedPort = "0000";
								} else {
									BridgeStpInterface stpIface = new BridgeStpInterface(stpport,vlanindex);
									stpIface.setStpPortDesignatedBridge(stpPortDesignatedBridge);
									stpIface.setStpPortDesignatedPort(stpPortDesignatedPort);
									m_node.addStpInterface(stpIface);
								}

								dbStpIntEntry.updateStpPortState(dot1dstpptentry.getDot1dStpPortState());
								dbStpIntEntry.updateStpPortPathCost(dot1dstpptentry.getDot1dStpPortPathCost());
								dbStpIntEntry.updateStpportDesignatedBridge(stpPortDesignatedBridge);
								dbStpIntEntry.updateStpportDesignatedRoot(dot1dstpptentry.getDot1dStpPortDesignatedRoot());
								dbStpIntEntry.updateStpPortDesignatedCost(dot1dstpptentry.getDot1dStpPortDesignatedCost());
								dbStpIntEntry.updateStpportDesignatedPort(stpPortDesignatedPort);
								dbStpIntEntry.updateStatus(DbStpNodeEntry.STATUS_ACTIVE);
								dbStpIntEntry.set_lastpolltime(now);

								dbStpIntEntry.store(dbConn);

							}
						}
					}
				
					if (snmpVlanColl.hasDot1dTpFdbTable()) {
						if (log.isDebugEnabled())
							log
									.debug("store: parsing Dot1dTpFdbTable");

						Iterator subite = snmpVlanColl.getDot1dFdbTable()
							.getEntries().iterator();
						while (subite.hasNext()) {
							Dot1dTpFdbTableEntry dot1dfdbentry = (Dot1dTpFdbTableEntry) subite
								.next();
							String curMacAddress = dot1dfdbentry.getDot1dTpFdbAddress();
							
							if (curMacAddress == null || curMacAddress.equals("000000000000")) {
									log.warn("store: Dot1dTpFdbTable invalid macaddress "
										+ curMacAddress + " Skipping.");
								continue;
							}

							if (log.isDebugEnabled())
								log.debug("store: Dot1dTpFdbTable found macaddress "
									+ curMacAddress);

							int fdbport = dot1dfdbentry.getDot1dTpFdbPort();

							if (fdbport == 0 || fdbport == -1 ) {
								if (log.isDebugEnabled())
									log.debug("store: Dot1dTpFdbTable mac learned on invalid port "
										+ fdbport + " . Skipping");
								continue;
							}

							if (log.isDebugEnabled())
								log
									.debug("store: Dot1dTpFdbTable mac address found "
											+ " on bridge port "
											+ fdbport);

							int curfdbstatus = dot1dfdbentry.getDot1dTpFdbStatus();

							if (curfdbstatus == SNMP_DOT1D_FDB_STATUS_LEARNED) {
								m_node.addMacAddress(fdbport,
									curMacAddress, vlanindex);
								if (log.isDebugEnabled())
									log.debug("store: Dot1dTpFdbTable found learned status"
												+ " on bridge port ");
							} else if (curfdbstatus == SNMP_DOT1D_FDB_STATUS_SELF) {
								m_node.addBridgeIdentifier(curMacAddress);
								if (log.isDebugEnabled())
									log.debug("store: Dot1dTpFdbTable mac is bridge identifier");
							} else if (curfdbstatus == SNMP_DOT1D_FDB_STATUS_INVALID) {
								if (log.isDebugEnabled())
									log.debug("store: Dot1dTpFdbTable found INVALID status. Skipping");
							} else	if (curfdbstatus == SNMP_DOT1D_FDB_STATUS_MGMT) {
								if (log.isDebugEnabled())
									log.debug("store: Dot1dTpFdbTable found MGMT status. Skipping");
							} else if (curfdbstatus == SNMP_DOT1D_FDB_STATUS_OTHER) {
								if (log.isDebugEnabled())
									log.debug("store: Dot1dTpFdbTable found OTHER status. Skipping");
							} else if (curfdbstatus == -1) {
								log.warn("store: Dot1dTpFdbTable null status found. Skipping");
							}
						}
					}

					if (snmpVlanColl.hasQBridgeDot1dTpFdbTable()) {
						if (log.isDebugEnabled())
							log
									.debug("store: parsing QBridgeDot1dTpFdbTable");

						Iterator subite = snmpVlanColl.getQBridgeDot1dFdbTable()
							.getEntries().iterator();
						while (subite.hasNext()) {
							QBridgeDot1dTpFdbTableEntry dot1dfdbentry = (QBridgeDot1dTpFdbTableEntry) subite
								.next();

							String curMacAddress = dot1dfdbentry.getQBridgeDot1dTpFdbAddress();

							if (curMacAddress == null || curMacAddress.equals("000000000000")) {
								log.warn("store: QBridgeDot1dTpFdbTable invalid macaddress "
									+ curMacAddress + " Skipping.");
								continue;
							}

							if (log.isDebugEnabled())
								log.debug("store: Dot1dTpFdbTable found macaddress "
									+ curMacAddress);

							int fdbport = dot1dfdbentry.getQBridgeDot1dTpFdbPort();

							if (fdbport == 0 || fdbport == -1 ) {
								if (log.isDebugEnabled())
									log.debug("store: QBridgeDot1dTpFdbTable mac learned on invalid port "
										+ fdbport + " . Skipping");
								continue;
							}

							if (log.isDebugEnabled())
								log
									.debug("store: QBridgeDot1dTpFdbTable mac address found "
											+ " on bridge port "
											+ fdbport);

							int curfdbstatus = dot1dfdbentry.getQBridgeDot1dTpFdbStatus();

							if (curfdbstatus == SNMP_DOT1D_FDB_STATUS_LEARNED) {
								m_node.addMacAddress(fdbport,
									curMacAddress, vlanindex);
								if (log.isDebugEnabled())
									log.debug("store: QBridgeDot1dTpFdbTable found learned status"
												+ " on bridge port ");
							} else if (curfdbstatus == SNMP_DOT1D_FDB_STATUS_SELF) {
								m_node.addBridgeIdentifier(curMacAddress);
								if (log.isDebugEnabled())
									log.debug("store: QBridgeDot1dTpFdbTable mac is bridge identifier");
							} else if (curfdbstatus == SNMP_DOT1D_FDB_STATUS_INVALID) {
								if (log.isDebugEnabled())
									log.debug("store: QBridgeDot1dTpFdbTable found INVALID status. Skipping");
							} else	if (curfdbstatus == SNMP_DOT1D_FDB_STATUS_MGMT) {
								if (log.isDebugEnabled())
									log.debug("store: QBridgeDot1dTpFdbTable found MGMT status. Skipping");
							} else if (curfdbstatus == SNMP_DOT1D_FDB_STATUS_OTHER) {
								if (log.isDebugEnabled())
									log.debug("store: QBridgeDot1dTpFdbTable found OTHER status. Skipping");
							} else if (curfdbstatus == -1) {
								log.warn("store: QBridgeDot1dTpFdbTable null status found. Skipping");
							}
						}
					}

					//now adding bridge identifier mac addresses of switch from snmpinterface
					setBridgeIdentifierFromSnmpInterface(dbConn);
				}
			}
		}
		update(dbConn, now);
		
	}	
	
	private void update(Connection dbConn, Timestamp now) throws SQLException {
	
		Category log = ThreadCategory.getInstance(getClass());
		PreparedStatement stmt = null;

		int i = 0;
		stmt = dbConn.prepareStatement(SQL_UPDATE_ATINTERFACE);
		stmt.setInt(1, m_node.getNodeId());
		stmt.setTimestamp(2, now);

		i = stmt.executeUpdate();
		if (log.isDebugEnabled())
			log.debug("store: SQL statement " + SQL_UPDATE_ATINTERFACE + ". " + i
					+ " rows UPDATED for nodeid=" + m_node.getNodeId()
					+ ".");

		stmt.close();

		stmt = dbConn.prepareStatement(SQL_UPDATE_IPROUTEINTERFACE);
		stmt.setInt(1, m_node.getNodeId());
		stmt.setTimestamp(2, now);

		i = stmt.executeUpdate();
		if (log.isDebugEnabled())
			log.debug("store: SQL statement " + SQL_UPDATE_IPROUTEINTERFACE + ". " + i
					+ " rows UPDATED for nodeid=" + m_node.getNodeId()
					+ ".");

		stmt.close();

		stmt = dbConn.prepareStatement(SQL_UPDATE_STPNODE);
		stmt.setInt(1, m_node.getNodeId());
		stmt.setTimestamp(2, now);

		i = stmt.executeUpdate();
		if (log.isDebugEnabled())
			log.debug("store: SQL statement " + SQL_UPDATE_STPNODE + ". " + i
					+ " rows UPDATED for nodeid=" + m_node.getNodeId()
					+ ".");
		stmt.close();

		stmt = dbConn.prepareStatement(SQL_UPDATE_STPINTERFACE);
		stmt.setInt(1, m_node.getNodeId());
		stmt.setTimestamp(2, now);

		i = stmt.executeUpdate();
		if (log.isDebugEnabled())
			log.debug("store: SQL statement " + SQL_UPDATE_STPINTERFACE + ". " + i
					+ " rows UPDATED for nodeid=" + m_node.getNodeId()
					+ ".");
		stmt.close();
	}

	private void update(Connection dbConn, char status) throws SQLException {

		Category log = ThreadCategory.getInstance(getClass());
		PreparedStatement stmt = null;

		int i = 0;
		stmt = dbConn.prepareStatement(SQL_UPDATE_ATINTERFACE_STATUS);
		stmt.setString(1, new String(new char[] { status }));
		stmt.setInt(2, m_nodeId);
		stmt.setInt(3, m_nodeId);

		i = stmt.executeUpdate();
		if (log.isDebugEnabled())
			log.debug("update: SQL statement " + SQL_UPDATE_ATINTERFACE_STATUS + ". " + i
					+ " rows UPDATED for nodeid=" + m_nodeId
					+ ".");
		stmt.close();

		stmt = dbConn.prepareStatement(SQL_UPDATE_IPROUTEINTERFACE_STATUS);
		stmt.setString(1, new String(new char[] { status }));
		stmt.setInt(2, m_nodeId);

		i = stmt.executeUpdate();
		if (log.isDebugEnabled())
			log.debug("update: SQL statement " + SQL_UPDATE_IPROUTEINTERFACE_STATUS + ". " + i
					+ " rows UPDATED for nodeid=" + m_nodeId
					+ ".");
		stmt.close();

		stmt = dbConn.prepareStatement(SQL_UPDATE_STPNODE_STATUS);
		stmt.setString(1, new String(new char[] { status }));
		stmt.setInt(2, m_nodeId);

		i = stmt.executeUpdate();
		if (log.isDebugEnabled())
			log.debug("update: SQL statement " + SQL_UPDATE_STPNODE_STATUS + ". " + i
					+ " rows UPDATED for nodeid=" + m_nodeId
					+ ".");
		stmt.close();

		stmt = dbConn.prepareStatement(SQL_UPDATE_STPINTERFACE_STATUS);
		stmt.setString(1, new String(new char[] { status }));
		stmt.setInt(2, m_nodeId);

		i = stmt.executeUpdate();
		if (log.isDebugEnabled())
			log.debug("update: SQL statement " + SQL_UPDATE_STPINTERFACE_STATUS + ". " + i
					+ " rows UPDATED for nodeid=" + m_nodeId
					+ ".");
		stmt.close();

		stmt = dbConn.prepareStatement(SQL_UPDATE_DATALINKINTERFACE_STATUS);
		stmt.setString(1, new String(new char[] { status }));
		stmt.setInt(2, m_nodeId);
		stmt.setInt(3, m_nodeId);

		i = stmt.executeUpdate();
		if (log.isDebugEnabled())
			log.debug("update: SQL statement " + SQL_UPDATE_DATALINKINTERFACE_STATUS + ". " + i
					+ " rows UPDATED for nodeid=" + m_nodeId
					+ ".");
		stmt.close();

	}

	private void setBridgeIdentifierFromSnmpInterface(Connection dbConn) throws SQLException {

		Category log = ThreadCategory.getInstance(getClass());

		PreparedStatement stmt = null;
		stmt = dbConn.prepareStatement(SQL_GET_SNMPPHYSADDR_SNMPINTERFACE);
		stmt.setInt(1, m_node.getNodeId());
		
		ResultSet rs = stmt.executeQuery();

		while (rs.next()) {
			String macaddr = rs.getString("snmpphysaddr");
			if (macaddr == null)
				continue;
			m_node.addBridgeIdentifier(macaddr);
			if (log.isDebugEnabled())
				log
						.debug("setBridgeIdentifierFromSnmpInterface: found bridge identifier "
								+ macaddr
								+ " from snmpinterface db table");
		}


	}



	private int getNodeidFromIp(Connection dbConn, InetAddress ipaddr)
			throws SQLException {


		if (ipaddr.isLoopbackAddress() || ipaddr.getHostAddress().equals("0.0.0.0")) return -1;

		Category log = ThreadCategory.getInstance(getClass());
		
		int nodeid = -1;
		
		PreparedStatement stmt = null;
		stmt = dbConn.prepareStatement(SQL_GET_NODEID);
		stmt.setString(1, ipaddr.getHostAddress());

		if (log.isDebugEnabled())
			log.debug("getNodeidFromIp: executing query " + SQL_GET_NODEID + " with ip address=" + ipaddr.getHostAddress());

		ResultSet rs = stmt.executeQuery();

		if (!rs.next()) {
			rs.close();
			stmt.close();
			if (log.isDebugEnabled())
				log.debug("getNodeidFromIp: no entries found in ipinterface");
			return -1;
		}
		// extract the values.
		//
		int ndx = 1;

		// get the node id
		//
		nodeid = rs.getInt(ndx++);
		if (rs.wasNull())
			nodeid = -1;

		if (log.isDebugEnabled())
			log.debug("getNodeidFromIp: found nodeid " + nodeid);

		stmt.close();

		return nodeid;

	}

	private RouterInterface getNodeidMaskFromIp(Connection dbConn, InetAddress ipaddr)
	throws SQLException {
		if (ipaddr.isLoopbackAddress() || ipaddr.getHostAddress().equals("0.0.0.0")) return null;
			
		Category log = ThreadCategory.getInstance(getClass());
			
		int nodeid = -1;
		int ifindex = -1;
		String netmask = null;
		
		PreparedStatement stmt = null;
		stmt = dbConn.prepareStatement(SQL_GET_NODEID__IFINDEX_MASK);
		stmt.setString(1, ipaddr.getHostAddress());
			
		if (log.isDebugEnabled())
			log.debug("getNodeidMaskFromIp: executing query " + SQL_GET_NODEID__IFINDEX_MASK + " with ip address=" + ipaddr.getHostAddress());
			
			
		ResultSet rs = stmt.executeQuery();
			
		if (!rs.next()) {
			rs.close();
			stmt.close();
			if (log.isDebugEnabled())
				log.debug("getNodeidMaskFromIp: no entries found in snmpinterface");
			return null;
		}
		// extract the values.
		//
		// get the node id
		//
		nodeid = rs.getInt("nodeid");
		if (rs.wasNull()) {
			rs.close();
			stmt.close();
			if (log.isDebugEnabled())
				log.debug("getNodeidMaskFromIp: no nodeid found");
			return null;
		}

		ifindex = rs.getInt("snmpifindex");
		if (rs.wasNull()) {
			if (log.isDebugEnabled())
				log.debug("getNodeidMaskFromIp: no snmsnmpifindex found");
			ifindex = -1;
		}

		netmask = rs.getString("snmpipadentnetmask");
		if (rs.wasNull()) {
			if (log.isDebugEnabled())
				log.debug("getNodeidMaskFromIp: no snmpipadentnetmask found");
			netmask = "255.255.255.255";
		}

		rs.close();
		stmt.close();
		RouterInterface ri = new RouterInterface(nodeid,ifindex,netmask);
		return ri;
		
	}

	private RouterInterface getNodeFromIp(Connection dbConn, InetAddress ipaddr)
	throws SQLException {
		if (ipaddr.isLoopbackAddress() || ipaddr.getHostAddress().equals("0.0.0.0")) return null;
			
		Category log = ThreadCategory.getInstance(getClass());
			
		int nodeid = -1;
		int ifindex = -1;
		
		PreparedStatement stmt = null;
		stmt = dbConn.prepareStatement(SQL_GET_NODEID);
		stmt.setString(1, ipaddr.getHostAddress());
			
		if (log.isDebugEnabled())
			log.debug("getNodeFromIp: executing query " + SQL_GET_NODEID + " with ip address=" + ipaddr.getHostAddress());
			
			
		ResultSet rs = stmt.executeQuery();
			
		if (!rs.next()) {
			rs.close();
			stmt.close();
			if (log.isDebugEnabled())
				log.debug("getNodeFromIp: no entries found in snmpinterface");
			return null;
		}
		// extract the values.
		//
		// get the node id
		//
		nodeid = rs.getInt("nodeid");
		if (rs.wasNull()) {
			rs.close();
			stmt.close();
			if (log.isDebugEnabled())
				log.debug("getNodeFromIp: no nodeid found");
			return null;
		}

		rs.close();
		stmt.close();
		RouterInterface ri = new RouterInterface(nodeid,ifindex);
		return ri;
		
	}

	private AtInterface getNodeidIfindexFromIp(Connection dbConn, InetAddress ipaddr)
	throws SQLException {


		if (ipaddr.isLoopbackAddress() || ipaddr.getHostAddress().equals("0.0.0.0")) return null;
		
		Category log = ThreadCategory.getInstance(getClass());
		
		int atnodeid = -1;
		int atifindex = -1;
		
		PreparedStatement stmt = dbConn.prepareStatement(SQL_GET_NODEID_IFINDEX_IPINT);
	
		stmt.setString(1, ipaddr.getHostAddress());

		if (log.isDebugEnabled()) 
			log.debug("getNodeidIfindexFromIp: executing SQL Statement " + SQL_GET_NODEID_IFINDEX_IPINT + " with ip address=" + ipaddr.getHostAddress());
		ResultSet rs = stmt.executeQuery();

		if (!rs.next()) {
			rs.close();
			stmt.close();
			return null;
		}
		
		atnodeid = rs.getInt("nodeid");
		if (rs.wasNull()) {
			return null;
		}
		// save info for DiscoveryLink
		AtInterface ati = new AtInterface(atnodeid,ipaddr.getHostAddress());

		// get ifindex if exists
		atifindex = rs.getInt("ifindex");
		if (rs.wasNull()) {
			if (log.isInfoEnabled())
				log.info("getNodeidIfindexFromIp: nodeid "+ atnodeid +" no ifindex (-1) found for ipaddress "
						+ ipaddr + ".");
		} else {
			if (log.isInfoEnabled())
				log.info("getNodeidIfindexFromIp: nodeid "+ atnodeid +" ifindex " + atifindex + " found for ipaddress "
						+ ipaddr + ".");
			ati.setIfindex(atifindex);
		}
		
		return ati;
		
	}


	private int getSnmpIfType(Connection dbConn, int nodeid, int ifindex)
			throws SQLException {
		Category log = ThreadCategory.getInstance(getClass());

		int snmpiftype = -1;
		PreparedStatement stmt = null;
		stmt = dbConn.prepareStatement(SQL_GET_SNMPIFTYPE);
		stmt.setInt(1, nodeid);
		stmt.setInt(2, ifindex);

		if (log.isDebugEnabled())
			log.debug("getSnmpIfType: executing query "
					+ SQL_GET_SNMPIFTYPE + " with nodeid=" + nodeid + " and ifindex=" + ifindex);

		ResultSet rs = stmt.executeQuery();

		if (!rs.next()) {
			rs.close();
			stmt.close();
			if (log.isDebugEnabled())
				log
						.debug("getSnmpIfType: no entries found in snmpinterface");
			return -1;
		}

		// extract the values.
		//
		int ndx = 1;

		// get the node id
		//
		snmpiftype = rs.getInt(ndx++);
		if (rs.wasNull())
			snmpiftype = -1;

		if (log.isDebugEnabled())
			log
					.debug("getSnmpIfType: found in snmpinterface snmpiftype="
							+ snmpiftype);

		stmt.close();

		return snmpiftype;

	}
	
	private int getIfIndexByName(Connection dbConn,
			int nodeid, String ifName) throws SQLException {

		Category log = ThreadCategory.getInstance(getClass());

		PreparedStatement stmt = null;
		stmt = dbConn.prepareStatement(SQL_GET_IFINDEX_SNMPINTERFACE_NAME);
		stmt.setInt(1, nodeid);
		stmt.setString(2, ifName);
		stmt.setString(3, ifName);
		if (log.isDebugEnabled())
			log.debug("getIfIndexByName: executing query"
					+ SQL_GET_IFINDEX_SNMPINTERFACE_NAME + "nodeid =" + nodeid + "and ifName=" + ifName);

		ResultSet rs = stmt.executeQuery();

		if (!rs.next()) {
			rs.close();
			stmt.close();
			if (log.isDebugEnabled())
				log
						.debug("getIfIndexByName: no entries found in snmpinterface");
			return -1;
		}

		// extract the values.
		//
		int ndx = 1;

		if (rs.wasNull()) {
			
			if (log.isDebugEnabled())
				log
						.debug("getIfIndexByName: no entries found in snmpinterface");
			return -1;
			
		}

		int ifindex = rs.getInt(ndx++);

		if (log.isDebugEnabled())
			log.debug("getIfIndexByName: found ifindex="
					+ ifindex);

		stmt.close();

		return ifindex;
	}

	public LinkableNode getLinkableNode() {
		return m_node;
	}
	
	
	
	private void sendNewSuspectEvent(InetAddress ipaddress) {
		if (isAutoDiscovery()) { 
			Category log = ThreadCategory.getInstance(getClass());
			if (log.isDebugEnabled())
				log.debug("sendNewSuspectEvent:  found ip address to send :" + ipaddress);

			if (ipaddress.isLoopbackAddress() || ipaddress.isMulticastAddress() || ipaddress.getHostAddress().equals("0.0.0.0")) {
				if (log.isDebugEnabled())
					log.debug("sendNewSuspectEvent: not sending event for invalid ip address");
			} else {
					if (log.isDebugEnabled())
						log.debug("sendNewSuspectEvent: sending event for valid ip address");
					Linkd.getInstance().sendNewSuspectEvent(ipaddress.getHostAddress(), m_snmpcoll.getTarget().getHostAddress());
			}
		}

	}

	public boolean isAutoDiscovery() {
		return autoDiscovery;
	}

	public void setAutoDiscovery(boolean autoDiscovery) {
		this.autoDiscovery = autoDiscovery;
	}
	
}
