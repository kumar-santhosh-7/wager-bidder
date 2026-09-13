package com.ledgerbid.api.service;

import com.ledgerbid.api.dto.CreateUserRequest;
import com.ledgerbid.api.dto.CreditRequest;
import com.ledgerbid.api.dto.Mappers;
import com.ledgerbid.api.dto.UserDto;
import com.ledgerbid.api.entity.LedgerType;
import com.ledgerbid.api.entity.Role;
import com.ledgerbid.api.entity.UserAccount;
import com.ledgerbid.api.error.ApiException;
import com.ledgerbid.api.repo.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserAccountRepository users;
    private final AuthService auth;
    private final IdService ids;

    public UserService(UserAccountRepository users, AuthService auth, IdService ids) {
        this.users = users;
        this.auth = auth;
        this.ids = ids;
    }

    @Transactional
    public UserDto createPlayer(CreateUserRequest req) {
        if (users.existsByUsernameIgnoreCase(req.username().trim())) {
            throw ApiException.bad("Username taken");
        }
        UserAccount u = new UserAccount();
        u.setId(ids.next("u"));
        u.setName(req.name().trim());
        u.setUsername(req.username().trim().toLowerCase());
        u.setPasswordHash(auth.encoder().encode(req.password()));
        u.setRole(Role.PLAYER);
        u.setCoins(req.coins());
        u.setWins(0);
        u.setLosses(0);
        u.setActive(true);
        users.save(u);
        if (req.coins() > 0) {
            ids.ledger(u.getId(), LedgerType.CREDIT, req.coins(), "Opening balance");
        }
        return Mappers.user(u);
    }

    @Transactional
    public void credit(String userId, CreditRequest req) {
        UserAccount user = users.findLockedById(userId).orElseThrow(() -> ApiException.notFound("Player not found"));
        if (user.getRole() != Role.PLAYER) {
            throw ApiException.bad("Player not found");
        }
        user.setCoins(user.getCoins() + req.amount());
        users.save(user);
        String note = req.note() == null || req.note().isBlank() ? "Admin credit" : req.note().trim();
        ids.ledger(userId, LedgerType.CREDIT, req.amount(), note);
    }

    @Transactional
    public void setActive(String actorId, String userId, boolean active) {
        UserAccount user = users.findById(userId).orElseThrow(() -> ApiException.notFound("Player not found"));
        if (user.getRole() != Role.PLAYER) {
            throw ApiException.bad("Only players can be activated or deactivated");
        }
        if (user.getId().equals(actorId)) {
            throw ApiException.bad("You cannot change your own account status");
        }
        user.setActive(active);
        users.save(user);
        if (!active) {
            auth.revokeUser(userId);
        }
    }

    @Transactional
    public void deletePlayer(String actorId, String userId) {
        UserAccount user = users.findById(userId).orElseThrow(() -> ApiException.notFound("Player not found"));
        if (user.getRole() != Role.PLAYER) {
            throw ApiException.bad("Only players can be deleted");
        }
        if (user.getId().equals(actorId)) {
            throw ApiException.bad("You cannot delete your own account");
        }
        auth.revokeUser(userId);
        users.delete(user);
    }
}
