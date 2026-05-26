package com.recipekr.controller;

import com.recipekr.service.DiscountCrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * CrawlerController - 사용자 수동 장보기(크롤링) 트리거용 API
 */
@Slf4j
@RestController
@RequestMapping("/api/crawler")
@RequiredArgsConstructor
public class CrawlerController {

    private final DiscountCrawlerService discountCrawlerService;

    @PostMapping("/run")
    public ResponseEntity<Map<String, String>> runCrawler() {
        log.info("[API] 수동 크롤링(장보기) 요청 수신");
        
        // 크롤링은 시간이 걸리므로 동기적으로 처리하여 끝날 때까지 기다립니다.
        // 프론트엔드에서는 로딩 바를 보여줍니다.
        boolean success = discountCrawlerService.runAll();
        
        Map<String, String> response = new HashMap<>();
        if (success) {
            response.put("status", "success");
            response.put("message", "크롤링 완료");
        } else {
            response.put("status", "error");
            response.put("message", "크롤링 실패");
        }
        
        return ResponseEntity.ok(response);
    }
}
