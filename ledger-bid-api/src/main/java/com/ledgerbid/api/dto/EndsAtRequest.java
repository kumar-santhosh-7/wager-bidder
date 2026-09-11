package com.ledgerbid.api.dto;

import jakarta.validation.constraints.NotBlank;

public record EndsAtRequest(@NotBlank String endsAt) {
}
