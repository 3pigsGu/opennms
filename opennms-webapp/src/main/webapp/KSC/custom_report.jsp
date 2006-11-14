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
		java.io.*,
		org.opennms.web.Util,
		org.opennms.web.graph.ResourceId,
		org.opennms.web.performance.*,
		org.opennms.web.graph.PrefabGraph,
		org.opennms.web.element.NetworkElementFactory,
		org.opennms.netmgt.config.kscReports.*,
		org.opennms.netmgt.config.KSC_PerformanceReportFactory
	"
%>

<%@ include file="/WEB-INF/jspf/KSC/init2.jspf" %> 
<%@ include file="/WEB-INF/jspf/graph-common.jspf"%>

<%
    // Get Form Variables
    Report report = this.reportFactory.getWorkingReport();
    int report_index = this.reportFactory.getWorkingReportIndex();      
    String number_graphs[] = {"1", "2", "3", "4", "5", "6"};
%>


<jsp:include page="/includes/header.jsp" flush="false" >
  <jsp:param name="title" value="Key SNMP Customized Performance Reports" />
  <jsp:param name="headTitle" value="Performance" />
  <jsp:param name="headTitle" value="Reports" />
  <jsp:param name="headTitle" value="KSC" />
  <jsp:param name="location" value="KSC Reports" />
  <jsp:param name="breadcrumb" value="<a href='report/index.jsp'>Reports</a>" />
  <jsp:param name="breadcrumb" value="<a href='KSC/index.jsp'>KSC Reports</a>" />
  <jsp:param name="breadcrumb" value="Custom Report" />
</jsp:include>


<%-- A script to Save the file --%>
<script language="Javascript" type="text/javascript">
    function saveReport()
    {
        document.customize_form.action.value = "Save"; 
        document.customize_form.submit();
    }
 
    function addNewGraph()
    {
        document.customize_form.action.value = "AddGraph"; 
        document.customize_form.submit();
    }
 
    function modifyGraph(graph_index)
    {
        document.customize_form.action.value = "ModGraph"; 
        document.customize_form.graph_index.value = graph_index; 
        document.customize_form.submit();
    }
 
    function deleteGraph(graph_index)
    {
        document.customize_form.action.value = "DelGraph";
        document.customize_form.graph_index.value = graph_index; 
        document.customize_form.submit();
    }
 
    function cancelReport()
    {
        var fer_sure = confirm("Do you really want to cancel configuration changes?");
        if (fer_sure==true) {
            setLocation("index.jsp");
        }
    }
    
</script>


