package com.pos.assistant;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param model          Claude model id
 * @param effort         output_config.effort (low | medium | high | xhigh | max)
 * @param posApiUrl      base URL of the API gateway the tools call
 * @param vectorStore    {@code pgvector} (persistent, shared) or {@code simple} (in-memory)
 * @param embeddings     {@code transformers} (local ONNX all-MiniLM-L6-v2, 384 dims) or {@code hashing} (offline, no model download)
 */
@ConfigurationProperties(prefix = "assistant")
public record AssistantProperties(
        String model,
        long maxTokens,
        String effort,
        int maxToolIterations,
        int retrievalTopK,
        String posApiUrl,
        String vectorStore,
        String embeddings) {
}
