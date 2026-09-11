package com.ledgerbid.api.dto;

import com.ledgerbid.api.entity.Side;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlaceBidRequest(
        @NotBlank String roundId,
        @NotNull Side side,
        @Min(1) int amount
) {
}
