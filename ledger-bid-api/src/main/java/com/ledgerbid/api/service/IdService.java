package com.ledgerbid.api.service;

import com.ledgerbid.api.entity.LedgerEntry;
import com.ledgerbid.api.entity.LedgerType;
import com.ledgerbid.api.repo.LedgerEntryRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class IdService {
    private final LedgerEntryRepository ledgerRepo;

    public IdService(LedgerEntryRepository ledgerRepo) {
        this.ledgerRepo = ledgerRepo;
    }

    public String next(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    public LedgerEntry ledger(String userId, LedgerType type, int amount, String note) {
        LedgerEntry e = new LedgerEntry();
        e.setId(next("l"));
        e.setUserId(userId);
        e.setType(type);
        e.setAmount(amount);
        e.setNote(note);
        e.setCreatedAt(Instant.now());
        return ledgerRepo.save(e);
    }
}
