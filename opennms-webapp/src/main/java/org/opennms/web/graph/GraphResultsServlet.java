//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2002-2003 The OpenNMS Group, Inc.  All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified 
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
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

package org.opennms.web.graph;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opennms.core.resource.Vault;
import org.opennms.web.element.NetworkElementFactory;
import org.opennms.web.performance.GraphResource;
import org.opennms.web.performance.GraphResourceType;
import org.opennms.web.performance.PerformanceModel;
import org.opennms.web.response.ResponseTimeModel;
import org.opennms.web.MissingParameterException;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Changes the label of a node, throws an event signalling that change, and then
 * redirects the user to a web page displaying that node's details.
 * 
 * @author <a href="dj@gregor.com">DJ Gregor</a>
 */
public class GraphResultsServlet extends HttpServlet {
    private PerformanceModel m_performanceModel;
    private ResponseTimeModel m_responseTimeModel;
    private RelativeTimePeriod[] m_periods;

    public void init() throws ServletException {
        WebApplicationContext m_webAppContext = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
        m_performanceModel = (PerformanceModel) m_webAppContext.getBean("performanceModel", PerformanceModel.class);
        m_responseTimeModel = (ResponseTimeModel) m_webAppContext.getBean("responseTimeModel", ResponseTimeModel.class);

	m_periods = RelativeTimePeriod.getDefaultPeriods();
    }

