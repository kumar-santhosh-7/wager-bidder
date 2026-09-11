package com.ledgerbid.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRoundRequest(
        @NotBlank String optionA,
        @NotBlank String optionB,
        @NotBlank String endsAt,
        String photoA,
        String photoB
) {
}
