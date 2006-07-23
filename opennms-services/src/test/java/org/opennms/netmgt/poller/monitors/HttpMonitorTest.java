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
//   OpenNMS Licensing       <license@opennms.org>
//   http://www.opennms.org/
//   http://www.opennms.com/
//
// Tab Size = 8

package org.opennms.netmgt.poller.monitors;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import junit.framework.TestCase;

import org.opennms.netmgt.config.poller.Package;
import org.opennms.netmgt.config.poller.Parameter;
import org.opennms.netmgt.mock.MockLogAppender;
import org.opennms.netmgt.mock.MockMonitoredService;
import org.opennms.netmgt.mock.MockUtil;
import org.opennms.netmgt.model.PollStatus;
import org.opennms.netmgt.poller.MonitoredService;
import org.opennms.netmgt.poller.ServiceMonitor;
import org.opennms.netmgt.xml.event.Parm;
import org.opennms.netmgt.xml.event.Parms;
import org.opennms.netmgt.xml.event.Value;
public class HttpMonitorTest extends TestCase {

    private boolean m_runTests = false;


    protected void setUp() throws Exception {
        super.setUp();
        MockLogAppender.setupLogging();
    }
    
    public void testParms() {
        Parms eventParms = new Parms();
        Parm eventParm = new Parm();
        Value parmValue = new Value();
        
        assertTrue(eventParms.getParmCount() == 0);
        
        eventParm.setParmName("test");
        parmValue.setContent("test value");
        eventParm.setValue(parmValue);
        eventParms.addParm(eventParm);
        
        assertTrue(eventParms.getParmCount() == 1);
        assertTrue(eventParms.getParm(0).getParmName() == "test");
        assertTrue(eventParms.getParm(0).getValue().getContent() == "test value");
        
    }

    /*
     * Test method for 'org.opennms.netmgt.poller.monitors.HttpMonitor.poll(NetworkInterface, Map, Package)'
     */
    public void testPollStatusReason() throws UnknownHostException {
        
        if (m_runTests == false) return;
        
        Map m = Collections.synchronizedMap(new TreeMap());
        Parameter p = new Parameter();
        
        ServiceMonitor monitor = new HttpMonitor();
        Package pkg = new Package();
        MonitoredService svc = getMonitoredService(99, "1.1.1.1", "HTTP");

        
        p.setKey("port");
        p.setValue("80");
        m.put(p.getKey(), p.getValue());
        
        p.setKey("retry");
        p.setValue("1");
        m.put(p.getKey(), p.getValue());
        
        p.setKey("timeout");
        p.setValue("2000");
        m.put(p.getKey(), p.getValue());
        
        PollStatus status = monitor.poll(svc, m, pkg);        
        MockUtil.println("Reason: "+status.getReason());
        assertEquals(PollStatus.SERVICE_UNAVAILABLE, status.getStatusCode());
        assertNotNull(status.getReason());
        
    }
    
    public void testResponseRange() throws UnknownHostException {
        
        if (m_runTests == false) return;
        
        Map m = Collections.synchronizedMap(new TreeMap());
        Parameter p = new Parameter();
        
        ServiceMonitor monitor = new HttpMonitor();
        Package pkg = new Package();
        MonitoredService svc = getMonitoredService(3, "www.opennms.org", "HTTP");

        
        p.setKey("port");
        p.setValue("80");
        m.put(p.getKey(), p.getValue());
        
        p.setKey("retry");
        p.setValue("1");
        m.put(p.getKey(), p.getValue());
        
        p.setKey("timeout");
        p.setValue("2000");
        m.put(p.getKey(), p.getValue());
        
        p.setKey("response");
        p.setValue("100-299");
        m.put(p.getKey(), p.getValue());
        
        PollStatus status = monitor.poll(svc, m, pkg);        
        MockUtil.println("Reason: "+status.getReason());
        assertEquals(PollStatus.SERVICE_UNAVAILABLE, status.getStatusCode());
        assertNotNull(status.getReason());

        p.setKey("response");
        p.setValue("100,200,302,400-500");
        m.put(p.getKey(), p.getValue());
        
        monitor = new HttpMonitor();
        status = monitor.poll(svc, m, pkg);        
        MockUtil.println("Reason: "+status.getReason());
        assertEquals(PollStatus.SERVICE_AVAILABLE, status.getStatusCode());
        assertNull(status.getReason());

        p.setKey("response");
        p.setValue("*");
        m.put(p.getKey(), p.getValue());
        
        monitor = new HttpMonitor();
        status = monitor.poll(svc, m, pkg);        
        MockUtil.println("Reason: "+status.getReason());
        assertEquals(PollStatus.SERVICE_AVAILABLE, status.getStatusCode());
        assertNull(status.getReason());
    }
    
