//This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2002-2003 The OpenNMS Group, Inc.  All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified 
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.log4j.Category;
import org.opennms.core.utils.ThreadCategory;
import org.opennms.netmgt.EventConstants;
import org.opennms.netmgt.config.DbConnectionFactory;
import org.opennms.netmgt.eventd.db.AutoAction;
import org.opennms.netmgt.eventd.db.Constants;
import org.opennms.netmgt.eventd.db.OperatorAction;
import org.opennms.netmgt.eventd.db.Parameter;
import org.opennms.netmgt.eventd.db.SnmpInfo;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.netmgt.xml.event.Header;
import org.opennms.netmgt.xml.event.Operaction;

/**
 * EventWriter loads the information in each 'Event' into the database.
 * 
 * While loading mutiple values of the same element into a single DB column, the
 * mutiple values are delimited by MULTIPLE_VAL_DELIM.
 * 
 * When an element and its attribute are loaded into a single DB column, the
 * value and the attribute are separated by a DB_ATTRIB_DELIM.
 * 
 * When using delimiters to append values, if the values already have the
 * delimiter, the delimiter in the value is escaped as in URLs.
 * 
 * Values for the ' <parms>' block are loaded with each parm name and parm value
 * delimited with the NAME_VAL_DELIM.
 * 
 * @see org.opennms.netmgt.eventd.db.Constants#MULTIPLE_VAL_DELIM
 * @see org.opennms.netmgt.eventd.db.Constants#DB_ATTRIB_DELIM
 * @see org.opennms.netmgt.eventd.db.Constants#NAME_VAL_DELIM
 * 
 * @author <A HREF="mailto:sowmya@opennms.org">Sowmya Nataraj </A>
 * @author <A HREF="http://www.opennms.org">OpenNMS.org </A>
 */
class Persist {
    //
    // Field sizes in the events table
    //
    private static final int EVENT_UEI_FIELD_SIZE = 256;

    private static final int EVENT_HOST_FIELD_SIZE = 256;

    private static final int EVENT_INTERFACE_FIELD_SIZE = 16;

    private static final int EVENT_DPNAME_FIELD_SIZE = 12;

    private static final int EVENT_SNMPHOST_FIELD_SIZE = 256;

    private static final int EVENT_SNMP_FIELD_SIZE = 256;

    private static final int EVENT_DESCR_FIELD_SIZE = 4000;

    private static final int EVENT_LOGGRP_FIELD_SIZE = 32;

    private static final int EVENT_LOGMSG_FIELD_SIZE = 256;

    private static final int EVENT_PATHOUTAGE_FIELD_SIZE = 1024;

    private static final int EVENT_CORRELATION_FIELD_SIZE = 1024;

    private static final int EVENT_OPERINSTRUCT_FIELD_SIZE = 1024;

    private static final int EVENT_AUTOACTION_FIELD_SIZE = 256;

    private static final int EVENT_OPERACTION_FIELD_SIZE = 256;

    private static final int EVENT_OPERACTION_MENU_FIELD_SIZE = 64;

    private static final int EVENT_NOTIFICATION_FIELD_SIZE = 128;

    private static final int EVENT_TTICKET_FIELD_SIZE = 128;

    private static final int EVENT_FORWARD_FIELD_SIZE = 256;

    private static final int EVENT_MOUSEOVERTEXT_FIELD_SIZE = 64;

    private static final int EVENT_ACKUSER_FIELD_SIZE = 256;

    private static final int EVENT_SOURCE_FIELD_SIZE = 128;

    /**
     * The character to put in if the log or display is to be set to yes
     */
    private static char MSG_YES = 'Y';

    /**
     * The character to put in if the log or display is to be set to no
     */
    private static char MSG_NO = 'N';

    /**
     * The database connection
     */
    protected Connection m_dbConn;

    /**
     * SQL statement to get service id for a service name
     */
    protected PreparedStatement m_getSvcIdStmt;

    /**
     * SQL statement to get hostname for an ip from the ipinterface table
     */
    protected PreparedStatement m_getHostNameStmt;

    /**
     * SQL statement to get next event id from sequence
     */
    protected PreparedStatement m_getNextIdStmt;

