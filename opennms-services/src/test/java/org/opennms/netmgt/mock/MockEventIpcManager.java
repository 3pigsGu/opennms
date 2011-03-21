//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2005 The OpenNMS Group, Inc.  All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified 
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Modifications:
//
// 2008 Feb 05: Java 5 generics, some code formatting. - dj@opennms.org
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
// OpenNMS Licensing       <license@opennms.org>
//     http://www.opennms.org/
//     http://www.opennms.com/
//

package org.opennms.netmgt.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.opennms.netmgt.config.EventConfDao;
import org.opennms.netmgt.config.EventdConfigManager;
import org.opennms.netmgt.eventd.EventIpcBroadcaster;
import org.opennms.netmgt.eventd.EventIpcManager;
import org.opennms.netmgt.eventd.EventIpcManagerProxy;
import org.opennms.netmgt.eventd.processor.EventExpander;
import org.opennms.netmgt.model.events.EventListener;
import org.opennms.netmgt.model.events.EventProxyException;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.netmgt.xml.event.Log;
import org.opennms.test.mock.MockUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.util.Assert;

public class MockEventIpcManager implements EventIpcManager, EventIpcBroadcaster, InitializingBean {

    static class ListenerKeeper {
        EventListener m_listener;

        Set<String> m_ueiList;

        ListenerKeeper(final EventListener listener, final Set<String> ueiList) {
            m_listener = listener;
            m_ueiList = ueiList;
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(27, 31)
                .append(m_listener)
                .append(m_ueiList)
                .toHashCode();
        }

        @Override
        public boolean equals(final Object o) {
            if (o == null) return false;
            if (o instanceof ListenerKeeper) {
                final ListenerKeeper keeper = (ListenerKeeper) o;
                return m_listener.equals(keeper.m_listener) && (m_ueiList == null ? keeper.m_ueiList == null : m_ueiList.equals(keeper.m_ueiList));
            }
            return false;
        }

        private boolean eventMatches(final Event e) {
            if (m_ueiList == null)
                return true;
            return m_ueiList.contains(e.getUei());
        }

        public void sendEventIfAppropriate(final Event e) {
            if (eventMatches(e)) {
                m_listener.onEvent(e);
            }
        }
    }

    /**
     * This class implements {@link EventConfDao} but every call returns null.
     */
    private static class EmptyEventConfDao implements EventConfDao {
        public void addEvent(final org.opennms.netmgt.xml.eventconf.Event event) {}

        public void addEventToProgrammaticStore(final org.opennms.netmgt.xml.eventconf.Event event) {}

        public org.opennms.netmgt.xml.eventconf.Event findByEvent(final Event matchingEvent) {
            return null;
        }

        public org.opennms.netmgt.xml.eventconf.Event findByUei(final String uei) {
            return null;
        }

        public String getEventLabel(final String uei) {
            return null;
        }

        public Map<String, String> getEventLabels() {
            return null;
        }

        public List<String> getEventUEIs() {
            return null;
        }

        public List<org.opennms.netmgt.xml.eventconf.Event> getEvents(final String uei) {
            return null;
        }

        public List<org.opennms.netmgt.xml.eventconf.Event> getEventsByLabel() {
            return null;
        }

        public boolean isSecureTag(final String tag) {
            return false;
        }

        public void reload() throws DataAccessException {}

        public boolean removeEventFromProgrammaticStore(final org.opennms.netmgt.xml.eventconf.Event event) {
            return false;
        }

        public void saveCurrent() {}
    }

    private EventAnticipator m_anticipator;
    
    private EventWriter m_eventWriter = new EventWriter() {
        public void writeEvent(final Event e) {
            
        }
    };

    private List<ListenerKeeper> m_listeners = new ArrayList<ListenerKeeper>();

    private volatile int m_pendingEvents;

    private volatile int m_eventDelay = 20;

    private boolean m_synchronous = true;
    
    private ScheduledExecutorService m_scheduler = null;

    private EventIpcManagerProxy m_proxy;

    public MockEventIpcManager() {
        m_anticipator = new EventAnticipator();
    }
    
    public void addEventListener(final EventListener listener) {
        m_listeners.add(new ListenerKeeper(listener, null));
    }

    public void addEventListener(final EventListener listener, final Collection<String> ueis) {
        m_listeners.add(new ListenerKeeper(listener, new HashSet<String>(ueis)));
    }

