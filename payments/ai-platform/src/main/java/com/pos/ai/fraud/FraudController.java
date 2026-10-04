package com.pos.ai.fraud;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fraud")
public class FraudController {

    private final FraudFeatureStore features;
    private final FraudModel model;

    public FraudController(FraudFeatureStore features, FraudModel model) {
        this.features = features;
        this.model = model;
    }

    @PostMapping("/score")
    public FraudModel.FraudScore score(@Valid @RequestBody ScoreRequest request) {
        FraudModel.FraudScore score = model.score(
                features.features(request.cardFingerprint(), request.storeId(), request.amountCents()));
        features.recordAttempt(request.cardFingerprint(), request.storeId());
        return score;
    }

    public record ScoreRequest(String transactionId, @NotBlank String storeId, String cardFingerprint,
                               String method, @Min(0) long amountCents) {
    }
}
