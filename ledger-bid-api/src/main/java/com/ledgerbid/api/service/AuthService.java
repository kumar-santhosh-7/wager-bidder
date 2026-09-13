package com.ledgerbid.api.service;

import com.ledgerbid.api.dto.LoginResponse;
import com.ledgerbid.api.dto.Mappers;
import com.ledgerbid.api.entity.AuthToken;
import com.ledgerbid.api.entity.UserAccount;
import com.ledgerbid.api.error.ApiException;
import com.ledgerbid.api.repo.AuthTokenRepository;
import com.ledgerbid.api.repo.UserAccountRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {
    private final UserAccountRepository users;
    private final AuthTokenRepository tokens;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(UserAccountRepository users, AuthTokenRepository tokens) {
        this.users = users;
        this.tokens = tokens;
    }

    public PasswordEncoder encoder() {
        return encoder;
    }

    @Transactional
    public LoginResponse login(String username, String password) {
        UserAccount user = users.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> ApiException.unauthorized("Invalid credentials"));
        if (!encoder.matches(password, user.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid credentials");
        }
        if (!user.isActive()) {
            throw ApiException.forbidden("Account is inactive");
        }
        AuthToken token = new AuthToken();
        token.setToken(UUID.randomUUID().toString().replace("-", ""));
        token.setUserId(user.getId());
        token.setCreatedAt(Instant.now());
        tokens.save(token);
        return new LoginResponse(token.getToken(), Mappers.user(user));
    }

    @Transactional
    public void revokeUser(String userId) {
        tokens.deleteByUserId(userId);
    }

    @Transactional
    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            tokens.deleteById(token);
        }
    }

    public UserAccount requireUser(String token) {
        if (token == null || token.isBlank()) {
            throw ApiException.unauthorized("Login required");
        }
        AuthToken row = tokens.findById(token).orElseThrow(() -> ApiException.unauthorized("Session expired"));
        UserAccount user = users.findById(row.getUserId()).orElseThrow(() -> ApiException.unauthorized("User not found"));
        if (!user.isActive()) {
            throw ApiException.forbidden("Account is inactive");
        }
        return user;
    }
}
