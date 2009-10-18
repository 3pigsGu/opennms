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
package org.opennms.web.svclayer.support;

import java.util.ArrayList;
import java.util.Calendar;

import org.opennms.netmgt.config.databaseReports.ReportParm;
import org.opennms.netmgt.dao.DatabaseReportConfigDao;
import org.opennms.web.svclayer.DatabaseReportCriteriaService;

public class DefaultDatabaseReportCriteriaService implements
        DatabaseReportCriteriaService {
    
    DatabaseReportConfigDao m_dao;

    public DatabaseReportCriteria getCriteria(String id) {
       
        DatabaseReportCriteria criteria = new DatabaseReportCriteria();
        
        criteria.setLogo("logo");
        criteria.setMailFormat("SVG");
        criteria.setPersist(true);
        criteria.setSendMail(true);
        
        ReportParm[] dates = m_dao.getDates(id);
        if (dates.length > 0) {
            ArrayList<DatabaseReportDateParm> dateParms = new ArrayList<DatabaseReportDateParm>();;
            for (int i = 0 ; i < dates.length ; i++ ) {
                DatabaseReportDateParm dateParm = new DatabaseReportDateParm();
                dateParm.setDisplayName(dates[i].getDisplayName());
                dateParm.setName(dates[i].getDisplayName());
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(System.currentTimeMillis());
                //cal.add(Calendar.YEAR, -1);
                dateParm.setDate(cal.getTime());
                dateParms.add(dateParm);

            }
            
            criteria.setDates(dateParms);
        }
        
        ReportParm[] categories = m_dao.getReportCategories(id);
        if (categories.length > 0) {
            ArrayList<DatabaseReportCategoryParm> catParms = new ArrayList<DatabaseReportCategoryParm>();;
            for (int i = 0 ; i < categories.length ; i++ ) {
                DatabaseReportCategoryParm catParm = new DatabaseReportCategoryParm();
                catParm.setDisplayName(categories[i].getDisplayName());
                catParm.setName(categories[i].getName());
                catParms.add(catParm);
            }
            criteria.setCategories(catParms);
        }

//        UserFactory.init();
//        UserManager userFactory = UserFactory.getInstance();
//        criteria.setEmail(userFactory.getEmail(request.getRemoteUser()));


        return criteria;
    }
    
    public void setDatabaseReportDao(DatabaseReportConfigDao databaseReportDao) {
        m_dao = databaseReportDao;
    }

}
