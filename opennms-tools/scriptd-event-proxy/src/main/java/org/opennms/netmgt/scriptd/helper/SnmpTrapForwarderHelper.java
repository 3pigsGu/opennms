package org.opennms.netmgt.scriptd.helper;

import java.net.UnknownHostException;

import org.opennms.netmgt.snmp.SnmpTrapBuilder;
import org.opennms.netmgt.snmp.SnmpV3TrapBuilder;
import org.opennms.netmgt.xml.event.Event;

public abstract class SnmpTrapForwarderHelper extends AbstractEventForwarder implements
		EventForwarder {

	String source_ip;
	
	String ip;
	String community;
	int port;
	
	int securityLevel;
	String securityname;
	String authPassPhrase;
	String authProtocol;
	String privPassPhrase;
	String privprotocol;
	SnmpTrapHelper snmpTrapHelper;

	public SnmpTrapHelper getSnmptrapHelper() {
		return snmpTrapHelper;
	}

	public void setSnmptrapHelper(SnmpTrapHelper snmpTrapHelper) {
		this.snmpTrapHelper = snmpTrapHelper;
	}

	public SnmpTrapForwarderHelper(String source_ip, String ip, int port, String community,SnmpTrapHelper snmpTrapHelper) {
		this.source_ip = source_ip;
		this.ip = ip;
		this.port=port;
		this.community=community;		
		this.snmpTrapHelper = snmpTrapHelper;
	}

	public SnmpTrapForwarderHelper(String ip, int port, String community, SnmpTrapHelper snmpTrapHelper) {
		this.ip = ip;
		this.port=port;
		this.community=community;
		this.snmpTrapHelper = snmpTrapHelper;
	}

	
	public SnmpTrapForwarderHelper(String ip, int port, int securityLevel,
			String securityname, String authPassPhrase, String authProtocol,
			String privPassPhrase, String privprotocol, SnmpTrapHelper snmpTrapHelper) {
		super();
		this.ip = ip;
		this.port = port;
		this.securityLevel = securityLevel;
		this.securityname = securityname;
		this.authPassPhrase = authPassPhrase;
		this.authProtocol = authProtocol;
		this.privPassPhrase = privPassPhrase;
		this.privprotocol = privprotocol;
		this.snmpTrapHelper = snmpTrapHelper;

	}

	public void sendV1AlarmTrap( Event event, boolean sync) throws UnknownHostException {
		SnmpTrapBuilder trap = snmpTrapHelper.createV1Trap(".1.3.6.1.4.1.5813.1",getSource_ip(), 6, 3, 0);
		trap = buildAlarmTrap(event, sync, trap);
        try {
			trap.send(getIp(), getPort(), getCommunity());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendV2AlarmTrap( Event event, boolean sync) throws UnknownHostException, SnmpTrapHelperException {
		long trapTimeStamp = 0;
		SnmpTrapBuilder trap = snmpTrapHelper.createV2Trap(".1.3.6.1.4.1.5813.1.3",Long.toString(trapTimeStamp));
		trap=buildAlarmTrap(event, sync, trap);
        try {
			trap.send(getIp(), getPort(), getCommunity());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendV3AlarmTrap( Event event, boolean sync) throws UnknownHostException, SnmpTrapHelperException {
		long trapTimeStamp = 0;
		SnmpTrapBuilder trap = snmpTrapHelper.createV3Trap(".1.3.6.1.4.1.5813.1.3",Long.toString(trapTimeStamp));
		trap=buildAlarmTrap(event, sync, trap);
		SnmpV3TrapBuilder v3trap = (SnmpV3TrapBuilder) trap;
		try {
			v3trap.send(getIp(), getPort(), getSecurityLevel(), getSecurityname(), getAuthPassPhrase(), getAuthProtocol(), getPrivPassPhrase(), getPrivprotocol());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendV1EventTrap( Event event) throws UnknownHostException {
		SnmpTrapBuilder trap = snmpTrapHelper.createV1Trap(".1.3.6.1.4.1.5813.1",getSource_ip(), 6, 1, 0);
		trap = buildEventTrap(event, trap);
        try {
			trap.send(getIp(), getPort(), getCommunity());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendV2EventTrap( Event event) throws UnknownHostException, SnmpTrapHelperException {
		long trapTimeStamp = 0;
		SnmpTrapBuilder trap = snmpTrapHelper.createV2Trap(".1.3.6.1.4.1.5813.1.1",Long.toString(trapTimeStamp));
		trap = buildEventTrap(event, trap);
        try {
			trap.send(getIp(), getPort(), getCommunity());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendV3EventTrap( Event event) throws UnknownHostException, SnmpTrapHelperException {
		long trapTimeStamp = 0;
		SnmpTrapBuilder trap = snmpTrapHelper.createV3Trap(".1.3.6.1.4.1.5813.1.1",Long.toString(trapTimeStamp));
		trap = buildEventTrap(event, trap);
		SnmpV3TrapBuilder v3trap = (SnmpV3TrapBuilder) trap;
		try {
			v3trap.send(getIp(), getPort(), getSecurityLevel(), getSecurityname(), getAuthPassPhrase(), getAuthProtocol(), getPrivPassPhrase(), getPrivprotocol());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private SnmpTrapBuilder buildAlarmTrap(Event event, boolean sync, SnmpTrapBuilder trap) {
		try {
			 trap = buildEventTrap(event, trap);
             if (event.getAlarmData() != null ) {
            	 if (event.getAlarmData().getAlarmType() == 2) {
            		 snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.3.1.0", "OctetString", "text", event.getAlarmData().getClearKey());
            	 	 snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.18.0", "OctetString", "text", "Cleared");
            	 } else 
            		 snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.3.1.0", "OctetString", "text", event.getAlarmData().getReductionKey());             		 
             } else {
                 snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.3.1.0", "OctetString", "text", "null");            	 
             }
             if (sync)
            	 snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.3.2.0", "OctetString", "text", "SYNC");
             else
                 snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.3.2.0", "OctetString", "text", "null");            	 
		} catch (SnmpTrapHelperException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
 		return trap;
	}

	private SnmpTrapBuilder buildEventTrap(Event event, SnmpTrapBuilder trap) {
		try {
			 Integer t_dbid = new Integer(event.getDbid());
             if (t_dbid.intValue() > 0)
                     snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.1.0", "OctetString", "text", t_dbid.toString());
             else
                     snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.1.0", "OctetString", "text", "null");
             if (event.getDistPoller() != null)
                     snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.2.0", "OctetString", "text", event.getDistPoller());
             else
                     snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.2.0", "OctetString", "text", "null");
             if (event.getCreationTime() != null)
                     snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.3.0", "OctetString", "text", event.getCreationTime());
             else
                     snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.3.0", "OctetString", "text", "null");
             if (event.getMasterStation() != null)
                     snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.4.0", "OctetString", "text", event.getMasterStation());
             else
                     snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.4.0", "OctetString", "text", "null");
             if (event.getUei() != null)
                     snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.6.0", "OctetString", "text", event.getUei());
             else
                     snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.6.0", "OctetString", "text", "null");
             if (event.getSource() != null)
                     snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.7.0", "OctetString", "text", event.getSource());
             else
                     snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.7.0", "OctetString", "text", "null");
             String label=null;
             if (event.hasNodeid()) {
            	 	label = DbHelper.getNodeLabel(new Long(event.getNodeid()).toString());
            	 	snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.8.0", "OctetString", "text", new Long(event.getNodeid()).toString());
             } else
                     snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.8.0", "OctetString", "text", "null");
             if (event.getTime() != null)
                     snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.9.0", "OctetString", "text", event.getTime());
             else
                     snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.9.0", "OctetString", "text", "null");
             if (event.getHost() != null)
                     snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.10.0", "OctetString", "text", event.getHost());
             else
                     snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.10.0", "OctetString", "text", "null");
             if (event.getInterface() != null)
                     snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.11.0", "OctetString", "text", event.getInterface());
             else
                     snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.11.0", "OctetString", "text", "null");
             if (event.getSnmphost() != null)
                     snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.12.0", "OctetString", "text", event.getSnmphost());
             else
                     snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.12.0", "OctetString", "text", "null");
             if (event.getService() != null)
                     snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.13.0", "OctetString", "text", event.getService());
             else
                     snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.13.0", "OctetString", "text", "null");    
             if (event.getDescr() != null) {
                 String descrString = event.getDescr().replaceAll("&lt;.*&gt;", " ").replaceAll("\\s+", " ");
                 snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.16.0", "OctetString", "text", descrString);
             } else
                 snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.16.0", "OctetString", "text", "null");
             if (event.getLogmsg() != null && event.getLogmsg().getContent() != null) {
                 String logString = event.getLogmsg().getContent().replaceAll("&lt;.*&gt;", " ").replaceAll("\\s+", " ");
                 snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.17.0", "OctetString", "text", logString);
             } else
                 snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.17.0", "OctetString", "text", "null");
             if (event.getSeverity() != null)
                 snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.18.0", "OctetString", "text", event.getSeverity());
             else
                 snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.18.0", "OctetString", "text", "null");
             if (event.getPathoutage() != null)
                 snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.19.0", "OctetString", "text", event.getPathoutage());
             else
                 snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.19.0", "OctetString", "text", "null");
             if (event.getOperinstruct() != null)
                 snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.20.0", "OctetString", "text", event.getOperinstruct());
             else
                 snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.20.0", "OctetString", "text", "null");

             String retParmVal = null;
             if (event.getInterface() != null) {
                     retParmVal = event.getInterface();
                     java.net.InetAddress inet = java.net.InetAddress.getByName(retParmVal);
                     retParmVal = inet.getHostName();
             }
             if (retParmVal != null)
                     snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.21.0", "OctetString", "text", retParmVal);
             else
                     snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.21.0", "OctetString", "text", "null");

             if (label != null)
                     snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.22.0", "OctetString", "text", label);
             else
                     snmpTrapHelper.addVarBinding(trap, ".1.3.6.1.4.1.5813.20.1.22.0", "OctetString", "text", "null");
             
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (SnmpTrapHelperException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}		
        return trap;
	}

	public String getSource_ip() {
		return source_ip;
	}

	public void setSource_ip(String source_ip) {
		this.source_ip = source_ip;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getCommunity() {
		return community;
	}

	public void setCommunity(String community) {
		this.community = community;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getSecurityLevel() {
		return securityLevel;
	}

	public void setSecurityLevel(int securityLevel) {
		this.securityLevel = securityLevel;
	}

	public String getSecurityname() {
		return securityname;
	}

	public void setSecurityname(String securityname) {
		this.securityname = securityname;
	}

	public String getAuthPassPhrase() {
		return authPassPhrase;
	}

	public void setAuthPassPhrase(String authPassPhrase) {
		this.authPassPhrase = authPassPhrase;
	}

	public String getAuthProtocol() {
		return authProtocol;
	}

	public void setAuthProtocol(String authProtocol) {
		this.authProtocol = authProtocol;
	}

	public String getPrivPassPhrase() {
		return privPassPhrase;
	}

	public void setPrivPassPhrase(String privPassPhrase) {
		this.privPassPhrase = privPassPhrase;
	}

	public String getPrivprotocol() {
		return privprotocol;
	}

	public void setPrivprotocol(String privprotocol) {
		this.privprotocol = privprotocol;
	}
	
}
