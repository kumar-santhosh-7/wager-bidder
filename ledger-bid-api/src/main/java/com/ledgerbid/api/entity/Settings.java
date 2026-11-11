package com.ledgerbid.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "settings")
public class Settings {
    @Id
    private Long id;

    @Column(name = "house_edge", nullable = false)
    private double houseEdge;

    @Column(name = "min_bet", nullable = false)
    private int minBet;

    @Column(name = "max_bet", nullable = false)
    private int maxBet;

    @Column(nullable = false)
    private boolean maintenance;

    @Column(name = "home_description", length = 500)
    private String homeDescription;

    /** Percent (0-100) debited from a winner's payout when a match is settled. */
    @Column(name = "winner_fee_pct", nullable = false, columnDefinition = "DOUBLE NOT NULL DEFAULT 0")
    private double winnerFeePct;

    /** Percent (0-100) debited from each player's refund when a match ends in a draw. */
    @Column(name = "draw_fee_pct", nullable = false, columnDefinition = "DOUBLE NOT NULL DEFAULT 0")
    private double drawFeePct;
}
