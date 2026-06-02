package com.crm.ui;

import com.crm.dto.response.ForecastByStageResponse;
import com.crm.dto.response.ForecastSummaryResponse;
import com.crm.service.ForecastService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;

@Route(value = "forecast", layout = MainLayout.class)
@PageTitle("Forecast | CRM")
@RolesAllowed({"SALES", "ADMIN"})
public class ForecastView extends VerticalLayout {

    private final ForecastService forecastService;

    public ForecastView(ForecastService forecastService) {
        this.forecastService = forecastService;
        setSizeFull();
        setPadding(true);

        add(new H2("Forecast"));

        TabSheet tabs = new TabSheet();
        tabs.setSizeFull();
        tabs.add(new Tab("Leads"), buildTab(forecastService.leadForecast(), false));
        tabs.add(new Tab("Opportunities"), buildTab(forecastService.opportunityForecast(), true));
        tabs.add(new Tab("Quotes"), buildTab(forecastService.quoteForecast(), false));

        add(tabs);
        setFlexGrow(1, tabs);
    }

    private VerticalLayout buildTab(ForecastSummaryResponse summary, boolean showWeighted) {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);

        HorizontalLayout summaryRow = new HorizontalLayout();
        summaryRow.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        summaryRow.add(summaryCard("Period", summary.period()));
        summaryRow.add(summaryCard("Total", formatAmount(summary.totalAmount())));
        summaryRow.add(summaryCard("Records", String.valueOf(summary.count())));
        if (showWeighted && summary.weightedAmount() != null) {
            summaryRow.add(summaryCard("Weighted", formatAmount(summary.weightedAmount())));
        }
        content.add(summaryRow);

        Grid<ForecastByStageResponse> grid = new Grid<>(ForecastByStageResponse.class, false);
        grid.addColumn(ForecastByStageResponse::stage).setHeader("Stage / Status").setSortable(true).setFlexGrow(2);
        grid.addColumn(ForecastByStageResponse::count).setHeader("Count").setSortable(true);
        grid.addColumn(r -> formatAmount(r.amount())).setHeader("Amount").setSortable(true);
        if (showWeighted) {
            grid.addColumn(r -> r.weighted() != null ? formatAmount(r.weighted()) : "—").setHeader("Weighted").setSortable(true);
        }
        grid.setItems(summary.byStage());
        grid.setAllRowsVisible(true);
        content.add(grid);
        content.setFlexGrow(1, grid);

        return content;
    }

    private VerticalLayout summaryCard(String label, String value) {
        VerticalLayout card = new VerticalLayout();
        card.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.Padding.LARGE,
                LumoUtility.BoxSizing.BORDER
        );
        card.getStyle().set("box-shadow", "var(--lumo-box-shadow-xs)");
        card.setWidth("220px");
        card.setSpacing(false);

        Span valueSpan = new Span(value);
        valueSpan.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.BOLD);

        H4 labelEl = new H4(label);
        labelEl.addClassNames(LumoUtility.Margin.NONE, LumoUtility.TextColor.SECONDARY);

        card.add(valueSpan, labelEl);
        return card;
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "—";
        return String.format("%,.2f", amount);
    }
}
