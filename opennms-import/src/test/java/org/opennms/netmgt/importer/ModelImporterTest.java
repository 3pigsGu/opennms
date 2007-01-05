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
//      OpenNMS Licensing       <license@opennms.org>
//      http://www.opennms.org/
//      http://www.opennms.com/
//
package org.opennms.netmgt.importer;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.Iterator;

import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.opennms.netmgt.config.SnmpPeerFactory;
import org.opennms.netmgt.config.modelimport.Category;
import org.opennms.netmgt.config.modelimport.Interface;
import org.opennms.netmgt.config.modelimport.ModelImport;
import org.opennms.netmgt.config.modelimport.MonitoredService;
import org.opennms.netmgt.config.modelimport.Node;
import org.opennms.netmgt.dao.AbstractDaoTestCase;
import org.opennms.netmgt.importer.specification.ImportVisitor;
import org.opennms.netmgt.importer.specification.SpecFile;
import org.opennms.netmgt.model.OnmsAssetRecord;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.style.ToStringCreator;

/**
 * Unit test for ModelImport application.
 */
public class ModelImporterTest extends AbstractDaoTestCase {

    private ModelImporter m_importer;

    public void setUp() throws Exception {
        setPopulate(false);
        setRunTestsInTransaction(false);
        
        super.setUp();
        
        initSnmpPeerFactory();


        m_importer = new ModelImporter();
        m_importer.setTransactionTemplate(m_transTemplate);
        m_importer.setDistPollerDao(getDistPollerDao());
        m_importer.setNodeDao(getNodeDao());
        m_importer.setIpInterfaceDao(getIpInterfaceDao());
        m_importer.setMonitoredServiceDao(getMonitoredServiceDao());
        m_importer.setServiceTypeDao(getServiceTypeDao());
        m_importer.setAssetRecordDao(getAssetRecordDao());
        m_importer.setCategoryDao(getCategoryDao());
    }

    private void initSnmpPeerFactory() throws IOException, MarshalException, ValidationException {
        Reader rdr = new StringReader("<?xml version=\"1.0\"?>\n" + 
                "<snmp-config port=\"161\" retry=\"0\" timeout=\"2000\"\n" + 
                "             read-community=\"public\" \n" + 
                "                 version=\"v1\">\n" + 
                "\n" + 
                "</snmp-config>");
        
        SnmpPeerFactory.setInstance(new SnmpPeerFactory(rdr));
    }


    class CountingVisitor implements ImportVisitor {
        

        private int m_modelImportCount;
        private int m_modelImportCompleted;
        private int m_nodeCount;
        private int m_nodeCompleted;
        private int m_ifaceCount;
        private int m_ifaceCompleted;
        private int m_svcCount;
        private int m_svcCompleted;
        private int m_categoryCount;
        private int m_categoryCompleted;
        
        public int getModelImportCount() {
            return m_modelImportCount;
        }
        
        public int getModelImportCompletedCount() {
            return m_modelImportCompleted;
        }
        
        public int getNodeCount() {
            return m_nodeCount;
        }
        
        public int getNodeCompletedCount() {
            return m_nodeCompleted;
        }
        
        public int getInterfaceCount() {
            return m_ifaceCount;
        }
        
        public int getInterfaceCompletedCount() {
            return m_ifaceCompleted;
        }
        
        public int getMonitoredServiceCount() {
            return m_svcCount;
        }
        
        public int getMonitoredServiceCompletedCount() {
            return m_svcCompleted;
        }
        
        public int getCategoryCount() {
            return m_categoryCount;
        }

        public int getCategoryCompletedCount() {
            return m_categoryCompleted;
        }

        public void visitModelImport(ModelImport mi) {
            m_modelImportCount++;
        }

        public void visitNode(Node node) {
            m_nodeCount++;
            assertEquals("apknd", node.getNodeLabel());
            assertEquals("4243", node.getForeignId());
        }

        public void visitInterface(Interface iface) {
            m_ifaceCount++;
        }

        public void visitMonitoredService(MonitoredService svc) {
            m_svcCount++;
        }

        public void visitCategory(Category category) {
            m_categoryCount++;
        }
        
