package com.crm;

import com.crm.config.performance.StartupPerformanceProfiler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class CrmApplication {
    public static void main(String[] args) {
        StartupPerformanceProfiler.markJvmStart();
        long bootstrapStart = System.nanoTime();
        SpringApplication.run(CrmApplication.class, args);
        StartupPerformanceProfiler.record("phase.spring-boot-run-total",
                System.nanoTime() - bootstrapStart);
    }
}