    public void testPollValidVirtualDomain() throws UnknownHostException {

        if (m_runTests == false) return;
        
        Map m = Collections.synchronizedMap(new TreeMap());
        Parameter p = new Parameter();
        
        ServiceMonitor monitor = new HttpMonitor();
        Package pkg = new Package();
        
        p.setKey("port");
        p.setValue("80");
        m.put(p.getKey(), p.getValue());
        
        p.setKey("retry");
        p.setValue("1");
        m.put(p.getKey(), p.getValue());
        
        p.setKey("timeout");
        p.setValue("1000");
        m.put(p.getKey(), p.getValue());

        //Try on opennms.org with vhost opennms.com
        p.setKey("host-name");
        p.setValue("www.opennms.com");
        m.put(p.getKey(), p.getValue());
                
        m.put(p.getKey(), p.getValue());
        
        MonitoredService svc = getMonitoredService(3, "www.opennms.org", "HTTP");
        monitor.poll(svc, m, pkg);
        
    }
    
    public void testMatchingTextInResponse() throws UnknownHostException {
        
        if (m_runTests == false) return;
        
        Map m = Collections.synchronizedMap(new TreeMap());
        Parameter p = new Parameter();
        PollStatus status = null;
        
        ServiceMonitor monitor = new HttpMonitor();
        Package pkg = new Package();
        MonitoredService svc = getMonitoredService(3, "www.opennms.org", "HTTP");

        p.setKey("port");
        p.setValue("80");
        m.put(p.getKey(), p.getValue());
        
        p.setKey("retry");
        p.setValue("1");
        m.put(p.getKey(), p.getValue());
        
        p.setKey("timeout");
        p.setValue("2000");
        m.put(p.getKey(), p.getValue());
        
        p.setKey("response");
        p.setValue("100-499");
        m.put(p.getKey(), p.getValue());
        
        p.setKey("response-text");
        p.setValue("surfing");
        m.put(p.getKey(), p.getValue());
        
        /*
         * Adding this parameter because we get a redirect on www.opennms.org
         */
        p.setKey("host-name");
        p.setValue("www.opennms.com");
        m.put(p.getKey(), p.getValue());

        status = monitor.poll(svc, m, pkg);        
        MockUtil.println("Reason: "+status.getReason());
        assertEquals(PollStatus.SERVICE_UNAVAILABLE, status.getStatusCode());
        assertNotNull(status.getReason());

        p.setKey("response-text");
        p.setValue("consulting");
        m.put(p.getKey(), p.getValue());

        MockUtil.println("\nliteral text check: \"consulting\"");
        monitor = new HttpMonitor();
        status = monitor.poll(svc, m, pkg);
        MockUtil.println("Reason: "+status.getReason());
        assertEquals(PollStatus.SERVICE_AVAILABLE, status.getStatusCode());
        assertNull(status.getReason());

        p.setKey("response-text");
        p.setValue("~.*[Cc]onsulting.*");
        m.put(p.getKey(), p.getValue());

        MockUtil.println("\nregex check: \".*[Cc]consulting.*\"");
        monitor = new HttpMonitor();
        status = monitor.poll(svc, m, pkg);
        MockUtil.println("Reason: "+status.getReason());
        assertEquals(PollStatus.SERVICE_AVAILABLE, status.getStatusCode());
        assertNull(status.getReason());

    }
    
    public void testBase64Encoding() {
        if (m_runTests == false) return;
        
        Map m = Collections.synchronizedMap(new TreeMap());
        Parameter p = new Parameter();
        HttpMonitor monitor = new HttpMonitor();
        p.setKey("basic-authentication");
        p.setValue("Aladdin:open sesame");
        m.put(p.getKey(), p.getValue());
        assertEquals("QWxhZGRpbjpvcGVuIHNlc2FtZQ==", monitor.getBasicAuthentication(m));
        assertFalse( "QWxhZGRpbjpvcZVuIHNlc2FtZQ==".equals(monitor.getBasicAuthentication(m)));
    }
    
    
    private MonitoredService getMonitoredService(int nodeId, String hostname, String svcName) throws UnknownHostException {
        return new MockMonitoredService(nodeId, hostname, InetAddress.getByName(hostname).getHostAddress(), svcName);
    }
    
