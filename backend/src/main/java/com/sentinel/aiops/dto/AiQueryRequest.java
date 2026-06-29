package com.sentinel.aiops.dto;

import jakarta.validation.constraints.NotBlank;

/** Natural-language question for the AI assistant. */
public record AiQueryRequest(@NotBlank String question) {}
