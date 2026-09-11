package com.ledgerbid.api.dto;

import com.ledgerbid.api.entity.BidStatus;
import com.ledgerbid.api.entity.Side;

import java.time.Instant;

public record BidDto(
        String id,
        String userId,
        String roundId,
        Side side,
        int amount,
        int payout,
        BidStatus status,
        Instant createdAt
) {
}
