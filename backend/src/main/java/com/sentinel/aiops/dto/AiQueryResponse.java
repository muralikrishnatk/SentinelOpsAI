package com.sentinel.aiops.dto;

/** Answer from the AI assistant plus which provider served it. */
public record AiQueryResponse(String answer, String provider, boolean degraded) {}
