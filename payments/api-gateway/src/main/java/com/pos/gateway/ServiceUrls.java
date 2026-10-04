package com.pos.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "services")
public record ServiceUrls(String cart, String pricing, String checkout, String inventory, String receipt,
                          String loyalty, String analytics, String aiPlatform, String assistant) {
}
