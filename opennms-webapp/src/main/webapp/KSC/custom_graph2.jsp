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

--%>

<%@page language="java"
	contentType="text/html"
	session="true"
	import="java.util.*,
		org.opennms.web.*,
		org.opennms.web.performance.*,
		java.util.Calendar,
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
    String nodeId = request.getParameter("node");
    String intfId = request.getParameter("intf");
    String domain = request.getParameter("domain");
    ArrayList intfs;
    if( nodeId != null ) {
        intfs = this.model.getQueryableInterfacesForNode(nodeId);
    } else if (domain != null) {
        intfs = this.model.getQueryableInterfacesForDomain(domain);
    } else {
        throw new MissingParameterException( "node or domain", new String[] {"node or domain", "endUrl"} );
    }
    if( intfId == null ) {
        intfId = "";
    }
    
    Collections.sort(intfs);        
%>

<jsp:include page="/includes/header.jsp" flush="false" >
  <jsp:param name="title" value="Key SNMP Customized Performance Reports" />
  <jsp:param name="headTitle" value="Choose Interface" />
  <jsp:param name="headTitle" value="Performance" />
  <jsp:param name="headTitle" value="Reports" />
  <jsp:param name="breadcrumb" value="<a href='report/index.jsp'>Reports</a>" />
  <jsp:param name="breadcrumb" value="<a href='KSC/index.jsp'>KSC Reports</a>" />
  <jsp:param name="breadcrumb" value="Custom Graph" />
</jsp:include>

  <script language="Javascript" type="text/javascript" >
      function validateInterface()
      {
          var isChecked = false
          for( i = 0; i < document.report.intf.length; i++ )
          {
              //make sure something is checked before proceeding
              if (document.report.intf[i].selected)
              {
                  isChecked=true;
              }
          }
  
          if (!isChecked)
          {
              alert("Please check the interfaces that you would like to report on.");
          }
          return isChecked;
      }
  
      function submitForm()
      {
          if(validateInterface())
          {
              document.report.submit();
          }
      }
  </script>

<h3 align="center"> Customized Report - Graph Definition </h3>


<table width="100%" align="center">
  <tr>
    <td align="center">
      <form method="get" name="report" action="KSC/custom_graph3.jsp" >
        <input type=hidden name="node" value="<%=nodeId%>">
	<input type=hidden name="domain" value="<%=domain%>">
        <table>
          <tr>
            <td>
                <h3>Choose an Interface to Query</h3>
            </td>
          </tr>

          <tr>
            <td>
                <select name="intf" size="10">
                  <% if(nodeId != null) { %>
                      <option value="">Node-level Performance Data</option>
                  <% } %>		      
                  <% for(Iterator i = intfs.iterator(); i.hasNext(); ) { %>
                      <% String intf = (String) i.next(); %>
                      <% String label = (nodeId == null ? intf : this.model.getHumanReadableNameForIfLabel(Integer.parseInt(nodeId), intf)); %>
                      <% if (intfId.equals(intf)) { %>
                          <option value="<%=intf%>" SELECTED><%=label%></option>
                      <% } else { %>
                          <option value="<%=intf%>"><%=label%></option>
                      <% } %>
                  <% } %>
              </select>
            </td>
          </tr>

          <tr>
            <td >
                <input type="button" value="Submit" onclick="submitForm()" />
            </td>
          </tr>
        </table>
      </form>
    </td>
  </tr>
</table>

<jsp:include page="/includes/footer.jsp" flush="false" />
