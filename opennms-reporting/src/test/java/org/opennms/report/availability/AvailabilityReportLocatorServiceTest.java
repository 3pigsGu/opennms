package org.opennms.report.availability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.opennms.netmgt.dao.AvailabilityReportLocatorDao;
import org.opennms.netmgt.model.AvailabilityReportLocator;

import junit.framework.TestCase;
import static org.easymock.EasyMock.*;

public class AvailabilityReportLocatorServiceTest extends TestCase {

	AvailabilityReportLocatorService locatorService;
	private AvailabilityReportLocatorDao availabilityReportLocatorDao;
	
	protected void setUp() throws Exception {
		super.setUp();
		
		availabilityReportLocatorDao = createMock(AvailabilityReportLocatorDao.class);
		locatorService = new AvailabilityReportLocatorService();
		locatorService.setAvailabilityReportLocatorDao(availabilityReportLocatorDao);
		
		
		
		
	}
	
	public void testDelete() {
		
		// record expected calls
		availabilityReportLocatorDao.delete(1);
		
		// tell mock to match up actual call to expected calls
		replay(availabilityReportLocatorDao);
		
		// call service method that makes call to mock
		locatorService.deleteReport(1);
		
		// verify that all calls matched
		verify(availabilityReportLocatorDao);
	}
	
	public void testAdd() {
		
		AvailabilityReportLocator locator = new AvailabilityReportLocator();
		
//		record expected calls
		availabilityReportLocatorDao.save(locator);
		
		// tell mock to match up actual call to expected calls
		replay(availabilityReportLocatorDao);
		
		// call service method that makes call to mock
		locatorService.addReport(locator);
		
		// verify that all calls matched
		verify(availabilityReportLocatorDao);
		
	}
	
	public void testLocateReports() {
		
		List expectedReports = new ArrayList();
		
		expect(availabilityReportLocatorDao.findAll()).andReturn(expectedReports);
		
		replay(availabilityReportLocatorDao);
		
		Collection actualReports = locatorService.locateReports();
		
		verify(availabilityReportLocatorDao);
		
		assertSame("Expected loctedReports to be the same as the dao list", expectedReports, actualReports);
	}
	
	

}
