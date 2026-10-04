package com.pos.assistant.tools;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Tool;
import com.pos.assistant.kb.KnowledgeBase;
import com.pos.common.events.EventJson;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Tools Claude can call. Every tool is read-only: the assistant can look things up and explain them, but
 * money-moving actions (refunds, voids, price overrides) stay with the associate at the till.
 */
@Component
public class PosApiTools {

    private final RestClient pos;
    private final KnowledgeBase knowledgeBase;
    private final Map<String, Definition> tools = new LinkedHashMap<>();

    public PosApiTools(RestClient posApi, KnowledgeBase knowledgeBase) {
        this.pos = posApi;
        this.knowledgeBase = knowledgeBase;
        register();
    }

    private void register() {
        define("get_transaction",
                "Look up one POS transaction by id (e.g. TX-20261004-1A2B3C4D): status, line items, totals, tender, "
                        + "decline reason and refund state. Amounts are integer cents.",
                Map.of("transaction_id", string("Transaction id")), List.of("transaction_id"),
                in -> get("/api/transactions/{id}", str(in, "transaction_id")));
        define("list_recent_transactions",
                "List the most recent transactions for a store, newest first. Use to find a transaction when the "
                        + "associate only knows roughly when it happened or what was bought.",
                Map.of("store_id", string("Store id, e.g. store-001"), "limit", integer("Max rows, 1-50 (default 10)")),
                List.of("store_id"),
                in -> get("/api/transactions?storeId={s}&limit={l}", str(in, "store_id"), intOr(in, "limit", 10)));
        define("search_products",
                "Search the catalog by name, SKU or category. Returns SKU, name, category, price in cents and taxability.",
                Map.of("query", string("Free-text search, e.g. 'earbuds' or 'bakery'")), List.of("query"),
                in -> get("/api/products?q={q}", str(in, "query")));
        define("get_promotions",
                "List the promotions currently configured in pricing, with their rules.",
                Map.of(), List.of(), in -> get("/api/promotions"));
        define("get_inventory",
                "Current on-hand stock and reorder point for one SKU.",
                Map.of("sku", string("SKU, e.g. ELE-002")), List.of("sku"),
                in -> get("/api/inventory/{sku}", str(in, "sku")));
        define("get_low_stock",
                "All SKUs at or below their reorder point.",
                Map.of(), List.of(), in -> get("/api/inventory/low-stock"));
        define("get_loyalty_account",
                "Loyalty balance, tier and recent ledger entries for a customer id.",
                Map.of("customer_id", string("Loyalty customer id")), List.of("customer_id"),
                in -> get("/api/loyalty/{c}", str(in, "customer_id")));
        define("get_recommendations",
                "Items frequently bought together with the given basket (from the recommendation model).",
                Map.of("skus", Map.of("type", "array", "items", Map.of("type", "string"), "description", "SKUs in the basket"),
                        "limit", integer("How many suggestions (default 3)")),
                List.of("skus"),
                in -> pos.get().uri(b -> b.path("/api/recommendations")
                                .queryParam("sku", strList(in, "skus").toArray())
                                .queryParam("limit", intOr(in, "limit", 3)).build())
                        .retrieve().body(String.class));
        define("get_demand_forecast",
                "Daily unit-demand forecast for a SKU from the forecasting model, with recent history.",
                Map.of("sku", string("SKU"), "days", integer("Forecast horizon in days, 1-30 (default 7)")),
                List.of("sku"),
                in -> get("/api/forecast/{sku}?days={d}", str(in, "sku"), intOr(in, "days", 7)));
        define("get_sales_summary",
                "Live sales aggregates for a store: gross/net sales, ticket count, average ticket, refunds, top SKUs, "
                        + "sales by hour (UTC) and by tender. Amounts are cents.",
                Map.of("store_id", string("Store id")), List.of("store_id"),
                in -> get("/api/analytics/stores/{s}", str(in, "store_id")));
        define("search_knowledge_base",
                "Search store policies and procedures (refunds, declines, promotions, loyalty, cash handling, card "
                        + "reader troubleshooting, receiving). Use when the passages already provided don't cover the question.",
                Map.of("query", string("What to look up")), List.of("query"),
                in -> EventJson.write(knowledgeBase.search(str(in, "query"), 4)));
    }

    public List<Tool> definitions() {
        return tools.values().stream().map(Definition::tool).toList();
    }

    /** Runs a tool. Failures are returned as {@link Result#isError()} so Claude can recover instead of the turn failing. */
    public Result execute(String name, Map<String, Object> input) {
        Definition definition = tools.get(name);
        if (definition == null) {
            return Result.error("Unknown tool: " + name);
        }
        for (String required : definition.required()) {
            if (input == null || input.get(required) == null) {
                return Result.error("Missing required parameter: " + required);
            }
        }
        try {
            return Result.ok(definition.handler().apply(input));
        } catch (RestClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            return Result.error("POS API returned " + status.value() + ": " + e.getResponseBodyAsString());
        } catch (RuntimeException e) {
            return Result.error("Tool failed: " + e.getMessage());
        }
    }

    private String get(String uri, Object... vars) {
        return pos.get().uri(uri, vars).retrieve().body(String.class);
    }

    private void define(String name, String description, Map<String, Object> properties, List<String> required,
                        Function<Map<String, Object>, String> handler) {
        Tool.InputSchema.Properties.Builder props = Tool.InputSchema.Properties.builder();
        properties.forEach((key, schema) -> props.putAdditionalProperty(key, JsonValue.from(schema)));
        Tool tool = Tool.builder()
                .name(name)
                .description(description)
                .inputSchema(Tool.InputSchema.builder().properties(props.build()).required(required).build())
                .build();
        tools.put(name, new Definition(tool, required, handler));
    }

    private static Map<String, Object> string(String description) {
        return Map.of("type", "string", "description", description);
    }

    private static Map<String, Object> integer(String description) {
        return Map.of("type", "integer", "description", description);
    }

    private static String str(Map<String, Object> in, String key) {
        return String.valueOf(in.get(key)).trim();
    }

    private static int intOr(Map<String, Object> in, String key, int fallback) {
        Object v = in.get(key);
        if (v instanceof Number n) {
            return Math.max(1, Math.min(n.intValue(), 50));
        }
        return fallback;
    }

    @SuppressWarnings("unchecked")
    private static List<String> strList(Map<String, Object> in, String key) {
        Object v = in.get(key);
        if (v instanceof List<?> list) {
            return ((List<Object>) list).stream().map(String::valueOf).toList();
        }
        return List.of(String.valueOf(v));
    }

    private record Definition(Tool tool, List<String> required, Function<Map<String, Object>, String> handler) {
    }

    public record Result(String content, boolean isError) {
        static Result ok(String content) {
            return new Result(content == null ? "" : content, false);
        }

        static Result error(String message) {
            return new Result(message, true);
        }
    }
}
