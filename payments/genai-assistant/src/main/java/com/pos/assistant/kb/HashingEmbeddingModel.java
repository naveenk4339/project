package com.pos.assistant.kb;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Feature-hashed bag-of-words embeddings (unigrams + bigrams, L2-normalised). No model download and fully
 * deterministic, which makes it the right choice for tests and air-gapped demos; use the transformers model
 * for real semantic retrieval.
 */
public class HashingEmbeddingModel implements EmbeddingModel {

    private final int dimensions;

    public HashingEmbeddingModel(int dimensions) {
        this.dimensions = dimensions;
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>();
        List<String> inputs = request.getInstructions();
        for (int i = 0; i < inputs.size(); i++) {
            embeddings.add(new Embedding(vector(inputs.get(i)), i));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        return vector(document.getText());
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    float[] vector(String text) {
        float[] v = new float[dimensions];
        String[] tokens = text.toLowerCase(Locale.ROOT).split("[^a-z0-9_]+");
        String previous = null;
        for (String token : tokens) {
            if (token.length() < 2) {
                continue;
            }
            add(v, token, 1.0f);
            if (previous != null) {
                add(v, previous + " " + token, 0.5f);
            }
            previous = token;
        }
        double norm = 0;
        for (float x : v) {
            norm += x * x;
        }
        if (norm > 0) {
            float inv = (float) (1 / Math.sqrt(norm));
            for (int i = 0; i < v.length; i++) {
                v[i] *= inv;
            }
        }
        return v;
    }

    private void add(float[] v, String feature, float weight) {
        int h = feature.hashCode();
        v[Math.floorMod(h, dimensions)] += (h & 0x40000000) == 0 ? weight : -weight;
    }
}
