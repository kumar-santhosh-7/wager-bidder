package com.ledgerbid.api.service;

import com.ledgerbid.api.dto.SettingsDto;
import com.ledgerbid.api.dto.SettingsPatchRequest;
import com.ledgerbid.api.dto.Mappers;
import com.ledgerbid.api.entity.Settings;
import com.ledgerbid.api.error.ApiException;
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
        if (req.homeDescription() != null) {
            String text = req.homeDescription().trim();
            s.setHomeDescription(text.isEmpty() ? null : text);
        }
        if (req.winnerFeePct() != null) {
            s.setWinnerFeePct(feePct(req.winnerFeePct(), "Winner %"));
        }
        if (req.drawFeePct() != null) {
            s.setDrawFeePct(feePct(req.drawFeePct(), "Draw %"));
        }
        return Mappers.settings(settings.save(s));
    }

    private double feePct(double value, String label) {
        if (Double.isNaN(value) || value < 0 || value > 100) {
            throw ApiException.bad(label + " must be between 0 and 100");
        }
        return value;
    }
}
