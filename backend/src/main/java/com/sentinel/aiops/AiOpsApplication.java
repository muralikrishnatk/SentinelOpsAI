package com.sentinel.aiops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Sentinel AIOps Platform.
 *
 * <p>An AI-powered incident management and observability platform. The Spring
 * container itself is the canonical example of the <b>Singleton</b> and
 * <b>Dependency Injection</b> patterns: every {@code @Component}, {@code @Service}
 * and {@code @Repository} bean is created once and injected wherever needed.</p>
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class AiOpsApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiOpsApplication.class, args);
    }
}
