package com.pos.loyalty;

import com.pos.common.events.EventEnvelope;
import com.pos.common.events.TransactionCompleted;
import com.pos.common.events.TransactionEventHandler;
import com.pos.common.events.TransactionRefunded;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Points ledger. Members earn 1 point per whole dollar spent before tax (2 for GOLD). Every ledger row carries
 * the source event id under a unique constraint, which makes redelivered events no-ops. Refunds claw back
 * exactly what that transaction earned.
 */
@Service
public class LoyaltyService implements TransactionEventHandler {

    static final long SILVER_AT = 500;
    static final long GOLD_AT = 2000;

    private final JdbcTemplate jdbc;

    public LoyaltyService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void onCompleted(EventEnvelope envelope, TransactionCompleted event) {
        if (event.customerId() == null || event.customerId().isBlank()) {
            return;
        }
        ensureAccount(event.customerId());
        String tier = tier(event.customerId());
        long multiplier = "GOLD".equals(tier) ? 2 : 1;
        long points = ((event.totalCents() - event.taxCents()) / 100) * multiplier;
        if (points > 0) {
            post(envelope.eventId(), event.customerId(), event.transactionId(), points, "EARN");
        }
    }

    @Override
    @Transactional
    public void onRefunded(EventEnvelope envelope, TransactionRefunded event) {
        if (event.customerId() == null || event.customerId().isBlank()) {
            return;
        }
        Long earned = jdbc.queryForObject(
                "SELECT COALESCE(SUM(points), 0) FROM loyalty_ledger WHERE customer_id = ? AND transaction_id = ?",
                Long.class, event.customerId(), event.transactionId());
        if (earned != null && earned > 0) {
            post(envelope.eventId(), event.customerId(), event.transactionId(), -earned, "REVERSAL");
        }
    }

    private void post(UUID eventId, String customerId, String transactionId, long points, String type) {
        int inserted = jdbc.update("""
                INSERT INTO loyalty_ledger (id, event_id, customer_id, transaction_id, points, entry_type, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING""",
                UUID.randomUUID(), eventId, customerId, transactionId, points, type, Timestamp.from(Instant.now()));
        if (inserted == 0) {
            return; // redelivered event
        }
        jdbc.update("UPDATE loyalty_account SET points = points + ?, lifetime_points = lifetime_points + GREATEST(?, 0) WHERE customer_id = ?",
                points, points, customerId);
        jdbc.update("""
                UPDATE loyalty_account SET tier = CASE
                    WHEN lifetime_points >= ? THEN 'GOLD' WHEN lifetime_points >= ? THEN 'SILVER' ELSE 'BRONZE' END
                WHERE customer_id = ?""", GOLD_AT, SILVER_AT, customerId);
    }

    private void ensureAccount(String customerId) {
        jdbc.update("""
                INSERT INTO loyalty_account (customer_id, points, lifetime_points, tier, created_at)
                VALUES (?, 0, 0, 'BRONZE', ?) ON CONFLICT DO NOTHING""",
                customerId, Timestamp.from(Instant.now()));
    }

    private String tier(String customerId) {
        return jdbc.queryForObject("SELECT tier FROM loyalty_account WHERE customer_id = ?", String.class, customerId);
    }

    @Transactional(readOnly = true)
    public Optional<Account> account(String customerId) {
        List<Account> rows = jdbc.query("SELECT customer_id, points, lifetime_points, tier FROM loyalty_account WHERE customer_id = ?",
                (rs, i) -> new Account(rs.getString(1), rs.getLong(2), rs.getLong(3), rs.getString(4), List.of()),
                customerId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        List<LedgerEntry> recent = jdbc.query("""
                SELECT transaction_id, points, entry_type, created_at FROM loyalty_ledger
                WHERE customer_id = ? ORDER BY created_at DESC LIMIT 10""",
                (rs, i) -> new LedgerEntry(rs.getString(1), rs.getLong(2), rs.getString(3), rs.getTimestamp(4).toInstant()),
                customerId);
        Account a = rows.getFirst();
        return Optional.of(new Account(a.customerId(), a.points(), a.lifetimePoints(), a.tier(), recent));
    }

    public record Account(String customerId, long points, long lifetimePoints, String tier, List<LedgerEntry> recent) {
    }

    public record LedgerEntry(String transactionId, long points, String type, Instant at) {
    }
}
