package com.ledgerbid.api.repo;

import com.ledgerbid.api.entity.Settings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingsRepository extends JpaRepository<Settings, Long> {
}
