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
// Created: October 5th, 2009
//
// Copyright (C) 2009 The OpenNMS Group, Inc.  All rights reserved.
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
package org.opennms.web.svclayer;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;
import org.opennms.netmgt.dao.castor.DefaultDatabaseReportConfigDao;
import org.opennms.test.ConfigurationTestUtils;
import org.opennms.web.command.DatabaseReportCriteriaCommand;
import org.opennms.web.svclayer.support.DefaultDatabaseReportCriteriaService;
import org.springframework.core.io.InputStreamResource;

public class DefaultDatabaseReportCriteriaServiceTest {
    
    private static final String ID = "defaultCalendarReport";
    private static final String DESCRIPTION = "default calendar report";
    private static final String DATE_DISPLAY_NAME = "end date";
    private static final String DATE_NAME = "endDate";
    
    private DefaultDatabaseReportConfigDao m_dao;
    private DatabaseReportCriteriaService m_criteriaService;
    
    @Before
    public void setupDao() throws Exception {

        m_dao = new DefaultDatabaseReportConfigDao();
        InputStream in = ConfigurationTestUtils.getInputStreamForConfigFile("/database-reports.xml");
        m_dao.setConfigResource(new InputStreamResource(in));
        m_dao.afterPropertiesSet();
        
        m_criteriaService = new DefaultDatabaseReportCriteriaService();
        m_criteriaService.setDatabaseReportConfigDao(m_dao);
        
    }
    
    
    @Test
    public void testDatabaseReportService() throws Exception {
        
        // FIXME this test is failing
        
        DatabaseReportCriteriaCommand criteria = m_criteriaService.getCriteria(ID, "admin");
        
        assertEquals(criteria.getDates().size(),1);
        assertEquals(criteria.getDates().get(0).getDisplayName(),DATE_DISPLAY_NAME);
        assertEquals(criteria.getDates().get(0).getName(),DATE_NAME);
        
    }

}
