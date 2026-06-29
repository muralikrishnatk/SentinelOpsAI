package com.sentinel.aiops.controller;

import com.sentinel.aiops.domain.enums.Role;
import com.sentinel.aiops.dto.AuthDtos.*;
import com.sentinel.aiops.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Auth & Users", description = "Authentication and user/role management")
public class AuthController {

    private final UserService users;

    public AuthController(UserService users) { this.users = users; }

    @PostMapping("/auth/register")
    @Operation(summary = "Self-service signup (always creates a VIEWER)")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return users.register(req, false);
    }

    @PostMapping("/auth/login")
    @Operation(summary = "Log in and receive a JWT")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return users.login(req);
    }

    @GetMapping("/auth/me")
    @Operation(summary = "Current user profile")
    public UserView me(Authentication auth) {
        return users.me(auth.getName());
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users (ADMIN)")
    public List<UserView> list() { return users.list(); }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a user with a chosen role (ADMIN)")
    public AuthResponse create(@Valid @RequestBody RegisterRequest req) {
        return users.register(req, true);
    }

    @PutMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change a user's role (ADMIN)")
    public UserView changeRole(@PathVariable Long id, @RequestParam Role role) {
        return users.changeRole(id, role);
    }
}
