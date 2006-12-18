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

package org.opennms.web.svclayer.support;

import java.io.File;
import java.io.FileReader;

import org.opennms.netmgt.config.C3P0ConnectionFactory;
import org.opennms.netmgt.config.CategoryFactory;
import org.opennms.netmgt.config.DataSourceFactory;
import org.opennms.netmgt.config.SiteStatusViewsFactory;
import org.opennms.netmgt.config.SurveillanceViewsFactory;
import org.opennms.netmgt.config.ViewsDisplayFactory;
import org.opennms.web.svclayer.ProgressMonitor;
import org.opennms.web.svclayer.SimpleWebTable;
import org.opennms.web.svclayer.SurveillanceService;
import org.opennms.web.svclayer.SurveillanceTable;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;

public class DefaultSurveillanceServiceIntegrationTest extends AbstractTransactionalDataSourceSpringContextTests {
    
    private SurveillanceService m_surveillanceService;
    
    
    
    public DefaultSurveillanceServiceIntegrationTest() throws Exception {
    	File f = new File("src/test/opennms-home");
		System.setProperty("opennms.home", f.getAbsolutePath());

		File rrdDir = new File("target/test/opennms-home/share/rrd");
		if (!rrdDir.exists()) {
			rrdDir.mkdirs();
		}
		System.setProperty("opennms.logs.dir", "src/test/opennms-home/logs");
		System.setProperty("rrd.base.dir", rrdDir.getAbsolutePath());
    }
    
    
    /**
     * This parm gets autowired from the application context by TDSCT (the base class for this test)
     * pretty cool Spring Framework trickery
     * @param svc
     */
    public void setSurveillanceService(SurveillanceService svc) {
        m_surveillanceService = svc;
    }

    @Override
    protected String[] getConfigLocations() {
        return new String[] {
                "META-INF/opennms/applicationContext-dao.xml",
                "org/opennms/web/svclayer/applicationContext-svclayer.xml",
        };
    }
    
    public void testBogus() {
        // Empty test so JUnit doesn't complain about not having any tests to run
    }
    
    public void FIXMEtestCreateSurveillanceServiceTableUsingViewName() {
        String viewName = "default";
        SimpleWebTable table = m_surveillanceService.createSurveillanceTable(viewName, new ProgressMonitor() {

			public void beginNextPhase(String string) {
				// TODO Auto-generated method stub
				
			}

			public void finished() {
				// TODO Auto-generated method stub
				
			}

			public void setPhaseCount(int i) {
				// TODO Auto-generated method stub
				
			}
        	
        });
        
        
        assertEquals("default", table.getTitle());
    }

}
