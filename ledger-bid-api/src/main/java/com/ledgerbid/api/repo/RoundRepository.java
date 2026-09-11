package com.ledgerbid.api.repo;

import com.ledgerbid.api.entity.Round;
import com.ledgerbid.api.entity.RoundStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoundRepository extends JpaRepository<Round, String> {
    boolean existsByStatus(RoundStatus status);

    List<Round> findAllByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Round r where r.id = :id")
    Optional<Round> findLockedById(@Param("id") String id);
}
