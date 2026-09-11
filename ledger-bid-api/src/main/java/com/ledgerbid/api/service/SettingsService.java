package com.ledgerbid.api.service;

import com.ledgerbid.api.dto.SettingsDto;
import com.ledgerbid.api.dto.SettingsPatchRequest;
import com.ledgerbid.api.dto.Mappers;
import com.ledgerbid.api.entity.Settings;
import com.ledgerbid.api.repo.SettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {
    private final SettingsRepository settings;

    public SettingsService(SettingsRepository settings) {
        this.settings = settings;
    }

    public Settings current() {
        return settings.findById(1L).orElseThrow();
    }

    @Transactional
    public SettingsDto patch(SettingsPatchRequest req) {
        Settings s = current();
        if (req.houseEdge() != null) {
            s.setHouseEdge(Math.min(0.4, Math.max(0, req.houseEdge())));
        }
        if (req.minBet() != null) {
            s.setMinBet(Math.max(1, req.minBet()));
        }
        if (req.maxBet() != null) {
            s.setMaxBet(Math.max(s.getMinBet(), req.maxBet()));
        }
        if (req.maintenance() != null) {
            s.setMaintenance(req.maintenance());
        }
        return Mappers.settings(settings.save(s));
    }
}
