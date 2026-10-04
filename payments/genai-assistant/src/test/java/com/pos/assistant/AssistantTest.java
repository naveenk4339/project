package com.pos.assistant;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.ObjectMappers;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.services.blocking.MessageService;
import com.pos.assistant.chat.AssistantService;
import com.pos.assistant.kb.KnowledgeBase;
import com.pos.assistant.tools.PosApiTools;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class AssistantTest {

    @Autowired
    KnowledgeBase knowledgeBase;
    @Autowired
    AssistantService assistant;
    @Autowired
    PosApiTools tools;

    @MockitoBean
    AnthropicClient claude;
    @MockitoBean
    PosApiTools mockedTools;

    @Test
    void retrievesTheRelevantPolicy() {
        List<KnowledgeBase.Hit> hits = knowledgeBase.search("card declined suspected fraud what do I do", 3);
        assertThat(hits).isNotEmpty();
        assertThat(hits.getFirst().title()).isEqualTo("Payment Declines & Decline Codes");

        assertThat(knowledgeBase.search("how many loyalty points does a gold member earn", 2))
                .extracting(KnowledgeBase.Hit::title).contains("Loyalty Program");
    }

    @Test
    void runsToolLoopAndReturnsFinalAnswer() {
        MessageService messages = mock(MessageService.class);
        when(claude.messages()).thenReturn(messages);
        when(mockedTools.definitions()).thenReturn(List.of());
        when(mockedTools.execute(eq("get_transaction"), any()))
                .thenReturn(new PosApiTools.Result("{\"id\":\"TX-1\",\"status\":\"DECLINED\",\"declineReason\":\"SUSPECTED_FRAUD\"}", false));

        Message toolCall = message("""
                {"id":"msg_1","type":"message","role":"assistant","model":"claude-opus-5-5",
                 "content":[{"type":"tool_use","id":"toolu_1","name":"get_transaction","input":{"transaction_id":"TX-1"}}],
                 "stop_reason":"tool_use","stop_sequence":null,"usage":{"input_tokens":10,"output_tokens":5}}""");
        Message answer = message("""
                {"id":"msg_2","type":"message","role":"assistant","model":"claude-opus-5-5",
                 "content":[{"type":"text","text":"TX-1 was declined as suspected fraud (Payment Declines). Ask for another tender."}],
                 "stop_reason":"end_turn","stop_sequence":null,"usage":{"input_tokens":10,"output_tokens":5}}""");
        when(messages.create(any(MessageCreateParams.class))).thenReturn(toolCall, answer);

        AssistantService.Reply reply = assistant.chat(null, "store-001", "Why was TX-1 declined?");

        assertThat(reply.mode()).isEqualTo("claude");
        assertThat(reply.answer()).contains("suspected fraud");
        assertThat(reply.toolsUsed()).containsExactly("get_transaction");
        verify(mockedTools).execute("get_transaction", Map.of("transaction_id", "TX-1"));

        ArgumentCaptor<MessageCreateParams> sent = ArgumentCaptor.forClass(MessageCreateParams.class);
        verify(messages, times(2)).create(sent.capture());
        MessageCreateParams second = sent.getAllValues().get(1);
        assertThat(second.model().asString()).isEqualTo("claude-opus-5-5");
        // user question (with KB context) -> assistant tool_use -> user tool_result
        List<MessageParam> history = second.messages();
        assertThat(history).hasSize(3);
        assertThat(history.get(0).content().asString()).contains("<knowledge_base>", "Associate's store: store-001");
        ContentBlockParam toolResult = history.get(2).content().asBlockParams().getFirst();
        assertThat(toolResult.toolResult().orElseThrow().toolUseId()).isEqualTo("toolu_1");

        // a follow-up in the same conversation carries the earlier turns forward unchanged
        when(messages.create(any(MessageCreateParams.class))).thenReturn(answer);
        assistant.chat(reply.conversationId(), "store-001", "And what should I tell the customer?");
        verify(messages, times(3)).create(sent.capture());
        assertThat(sent.getValue().messages()).hasSize(5);
    }

    /** Parsed exactly like an API response body. */
    private static Message message(String json) {
        try {
            return ObjectMappers.jsonMapper().readValue(json, Message.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
