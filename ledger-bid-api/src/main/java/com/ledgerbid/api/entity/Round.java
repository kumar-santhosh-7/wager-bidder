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
@Table(name = "rounds")
public class Round {
    @Id
    private String id;

    @Column(name = "option_a", nullable = false, length = 80)
    private String optionA;

    @Column(name = "option_b", nullable = false, length = 80)
    private String optionB;

    @Column(name = "pool_a", nullable = false)
    private int poolA;

    @Column(name = "pool_b", nullable = false)
    private int poolB;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RoundStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 8)
    private Side winner;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "photo_a", length = 500)
    private String photoA;

    @Column(name = "photo_b", length = 500)
    private String photoB;
}
