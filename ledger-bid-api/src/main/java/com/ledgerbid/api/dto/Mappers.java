package com.ledgerbid.api.dto;

import com.ledgerbid.api.entity.Bid;
import com.ledgerbid.api.entity.LedgerEntry;
import com.ledgerbid.api.entity.Round;
import com.ledgerbid.api.entity.Settings;
import com.ledgerbid.api.entity.UserAccount;

public final class Mappers {
    private Mappers() {
    }

    public static UserDto user(UserAccount u) {
        return new UserDto(u.getId(), u.getUsername(), u.getName(), u.getRole(), u.getCoins(), u.getWins(), u.getLosses());
    }

    public static RoundDto round(Round r) {
        return new RoundDto(
                r.getId(),
                r.getOptionA(),
                r.getOptionB(),
                r.getPoolA(),
                r.getPoolB(),
                r.getStatus(),
                r.getWinner(),
                r.getCreatedAt(),
                r.getEndsAt(),
                r.getPhotoA(),
                r.getPhotoB()
        );
    }

    public static BidDto bid(Bid b) {
        return new BidDto(b.getId(), b.getUserId(), b.getRoundId(), b.getSide(), b.getAmount(), b.getPayout(), b.getStatus(), b.getCreatedAt());
    }

    public static LedgerDto ledger(LedgerEntry e) {
        return new LedgerDto(e.getId(), e.getUserId(), e.getType(), e.getAmount(), e.getNote(), e.getCreatedAt());
    }

    public static SettingsDto settings(Settings s) {
        return new SettingsDto(s.getHouseEdge(), s.getMinBet(), s.getMaxBet(), s.isMaintenance());
    }
}
