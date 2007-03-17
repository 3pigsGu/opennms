//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2002-2005 The OpenNMS Group, Inc.  All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified 
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Modifications:
//
// 2007 Mar 13: Call VacuumdConfigFactory.init(), not reload(). - dj@opennms.org
// 2004 Aug 28: Created this file.
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
// Tab Size = 8
//

package org.opennms.netmgt.vacuumd;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;

import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.opennms.core.utils.ThreadCategory;
import org.opennms.netmgt.config.DataSourceFactory;
import org.opennms.netmgt.config.VacuumdConfigFactory;
import org.opennms.netmgt.config.vacuumd.Automation;
import org.opennms.netmgt.daemon.AbstractServiceDaemon;
import org.opennms.netmgt.eventd.EventIpcManager;
import org.opennms.netmgt.eventd.EventListener;
import org.opennms.netmgt.scheduler.LegacyScheduler;
import org.opennms.netmgt.scheduler.Schedule;
import org.opennms.netmgt.scheduler.Scheduler;
import org.opennms.netmgt.xml.event.Event;

/**
 * Implements a daemon whose job it is to run periodic updates against the
 * database for database maintenance work.
 */
public class Vacuumd extends AbstractServiceDaemon implements Runnable, EventListener {

    private static final String RELOAD_CONFIG_UEI = "uei.opennms.org/internal/reloadVacuumdConfig";

    private static Vacuumd m_singleton;

    private Thread m_thread;

    private long m_startTime;

    private boolean m_stopped = false;

    private LegacyScheduler m_scheduler;
    
    private EventIpcManager m_eventMgr;

    public synchronized static Vacuumd getSingleton() {
        if (m_singleton == null) {
            m_singleton = new Vacuumd();
        }
        return m_singleton;
    }
    
    public Vacuumd() {
    	super("OpenNMS.Vacuumd");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.opennms.netmgt.vacuumd.jmx.VacuumdMBean#init()
     */
    protected void onInit() {


        try {
            log().info("Loading the configuration file.");
            VacuumdConfigFactory.init();
            getEventManager().addEventListener(this, RELOAD_CONFIG_UEI);
        } catch (MarshalException ex) {
            log().error("Failed to load outage configuration", ex);
            throw new UndeclaredThrowableException(ex);
        } catch (ValidationException ex) {
            log().error("Failed to load outage configuration", ex);
            throw new UndeclaredThrowableException(ex);
        } catch (IOException ex) {
            log().error("Failed to load outage configuration", ex);
            throw new UndeclaredThrowableException(ex);
        }

        log().info("Vaccumd initialization complete");
        
        createScheduler();
        scheduleAutomations();

    }

    protected void onStart() {
		m_startTime = System.currentTimeMillis();
        m_thread = new Thread(this, "Vacuumd-Thread");
        m_thread.start();
        m_scheduler.start();
	}

    protected void onStop() {
		m_stopped = true;
	}

    protected void onPause() {
		m_scheduler.pause();
        onStop();
	}

    protected void onResume() {
		m_thread = new Thread(this, "Vacuumd-Thread");
        m_scheduler.resume();
        m_thread.start();
	}

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    public void run() {
        ThreadCategory.setPrefix(getName());
        log().info("Vacuumd scheduling started");
        setStatus(RUNNING);

        long now = System.currentTimeMillis();
        long period = VacuumdConfigFactory.getInstance().getPeriod();

        log().info("Vacuumd sleeping until time to execute statements period = " + period);

        long waitTime = 500L;

        while (!m_stopped) {

            try {
                now = waitPeriod(now, period, waitTime);

                log().info("Vacuumd beginning to execute statements");
                executeStatements();

                m_startTime = System.currentTimeMillis();

            } catch (Exception e) {
                log().error("Unexpected exception: ", e);
            }
        }
    }

    /**
     * 
     */
    private void executeStatements() {
        if (!m_stopped) {
            String[] stmts = VacuumdConfigFactory.getInstance().getStatements();
            for (int i = 0; i < stmts.length; i++) {
                runUpdate(stmts[i]);
            }

        }
    }

    /**
     * @param now
     * @param period
     * @param waitTime
     * @return
     */
    private long waitPeriod(long now, long period, long waitTime) {
        int count = 0;
        while (!m_stopped && ((now - m_startTime) < period)) {
            try {

                if (count % 100 == 0) {
                    log().debug("Vacuumd: " + (period - now + m_startTime) + " millis remaining to execution.");
                }
                Thread.sleep(waitTime);
                now = System.currentTimeMillis();
                count++;
            } catch (InterruptedException e) {
                // FIXME: what do I do here?
            }
        }
        return now;
    }
    
    private void runUpdate(String sql) {
        log().info("Vacuumd executing statement: " + sql);
        // update the database
        Connection dbConn = null;
        boolean commit = false;
        try {
            dbConn = DataSourceFactory.getInstance().getConnection();
            dbConn.setAutoCommit(false);

            PreparedStatement stmt = dbConn.prepareStatement(sql);
            int count = stmt.executeUpdate();
            stmt.close();

            if (log().isDebugEnabled())
                log().debug("Vacuumd: Ran update " + sql + ": this affected " + count + " rows");

            commit = true;
        } catch (SQLException ex) {
            log().error("Vacuumd:  Database error execuating statement  " + sql, ex);
        } finally {

            if (dbConn != null)
                try {
                    if (commit) {
                        dbConn.commit();
                    } else {
                        dbConn.rollback();
                    }
                } catch (SQLException ex) {
                } finally {
                    if (dbConn != null)
                        try {
                            dbConn.close();
                        } catch (Exception e) {
                        }
                }
        }

    }
    
    private void createScheduler() {
        // Create a scheduler
        //
        try {
            log().debug("init: Creating Vacuumd scheduler");
            m_scheduler = new LegacyScheduler("Vacuumd", 2);
        } catch (RuntimeException e) {
            log().fatal("init: Failed to create Vacuumd scheduler", e);
            throw e;
        }
    }
    
    public Scheduler getScheduler() {
        return m_scheduler;
    }
    
    private void scheduleAutomations() {
        
        Collection autos = VacuumdConfigFactory.getInstance().getAutomations();
        Iterator it = autos.iterator();
        
        while (it.hasNext()) {
            
            scheduleAutomation((Automation)it.next());
            
        }
    }
    
    private void scheduleAutomation(Automation auto) {
        
        if (auto.getActive()) {
            AutomationProcessor ap = new AutomationProcessor(auto);
            Schedule s = new Schedule(ap, new AutomationInterval(auto.getInterval()), m_scheduler);
            ap.setSchedule(s);
            s.schedule();
        }
    }

    public EventIpcManager getEventManager() {
        return m_eventMgr;
    }

    public void setEventManager(EventIpcManager eventMgr) {
        m_eventMgr = eventMgr;
    }

    public void onEvent(Event e) {
        if (RELOAD_CONFIG_UEI.equals(e.getUei())) {
            try {
                m_scheduler.pause();
                VacuumdConfigFactory.reload();
                m_scheduler.resume();
            } catch (MarshalException e1) {
                log().error("onEvent: problem marshaling vacuumd configuration", e1);
            } catch (ValidationException e1) {
                log().error("onEvent: problem validating vacuumd configuration", e1);
            } catch (IOException e1) {
                log().error("onEvent: IO problem reading vacuumd configuration", e1);
            }
        }
    }

}
