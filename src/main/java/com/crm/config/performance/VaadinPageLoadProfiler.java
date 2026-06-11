package com.crm.config.performance;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.performance.profiling", havingValue = "true")
public class VaadinPageLoadProfiler implements VaadinServiceInitListener {

    private static final Logger log = LoggerFactory.getLogger(VaadinPageLoadProfiler.class);

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiEvent -> {
            long uiStart = System.nanoTime();
            var ui = uiEvent.getUI();
            ui.addBeforeEnterListener(e ->
                    StartupPerformanceProfiler.record(
                            "vaadin.route:" + e.getLocation().getPath(),
                            System.nanoTime() - uiStart));

            ui.addAttachListener(e -> {
                long attachMs = (System.nanoTime() - uiStart) / 1_000_000;
                log.info("[PERF] Vaadin UI attached — {} ms since UI init", attachMs);
            });
        });

        event.getSource().addSessionInitListener(sessionEvent ->
                log.info("[PERF] Vaadin session created"));
    }
}
