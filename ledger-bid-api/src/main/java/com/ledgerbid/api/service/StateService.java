package com.ledgerbid.api.service;

import com.ledgerbid.api.dto.BootstrapDto;
import com.ledgerbid.api.dto.Mappers;
import com.ledgerbid.api.entity.Settings;
import com.ledgerbid.api.entity.UserAccount;
import com.ledgerbid.api.repo.BidRepository;
import com.ledgerbid.api.repo.LedgerEntryRepository;
import com.ledgerbid.api.repo.RoundRepository;
import com.ledgerbid.api.repo.SettingsRepository;
import com.ledgerbid.api.repo.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StateService {
    private final UserAccountRepository users;
    private final RoundRepository rounds;
    private final BidRepository bids;
    private final LedgerEntryRepository ledger;
    private final SettingsRepository settings;

    public StateService(
            UserAccountRepository users,
            RoundRepository rounds,
            BidRepository bids,
            LedgerEntryRepository ledger,
            SettingsRepository settings
    ) {
        this.users = users;
        this.rounds = rounds;
        this.bids = bids;
        this.ledger = ledger;
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public BootstrapDto bootstrap(UserAccount me) {
        Settings s = settings.findById(1L).orElseThrow();
        return new BootstrapDto(
                Mappers.user(me),
                users.findAll().stream().map(Mappers::user).toList(),
                rounds.findAllByOrderByCreatedAtDesc().stream().map(Mappers::round).toList(),
                bids.findAllByOrderByCreatedAtDesc().stream().map(Mappers::bid).toList(),
                ledger.findAllByOrderByCreatedAtDesc().stream().map(Mappers::ledger).toList(),
                Mappers.settings(s)
        );
    }
}
