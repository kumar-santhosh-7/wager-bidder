package com.ledgerbid.api.service;

import com.ledgerbid.api.dto.CreateRoundRequest;
import com.ledgerbid.api.dto.EndsAtRequest;
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
import java.util.List;

@Service
public class HouseService {
    private final RoundRepository rounds;
    private final BidRepository bids;
    private final UserAccountRepository users;
    private final SettingsService settingsService;
    private final IdService ids;
    private final int withdrawLockMinutes;

    public static final int MATCH_LOCK_HOURS = 1;

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

    public static double cutPercent(int lower, int higher) {
        if (higher <= 0) {
            return 0;
        }
        return (lower * 100.0) / higher;
    }

    public static int higherMemberCut(int amount, double cutPct) {
        return (int) Math.round((amount / 100.0) * cutPct);
    }

    public static int higherMemberSurplus(int amount, double cutPct) {
        return amount - higherMemberCut(amount, cutPct);
    }

    public static int settlePayout(int amount, Side side, Side winner, int poolA, int poolB) {
        int higher = Math.max(poolA, poolB);
        int lower = Math.min(poolA, poolB);
        double pct = cutPercent(lower, higher);
        boolean higherIsA = poolA >= poolB;
        boolean isHigher = higherIsA ? side == Side.A : side == Side.B;
        int cut = isHigher ? higherMemberCut(amount, pct) : 0;
        int surplus = isHigher ? amount - cut : 0;
        int potWin = side == winner ? (isHigher ? cut * 2 : amount * 2) : 0;
        return surplus + potWin;
    }

    public static double payoutMultiplier(int poolSide, int poolOther, double houseEdge) {
        if (poolSide <= 0) {
            return 0;
        }
        if (poolSide >= poolOther) {
            return 1 + ((double) poolOther / poolSide);
        }
        return 2;
    }

    private Instant parseInstant(String raw, String error) {
        try {
            return Instant.parse(raw);
        } catch (Exception e) {
            throw ApiException.bad(error);
        }
    }

    private Instant parseFuture(String endsAt) {
        Instant t = parseInstant(endsAt, "Invalid end time");
        if (!t.isAfter(Instant.now())) {
            throw ApiException.bad("Set a bidding end time in the future");
        }
        return t;
    }

    @Transactional
    public void promoteDueRounds() {
        freezeDueRounds();
        if (rounds.existsByStatus(RoundStatus.LIVE)) {
            return;
        }
        List<Round> due = rounds.findByStatusAndStartsAtLessThanEqualOrderByStartsAtAsc(RoundStatus.UPCOMING, Instant.now());
        if (due.isEmpty()) {
            return;
        }
        Round next = due.get(0);
        next.setStatus(RoundStatus.LIVE);
        rounds.save(next);
    }

    private Instant matchLockAt(Round round) {
        Instant close = biddingCloseOf(round);
        Instant start = round.getStartsAt() != null ? round.getStartsAt() : round.getCreatedAt();
        Instant lockAt = close.minus(MATCH_LOCK_HOURS, ChronoUnit.HOURS);
        return lockAt.isAfter(start) ? lockAt : close;
    }

    @Transactional
    public void freezeDueRounds() {
        Instant now = Instant.now();
        for (Round round : rounds.findByStatus(RoundStatus.LIVE)) {
            if (round.getMatchedAt() == null && (!now.isBefore(matchLockAt(round)) || !now.isBefore(biddingCloseOf(round)))) {
                freezeRound(round);
            }
        }
    }

    private void freezeRound(Round round) {
        if (round.getMatchedAt() != null) {
            return;
        }
        int poolA = round.getPoolA();
        int poolB = round.getPoolB();
        double pct = cutPercent(Math.min(poolA, poolB), Math.max(poolA, poolB));
        boolean higherIsA = poolA >= poolB;
        int newA = 0;
        int newB = 0;
        for (Bid bid : bids.findByRoundId(round.getId())) {
            if (bid.getStatus() == BidStatus.REQUESTED) {
                refundRequest(bid, round, "Bid request refunded · match locked · " + round.getOptionA() + " vs " + round.getOptionB());
                bid.setStatus(BidStatus.REJECTED);
                bids.save(bid);
                continue;
            }
            if (bid.getStatus() != BidStatus.PENDING) {
                continue;
            }
            boolean isHigher = higherIsA ? bid.getSide() == Side.A : bid.getSide() == Side.B;
            int cut = isHigher ? higherMemberCut(bid.getAmount(), pct) : bid.getAmount();
            int surplus = isHigher ? bid.getAmount() - cut : 0;
            bid.setMatchedAmount(cut);
            bids.save(bid);
            if (surplus > 0) {
                UserAccount user = users.findLockedById(bid.getUserId()).orElse(null);
                if (user != null) {
                    user.setCoins(user.getCoins() + surplus);
                    users.save(user);
                    ids.ledger(
                        user.getId(),
                        LedgerType.CREDIT,
                        surplus,
                        "Unmatched coins returned 1 hour before bidding close · " + round.getOptionA() + " vs " + round.getOptionB()
                    );
                }
            }
            if (bid.getSide() == Side.A) {
                newA += cut;
            } else {
                newB += cut;
            }
        }
        round.setPoolA(newA);
        round.setPoolB(newB);
        round.setMatchedAt(Instant.now());
        rounds.save(round);
    }