    /**
     * SQL statement to get insert an event into the db
     */
    protected PreparedStatement m_insStmt;
    protected PreparedStatement m_reductionQuery;
    protected PreparedStatement m_upDateStmt;

    /**
     * Sets the statement up for a String value.
     * 
     * @param stmt
     *            The statement to add the value to.
     * @param ndx
     *            The ndx for the value.
     * @param value
     *            The value to add to the statement.
     * 
     * @exception java.sql.SQLException
     *                Thrown if there is an error adding the value to the
     *                statement.
     */
    private void set(PreparedStatement stmt, int ndx, String value) throws SQLException {
        if (value == null || value.length() == 0) {
            stmt.setNull(ndx, Types.VARCHAR);
        } else {
            stmt.setString(ndx, value);
        }
    }

    /**
     * Sets the statement up for an integer type. If the integer type is less
     * than zero, then it is set to null!
     * 
     * @param stmt
     *            The statement to add the value to.
     * @param ndx
     *            The ndx for the value.
     * @param value
     *            The value to add to the statement.
     * 
     * @exception java.sql.SQLException
     *                Thrown if there is an error adding the value to the
     *                statement.
     */
    private void set(PreparedStatement stmt, int ndx, int value) throws SQLException {
        if (value < 0) {
            stmt.setNull(ndx, Types.INTEGER);
        } else {
            stmt.setInt(ndx, value);
        }
    }

    /**
     * Sets the statement up for a timestamp type.
     * 
     * @param stmt
     *            The statement to add the value to.
     * @param ndx
     *            The ndx for the value.
     * @param value
     *            The value to add to the statement.
     * 
     * @exception java.sql.SQLException
     *                Thrown if there is an error adding the value to the
     *                statement.
     */
    private void set(PreparedStatement stmt, int ndx, Timestamp value) throws SQLException {
        if (value == null) {
            stmt.setNull(ndx, Types.TIMESTAMP);
        } else {
            stmt.setTimestamp(ndx, value);
        }
    }

    /**
     * Sets the statement up for a character value.
     * 
     * @param stmt
     *            The statement to add the value to.
     * @param ndx
     *            The ndx for the value.
     * @param value
     *            The value to add to the statement.
     * 
     * @exception java.sql.SQLException
     *                Thrown if there is an error adding the value to the
     *                statement.
     */
    private void set(PreparedStatement stmt, int ndx, char value) throws SQLException {
        stmt.setString(ndx, String.valueOf(value));
    }

    /**
     * This method is used to convert the service name into a service id. It
     * first looks up the information from a service map of Eventd and if no
     * match is found, by performing a lookup in the database. If the conversion
     * is successful then the corresponding integer identifier will be returned
     * to the caller.
     * 
     * @param name
     *            The name of the service
     * 
     * @return The integer identifier for the service name.
     * 
     * @exception java.sql.SQLException
     *                Thrown if there is an error accessing the stored data or
     *                the SQL text is malformed. This will also be thrown if the
     *                result cannot be obtained.
     * 
     * @see EventdConstants#SQL_DB_SVCNAME_TO_SVCID
     * 
     */
    private int getServiceID(String name) throws SQLException {
        //
        // Check the name to make sure that it is not null
        //
        if (name == null)
            throw new NullPointerException("The service name was null");

        // ask persistd
        //
        int id = Eventd.getServiceID(name);
        if (id != -1)
            return id;

        //
        // talk to the database and get the identifer
        //
        m_getSvcIdStmt.setString(1, name);
        ResultSet rset = m_getSvcIdStmt.executeQuery();
        if (rset.next()) {
            id = rset.getInt(1);
        }

        // close result set
        rset.close();

        // inform persistd about the new find
        //
        if (id != -1)
            Eventd.addServiceMapping(name, id);

        //
        // return the id to the caller
        //
        return id;
    }

