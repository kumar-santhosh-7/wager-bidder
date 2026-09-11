package com.ledgerbid.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreditRequest(
        @Min(1) int amount,
        @NotBlank String note
) {
}
