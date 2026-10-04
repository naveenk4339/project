package com.pos.relay;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @param lockClause row-locking suffix for the batch query. {@code FOR UPDATE SKIP LOCKED} lets several relay
 *                   replicas drain the outbox concurrently without publishing the same row twice.
 */
@ConfigurationProperties(prefix = "outbox")
public record OutboxProperties(int batchSize, String lockClause, Duration sendTimeout, Duration retention) {
}
