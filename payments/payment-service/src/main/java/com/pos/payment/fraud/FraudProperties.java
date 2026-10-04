package com.pos.payment.fraud;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @param declineThreshold score at or above which a card payment is declined before reaching the processor
 * @param failOpenMaxCents if the fraud service is unavailable, allow card payments up to this amount
 */
@ConfigurationProperties(prefix = "fraud")
public record FraudProperties(String url, Duration timeout, double declineThreshold, long failOpenMaxCents) {
}
