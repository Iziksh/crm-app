package com.crm.config.performance;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.sql.DataSource;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.concurrent.atomic.AtomicBoolean;

@Configuration
@ConditionalOnProperty(name = "app.performance.profiling", havingValue = "true")
public class StartupPerformanceConfig {

    private static final Logger log = LoggerFactory.getLogger(StartupPerformanceConfig.class);
    private static final AtomicBoolean firstHttpRequestLogged = new AtomicBoolean(false);

    public StartupPerformanceConfig(@Value("${app.performance.profiling:false}") boolean profiling) {
        StartupPerformanceProfiler.setEnabled(profiling);
    }

    @Bean
    ApplicationListener<ApplicationStartedEvent> onApplicationStarted(DataSource dataSource) {
        return event -> {
            StartupPerformanceProfiler.time("phase.application-bootstrap", () -> {});
            logDataSourcePool("phase.database-pool-ready", dataSource);
            logMemory("phase.post-started");
        };
    }

    @Bean
    ApplicationListener<ApplicationReadyEvent> onApplicationReady() {
        return event -> {
            StartupPerformanceProfiler.logSummary("SERVER READY — startup phases");
            logMemory("phase.application-ready");
        };
    }

    @Bean
    FilterRegistrationBean<OncePerRequestFilter> firstRequestTimingFilter() {
        FilterRegistrationBean<OncePerRequestFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain)
                    throws ServletException, IOException {
                long start = System.nanoTime();
                try {
                    filterChain.doFilter(request, response);
                } finally {
                    String path = request.getRequestURI();
                    if (path.startsWith("/VAADIN/") || path.endsWith(".js") || path.endsWith(".css")
                            || path.endsWith(".woff2") || path.endsWith(".svg")) {
                        StartupPerformanceProfiler.record("frontend.asset:" + shortenPath(path), System.nanoTime() - start);
                    } else if (!path.startsWith("/actuator")) {
                        StartupPerformanceProfiler.record("http.request:" + path, System.nanoTime() - start);
                    }
                    if (firstHttpRequestLogged.compareAndSet(false, true)
                            && !path.startsWith("/actuator")) {
                        log.info("[PERF] First HTTP request: {} {} — {} ms",
                                request.getMethod(), path, (System.nanoTime() - start) / 1_000_000);
                    }
                }
            }
        });
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

    private static void logDataSourcePool(String label, DataSource dataSource) {
        if (dataSource instanceof HikariDataSource hikari) {
            log.info("[PERF] {} — pool={}, active={}, idle={}, total={}, waiting={}",
                    label,
                    hikari.getPoolName(),
                    hikari.getHikariPoolMXBean() != null ? hikari.getHikariPoolMXBean().getActiveConnections() : "?",
                    hikari.getHikariPoolMXBean() != null ? hikari.getHikariPoolMXBean().getIdleConnections() : "?",
                    hikari.getHikariPoolMXBean() != null ? hikari.getHikariPoolMXBean().getTotalConnections() : "?",
                    hikari.getHikariPoolMXBean() != null ? hikari.getHikariPoolMXBean().getThreadsAwaitingConnection() : "?");
        } else {
            log.info("[PERF] {} — datasource={}", label, dataSource.getClass().getSimpleName());
        }
    }

    private static void logMemory(String label) {
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        long heapUsedMb = mem.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        long heapMaxMb = mem.getHeapMemoryUsage().getMax() / (1024 * 1024);
        long nonHeapMb = mem.getNonHeapMemoryUsage().getUsed() / (1024 * 1024);
        log.info("[PERF] {} — heap={}MB / {}MB max, non-heap={}MB",
                label, heapUsedMb, heapMaxMb, nonHeapMb);
    }

    private static String shortenPath(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash) : path;
    }
}
