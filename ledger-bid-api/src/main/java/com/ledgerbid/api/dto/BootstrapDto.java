package com.ledgerbid.api.dto;

import java.util.List;

public record BootstrapDto(
        UserDto me,
        List<UserDto> users,
        List<RoundDto> rounds,
        List<BidDto> bids,
        List<LedgerDto> ledger,
        SettingsDto settings
) {
}
