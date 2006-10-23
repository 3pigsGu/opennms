<%--

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
// Modifications:
//
// 2003 Feb 07: Fixed URLEncoder issues.
// 2002 Nov 26: Fixed breadcrumbs issue.
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

--%>

<%@page language="java"
	contentType="text/html"
	session="true"
	import="org.opennms.web.performance.*,
		org.opennms.web.*,
		org.opennms.web.graph.*,
    	org.opennms.web.element.NetworkElementFactory,
		java.io.File,
		java.util.List,
		org.opennms.netmgt.utils.RrdFileConstants,
		org.springframework.web.context.WebApplicationContext,
      	org.springframework.web.context.support.WebApplicationContextUtils
	"
%>

<%! 
    public PerformanceModel model = null;

    public void init() throws ServletException {
	    WebApplicationContext m_webAppContext = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
		this.model = (PerformanceModel) m_webAppContext.getBean("performanceModel", PerformanceModel.class);
    }
%>
 
<%
    String[] requiredParameters = new String[] {
        "node or domain", 
        "resourceType",
        "resource"
    };

    // optional parameter node
    String nodeIdString = request.getParameter("node");

    //optional parameter domain
    String domain = request.getParameter("domain");

    //required parameter resourceType
    String resourceTypeName = request.getParameter("resourceType");
    if (resourceTypeName == null) {
        throw new MissingParameterException("resourceType", requiredParameters);
    }
    
    //required parameter resource
    String resourceName = request.getParameter("resource");
    if (resourceName == null) {
        throw new MissingParameterException("resource", requiredParameters);
    }
    
	GraphResourceType resourceType =
	    model.getResourceTypeByName(resourceTypeName);

	GraphResource resource;
    
    String label = null;
    int nodeId = -1;
    if (nodeIdString != null) {
        nodeId = Integer.parseInt(nodeIdString);
        label = NetworkElementFactory.getNodeLabel(nodeId);
        resource =
	        model.getResourceForNodeResourceResourceType(nodeId,
        	                                             resourceName,
                                                         resourceTypeName);
    } else if (domain != null) {
        label = domain;
        resource =
		    model.getResourceForDomainResourceResourceType(domain,
    	    	                                           resourceName,
        	                                               resourceTypeName);
    } else {
        throw new MissingParameterException("node or domain",
                                            requiredParameters);
    }

    /*
    File rrdPath = null;
    File nodeDir = null;
    String rrdDir = null;
    if("".equals(intf) && nodeIdString != null) {
        rrdPath = new File(this.model.getRrdDirectory(), nodeIdString);
        rrdDir = nodeIdString;
    } else {
        if(nodeIdString != null) {
            nodeDir = new File(this.model.getRrdDirectory(), nodeIdString);
            rrdDir = nodeIdString + File.separator + intf;
        } else {
            nodeDir = new File(this.model.getRrdDirectory(), domain);
            rrdDir = domain + File.separator + intf;
        }
        rrdPath = new File(nodeDir, intf);
    }
    
    File[] rrds = rrdPath.listFiles(RrdFileConstants.RRD_FILENAME_FILTER);

    if (rrds == null) {
        this.log("Invalid rrd directory: " + rrdPath);
        throw new IllegalArgumentException("Invalid rrd directory: " + rrdPath);
    }
    */
    


%>

<jsp:include page="/includes/header.jsp" flush="false" >
  <jsp:param name="title" value="Custom Performance Reporting" />
  <jsp:param name="headTitle" value="Custom" />
  <jsp:param name="headTitle" value="Performance" />
  <jsp:param name="headTitle" value="Reports" />
  <jsp:param name="breadcrumb" value="<a href='report/index.jsp'>Reports</a>" />
  <jsp:param name="breadcrumb" value="<a href='performance/index.jsp'>Performance</a>" />
  <jsp:param name="breadcrumb" value="Custom" />
</jsp:include>

<h3>Step 2: Choose the Data Sources</h3> 

<% if(nodeIdString != null) { %>
  <% if("".equals(resourceName)) { %>
    Node: <%=label%>
  <% } else { %>
    Node: <%=label%> &nbsp;&nbsp; Interface: <%=this.model.getHumanReadableNameForIfLabel(nodeId, resourceName)%>
   <% } %>
<% } else { %>
  Domain: <%=label%> &nbsp;&nbsp; Interface: <%=resourceName%>
<% } %>

<form method="get" action="performance/adhoc3.jsp" >
  <%=Util.makeHiddenTags(request)%>

  <table width="100%" cellspacing="2" cellpadding="2" border="0">
    <% boolean anythingSelected = false; %>
    <% for(int dsindex=0; dsindex < 4; dsindex++ ) { %>
      <!-- Data Source <%=dsindex+1%> -->     
      <tr>
        <td valign="top">
          Data Source <%=dsindex+1%> <%=(dsindex==0) ? "(required)" : "(optional)"%>:<br>
          <select name="ds" size="6">
            <% for (GraphAttribute attribute : resource.getAttributes()) { %>
              <option <%= !anythingSelected ? "selected=\"\"" : "" %>>
                <%= Util.htmlify(attribute.getName()) %>
              </option>
	          <% anythingSelected = true; %>    
            <% } %>    
          </select>
        </td>

        <td valign="top">
          <table width="100%" cellspacing="0" cellpadding="2">
            <tr>
              <td width="5%">Title:</td>
              <td><input type="input" name="dstitle" value="DataSource <%=dsindex+1%>" /></td>
            </tr>

            <tr>
              <td width="5%">Color:</td> 
              <td> 
                <select name="color">
                  <option value="ff0000"<%=(dsindex==0) ? " selected=\"selected\"" : ""%>>Red</option>
                  <option value="00ff00"<%=(dsindex==1) ? " selected=\"selected\"" : ""%>>Green</option>
                  <option value="0000ff"<%=(dsindex==2) ? " selected=\"selected\"" : ""%>>Blue</option>
                  <option value="000000"<%=(dsindex==3) ? " selected=\"selected\"" : ""%>>Black</option>
                </select>
              </td>
            </tr>

            <tr>
              <td width="5%">Style:</td> 
              <td> 
                <select name="style">
                  <option value="LINE1">Thin Line</option>
                  <option value="LINE2" selected="selected">Medium Line</option>
                  <option value="LINE3">Thick Line</option>
                  <option value="AREA">Area</option>
                  <% if( dsindex > 0 ) { %>
                    <option value="STACK">Stack</option>
                  <% } %>
                </select>
              </td>
            </tr>

            <tr>
              <td width="5%">Value&nbsp;Type:</td> 
              <td> 
                <select name="agfunction">
                  <option value="AVERAGE" selected="selected">Average</option>
                  <option value="MIN">Minimum</option>
                  <option value="MAX">Maximum</option>
                </select>
              </td>
            </tr>
          </table>

      <tr><td colspan="2"><hr></td></tr>
    <% } %>
  </table> 

  <input type="submit" value="Next"/>
  <input type="reset" />
</form>

<jsp:include page="/includes/footer.jsp" flush="false" />
