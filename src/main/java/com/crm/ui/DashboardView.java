package com.crm.ui;

import com.crm.domain.enums.ActivityStatus;
import com.crm.domain.enums.ContractStatus;
import com.crm.domain.enums.LeadStatus;
import com.crm.domain.enums.OpportunityStage;
import com.crm.repository.AccountRepository;
import com.crm.repository.ActivityRepository;
import com.crm.repository.ContactRepository;
import com.crm.repository.ContractRepository;
import com.crm.repository.LeadRepository;
import com.crm.repository.OpportunityRepository;
import com.crm.service.ContractService;
import com.crm.service.OpportunityService;
import com.crm.service.TranslationService;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;

import java.math.BigDecimal;
import java.util.List;

@Route(value = "dashboard", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@PermitAll
public class DashboardView extends VerticalLayout implements HasDynamicTitle {

    private final TranslationService i18n;

    public DashboardView(AccountRepository accountRepository,
                         ContactRepository contactRepository,
                         ActivityRepository activityRepository,
                         LeadRepository leadRepository,
                         OpportunityRepository opportunityRepository,
                         ContractRepository contractRepository,
                         OpportunityService opportunityService,
                         ContractService contractService,
                         TranslationService i18n) {
        this.i18n = i18n;

        setSpacing(true);
        setPadding(true);

        add(new H2(i18n.translate("view.dashboard.title")));

        // ── KPI stat cards ────────────────────────────────────────────────────
        long accountCount    = accountRepository.count();
        long contactCount    = contactRepository.count();
        long openActivities  = activityRepository.countByStatus(ActivityStatus.OPEN);
        long newLeads        = leadRepository.countByStatus(LeadStatus.NEW);
        BigDecimal pipeline  = opportunityService.sumPipelineAmount();
        String pipelineStr   = i18n.translate("view.dashboard.pipelineCurrency",
                String.format("%,.0f", pipeline));
        long expiringContracts = contractService.findExpiringWithin(30).size();

        long wonOpps  = opportunityRepository.countByStage(OpportunityStage.WON);
        long lostOpps = opportunityRepository.countByStage(OpportunityStage.LOST);
        long totalClosed = wonOpps + lostOpps;
        String winRate = totalClosed > 0
                ? String.format("%.0f%%", (double) wonOpps / totalClosed * 100)
                : i18n.translate("common.emDash");

        HorizontalLayout kpiRow = new HorizontalLayout();
        kpiRow.setWidthFull();
        kpiRow.getStyle().set("flex-wrap", "wrap").set("gap", "12px");
        kpiRow.add(
                statCard(i18n.translate("view.dashboard.accounts"),     String.valueOf(accountCount),     "#1565c0"),
                statCard(i18n.translate("view.dashboard.contacts"),     String.valueOf(contactCount),     "#1565c0"),
                statCard(i18n.translate("view.dashboard.openActivities"), String.valueOf(openActivities), "#e65100"),
                statCard(i18n.translate("view.dashboard.newLeads"),     String.valueOf(newLeads),         "#6a1b9a"),
                statCard(i18n.translate("view.dashboard.pipelineValue"), pipelineStr,                      "#2e7d32"),
                statCard(i18n.translate("view.dashboard.contractsExpiring"), String.valueOf(expiringContracts), "#c62828"),
                statCard(i18n.translate("view.dashboard.winRate"),        winRate,                          "#2e7d32")
        );
        add(kpiRow);

        // ── Charts row 1 ─────────────────────────────────────────────────────
        H3 chartsTitle = new H3(i18n.translate("view.dashboard.salesOverview"));
        chartsTitle.addClassNames(LumoUtility.Margin.Top.LARGE, LumoUtility.Margin.Bottom.SMALL);
        add(chartsTitle);

        // Pipeline by stage
        long prospecting  = opportunityRepository.countByStage(OpportunityStage.PROSPECTING);
        long qualification = opportunityRepository.countByStage(OpportunityStage.QUALIFICATION);
        long proposal     = opportunityRepository.countByStage(OpportunityStage.PROPOSAL);
        long negotiation  = opportunityRepository.countByStage(OpportunityStage.NEGOTIATION);
        long maxOpp = Math.max(1, Math.max(prospecting, Math.max(qualification,
                Math.max(proposal, Math.max(negotiation, Math.max(wonOpps, lostOpps))))));

        Div pipelineChart = chartCard(i18n.translate("view.dashboard.pipelineByStage"), List.of(
                bar(i18n.translateEnum(OpportunityStage.PROSPECTING),  prospecting,  maxOpp, "#90caf9"),
                bar(i18n.translateEnum(OpportunityStage.QUALIFICATION), qualification, maxOpp, "#42a5f5"),
                bar(i18n.translateEnum(OpportunityStage.PROPOSAL),     proposal,      maxOpp, "#1e88e5"),
                bar(i18n.translateEnum(OpportunityStage.NEGOTIATION),  negotiation,   maxOpp, "#1565c0"),
                bar(i18n.translateEnum(OpportunityStage.WON),          wonOpps,        maxOpp, "#43a047"),
                bar(i18n.translateEnum(OpportunityStage.LOST),         lostOpps,       maxOpp, "#e53935")
        ));

        // Lead funnel
        long contacted = leadRepository.countByStatus(LeadStatus.CONTACTED);
        long qualified = leadRepository.countByStatus(LeadStatus.QUALIFIED);
        long wonLeads  = leadRepository.countByStatus(LeadStatus.WON);
        long lostLeads = leadRepository.countByStatus(LeadStatus.LOST);
        long maxLead   = Math.max(1, Math.max(newLeads, Math.max(contacted,
                Math.max(qualified, Math.max(wonLeads, lostLeads)))));

        Div leadChart = chartCard(i18n.translate("view.dashboard.leadFunnel"), List.of(
                bar(i18n.translateEnum(LeadStatus.NEW),       newLeads,  maxLead, "#90caf9"),
                bar(i18n.translateEnum(LeadStatus.CONTACTED), contacted, maxLead, "#42a5f5"),
                bar(i18n.translateEnum(LeadStatus.QUALIFIED), qualified, maxLead, "#1e88e5"),
                bar(i18n.translateEnum(LeadStatus.WON),       wonLeads,  maxLead, "#43a047"),
                bar(i18n.translateEnum(LeadStatus.LOST),      lostLeads, maxLead, "#e53935")
        ));

        HorizontalLayout row1 = chartRow(pipelineChart, leadChart);
        add(row1);

        // ── Charts row 2 ─────────────────────────────────────────────────────
        long inProgress = activityRepository.countByStatus(ActivityStatus.IN_PROGRESS);
        long resolved   = activityRepository.countByStatus(ActivityStatus.RESOLVED);
        long closed     = activityRepository.countByStatus(ActivityStatus.CLOSED);
        long maxAct     = Math.max(1, Math.max(openActivities,
                Math.max(inProgress, Math.max(resolved, closed))));

        Div activityChart = chartCard(i18n.translate("view.dashboard.activityBreakdown"), List.of(
                bar(i18n.translateEnum(ActivityStatus.OPEN),        openActivities, maxAct, "#ef6c00"),
                bar(i18n.translateEnum(ActivityStatus.IN_PROGRESS), inProgress,     maxAct, "#1e88e5"),
                bar(i18n.translateEnum(ActivityStatus.RESOLVED),    resolved,        maxAct, "#43a047"),
                bar(i18n.translateEnum(ActivityStatus.CLOSED),      closed,          maxAct, "#78909c")
        ));

        long activeContracts  = contractRepository.countByStatus(ContractStatus.ACTIVE);
        long draftContracts   = contractRepository.countByStatus(ContractStatus.DRAFT);
        long expiredContracts = contractRepository.countByStatus(ContractStatus.EXPIRED);
        long terminatedContracts = contractRepository.countByStatus(ContractStatus.TERMINATED);
        long maxCon = Math.max(1, Math.max(activeContracts, Math.max(draftContracts,
                Math.max(expiredContracts, terminatedContracts))));

        Div contractChart = chartCard(i18n.translate("view.dashboard.contractHealth"), List.of(
                bar(i18n.translateEnum(ContractStatus.ACTIVE),     activeContracts,     maxCon, "#43a047"),
                bar(i18n.translateEnum(ContractStatus.DRAFT),      draftContracts,      maxCon, "#1e88e5"),
                bar(i18n.translateEnum(ContractStatus.EXPIRED),    expiredContracts,    maxCon, "#e53935"),
                bar(i18n.translateEnum(ContractStatus.TERMINATED), terminatedContracts, maxCon, "#78909c")
        ));

        add(chartRow(activityChart, contractChart));
    }

    @Override
    public String getPageTitle() {
        return i18n.translate("page.dashboard");
    }

    // ── Layout helpers ────────────────────────────────────────────────────────

    private HorizontalLayout chartRow(Div left, Div right) {
        HorizontalLayout row = new HorizontalLayout(left, right);
        row.setWidthFull();
        row.getStyle().set("gap", "16px").set("align-items", "flex-start");
        left.getStyle().set("flex", "1");
        right.getStyle().set("flex", "1");
        return row;
    }

    private Div chartCard(String title, List<Div> bars) {
        Div card = new Div();
        card.getStyle()
                .set("background", "#fff")
                .set("border-radius", "8px")
                .set("padding", "20px 24px")
                .set("box-shadow", "0 1px 4px rgba(0,0,0,.10)")
                .set("min-width", "0");

        Span titleSpan = new Span(title);
        titleSpan.getStyle()
                .set("display", "block")
                .set("font-weight", "700")
                .set("font-size", "15px")
                .set("color", "#1565c0")
                .set("margin-bottom", "14px");
        card.add(titleSpan);

        for (Div b : bars) card.add(b);
        return card;
    }

    private Div bar(String label, long value, long max, String color) {
        double pct = max > 0 ? Math.min(100.0, (double) value / max * 100) : 0;

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("min-width", "110px")
                .set("font-size", "13px")
                .set("color", "#555")
                .set("white-space", "nowrap");

        Div track = new Div();
        track.getStyle()
                .set("flex", "1")
                .set("background", "#f0f4ff")
                .set("border-radius", "4px")
                .set("height", "18px")
                .set("overflow", "hidden");

        Div fill = new Div();
        fill.getStyle()
                .set("width", String.format("%.1f%%", pct))
                .set("background", color)
                .set("height", "100%")
                .set("border-radius", "4px");
        track.add(fill);

        Span countSpan = new Span(String.valueOf(value));
        countSpan.getStyle()
                .set("min-width", "32px")
                .set("text-align", "end")
                .set("font-weight", "700")
                .set("font-size", "13px")
                .set("color", "#333");

        Div row = new Div(labelSpan, track, countSpan);
        row.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "10px")
                .set("padding", "3px 0");
        return row;
    }

    private VerticalLayout statCard(String label, String value, String accentColor) {
        VerticalLayout card = new VerticalLayout();
        card.getStyle()
                .set("background", "#fff")
                .set("border-radius", "8px")
                .set("padding", "16px 20px")
                .set("box-shadow", "0 1px 4px rgba(0,0,0,.10)")
                .set("border-top", "3px solid " + accentColor)
                .set("min-width", "140px");
        card.setSpacing(false);
        card.setPadding(false);

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", "28px")
                .set("font-weight", "800")
                .set("color", accentColor)
                .set("line-height", "1.1");

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "12px")
                .set("color", "#888")
                .set("margin-top", "4px");

        card.add(valueSpan, labelSpan);
        return card;
    }
}
