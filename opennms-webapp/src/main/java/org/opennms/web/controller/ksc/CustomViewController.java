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
package org.opennms.web.controller.ksc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Category;
import org.opennms.core.utils.ThreadCategory;
import org.opennms.netmgt.config.KSC_PerformanceReportFactory;
import org.opennms.netmgt.config.kscReports.Graph;
import org.opennms.netmgt.config.kscReports.Report;
import org.opennms.netmgt.config.kscReports.ReportsList;
import org.opennms.netmgt.model.PrefabGraph;
import org.opennms.netmgt.model.OnmsResource;
import org.opennms.web.MissingParameterException;
import org.opennms.web.acegisecurity.Authentication;
import org.opennms.web.graph.KscResultSet;
import org.opennms.web.svclayer.KscReportService;
import org.opennms.web.svclayer.ResourceService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

public class CustomViewController extends AbstractController implements InitializingBean {
    
    private KSC_PerformanceReportFactory m_kscReportFactory;
    private KscReportService m_kscReportService;
    private ResourceService m_resourceService;
    private int m_defaultGraphsPerLine = 0;

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String[] requiredParameters = new String[] { "report or domain", "type" };
      
        // Get Form Variable
        String report_type = request.getParameter("type");
        if (report_type == null) {
            throw new MissingParameterException("type", requiredParameters);
        }
      
        String r_index = request.getParameter("report");
        String domain = request.getParameter("domain");
        int report_index = 0;
        if (r_index != null) {
            report_index = Integer.parseInt(r_index);
        } else if (domain == null) {
            throw new MissingParameterException("report or domain", requiredParameters);
        }
      
        String override_timespan = request.getParameter("timespan");
        String override_graphtype = request.getParameter("graphtype");
        if ((override_timespan == null) || (override_timespan.equals("null"))) {
            override_timespan = "none";
        }
        if ((override_graphtype == null) || (override_graphtype.equals("null"))) {
            override_graphtype = "none";
        }
      
        // Load report to view 
        Report report = null;
        if ("node".equals(report_type)) {
            report = getKscReportService().buildNodeReport(report_index);
        } else if ("domain".equals(report_type)) {
            report = getKscReportService().buildDomainReport(domain);
        } else if ("custom".equals(report_type)) {
            ReportsList reports_list = KSC_PerformanceReportFactory.getConfiguration();
            report = reports_list.getReport(report_index);
        } else {
            throw new IllegalArgumentException("value to 'type' parameter of '" + report_type + "' is not supported.  Must be one of: node, domain, or custom");
        }
      
        if (report == null) {
            throw new ServletException("Report does not exist");
        }
      
        // Define the possible graph options 
        PrefabGraph[] graph_options = new PrefabGraph[0];
      
        if (report.getGraphCount() > 0) {
            Set<PrefabGraph> prefabGraphs = new HashSet<PrefabGraph>();
            
            for (int i = 0; i < report.getGraphCount(); i++) {
                Graph graph = report.getGraph(i);
                OnmsResource resource = getKscReportService().getResourceFromGraph(graph);
                prefabGraphs.addAll(Arrays.asList(getResourceService().findPrefabGraphsForResource(resource)));
            }
            
            graph_options = prefabGraphs.toArray(new PrefabGraph[prefabGraphs.size()]);

            if (graph_options.length > 1) {
                Arrays.sort(graph_options);
            }

            /*
            if ("custom".equals(report_type) && "node".equals(resource.getResourceType().getName())) {
                graph_options = getResourceService().findPrefabGraphsForChildResources(resource.getParent(), "nodeSnmp", "interfaceSnmp");
            } else {
                graph_options = getResourceService().findPrefabGraphsForChildResources(resource.getParent(), "interfaceSnmp");
            }
            */
      
            // Get default graph type from first element of graph_options
            if (("node".equals(report_type) || "domain".equals(report_type))
                    && "none".equals(override_graphtype)
                    && graph_options.length > 0) {
                override_graphtype = graph_options[0].getName();
                if (log().isDebugEnabled()) {
                    log().debug("custom_view: setting default graph type to "
                                + override_graphtype);
                }
            }

            /*
            if (graph_options.length > 1) {
                Arrays.sort(graph_options);
            }
            */
        }
        