    /**
     * This method is used to convert the event host into a hostname id by
     * performing a lookup in the database. If the conversion is successful then
     * the corresponding hosname will be returned to the caller.
     * 
     * @param hostip
     *            The event host
     * 
     * @return The hostname
     * 
     * @exception java.sql.SQLException
     *                Thrown if there is an error accessing the stored data or
     *                the SQL text is malformed.
     * 
     * @see EventdConstants#SQL_DB_HOSTIP_TO_HOSTNAME
     * 
     */
    private String getHostName(String hostip) throws SQLException {

        //
        // talk to the database and get the identifer
        //
        String hostname = hostip;

        m_getHostNameStmt.setString(1, hostip);
        ResultSet rset = m_getHostNameStmt.executeQuery();
        if (rset.next()) {
            hostname = rset.getString(1);
        }

        // close and free the result set
        //
        rset.close();

        // hostname can be null - if it is, return the ip
        //
        if (hostname == null)
            hostname = hostip;

        //
        // return the hostname to the caller
        //
        return hostname;
    }
    
    protected boolean isReductionNeeded(Header eventHeader, Event event) throws SQLException {
        
        Category log = ThreadCategory.getInstance(AlarmWriter.class);
                
        if (log.isDebugEnabled()) {
            log.debug("Persist.isReductionNeeded: reductionKey: "+event.getReductionKey());
        }

        m_reductionQuery.setString(1, event.getReductionKey());

        ResultSet rs = m_reductionQuery.executeQuery();
        int count = 0;
        while (rs.next()) {
            count++;
        }
        
        return (count > 0 ? true : false);
    }
    
    protected void updateAlarm(Header eventHeader, Event event) throws SQLException {

        Category log = ThreadCategory.getInstance(Persist.class);

        m_upDateStmt.setInt(1, event.getDbid());
        
        java.sql.Timestamp eventTime = getEventTime(event, log);
        m_upDateStmt.setTimestamp(2, eventTime);
        m_upDateStmt.setString(3, event.getReductionKey());

        if (log.isDebugEnabled())
            log.debug("Persist.updateAlarm: reducing event: "+event.getDbid()+ "into alarm: ");
        
        m_upDateStmt.executeUpdate();
        
    }
    
    /**
     * Insert values into the ALARMS table
     * 
     * @exception java.sql.SQLException
     *                Thrown if there is an error adding the event to the
     *                database.
     * @exception java.lang.NullPointerException
     *                Thrown if a required resource cannot be found in the
     *                properties file.
     */
    protected void insertAlarm(Header eventHeader, Event event) throws SQLException {
        int alarmID = -1;
        Category log = ThreadCategory.getInstance(AlarmWriter.class);
        
        alarmID = getNextId();
        if (log.isDebugEnabled()) log.debug("AlarmWriter: DBID: "+ alarmID);

        //Column 1, alarmId
        m_insStmt.setInt(1, alarmID);
        
        //Column 2, eventUie
        m_insStmt.setString(2, Constants.format(event.getUei(), EVENT_UEI_FIELD_SIZE));
        
        //Column 3, dpName
        m_insStmt.setString(3, (eventHeader != null) ? Constants.format(eventHeader.getDpName(), EVENT_DPNAME_FIELD_SIZE) : "undefined");
        
        // Column 4, nodeID
        int nodeid = (int) event.getNodeid();
        m_insStmt.setInt(4, event.hasNodeid() ? nodeid : -1);
        
        //Column 5, serviceId
        //
        // convert the service name to a service id
        //
        int svcId = -1;
        if (event.getService() != null) {
            try {
                svcId = getServiceID(event.getService());
            } catch (SQLException sqlE) {
                log.warn("AlarmWriter.insertAlarm: Error converting service name \"" + event.getService() + "\" to an integer identifier, storing -1", sqlE);
            }
        }
        m_insStmt.setInt(5, svcId);

        //Column 6, reductionKey
        m_insStmt.setString(6, event.getReductionKey());
        
        //Column 7, counter
        m_insStmt.setInt(7, 1);
        
        //Column 8, serverity
        set(m_insStmt, 8, Constants.getSeverity(event.getSeverity()));

        //Column 9, lastEventId
        m_insStmt.setInt(9, event.getDbid());
        
        java.sql.Timestamp eventTime = getEventTime(event, log);
        m_insStmt.setTimestamp(10, eventTime);
        
        //Column 11, lastEventTime
        m_insStmt.setTimestamp(11, eventTime);
        
        //Column 12, description
        set(m_insStmt, 12, Constants.format(event.getDescr(), EVENT_DESCR_FIELD_SIZE));

        //Column 13, logMsg
        if (event.getLogmsg() != null) {
            // set log message
            set(m_insStmt, 13, Constants.format(event.getLogmsg().getContent(), EVENT_LOGMSG_FIELD_SIZE));
        } else {
            m_insStmt.setNull(13, Types.VARCHAR);
        }

        //Column 14, operInstruct
        set(m_insStmt, 14, Constants.format(event.getOperinstruct(), EVENT_OPERINSTRUCT_FIELD_SIZE));
        
        //Column 15, tticketId
        //Column 16, tticketState
        if (event.getTticket() != null) {
            set(m_insStmt, 15, Constants.format(event.getTticket().getContent(), EVENT_TTICKET_FIELD_SIZE));
            int ttstate = 0;
            if (event.getTticket().getState().equals("on"))
                ttstate = 1;
            set(m_insStmt, 16, ttstate);
        } else {
            m_insStmt.setNull(15, Types.VARCHAR);
            m_insStmt.setNull(16, Types.INTEGER);
        }

        //Column 17, mouseOverText
        set(m_insStmt, 17, Constants.format(event.getMouseovertext(), EVENT_MOUSEOVERTEXT_FIELD_SIZE));

        //Column 18, suppressedUntil
        //FIXME:
        m_insStmt.setTimestamp(18, eventTime);
        
        //Column 19, suppressedUser
        m_insStmt.setString(19, null);
        
        //Column 20, suppressedTime
        //FIXME:
        m_insStmt.setTimestamp(20, eventTime);
        
        //Column 21, alarmAckUser
        m_insStmt.setString(21, null);
        
        //Column 22, alarmAckTime
        m_insStmt.setTimestamp(22, eventTime);
        
        if (log.isDebugEnabled())
            log.debug("m_insStmt is: "+m_insStmt.toString());

        m_insStmt.executeUpdate();

        if (log.isDebugEnabled())
            log.debug("SUCCESSFULLY added " + event.getUei() + " related  data into the ALARMS table");
   
    }


