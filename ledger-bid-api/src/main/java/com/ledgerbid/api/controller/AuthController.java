package com.ledgerbid.api.controller;

import com.ledgerbid.api.config.WebConfig;
import com.ledgerbid.api.dto.LoginRequest;
import com.ledgerbid.api.dto.LoginResponse;
import com.ledgerbid.api.service.AuthService;
import com.ledgerbid.api.service.StateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService auth;
    private final StateService state;

    public AuthController(AuthService auth, StateService state) {
        this.auth = auth;
        this.state = state;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("ok", true, "service", "ledger-bid-api");
    }

    @PostMapping("/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req.username(), req.password());
    }

    @PostMapping("/auth/logout")
    public Map<String, Boolean> logout(@RequestHeader(value = "Authorization", required = false) String header) {
        String token = null;
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7).trim();
        }
        auth.logout(token);
        return Map.of("ok", true);
    }

    @GetMapping("/bootstrap")
    public ResponseEntity<?> bootstrap(HttpServletRequest request) {
        return ResponseEntity.ok(state.bootstrap(WebConfig.actor(request)));
    }
}
