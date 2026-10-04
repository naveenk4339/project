package com.pos.gateway;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

/**
 * Public API surface. Service-to-service endpoints are deliberately not routed: the payment service
 * (/api/payments), fraud scoring (/api/fraud) and the cart lifecycle calls checkout makes
 * (/api/carts/{id}/lock|unlock|complete) are reachable only inside the network.
 */
@Configuration
public class Routes {

    @Bean
    RouteLocator posRoutes(RouteLocatorBuilder routes, ServiceUrls urls) {
        return routes.routes()
                .route("internal-cart-lifecycle", r -> r.order(-1)
                        .path("/api/carts/*/lock", "/api/carts/*/unlock", "/api/carts/*/complete")
                        .filters(f -> f.setStatus(HttpStatus.NOT_FOUND))
                        .uri("no://op"))
                .route("cart", r -> r.path("/api/carts", "/api/carts/**").uri(urls.cart()))
                .route("pricing", r -> r.path("/api/products", "/api/products/**", "/api/promotions", "/api/pricing/**")
                        .uri(urls.pricing()))
                .route("checkout", r -> r.path("/api/checkout", "/api/transactions", "/api/transactions/**")
                        .uri(urls.checkout()))
                .route("inventory", r -> r.path("/api/inventory", "/api/inventory/**").uri(urls.inventory()))
                .route("receipt", r -> r.path("/api/receipts/**").uri(urls.receipt()))
                .route("loyalty", r -> r.path("/api/loyalty/**").uri(urls.loyalty()))
                .route("analytics", r -> r.path("/api/analytics/**").uri(urls.analytics()))
                .route("ai", r -> r.path("/api/recommendations", "/api/recommendations/**", "/api/forecast/**")
                        .uri(urls.aiPlatform()))
                .route("assistant", r -> r.path("/api/assistant/**").uri(urls.assistant()))
                .build();
    }
}
