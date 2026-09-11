package com.ledgerbid.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "auth_tokens")
public class AuthToken {
    @Id
    @Column(length = 64)
    private String token;

    @Column(name = "user_id", nullable = false, length = 40)
    private String userId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