    private Instant biddingCloseOf(Round round) {
        return round.getBiddingClosesAt() != null ? round.getBiddingClosesAt() : round.getEndsAt();
    }

    private boolean biddingOpen(Round round) {
        Instant now = Instant.now();
        if (round.getStatus() != RoundStatus.LIVE || round.getMatchedAt() != null) {
            return false;
        }
        if (round.getStartsAt() != null && now.isBefore(round.getStartsAt())) {
            return false;
        }
        if (!now.isBefore(biddingCloseOf(round))) {
            return false;
        }
        return now.isBefore(matchLockAt(round));
    }

    private boolean canWithdraw(Round round) {
        if (!biddingOpen(round)) {
            return false;
        }
        return Instant.now().plus(withdrawLockMinutes, ChronoUnit.MINUTES).isBefore(matchLockAt(round));
    }

    @Transactional
    public void createRound(CreateRoundRequest req) {
        Instant now = Instant.now();
        Instant startsAt = now;
        if (req.startsAt() != null && !req.startsAt().isBlank()) {
            startsAt = parseInstant(req.startsAt(), "Invalid start time");
        }
        Instant endsAt = parseInstant(req.endsAt(), "Invalid end time");
        Instant biddingClosesAt = endsAt;
        if (req.biddingClosesAt() != null && !req.biddingClosesAt().isBlank()) {
            biddingClosesAt = parseInstant(req.biddingClosesAt(), "Invalid bidding close time");
        }
        if (!endsAt.isAfter(startsAt)) {
            throw ApiException.bad("End time must be after start time");
        }
        if (!biddingClosesAt.isAfter(startsAt)) {
            throw ApiException.bad("Bidding close must be after start time");
        }
        if (biddingClosesAt.isAfter(endsAt)) {
            throw ApiException.bad("Bidding close cannot be after match end");
        }
        if (!endsAt.isAfter(now)) {
            throw ApiException.bad("Set a match end time in the future");
        }
        boolean upcoming = startsAt.isAfter(now.plusSeconds(5));
        if (!upcoming && !biddingClosesAt.isAfter(now)) {
            throw ApiException.bad("Set a bidding close time in the future");
        }
        if (!upcoming && rounds.existsByStatus(RoundStatus.LIVE)) {
            throw ApiException.bad("A live round already exists. Schedule this as an upcoming match.");
        }
        Round r = new Round();
        r.setId(ids.next("r"));
        r.setOptionA(req.optionA().trim());
        r.setOptionB(req.optionB().trim());
        r.setPoolA(0);
        r.setPoolB(0);
        r.setStatus(upcoming ? RoundStatus.UPCOMING : RoundStatus.LIVE);
        r.setCreatedAt(now);
        r.setStartsAt(startsAt);
        r.setEndsAt(endsAt);
        r.setBiddingClosesAt(biddingClosesAt);
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
    public void setEndsAt(String roundId, EndsAtRequest req) {
        Round round = rounds.findById(roundId).orElseThrow(() -> ApiException.notFound("Round not found"));
        if (round.getStatus() == RoundStatus.SETTLED) {
            throw ApiException.bad("Round already settled");
        }
        if (round.getMatchedAt() != null) {
            throw ApiException.bad("Match is locked. Leftover coins already returned.");
        }
        Instant endsAt = parseFuture(req.endsAt());
        Instant closeAt = endsAt;
        if (req.biddingClosesAt() != null && !req.biddingClosesAt().isBlank()) {
            closeAt = parseInstant(req.biddingClosesAt(), "Invalid bidding close time");
            if (!closeAt.isAfter(Instant.now())) {
                throw ApiException.bad("Set a bidding close time in the future");
            }
        }
        if (closeAt.isAfter(endsAt)) {
            throw ApiException.bad("Bidding close cannot be after match end");
        }
        round.setEndsAt(endsAt);
        round.setBiddingClosesAt(closeAt);
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
        if (req.side() == Side.DRAW) {
            throw ApiException.bad("Pick a side");
        }
        if (req.amount() < settings.getMinBet() || req.amount() > settings.getMaxBet()) {
            throw ApiException.bad("Bet must be " + settings.getMinBet() + "–" + settings.getMaxBet());
        }
        Round round = rounds.findLockedById(req.roundId()).orElseThrow(() -> ApiException.bad("Round is not live"));
        if (round.getMatchedAt() == null && (!Instant.now().isBefore(matchLockAt(round)) || !Instant.now().isBefore(biddingCloseOf(round)))) {
            freezeRound(round);
        }
        if (round.getStatus() != RoundStatus.LIVE || !biddingOpen(round)) {
            throw ApiException.bad("Bidding has closed for this match");
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
        if (round.getMatchedAt() == null && (!Instant.now().isBefore(matchLockAt(round)) || !Instant.now().isBefore(biddingCloseOf(round)))) {
            freezeRound(round);
        }
        if (round.getStatus() != RoundStatus.LIVE || round.getMatchedAt() != null || !biddingOpen(round)) {
            throw ApiException.bad("Bidding has closed for this match");
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
        if (round.getMatchedAt() != null || !canWithdraw(round)) {
            throw ApiException.bad("Withdraws close 1 hour before bidding ends, when leftover coins are returned");
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
        Instant now = Instant.now();
        if (winner != Side.DRAW) {
            if (round.getMatchedAt() == null && now.isBefore(matchLockAt(round))) {
                throw ApiException.bad("Leftover coins return 1 hour before bidding closes. Announce the winner after that.");
            }
            if (round.getMatchedAt() == null) {
                freezeRound(round);
            }
        }
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
            int stake = bid.getMatchedAmount() != null ? bid.getMatchedAmount() : bid.getAmount();
            if (winner == Side.DRAW) {
                UserAccount player = users.findLockedById(bid.getUserId()).orElseThrow(() -> ApiException.notFound("Player not found"));
                player.setCoins(player.getCoins() + stake);
                users.save(player);
                ids.ledger(player.getId(), LedgerType.CREDIT, stake, "Draw refund · " + round.getOptionA() + " vs " + round.getOptionB());
                bid.setStatus(BidStatus.DRAW);
                bid.setPayout(stake);
                bids.save(bid);
                continue;
            }
            boolean won = bid.getSide() == winner;
            int payout = won ? stake * 2 : 0;
            bid.setStatus(won ? BidStatus.WON : BidStatus.LOST);
            bid.setPayout(payout);
            bids.save(bid);
            UserAccount user = users.findLockedById(bid.getUserId()).orElse(null);
            if (user == null) {
                continue;
            }
            if (payout > 0) {
                user.setCoins(user.getCoins() + payout);
                ids.ledger(
                    user.getId(),
                    LedgerType.PAYOUT,
                    payout,
                    "Win payout · " + round.getOptionA() + " vs " + round.getOptionB()
                );
            }
            if (won) {
                user.setWins(user.getWins() + 1);
            } else {
                user.setLosses(user.getLosses() + 1);
            }
            users.save(user);
        }
        round.setStatus(RoundStatus.SETTLED);
        round.setWinner(winner);
        rounds.save(round);
    }

    @Transactional
    public void deleteRound(String roundId) {
        Round round = rounds.findLockedById(roundId).orElseThrow(() -> ApiException.notFound("Round not found"));
        String label = round.getOptionA() + " vs " + round.getOptionB();
        for (Bid bid : bids.findByRoundId(roundId)) {
            if (bid.getStatus() == BidStatus.REQUESTED || bid.getStatus() == BidStatus.PENDING) {
                int refund = bid.getMatchedAmount() != null ? bid.getMatchedAmount() : bid.getAmount();
                if (refund > 0) {
                    UserAccount player = users.findLockedById(bid.getUserId()).orElse(null);
                    if (player != null) {
                        player.setCoins(player.getCoins() + refund);
                        users.save(player);
                        ids.ledger(player.getId(), LedgerType.CREDIT, refund, "Match deleted · refund · " + label);
                    }
                }
            }
        }
        bids.deleteByRoundId(roundId);
        rounds.delete(round);
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
