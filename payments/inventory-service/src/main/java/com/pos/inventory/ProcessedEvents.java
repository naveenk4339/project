package com.pos.inventory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

/** Inbox table for idempotent consumption: claiming the event id and applying the change commit together. */
@Component
public class ProcessedEvents {

    private final JdbcTemplate jdbc;

    public ProcessedEvents(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean claim(UUID eventId) {
        // ON CONFLICT rather than catching a duplicate-key error: in PostgreSQL a failed statement aborts the transaction
        return jdbc.update("INSERT INTO processed_event (event_id, processed_at) VALUES (?, ?) ON CONFLICT DO NOTHING",
                eventId, Timestamp.from(Instant.now())) == 1;
    }
}
