package com.ledgerbid.api.dto;

import com.ledgerbid.api.entity.Role;

public record UserDto(
        String id,
        String username,
        String name,
        Role role,
        int coins,
        int wins,
        int losses
) {
}
