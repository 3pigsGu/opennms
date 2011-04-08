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
// Modifications:
//
// 2007 Aug 23: Use RrdTestUtils.initialize to initialize the RRD
//              subsystem. - dj@opennms.org
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
package org.opennms.netmgt.collectd;

import java.io.IOException;
import java.io.InputStream;

import javax.transaction.TransactionManager;

import junit.framework.TestCase;

import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.opennms.netmgt.config.CollectdConfigFactory;
import org.opennms.netmgt.config.DataCollectionConfigFactory;
import org.opennms.netmgt.config.DataSourceFactory;
import org.opennms.netmgt.config.DatabaseSchemaConfigFactory;
import org.opennms.netmgt.config.JMXDataCollectionConfigFactory;
import org.opennms.netmgt.config.SnmpPeerFactory;
import org.opennms.netmgt.dao.CollectorConfigDao;
import org.opennms.netmgt.dao.FilterDao;
import org.opennms.netmgt.dao.castor.DefaultDataCollectionConfigDao;
import org.opennms.netmgt.dao.support.JdbcFilterDao;
import org.opennms.netmgt.dao.support.RrdTestUtils;
import org.opennms.netmgt.filter.FilterDaoFactory;
import org.opennms.netmgt.mock.MockDatabase;
import org.opennms.test.ConfigurationTestUtils;
import org.opennms.test.mock.MockLogAppender;
import org.opennms.test.mock.MockUtil;
import org.springframework.core.io.InputStreamResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class CollectorConfigDaoImplTest extends TestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MockUtil.println("------------ Begin Test "+getName()+" --------------------------");
        MockLogAppender.setupLogging();

        MockDatabase m_db = new MockDatabase();
        //        m_db.populate(m_network);

        final TransactionAwareDataSourceProxy proxy = new TransactionAwareDataSourceProxy(m_db);
		DataSourceFactory.setInstance(proxy);

		PlatformTransactionManager mgr = new DataSourceTransactionManager(proxy);
        
        FilterDao dao = FilterDaoFactory.getInstance();
        if (dao instanceof JdbcFilterDao) {
        	((JdbcFilterDao)dao).setTransactionTemplate(new TransactionTemplate(mgr));
        }
    }

    @Override
    public void runTest() throws Throwable {
        super.runTest();
        MockLogAppender.assertNoWarningsOrGreater();
    }

    @Override
    protected void tearDown() throws Exception {
        MockUtil.println("------------ End Test "+getName()+" --------------------------");
        super.tearDown();
    }

    private InputStream getInputStreamForFile(String fileName) {
        return getClass().getResourceAsStream(fileName);
    }

    public void testInstantiate() throws MarshalException, ValidationException, IOException, Exception {
        initialize();
    }

    private CollectorConfigDao initialize() throws IOException, MarshalException, ValidationException, Exception {
        System.setProperty("opennms.home", ConfigurationTestUtils.getDaemonEtcDirectory().getParentFile().getAbsolutePath());
        RrdTestUtils.initialize();

        InputStream stream = null;

        stream = getInputStreamForFile("/org/opennms/netmgt/config/test-database-schema.xml");
        DatabaseSchemaConfigFactory.setInstance(new DatabaseSchemaConfigFactory(stream));
        stream.close();

        stream = getInputStreamForFile("/org/opennms/netmgt/config/jmx-datacollection-testdata.xml");
        JMXDataCollectionConfigFactory.setInstance(new JMXDataCollectionConfigFactory(stream));
        stream.close();

        stream = getInputStreamForFile("/org/opennms/netmgt/config/snmp-config.xml");
        SnmpPeerFactory.setInstance(new SnmpPeerFactory(stream));
        stream.close();

        stream = getInputStreamForFile("/org/opennms/netmgt/config/datacollection-config.xml");
        DefaultDataCollectionConfigDao dataCollectionDao = new DefaultDataCollectionConfigDao();
        dataCollectionDao.setConfigResource(new InputStreamResource(stream));
        dataCollectionDao.afterPropertiesSet();
        DataCollectionConfigFactory.setInstance(dataCollectionDao);
        stream.close();

        stream = getInputStreamForFile("/org/opennms/netmgt/config/collectd-testdata.xml");
        CollectdConfigFactory.setInstance(new CollectdConfigFactory(stream, "localhost", false));
        stream.close();

        return new CollectorConfigDaoImpl();
    }
}
