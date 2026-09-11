package com.ledgerbid.api.seed;

import com.ledgerbid.api.entity.Bid;
import com.ledgerbid.api.entity.BidStatus;
import com.ledgerbid.api.entity.LedgerEntry;
import com.ledgerbid.api.entity.LedgerType;
import com.ledgerbid.api.entity.Role;
import com.ledgerbid.api.entity.Round;
import com.ledgerbid.api.entity.RoundStatus;
import com.ledgerbid.api.entity.Settings;
import com.ledgerbid.api.entity.Side;
import com.ledgerbid.api.entity.UserAccount;
import com.ledgerbid.api.repo.BidRepository;
import com.ledgerbid.api.repo.LedgerEntryRepository;
import com.ledgerbid.api.repo.RoundRepository;
import com.ledgerbid.api.repo.SettingsRepository;
import com.ledgerbid.api.repo.UserAccountRepository;
import com.ledgerbid.api.service.AuthService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class DemoDataSeeder implements CommandLineRunner {
    private final UserAccountRepository users;
    private final RoundRepository rounds;
    private final BidRepository bids;
    private final LedgerEntryRepository ledger;
    private final SettingsRepository settings;
    private final AuthService auth;

    public DemoDataSeeder(
            UserAccountRepository users,
            RoundRepository rounds,
            BidRepository bids,
            LedgerEntryRepository ledger,
            SettingsRepository settings,
            AuthService auth
    ) {
        this.users = users;
        this.rounds = rounds;
        this.bids = bids;
        this.ledger = ledger;
        this.settings = settings;
        this.auth = auth;
    }

    @Override
    public void run(String... args) {
        if (settings.count() == 0) {
            Settings s = new Settings();
            s.setId(1L);
            s.setHouseEdge(0.08);
            s.setMinBet(50);
            s.setMaxBet(5000);
            s.setMaintenance(false);
            settings.save(s);
        }
        if (users.count() > 0) {
            return;
        }
        Instant now = Instant.now();
        String hash1234 = auth.encoder().encode("1234");
        users.save(user("u-admin", "admin", auth.encoder().encode("admin123"), "House Admin", Role.ADMIN, 0, 0, 0));
        users.save(user("u-arun", "arun", hash1234, "Arun Kumar", Role.PLAYER, 2480, 12, 7));
        users.save(user("u-meera", "meera", hash1234, "Meera Shah", Role.PLAYER, 1760, 8, 9));
        users.save(user("u-kiran", "kiran", hash1234, "Kiran Rao", Role.PLAYER, 920, 4, 11));
        users.save(user("u-priya", "priya", hash1234, "Priya Nair", Role.PLAYER, 3340, 15, 6));

        rounds.save(round("r-live", "Lions", "Tigers", 4200, 3100, RoundStatus.LIVE, null, now.minus(12, ChronoUnit.MINUTES), now.plus(25, ChronoUnit.MINUTES)));
        rounds.save(round("r-2", "Gold", "Blue", 5100, 6400, RoundStatus.SETTLED, Side.B, now.minus(6, ChronoUnit.HOURS), now.minus(5, ChronoUnit.HOURS).minus(54, ChronoUnit.MINUTES)));
        rounds.save(round("r-3", "Dawn", "Dusk", 2800, 2200, RoundStatus.SETTLED, Side.A, now.minus(22, ChronoUnit.HOURS), now.minus(21, ChronoUnit.HOURS).minus(54, ChronoUnit.MINUTES)));

        bids.save(bid("b-1", "u-arun", "r-2", Side.B, 400, 668, BidStatus.WON, now.minus(6, ChronoUnit.HOURS)));
        bids.save(bid("b-2", "u-arun", "r-3", Side.B, 200, 0, BidStatus.LOST, now.minus(22, ChronoUnit.HOURS)));
        bids.save(bid("b-3", "u-meera", "r-live", Side.A, 300, 0, BidStatus.PENDING, now.minus(8, ChronoUnit.MINUTES)));
        bids.save(bid("b-4", "u-priya", "r-live", Side.B, 500, 0, BidStatus.PENDING, now.minus(5, ChronoUnit.MINUTES)));

        ledger.save(entry("l-1", "u-arun", LedgerType.CREDIT, 3000, "Opening balance", now.minus(48, ChronoUnit.HOURS)));
        ledger.save(entry("l-2", "u-arun", LedgerType.BET, -200, "Bid on Dusk · Dawn vs Dusk", now.minus(22, ChronoUnit.HOURS)));
        ledger.save(entry("l-3", "u-arun", LedgerType.BET, -400, "Bid on Blue · Gold vs Blue", now.minus(6, ChronoUnit.HOURS)));
        ledger.save(entry("l-4", "u-arun", LedgerType.PAYOUT, 668, "Win payout · Gold vs Blue", now.minus(5, ChronoUnit.HOURS).minus(48, ChronoUnit.MINUTES)));
        ledger.save(entry("l-5", "u-meera", LedgerType.BET, -300, "Bid on Lions · live", now.minus(8, ChronoUnit.MINUTES)));
    }

    private UserAccount user(String id, String username, String hash, String name, Role role, int coins, int wins, int losses) {
        UserAccount u = new UserAccount();
        u.setId(id);
        u.setUsername(username);
        u.setPasswordHash(hash);
        u.setName(name);
        u.setRole(role);
        u.setCoins(coins);
        u.setWins(wins);
        u.setLosses(losses);
        return u;
    }

    private Round round(String id, String a, String b, int poolA, int poolB, RoundStatus status, Side winner, Instant created, Instant ends) {
        Round r = new Round();
        r.setId(id);
        r.setOptionA(a);
        r.setOptionB(b);
        r.setPoolA(poolA);
        r.setPoolB(poolB);
        r.setStatus(status);
        r.setWinner(winner);
        r.setCreatedAt(created);
        r.setEndsAt(ends);
        return r;
    }

    private Bid bid(String id, String userId, String roundId, Side side, int amount, int payout, BidStatus status, Instant created) {
        Bid b = new Bid();
        b.setId(id);
        b.setUserId(userId);
        b.setRoundId(roundId);
        b.setSide(side);
        b.setAmount(amount);
        b.setPayout(payout);
        b.setStatus(status);
        b.setCreatedAt(created);
        return b;
    }

    private LedgerEntry entry(String id, String userId, LedgerType type, int amount, String note, Instant created) {
        LedgerEntry e = new LedgerEntry();
        e.setId(id);
        e.setUserId(userId);
        e.setType(type);
        e.setAmount(amount);
        e.setNote(note);
        e.setCreatedAt(created);
        return e;
    }
}
