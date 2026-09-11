package com.ledgerbid.api.repo;

import com.ledgerbid.api.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BidRepository extends JpaRepository<Bid, String> {
    List<Bid> findAllByOrderByCreatedAtDesc();

    List<Bid> findByRoundId(String roundId);
}
