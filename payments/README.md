# POS Payment Platform (sample)

A working sample of a retail point-of-sale payment system: a POS client talks to an API gateway in front of
cart, pricing, checkout and payment services. Completed sales are written to a Transaction DB together with an
**outbox** row, relayed to **Kafka**, and fanned out to inventory, receipts, loyalty, analytics and an
**AI platform** (fraud, recommendation and forecast models). A **GenAI assistant** answers store associates'
questions using RAG over a POS knowledge base plus tool calls into the POS APIs.

Java 21 · Spring Boot 3.5 · Spring Cloud Gateway · Spring Kafka · PostgreSQL 16 + pgvector · Apache Kafka 3.9 ·
Spring AI (vector store, ONNX embeddings) · Anthropic Java SDK (Claude)

```
POS Client (web, served by the gateway)
   │
API Gateway :8080 ── routes public APIs, hides internal ones, X-Request-Id, optional terminal API key
   ├── Cart Service :8081 ─────────┐
   ├── Pricing Service :8082       │
   └── Checkout Service :8083 ─────┤   checkout saga: lock cart → price → persist → pay → complete
            └── Payment Service :8084 ── fraud score (sync) → AI Platform
                                   │
                         Transaction DB (Postgres "pos")
                                   │  pos_transaction + outbox_event written in ONE local transaction
                         Outbox Relay :8085 ── FOR UPDATE SKIP LOCKED, keyed by transaction id
                                   │
                         Kafka  topic pos.transaction-events (+ .DLT)
     ┌──────────────┬──────────────┼──────────────┬───────────────────┐
 Inventory :8086  Receipt :8087  Loyalty :8088  Analytics :8089   AI Platform :8090
                                                                   ├─ Fraud model (logistic regression)
                                                                   ├─ Recommendation (co-purchase cosine)
                                                                   └─ Forecast (Holt linear smoothing)

GenAI Assistant :8091 ── Claude (Anthropic Java SDK, tool use) + Spring AI VectorStore (pgvector)
                         over the POS knowledge base + read-only POS API tools via the gateway
```

## Modules

| Module | Port | What it does |
|---|---|---|
| `common` | – | Event contracts (`TransactionCompleted`, `TransactionRefunded`, envelope), JSON config, Kafka retry + dead-letter defaults |
| `api-gateway` | 8080 | Spring Cloud Gateway routes, request ids, optional `X-POS-Key` auth, serves the POS web client |
| `cart-service` | 8081 | Baskets; `OPEN → LOCKED → CHECKED_OUT` lifecycle so a cart can't be edited or paid twice |
| `pricing-service` | 8082 | Catalog, promotions (category %, buy-X-get-Y, spend threshold), per-store tax, all in integer cents |
| `checkout-service` | 8083 | The checkout saga, transactions, refunds, and the **transactional outbox** |
| `payment-service` | 8084 | Idempotent authorize/capture/refund, simulated card processor, cash + change, fraud screening |
| `outbox-relay` | 8085 | Drains `outbox_event` to Kafka (at-least-once, ordered per transaction) |
| `inventory-service` | 8086 | Stock levels from events, low-stock report, manual adjustments |
| `receipt-service` | 8087 | 40-column receipts from events |
| `loyalty-service` | 8088 | Points ledger with tiers; refunds claw back exactly what the sale earned |
| `analytics-service` | 8089 | Live per-store aggregates (gross/net, tickets, avg ticket, by hour, by tender, top SKUs) |
| `ai-platform` | 8090 | Fraud scoring API + recommendation + demand-forecast models trained online from Kafka |
| `genai-assistant` | 8091 | Associate assistant: RAG + Claude tool loop over POS APIs |

## Correctness guarantees (and where they live)

- **No double charge.** `POST /api/checkout` requires an `Idempotency-Key`. A replay returns the original
  transaction; a request whose payment outcome is unknown (timeout) stays `PENDING` and the retry *resumes* it.
  The payment service is itself idempotent on transaction id (unique constraint), so the resumed authorize
  returns the original result. → `CheckoutService`, `PaymentService`
- **Event iff commit.** The `TransactionCompleted`/`TransactionRefunded` row is inserted in the same DB
  transaction as the state change (`OutboxWriter` uses `Propagation.MANDATORY`). There is no dual write to Kafka.
- **At-least-once, ordered.** The relay marks a row published only after the broker acks it, keys records by
  transaction id, and stops publishing an aggregate's later events if an earlier one fails. Several relay
  replicas can run concurrently thanks to `FOR UPDATE SKIP LOCKED`.
- **Idempotent consumers.** Inventory claims each event id in an inbox table in the same transaction as the
  stock change; loyalty's ledger has a unique `event_id`; receipts/analytics/AI de-duplicate in memory.
  Both use `INSERT … ON CONFLICT DO NOTHING`, never "catch the duplicate-key error", because in PostgreSQL
  a failed statement aborts the surrounding transaction.
- **Poison messages don't block a partition.** Malformed events go straight to `pos.transaction-events.DLT`;
  transient failures retry with exponential back-off first. → `common/KafkaConsumerDefaults`
