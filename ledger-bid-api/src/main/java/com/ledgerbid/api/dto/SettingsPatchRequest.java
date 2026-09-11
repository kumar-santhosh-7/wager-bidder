package com.ledgerbid.api.dto;

public record SettingsPatchRequest(
        Double houseEdge,
        Integer minBet,
        Integer maxBet,
        Boolean maintenance
) {
}
