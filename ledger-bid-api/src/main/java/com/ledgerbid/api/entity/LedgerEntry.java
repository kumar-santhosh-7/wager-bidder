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
@Table(name = "ledger_entries")
public class LedgerEntry {
    @Id
    private String id;

    @Column(name = "user_id", nullable = false, length = 40)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private LedgerType type;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false, length = 255)
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
