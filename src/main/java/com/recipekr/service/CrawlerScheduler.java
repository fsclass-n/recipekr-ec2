package com.recipekr.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CrawlerScheduler - RPA 크롤러 자동 실행 스케줄러
 * -------------------------------------------------
 * Spring @Scheduled를 통해 매일 새벽 1시에 이마트/롯데마트/홈플러스
 * 할인 식재료 크롤링을 자동 실행하고 DB를 갱신합니다.
 *
 * [스케줄 주기]
 * - 메인 크롤링: 매일 새벽 01:00 KST (cron: "0 0 1 * * *")
 * - 데이터 정리: 매일 새벽 02:00 KST (7일 이상 오래된 데이터 삭제)
 *
 * [활성화 조건]
 * application.yml의 crawler.enabled=true 일 때만 실행됩니다.
 * 로컬 개발 환경에서는 비활성화 가능합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlerScheduler {

    private final DiscountCrawlerService discountCrawlerService;

    /**
     * 매일 새벽 1시: 이마트/롯데마트/홈플러스 3사 전체 크롤링 실행
     * 크론 표현식: 0 0 1 * * * = 매일 01:00:00
     */
    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
    public void scheduledCrawlAll() {
        log.info("========================================");
        log.info("[스케줄] 대형마트 할인 식재료 크롤링 배치 시작");
        log.info("========================================");

        try {
            boolean success = discountCrawlerService.runAll();

            if (success) {
                List<String> crawledMarkets = discountCrawlerService.getCrawledMarketsToday();
                log.info("[스케줄] 크롤링 완료! 오늘 수집된 마트: {}", crawledMarkets);
            } else {
                log.error("[스케줄] 크롤링 실패 - 관리자 확인 필요");
            }

        } catch (Exception e) {
            log.error("[스케줄] 크롤링 배치 오류: {}", e.getMessage(), e);
        }

        log.info("========================================");
        log.info("[스케줄] 크롤링 배치 종료");
        log.info("========================================");
    }

    /**
     * 매일 새벽 2시: 7일 이상 오래된 할인 데이터 정리
     * 크론 표현식: 0 0 2 * * * = 매일 02:00:00
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    public void scheduledCleanup() {
        log.info("[스케줄] 오래된 할인 데이터 정리 배치 시작");
        try {
            discountCrawlerService.cleanupOldData();
            log.info("[스케줄] 데이터 정리 완료");
        } catch (Exception e) {
            log.error("[스케줄] 데이터 정리 오류: {}", e.getMessage(), e);
        }
    }
}
