package com.ledgerbid.api.repo;

import com.ledgerbid.api.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, String> {
    List<LedgerEntry> findAllByOrderByCreatedAtDesc();
}