    public void testBasicAuthentication() throws UnknownHostException {
        
        if (m_runTests == false) return;
        
        Map m = Collections.synchronizedMap(new TreeMap());
        Parameter p = new Parameter();
        PollStatus status = null;
        
        ServiceMonitor monitor = new HttpMonitor();
        Package pkg = new Package();
        MonitoredService svc = getMonitoredService(1, "localhost", "HTTP");
        
        p.setKey("port");
        p.setValue("8080");
        m.put(p.getKey(), p.getValue());
        
        p.setKey("retry");
        p.setValue("1");
        m.put(p.getKey(), p.getValue());
        
        p.setKey("timeout");
        p.setValue("2000");
        m.put(p.getKey(), p.getValue());
        
        p.setKey("response");
        p.setValue("100-302");
        m.put(p.getKey(), p.getValue());
                
        p.setKey("verbose");
        p.setValue("true");
        m.put(p.getKey(), p.getValue());
        
        p.setKey("url");
        p.setValue("/opennms/event/list");
        m.put(p.getKey(), p.getValue());

        p.setKey("basic-authentication");
        p.setValue("admin:admin");
        m.put(p.getKey(), p.getValue());
        
        status = monitor.poll(svc, m, pkg);
        MockUtil.println("Reason: "+status.getReason());
        assertEquals(PollStatus.SERVICE_AVAILABLE, status.getStatusCode());
        assertNull(status.getReason());
        
    }
    
    public void testBasicAuthenticationWithHttps() throws UnknownHostException {
        
        if (m_runTests == false) return;
        
        Map m = Collections.synchronizedMap(new TreeMap());
        Parameter p = new Parameter();
        PollStatus status = null;
        
        ServiceMonitor monitor = new HttpsMonitor();
        Package pkg = new Package();
        MonitoredService svc = getMonitoredService(1, "localhost", "HTTPS");
        
        p.setKey("port");
        p.setValue("443");
        m.put(p.getKey(), p.getValue());
        
        p.setKey("retry");
        p.setValue("1");
        m.put(p.getKey(), p.getValue());
        
        p.setKey("timeout");
        p.setValue("2000");
        m.put(p.getKey(), p.getValue());
        
        p.setKey("response");
        p.setValue("100-302");
        m.put(p.getKey(), p.getValue());
                
        p.setKey("verbose");
        p.setValue("true");
        m.put(p.getKey(), p.getValue());
        
        p.setKey("url");
        p.setValue("/opennms/event/list");
        m.put(p.getKey(), p.getValue());

        p.setKey("basic-authentication");
        p.setValue("admin:admin");
        m.put(p.getKey(), p.getValue());
        
        status = monitor.poll(svc, m, pkg);
        MockUtil.println("Reason: "+status.getReason());
        assertEquals(PollStatus.SERVICE_AVAILABLE, status.getStatusCode());
        assertNull(status.getReason());
        
    }

    public void testWithUrl() throws UnknownHostException {
        if (m_runTests == false) return;
        
        Map m = Collections.synchronizedMap(new TreeMap());
        Parameter p = new Parameter();
        PollStatus status = null;
        
        ServiceMonitor monitor = new HttpMonitor();
        Package pkg = new Package();
        MonitoredService svc = getMonitoredService(3, "www.opennms.org", "HTTP");
        
        p.setKey("url");
        //found I needed the trailing "/" on this url
        p.setValue("/wiki/");
        m.put(p.getKey(), p.getValue());
        
        p.setKey("port");
        p.setValue("80");
        m.put(p.getKey(), p.getValue());
        
        p.setKey("retry");
        p.setValue("1");
        m.put(p.getKey(), p.getValue());
        
        p.setKey("timeout");
        p.setValue("2000");
        m.put(p.getKey(), p.getValue());
        
        p.setKey("response");
        p.setValue("100-499");
        m.put(p.getKey(), p.getValue());
        
        p.setKey("response-text");
        p.setValue("OpenNMS");
        m.put(p.getKey(), p.getValue());
        
        //be sure to uncomment this when not running interactively
//        p.setKey("verbose");
//        p.setValue("true");
//        m.put(p.getKey(), p.getValue());
        
        status = monitor.poll(svc, m, pkg);
        MockUtil.println("Reason: "+status.getReason());
        assertEquals(PollStatus.SERVICE_AVAILABLE, status.getStatusCode());
        assertNull(status.getReason());

    }

    
    public void testPollInValidVirtualDomain() throws UnknownHostException {

        if (m_runTests == false) return;
        
        Map m = Collections.synchronizedMap(new TreeMap());
        Parameter p = new Parameter();
        
        ServiceMonitor monitor = new HttpMonitor();
        Package pkg = new Package();
        MonitoredService svc = getMonitoredService(3, "www.opennms.org", "HTTP");
        
        p.setKey("port");
        p.setValue("80");
        m.put(p.getKey(), p.getValue());
        
        p.setKey("retry");
        p.setValue("1");
        m.put(p.getKey(), p.getValue());
        
        p.setKey("timeout");
        p.setValue("1000");
        m.put(p.getKey(), p.getValue());

        p.setKey("host name");
        p.setValue("opennms.com");
        m.put(p.getKey(), p.getValue());
        
        p.setKey("url");
        p.setValue("/solutions/");
        m.put(p.getKey(), p.getValue());

        p.setKey("response-text");
        p.setValue("~.*[Cc]onsulting.*");
        m.put(p.getKey(), p.getValue());

        //Try on opennms.org
        monitor.poll(svc, m, pkg);        
        
    }

}
