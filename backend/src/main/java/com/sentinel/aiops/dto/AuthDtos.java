package com.sentinel.aiops.dto;

import com.sentinel.aiops.domain.enums.Role;
import jakarta.validation.constraints.NotBlank;

/** Auth request/response payloads. */
public class AuthDtos {

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record RegisterRequest(
            @NotBlank String username,
            @NotBlank String password,
            String displayName,
            String email,
            Role role,           // ADMIN-only field; ignored for self-signup
            String team) {}

    public record AuthResponse(String token, String username, String role,
                               String displayName, String team) {}

    public record UserView(Long id, String username, String displayName,
                           String email, Role role, String team, boolean enabled) {}
}
