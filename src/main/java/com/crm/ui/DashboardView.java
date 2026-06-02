package com.crm.ui;

import com.crm.domain.enums.ActivityStatus;
import com.crm.domain.enums.LeadStatus;
import com.crm.repository.AccountRepository;
import com.crm.repository.ActivityRepository;
import com.crm.repository.ContactRepository;
import com.crm.repository.LeadRepository;
import com.crm.service.ContractService;
import com.crm.service.OpportunityService;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;

@Route(value = "dashboard", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@PageTitle("Dashboard | CRM")
@PermitAll
public class DashboardView extends VerticalLayout {

    public DashboardView(AccountRepository accountRepository,
                         ContactRepository contactRepository,
                         ActivityRepository activityRepository,
                         LeadRepository leadRepository,
                         OpportunityService opportunityService,
                         ContractService contractService) {
        setSpacing(true);
        setPadding(true);

        add(new H2("Dashboard"));

        long accountCount = accountRepository.count();
        long contactCount = contactRepository.count();
        long openActivities = activityRepository.countByStatus(ActivityStatus.OPEN);
        long newLeads = leadRepository.countByStatus(LeadStatus.NEW);
        String pipelineValue = "USD " + String.format("%.2f", opportunityService.sumPipelineAmount());
        long expiringContracts = contractService.findExpiringWithin(30).size();

        HorizontalLayout cards = new HorizontalLayout();
        cards.setWidthFull();
        cards.add(statCard("Accounts", String.valueOf(accountCount)));
        cards.add(statCard("Contacts", String.valueOf(contactCount)));
        cards.add(statCard("Open Activities", String.valueOf(openActivities)));
        cards.add(statCard("New Leads", String.valueOf(newLeads)));
        cards.add(statCard("Pipeline Value", pipelineValue));
        cards.add(statCard("Contracts Expiring (30d)", String.valueOf(expiringContracts)));
        add(cards);

        add(new Paragraph("Welcome to the CRM. Use the navigation menu to manage your data."));
    }

    private VerticalLayout statCard(String label, String value) {
        VerticalLayout card = new VerticalLayout();
        card.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.Padding.LARGE,
                LumoUtility.BoxSizing.BORDER
        );
        card.getStyle().set("box-shadow", "var(--lumo-box-shadow-xs)");
        card.setWidth("200px");

        Span valueSpan = new Span(value);
        valueSpan.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.BOLD);

        H3 labelEl = new H3(label);
        labelEl.addClassNames(LumoUtility.Margin.NONE, LumoUtility.TextColor.SECONDARY);

        card.add(valueSpan, labelEl);
        return card;
    }
}
