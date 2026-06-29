package com.sentinel.aiops.controller;

import com.sentinel.aiops.dto.ServiceDtos.*;
import com.sentinel.aiops.service.ServiceCatalogService;
import com.sentinel.aiops.service.realtime.SseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Services & Realtime", description = "Service catalog and live update stream")
public class ServiceCatalogController {

    private final ServiceCatalogService services;
    private final SseService sse;

    public ServiceCatalogController(ServiceCatalogService services, SseService sse) {
        this.services = services;
        this.sse = sse;
    }

    @GetMapping("/services")
    @Operation(summary = "List catalog services with derived health")
    public List<ServiceView> list() { return services.list(); }

    @PostMapping("/services")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register a service (ADMIN)")
    public ServiceView create(@Valid @RequestBody ServiceRequest req) {
        return services.create(req);
    }

    @DeleteMapping("/services/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove a service (ADMIN)")
    public void delete(@PathVariable Long id) { services.delete(id); }

    @GetMapping(value = "/stream", produces = "text/event-stream")
    @Operation(summary = "Subscribe to live incident updates (SSE)")
    public SseEmitter stream() { return sse.subscribe(); }
}