        public String toString() {
            return (new ToStringCreator(this)
                .append("modelImportCount", getModelImportCount())
                .append("modelImportCompletedCount", getModelImportCompletedCount())
                .append("nodeCount", getNodeCount())
                .append("nodeCompletedCount", getNodeCompletedCount())
                .append("interfaceCount", getInterfaceCount())
                .append("interfaceCompletedCount", getInterfaceCompletedCount())
                .append("monitoredServiceCount", getMonitoredServiceCount())
                .append("monitoredServiceCompletedCount", getMonitoredServiceCompletedCount())
                .append("categoryCount", getCategoryCount())
                .append("categoryCompletedCount", getCategoryCompletedCount())
                .toString());
        }

        public void completeModelImport(ModelImport modelImport) {
            m_modelImportCompleted++;
        }

        public void completeNode(Node node) {
            m_nodeCompleted++;
        }

        public void completeInterface(Interface iface) {
            m_ifaceCompleted++;
        }

        public void completeMonitoredService(MonitoredService svc) {
            m_svcCompleted++;
        }

        public void completeCategory(Category category) {
            m_categoryCompleted++;
        }
        
    }
    
    public void testVisit() throws Exception {

        SpecFile specFile = new SpecFile();
        specFile.loadResource(new ClassPathResource("/NewFile2.xml"));
        CountingVisitor visitor = new CountingVisitor();
        specFile.visitImport(visitor);
        verifyCounts(visitor);
    }
    
    public void testFindQuery() throws Exception {
        ModelImporter mi = m_importer;        
        String specFile = "/tec_dump.xml.smalltest";
        mi.importModelFromResource(new ClassPathResource(specFile));
        Collection c = m_importer.getAssetRecordDao().findAll();
        for (Iterator it = c.iterator(); it.hasNext();) {
            OnmsAssetRecord assetRecord = (OnmsAssetRecord) it.next();
            System.err.println(assetRecord.getAssetNumber());
        }
    }
    
    public void testPopulate() throws Exception {
        ModelImporter mi = m_importer;        
        String specFile = "/tec_dump.xml.smalltest";
        mi.importModelFromResource(new ClassPathResource(specFile));

        //Verify distpoller count
        assertEquals(1, mi.getDistPollerDao().countAll());
        
        //Verify node count
        assertEquals(10, mi.getNodeDao().countAll());
        
        //Verify ipinterface count
        assertEquals(30, mi.getIpInterfaceDao().countAll());
        
        //Verify ifservices count
        assertEquals(50, mi.getMonitoredServiceDao().countAll());
        
        //Verify service count
        assertEquals(3, mi.getServiceTypeDao().countAll());
    }
    
    /**
     * This test first bulk imports 10 nodes then runs update with 1 node missing
     * from the import file.
     * 
     * @throws ModelImportException
     */
    public void testDelete() throws Exception {
        
        //Initialize the database
        ModelImporter mi = m_importer;
        String specFile = "/tec_dump.xml.smalltest";
        mi.importModelFromResource(new ClassPathResource(specFile));
        
        assertEquals(10, mi.getNodeDao().countAll());
        
//        ImportVisitor visitor = new ImportAccountant();
//        mi.loadResource("/"+"tec_dump.xml.smalltest.delete");
//        mi.visitImport(visitor);
        
    }
    private void verifyCounts(CountingVisitor visitor) {
        //System.err.println(visitor);
        assertEquals(1, visitor.getModelImportCount());
        assertEquals(1, visitor.getNodeCount());
        assertEquals(3, visitor.getCategoryCount());
        assertEquals(4, visitor.getInterfaceCount());
        assertEquals(6, visitor.getMonitoredServiceCount());
        assertEquals(visitor.getModelImportCount(), visitor.getModelImportCompletedCount());
        assertEquals(visitor.getNodeCount(), visitor.getNodeCompletedCount());
        assertEquals(visitor.getCategoryCount(), visitor.getCategoryCompletedCount());
        assertEquals(visitor.getInterfaceCount(), visitor.getInterfaceCompletedCount());
        assertEquals(visitor.getMonitoredServiceCount(), visitor.getMonitoredServiceCompletedCount());
    }
    
    
}
