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
package org.opennms.netmgt.xmlrpcd;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.xmlrpc.WebServer;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.jmock.core.Constraint;
import org.opennms.netmgt.mock.MockLogAppender;
import org.opennms.spring.xmlrpc.XmlRpcProxyFactoryBean;
import org.opennms.spring.xmlrpc.XmlRpcServiceExporter;
import org.opennms.spring.xmlrpc.XmlRpcWebServerFactoryBean;

/**
 * Represents a XmlRpcTest 
 *
 * @author brozow
 */
public class XmlRpcTest extends MockObjectTestCase {


    static private WebServer m_webServer;
    private Mock m_mockProvisioner;
    private Provisioner m_proxy;
    private XmlRpcServiceExporter m_exporter;

    protected void setUp() throws Exception {
        MockLogAppender.setupLogging();
        

        m_mockProvisioner = mock(Provisioner.class);
        Provisioner bean = (Provisioner)m_mockProvisioner.proxy();
        
        m_proxy = createRemoteProxy(bean);

    }
    
    private Provisioner createLocalProxy(Provisioner bean) {
        return bean;
    }

    private Provisioner createRemoteProxy(Provisioner bean) throws Exception {
        setUpWebServer();
        
        m_exporter = new XmlRpcServiceExporter();
        m_exporter.setServiceInterface(Provisioner.class);
        m_exporter.setService(bean);
        m_exporter.setWebServer(m_webServer);
        m_exporter.afterPropertiesSet();
        
        Thread.sleep(1000);
        
        return createRemoteProxy("http://localhost:9192/RPC2");
    }

    private Provisioner createRemoteProxy(String serverUrl) throws Exception {
        XmlRpcProxyFactoryBean pfb = new XmlRpcProxyFactoryBean();
        pfb.setServiceInterface(Provisioner.class);
        pfb.setServiceUrl(serverUrl);
        pfb.afterPropertiesSet();
        return (Provisioner) pfb.getObject();
    }

    private void setUpWebServer() throws Exception {
        if (m_webServer == null) {
            //XmlRpc.debug = true;
            XmlRpcWebServerFactoryBean wsf = new XmlRpcWebServerFactoryBean();
            wsf.setPort(9192);
            wsf.setSecure(false);
            wsf.afterPropertiesSet();
            m_webServer = (WebServer)wsf.getObject();
            Thread.sleep(1000);
        }
    }
    
    protected void tearDown() throws Exception {
        if (m_exporter != null)
            m_exporter.destroy();
    }

    public void testXmlRpcAddServiceICMP() throws Throwable {
        m_mockProvisioner.expects(once())
        .method("addServiceICMP")
        .with(new Constraint[]{ eq("RS-ICMP-1"), eq(3), eq(1000), eq(300000), eq(30000), eq(300000) })
        .will(returnValue(true));
        
        boolean retVal = m_proxy.addServiceICMP("RS-ICMP-1", 3, 1000, 300000, 30000, 300000);
        
        assertTrue(retVal);
    }

    public void testXmlRpcAddServiceICMPIllegalArg() throws Throwable {
        String msg = "retries must be greater than or equals to zero";
        m_mockProvisioner.expects(once())
        .method("addServiceICMP")
        .with(new Constraint[]{ eq("RS-ICMP-1"), eq(-1), eq(1000), eq(300000), eq(30000), eq(300000) })
        .will(throwException(new IllegalArgumentException(msg)));
        
        try {
            boolean retVal = m_proxy.addServiceICMP("RS-ICMP-1", -1, 1000, 300000, 30000, 300000);
            fail("Expected exception to be thrown");
        } catch(IllegalArgumentException e) {
            assertEquals(msg, e.getMessage());
        } 
        
    }

    public void testAddServiceDNS() {
        m_mockProvisioner.expects(once())
        .method("addServiceDNS")
        .with(new Constraint[]{ eq("RS-DNS-1"), eq(3), eq(1000), eq(300000), eq(30000), eq(300000), eq(1234), eq("www.opennms.org") })
        .will(returnValue(true));
        
        boolean retVal = m_proxy.addServiceDNS("RS-DNS-1", 3, 1000, 300000, 30000, 300000, 1234, "www.opennms.org");
        
        
        assertTrue(retVal);
    }
    
