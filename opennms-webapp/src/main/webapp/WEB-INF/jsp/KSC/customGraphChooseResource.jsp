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

<%@ page language="java" contentType="text/html" session="true" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<jsp:include page="/includes/header.jsp" flush="false" >
  <jsp:param name="title" value="Key SNMP Customized Performance Reports" />
  <jsp:param name="headTitle" value="Choose Interface" />
  <jsp:param name="headTitle" value="Performance" />
  <jsp:param name="headTitle" value="Reports" />
  <jsp:param name="breadcrumb" value="<a href='report/index.jsp'>Reports</a>" />
  <jsp:param name="breadcrumb" value="<a href='KSC/index.htm'>KSC Reports</a>" />
  <jsp:param name="breadcrumb" value="Custom Graph" />
</jsp:include>

  <script language="Javascript" type="text/javascript" >
      function validateResource()
      {
          var isChecked = false
          for (i = 0; i < document.report.resourceId.length; i++) {
              //make sure something is checked before proceeding
              if (document.report.resourceId[i].selected) {
                  isChecked=true;
              }
          }
  
          if (!isChecked) {
              alert("Please check the resources that you would like to report on.");
          }
          return isChecked;
      }
  
      function submitForm() {
          if (validateResource()) {
              document.report.submit();
          }
      }
  </script>

<h3 align="center">Customized Report - Graph Definition</h3>


<table width="100%" align="center">
  <tr>
    <td align="center">
      <form method="get" name="report" action="KSC/customGraphEditDetails.htm" >
      <%--
        <input type=hidden name="node" value="${param.node}">
        <input type=hidden name="domain" value="${param.domain}">
        --%>
        <table>
          <tr>
            <td>
              <h3>Choose an Interface to Query</h3>
            </td>
          </tr>

          <c:choose>
            <c:when test="${empty resources}">
              <tr>
                <td>
                  No node-level or interface SNMP resources found on the parent resource
                </td>
              </tr>
            </c:when>
            
            <c:otherwise>
              <tr>
                <td>
                  <select name="resourceId" size="10">
                    <option value="${param.resourceId}">-- Parent Resource --</option>
                    <c:forEach var="resource" items="${resources}">
                      <c:set var="selected" value=""/>
                      <c:if test="${param.selectedResourceId == resource.id}">
                        <c:set var="selected" value="selected"/>
                      </c:if>
                      <option value="${resource.id}" ${selected}>${resource.label}</option>
                    </c:forEach>
                  </select>
                </td>
              </tr>

              <tr>
                <td>
                  <input type="button" value="Submit" onclick="submitForm()" />
                </td>
              </tr>
            </c:otherwise>
          </c:choose>
        </table>
      </form>
    </td>
  </tr>
</table>

<jsp:include page="/includes/footer.jsp" flush="false" />
