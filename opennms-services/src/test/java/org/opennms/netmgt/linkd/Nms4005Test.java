/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2011-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.linkd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opennms.netmgt.nb.TestNetworkBuilder.R1_NAME;
import static org.opennms.netmgt.nb.TestNetworkBuilder.R2_NAME;
import static org.opennms.netmgt.nb.TestNetworkBuilder.R3_NAME;
import static org.opennms.netmgt.nb.TestNetworkBuilder.R4_NAME;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opennms.core.spring.BeanUtils;
import org.opennms.core.test.MockLogAppender;
import org.opennms.core.test.db.annotations.JUnitTemporaryDatabase;
import org.opennms.core.test.snmp.annotations.JUnitSnmpAgent;
import org.opennms.core.test.snmp.annotations.JUnitSnmpAgents;
import org.opennms.netmgt.config.LinkdConfig;
import org.opennms.netmgt.config.linkd.Package;
import org.opennms.netmgt.dao.api.DataLinkInterfaceDao;
import org.opennms.netmgt.dao.api.NodeDao;
import org.opennms.netmgt.dao.api.SnmpInterfaceDao;
import org.opennms.netmgt.model.DataLinkInterface;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.nb.Nms4005NetworkBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

public class Nms4005Test extends LinkdTestBuilder {

    @Autowired
    TransactionTemplate m_transactionTemplate; 

    Nms4005NetworkBuilder builder = new Nms4005NetworkBuilder();

    @Autowired
    private Linkd m_linkd;

    private LinkdConfig m_linkdConfig;

    @Autowired
    private NodeDao m_nodeDao;

    @Autowired
    private SnmpInterfaceDao m_snmpInterfaceDao;

    @Autowired
    private DataLinkInterfaceDao m_dataLinkInterfaceDao;

    @Override
    public void afterPropertiesSet() throws Exception {
        BeanUtils.assertAutowiring(this);
    }

	@Before
    public void setUp() throws Exception {
        Properties p = new Properties();
        p.setProperty("log4j.logger.org.hibernate.SQL", "WARN");
        p.setProperty("log4j.logger.org.hibernate.cfg", "WARN");
        p.setProperty("log4j.logger.org.springframework","WARN");
        p.setProperty("log4j.logger.com.mchange.v2.resourcepool", "WARN");
        MockLogAppender.setupLogging(p);

        for (Package pkg : Collections.list(m_linkdConfig.enumeratePackage())) {
            pkg.setForceIpRouteDiscoveryOnEthernet(true);
        }        

        m_transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                m_nodeDao.save(builder.getR1());
                m_nodeDao.save(builder.getR2());
                m_nodeDao.save(builder.getR3());
                m_nodeDao.save(builder.getR4());
                m_nodeDao.flush();
            }
        });
    }

    @After
    @Override // Override this so that we can manually wrap it in a transaction if necessary
    public void tearDown() throws Exception {
        m_transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                for (final OnmsNode node : m_nodeDao.findAll()) {
                    m_nodeDao.delete(node);
                }
                m_nodeDao.flush();
            }
        });
    }

