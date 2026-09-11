package com.ledgerbid.api.repo;

import com.ledgerbid.api.entity.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuthTokenRepository extends JpaRepository<AuthToken, String> {
    List<AuthToken> findByUserId(String userId);

    void deleteByUserId(String userId);
}
