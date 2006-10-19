package org.opennms.report.availability;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.opennms.netmgt.config.C3P0ConnectionFactory;
import org.opennms.netmgt.config.DataSourceFactory;
import org.opennms.netmgt.mock.MockDatabase;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;
import org.springframework.transaction.annotation.Transactional;

public class AvailabilityReportSchedulerServiceTest extends AbstractTransactionalDataSourceSpringContextTests{
        private AvailabilityReportLocatorService m_locatorService;
        private AvailabilityReportSchedulerService m_schedulerService;
        private MockDatabase m_db;
        
        public AvailabilityReportSchedulerServiceTest() throws MarshalException, ValidationException, IOException, PropertyVetoException, SQLException {
            System.setProperty("opennms.home", "src/test/opennms-home");
            m_db = new MockDatabase();
            DataSourceFactory.setInstance(m_db);
        }
	
	@Override
	protected String[] getConfigLocations() {
		return new String[] {
				"META-INF/opennms/applicationContext-dao.xml",
				"org/opennms/report/svclayer/applicationContext-svclayer.xml" };
	}
        
        @Override
        protected void onTearDownAfterTransaction() throws Exception {
            super.onTearDownAfterTransaction();
            m_db.drop();
        }
        
	public void testBogus() {
	    // this is just here so that JUnit doesn't complain about not having any tests
        }
	
	@Transactional(readOnly=false)
	public void FIXMEtestScheduleReport() {
		Date date = new Date();
		m_schedulerService.Schedule("all", "html", "classic", date);
		assertNotNull(m_locatorService.locateReports());
		
		
	}


	public AvailabilityReportLocatorService getLocatorService() {
		return m_locatorService;
	}


	public void setLocatorService(AvailabilityReportLocatorService locatorService) {
		m_locatorService = locatorService;
	}


	public AvailabilityReportSchedulerService getSchedulerService() {
		return m_schedulerService;
	}


	public void setSchedulerService(
			AvailabilityReportSchedulerService schedulerService) {
		m_schedulerService = schedulerService;
	}
}
