/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2013-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/
package org.opennms.features.vaadin.surveillanceviews.ui;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import org.opennms.features.vaadin.surveillanceviews.config.SurveillanceViewProvider;
import org.opennms.features.vaadin.surveillanceviews.model.View;
import org.opennms.features.vaadin.surveillanceviews.service.SurveillanceViewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The surveillance view application's "main" class
 *
 * @author Christian Pape
 */
@SuppressWarnings("serial")
@Theme("opennms")
@Title("OpenNMS Surveillance Views")
public class SurveillanceViewsUI extends UI {
    private static final Logger LOG = LoggerFactory.getLogger(SurveillanceViewsUI.class);

    private SurveillanceViewService m_surveillanceViewService;

    @Override
    protected void init(VaadinRequest request) {
        VerticalLayout rootLayout = new VerticalLayout();
        rootLayout.setSpacing(true);

        String viewName = request.getParameter("viewName") == null ? "default" : request.getParameter("viewName");
        boolean dashboard = request.getParameter("dashboard") != null && "true".equals(request.getParameter("dashboard"));
        String username = request.getRemoteUser();

        View view = SurveillanceViewProvider.getInstance().getView("default");

        rootLayout.addComponent(new Label("viewName=" + viewName));
        rootLayout.addComponent(new Label("dashboard=" + dashboard));
        rootLayout.addComponent(new Label("username=" + username));

        boolean isDashboardRole = true;

        SecurityContext context = SecurityContextHolder.getContext();

        if ((context != null) && !(context.toString().contains(org.opennms.web.api.Authentication.ROLE_DASHBOARD))) {
            isDashboardRole = false;
        }

        LOG.debug("User {} is in dashboard role? {}", username, isDashboardRole);

        rootLayout.addComponent(new SurveillanceView(view, m_surveillanceViewService, dashboard, !isDashboardRole));

        setContent(rootLayout);
    }

    /**
     * Method for setting the {@link org.opennms.features.vaadin.surveillanceviews.service.SurveillanceViewService} instance to be used
     *
     * @param surveillanceViewService the instance to be used
     */
    public void setSurveillanceViewService(SurveillanceViewService surveillanceViewService) {
        this.m_surveillanceViewService = surveillanceViewService;
    }
}
