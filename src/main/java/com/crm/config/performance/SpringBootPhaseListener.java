package com.crm.config.performance;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.performance.profiling", havingValue = "true")
public class SpringBootPhaseListener {

    private long lastPhaseNanos = System.nanoTime();

    @EventListener
    public void onEnvironmentPrepared(ApplicationEnvironmentPreparedEvent event) {
        recordPhase("phase.environment-prepared");
    }

    @EventListener
    public void onContextInitialized(ApplicationContextInitializedEvent event) {
        recordPhase("phase.context-initialized");
    }

    @EventListener
    public void onApplicationPrepared(ApplicationPreparedEvent event) {
        recordPhase("phase.application-prepared");
    }

    @EventListener
    public void onContextRefreshed(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() != null) return;
        recordPhase("phase.context-refreshed");
    }

    private void recordPhase(String phase) {
        long now = System.nanoTime();
        StartupPerformanceProfiler.record(phase, now - lastPhaseNanos);
        lastPhaseNanos = now;
    }
}
