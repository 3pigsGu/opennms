package org.opennms.web.controller.admin.thresholds;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opennms.netmgt.EventConstants;
import org.opennms.netmgt.config.EventconfFactory;
import org.opennms.netmgt.config.ThresholdingConfigFactory;
import org.opennms.netmgt.config.threshd.Basethresholddef;
import org.opennms.netmgt.config.threshd.Expression;
import org.opennms.netmgt.config.threshd.Group;
import org.opennms.netmgt.config.threshd.Threshold;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.netmgt.xml.eventconf.Logmsg;
import org.opennms.web.Util;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;


public class ThresholdController extends AbstractController implements InitializingBean {

    private static final String SAVE_BUTTON_TITLE="Save";
    private static final String CANCEL_BUTTON_TITLE="Cancel";
    
    private boolean eventConfChanged=false; 
    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ModelAndView modelAndView;
        ThresholdingConfigFactory.init();
        EventconfFactory.init();
        String editGroup = request.getParameter("editGroup");
        String deleteThreshold = request.getParameter("deleteThreshold");
        String editThreshold = request.getParameter("editThreshold");
        String newThreshold = request.getParameter("newThreshold");
        String finishThresholdEdit = request.getParameter("finishThresholdEdit");
        String deleteExpression = request.getParameter("deleteExpression");
        String editExpression = request.getParameter("editExpression");
        String newExpression = request.getParameter("newExpression");
        String finishExpressionEdit = request.getParameter("finishExpressionEdit");
        String groupName=request.getParameter("groupName");
       
