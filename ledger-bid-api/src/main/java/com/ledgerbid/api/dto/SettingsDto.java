package com.ledgerbid.api.dto;

public record SettingsDto(
        double houseEdge,
        int minBet,
        int maxBet,
        boolean maintenance
) {
}
