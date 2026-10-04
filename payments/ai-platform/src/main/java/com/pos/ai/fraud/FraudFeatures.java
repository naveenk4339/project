package com.pos.ai.fraud;

/**
 * @param attemptsLast10Min authorization attempts on this card in the last 10 minutes (excluding this one)
 * @param distinctStoresLastHour stores this card was presented at in the last hour, including this one
 * @param amountToAverageRatio this amount divided by the card's average completed ticket (0 if no history)
 */
public record FraudFeatures(long amountCents, int attemptsLast10Min, int distinctStoresLastHour, boolean newCard,
                            double amountToAverageRatio, int hourOfDayUtc) {
}