        ArrayList<KscResultSet> resultSets = new ArrayList<KscResultSet>(report.getGraphCount());
        for (int i = 0; i < report.getGraphCount(); i++) {
            Graph current_graph = report.getGraph(i);
            
            OnmsResource resource = getKscReportService().getResourceFromGraph(current_graph);

            String display_graphtype = null;
            if ("none".equals(override_graphtype)) {
                display_graphtype = current_graph.getGraphtype();
            } else {
                display_graphtype = override_graphtype;
            }
            
            PrefabGraph display_graph = getResourceService().getPrefabGraph(display_graphtype);
            
            // gather start/stop time information
            String display_timespan = null;
            if ("none".equals(override_timespan)) {
                display_timespan = current_graph.getTimespan();
            } else {
                display_timespan = override_timespan;
            }
            Calendar begin_time = Calendar.getInstance();
            Calendar end_time = Calendar.getInstance();
            KSC_PerformanceReportFactory.getBeginEndTime(display_timespan, begin_time, end_time);
            
            KscResultSet resultSet = new KscResultSet(current_graph.getTitle(), begin_time.getTime(), end_time.getTime(), resource, display_graph);
            resultSets.add(resultSet);
        }

        ModelAndView modelAndView = new ModelAndView("KSC/customView");

        modelAndView.addObject("reportType", report_type);
        modelAndView.addObject("domain", domain);
        modelAndView.addObject("report", r_index);
        
        modelAndView.addObject("title", report.getTitle());
        modelAndView.addObject("resultSets", resultSets);
        
        if (report.getShow_timespan_button()) {
            if ("none".equals(override_timespan) || !getKscReportService().getTimeSpans(true).containsKey(override_timespan)) {
                modelAndView.addObject("timeSpan", "none");
            } else {
                modelAndView.addObject("timeSpan", override_timespan);
            }
            modelAndView.addObject("timeSpans", getKscReportService().getTimeSpans(true));
        } else {
            // Make sure it's null so the pulldown list isn't shown
            modelAndView.addObject("timeSpan", null);
        }

        if (report.getShow_graphtype_button()) {
            LinkedHashMap<String, String> graphTypes = new LinkedHashMap<String, String>();
            graphTypes.put("none", "none");
            for (PrefabGraph graph_option : graph_options) {
                graphTypes.put(graph_option.getName(), graph_option.getName());
            }
            
            if ("none".equals(override_graphtype) || !graphTypes.containsKey(override_graphtype)) {
                modelAndView.addObject("graphType", "none");
            } else {
                modelAndView.addObject("graphType", override_graphtype);
            }
            modelAndView.addObject("graphTypes", graphTypes);
        } else {
            // Make sure it's null so the pulldown list isn't shown
            modelAndView.addObject("graphType", null);
        }
        
        modelAndView.addObject("showCustomizeButton", !request.isUserInRole(Authentication.READONLY_ROLE));
        modelAndView.addObject("graphsPerLine", getDefaultGraphsPerLine());
        
        return modelAndView;
    }

    private Category log() {
        return ThreadCategory.getInstance(getClass());
    }

    public KSC_PerformanceReportFactory getKscReportFactory() {
        return m_kscReportFactory;
    }

    public void setKscReportFactory(KSC_PerformanceReportFactory kscReportFactory) {
        m_kscReportFactory = kscReportFactory;
    }

    public int getDefaultGraphsPerLine() {
        return m_defaultGraphsPerLine;
    }

    public void setDefaultGraphsPerLine(int defaultGraphsPerLine) {
        if (defaultGraphsPerLine <= 0) {
            throw new IllegalArgumentException("property defaultGraphsPerLine must be greater than zero");
        }
        m_defaultGraphsPerLine = defaultGraphsPerLine;
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
        if (m_defaultGraphsPerLine == 0) {
            throw new IllegalStateException("property defaultGraphsPerLine must be set");
        }
    }

}
