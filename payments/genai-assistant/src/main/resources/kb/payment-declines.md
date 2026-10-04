# Payment Declines & Decline Codes

When a payment is declined the till shows the decline reason and the cart is unlocked so the customer can try
another tender. Nothing is charged for a declined payment.

| Code | Meaning | What to do |
|------|---------|-----------|
| INSUFFICIENT_FUNDS | The issuer declined for funds or limit. | Ask for another card or tender. Do not retry the same card more than once. |
| EXPIRED_CARD | The card is past its expiry date. | Ask for another card. |
| SUSPECTED_FRAUD | Our fraud model scored the payment as high risk (score 0.85 or above) before it reached the card network. | Do not retry the card. Politely ask for another tender. If the customer insists, call a supervisor; do not discuss the fraud score with the customer. |
| FRAUD_CHECK_UNAVAILABLE | The fraud service was unreachable and the amount was above the $100.00 fail-open limit. | Retry in a minute, split the purchase, or take another tender. Small card payments still go through while the fraud service is down. |
| INSUFFICIENT_CASH_TENDERED | The cash entered was less than the total. | Re-enter the tendered amount. |

## Fraud signals the model looks at
The fraud model considers the amount, how many times the same card was tried in the last 10 minutes, whether the
card was used at several stores in the last hour, whether the card is new to us, the time of day and whether the
amount is far above the card's usual spend. Repeated quick attempts on one card are the strongest signal of card
testing.

## "Payment pending" / timeouts
If the till shows *Payment status unknown, retry*, press **Retry**. The retry re-uses the same checkout request, so
the customer can never be charged twice; the system returns the original outcome if the payment had in fact gone
through.
