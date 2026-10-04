package com.pos.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayTest {

    static HttpServer backend;

    @Autowired
    WebTestClient web;

    @DynamicPropertySource
    static void backend(DynamicPropertyRegistry registry) throws IOException {
        backend = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        backend.createContext("/", exchange -> {
            byte[] body = ("{\"path\":\"" + exchange.getRequestURI().getPath() + "\",\"requestId\":\""
                    + exchange.getRequestHeaders().getFirst("X-Request-Id") + "\"}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        backend.start();
        String url = "http://127.0.0.1:" + backend.getAddress().getPort();
        for (String s : new String[]{"cart", "pricing", "checkout", "inventory", "receipt", "loyalty", "analytics", "ai-platform", "assistant"}) {
            registry.add("services." + s, () -> url);
        }
    }

    @AfterAll
    static void stop() {
        backend.stop(0);
    }

    @Test
    void routesPublicApisAndPropagatesRequestId() {
        web.get().uri("/api/products?q=coffee").header("X-Request-Id", "req-123").exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Request-Id", "req-123")
                .expectBody().jsonPath("$.path").isEqualTo("/api/products").jsonPath("$.requestId").isEqualTo("req-123");
        web.get().uri("/api/transactions/TX-1").exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.path").isEqualTo("/api/transactions/TX-1");
    }

    @Test
    void doesNotExposeInternalEndpoints() {
        web.post().uri("/api/carts/abc/lock").exchange().expectStatus().isNotFound();
        web.post().uri("/api/payments/authorize").exchange().expectStatus().isNotFound();
        web.post().uri("/api/fraud/score").exchange().expectStatus().isNotFound();
    }

    @Test
    void servesPosClient() {
        web.get().uri("/index.html").exchange().expectStatus().isOk();
    }
}
