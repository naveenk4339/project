package com.pos.ai.fraud;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Logistic-regression fraud scorer. The coefficients are hand-set for the demo; in production they would be
 * learned offline (or the class swapped for an ONNX/XGBoost model) behind the same {@link #score} contract.
 */
@Component
public class FraudModel {

    static final double INTERCEPT = -4.0;
    static final double W_LOG_AMOUNT = 0.35;
    static final double W_VELOCITY = 0.9;
    static final double W_MULTI_STORE = 1.2;
    static final double W_NEW_CARD = 0.6;
    static final double W_OFF_HOURS = 0.8;
    static final double W_AMOUNT_SPIKE = 0.5;

    public static final double REVIEW_AT = 0.6;
    public static final double DECLINE_AT = 0.85;

    public FraudScore score(FraudFeatures f) {
        Map<String, Double> contributions = new LinkedHashMap<>();
        contributions.put("amount", W_LOG_AMOUNT * Math.log1p(f.amountCents() / 100.0));
        contributions.put("velocity", W_VELOCITY * Math.max(0, f.attemptsLast10Min() - 2));
        contributions.put("multi_store", W_MULTI_STORE * Math.max(0, f.distinctStoresLastHour() - 1));
        contributions.put("new_card", f.newCard() ? W_NEW_CARD : 0);
        contributions.put("off_hours", f.hourOfDayUtc() < 5 ? W_OFF_HOURS : 0);
        contributions.put("amount_spike", f.amountToAverageRatio() > 3 ? W_AMOUNT_SPIKE * Math.min(f.amountToAverageRatio(), 10) / 3 : 0);

        double logit = INTERCEPT + contributions.values().stream().mapToDouble(Double::doubleValue).sum();
        double score = 1 / (1 + Math.exp(-logit));

        List<String> reasons = new ArrayList<>();
        contributions.entrySet().stream()
                .filter(e -> e.getValue() >= 0.5 && !e.getKey().equals("amount"))
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                .forEach(e -> reasons.add(e.getKey()));
        String decision = score >= DECLINE_AT ? "DECLINE" : score >= REVIEW_AT ? "REVIEW" : "APPROVE";
        return new FraudScore(Math.round(score * 1000) / 1000.0, decision, reasons, f);
    }

    public record FraudScore(double score, String decision, List<String> reasons, FraudFeatures features) {
    }
}
