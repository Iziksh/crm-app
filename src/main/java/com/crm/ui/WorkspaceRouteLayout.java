package com.crm.ui;

import com.crm.service.WorkspaceContext;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RoutePrefix;
import com.vaadin.flow.router.RouterLayout;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Outer layout that contributes /:workspaceSlug as the first URL segment for all
 * authenticated views via MainLayout's @ParentLayout reference to this class.
 * This produces URLs of the form /{workspaceSlug}/{view}.
 * Admin users use "all" as their slug.
 */
@RoutePrefix(":workspaceSlug")
@PermitAll
public class WorkspaceRouteLayout extends Div implements RouterLayout, BeforeEnterObserver {

    private final WorkspaceContext workspaceContext;

    public WorkspaceRouteLayout(WorkspaceContext workspaceContext) {
        this.workspaceContext = workspaceContext;
        setSizeFull();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return;
        }

        String urlSlug = event.getRouteParameters().get("workspaceSlug").orElse("");
        String expectedSlug = workspaceContext.currentWorkspaceSlug();

        if (!expectedSlug.equals(urlSlug)) {
            String path = event.getLocation().getPath();
            String viewPath = path.contains("/") ? path.substring(path.indexOf('/') + 1) : "dashboard";
            event.forwardTo(expectedSlug + "/" + viewPath);
        }
    }
}
