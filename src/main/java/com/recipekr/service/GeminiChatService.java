package com.recipekr.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GeminiChatService - Java에서 직접 Gemini REST API 호출
 * 실시간 챗봇 대화를 위해 파이썬을 거치지 않고 직접 통신합니다.
 */
@Slf4j
@Service
public class GeminiChatService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Environment environment;
    private String apiKeyCache = null;

    public GeminiChatService(Environment environment) {
        this.environment = environment;
    }

    private boolean isDemoMode() {
        return Arrays.asList(environment.getActiveProfiles()).contains("demo");
    }

    private String getApiKey() {
        if (apiKeyCache != null) return apiKeyCache;
        
        // 1. 시스템 환경변수 확인
        String key = System.getenv("GEMINI_API_KEY");
        if (key != null && !key.isBlank()) {
            apiKeyCache = key;
            return key;
        }

        // 2. .env 파일 파싱 (프로젝트 루트 또는 python-ai 폴더)
        Path[] envPaths = {
            Paths.get(System.getProperty("user.dir"), ".env"),
            Paths.get(System.getProperty("user.dir"), "python-ai", ".env")
        };

        for (Path path : envPaths) {
            try {
                if (Files.exists(path)) {
                    List<String> lines = Files.readAllLines(path);
                    for (String line : lines) {
                        line = line.trim();
                        if (line.startsWith("GEMINI_API_KEY=")) {
                            String parsedKey = line.substring("GEMINI_API_KEY=".length()).replace("\"", "").replace("'", "");
                            apiKeyCache = parsedKey;
                            return parsedKey;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn(".env 파일 읽기 실패: {}", path, e);
            }
        }
        
        return null;
    }

    /**
     * Gemini REST API를 호출하여 채팅 응답을 받습니다.
     * @param history 대화 기록 (역할과 텍스트)
     * @param message 사용자 입력 메시지
     * @return AI 응답 텍스트
     */
    public String chat(List<Map<String, String>> history, String message) {
        if (isDemoMode()) {
            return "Demo mode response: I can help you explore the RecipeKR UI without a Gemini API key. Try the recipe recommendation page, login with demo / Admin1234!, or browse sample discount ingredients.";
        }

        String apiKey = getApiKey();
        if (apiKey == null) {
            return "API 키가 설정되지 않았습니다. 관리자에게 문의하세요.";
        }

        try {
            // 요청 바디 생성 (Gemini API 형식에 맞게)
            List<Map<String, Object>> contents = new ArrayList<>();
            
            // 시스템 프롬프트를 첫 메시지로 추가 (역할 부여)
            Map<String, Object> systemContent = new HashMap<>();
            systemContent.put("role", "user"); // gemini는 system 역할을 별도로 지원하는 방식이 다를 수 있어 user로 주입 후 model 응답으로 시작
            systemContent.put("parts", List.of(Map.of("text", "너는 이제부터 '냉장고 레시피' 서비스의 친절하고 유머러스한 요리사 제미나이(Gemini)야. 요리 레시피, 재료 보관법, 영양 정보에 대해서만 답변해줘. 마크다운으로 이쁘게 꾸며줘.")));
            contents.add(systemContent);
            
            Map<String, Object> systemAck = new HashMap<>();
            systemAck.put("role", "model");
            systemAck.put("parts", List.of(Map.of("text", "네, 알겠습니다! 저는 지금부터 친절한 냉장고 레시피 요리사입니다! 무엇을 도와드릴까요?")));
            contents.add(systemAck);

            // 이전 히스토리 추가
            if (history != null) {
                for (Map<String, String> h : history) {
                    Map<String, Object> content = new HashMap<>();
                    content.put("role", h.get("role").equals("user") ? "user" : "model");
                    content.put("parts", List.of(Map.of("text", h.get("content"))));
                    contents.add(content);
                }
            }

            // 현재 메시지 추가
            Map<String, Object> currentContent = new HashMap<>();
            currentContent.put("role", "user");
            currentContent.put("parts", List.of(Map.of("text", message)));
            contents.add(currentContent);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", contents);

            // 요청 보내기
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String[] models = {"gemini-2.5-flash", "gemini-1.5-flash", "gemini-pro"};
            String responseBody = null;
            Exception lastException = null;

            for (String modelName : models) {
                String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey;
                try {
                    ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
                    responseBody = response.getBody();
                    log.info("Gemini 챗봇 응답 성공 (model: {})", modelName);
                    break;
                } catch (Exception e) {
                    lastException = e;
                    log.warn("Gemini model {} 호출 실패: {}", modelName, e.getMessage());
                }
            }

            if (responseBody == null) {
                throw new RuntimeException("모든 Gemini 모델 호출에 실패했습니다. 마지막 오류: " + (lastException != null ? lastException.getMessage() : ""));
            }

            // 응답 파싱
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode candidates = rootNode.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    return parts.get(0).path("text").asText();
                }
            }
            return "죄송합니다. 답변을 생성하지 못했습니다.";

        } catch (Exception e) {
            log.error("Gemini 챗봇 API 호출 실패", e);
            return "API 호출 중 오류가 발생했습니다: " + e.getMessage();
        }
    }
}
