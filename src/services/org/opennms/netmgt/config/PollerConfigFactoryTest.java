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
// OpenNMS Licensing       <license@opennms.org>
//     http://www.opennms.org/
//     http://www.opennms.com/
//
package org.opennms.netmgt.config;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.opennms.netmgt.config.poller.Downtime;
import org.opennms.netmgt.config.poller.Filter;
import org.opennms.netmgt.config.poller.IncludeRange;
import org.opennms.netmgt.config.poller.Package;
import org.opennms.netmgt.config.poller.Rrd;
import org.opennms.netmgt.config.poller.Service;
import org.opennms.netmgt.mock.MockDatabase;
import org.opennms.netmgt.mock.MockNetwork;
import org.opennms.netmgt.mock.MockUtil;
import org.opennms.netmgt.mock.OpenNMSTestCase;

public class PollerConfigFactoryTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PollerConfigFactoryTest.class);
    }
    
    public static final String POLLER_CONFIG = "\n" +
            "<poller-configuration\n" +
            "   threads=\"10\"\n" +
            "   nextOutageId=\"SELECT nextval(\'outageNxtId\')\"\n" +
            "   serviceUnresponsiveEnabled=\"false\">\n" +
            "   <node-outage status=\"on\" pollAllIfNoCriticalServiceDefined=\"true\"></node-outage>\n" +
            "   <package name=\"default\">\n" +
            "       <filter>IPADDR IPLIKE *.*.*.*</filter>\n" +
            "       <rrd step = \"300\">\n" + 
            "           <rra>RRA:AVERAGE:0.5:1:2016</rra>\n" + 
            "           <rra>RRA:AVERAGE:0.5:12:4464</rra>\n" + 
            "           <rra>RRA:MIN:0.5:12:4464</rra>\n" + 
            "           <rra>RRA:MAX:0.5:12:4464</rra>\n" + 
            "       </rrd>\n" +
            "       <service name=\"ICMP\" interval=\"300000\">\n" +
            "       </service>\n" +
            "       <downtime begin=\"0\" end=\"30000\"/>\n" + 
            "   </package>\n" +
            "   <monitor service=\"ICMP\" class-name=\"org.opennms.netmgt.poller.monitors.LdapMonitor\"/>\n"+
            "</poller-configuration>\n";

    protected void setUp() throws Exception {
        super.setUp();
        MockUtil.setupLogging();
        MockUtil.resetLogLevel();
        
        DatabaseSchemaConfigFactory dscf = new DatabaseSchemaConfigFactory(new FileReader("etc/database-schema.xml"));
        DatabaseSchemaConfigFactory.setInstance(dscf);
        
        MockNetwork network = new MockNetwork();
        network.setCriticalService("ICMP");
        network.addNode(1, "Router");
        network.addInterface("192.168.1.1");
        network.addService("ICMP");
        network.addService("SMTP");
        network.addInterface("192.168.1.2");
        network.addService("ICMP");
        network.addService("SMTP");
        network.addNode(2, "Server");
        network.addInterface("192.168.1.3");
        network.addService("ICMP");
        network.addService("HTTP");
        network.addNode(3, "Firewall");
        network.addInterface("192.168.1.4");
        network.addService("SMTP");
        network.addService("HTTP");
        network.addInterface("192.168.1.5");
        network.addService("SMTP");
        network.addService("HTTP");
        network.addInterface("192.169.1.5");
        network.addService("SMTP");
        network.addService("HTTP");
        
        MockDatabase db = new MockDatabase();
        db.populate(network);
        DatabaseConnectionFactory.setInstance(db);
        
    }

    protected void tearDown() throws Exception {
        DatabaseConnectionFactory.setInstance(null);
        super.tearDown();
        assertTrue("Warnings in Log!", MockUtil.noWarningsOrHigherLogged());
    }
    
    static class TestPollerConfigManager extends PollerConfigManager {
        String m_xml;
        private String m_localServer;
        private boolean m_verifyServer;
        public TestPollerConfigManager(String xml, String localServer, boolean verifyServer) throws MarshalException, ValidationException, IOException {
            super(new StringReader(xml), localServer, verifyServer);
            m_localServer = localServer;
            m_verifyServer = verifyServer;
            save();
        }

        public void update() throws IOException, MarshalException, ValidationException {
            reloadXML(new StringReader(m_xml));
        }

        protected void saveXml(String xml) throws IOException {
            m_xml = xml;
        }

//        protected List getIpList(Package pkg) {
//            return Collections.EMPTY_LIST;
//        }
//        
        public String getXml() {
            return m_xml;
        }
        
        

        
    }
    
    public void testPollerConfigFactory() throws Exception {
        TestPollerConfigManager factory = new TestPollerConfigManager(POLLER_CONFIG, "localhost", false);
        assertNull(factory.getPackage("TestPkg"));
        Package pkg = new Package();
        pkg.setName("TestPkg");
        
        Filter filter = new Filter();
        filter.setContent("IPADDR IPLIKE *.*.*.*");
        pkg.setFilter(filter);
        
        Rrd rrd = new Rrd();
        rrd.setStep(300);
        rrd.addRra("RRA:AVERAGE:0.5:1:2016");
        pkg.setRrd(rrd);
        
        Service svc = new Service();
        svc.setName("TestService");
        svc.setInterval(300000);
        pkg.addService(svc);
        
        Downtime dt = new Downtime();
        dt.setBegin(0);
        pkg.addDowntime(dt);
        
        IncludeRange inclde = new IncludeRange();
        inclde.setBegin("192.169.0.0");
        inclde.setEnd("192.169.255.255");
        pkg.addIncludeRange(inclde);
        
        factory.addPackage(pkg);
        factory.save();
        
        assertNotNull(factory.getPackage("TestPkg"));
        
        TestPollerConfigManager newFactory = new TestPollerConfigManager(factory.getXml(), "localhost", false);
        Package p = newFactory.getPackage("TestPkg");
        assertNotNull(p);
        assertTrue(newFactory.interfaceInPackage("192.169.1.5", p));
        assertFalse(newFactory.interfaceInPackage("192.168.1.5", p));
        
    }
    
    public void testInterfaceInPackage() throws Exception {
        TestPollerConfigManager factory = new TestPollerConfigManager(POLLER_CONFIG, "localhost", false);
        Package pkg = factory.getPackage("default");
        assertNotNull("Unable to find pkg default", pkg);
        
        assertTrue("Expected 192.168.1.1 to be in the package", factory.interfaceInPackage("192.168.1.1", pkg));
        
        
        
    }

}
