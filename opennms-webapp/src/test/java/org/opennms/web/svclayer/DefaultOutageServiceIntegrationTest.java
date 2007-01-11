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

package org.opennms.web.svclayer;

import java.io.File;
import java.util.Collection;
import java.util.Date;

import org.opennms.netmgt.model.OnmsOutage;
import org.opennms.web.svclayer.outage.OutageService;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;

public class DefaultOutageServiceIntegrationTest extends
		AbstractTransactionalDataSourceSpringContextTests {

	private static final int RANGE_LIMIT = 5;
	private OutageService outageService;
        
	public DefaultOutageServiceIntegrationTest() throws Exception {
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
	 * This get autowired by the base class
	 * @param outageService
	 */
	public void setOutageService(OutageService outageService) {
		this.outageService = outageService;
	}
	
	@Override
	protected String[] getConfigLocations() {
		return new String[] {
				"META-INF/opennms/applicationContext-dao.xml",
				"org/opennms/web/svclayer/applicationContext-svclayer.xml",
		};
	}

	public void FIXMEtestGetRangeOutages() {
		Collection<OnmsOutage> outages = outageService.getCurrentOutagesByRange(1,RANGE_LIMIT,"iflostservice","asc");
		assertFalse("Collection should not be emtpy", outages.isEmpty());
		assertEquals("Collection should be of size " + RANGE_LIMIT, RANGE_LIMIT, outages.size());
	}
	
	// More tests should be defined for these
	
	public void FIXMEtestGetSupressedOutages() {
		Collection<OnmsOutage> outages = outageService.getSuppressedOutages();
		assertTrue("Collection should be emtpy ", outages.isEmpty());
		
	}
	
	
	public void testloadOneOutage() {
			OnmsOutage outage = outageService.load(1);
			assertTrue("We loaded one outage ",outage.getId().equals(1));
	}
	
	public void FIXMEtestNoOfSuppressedOutages(){
		Integer outages = outageService.getSuppressedOutageCount();
		assertTrue("We should find suppressed messages ", outages == 0);
	}

	public void FIXMEtestSuppression() {
		Date now = new Date();
		
		Date time = now;
		//Load Outage manipulate and save it.
		OnmsOutage myOutage = outageService.load(new Integer(1));
		assertTrue("Loaded the outage ", myOutage.getId().equals(new Integer(1)));
		myOutage.setSuppressTime(time);
		outageService.update(myOutage);
		OnmsOutage fixedOutage = outageService.load(new Integer(1));
		
			
	}
	
	
}
