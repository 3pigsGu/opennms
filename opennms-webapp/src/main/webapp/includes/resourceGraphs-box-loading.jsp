<%--

//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2009 The OpenNMS Group, Inc.  All rights reserved.
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

--%>

<%--
  This page is included by other JSPs to create a box containing an
  asynchronously loaded entry to the resource graph reporting system.
  
  It expects that a <base> tag has been set in the including page
  that directs all URLs to be relative to the servlet context.
--%>

<%@page language="java"
        contentType="text/html"
        session="true"
%>

<script type="text/javascript">
  function resetChooseResourceBoxSelected() {
    document.chooseResourceBoxNodeList.parentResource[0].selected = true;
  }
  
  function validateChooseResourceBoxNodeChosen() {
    var selectedParentResource = false
    
    for (i = 0; i < document.chooseResourceBoxNodeList.parentResource.length; i++) {
      // make sure something is checked before proceeding
      if (document.chooseResourceBoxNodeList.parentResource[i].selected
          && document.chooseResourceBoxNodeList.parentResource[i].value != "") {
        selectedParentResource = document.chooseResourceBoxNodeList.parentResource[i].value;
        break;
      }
    }
    
    return selectedParentResource;
  }
  
  function goChooseResourceBoxChange() {
    var nodeChosen = validateChooseResourceBoxNodeChosen();
    if (nodeChosen != false) {
      document.chooseResourceBoxForm.parentResource.value = nodeChosen;
      document.chooseResourceBoxForm.submit();
      /*
       * We reset the selection after submitting the form so if the user
       * uses the back button to get back to this page, it will be set at
       * the "choose a node" option.  Without this, they wouldn't be able
       * to proceed forward to the same node because won't trigger the
       * onChange action on the <select/> element.  We also do the submit
       * in a separate form after we copy the chosen value over, just to
       * ensure that no problems happen by resetting the selection
       * immediately after calling submit().
       */
      resetChooseResourceBoxSelected();
    }
  }
</script>

<script type="text/javascript">
function onResourceGraphsIFrameLoad() {
  var resourceGraphsDivContent = document.getElementById("resourceGraphsIFrame").contentWindow.document.body.innerHTML;
  document.getElementById("resourceGraphsDiv").innerHTML = resourceGraphsDivContent;
  resetChooseResourceBoxSelected();
}
</script>

<iframe id="resourceGraphsIFrame" style="display: none; visibility: hidden; height: 1px;" onload="onResourceGraphsIFrameLoad();"></iframe>

<div id="resourceGraphsDiv">
  <h3><a href="graph/index.jsp">Resource Graphs</a>&nbsp;<img src="images/progress.gif" width="16" height="16" alt="(Loading...)" /></h3>
  <div class="boxWrapper">
    &nbsp;
  </div>
</div>

<script type="text/javascript">
document.getElementById("resourceGraphsIFrame").src = "includes/resourceGraphs-box.jsp";
</script>
