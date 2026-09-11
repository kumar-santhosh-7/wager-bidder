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
}