- **Money is integers.** Cents everywhere; percentage discounts and tax round half-up once; threshold discounts
  are pro-rated across lines so line totals + tax always reconcile to the transaction total.
- **Fraud check can't stall the till.** 300 ms timeout; if the fraud service is down, card payments up to $100
  fail open and larger ones decline with `FRAUD_CHECK_UNAVAILABLE`.
- **Internal endpoints aren't public.** The gateway doesn't route `/api/payments/**`, `/api/fraud/**` or the cart
  `lock/unlock/complete` calls that only checkout should make.

## Run it

Prerequisites: JDK 21, Maven 3.9, Docker.

**Everything in containers**

```bash
cd payments
export ANTHROPIC_API_KEY=...        # optional - without it the assistant answers from the knowledge base only
docker compose up --build
open http://localhost:8080          # POS client
```

**Services as local JVMs (faster dev loop)**

```bash
cd payments
docker compose up -d postgres kafka
mvn -q package -DskipTests
scripts/run-local.sh                # logs in ./logs; `scripts/run-local.sh stop` to stop
scripts/demo.sh                     # scripted sale → retry → events → decline → refund → assistant
```

**Tests** (H2 + embedded Kafka, no Docker needed): `mvn verify`

### Simulated cards

| Token | Result |
|---|---|
| `tok_visa`, `tok_mastercard`, `tok_amex` | approved |
| `tok_decline` | declined `INSUFFICIENT_FUNDS` |
| `tok_expired` | declined `EXPIRED_CARD` |
| `tok_limit_<cents>` | approved up to that amount |

## API (through the gateway)

```
POST /api/carts                         {storeId, terminalId, customerId?}
POST /api/carts/{id}/items              {sku, quantity}
PUT  /api/carts/{id}/items/{sku}        {quantity}          (0 removes)
PUT  /api/carts/{id}/customer           {customerId}
GET  /api/products?q=   /api/promotions   POST /api/pricing/quote {storeId, items}
POST /api/checkout                      Idempotency-Key: <uuid>   {cartId, payment:{method: CARD|CASH, cardToken?, tenderedCents?}}
                                        201 paid · 402 declined · 503 outcome unknown (retry with the same key)
GET  /api/transactions/{id}   GET /api/transactions?storeId=&limit=
POST /api/transactions/{id}/refund      Idempotency-Key: <key>    {reason}
GET  /api/receipts/{txId}[/text]   /api/inventory[/{sku}|/low-stock]   /api/loyalty/{customerId}
GET  /api/analytics/stores/{storeId}   /api/recommendations?sku=..&sku=..   /api/forecast/{sku}?days=7
POST /api/assistant/chat                {message, storeId?, conversationId?}
```

## GenAI assistant

- **LLM:** Claude via the official Anthropic Java SDK (`claude-opus-5-5` by default, `ASSISTANT_MODEL` to
  change; `ASSISTANT_EFFORT` sets `output_config.effort`, default `medium`). A manual tool-use loop runs up to 8
  rounds; history is append-only (`Message.toParam()`), and the system prompt is prompt-cached. Server-side refusal
  fallbacks are enabled (`fallbacks: "default"`).
- **RAG:** Spring AI `VectorStore` — pgvector in Postgres (`assistant` DB) with local ONNX
  all-MiniLM-L6-v2 embeddings (`spring-ai-transformers`, downloaded once on first start). Knowledge base is
  Markdown in `genai-assistant/src/main/resources/kb/`, re-ingested idempotently at start-up. Set
  `ASSISTANT_EMBEDDINGS=hashing` / `ASSISTANT_VECTOR_STORE=simple` for a fully offline, no-database mode.
- **Tools (read-only):** transactions, recent transactions, product search, promotions, inventory, low stock,
  loyalty, recommendations, demand forecast, sales summary, knowledge-base search. The assistant can explain a
  refund but cannot issue one — money-moving actions stay with the associate.
- **No API key?** It still works in *retrieval-only* mode and returns the relevant policy passages.

LangChain4j would slot in the same place; Spring AI was used because the rest of the stack is Spring.

## What's simulated / not production-ready

- The card processor is a simulator behind the `CardProcessor` port; no PAN ever enters the system (tokens only,
  fingerprints are SHA-256 of the token). A real deployment adds a processor adapter, P2PE readers and PCI scope
  controls.
- Fraud model weights are hand-set (logistic regression with explainable reason codes); recommendation and
  forecast models are simple online algorithms. Each sits behind a small interface so a trained model
  (ONNX/XGBoost, a feature store) can replace it.
- Receipt, analytics and AI-platform state is in memory and rebuilt by replaying the topic on start-up
  (they use a per-instance consumer group). Kafka retention must cover the history you want rebuilt.
- Terminal auth is a shared API key at the gateway; production would use per-terminal mTLS/OAuth2.
- Cart, checkout and payment share one Transaction DB (as in the diagram) but each owns its tables and Flyway
  history table.
