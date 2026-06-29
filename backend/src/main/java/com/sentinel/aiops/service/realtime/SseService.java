package com.sentinel.aiops.service.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Server-Sent Events hub. Connected dashboards subscribe once; the service
 * fans out live incident updates so the UI reflects changes without polling.
 * Chosen over WebSocket for simplicity (one-way server→client is all we need).
 */
@Service
@Slf4j
public class SseService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ObjectMapper mapper;

    public SseService(ObjectMapper mapper) { this.mapper = mapper; }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException ignored) {}
        return emitter;
    }

    public void broadcast(String event, Object payload) {
        String json;
        try {
            json = mapper.writeValueAsString(payload);
        } catch (Exception e) {
            json = "{}";
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(event).data(json));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }

    public int connectionCount() { return emitters.size(); }

    public Map<String, Object> status() {
        return Map.of("activeStreams", emitters.size());
    }
}
