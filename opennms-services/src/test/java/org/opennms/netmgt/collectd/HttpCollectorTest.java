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
// OpenNMS Licensing       <license@opennms.org>
//     http://www.opennms.org/
//     http://www.opennms.com/
//

package org.opennms.netmgt.collectd;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.opennms.netmgt.config.HttpCollectionConfigFactory;
import org.opennms.netmgt.mock.OpenNMSTestCase;
import org.opennms.netmgt.model.OnmsDistPoller;
import org.opennms.netmgt.model.OnmsIpInterface;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.utils.EventProxy;

/**
 * @author <a href="mailto:david@opennms.org">David Hustace</a>
 *
 */
public class HttpCollectorTest extends OpenNMSTestCase {

    private HttpCollectionConfigFactory m_factory;

    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        initializeFactory();
    }

    private void initializeFactory() throws MarshalException, ValidationException, IOException {
        m_factory = new HttpCollectionConfigFactory(getConfigRdr());
        HttpCollectionConfigFactory.setInstance(m_factory);
    }

    private Reader getConfigRdr() {
        return new StringReader("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
                "<http-datacollection-config  \n" + 
                "    xmlns:http-dc=\"http://xmlns.opennms.org/xsd/config/http-datacollection\" \n" + 
                "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" + 
                "    xsi:schemaLocation=\"http://xmlns.opennms.org/xsd/config/http-datacollection " +
                "      http://www.opennms.org/xsd/config/http-datacollection-config.xsd\" \n" + 
                "    rrdRepository=\"@install.share.dir@/rrd/snmp/\" >\n" + 
                "  <http-collection name=\"default\">\n" + 
                "    <rrd step=\"300\">\n" + 
                "      <rra>RRA:AVERAGE:0.5:1:8928</rra>\n" + 
                "      <rra>RRA:AVERAGE:0.5:12:8784</rra>\n" + 
                "      <rra>RRA:MIN:0.5:12:8784</rra>\n" + 
                "      <rra>RRA:MAX:0.5:12:8784</rra>\n" + 
                "    </rrd>\n" + 
                "    <uris>\n" + 
                "      <uri name=\"test-document-count\">\n" + 
                "        <url path=\"/test/resources/httpcolltest.html\"\n" + 
                "             user-agent=\"Mozilla/5.0 (Macintosh; U; PPC Mac OS X; en) " +
                "AppleWebKit/412 (KHTML, like Gecko) Safari/412\" \n" + 
                "             matches=\"(?s).*Document Count:\\s+([0-9]+).*\" response-range=\"100-399\" >\n" + 
                "        </url>\n" + 
                "        <attributes>\n" + 
                "          <attrib alias=\"documentCount\" match-group=\"1\" type=\"counter32\"/>\n" + 
                "        </attributes>\n" + 
                "      </uri>\n" + 
                "    </uris>\n" + 
                "  </http-collection>\n" + 
                "</http-datacollection-config>");
    }

    /* (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link org.opennms.netmgt.collectd.HttpCollector#collect(
     *   org.opennms.netmgt.collectd.CollectionAgent, org.opennms.netmgt.utils.EventProxy, java.util.Map)}.
     */
    public final void testCollect() {
        HttpCollector collector = new HttpCollector();
        OnmsDistPoller distPoller = new OnmsDistPoller("localhost", "127.0.0.1");
        OnmsNode node = new OnmsNode(distPoller );
        OnmsIpInterface iface = new OnmsIpInterface("209.61.128.9", node );
        CollectionAgent agent = new CollectionAgent(iface );
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("http-collection", "default");
        EventProxy eproxy = getEventProxy();
        collector.collect(agent, eproxy, parameters);
    }

    /**
     * Test method for {@link org.opennms.netmgt.collectd.HttpCollector#initialize(java.util.Map)}.
     */
    public final void testInitializeMap() {
//        fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link org.opennms.netmgt.collectd.HttpCollector#initialize(org.opennms.netmgt.collectd.CollectionAgent, java.util.Map)}.
     */
    public final void testInitializeCollectionAgentMap() {
//        fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link org.opennms.netmgt.collectd.HttpCollector#release()}.
     */
    public final void testRelease() {
//        fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link org.opennms.netmgt.collectd.HttpCollector#release(org.opennms.netmgt.collectd.CollectionAgent)}.
     */
    public final void testReleaseCollectionAgent() {
//        fail("Not yet implemented"); // TODO
    }

}
