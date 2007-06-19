package org.opennms.netmgt.config;

import java.io.Reader;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

import junit.framework.TestCase;

import org.opennms.netmgt.EventConstants;
import org.opennms.netmgt.snmp.SnmpAgentConfig;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.netmgt.xml.event.Parm;
import org.opennms.netmgt.xml.event.Parms;
import org.opennms.netmgt.xml.event.Value;

public class ConfigureSnmpTest extends TestCase {
	
    String m_SnmpSpec = "<?xml version=\"1.0\"?>\n" + 
    "<snmp-config retry=\"3\" timeout=\"800\"\n" + 
    "   read-community=\"public\" write-community=\"private\">\n" + 
    "   <definition version=\"v2c\">\n" + 
    "       <specific>192.168.0.5</specific>\n" + 
    "   </definition>\n" + 
    "\n" + 
    "   <definition read-community=\"opennmsrules\">\n" + 
    "       <range begin=\"192.168.100.1\" end=\"192.168.100.254\"/>\n" + 
    "       <range begin=\"192.168.101.1\" end=\"192.168.101.254\"/>\n" + 
    "       <range begin=\"192.168.102.1\" end=\"192.168.102.254\"/>\n" + 
    "       <range begin=\"192.168.103.1\" end=\"192.168.103.254\"/>\n" + 
    "       <range begin=\"192.168.104.1\" end=\"192.168.104.254\"/>\n" + 
    "       <range begin=\"192.168.105.1\" end=\"192.168.105.254\"/>\n" + 
    "       <range begin=\"192.168.106.1\" end=\"192.168.106.254\"/>\n" + 
    "       <range begin=\"192.168.107.1\" end=\"192.168.107.254\"/>\n" +
    "       <range begin=\"192.168.0.1\" end=\"192.168.0.10\"/>\n" + 
    "   </definition>\n" + 
    "   <definition version=\"v2c\" read-community=\"splice-test\">\n" + 
    "       <specific>10.1.1.1</specific>\n" + 
    "       <specific>10.1.1.2</specific>\n" + 
    "       <specific>10.1.1.3</specific>\n" + 
    "       <specific>10.1.1.5</specific>\n" + 
    "       <specific>10.1.1.6</specific>\n" + 
    "       <specific>10.1.1.10</specific>\n" + 
    "       <range begin=\"10.1.2.1\" end=\"10.1.2.100\"/>\n" + 
    "       <range begin=\"11.1.2.1\" end=\"11.1.2.100\"/>\n" + 
    "       <range begin=\"12.1.2.1\" end=\"12.1.2.100\"/>\n" + 
    "   </definition>\n" + 
    "   <definition read-community=\"splice2-test\">\n" + 
    "       <specific>10.1.1.10</specific>\n" + 
    "       <range begin=\"10.1.1.11\" end=\"10.1.1.100\"/>\n" + 
    "       <range begin=\"11.1.2.1\" end=\"11.1.2.100\"/>\n" + 
    "       <range begin=\"12.1.2.1\" end=\"12.1.2.100\"/>\n" + 
    "   </definition>\n" + 
    "   <definition read-community=\"splice3-test\">\n" + 
    "       <specific>10.1.1.10</specific>\n" + 
    "       <specific>10.1.1.12</specific>\n" + 
    "       <range begin=\"10.1.1.11\" end=\"10.1.1.100\"/>\n" + 
    "       <range begin=\"11.1.2.1\" end=\"11.1.2.1\"/>\n" + 
    "       <range begin=\"12.1.2.1\" end=\"12.1.2.1\"/>\n" + 
    "   </definition>\n" + 
    "\n" + 
    "</snmp-config>\n" + 
    "";
    final private int m_startingDefCount = 5;

    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
    	super.setUp();
    	Reader rdr = new StringReader(m_SnmpSpec);
    	SnmpPeerFactory.setInstance(new SnmpPeerFactory(rdr));
    }

    //start back porting configure snmp enhancements from stable
    /**
     * Test method for {@link org.opennms.netmgt.config.SnmpPeerFactory#toLong(java.net.InetAddress)}.
     * Tests creating a string representation of an IP address that is converted to an InetAddress and then
     * a long and back to an IP address.
     * 
     * @throws UnknownHostException 
     */
    public void testToLongToAddr() throws UnknownHostException {
        String addr = "192.168.1.1";
        assertEquals(addr, SnmpPeerFactory.toIpAddr(SnmpPeerFactory.toLong(InetAddress.getByName(addr))));
    }

    /**
     * Test method for {@link org.opennms.netmgt.config.SnmpPeerFactory#createSnmpEventInfo(org.opennms.netmgt.xml.event.Event)}.
     * Tests creating an SNMP config definition from a configureSNMP event.
     * 
     * @throws UnknownHostException 
     */
    public void testCreateSnmpEventInfo() throws UnknownHostException {
        Event event = createConfigureSnmpEvent("192.168.1.1", null);
        addCommunityStringToEvent(event, "seemore");
        
        SnmpEventInfo info = new SnmpEventInfo(event);
        
        assertNotNull(info);
        assertEquals("192.168.1.1", info.getFirstIPAddress());
        assertEquals(SnmpPeerFactory.toLong(InetAddress.getByName("192.168.1.1")), info.getFirst());
        assertNull(info.getLastIPAddress());
        assertTrue(info.isSpecific());
    }
    
    /**
     * Tests getting the correct SNMP Peer after a configureSNMP event and merge to the running config.
     * @throws UnknownHostException
     */
    public void testSnmpEventInfoClassWithSpecific() throws UnknownHostException {
        final String addr = "192.168.0.5";
        Event event = createConfigureSnmpEvent(addr, null);
        addCommunityStringToEvent(event, "abc");
        SnmpEventInfo info = new SnmpEventInfo(event);
        
        SnmpConfigManager mgr = new SnmpConfigManager(SnmpPeerFactory.getSnmpConfig());
        mgr.mergeIntoConfig(info.createDef());

        SnmpAgentConfig agent = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(addr));
        assertEquals(agent.getAddress().getHostAddress(), addr);
        assertEquals("abc", agent.getReadCommunity());
    }
    
    /**
     * This test should remove the specific 192.168.0.5 from the first definition and
     * replace it with a range 192.168.0.5 - 192.168.0.7.
     * 
     * @throws UnknownHostException
     */
    public void testSnmpEventInfoClassWithRangeReplacingSpecific() throws UnknownHostException {
        final String addr1 = "192.168.0.5";
        final String addr2 = "192.168.0.7";
        
        SnmpAgentConfig agent = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(addr1));
        assertEquals(SnmpAgentConfig.VERSION2C, agent.getVersion());
        
        Event event = createConfigureSnmpEvent(addr1, addr2);
        SnmpEventInfo info = new SnmpEventInfo(event);
        info.setVersion("v2c");
        
        SnmpConfigManager mgr = new SnmpConfigManager(SnmpPeerFactory.getSnmpConfig());
        mgr.mergeIntoConfig(info.createDef());
        
        agent = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(addr1));
        assertEquals(agent.getAddress().getHostAddress(), addr1);
        assertEquals(SnmpAgentConfig.VERSION2C, agent.getVersion());
        assertEquals(m_startingDefCount, SnmpPeerFactory.getSnmpConfig().getDefinitionCount());
    }

    /**
     * Tests getting the correct SNMP Peer after merging a new range that super sets a current range.
     * 
     * @throws UnknownHostException
     */
    public void testSnmpEventInfoClassWithRangeSuperSettingDefRanges() throws UnknownHostException {
        final String addr1 = "192.168.99.1";
        final String addr2 = "192.168.108.254";
        
        SnmpAgentConfig agent = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(addr1));
        assertEquals(SnmpAgentConfig.VERSION1, agent.getVersion());
        
        Event event = createConfigureSnmpEvent(addr1, addr2);
        SnmpEventInfo info = new SnmpEventInfo(event);
        info.setCommunityString("opennmsrules");
        
        SnmpConfigManager mgr = new SnmpConfigManager(SnmpPeerFactory.getSnmpConfig());
        mgr.mergeIntoConfig(info.createDef());
        
        agent = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(addr1));
        assertEquals(agent.getAddress().getHostAddress(), addr1);
        assertEquals(SnmpAgentConfig.VERSION1, agent.getVersion());
        assertEquals(m_startingDefCount, SnmpPeerFactory.getSnmpConfig().getDefinitionCount());
    }

    /**
     * Tests getting the correct SNMP Peer after receiving a configureSNMP event that moves a
     * specific from one definition into another.
     * 
     * @throws UnknownHostException
     */
    public void testSplicingSpecificsIntoRanges() throws UnknownHostException {
        assertEquals(3, SnmpPeerFactory.getSnmpConfig().getDefinition(2).getRangeCount());
        assertEquals(6, SnmpPeerFactory.getSnmpConfig().getDefinition(2).getSpecificCount());
        
        final String specificAddr = "10.1.1.7";
        final Event event = createConfigureSnmpEvent(specificAddr, null);
        final SnmpEventInfo info = new SnmpEventInfo(event);
        info.setCommunityString("splice-test");
        info.setVersion("v2c");
        
        SnmpConfigManager mgr = new SnmpConfigManager(SnmpPeerFactory.getSnmpConfig());
        mgr.mergeIntoConfig(info.createDef());
        
        assertEquals(5, SnmpPeerFactory.getSnmpConfig().getDefinition(2).getRangeCount());
        
        assertEquals("10.1.1.10", SnmpPeerFactory.getSnmpConfig().getDefinition(2).getSpecific(0));
        assertEquals(1, SnmpPeerFactory.getSnmpConfig().getDefinition(2).getSpecificCount());
        assertEquals(m_startingDefCount, SnmpPeerFactory.getSnmpConfig().getDefinitionCount());
    }
    
    /**
     * This test should show that a specific is added to the definition and the current
     * single definition should become the beginning address in the adjacent range.
     * 
     * @throws UnknownHostException
     */
    public void testSplice2() throws UnknownHostException {
        assertEquals(3, SnmpPeerFactory.getSnmpConfig().getDefinition(3).getRangeCount());
        assertEquals(1, SnmpPeerFactory.getSnmpConfig().getDefinition(3).getSpecificCount());
        assertEquals("10.1.1.10", SnmpPeerFactory.getSnmpConfig().getDefinition(3).getSpecific(0));
        assertEquals("10.1.1.11", SnmpPeerFactory.getSnmpConfig().getDefinition(3).getRange(0).getBegin());
        
        final String specificAddr = "10.1.1.7";
        final Event event = createConfigureSnmpEvent(specificAddr, null);
        final SnmpEventInfo info = new SnmpEventInfo(event);
        info.setCommunityString("splice2-test");

        SnmpConfigManager mgr = new SnmpConfigManager(SnmpPeerFactory.getSnmpConfig());
        mgr.mergeIntoConfig(info.createDef());
        
        assertEquals(3, SnmpPeerFactory.getSnmpConfig().getDefinition(3).getRangeCount());
        assertEquals(1, SnmpPeerFactory.getSnmpConfig().getDefinition(3).getSpecificCount());
        assertEquals("10.1.1.7", SnmpPeerFactory.getSnmpConfig().getDefinition(3).getSpecific(0));
        assertEquals("10.1.1.10", SnmpPeerFactory.getSnmpConfig().getDefinition(3).getRange(0).getBegin());

        String marshalledConfig = SnmpPeerFactory.marshallConfig();
        assertNotNull(marshalledConfig);
        
    }

    private Event createConfigureSnmpEvent(final String firstIp, final String lastIp) {
        Event event = new Event();
        event.setUei(EventConstants.CONFIGURE_SNMP_EVENT_UEI);
        
        Parm vParm = new Parm();
        vParm.setParmName(EventConstants.PARM_FIRST_IP_ADDRESS);
        Value value = new Value();
        value.setContent(firstIp);
        value.setType("String");
        vParm.setValue(value);
        
        Parms parms = new Parms();
        parms.addParm(vParm);
        
        vParm = new Parm();
        vParm.setParmName(EventConstants.PARM_LAST_IP_ADDRESS);
        value = new Value();
        value.setContent(lastIp);
        value.setType("String");
        vParm.setValue(value);
        parms.addParm(vParm);

        event.setParms(parms);
        return event;
    }

    private void addCommunityStringToEvent(final Event event, final String commStr) {
        Parms parms = event.getParms();
        Parm vParm = new Parm();
        vParm.setParmName(EventConstants.PARM_COMMUNITY_STRING);
        Value value = new Value();
        value.setContent(commStr);
        value.setType("String");
        vParm.setValue(value);
        parms.addParm(vParm);
    }

}
