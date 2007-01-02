package org.opennms.web.controller.ksc;

import java.util.ArrayList;
import java.util.Calendar;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opennms.netmgt.config.KSC_PerformanceReportFactory;
import org.opennms.netmgt.config.kscReports.Graph;
import org.opennms.netmgt.config.kscReports.Report;
import org.opennms.netmgt.model.PrefabGraph;
import org.opennms.netmgt.model.OnmsResource;
import org.opennms.web.graph.KscResultSet;
import org.opennms.web.svclayer.KscReportService;
import org.opennms.web.svclayer.ResourceService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

public class CustomReportController extends AbstractController implements InitializingBean {
    
    private KSC_PerformanceReportFactory m_kscReportFactory;
    private KscReportService m_kscReportService;
    private ResourceService m_resourceService;

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Get Form Variables
        Report report = getKscReportFactory().getWorkingReport();
        if (report == null) {
            throw new IllegalStateException("There is no working report");
        }
//        int report_index = getReportFactory().getWorkingReportIndex();      
//        String number_graphs[] = {"1", "2", "3", "4", "5", "6"};
        
        ArrayList<KscResultSet> resultSets = new ArrayList<KscResultSet>(report.getGraphCount());
        for (int i = 0; i < report.getGraphCount(); i++) { 
            Graph current_graph = report.getGraph(i); 
            PrefabGraph display_graph = getResourceService().getPrefabGraph(current_graph.getGraphtype());
            
            OnmsResource resource = getKscReportService().getResourceFromGraph(current_graph);

            Calendar begin_time = Calendar.getInstance();
            Calendar end_time = Calendar.getInstance();
            KSC_PerformanceReportFactory.getBeginEndTime(current_graph.getTimespan(), begin_time, end_time); 

            KscResultSet resultSet = new KscResultSet(current_graph.getTitle(), begin_time.getTime(), end_time.getTime(), resource, display_graph);
            resultSets.add(resultSet);
        }

        ModelAndView modelAndView = new ModelAndView("KSC/customReport");

        modelAndView.addObject("showTimeSpan", report.getShow_timespan_button());
        modelAndView.addObject("showGraphType", report.getShow_graphtype_button());
        
        modelAndView.addObject("title", report.getTitle());
        modelAndView.addObject("resultSets", resultSets);
        
        return modelAndView;
    }

    public KSC_PerformanceReportFactory getKscReportFactory() {
        return m_kscReportFactory;
    }

    public void setKscReportFactory(KSC_PerformanceReportFactory kscReportFactory) {
        m_kscReportFactory = kscReportFactory;
    }

    public KscReportService getKscReportService() {
        return m_kscReportService;
    }

    public void setKscReportService(KscReportService kscReportService) {
        m_kscReportService = kscReportService;
    }

    public ResourceService getResourceService() {
        return m_resourceService;
    }

    public void setResourceService(ResourceService resourceService) {
        m_resourceService = resourceService;
    }

    public void afterPropertiesSet() throws Exception {
        if (m_kscReportFactory == null) {
            throw new IllegalStateException("property kscReportFactory must be set");
        }
        if (m_kscReportService == null) {
            throw new IllegalStateException("property kscReportService must be set");
        }
        if (m_resourceService == null) {
            throw new IllegalStateException("property resourceService must be set");
        }
    }

}
