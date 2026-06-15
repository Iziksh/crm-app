package com.crm.ui;

import com.crm.service.WorkspaceContext;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Route("")
@AnonymousAllowed
public class RootRedirectView extends Div implements BeforeEnterObserver {

    private final WorkspaceContext workspaceContext;

    public RootRedirectView(WorkspaceContext workspaceContext) {
        this.workspaceContext = workspaceContext;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            event.forwardTo("login");
            return;
        }
        event.forwardTo(workspaceContext.currentWorkspaceSlug() + "/dashboard");
    }
}
