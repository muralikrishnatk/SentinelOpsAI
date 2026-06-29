package com.sentinel.aiops.service.command;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Executes {@link IncidentCommand}s, recording an audit history and emitting a
 * timing metric per command. A single choke point for every mutating operation.
 */
@Component
@Slf4j
public class CommandInvoker {

    private final MeterRegistry registry;
    private final Deque<String> history = new ArrayDeque<>();

    public CommandInvoker(MeterRegistry registry) { this.registry = registry; }

    public <T> T run(IncidentCommand<T> command) {
        Timer.Sample sample = Timer.start(registry);
        try {
            T result = command.execute();
            history.push(command.describe());
            log.info("CMD ok: {}", command.describe());
            return result;
        } finally {
            sample.stop(registry.timer("aiops.command.duration",
                    "command", command.getClass().getSimpleName()));
        }
    }

    public List<String> recentHistory() {
        return history.stream().limit(20).toList();
    }
}