    public void addEventListener(final EventListener listener, final String uei) {
        m_listeners.add(new ListenerKeeper(listener, Collections.singleton(uei)));
    }

    public void broadcastNow(final Event event) {
        MockUtil.println("Sending: " + new EventWrapper(event));
        final List<ListenerKeeper> listeners = new ArrayList<ListenerKeeper>(m_listeners);
        for (final ListenerKeeper k : listeners) {
            k.sendEventIfAppropriate(event);
        }
    }
    
    public void setEventWriter(final EventWriter eventWriter) {
        m_eventWriter = eventWriter;
    }

    public EventAnticipator getEventAnticipator() {
        return m_anticipator;
    }
    
    public void setEventAnticipator(final EventAnticipator anticipator) {
        m_anticipator = anticipator;
    }

    public void removeEventListener(final EventListener listener) {
        m_listeners.remove(new ListenerKeeper(listener, null));
    }

    public void removeEventListener(final EventListener listener, final Collection<String> ueis) {
        m_listeners.remove(new ListenerKeeper(listener, new HashSet<String>(ueis)));
    }

    public void removeEventListener(final EventListener listener, final String uei) {
        m_listeners.remove(new ListenerKeeper(listener, Collections.singleton(uei)));
    }
    
    public synchronized void setEventDelay(final int millis) {
        m_eventDelay  = millis;
    }

    /**
     * @param event
     */
    public void sendEventToListeners(final Event event) {
        m_eventWriter.writeEvent(event);
        broadcastNow(event);
    }

    public void setSynchronous(final boolean syncState) {
        m_synchronous = syncState;
    }
    
    public boolean isSynchronous() {
        return m_synchronous;
    }
    
    public synchronized void sendNow(final Event event) {
        // Expand the event parms
        final EventExpander expander = new EventExpander();
        expander.setEventConfDao(new EmptyEventConfDao());
        expander.expandEvent(event);
        m_pendingEvents++;
        MockUtil.println("StartEvent processing: m_pendingEvents = "+m_pendingEvents);
        MockUtil.println("Received: "+ new EventWrapper(event));
        m_anticipator.eventReceived(event);

        final Runnable r = new Runnable() {
            public void run() {
                try {
                    m_eventWriter.writeEvent(event);
                    broadcastNow(event);
                    m_anticipator.eventProcessed(event);
                } finally {
                    synchronized(MockEventIpcManager.this) {
                        m_pendingEvents--;
                        MockUtil.println("Finished processing event m_pendingEvents = "+m_pendingEvents);
                        MockEventIpcManager.this.notifyAll();
                    }
                }
            }
        };
        
        if (isSynchronous()) {
            r.run();
        } else {
            getScheduler().schedule(r, m_eventDelay, TimeUnit.MILLISECONDS);
        }
    }
    
    ScheduledExecutorService getScheduler() {
        if (m_scheduler == null) {
            m_scheduler = Executors.newSingleThreadScheduledExecutor();
        }
        return m_scheduler;
    }

    public void sendNow(final Log eventLog) {
        for (final Event event : eventLog.getEvents().getEventCollection()) {
            sendNow(event);
        }
    }

    /**
     * 
     */
    public synchronized void finishProcessingEvents() {
        while (m_pendingEvents > 0) {
            MockUtil.println("Waiting for event processing: m_pendingEvents = "+m_pendingEvents);
            try {
                wait(10000);
            	break;
            } catch (final InterruptedException e) {
            	Thread.currentThread().interrupt();
            }
        }
    }

    public EventdConfigManager getEventdConfigMgr() {
        // TODO Auto-generated method stub
        return null;
    }

    public void setEventdConfigMgr(final EventdConfigManager eventdConfigMgr) {
        // TODO Auto-generated method stub
        
    }

    public void setDataSource(final DataSource instance) {
        // TODO Auto-generated method stub
        
    }
    
    
    

    public void reset() {
        m_listeners = new ArrayList<ListenerKeeper>();
        m_anticipator.reset();
    }

    public void setEventIpcManagerProxy(final EventIpcManagerProxy proxy) {
        m_proxy = proxy;
    }

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(m_proxy, "expected to have proxy set");
        m_proxy.setDelegate(this);
    }

    public void send(final Event event) throws EventProxyException {
        sendNow(event);
    }

    public void send(final Log eventLog) throws EventProxyException {
        sendNow(eventLog);
    }

}