/*
 *  (3)10.1.1.2<>R1<>10.1.3.1 (2)---(1) 10.1.3.2 <>R3<>
 *                10.1.2.1                          <>R3<>
 *               (1)                             <>R3<>
 *                |                              <>R3<>10.1.4.1 (2)---(1) 10.1.4.2<>R4
 *               (1)                             <>R3<>
 *             10.1.2.2                          <>R3<>
 *                <>R2<>10.1.5.1 (2)---(3) 10.1.5.2 <>R3<>
 */
    @Test(timeout=60000)
    @JUnitSnmpAgents(value={
            @JUnitSnmpAgent(host="10.1.1.2", port=161, resource="classpath:linkd/nms4005/10.1.1.2-walk.txt"),
            @JUnitSnmpAgent(host="10.1.2.2", port=161, resource="classpath:linkd/nms4005/10.1.2.2-walk.txt"),
            @JUnitSnmpAgent(host="10.1.3.2", port=161, resource="classpath:linkd/nms4005/10.1.3.2-walk.txt"),
            @JUnitSnmpAgent(host="10.1.4.2", port=161, resource="classpath:linkd/nms4005/10.1.4.2-walk.txt")
    })
    @JUnitTemporaryDatabase
    public void testNms4005Network() throws Exception {
        final OnmsNode cisco1 = m_nodeDao.findByForeignId("linkd", R1_NAME);
        final OnmsNode cisco2 = m_nodeDao.findByForeignId("linkd", R2_NAME);
        final OnmsNode cisco3 = m_nodeDao.findByForeignId("linkd", R3_NAME);
        final OnmsNode cisco4 = m_nodeDao.findByForeignId("linkd", R4_NAME);

        assertTrue(m_linkd.scheduleNodeCollection(cisco1.getId()));
        assertTrue(m_linkd.scheduleNodeCollection(cisco2.getId()));
        assertTrue(m_linkd.scheduleNodeCollection(cisco3.getId()));
        assertTrue(m_linkd.scheduleNodeCollection(cisco4.getId()));

        assertTrue(m_linkd.runSingleSnmpCollection(cisco1.getId()));
        assertTrue(m_linkd.runSingleSnmpCollection(cisco2.getId()));
        assertTrue(m_linkd.runSingleSnmpCollection(cisco3.getId()));
        assertTrue(m_linkd.runSingleSnmpCollection(cisco4.getId()));

        assertTrue(m_linkd.runSingleLinkDiscovery("example1"));

        final List<DataLinkInterface> ifaces = m_dataLinkInterfaceDao.findAll();
        for (final DataLinkInterface link: ifaces) {
            printLink(link);
        }
        assertEquals("we should have found 4 data links", 4, ifaces.size());
        //Rerun collection and discovery must be all the same...
        assertTrue(m_linkd.runSingleSnmpCollection(cisco1.getId()));
        assertTrue(m_linkd.runSingleSnmpCollection(cisco2.getId()));
        assertTrue(m_linkd.runSingleSnmpCollection(cisco3.getId()));
        assertTrue(m_linkd.runSingleSnmpCollection(cisco4.getId()));

        assertTrue(m_linkd.runSingleLinkDiscovery("example1"));

        assertEquals("we should have found 4 data links", 4, ifaces.size());

    }

    /**
     * This test is the same as {@link #testNms4005Network()} except that it spawns multiple threads
     * for each scan to ensure that the upsert code is working properly.
     */
    @Test(timeout=60000)
    @JUnitSnmpAgents(value={
            @JUnitSnmpAgent(host="10.1.1.2", port=161, resource="classpath:linkd/nms4005/10.1.1.2-walk.txt"),
            @JUnitSnmpAgent(host="10.1.2.2", port=161, resource="classpath:linkd/nms4005/10.1.2.2-walk.txt"),
            @JUnitSnmpAgent(host="10.1.3.2", port=161, resource="classpath:linkd/nms4005/10.1.3.2-walk.txt"),
            @JUnitSnmpAgent(host="10.1.4.2", port=161, resource="classpath:linkd/nms4005/10.1.4.2-walk.txt")
    })
    @JUnitTemporaryDatabase
    public void testNms4005NetworkWithThreads() throws Exception {

        // Run this test in a separate transaction from the @Before method to make sure
        // that all of the data is present in the database before the test begins.
        m_transactionTemplate.execute(new TransactionCallbackWithoutResult() {

            @Override
            protected void doInTransactionWithoutResult(TransactionStatus arg0) {
                final OnmsNode cisco1 = m_nodeDao.findByForeignId("linkd", R1_NAME);
                final OnmsNode cisco2 = m_nodeDao.findByForeignId("linkd", R2_NAME);
                final OnmsNode cisco3 = m_nodeDao.findByForeignId("linkd", R3_NAME);
                final OnmsNode cisco4 = m_nodeDao.findByForeignId("linkd", R4_NAME);


                assertTrue(m_linkd.scheduleNodeCollection(cisco1.getId()));
                assertTrue(m_linkd.scheduleNodeCollection(cisco2.getId()));
                assertTrue(m_linkd.scheduleNodeCollection(cisco3.getId()));
                assertTrue(m_linkd.scheduleNodeCollection(cisco4.getId()));

                final int NUMBER_OF_THREADS = 20;

                List<Thread> waitForMe = new ArrayList<Thread>();
                for (int i = 0; i < NUMBER_OF_THREADS; i++) {
                    Thread thread = new Thread("NMS-4005-Test-Thread-" + i) {
                        @Override
                        public void run() {
                            assertTrue(m_linkd.runSingleSnmpCollection(cisco1.getId()));
                        }
                    };
                    thread.start();
                    waitForMe.add(thread);
                }
                for (Thread thread : waitForMe) {
                    try { thread.join(); } catch (InterruptedException e) {}
                }
                waitForMe.clear();
                for (int i = 0; i < NUMBER_OF_THREADS; i++) {
                    Thread thread = new Thread("NMS-4005-Test-Thread-" + i) {
                        @Override
                        public void run() {
                            assertTrue(m_linkd.runSingleSnmpCollection(cisco2.getId()));
                        }
                    };
                    thread.start();
                    waitForMe.add(thread);
                }
                for (Thread thread : waitForMe) {
                    try { thread.join(); } catch (InterruptedException e) {}
                }
                waitForMe.clear();
                for (int i = 0; i < NUMBER_OF_THREADS; i++) {
                    Thread thread = new Thread("NMS-4005-Test-Thread-" + i) {
                        @Override
                        public void run() {
                            assertTrue(m_linkd.runSingleSnmpCollection(cisco3.getId()));
                        }
                    };
                    thread.start();
                    waitForMe.add(thread);
                }
                for (Thread thread : waitForMe) {
                    try { thread.join(); } catch (InterruptedException e) {}
                }
                waitForMe.clear();
                for (int i = 0; i < NUMBER_OF_THREADS; i++) {
                    Thread thread = new Thread("NMS-4005-Test-Thread-" + i) {
                        @Override
                        public void run() {
                            assertTrue(m_linkd.runSingleSnmpCollection(cisco4.getId()));
                        }
                    };
                    thread.start();
                    waitForMe.add(thread);
                }
                for (Thread thread : waitForMe) {
                    try { thread.join(); } catch (InterruptedException e) {}
                }
                waitForMe.clear();

                assertTrue(m_linkd.runSingleLinkDiscovery("example1"));

                final List<DataLinkInterface> ifaces = m_dataLinkInterfaceDao.findAll();
                for (final DataLinkInterface link: ifaces) {
                    printLink(link);
                }
                assertEquals("we should have found 4 data links", 4, ifaces.size());
            }
        });
    }
}
