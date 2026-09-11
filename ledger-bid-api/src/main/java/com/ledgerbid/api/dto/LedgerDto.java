package com.ledgerbid.api.dto;

import com.ledgerbid.api.entity.LedgerType;

import java.time.Instant;

public record LedgerDto(
        String id,
        String userId,
        LedgerType type,
        int amount,
        String note,
        Instant createdAt
) {
}
