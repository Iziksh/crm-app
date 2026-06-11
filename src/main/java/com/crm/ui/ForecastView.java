package com.crm.ui;

import com.crm.dto.response.ForecastByStageResponse;
import com.crm.dto.response.ForecastSummaryResponse;
import com.crm.service.ForecastService;
import com.crm.service.TranslationService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;

@Route(value = "forecast", layout = MainLayout.class)
@RolesAllowed({"SALES", "ADMIN"})
public class ForecastView extends VerticalLayout implements HasDynamicTitle {

    private final TranslationService i18n;
    private final ForecastService forecastService;

    public ForecastView(ForecastService forecastService, TranslationService i18n) {
        this.forecastService = forecastService;
        this.i18n = i18n;
        setSizeFull();
        setPadding(true);

        add(new H2(i18n.translate("view.forecast.title")));

        TabSheet tabs = new TabSheet();
        tabs.setSizeFull();
        tabs.add(new Tab(i18n.translate("view.forecast.tab.leads")), buildTab(forecastService.leadForecast(), false));
        tabs.add(new Tab(i18n.translate("view.forecast.tab.opportunities")), buildTab(forecastService.opportunityForecast(), true));
        tabs.add(new Tab(i18n.translate("view.forecast.tab.quotes")), buildTab(forecastService.quoteForecast(), false));

        add(tabs);
        setFlexGrow(1, tabs);
    }

    @Override
    public String getPageTitle() {
        return i18n.translate("page.forecast");
    }

    private VerticalLayout buildTab(ForecastSummaryResponse summary, boolean showWeighted) {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);

        HorizontalLayout summaryRow = new HorizontalLayout();
        summaryRow.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        summaryRow.add(summaryCard(i18n.translate("common.period"), summary.period()));
        summaryRow.add(summaryCard(i18n.translate("common.total"), formatAmount(summary.totalAmount())));
        summaryRow.add(summaryCard(i18n.translate("common.records"), String.valueOf(summary.count())));
        if (showWeighted && summary.weightedAmount() != null) {
            summaryRow.add(summaryCard(i18n.translate("common.weighted"), formatAmount(summary.weightedAmount())));
        }
        content.add(summaryRow);

        Grid<ForecastByStageResponse> grid = new Grid<>(ForecastByStageResponse.class, false);
        grid.addColumn(ForecastByStageResponse::stage).setHeader(i18n.translate("view.forecast.column.stageStatus")).setSortable(true).setFlexGrow(2);
        grid.addColumn(ForecastByStageResponse::count).setHeader(i18n.translate("common.count")).setSortable(true);
        grid.addColumn(r -> formatAmount(r.amount())).setHeader(i18n.translate("common.amount")).setSortable(true);
        if (showWeighted) {
            grid.addColumn(r -> r.weighted() != null ? formatAmount(r.weighted()) : i18n.translate("common.emDash"))
                    .setHeader(i18n.translate("common.weighted")).setSortable(true);
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
        if (amount == null) return i18n.translate("common.emDash");
        return String.format("%,.2f", amount);
    }
}
