package com.sentinel.aiops.service;

import com.sentinel.aiops.domain.User;
import com.sentinel.aiops.domain.enums.Role;
import com.sentinel.aiops.dto.AuthDtos.*;
import com.sentinel.aiops.exception.NotFoundException;
import com.sentinel.aiops.repository.UserRepository;
import com.sentinel.aiops.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuthenticationManager authManager;

    public UserService(UserRepository repo, PasswordEncoder encoder,
                       JwtService jwt, AuthenticationManager authManager) {
        this.repo = repo;
        this.encoder = encoder;
        this.jwt = jwt;
        this.authManager = authManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req, boolean allowRoleChoice) {
        if (repo.existsByUsername(req.username())) {
            throw new IllegalArgumentException("Username already taken");
        }
        Role role = (allowRoleChoice && req.role() != null) ? req.role() : Role.VIEWER;
        User u = User.builder()
                .username(req.username())
                .passwordHash(encoder.encode(req.password()))
                .displayName(req.displayName() == null ? req.username() : req.displayName())
                .email(req.email())
                .role(role)
                .team(req.team())
                .enabled(true)
                .build();
        repo.save(u);
        return toAuth(u);
    }

    public AuthResponse login(LoginRequest req) {
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        } catch (BadCredentialsException e) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        User u = repo.findByUsername(req.username())
                .orElseThrow(() -> new NotFoundException("User not found"));
        return toAuth(u);
    }

    @Transactional(readOnly = true)
    public UserView me(String username) {
        return toView(repo.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found")));
    }

    @Transactional(readOnly = true)
    public List<UserView> list() {
        return repo.findAll().stream().map(this::toView).toList();
    }

    @Transactional
    public UserView changeRole(Long id, Role role) {
        User u = repo.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        u.setRole(role);
        return toView(repo.save(u));
    }

    private AuthResponse toAuth(User u) {
        String token = jwt.issue(u.getUsername(), u.getRole().name(), u.getTeam());
        return new AuthResponse(token, u.getUsername(), u.getRole().name(),
                u.getDisplayName(), u.getTeam());
    }

    private UserView toView(User u) {
        return new UserView(u.getId(), u.getUsername(), u.getDisplayName(),
                u.getEmail(), u.getRole(), u.getTeam(), u.isEnabled());
    }
}