    /**
     * Insert values into the EVENTS table
     * 
     * @exception java.sql.SQLException
     *                Thrown if there is an error adding the event to the
     *                database.
     * @exception java.lang.NullPointerException
     *                Thrown if a required resource cannot be found in the
     *                properties file.
     */
    protected void insertEvent(Header eventHeader, Event event) throws SQLException {
        int eventID = -1;

        Category log = ThreadCategory.getInstance(EventWriter.class);

        // events next id from sequence
        //
        // Execute the statement to get the next event id
        //
        eventID = getNextId();

        if (log.isDebugEnabled())
            log.debug("EventWriter: DBID: " + eventID);

        synchronized (event) {
            event.setDbid(eventID);
        }

        //
        // Set up the sql information now
        //

        // eventID
        m_insStmt.setInt(1, eventID);

        // eventUEI
        m_insStmt.setString(2, Constants.format(event.getUei(), EVENT_UEI_FIELD_SIZE));

        // nodeID
        int nodeid = (int) event.getNodeid();
        set(m_insStmt, 3, event.hasNodeid() ? nodeid : -1);

        // eventTime
        java.sql.Timestamp eventTime = getEventTime(event, log);
        m_insStmt.setTimestamp(4, eventTime);
        
        //
        // Resolve the event host to a hostname using
        // the ipinterface table
        //
        String hostname = getEventHost(event);

        // eventHost
        set(m_insStmt, 5, Constants.format(hostname, EVENT_HOST_FIELD_SIZE));

        // ipAddr
        set(m_insStmt, 6, Constants.format(event.getInterface(), EVENT_INTERFACE_FIELD_SIZE));

        // eventDpName
        m_insStmt.setString(7, (eventHeader != null) ? Constants.format(eventHeader.getDpName(), EVENT_DPNAME_FIELD_SIZE) : "undefined");

        // eventSnmpHost
        set(m_insStmt, 8, Constants.format(event.getSnmphost(), EVENT_SNMPHOST_FIELD_SIZE));

        //
        // convert the service name to a service id
        //
        int svcId = getEventServiceId(event, log);

        // service identifier
        set(m_insStmt, 9, svcId);

        // eventSnmp
        if (event.getSnmp() != null)
            m_insStmt.setString(10, SnmpInfo.format(event.getSnmp(), EVENT_SNMP_FIELD_SIZE));
        else
            m_insStmt.setNull(10, Types.VARCHAR);

        // eventParms
        set(m_insStmt, 11, (event.getParms() != null) ? Parameter.format(event.getParms()) : null);

        /*FIXME: Not sure why this is here... came in from merge of 1.2.1 to head
         * castor hasn't created the getIfIndex() method and not sure why
         * this call is here.  Need to check the CVS history.
         */
//        // grab the ifIndex out of the parms if it is defined   
//        if (event.getIfIndex() != null) {
//            if (event.getParms() != null) {
//                Parameter.format(event.getParms());
//            }
//        }

        // eventCreateTime
        java.sql.Timestamp eventCreateTime = new java.sql.Timestamp((new java.util.Date()).getTime());
        m_insStmt.setTimestamp(12, eventCreateTime);

        // eventDescr
        set(m_insStmt, 13, Constants.format(event.getDescr(), EVENT_DESCR_FIELD_SIZE));

        // eventLoggroup
        set(m_insStmt, 14, (event.getLoggroupCount() > 0) ? Constants.format(event.getLoggroup(), EVENT_LOGGRP_FIELD_SIZE) : null);

        // eventLogMsg
        // eventLog
        // eventDisplay
        if (event.getLogmsg() != null) {
            // set log message
            set(m_insStmt, 15, Constants.format(event.getLogmsg().getContent(), EVENT_LOGMSG_FIELD_SIZE));
            String logdest = event.getLogmsg().getDest();
            // if 'logndisplay' set both log and display
            // column to yes
            if (logdest.equals("logndisplay")) {
                set(m_insStmt, 16, MSG_YES);
                set(m_insStmt, 17, MSG_YES);
            }
            // if 'logonly' set log column to true
            else if (logdest.equals("logonly")) {
                set(m_insStmt, 16, MSG_YES);
                set(m_insStmt, 17, MSG_NO);
            }
            // if 'displayonly' set display column to true
            else if (logdest.equals("displayonly")) {
                set(m_insStmt, 16, MSG_NO);
                set(m_insStmt, 17, MSG_YES);
            }
            // if 'suppress' set both log and display to false
            else if (logdest.equals("suppress")) {
                set(m_insStmt, 16, MSG_NO);
                set(m_insStmt, 17, MSG_NO);
            }
        } else {
            m_insStmt.setNull(15, Types.VARCHAR);

            // If this is an event that had no match in the event conf
            // mark it as to be logged and displayed so that there
            // are no events that slip through the system
            // without the user knowing about them

            set(m_insStmt, 16, MSG_YES);
            set(m_insStmt, 17, MSG_YES);
        }

        // eventSeverity
        set(m_insStmt, 18, Constants.getSeverity(event.getSeverity()));

        // eventPathOutage
        set(m_insStmt, 19, (event.getPathoutage() != null) ? Constants.format(event.getPathoutage(), EVENT_PATHOUTAGE_FIELD_SIZE) : null);

        // eventCorrelation
        set(m_insStmt, 20, (event.getCorrelation() != null) ? org.opennms.netmgt.eventd.db.Correlation.format(event.getCorrelation(), EVENT_CORRELATION_FIELD_SIZE) : null);

        // eventSuppressedCount
        m_insStmt.setNull(21, Types.INTEGER);

        // eventOperInstruct
        set(m_insStmt, 22, Constants.format(event.getOperinstruct(), EVENT_OPERINSTRUCT_FIELD_SIZE));

        // eventAutoAction
        set(m_insStmt, 23, (event.getAutoactionCount() > 0) ? AutoAction.format(event.getAutoaction(), EVENT_AUTOACTION_FIELD_SIZE) : null);

        // eventOperAction / eventOperActionMenuText
        if (event.getOperactionCount() > 0) {
            List a = new ArrayList();
            List b = new ArrayList();

            Enumeration en = event.enumerateOperaction();
            while (en.hasMoreElements()) {
                Operaction eoa = (Operaction) en.nextElement();
                a.add(eoa);
                b.add(eoa.getMenutext());
            }

            set(m_insStmt, 24, OperatorAction.format(a, EVENT_OPERACTION_FIELD_SIZE));
            set(m_insStmt, 25, Constants.format(b, EVENT_OPERACTION_MENU_FIELD_SIZE));
        } else {
            m_insStmt.setNull(24, Types.VARCHAR);
            m_insStmt.setNull(25, Types.VARCHAR);
        }

        // eventNotification, this column no longer needed
        m_insStmt.setNull(26, Types.VARCHAR);

        // eventTroubleTicket / eventTroubleTicket state
        if (event.getTticket() != null) {
            set(m_insStmt, 27, Constants.format(event.getTticket().getContent(), EVENT_TTICKET_FIELD_SIZE));
            int ttstate = 0;
            if (event.getTticket().getState().equals("on"))
                ttstate = 1;

            set(m_insStmt, 28, ttstate);
        } else {
            m_insStmt.setNull(27, Types.VARCHAR);
            m_insStmt.setNull(28, Types.INTEGER);
        }

        // eventForward
        set(m_insStmt, 29, (event.getForwardCount() > 0) ? org.opennms.netmgt.eventd.db.Forward.format(event.getForward(), EVENT_FORWARD_FIELD_SIZE) : null);

        // event mouseOverText
        set(m_insStmt, 30, Constants.format(event.getMouseovertext(), EVENT_MOUSEOVERTEXT_FIELD_SIZE));

        // eventAckUser
        if (event.getAutoacknowledge() != null && event.getAutoacknowledge().getState().equals("on")) {

            set(m_insStmt, 31, Constants.format(event.getAutoacknowledge().getContent(), EVENT_ACKUSER_FIELD_SIZE));

            // eventAckTime - if autoacknowledge is present,
            // set time to event create time
            set(m_insStmt, 32, eventCreateTime);
        } else {
            m_insStmt.setNull(31, Types.INTEGER);
            m_insStmt.setNull(32, Types.TIMESTAMP);
        }

        // eventSource
        set(m_insStmt, 33, Constants.format(event.getSource(), EVENT_SOURCE_FIELD_SIZE));

        // execute
        m_insStmt.executeUpdate();

        if (log.isDebugEnabled())
            log.debug("SUCCESSFULLY added " + event.getUei() + " related  data into the EVENTS table");
    }

