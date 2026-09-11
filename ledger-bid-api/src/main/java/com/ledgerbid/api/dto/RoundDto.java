package com.ledgerbid.api.dto;

import com.ledgerbid.api.entity.RoundStatus;
import com.ledgerbid.api.entity.Side;

import java.time.Instant;

public record RoundDto(
        String id,
        String optionA,
        String optionB,
        int poolA,
        int poolB,
        RoundStatus status,
        Side winner,
        Instant createdAt,
        Instant endsAt,
        String photoA,
        String photoB
) {
}
