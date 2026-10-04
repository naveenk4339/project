package com.pos.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Terminal authentication. When {@code gateway.api-key} is set, every /api call must carry it in
 * {@code X-POS-Key}. Left empty for local demos; a production gateway would use mTLS or OAuth2 per terminal.
 */
@Component
public class ApiKeyFilter implements WebFilter {

    private final byte[] apiKey;

    public ApiKeyFilter(@Value("${gateway.api-key:}") String apiKey) {
        this.apiKey = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (apiKey.length == 0 || !exchange.getRequest().getPath().value().startsWith("/api/")) {
            return chain.filter(exchange);
        }
        String presented = exchange.getRequest().getHeaders().getFirst("X-POS-Key");
        if (presented != null && MessageDigest.isEqual(apiKey, presented.getBytes(StandardCharsets.UTF_8))) {
            return chain.filter(exchange);
        }
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
