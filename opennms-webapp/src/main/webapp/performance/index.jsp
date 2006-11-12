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
	import="java.util.*,
		org.opennms.web.Util,
		org.opennms.web.performance.*,
		org.opennms.web.ServletInitializer,
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
    PerformanceModel.QueryableNode[] nodes = this.model.getQueryableNodes();
    String[] domains = this.model.getQueryableDomains();
%>

<jsp:include page="/includes/header.jsp" flush="false" >
  <jsp:param name="title" value="Performance" />
  <jsp:param name="headTitle" value="Performance" />
  <jsp:param name="headTitle" value="Reports" />
  <jsp:param name="location" value="performance" />
  <jsp:param name="breadcrumb" value="<a href='report/index.jsp'>Reports</a>" />
  <jsp:param name="breadcrumb" value="Performance" />
</jsp:include>

<script language="Javascript" type="text/javascript" >
  function validateNode()
  {
      var isChecked = false
      for( i = 0; i < document.choose_node.parentResource.length; i++ )
      {
         //make sure something is checked before proceeding
         if (document.choose_node.parentResource[i].selected)
         {
            isChecked=true;
         }
      }

      if (!isChecked)
      {
          alert("Please check the node that you would like to report on.");
      }
      return isChecked;
  }

  function validateNodeAdhoc()
  {
      var isChecked = false
      for( i = 0; i < document.choose_node_adhoc.parentResource.length; i++ )
      {
         //make sure something is checked before proceeding
         if (document.choose_node_adhoc.parentResource[i].selected)
         {
            isChecked=true;
         }
      }

      if (!isChecked)
      {
          alert("Please check the node that you would like to report on.");
      }
      return isChecked;
  }

  function validateDomain()
  {
      var isChecked = false
      for( i = 0; i < document.choose_domain.parentResource.length; i++ )
      {
         //make sure something is checked before proceeding
         if (document.choose_domain.parentResource[i].selected)
         {
            isChecked=true;
         }
      }

      if (!isChecked)
      {
          alert("Please check the domain that you would like to report on.");
      }
      return isChecked;
  }

  function validateDomainAdhoc()
  {
      var isChecked = false
      for( i = 0; i < document.choose_domain_adhoc.parentResource.length; i++ )
      {
         //make sure something is checked before proceeding
         if (document.choose_domain_adhoc.parentResource[i].selected)
         {
            isChecked=true;
         }
      }

      if (!isChecked)
      {
          alert("Please check the domain that you would like to report on.");
      }
      return isChecked;
  }

  function submitNodeForm()
  {
      if (validateNode())
      {
          document.choose_node.submit();
      }
  }

  function submitNodeFormAdhoc()
  {
      if (validateNodeAdhoc())
      {
          document.choose_node_adhoc.submit();
      }
  }

  function submitDomainForm()
  {
      if (validateDomain())
      {
          document.choose_domain.submit();
      }
  }

  function submitDomainFormAdhoc()
  {
      if (validateDomainAdhoc())
      {
          document.choose_domain_adhoc.submit();
      }
  }
</script>

<div style="width: 30%; float: left;">
  <h3>Standard Node<br>Performance Reports</h3>

  <p>
    Choose a node for a standard performance report.
  </p>

  <form method="get" name="choose_node" action="graph/chooseresource.htm">
    <input type="hidden" name="reports" value="all" />
    <input type="hidden" name="parentResourceType" value="node" />

    <select name="parentResource" size="10">
      <% for( int i=0; i < nodes.length; i++ ) { %>
        <option value="<%=nodes[i].getNodeId()%>"><%=nodes[i].getNodeLabel()%></option>
      <% } %>
    </select>

    <br/>
    <br/>

    <input type="button" value="Start" onclick="submitNodeForm()"/>
  </form>

  <h3>Custom Node<br>Performance Reports</h3>

  <p>
    Choose a node for a custom performance report.
  </p>

  <form method="get" name="choose_node_adhoc" action="graph/chooseresource.htm">
    <input type="hidden" name="endUrl" value="performance/adhoc2.jsp"/>
    <input type="hidden" name="parentResourceType" value="node"/>
    <select name="parentResource" size="10">
      <% for( int i=0; i < nodes.length; i++ ) { %>
        <option value="<%=nodes[i].getNodeId()%>"><%=nodes[i].getNodeLabel()%></option>
      <% } %>
    </select>

    <br/>
    <br/>

    <input type="button" value="Start" onclick="submitNodeFormAdhoc()"/>
  </form>
</div>

<div style="width: 30%; float: left;">
<% if (domains.length > 0) { %>
  <h3>Standard Domain<br>Performance Reports</h3>

  <p>
    Choose a domain for a standard performance report.
  </p>

  <form method="get" name="choose_domain" action="graph/chooseresource.htm">
    <input type="hidden" name="reports" value="all" />
    <input type="hidden" name="parentResourceType" value="domain" />

    <select name="parentResource" size="10">
      <% for( int i=0; i < domains.length; i++ ) { %>
        <option value="<%=domains[i]%>"><%=domains[i]%></option>
      <% } %>
    </select>

    <br/>
    <br/>

    <input type="button" value="Start" onclick="submitDomainForm()"/>
  </form>

  <h3>Custom Domain<br>Performance Reports</h3>

  <p>
    Choose a domain for a custom performance report.
  </p>

  <form method="get" name="choose_domain_adhoc" action="graph/chooseresource.htm">
    <input type="hidden" name="endUrl" value="performance/adhoc2.jsp"/>
    <input type="hidden" name="parentResourceType" value="domain"/>
    <select name="parentResource" size="10">
      <% for( int i=0; i < domains.length; i++ ) { %>
        <option value="<%=domains[i]%>"><%=domains[i]%></option>
      <% } %>
    </select>

    <br/>
    <br/>

    <input type="button" value="Start" onclick="submitDomainFormAdhoc()"/>
  </form>
<% } %>
</div>

<div style="width: 40%; float: right;">
  <h3 align=center>Network Performance Data</h3>

  <p>
    The <strong>Standard Performance Reports</strong> provide a stock way to
    easily visualize the critical SNMP data collected from managed nodes
    throughout your network.
  <p>

  <p>
    <strong>Custom Performance Reports</strong> can be used to produce a
    single graph that contains the data of your choice from a single
    interface or node.  You can select the timeframe, line colors, line
     styles, and title of the graph and you can bookmark the results.
  </p>
</div>

<jsp:include page="/includes/footer.jsp" flush="false"/>