    /**
     * @param event
     * @param log
     * @return
     */
    private int getEventServiceId(Event event, Category log) {
        int svcId = -1;
        if (event.getService() != null) {
            try {
                svcId = getServiceID(event.getService());
            } catch (SQLException sqlE) {
                log.warn("EventWriter.add: Error converting service name \"" + event.getService() + "\" to an integer identifier, storing -1", sqlE);
            }
        }
        return svcId;
    }

    /**
     * @param event
     * @return
     */
    private String getEventHost(Event event) {
        String hostname = event.getHost();
        if (hostname != null) {
            try {
                hostname = getHostName(hostname);
            } catch (SQLException sqlE) {
                // hostname can be null - so do nothing
                // use the IP
                hostname = event.getHost();
            }
        }
        return hostname;
    }

    /**
     * @param event
     * @param log
     * @return
     */
    private java.sql.Timestamp getEventTime(Event event, Category log) {
        java.sql.Timestamp eventTime = null;
        try {
            java.util.Date date = EventConstants.parseToDate(event.getTime());
            eventTime = new java.sql.Timestamp(date.getTime());
        } catch (java.text.ParseException pe) {
            log.warn("Failed to convert time " + event.getTime() + " to java.sql.Timestamp, Setting current time instead", pe);
            eventTime = new java.sql.Timestamp((new java.util.Date()).getTime());
        }
        return eventTime;
    }

    /**
     * Constructor
     * @param connectionFactory 
     */
    public Persist(DbConnectionFactory connectionFactory) throws SQLException {
        // Get a database connection
        m_dbConn = connectionFactory.getConnection();
    }

    /**
     * Close all the connection statements
     */
    public void close() {
        try {
            m_dbConn.close();
        } catch (SQLException e) {
            ThreadCategory.getInstance(EventWriter.class).warn("SQLException while closing database connection", e);
        }
    }
    
    private int getNextId() throws SQLException {
        int id;
        // Get the next id from sequence specified in 
        ResultSet rs = m_getNextIdStmt.executeQuery();
        rs.next();
        id = rs.getInt(1);
        rs.close();
        rs = null;
        return id;
    }


}
