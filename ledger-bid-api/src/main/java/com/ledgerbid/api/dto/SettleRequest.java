package com.ledgerbid.api.dto;

import com.ledgerbid.api.entity.Side;
import jakarta.validation.constraints.NotNull;

public record SettleRequest(@NotNull Side winner) {
}
