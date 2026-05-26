package com.recipekr.controller;

import com.recipekr.service.GeminiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ChatbotController - 프론트엔드 채팅 UI와 비동기로 통신하는 컨트롤러
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatbotController {

    private final GeminiChatService geminiChatService;

    @PostMapping
    public Map<String, String> chat(@RequestBody Map<String, Object> payload) {
        String message = (String) payload.get("message");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> history = (List<Map<String, String>>) payload.get("history");

        String response = geminiChatService.chat(history, message);

        return Map.of("response", response);
    }
}
