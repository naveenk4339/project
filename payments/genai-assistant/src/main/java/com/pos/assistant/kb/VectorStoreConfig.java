package com.pos.assistant.kb;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class VectorStoreConfig {

    /** all-MiniLM-L6-v2 output size, shared by both embedding options so the pgvector table never changes shape. */
    static final int DIMENSIONS = 384;

    @Bean
    @ConditionalOnProperty(name = "assistant.embeddings", havingValue = "transformers", matchIfMissing = true)
    EmbeddingModel transformersEmbeddingModel() {
        // downloads and caches the ONNX model on first start; runs locally on CPU afterwards
        return new TransformersEmbeddingModel();
    }

    @Bean
    @ConditionalOnProperty(name = "assistant.embeddings", havingValue = "hashing")
    EmbeddingModel hashingEmbeddingModel() {
        return new HashingEmbeddingModel(DIMENSIONS);
    }

    @Bean
    @ConditionalOnProperty(name = "assistant.vector-store", havingValue = "pgvector", matchIfMissing = true)
    VectorStore pgVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(DIMENSIONS)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .vectorTableName("pos_knowledge_base")
                .initializeSchema(true)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "assistant.vector-store", havingValue = "simple")
    VectorStore simpleVectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
