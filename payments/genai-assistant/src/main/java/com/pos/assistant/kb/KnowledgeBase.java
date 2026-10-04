package com.pos.assistant.kb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The POS knowledge base: store procedures and policies written as Markdown under {@code classpath:kb/}.
 * Each file is split into ~400-token chunks with deterministic ids, so re-ingesting on every start-up
 * upserts the same rows in pgvector instead of duplicating them.
 */
@Component
public class KnowledgeBase {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBase.class);

    private final VectorStore vectorStore;
    private final TokenTextSplitter splitter = TokenTextSplitter.builder()
            .withChunkSize(400).withMinChunkSizeChars(200).withKeepSeparator(true).build();
    private volatile boolean ready;

    public KnowledgeBase(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ingest() throws IOException {
        Resource[] files = new PathMatchingResourcePatternResolver().getResources("classpath*:kb/*.md");
        List<Document> chunks = new ArrayList<>();
        for (Resource file : files) {
            String text = file.getContentAsString(StandardCharsets.UTF_8);
            String source = file.getFilename();
            String title = text.lines().filter(l -> l.startsWith("# ")).findFirst().map(l -> l.substring(2).trim()).orElse(source);
            List<Document> split = splitter.apply(List.of(new Document(text, Map.of("source", source, "title", title))));
            for (int i = 0; i < split.size(); i++) {
                Document chunk = split.get(i);
                String id = UUID.nameUUIDFromBytes((source + "#" + i).getBytes(StandardCharsets.UTF_8)).toString();
                chunks.add(new Document(id, chunk.getText(), Map.of("source", source, "title", title, "chunk", i)));
            }
        }
        vectorStore.add(chunks);
        ready = true;
        log.info("Knowledge base ready: {} chunk(s) from {} file(s)", chunks.size(), files.length);
    }

    public List<Hit> search(String query, int topK) {
        if (!ready) {
            return List.of();
        }
        List<Document> docs = vectorStore.similaritySearch(SearchRequest.builder().query(query).topK(topK).build());
        return docs.stream()
                .map(d -> new Hit(String.valueOf(d.getMetadata().get("title")), String.valueOf(d.getMetadata().get("source")),
                        d.getText(), d.getScore() == null ? 0 : d.getScore()))
                .toList();
    }

    public record Hit(String title, String source, String text, double score) {
    }
}
