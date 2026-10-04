package com.pos.ai;

import com.pos.ai.forecast.DemandForecaster;
import com.pos.ai.fraud.FraudFeatures;
import com.pos.ai.fraud.FraudModel;
import com.pos.ai.recommendation.CoPurchaseRecommender;
import com.pos.common.events.TransactionCompleted;
import com.pos.common.events.TransactionLine;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModelsTest {

    private final FraudModel fraud = new FraudModel();

    @Test
    void ordinaryPurchaseIsApproved() {
        var score = fraud.score(new FraudFeatures(1_200, 0, 1, false, 1.1, 14));
        assertThat(score.decision()).isEqualTo("APPROVE");
        assertThat(score.score()).isLessThan(0.1);
    }

    @Test
    void cardTestingPatternIsDeclinedWithReasons() {
        // rapid repeated attempts across several stores in the middle of the night
        var score = fraud.score(new FraudFeatures(45_000, 6, 3, true, 0, 3));
        assertThat(score.decision()).isEqualTo("DECLINE");
        assertThat(score.reasons()).startsWith("velocity").contains("multi_store", "off_hours");
    }

    @Test
    void recommendsFrequentlyCoPurchasedItems() {
        CoPurchaseRecommender rec = new CoPurchaseRecommender();
        for (int i = 0; i < 5; i++) {
            rec.learn(sale("COF-001", "BAK-002"));
        }
        rec.learn(sale("COF-001", "SNK-001"));
        rec.learn(sale("MER-001"));

        assertThat(rec.forBasket(List.of("COF-001"), 2))
                .extracting(CoPurchaseRecommender.Recommendation::sku)
                .containsExactly("BAK-002", "SNK-001");
        assertThat(rec.forBasket(List.of("ELE-002"), 1)).hasSize(1); // popularity fallback
    }

    @Test
    void forecastFollowsTrend() {
        DemandForecaster forecaster = new DemandForecaster();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (int i = 0; i < 10; i++) {
            forecaster.recordUnits("COF-001", today.minusDays(9 - i), 10 + 2L * i); // 10,12,...,28
        }
        var f = forecaster.forecast("COF-001", 3);
        assertThat(f.forecast()).hasSize(3);
        assertThat(f.forecast().get(0).units()).isBetween(28L, 34L);
        assertThat(f.forecast().get(2).units()).isGreaterThan(f.forecast().get(0).units());
        assertThat(forecaster.forecast("UNKNOWN", 3).totalForecastUnits()).isZero();
    }

    private static TransactionCompleted sale(String... skus) {
        List<TransactionLine> lines = Arrays.stream(skus)
                .map(s -> new TransactionLine(s, s, "c", 1, 100, 0, 100)).toList();
        return new TransactionCompleted("TX", "store-001", "t1", null, lines, 0, 0, 0, 0, List.of(), null, Instant.now());
    }
}
