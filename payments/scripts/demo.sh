#!/usr/bin/env bash
# Walks one sale through the whole platform via the API gateway and shows every downstream effect.
# Requires curl and jq. GW defaults to the local gateway.
set -euo pipefail
GW="${GW:-http://localhost:8080}"
STORE="${STORE:-store-001}"
CUSTOMER="${CUSTOMER:-cust-$RANDOM}"
j() { curl -fsS -H 'Content-Type: application/json' "$@"; }
step() { printf '\n\033[1m== %s\033[0m\n' "$*"; }

step "Stock before"
j "$GW/api/inventory/BAK-001" | jq -c .

step "Open a cart for loyalty member $CUSTOMER and scan items"
CART=$(j -X POST "$GW/api/carts" -d "{\"storeId\":\"$STORE\",\"terminalId\":\"till-01\",\"customerId\":\"$CUSTOMER\"}" | jq -r .id)
j -X POST "$GW/api/carts/$CART/items" -d '{"sku":"COF-001","quantity":2}' > /dev/null
j -X POST "$GW/api/carts/$CART/items" -d '{"sku":"BAK-001","quantity":2}' > /dev/null
j -X POST "$GW/api/carts/$CART/items" -d '{"sku":"SNK-001","quantity":3}' | jq -c '{cart: .id, items}'

step "Price quote (promotions + tax)"
j -X POST "$GW/api/pricing/quote" -d "{\"storeId\":\"$STORE\",\"items\":$(j "$GW/api/carts/$CART" | jq -c .items)}" \
  | jq -c '{subtotalCents, discountCents, taxCents, totalCents, appliedPromotions}'

step "Checkout with a card (Idempotency-Key makes retries safe)"
KEY=$(uuidgen 2>/dev/null || cat /proc/sys/kernel/random/uuid)
TX=$(j -X POST "$GW/api/checkout" -H "Idempotency-Key: $KEY" \
  -d "{\"cartId\":\"$CART\",\"payment\":{\"method\":\"CARD\",\"cardToken\":\"tok_visa\"}}")
echo "$TX" | jq -c '{id, status, totalCents, payment: {cardBrand: .payment.cardBrand, last4: .payment.cardLast4, authCode: .payment.authCode, fraudScore: .payment.fraudScore}}'
TXID=$(echo "$TX" | jq -r .id)

step "Retry the same request (network blip) -> same transaction, no second charge"
j -X POST "$GW/api/checkout" -H "Idempotency-Key: $KEY" \
  -d "{\"cartId\":\"$CART\",\"payment\":{\"method\":\"CARD\",\"cardToken\":\"tok_visa\"}}" | jq -c '{id, status}'

step "Waiting for outbox -> Kafka -> consumers"
for _ in $(seq 1 30); do curl -fs "$GW/api/receipts/$TXID/text" > /dev/null && break; sleep 0.5; done
curl -fsS "$GW/api/receipts/$TXID/text"

step "Inventory after (BAK-001 down by 2)"
j "$GW/api/inventory/BAK-001" | jq -c .
step "Loyalty"
j "$GW/api/loyalty/$CUSTOMER" | jq -c '{customerId, points, tier}'
step "Analytics"
j "$GW/api/analytics/stores/$STORE" | jq -c '{transactions, grossSalesCents, averageTicketCents, salesByTender}'
step "Recommendations for a basket with coffee"
j "$GW/api/recommendations?sku=COF-001&limit=3" | jq -c .

step "Declined card -> 402, cart unlocked for another tender"
CART2=$(j -X POST "$GW/api/carts" -d "{\"storeId\":\"$STORE\",\"terminalId\":\"till-01\"}" | jq -r .id)
j -X POST "$GW/api/carts/$CART2/items" -d '{"sku":"ELE-002","quantity":1}' > /dev/null
DECLINE=$(curl -sS -o /tmp/pos-decline.json -w '%{http_code}' -H 'Content-Type: application/json' \
  -H "Idempotency-Key: $(cat /proc/sys/kernel/random/uuid)" -X POST "$GW/api/checkout" \
  -d "{\"cartId\":\"$CART2\",\"payment\":{\"method\":\"CARD\",\"cardToken\":\"tok_decline\"}}")
echo "HTTP $DECLINE $(jq -c '{id, status, declineReason}' /tmp/pos-decline.json)"
j "$GW/api/carts/$CART2" | jq -c '{cart: .id, status}'

step "Refund the first sale -> inventory restocked, loyalty reversed"
j -X POST "$GW/api/transactions/$TXID/refund" -H "Idempotency-Key: refund-$TXID" -d '{"reason":"demo"}' | jq -c '{id, status, refundedCents}'
sleep 3
j "$GW/api/inventory/BAK-001" | jq -c .
j "$GW/api/loyalty/$CUSTOMER" | jq -c '{points, recent: [.recent[] | {points, type}]}'

step "Ask the GenAI assistant"
j -X POST "$GW/api/assistant/chat" -d "{\"storeId\":\"$STORE\",\"message\":\"Transaction $TXID - what happened to it, and what is our refund policy for electronics?\"}" \
  | jq -r '"[\(.mode)] tools=\(.toolsUsed) sources=\(.sources)\n\(.answer)"'