    public void testAddServiceTCP() {
        m_mockProvisioner.expects(once())
        .method("addServiceTCP")
        .with(new Constraint[]{ eq("RS-TCP-1"), eq(3), eq(1000), eq(300000), eq(30000), eq(300000), eq(1234), eq("HELO")})
        .will(returnValue(true));
        
        boolean retVal = m_proxy.addServiceTCP("RS-TCP-1", 3, 1000, 300000, 30000, 300000, 1234, "HELO");
        
        assertTrue(retVal);
    }
    
    public void testAddServiceHTTP() throws MalformedURLException {
        String url = "http://www.opennms.org";
        m_mockProvisioner.expects(once())
        .method("addServiceHTTP")
        .with(new Constraint[]{ eq("RS-HTTP-1"), eq(3), eq(1000), eq(300000), eq(30000), eq(300000), eq(80), eq("200"), eq("Login"), eq(url)/*, eq("user:pw"), eq("OpenNMS Monitor") */})
        .will(returnValue(true));
        
        // TODO: HttpMonitor BackLevel
        boolean retVal = m_proxy.addServiceHTTP("RS-HTTP-1", 3, 1000, 300000, 30000, 300000, 80, "200", "Login", url/*, "user:pw", "OpenNMS Monitor"*/);
        
        assertTrue(retVal);
    }
    
    public void testAddServiceHTTPInvalidURL() throws MalformedURLException {
        String url = "htt://www.opennms.org";
        MalformedURLException urlException = getMalformedUrlException(url);
        m_mockProvisioner.expects(once())
        .method("addServiceHTTP")
        .with(new Constraint[]{ eq("RS-HTTP-1"), eq(3), eq(1000), eq(300000), eq(30000), eq(300000), eq(80), eq("200"), eq("Login"), eq(url)/*, eq("user:pw"), eq("OpenNMS Monitor") */})
        .will(throwException(urlException));
        
        try {
            // TODO: HttpMonitor BackLevel
            boolean retVal = m_proxy.addServiceHTTP("RS-HTTP-1", 3, 1000, 300000, 30000, 300000, 80, "200", "Login", url/*, "user:pw", "OpenNMS Monitor"*/);
            fail("Expected exception");
        } catch (MalformedURLException e) {
            assertEquals(urlException.getMessage(), e.getMessage());
        }
    }

    private MalformedURLException getMalformedUrlException(String url) {
        MalformedURLException urlException = null;
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            urlException = e;
        }
        return urlException;
    }
    
    public void testAddServiceHTTPS() throws MalformedURLException {
        String url = "https://www.opennms.org";
        m_mockProvisioner.expects(once())
        .method("addServiceHTTPS")
        .with(new Constraint[]{ eq("RS-HTTPS-1"), eq(3), eq(1000), eq(300000), eq(30000), eq(300000), eq(80), eq("200"), eq("Login"), eq(url)/*, eq("user:pw"), eq("OpenNMS Monitor") */})
        .will(returnValue(true));
        
        // TODO: HttpMonitor BackLevel
        boolean retVal = m_proxy.addServiceHTTPS("RS-HTTPS-1", 3, 1000, 300000, 30000, 300000, 80, "200", "Login", url/*, "user:pw", "OpenNMS Monitor"*/);
        
        assertTrue(retVal);
    }
    
    public void testAddServiceDatabase() throws MalformedURLException {
        String url = "jdbc://localhost/database";
        m_mockProvisioner.expects(once())
        .method("addServiceDatabase")
        .with(new Constraint[]{ eq("RS-POSTGRES-1"), eq(3), eq(1000), eq(300000), eq(30000), eq(300000), eq("sa"), eq(""), eq("org.postgresql.Driver"), eq(url)})
        .will(returnValue(true));
        
        boolean retVal = m_proxy.addServiceDatabase("RS-POSTGRES-1", 3, 1000, 300000, 30000, 300000, "sa", "", "org.postgresql.Driver", url);
        
        assertTrue(retVal);
    }
    

}
