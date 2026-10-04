package com.pos.payment.fraud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

/** Synchronous call to the AI platform's fraud model, with a tight timeout so it never stalls the till. */
@Component
public class FraudClient {

    private static final Logger log = LoggerFactory.getLogger(FraudClient.class);

    private final RestClient restClient;

    public FraudClient(RestClient.Builder builder, FraudProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.timeout());
        requestFactory.setReadTimeout(properties.timeout());
        this.restClient = builder.baseUrl(properties.url()).requestFactory(requestFactory).build();
    }

    public Optional<FraudScore> score(FraudRequest request) {
        try {
            return Optional.ofNullable(restClient.post().uri("/api/fraud/score").body(request)
                    .retrieve().body(FraudScore.class));
        } catch (RestClientException e) {
            log.warn("Fraud scoring unavailable for {}: {}", request.transactionId(), e.getMessage());
            return Optional.empty();
        }
    }

    public record FraudRequest(String transactionId, String storeId, String cardFingerprint, String method,
                               long amountCents) {
    }

    public record FraudScore(double score, String decision, java.util.List<String> reasons) {
    }
}