    public void doPost(HttpServletRequest request,
                       HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    public void doGet(HttpServletRequest request,
                       HttpServletResponse response)
            throws ServletException, IOException {
        String graphType = request.getParameter("type");
        if (graphType == null) {
            throw new MissingParameterException("type",
                                                new String[] { "type" } );
        }

        GraphResults graphResults = new GraphResults();
	String[] reports = request.getParameterValues("reports");
	int nodeId = -1;
	String nodeIdString = request.getParameter("node");
	String resource = request.getParameter("resource");
        String domain = request.getParameter("domain");
        String resourceType = request.getParameter("resourceType");
	GraphModel model;
        String view;

        if ("performance".equals(graphType) && nodeIdString != null) {
	    //model = m_performanceModel;
	    view = "/performance/results.jsp";

	    String[] requiredParameters = new String[] { "reports",
                    "node", "resourceType", "resource" };
	    /*
             * If no reports were passed in, we are going to
             * choosereportanddate.jsp, which will allow the user to choose
             * which report, or tell the user if there are no reports available.
	     */
	    if (reports == null) {
                view = "/performance/choosereportanddate.jsp";
	    }


	    if (nodeIdString == null) {
		throw new MissingParameterException("node",
						    requiredParameters);
	    }
	    try {
		nodeId = Integer.parseInt(nodeIdString);
	    } catch (NumberFormatException e) {
		throw new ServletException("Could not parse node parameter "
					   + "into an integer", e);
	    }
            if (resourceType == null) {
                throw new MissingParameterException("resourceType",
                                                    requiredParameters);
            }

            if (resource == null) {
                throw new MissingParameterException("resource",
                                                    requiredParameters);
            }
            
            GraphResourceType rt =
                m_performanceModel.getResourceTypeByName(resourceType);
            graphResults.setResourceTypeLabel(rt.getLabel());

            GraphResource r =
                m_performanceModel.getResourceForNodeResourceResourceType(nodeId,
                                                                          resource,
                                                                          resourceType);
            graphResults.setResourceLabel(r.getLabel());
            
            model = rt.getModel();
	} else if ("performance".equals(graphType) && domain != null) {
	    model = m_performanceModel;
	    view = "/performance/domainResults.jsp";

	    String[] requiredParameters = new String[] { "reports", "domain",
                    "resource" };

	    if (reports == null) {
		throw new MissingParameterException("reports",
						    requiredParameters);
	    }

	    if (domain == null) {
		throw new MissingParameterException("domain",
						    requiredParameters);
	    }

	    if (resource == null) {
		throw new MissingParameterException("resource",
						    requiredParameters);
	    }
            
            graphResults.setResourceTypeLabel("Interface");
            graphResults.setResourceLabel(resource);
	} else if ("response".equals(graphType)) {
	    model = m_responseTimeModel;
	    view = "/response/results.jsp";

	    String[] requiredParameters = new String[] { "reports", "node",
							 "resource" };

	    if (reports == null) {
		throw new MissingParameterException("reports",
						    requiredParameters);
	    }

	    if (nodeIdString == null) {
		throw new MissingParameterException("node",
						    requiredParameters);
	    }
	    try {
		nodeId = Integer.parseInt(nodeIdString);
	    } catch (NumberFormatException e) {
		throw new ServletException("Could not parse node parameter "
					   + "into an integer: "
                                           + e.getMessage(), e);
	    }

	    if (resource == null) {
                throw new MissingParameterException("resource",
                                                    requiredParameters);
	    }
            
            graphResults.setResourceTypeLabel("Interface");
            graphResults.setResourceLabel(resource);
	} else {
	    throw new ServletException("Unsupported graph type \"" + graphType
				       + "\"");
	}

	// see if the start and end time were explicitly set as params    
	String start = request.getParameter("start");
	String end = request.getParameter("end");

	String relativeTime = request.getParameter("relativetime");
        
	if ((start == null || end == null) && relativeTime != null) {
	    RelativeTimePeriod period =
                RelativeTimePeriod.getPeriodByIdOrDefault(m_periods,
                                                          relativeTime,
                                                          m_periods[0]);
            
            long[] times = period.getStartAndEndTimes();
            start = Long.toString(times[0]);
            end = Long.toString(times[1]);
	}

	if (start == null || end == null) {
	    String startMonth = request.getParameter("startMonth");
	    String startDate  = request.getParameter("startDate");
	    String startYear  = request.getParameter("startYear");
	    String startHour  = request.getParameter("startHour");

	    String endMonth = request.getParameter("endMonth");
	    String endDate  = request.getParameter("endDate");
	    String endYear  = request.getParameter("endYear");
	    String endHour  = request.getParameter("endHour");

	    if (startMonth == null || startDate == null || startYear == null
		|| startHour == null || endMonth == null || endDate == null
		|| endYear == null   || endHour == null ) {
		throw new MissingParameterException("startMonth", new String[] {
							"startMonth",
							"startDate",
							"startYear",
							"startHour",
							"endMonth",
							"endDate",
							"endYear",
							"endHour" } );
	    }

	    Calendar startCal = Calendar.getInstance();
	    startCal.set( Calendar.MONTH, Integer.parseInt( startMonth ));
	    startCal.set( Calendar.DATE, Integer.parseInt( startDate ));
	    startCal.set( Calendar.YEAR, Integer.parseInt( startYear ));
	    startCal.set( Calendar.HOUR_OF_DAY, Integer.parseInt( startHour ));
	    startCal.set( Calendar.MINUTE, 0 );
	    startCal.set( Calendar.SECOND, 0 );
	    startCal.set( Calendar.MILLISECOND, 0 );

	    Calendar endCal = Calendar.getInstance();
	    endCal.set( Calendar.MONTH, Integer.parseInt( endMonth ));
	    endCal.set( Calendar.DATE, Integer.parseInt( endDate ));
	    endCal.set( Calendar.YEAR, Integer.parseInt( endYear ));
	    endCal.set( Calendar.HOUR_OF_DAY, Integer.parseInt( endHour ));
	    endCal.set( Calendar.MINUTE, 0 );
	    endCal.set( Calendar.SECOND, 0 );
	    endCal.set( Calendar.MILLISECOND, 0 );

	    start = Long.toString(startCal.getTime().getTime());
	    end = Long.toString(endCal.getTime().getTime());
	}

	// gather information for displaying around the image
	Date startDate = new Date(Long.parseLong(start));
	Date endDate   = new Date(Long.parseLong(end));

	graphResults.setModel(model);
	if (nodeIdString != null && nodeId > -1) {
	    graphResults.setNodeId(nodeId);
	    try {
                graphResults.setNodeLabel(NetworkElementFactory.getNodeLabel(nodeId));
            } catch (SQLException e) {
                SQLErrorCodeSQLExceptionTranslator translator =
                    new SQLErrorCodeSQLExceptionTranslator();
                throw translator.translate("Getting node label for node " + nodeId, null, e);
            }
        }
	if (domain != null) {
	    graphResults.setDomain(domain);
        }
        graphResults.setResourceType(resourceType);
        graphResults.setResource(resource);
	graphResults.setReports(reports);
	graphResults.setStart(startDate);
	graphResults.setEnd(endDate);
	graphResults.setRelativeTime(relativeTime);
	graphResults.setRelativeTimePeriods(m_periods);

	if(nodeIdString != null && nodeId > -1 && reports != null) {
            graphResults.initializeGraphs();
        } else if (domain != null) {
            graphResults.initializeDomainGraphs();
        }
        
	request.setAttribute("results", graphResults);

        // forward the request for proper display
        RequestDispatcher dispatcher =
	    getServletContext().getRequestDispatcher(view);
        dispatcher.forward(request, response);

    }
}
