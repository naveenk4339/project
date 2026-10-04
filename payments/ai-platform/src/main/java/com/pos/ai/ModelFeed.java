package com.pos.ai;

import com.pos.ai.forecast.DemandForecaster;
import com.pos.ai.fraud.FraudFeatureStore;
import com.pos.ai.recommendation.CoPurchaseRecommender;
import com.pos.common.events.EventEnvelope;
import com.pos.common.events.Topics;
import com.pos.common.events.TransactionCompleted;
import com.pos.common.events.TransactionEventDispatcher;
import com.pos.common.events.TransactionEventHandler;
import com.pos.common.events.TransactionRefunded;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Fans each transaction event out to the three models' online feature / training stores. */
@Component
public class ModelFeed implements TransactionEventHandler {

    private final FraudFeatureStore fraudFeatures;
    private final CoPurchaseRecommender recommender;
    private final DemandForecaster forecaster;
    private final Set<UUID> seen = Collections.newSetFromMap(Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<UUID, Boolean> eldest) {
                    return size() > 100_000;
                }
            }));

    public ModelFeed(FraudFeatureStore fraudFeatures, CoPurchaseRecommender recommender, DemandForecaster forecaster) {
        this.fraudFeatures = fraudFeatures;
        this.recommender = recommender;
        this.forecaster = forecaster;
    }

    // per-instance group id: models are in memory and rebuilt by replaying the topic on start-up
    @KafkaListener(topics = Topics.TRANSACTION_EVENTS, groupId = "ai-platform-${random.uuid}")
    public void on(String message) {
        TransactionEventDispatcher.dispatch(message, this);
    }

    @Override
    public void onCompleted(EventEnvelope envelope, TransactionCompleted sale) {
        if (!seen.add(envelope.eventId())) {
            return;
        }
        fraudFeatures.recordSale(sale);
        recommender.learn(sale);
        forecaster.recordSale(sale);
    }

    @Override
    public void onRefunded(EventEnvelope envelope, TransactionRefunded refund) {
        if (seen.add(envelope.eventId())) {
            forecaster.recordRefund(refund);
        }
    }
}
