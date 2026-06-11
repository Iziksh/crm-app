package com.crm.config;

import com.crm.service.LocaleService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.stereotype.Component;

@Component
public class LocaleUiInitListener implements VaadinServiceInitListener {

    private final LocaleService localeService;

    public LocaleUiInitListener(LocaleService localeService) {
        this.localeService = localeService;
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiEvent ->
                localeService.resolveAndApply(uiEvent.getUI()));
    }
}
