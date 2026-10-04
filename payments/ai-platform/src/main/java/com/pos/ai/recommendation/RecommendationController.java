package com.pos.ai.recommendation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final CoPurchaseRecommender recommender;

    public RecommendationController(CoPurchaseRecommender recommender) {
        this.recommender = recommender;
    }

    /** {@code GET /api/recommendations?sku=COF-001&sku=BAK-002&limit=3} */
    @GetMapping
    public List<CoPurchaseRecommender.Recommendation> recommend(@RequestParam List<String> sku,
                                                                @RequestParam(defaultValue = "3") int limit) {
        return recommender.forBasket(sku, Math.min(limit, 20));
    }
}
