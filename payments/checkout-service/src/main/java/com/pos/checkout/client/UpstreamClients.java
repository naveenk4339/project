package com.pos.checkout.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Configuration
public class UpstreamClients {

    @Bean
    CartClient cartClient(RestClient.Builder builder, @Value("${services.cart-url}") String url) {
        return new CartClient(client(builder, url, "cart", Duration.ofSeconds(2)));
    }

    @Bean
    PricingClient pricingClient(RestClient.Builder builder, @Value("${services.pricing-url}") String url) {
        return new PricingClient(client(builder, url, "pricing", Duration.ofSeconds(2)));
    }

    @Bean
    PaymentClient paymentClient(RestClient.Builder builder, @Value("${services.payment-url}") String url) {
        // card authorizations can take a few seconds at the acquirer
        return new PaymentClient(client(builder, url, "payment", Duration.ofSeconds(10)));
    }

    private static RestClient client(RestClient.Builder builder, String url, String name, Duration timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(timeout);
        return builder.clone()
                .baseUrl(url)
                .requestFactory(factory)
                .defaultStatusHandler(status -> status.is4xxClientError(), (request, response) -> {
                    String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    throw new UpstreamException(name, response.getStatusCode(), body);
                })
                .build();
    }
}
