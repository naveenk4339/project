package com.pos.assistant;

import com.pos.assistant.chat.AssistantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** No ANTHROPIC_API_KEY in this context, so no client bean exists and the assistant degrades to RAG-only answers. */
@SpringBootTest(properties = "ANTHROPIC_API_KEY=")
@ActiveProfiles("test")
class RetrievalOnlyModeTest {

    @Autowired
    AssistantService assistant;

    @Test
    void answersFromKnowledgeBaseWithoutLlm() {
        AssistantService.Reply reply = assistant.chat(null, "store-001", "Can I refund electronics after 20 days?");
        assertThat(reply.mode()).isEqualTo("retrieval-only");
        assertThat(reply.sources()).contains("Refunds & Returns");
        assertThat(reply.answer()).contains("15 days");
    }
}