<h3 align="center">Customized Report Configuration</h3>

    <form name="customize_form" method="get" action="KSC/form_proc_report.jsp">
        <input type=hidden name="action" value="none">
        <input type=hidden name="graph_index" value="-1">

    <table width="100" align="center">
        <tr align = "center">
            <td> 
                <table align="center">
                    <tr>
                        <td>
                            Title: 
                        </td>
                        <td>
                            <input type="text" name="report_title" value="<%=report.getTitle()%>" size="80" maxlength="80">
                        </td>
                    </tr>
                </table>
            </td> 
        </tr>
        <tr>
            <td>
 

            <table width="100%" border="2">
                <% int graph_count = report.getGraphCount();
                   for (int i=0; i< graph_count; i++) { 
                       int nodeId = 0;
                       String parentResourceType;
                       String parentResource;
                       String resourceType;
                       String resource;
                       Graph current_graph = report.getGraph(i); 
                       String curr_domain = current_graph.getDomain();
                       String intf = current_graph.getInterfaceId();
                       PrefabGraph display_graph = (PrefabGraph) this.model.getQuery(current_graph.getGraphtype());
                       
                       if(current_graph.getNodeId() != null && !current_graph.getNodeId().equals("null")) {
                           nodeId = Integer.parseInt(current_graph.getNodeId());
                           parentResourceType = "node";
                           parentResource = Integer.toString(nodeId);
                           if (intf == null || "".equals(intf)) {
                               resourceType = "node";
                               resource = "";
                           } else {
                               resourceType = "interface";
                               resource = intf;
                           }
                       } else {
                           parentResourceType = "domain";
                           parentResource = current_graph.getDomain();
                           resourceType = "interface";
                           resource = intf;
                       }
                       
                       ResourceId resourceId =
                           new ResourceId(parentResourceType, parentResource,
                                          resourceType, resource);
                       String resourceIdEncoded =
                           Util.encode(resourceId.getResourceId());
                       
                %>
            
                    <tr>
                        <td>
                            <input type="button" value="Modify" onclick="modifyGraph(<%=i%>)"><br>
                            <input type="button" value="Delete" onclick="deleteGraph(<%=i%>)">
                        </td>
                        <td align="right">
                            <h3> <%=current_graph.getTitle()%> <br>
                            <%if(nodeId > 0) {%>    
                                Node: <a href="element/node.jsp?node=<%=nodeId%>">
                                <%=NetworkElementFactory.getNodeLabel(nodeId)%></a><br>
                                <% if(intf != null ) { %>
                                    Interface: <%=this.model.getHumanReadableNameForIfLabel(nodeId, intf)%>
                                <% } %>
                            <%} else {%>
                                Domain: <%=curr_domain%><br>
                                Interface: <a href="element/nodelist.jsp?listInterfaces&ifAlias=<%=intf%>"><%=intf%></a><br/>
                            <%}%>
                            </h3>

                            <%-- gather start/stop time information --%>
                            <%  
                                Calendar begin_time = Calendar.getInstance();
                                Calendar end_time = Calendar.getInstance();
                                this.reportFactory.getBeginEndTime(current_graph.getTimespan(), begin_time, end_time); 
                                String start = Long.toString( begin_time.getTime().getTime() );
                                String startPretty = new Date( Long.parseLong( start )).toString();
                                String end = Long.toString( end_time.getTime().getTime() );
                                String endPretty = new Date( Long.parseLong( end )).toString();
                             %>

                            <b>From</b> <%=startPretty%> <br>
                            <b>To</b> <%=endPretty%>
                        </td>
              
                        <td align="left">
                            <img src="graph/graph.png?resourceId=<%= resourceIdEncoded %>&amp;report=<%=display_graph.getName()%>&amp;start=<%=start%>&amp;end=<%=end%>"/>
                        </td>
                    </tr>
                <% }  //end for loop %> 
            </table>  


            </td> 
        </tr>
        <tr>
            <td> 
                <input type="button" value="Add New Graph" onclick="addNewGraph()" alt="Add a new graph to the Report"><br>
            </td> 
        </tr>
        <tr>
            <td>
                <table align="center">
                     <tr>
                         <td>
                             <input type=checkbox name="show_timespan"  <% if (report.getShow_timespan_button()) {%> checked <%}%> >
                         </td>
                         <td>
                             Show Timespan Button (allows global manipulation of report timespan)
                         </td>
                     </tr>
                     <tr>
                         <td>
                             <input type=checkbox name="show_graphtype"  <% if (report.getShow_graphtype_button()) {%> checked <%}%> >
                         </td>
                         <td>
                             Show Graphtype Button (allows global manipulation of report prefabricated graph type)
                         </td>
                     </tr>
                </table> 
            </td> 
        </tr>
        <tr>
            <td> 
                <table align="center">
                    <tr>
                        <td>
                            <input type="button" value="Save" onclick="saveReport()" alt="Save the Report to File"><br>
                        </td>
                        <td>
                            <input type="button" value="Cancel" onclick="cancelReport()" alt="Cancel the report configuration"><br>
                        </td>
                    </tr>
                </table>
            </td> 
        </tr>
        <tr>
            <td>
              Please make sure to save the added graph with the report, as the first save only adds it to the the present view.
            </td>
        </tr>

    </table>

    </form>

<jsp:include page="/includes/footer.jsp" flush="false"/>
