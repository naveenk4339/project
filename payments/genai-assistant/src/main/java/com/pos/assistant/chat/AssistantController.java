package com.pos.assistant.chat;

import com.pos.assistant.kb.KnowledgeBase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistant;
    private final KnowledgeBase knowledgeBase;

    public AssistantController(AssistantService assistant, KnowledgeBase knowledgeBase) {
        this.assistant = assistant;
        this.knowledgeBase = knowledgeBase;
    }

    @PostMapping("/chat")
    public AssistantService.Reply chat(@Valid @RequestBody ChatRequest request) {
        return assistant.chat(request.conversationId(), request.storeId(), request.message());
    }

    @GetMapping("/kb/search")
    public List<KnowledgeBase.Hit> search(@RequestParam String q, @RequestParam(defaultValue = "4") int k) {
        return knowledgeBase.search(q, Math.min(k, 10));
    }

    public record ChatRequest(String conversationId, String storeId, @NotBlank @Size(max = 4000) String message) {
    }
}
