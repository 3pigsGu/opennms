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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opennms.netmgt.config.KSC_PerformanceReportFactory;
import org.opennms.netmgt.config.kscReports.Graph;
import org.opennms.netmgt.config.kscReports.Report;
import org.opennms.netmgt.model.OnmsResource;
import org.opennms.web.svclayer.KscReportService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

public class FormProcReportController extends AbstractController implements InitializingBean {
    
    private KSC_PerformanceReportFactory m_kscReportFactory;
    private KscReportService m_kscReportService;

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Get The Customizable Report 
        Report report = getKscReportFactory().getWorkingReport();

        // Get Form Variables
        String action = request.getParameter("action");
        String report_title = request.getParameter("report_title");
        String show_timespan = request.getParameter("show_timespan");
        String show_graphtype = request.getParameter("show_graphtype");
        String g_index = request.getParameter("graph_index");
        int graph_index = Integer.parseInt(g_index);
        int graphs_per_line = Integer.parseInt(request.getParameter("graphs_per_line"));
     
        // Save the global variables into the working report
        report.setTitle(report_title);
        if (show_graphtype == null) {
            report.setShow_graphtype_button(false);
        } else {
            report.setShow_graphtype_button(true);
        }
        
        if (show_timespan == null) {
            report.setShow_timespan_button(false);
        } else {
            report.setShow_timespan_button(true);
        } 
        
        if (graphs_per_line > 0) {
            report.setGraphs_per_line(graphs_per_line);
        } else {
            report.setGraphs_per_line(0);
        } 

        if (action.equals("Save")) {
            // The working model is complete now... lets save working model to configuration file 
            saveFactory();
        } else {
            if (action.equals("AddGraph") || action.equals("ModGraph")) {
                // Making a graph change... load it into the working area (the graph_index of -1 indicates a new graph)
                getKscReportFactory().loadWorkingGraph(graph_index);
            } else {
                if (action.equals("DelGraph")) { 
                    report.removeGraph(report.getGraph(graph_index));
                } else {
                    throw new ServletException("Invalid Argument for Customize Form Action.");
                }
            }
        }
        
        if (action.equals("Save")) {
            return new ModelAndView("redirect:/KSC/index.htm");
        } else if (action.equals("DelGraph")) {
            return new ModelAndView("redirect:/KSC/customReport.htm");
        } else if (action.equals("AddGraph")) {
            return new ModelAndView("redirect:/KSC/customGraphChooseParentResource.htm");
        } else if (action.equals("ModGraph")) {
            Graph graph = getKscReportFactory().getWorkingGraph();
            OnmsResource resource = getKscReportService().getResourceFromGraph(graph);
            return new ModelAndView("redirect:/KSC/customGraphEditDetails.htm", "resourceId", resource.getId());
        } else {
            throw new IllegalArgumentException("parameter action of '" + action + "' is not supported.  Must be one of: Save, Cancel, Update, AddGraph, or DelGraph");
        }
    }
    
    private void saveFactory() throws ServletException {    
        try {
            getKscReportFactory().unloadWorkingReport();  // first copy working report into report arrays
            getKscReportFactory().saveCurrent();          // Now unmarshal array to file
        } catch (Exception e) {
            throw new ServletException("Couldn't save KSC_PerformanceReportFactory.", e);
        }
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

    public void afterPropertiesSet() throws Exception {
        if (m_kscReportFactory == null) {
            throw new IllegalStateException("property kscReportFactory must be set");
        }
        if (m_kscReportService == null) {
            throw new IllegalStateException("property kscReportService must be set");
        }
    }

}
