package org.opennms.netmgt.dao.db;

import junit.framework.AssertionFailedError;

import org.opennms.test.ThrowableAnticipator;

/**
 * <p>Tests for the setSnmpInterfaceKeysOnUpdate trigger.</p>
 * 
 * <p>Cases that aren't tested:
 * <ul>
 *   <li>Null snmp</li>
 * </ul>
 * </p>
 * @author djgregor
 *
 */
public class TriggerSetSnmpInterfaceKeysOnUpdateTest extends
        PopulatedTemporaryDatabaseTestCase {
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        executeSQL("INSERT INTO node (nodeId, nodeCreateTime) VALUES ( 1, now() )");
    }
    
    public void testSameSnmpInterfaceIdDifferentNodeId() {
        executeSQL("INSERT INTO snmpInterface (id, nodeId, ipAddr, snmpIfIndex) VALUES ( 1, 1, '1.1.1.1', 1 )");
        executeSQL("INSERT INTO ipInterface (id, nodeId, ipAddr, ifIndex, snmpInterfaceId) VALUES ( 1, 1, '1.1.1.1', 1, 1 )");
        
        // Add new node, update snmpInterface, and verify
        executeSQL("INSERT INTO node (nodeId, nodeCreateTime) VALUES ( 2, now() )");
        assertEquals("snmpInterface update count", 1, jdbcTemplate.update("UPDATE snmpInterface SET nodeId = 2 WHERE nodeID = 1 AND ipAddr = '1.1.1.1'"));
        assertEquals("snmpIfIndex after update", 1, jdbcTemplate.queryForInt("SELECT snmpIfIndex FROM snmpInterface"));
        assertEquals("snmpInterfaceId before update", 1, jdbcTemplate.queryForInt("SELECT snmpInterfaceId FROM ipInterface"));
        
        assertEquals("ipInterface update count", 1, jdbcTemplate.update("UPDATE ipInterface SET nodeId = 2 WHERE nodeID = 1 AND ipAddr = '1.1.1.1'"));
        assertEquals("snmpInterfaceId after update", 1, jdbcTemplate.queryForInt("SELECT snmpInterfaceId FROM ipInterface"));
    }
    
    public void testSameSnmpInterfaceIdDifferentIfIndex() {
        executeSQL("INSERT INTO snmpInterface (id, nodeId, ipAddr, snmpIfIndex) VALUES ( 1, 1, '1.1.1.1', 1 )");
        executeSQL("INSERT INTO ipInterface (id, nodeId, ipAddr, ifIndex, snmpInterfaceId) VALUES ( 1, 1, '1.1.1.1', 1, 1 )");
        
        // Update snmpInterface and verify
        assertEquals("snmpInterface update count", 1, jdbcTemplate.update("UPDATE snmpInterface SET snmpIfIndex = 2 WHERE nodeID = 1 AND ipAddr = '1.1.1.1'"));
        assertEquals("snmpIfIndex after update", 2, jdbcTemplate.queryForInt("SELECT snmpIfIndex FROM snmpInterface"));
        assertEquals("snmpInterfaceId before update", 1, jdbcTemplate.queryForInt("SELECT snmpInterfaceId FROM ipInterface"));
        
        assertEquals("ipInterface update count", 1, jdbcTemplate.update("UPDATE ipInterface SET ifIndex = 2 WHERE nodeID = 1 AND ipAddr = '1.1.1.1'"));
        assertEquals("snmpInterfaceId after update", 1, jdbcTemplate.queryForInt("SELECT snmpInterfaceId FROM ipInterface"));
    }
    
    
    
    public void testSameSnmpInterfaceIdDifferentIfIndexNull() {
        executeSQL("INSERT INTO snmpInterface (nodeId, ipAddr, snmpIfIndex) VALUES ( 1, '1.1.1.1', 1 )");
        executeSQL("INSERT INTO ipInterface (nodeId, ipAddr, ifIndex, snmpInterfaceId) VALUES ( 1, '1.1.1.1', 1, 1 )");
        
        // Update snmpInterface and verify
        assertEquals("snmpInterface update count", 1, jdbcTemplate.update("UPDATE snmpInterface SET snmpIfIndex = 2 WHERE nodeID = 1 AND ipAddr = '1.1.1.1'"));
        assertEquals("snmpIfIndex after update", 2, jdbcTemplate.queryForInt("SELECT snmpIfIndex FROM snmpInterface"));
        assertEquals("snmpInterfaceId before update", 1, jdbcTemplate.queryForInt("SELECT snmpInterfaceId FROM ipInterface"));
        
        assertEquals("ipInterface update count", 1, jdbcTemplate.update("UPDATE ipInterface SET ifIndex = 2 WHERE nodeID = 1 AND ipAddr = '1.1.1.1'"));
        assertEquals("snmpInterfaceId after update", 1, jdbcTemplate.queryForInt("SELECT snmpInterfaceId FROM ipInterface"));
    }

    public void testSetIpInterfaceIfIndexLikeCapsdDoes() throws Exception {
        executeSQL("INSERT INTO ipInterface (id, nodeId, ipAddr, ifIndex) VALUES ( 1, 1, '1.1.1.1', null )");
        executeSQL("INSERT INTO snmpInterface (id, nodeId, ipAddr, snmpIfIndex) VALUES ( 1, 1, '1.1.1.1', 1)");
        
        assertEquals("ifIndex", null, jdbcTemplate.queryForObject("SELECT ifIndex FROM ipinterface", Integer.class));
        executeSQL("UPDATE ipInterface SET ifIndex = 1 WHERE nodeID = 1 AND ipAddr = '1.1.1.1'");
        assertEquals("ifIndex", 1, jdbcTemplate.queryForInt("SELECT ifIndex FROM ipinterface"));

        assertEquals("snmpInterfaceId", 1, jdbcTemplate.queryForInt("SELECT snmpInterfaceId FROM ipInterface WHERE nodeID = ?", 1));
    }
    
    public void testSetIpInterfaceIfIndexLikeCapsdDoesBadIfIndex() throws Exception {
        executeSQL("INSERT INTO ipInterface (nodeId, ipAddr, ifIndex) VALUES ( 1, '1.1.1.1', null )");
        executeSQL("INSERT INTO snmpInterface (nodeId, ipAddr, snmpIfIndex) VALUES ( 1, '1.1.1.1', 1)");
        
        assertEquals("ifIndex", null, jdbcTemplate.queryForObject("SELECT ifIndex FROM ipinterface", Integer.class));

        ThrowableAnticipator ta = new ThrowableAnticipator();
        ta.anticipate(new AssertionFailedError(ThrowableAnticipator.IGNORE_MESSAGE));
        try {
                executeSQL("UPDATE ipInterface SET ifIndex = 2 WHERE nodeID = 1 AND ipAddr = '1.1.1.1'");
        } catch (Throwable t) {
                ta.throwableReceived(t);
        } finally {
                ta.verifyAnticipated();
        }
    }
    
    public void testSetIpInterfaceIfIndexLikeCapsdButOppositeOrder() throws Exception {
        executeSQL("INSERT INTO snmpInterface (nodeId, ipAddr, snmpIfIndex) VALUES ( 1, '1.1.1.1', 1)");
        executeSQL("INSERT INTO ipInterface (nodeId, ipAddr, ifIndex) VALUES ( 1, '1.1.1.1', 1 )");
        
        assertEquals("snmpInterfaceId", 1, jdbcTemplate.queryForInt("SELECT snmpInterfaceId FROM ipInterface WHERE nodeID = ?", 1));
    }

    public void testSetIpInterfaceIfIndexLikeCapsdButOppositeOrderUpdateWithBadIfIndex() throws Exception {
        executeSQL("INSERT INTO snmpInterface (nodeId, ipAddr, snmpIfIndex) VALUES ( 1, '1.1.1.1', 1)");
        executeSQL("INSERT INTO ipInterface (nodeId, ipAddr, ifIndex) VALUES ( 1, '1.1.1.1', 1 )");
        
        assertEquals("snmpInterfaceId", 1, jdbcTemplate.queryForInt("SELECT snmpInterfaceId FROM ipInterface WHERE nodeID = ?", 1));
        
        ThrowableAnticipator ta = new ThrowableAnticipator();
        ta.anticipate(new AssertionFailedError(ThrowableAnticipator.IGNORE_MESSAGE));
        try {
                executeSQL("UPDATE ipInterface SET ifIndex = 2 WHERE nodeID = 1 AND ipAddr = '1.1.1.1'");
        } catch (Throwable t) {
                ta.throwableReceived(t);
        } finally {
                ta.verifyAnticipated();
        }
    }

}
