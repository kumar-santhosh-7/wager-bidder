package com.ledgerbid.api.service;

import com.ledgerbid.api.dto.CreateRoundRequest;
import com.ledgerbid.api.dto.PlaceBidRequest;
import com.ledgerbid.api.dto.PlaceBidResponse;
import com.ledgerbid.api.entity.Bid;
import com.ledgerbid.api.entity.BidStatus;
import com.ledgerbid.api.entity.LedgerType;
import com.ledgerbid.api.entity.Role;
import com.ledgerbid.api.entity.Round;
import com.ledgerbid.api.entity.RoundStatus;
import com.ledgerbid.api.entity.Settings;
import com.ledgerbid.api.entity.Side;
import com.ledgerbid.api.entity.UserAccount;
import com.ledgerbid.api.error.ApiException;
import com.ledgerbid.api.repo.BidRepository;
import com.ledgerbid.api.repo.RoundRepository;
import com.ledgerbid.api.repo.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class HouseService {
    private final RoundRepository rounds;
    private final BidRepository bids;
    private final UserAccountRepository users;
    private final SettingsService settingsService;
    private final IdService ids;
    private final int withdrawLockMinutes;

    public HouseService(
            RoundRepository rounds,
            BidRepository bids,
            UserAccountRepository users,
            SettingsService settingsService,
            IdService ids,
            @Value("${ledgerbid.withdraw-lock-minutes:5}") int withdrawLockMinutes
    ) {
        this.rounds = rounds;
        this.bids = bids;
        this.users = users;
        this.settingsService = settingsService;
        this.ids = ids;
        this.withdrawLockMinutes = withdrawLockMinutes;
    }

    public static double payoutMultiplier(int poolSide, int poolOther, double houseEdge) {
        int total = poolSide + poolOther;
        if (poolSide <= 0) {
            return 0;
        }
        return (total * (1 - houseEdge)) / poolSide;
    }

    private Instant parseFuture(String endsAt) {
        Instant t;
        try {
            t = Instant.parse(endsAt);
        } catch (Exception e) {
            throw ApiException.bad("Invalid end time");
        }
        if (!t.isAfter(Instant.now())) {
            throw ApiException.bad("Set a bidding end time in the future");
        }
        return t;
    }

    private boolean biddingOpen(Round round) {
        return round.getStatus() == RoundStatus.LIVE && round.getEndsAt().isAfter(Instant.now());
    }

    private boolean canWithdraw(Round round) {
        if (round.getStatus() != RoundStatus.LIVE) {
            return false;
        }
        return Instant.now().plus(withdrawLockMinutes, ChronoUnit.MINUTES).isBefore(round.getEndsAt());
    }

    @Transactional
    public void createRound(CreateRoundRequest req) {
        if (rounds.existsByStatus(RoundStatus.LIVE)) {
            throw ApiException.bad("A live round already exists");
        }
        Instant endsAt = parseFuture(req.endsAt());
        Round r = new Round();
        r.setId(ids.next("r"));
        r.setOptionA(req.optionA().trim());
        r.setOptionB(req.optionB().trim());
        r.setPoolA(0);
        r.setPoolB(0);
        r.setStatus(RoundStatus.LIVE);
        r.setCreatedAt(Instant.now());
        r.setEndsAt(endsAt);
        r.setPhotoA(blankToNull(req.photoA()));
        r.setPhotoB(blankToNull(req.photoB()));
        rounds.save(r);
    }

    @Transactional
    public void setPhotos(String roundId, String photoA, String photoB) {
        Round round = rounds.findById(roundId).orElseThrow(() -> ApiException.notFound("Round not found"));
        if (photoA != null) {
            round.setPhotoA(blankToNull(photoA));
        }
        if (photoB != null) {
            round.setPhotoB(blankToNull(photoB));
        }
        rounds.save(round);
    }

    @Transactional
    public void setEndsAt(String roundId, String endsAtRaw) {
        Round round = rounds.findById(roundId).orElseThrow(() -> ApiException.notFound("Round not found"));
        if (round.getStatus() == RoundStatus.SETTLED) {
            throw ApiException.bad("Round already settled");
        }
        round.setEndsAt(parseFuture(endsAtRaw));
        rounds.save(round);
    }

    @Transactional
    public PlaceBidResponse placeBid(UserAccount actor, PlaceBidRequest req) {
        if (actor.getRole() != Role.PLAYER) {
            throw ApiException.forbidden("Login as a player");
        }
        Settings settings = settingsService.current();
        if (settings.isMaintenance()) {
            throw ApiException.bad("House is in maintenance");
        }
        if (req.amount() < settings.getMinBet() || req.amount() > settings.getMaxBet()) {
            throw ApiException.bad("Bet must be " + settings.getMinBet() + "–" + settings.getMaxBet());
        }
        Round round = rounds.findLockedById(req.roundId()).orElseThrow(() -> ApiException.bad("Round is not live"));
        if (round.getStatus() != RoundStatus.LIVE || !biddingOpen(round)) {
            throw ApiException.bad("Bidding has ended");
        }
        UserAccount player = users.findLockedById(actor.getId()).orElseThrow(() -> ApiException.unauthorized("Login as a player"));
        if (!player.isActive()) {
            throw ApiException.forbidden("Account is inactive");
        }
        if (player.getCoins() < req.amount()) {
            throw ApiException.bad("Not enough coins");
        }
        player.setCoins(player.getCoins() - req.amount());
        users.save(player);
        Bid bid = new Bid();
        bid.setId(ids.next("b"));
        bid.setUserId(player.getId());
        bid.setRoundId(round.getId());
        bid.setSide(req.side());
        bid.setAmount(req.amount());
        bid.setPayout(0);
        bid.setStatus(BidStatus.REQUESTED);
        bid.setCreatedAt(Instant.now());
        bids.save(bid);
        String sideName = req.side() == Side.A ? round.getOptionA() : round.getOptionB();
        ids.ledger(
                player.getId(),
                LedgerType.BET,
                -req.amount(),
                "Bid request on " + sideName + " · " + round.getOptionA() + " vs " + round.getOptionB()
        );
        return new PlaceBidResponse(bid.getId());
    }

    @Transactional
    public void approveBid(String bidId) {
        Bid bid = bids.findById(bidId).orElseThrow(() -> ApiException.notFound("Bid not found"));
        if (bid.getStatus() != BidStatus.REQUESTED) {
            throw ApiException.bad("Only requested bids can be approved");
        }
        Round round = rounds.findLockedById(bid.getRoundId()).orElseThrow(() -> ApiException.bad("Round is not live"));
        if (round.getStatus() != RoundStatus.LIVE) {
            throw ApiException.bad("Round is locked");
        }
        if (bid.getSide() == Side.A) {
            round.setPoolA(round.getPoolA() + bid.getAmount());
        } else {
            round.setPoolB(round.getPoolB() + bid.getAmount());
        }
        rounds.save(round);
        bid.setStatus(BidStatus.PENDING);
        bids.save(bid);
    }

    @Transactional
    public void rejectBid(String bidId) {
        Bid bid = bids.findById(bidId).orElseThrow(() -> ApiException.notFound("Bid not found"));
        if (bid.getStatus() != BidStatus.REQUESTED) {
            throw ApiException.bad("Only requested bids can be declined");
        }
        Round round = rounds.findById(bid.getRoundId()).orElseThrow(() -> ApiException.notFound("Round not found"));
        refundRequest(bid, round, "Bid request declined · " + round.getOptionA() + " vs " + round.getOptionB());
        bid.setStatus(BidStatus.REJECTED);
        bids.save(bid);
    }

    private void refundRequest(Bid bid, Round round, String note) {
        UserAccount player = users.findLockedById(bid.getUserId()).orElseThrow(() -> ApiException.notFound("Player not found"));
        player.setCoins(player.getCoins() + bid.getAmount());
        users.save(player);
        ids.ledger(player.getId(), LedgerType.CREDIT, bid.getAmount(), note);
    }

    @Transactional
    public void withdrawBid(UserAccount actor, String bidId) {
        if (actor.getRole() != Role.PLAYER) {
            throw ApiException.forbidden("Login as a player");
        }
        Bid bid = bids.findById(bidId).orElseThrow(() -> ApiException.notFound("Bid not found"));
        if (!bid.getUserId().equals(actor.getId())) {
            throw ApiException.forbidden("Not your bid");
        }
        if (bid.getStatus() != BidStatus.PENDING && bid.getStatus() != BidStatus.REQUESTED) {
            throw ApiException.bad("This bid cannot be withdrawn");
        }
        Round round = rounds.findLockedById(bid.getRoundId()).orElseThrow(() -> ApiException.bad("Round is locked. Cannot withdraw"));
        if (round.getStatus() != RoundStatus.LIVE) {
            throw ApiException.bad("Round is locked. Cannot withdraw");
        }
        if (bid.getStatus() == BidStatus.REQUESTED) {
            UserAccount player = users.findLockedById(actor.getId()).orElseThrow();
            player.setCoins(player.getCoins() + bid.getAmount());
            users.save(player);
            bid.setStatus(BidStatus.WITHDRAWN);
            bids.save(bid);
            ids.ledger(player.getId(), LedgerType.WITHDRAW, bid.getAmount(), "Cancelled bid request · " + round.getOptionA() + " vs " + round.getOptionB());
            return;
        }
        if (!canWithdraw(round)) {
            throw ApiException.bad("Withdraws close 5 minutes before bidding ends");
        }
        UserAccount player = users.findLockedById(actor.getId()).orElseThrow();
        player.setCoins(player.getCoins() + bid.getAmount());
        users.save(player);
        if (bid.getSide() == Side.A) {
            round.setPoolA(Math.max(0, round.getPoolA() - bid.getAmount()));
        } else {
            round.setPoolB(Math.max(0, round.getPoolB() - bid.getAmount()));
        }
        rounds.save(round);
        bid.setStatus(BidStatus.WITHDRAWN);
        bids.save(bid);
        ids.ledger(player.getId(), LedgerType.WITHDRAW, bid.getAmount(), "Withdraw bid · " + round.getOptionA() + " vs " + round.getOptionB());
    }

    @Transactional
    public void settle(String roundId, Side winner) {
        Round round = rounds.findLockedById(roundId).orElseThrow(() -> ApiException.notFound("Round not found"));
        if (round.getStatus() == RoundStatus.SETTLED) {
            throw ApiException.bad("Round already settled");
        }
        Settings settings = settingsService.current();
        double mA = payoutMultiplier(round.getPoolA(), round.getPoolB(), settings.getHouseEdge());
        double mB = payoutMultiplier(round.getPoolB(), round.getPoolA(), settings.getHouseEdge());
        for (Bid bid : bids.findByRoundId(roundId)) {
            if (bid.getStatus() == BidStatus.REQUESTED) {
                refundRequest(bid, round, "Bid request refunded · round settled · " + round.getOptionA() + " vs " + round.getOptionB());
                bid.setStatus(BidStatus.REJECTED);
                bids.save(bid);
                continue;
            }
            if (bid.getStatus() != BidStatus.PENDING) {
                continue;
            }
            boolean won = bid.getSide() == winner;
            int payout = won ? (int) Math.round(bid.getAmount() * (bid.getSide() == Side.A ? mA : mB)) : 0;
            bid.setStatus(won ? BidStatus.WON : BidStatus.LOST);
            bid.setPayout(payout);
            bids.save(bid);
            UserAccount user = users.findLockedById(bid.getUserId()).orElse(null);
            if (user == null) {
                continue;
            }
            if (won) {
                user.setCoins(user.getCoins() + payout);
                user.setWins(user.getWins() + 1);
                users.save(user);
                ids.ledger(user.getId(), LedgerType.PAYOUT, payout, "Win payout · " + round.getOptionA() + " vs " + round.getOptionB());
            } else {
                user.setLosses(user.getLosses() + 1);
                users.save(user);
            }
        }
        round.setStatus(RoundStatus.SETTLED);
        round.setWinner(winner);
        rounds.save(round);
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
