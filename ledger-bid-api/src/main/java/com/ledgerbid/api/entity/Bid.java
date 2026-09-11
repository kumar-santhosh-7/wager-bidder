package com.ledgerbid.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "bids")
public class Bid {
    @Id
    private String id;

    @Column(name = "user_id", nullable = false, length = 40)
    private String userId;

    @Column(name = "round_id", nullable = false, length = 40)
    private String roundId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Side side;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false)
    private int payout;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private BidStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