        if(editGroup!=null) {
            modelAndView=gotoGroupEdit(groupName);
        } else if (newThreshold!=null) {
            modelAndView=gotoNewThreshold(groupName);
        } else if (editThreshold!=null) {
            String thresholdIndexString=request.getParameter("thresholdIndex");
            modelAndView=gotoEditThreshold(thresholdIndexString,groupName);
         } else if (deleteThreshold!=null) {
             String thresholdIndexString=request.getParameter("thresholdIndex");
             modelAndView=deleteThreshold(thresholdIndexString, groupName);
         } else if (finishThresholdEdit != null) {
             modelAndView=finishThresholdEdit(request);
         } else if (newExpression!=null) {
             modelAndView=gotoNewExpression(groupName);
         } else if (editExpression!=null) {
             String expressionIndexString=request.getParameter("expressionIndex");
             modelAndView=gotoEditExpression(expressionIndexString,groupName);
          } else if (deleteExpression!=null) {
              String expressionIndexString=request.getParameter("expressionIndex");
              modelAndView=deleteExpression(expressionIndexString, groupName);
          } else if (finishExpressionEdit != null) {
              modelAndView=finishExpressionEdit(request);
         
         } else {
             modelAndView=gotoGroupList();
        }
        return modelAndView;
    }
    
    private ModelAndView gotoGroupEdit(String groupName) {
        ThresholdingConfigFactory configFactory=ThresholdingConfigFactory.getInstance();
        ModelAndView modelAndView=new ModelAndView("admin/thresholds/editGroup");
        modelAndView.addObject("group",configFactory.getGroup(groupName));
        return modelAndView;
    }
    
    private void addStandardEditingBits(ModelAndView modelAndView) {
        Collection<String> dsTypes=new ArrayList<String>();
        dsTypes.add("node");
        dsTypes.add("if"); // "interface" is a wrong word
        modelAndView.addObject("dsTypes",dsTypes);

        Collection<String> thresholdTypes=new ArrayList<String>();
        thresholdTypes.add("high");
        thresholdTypes.add("low");
        thresholdTypes.add("relativeChange");
        modelAndView.addObject("thresholdTypes",thresholdTypes);
      
        modelAndView.addObject("saveButtonTitle", SAVE_BUTTON_TITLE);
        modelAndView.addObject("cancelButtonTitle", CANCEL_BUTTON_TITLE);
    }
    
    private ModelAndView gotoNewThreshold(String groupName) {
        ThresholdingConfigFactory configFactory=ThresholdingConfigFactory.getInstance();
        
        Group group=configFactory.getGroup(groupName);
        Threshold threshold=new Threshold();
        //Set the two default values which need to be set for the UI to work properly
        threshold.setDsType("node");
        threshold.setType("high"); 
	threshold.setTrigger(1); //Default to 1 - 0 will give an error, so we may as well be helpful
        
        //We're assuming that adding a threshold puts it at the end of the current list (i.e. that the Group implementation
        // uses a simple List structure, probably ArrayList).  We can be a bit cleverer later on and check though, so we should
        int thresholdIndex=group.getThresholdCount();
        
        group.addThreshold(threshold);
        
        //Double check the guess index, just in case:
        if(threshold!=group.getThreshold(thresholdIndex)) {
            //Ok, our guesses on indexing were completely wrong.  Failover and check each threshold in the group
            for(int i=0; i<group.getThresholdCount(); i++) {
                if(threshold==group.getThreshold(i)) {
                    thresholdIndex=i;
                    break; //out of the for loop
                }
            }
        }
        
        ModelAndView modelAndView;
        modelAndView=new ModelAndView("admin/thresholds/editThreshold");
        modelAndView.addObject("threshold",threshold);
        modelAndView.addObject("thresholdIndex", thresholdIndex );
        modelAndView.addObject("groupName", groupName);
        modelAndView.addObject("isNew", true);
        
        addStandardEditingBits(modelAndView);

        return modelAndView;
    }
  
    private ModelAndView gotoNewExpression(String groupName) {
        ThresholdingConfigFactory configFactory=ThresholdingConfigFactory.getInstance();
        
        Group group=configFactory.getGroup(groupName);
        Expression expression=new Expression();
        //Set the two default values which need to be set for the UI to work properly
        expression.setDsType("node");
        expression.setType("high"); 
        
        //We're assuming that adding a expression puts it at the end of the current list (i.e. that the Group implementation
        // uses a simple List structure, probably ArrayList).  We can be a bit cleverer later on and check though, so we should
        int expressionIndex=group.getExpressionCount();
        
        group.addExpression(expression);
        
        //Double check the guess index, just in case:
        if(expression!=group.getExpression(expressionIndex)) {
            //Ok, our guesses on indexing were completely wrong.  Failover and check each threshold in the group
            for(int i=0; i<group.getExpressionCount(); i++) {
                if(expression==group.getExpression(i)) {
                    expressionIndex=i;
                    break; //out of the for loop
                }
            }
        }
        
        ModelAndView modelAndView;
        modelAndView=new ModelAndView("admin/thresholds/editExpression");
        modelAndView.addObject("expression",expression);
        modelAndView.addObject("expressionIndex", expressionIndex );
        modelAndView.addObject("groupName", groupName);
        modelAndView.addObject("isNew", true);
        
        addStandardEditingBits(modelAndView);

        return modelAndView;
    }
    
    private ModelAndView gotoEditThreshold(String thresholdIndexString, String groupName) throws ServletException {
        ThresholdingConfigFactory configFactory=ThresholdingConfigFactory.getInstance();
        ModelAndView modelAndView;
        if(thresholdIndexString==null) {
            throw new ServletException("thresholdIndex parameter required to edit a threshold");
        }
        int thresholdIndex=Integer.parseInt(thresholdIndexString);

        Threshold threshold=configFactory.getGroup(groupName).getThreshold(thresholdIndex);
        modelAndView=new ModelAndView("admin/thresholds/editThreshold");
        
        modelAndView.addObject("threshold", threshold);
        modelAndView.addObject("thresholdIndex", thresholdIndex);
        modelAndView.addObject("groupName", groupName);
        modelAndView.addObject("isNew", false);
        addStandardEditingBits(modelAndView);
        
        return modelAndView;
    }
    
    private ModelAndView gotoEditExpression(String expressionIndexString, String groupName) throws ServletException {
        ThresholdingConfigFactory configFactory=ThresholdingConfigFactory.getInstance();
        ModelAndView modelAndView;
        if(expressionIndexString==null) {
            throw new ServletException("expressionIndex parameter required to edit a threshold");
        }
        int expressionIndex=Integer.parseInt(expressionIndexString);

        Expression expression=configFactory.getGroup(groupName).getExpression(expressionIndex);
        modelAndView=new ModelAndView("admin/thresholds/editExpression");
        
        modelAndView.addObject("expression", expression);
        modelAndView.addObject("expressionIndex", expressionIndex);
        modelAndView.addObject("groupName", groupName);
        modelAndView.addObject("isNew", false);
        addStandardEditingBits(modelAndView);
        
        return modelAndView;
    }
    
    private void sendNotifEvent(String uei) throws ServletException {
        Event event = new Event();
        event.setSource("Web UI");
        event.setUei(uei);
        try {
                event.setHost(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException uhE) {
                event.setHost("unresolved.host");
        }
        
        event.setTime(EventConstants.formatToString(new java.util.Date()));
        try {
                Util.createEventProxy().send(event);
        } catch (Exception e) {
                throw new ServletException("Could not send event " + event.getUei(), e);
        }
       
    }
    private void saveChanges() throws ServletException {
        ThresholdingConfigFactory configFactory=ThresholdingConfigFactory.getInstance();
        try {
            configFactory.saveCurrent();
            sendNotifEvent(EventConstants.THRESHOLDCONFIG_CHANGED_EVENT_UEI);
        } catch (Exception e) {
            throw new ServletException("Could not save the changes to the threshold because "+e.getMessage(),e);
        }
        
        if(eventConfChanged) {
            try {
                EventconfFactory.getInstance().saveCurrent();
                sendNotifEvent(EventConstants.EVENTSCONFIG_CHANGED_EVENT_UEI);
            } catch (Exception e) {
                throw new ServletException("Could not save the changes to the event configuration because "+e.getMessage(),e);
            }
            eventConfChanged=false;
        }

    }
    private ModelAndView deleteThreshold(String thresholdIndexString, String groupName) throws ServletException {
        ThresholdingConfigFactory configFactory=ThresholdingConfigFactory.getInstance();
        ModelAndView modelAndView;
        if(thresholdIndexString==null) {
            throw new ServletException("thresholdIndex parameter required to delete a threshold");
        }
        int thresholdIndex=Integer.parseInt(thresholdIndexString);
        Group group=configFactory.getGroup(groupName);
        group.removeThreshold(group.getThreshold(thresholdIndex));
        //and setup the group view again
        modelAndView=new ModelAndView("admin/thresholds/editGroup");
        modelAndView.addObject("group",configFactory.getGroup(groupName));
        saveChanges();
        return modelAndView;
    }
    
    private ModelAndView deleteExpression(String expressionIndexString, String groupName) throws ServletException {
        ThresholdingConfigFactory configFactory=ThresholdingConfigFactory.getInstance();
        ModelAndView modelAndView;
        if(expressionIndexString==null) {
            throw new ServletException("expressionIndex parameter required to delete a threshold");
        }
        int expressionIndex=Integer.parseInt(expressionIndexString);
        Group group=configFactory.getGroup(groupName);
        group.removeExpression(group.getExpression(expressionIndex));
        saveChanges();
        
        //and setup the group view again
        modelAndView=new ModelAndView("admin/thresholds/editGroup");
        modelAndView.addObject("group",configFactory.getGroup(groupName));
        return modelAndView;
    }
    private void commonFinishEdit(HttpServletRequest request, Basethresholddef baseDef) {
        String dsLabel=request.getParameter("dsLabel");
        if(dsLabel==null || "".equals(dsLabel)) {
            baseDef.setDsLabel(null); //Must set null in correct circumstances - empty string isn't quite the same thing
        } else {
            baseDef.setDsLabel(dsLabel);
        }
       
        String triggeredUEI=request.getParameter("triggeredUEI");
        if(triggeredUEI==null || "".equals(triggeredUEI)) {
            baseDef.setTriggeredUEI(null); //Must set null in correct circumstances - empty string isn't quite the same thing
        } else {
            baseDef.setTriggeredUEI(triggeredUEI);
            this.ensureUEIInEventConf(triggeredUEI, "exceeded");
        }
  
        String rearmedUEI=request.getParameter("rearmedUEI");
        if(rearmedUEI==null || "".equals(rearmedUEI)) {
            baseDef.setRearmedUEI(null); //Must set null in correct circumstances - empty string isn't quite the same thing
        } else {
            baseDef.setRearmedUEI(rearmedUEI);
            this.ensureUEIInEventConf(rearmedUEI, "rearmed");
        }
        baseDef.setDsType(request.getParameter("dsType"));
        baseDef.setType(request.getParameter("type"));
        baseDef.setRearm(Double.parseDouble(request.getParameter("rearm")));
        baseDef.setTrigger(Integer.parseInt(request.getParameter("trigger")));
        baseDef.setValue(Double.parseDouble(request.getParameter("value")));
 
    }
    
    private void ensureUEIInEventConf(String uei, String typeDesc) {
        EventconfFactory factory = EventconfFactory.getInstance();
        List<org.opennms.netmgt.xml.eventconf.Event> eventsForUEI=factory.getEvents(uei);
        if(eventsForUEI==null || eventsForUEI.size()==0) {
            //UEI doesn't exist.  Add it
            org.opennms.netmgt.xml.eventconf.Event event=new org.opennms.netmgt.xml.eventconf.Event();
            event.setUei(uei);
            event.setEventLabel("User-defined threshold event "+uei);
            event.setDescr("Threshold "+typeDesc+" for %service% datasource " +
                        "%parm[ds]% on interface %interface%, parms: %parm[all]");
            Logmsg logmsg=new Logmsg();
            logmsg.setDest("logndisplay");
            logmsg.setContent("Threshold "+typeDesc+" for %service% datasource %parm[ds]% on interface %interface%, parms: %parm[all]%");
            event.setLogmsg(logmsg);
            event.setSeverity("Warning");
            factory.addEventToProgrammaticStore(event);
            eventConfChanged=true;
        }
    }

    private ModelAndView finishThresholdEdit(HttpServletRequest request) throws ServletException {
        ThresholdingConfigFactory configFactory=ThresholdingConfigFactory.getInstance();
        ModelAndView modelAndView;
        String groupName=request.getParameter("groupName");
        String submitAction=request.getParameter("submitAction");
        Group group=configFactory.getGroup(groupName);
        String thresholdIndexString=request.getParameter("thresholdIndex");
        if(thresholdIndexString==null) {
            throw new ServletException("thresholdIndex parameter required to delete a threshold");
        }
        int thresholdIndex=Integer.parseInt(thresholdIndexString);
        Threshold threshold=group.getThreshold(thresholdIndex);
        
        if(SAVE_BUTTON_TITLE.equals(submitAction)) {
            this.commonFinishEdit(request, threshold);
            threshold.setDsName(request.getParameter("dsName"));
            saveChanges();
         } else if (CANCEL_BUTTON_TITLE.equals(submitAction)) {
            String isNew=request.getParameter("isNew");
            if("true".equals(isNew)) {
                //It was a new Threshold, but the user hit cancel.  Remove the new threshold from the group
                group.removeThreshold(threshold);
            }
        }
        //and got back to the editGroup page
        modelAndView=new ModelAndView("admin/thresholds/editGroup");
        modelAndView.addObject("group",configFactory.getGroup(groupName));
        return modelAndView;
    }
    
    private ModelAndView finishExpressionEdit(HttpServletRequest request) throws ServletException {
        ThresholdingConfigFactory configFactory=ThresholdingConfigFactory.getInstance();
        ModelAndView modelAndView;
        String groupName=request.getParameter("groupName");
        String submitAction=request.getParameter("submitAction");
        Group group=configFactory.getGroup(groupName);
        String expressionIndexString=request.getParameter("expressionIndex");
        if(expressionIndexString==null) {
            throw new ServletException("expressionIndex parameter required to delete a threshold");
        }
        int expressionIndex=Integer.parseInt(expressionIndexString);
        Expression expression=group.getExpression(expressionIndex);
        
        if(SAVE_BUTTON_TITLE.equals(submitAction)) {
            this.commonFinishEdit(request, expression);
            expression.setExpression(request.getParameter("expression"));
            saveChanges();
         } else if (CANCEL_BUTTON_TITLE.equals(submitAction)) {
            String isNew=request.getParameter("isNew");
            if("true".equals(isNew)) {
                //It was a new Threshold, but the user hit cancel.  Remove the new threshold from the group
                group.removeExpression(expression);
            }
        }
        //and got back to the editGroup page
        modelAndView=new ModelAndView("admin/thresholds/editGroup");
        modelAndView.addObject("group",configFactory.getGroup(groupName));
        return modelAndView;
    }   
    private ModelAndView gotoGroupList() throws ServletException {
        //Always reload to get a consistent view of the thresholds before we start editing.  
        //Otherwise we'll be dealing with questions on the mailing lists for the rest of our lives
        try {
             ThresholdingConfigFactory.reload();
        } catch (Exception e) {
            throw new ServletException("Could not reload ThresholdingConfigFactory because "+e.getMessage(), e);
        }
        ThresholdingConfigFactory configFactory=ThresholdingConfigFactory.getInstance();
        ModelAndView modelAndView= new ModelAndView("admin/thresholds/list");

        Map<String, Group> groupMap=new HashMap<String,Group>();
        for(String aName:configFactory.getGroupNames()) {
            groupMap.put(aName, configFactory.getGroup(aName));
        }

        modelAndView.addObject("groupMap", groupMap);
        return modelAndView;
    }
 
    
    public void afterPropertiesSet() throws Exception {
        //Check all properties set (see example if needed)
        /*if (m_resourceService == null) {
            throw new IllegalStateException(
                                            "property resourceService must be set");
        }*/

    }


}
