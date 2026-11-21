package com.RAGSupport;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @GetMapping("/query")
    public Map<String, String> query(@RequestParam String question) {
        String answer = chatbotService.answerUserQuery(question);
        return Map.of("answer", answer);
    }

    @PostMapping("/load-knowledge")
    public Map<String, String> loadKnowledge() {
        chatbotService.loadKnowledgeBase();
        return Map.of("message", "Knowledge base indexing triggered successfully.");
    }
}
