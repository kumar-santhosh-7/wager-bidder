package com.ledgerbid.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
        @NotBlank String name,
        @NotBlank String username,
        @NotBlank String password,
        @Min(0) int coins
) {
}
