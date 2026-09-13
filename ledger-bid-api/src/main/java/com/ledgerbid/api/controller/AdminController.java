package com.ledgerbid.api.controller;

import com.ledgerbid.api.config.WebConfig;
import com.ledgerbid.api.dto.CreateUserRequest;
import com.ledgerbid.api.dto.CreditRequest;
import com.ledgerbid.api.dto.SettingsPatchRequest;
import com.ledgerbid.api.dto.UserActiveRequest;
import com.ledgerbid.api.dto.UserDto;
import com.ledgerbid.api.service.SettingsService;
import com.ledgerbid.api.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AdminController {
    private final UserService users;
    private final SettingsService settings;

    public AdminController(UserService users, SettingsService settings) {
        this.users = users;
        this.settings = settings;
    }

    @PostMapping("/users")
    public UserDto createUser(HttpServletRequest request, @Valid @RequestBody CreateUserRequest req) {
        WebConfig.admin(request);
        return users.createPlayer(req);
    }

    @PostMapping("/users/{id}/credit")
    public Map<String, Boolean> credit(HttpServletRequest request, @PathVariable String id, @Valid @RequestBody CreditRequest req) {
        WebConfig.admin(request);
        users.credit(id, req);
        return Map.of("ok", true);
    }

    @PatchMapping("/users/{id}/active")
    public Map<String, Boolean> setActive(HttpServletRequest request, @PathVariable String id, @RequestBody UserActiveRequest req) {
        WebConfig.admin(request);
        users.setActive(WebConfig.admin(request).getId(), id, req.active());
        return Map.of("ok", true);
    }

    @DeleteMapping("/users/{id}")
    public Map<String, Boolean> deleteUser(HttpServletRequest request, @PathVariable String id) {
        WebConfig.admin(request);
        users.deletePlayer(WebConfig.admin(request).getId(), id);
        return Map.of("ok", true);
    }

    @PatchMapping("/settings")
    public Map<String, Boolean> patchSettings(HttpServletRequest request, @RequestBody SettingsPatchRequest req) {
        WebConfig.admin(request);
        settings.patch(req);
        return Map.of("ok", true);
    }
}
