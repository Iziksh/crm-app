package com.crm.ui;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("Login | CRM")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final Div errorMsg = new Div("Incorrect username or password.");

    public LoginView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        errorMsg.getStyle()
                .set("color", "#d32f2f")
                .set("font-size", "14px")
                .set("margin-bottom", "4px");
        errorMsg.setVisible(false);

        Html form = new Html("""
                <form method="post" action="login"
                      style="background:#fff;padding:32px;border-radius:8px;
                             box-shadow:0 2px 8px rgba(0,0,0,.15);min-width:320px;
                             display:flex;flex-direction:column;gap:14px;">
                  <h2 style="margin:0 0 4px;text-align:center;color:#1565c0;">CRM</h2>
                  <label style="display:flex;flex-direction:column;gap:4px;font-size:14px;font-weight:500;">
                    Username
                    <input type="text" name="username" autocomplete="username"
                           style="padding:8px 12px;border:1px solid #bbb;border-radius:4px;font-size:14px;outline:none;"/>
                  </label>
                  <label style="display:flex;flex-direction:column;gap:4px;font-size:14px;font-weight:500;">
                    Password
                    <input type="password" name="password" autocomplete="current-password"
                           style="padding:8px 12px;border:1px solid #bbb;border-radius:4px;font-size:14px;outline:none;"/>
                  </label>
                  <button type="submit"
                          style="padding:10px;background:#1565c0;color:#fff;border:none;
                                 border-radius:4px;cursor:pointer;font-size:14px;margin-top:4px;">
                    Log in
                  </button>
                </form>
                """);

        add(errorMsg, form);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            errorMsg.setVisible(true);
        }
    }
}
