package com.pos.pricing.domain;

/**
 * Promotion rules supported by the engine.
 * <ul>
 *   <li>{@code PERCENT_OFF_CATEGORY}: {@code percent}% off every item in {@code category}</li>
 *   <li>{@code BUY_X_GET_Y}: for every {@code buyQuantity + getQuantity} units of {@code sku}, {@code getQuantity} are free</li>
 *   <li>{@code ORDER_THRESHOLD}: {@code amountOffCents} off once the discounted subtotal reaches {@code thresholdCents}</li>
 * </ul>
 */
public record Promotion(
        String id,
        String description,
        Type type,
        String category,
        String sku,
        int percent,
        int buyQuantity,
        int getQuantity,
        long thresholdCents,
        long amountOffCents) {

    public enum Type { PERCENT_OFF_CATEGORY, BUY_X_GET_Y, ORDER_THRESHOLD }
}
