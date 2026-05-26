package com.recipekr.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipekr.domain.DiscountItem;
import com.recipekr.repository.DiscountItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * DiscountCrawlerService - RPA 크롤러 실행 및 결과 저장 서비스
 * -----------------------------------------------------------
 * Spring Boot에서 Python Playwright 크롤러를 ProcessBuilder로 호출하고,
 * 결과를 DiscountItemRepository를 통해 DB에 저장합니다.
 *
 * [스케줄 흐름]
 * CrawlerScheduler (매일 새벽 1시)
 *   → runAll() 호출
 *   → Python run_crawler.py 실행 (이마트/롯데마트/홈플러스)
 *   → stdout JSON 파싱 → DiscountItem 변환 → DB UPSERT 저장
 *   → 7일 이상 오래된 데이터 정리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiscountCrawlerService {

    private final DiscountItemRepository discountItemRepository;
    private final Environment environment;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private boolean isDemoMode() {
        return java.util.Arrays.asList(environment.getActiveProfiles()).contains("demo");
    }

    private String getPythonExecutable() {
        String projectConda = Paths.get(System.getProperty("user.dir"), ".conda", "python.exe").toString();
        if (new java.io.File(projectConda).exists()) {
            return projectConda;
        }
        // local myenv conda environment support
        String myenvConda = Paths.get(System.getProperty("user.home"), "anaconda3", "envs", "myenv", "python.exe").toString();
        if (new java.io.File(myenvConda).exists()) {
            return myenvConda;
        }
        return "python";
    }

    /** 크롤러 스크립트 위치: {프로젝트루트}/python-ai/crawler/run_crawler.py */
    private Path getCrawlerScript() {
        return Paths.get(System.getProperty("user.dir"))
                .resolve("python-ai")
                .resolve("crawler")
                .resolve("run_crawler.py")
                .toAbsolutePath();
    }

    // ─────────────────────────────────────────────────────────
    // 크롤러 실행
    // ─────────────────────────────────────────────────────────

    /**
     * 이마트, 롯데마트, 홈플러스 3사 전체 크롤링을 실행합니다.
     * Python 프로세스는 dry-run 없이 DB에 직접 저장합니다.
     * (Python 측이 직접 DB 저장을 하므로 Java에서는 실행만 트리거)
     *
     * @return 성공 여부
     */
    public boolean runAll() {
        if (isDemoMode()) {
            log.info("[Crawler] Demo mode: skipping real RPA crawl and using seeded sample discount data.");
            return true;
        }
        return runMarket("all");
    }

    /**
     * 특정 마트 크롤링 실행
     *
     * @param market "all" | "emart" | "lottemart" | "homeplus"
     * @return 성공 여부
     */
    public boolean runMarket(String market) {
        if (isDemoMode()) {
            log.info("[Crawler] Demo mode: skipping real RPA crawl for market={}", market);
            return true;
        }

        Path scriptPath = getCrawlerScript();
        log.info("[크롤러] Python 크롤러 실행 시작: market={}, script={}", market, scriptPath);

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    getPythonExecutable(),
                    scriptPath.toString(),
                    "--market", market
            );
            pb.redirectErrorStream(false);
            pb.environment().put("PYTHONIOENCODING", "utf-8");
            pb.environment().put("PYTHONUTF8", "1");

            // DB 접속 정보를 환경변수로 전달 (Python 크롤러가 읽음)
            String tidbUrl = System.getenv("TIDB_URL");
            String tidbUser = System.getenv("TIDB_USERNAME");
            String tidbPass = System.getenv("TIDB_PASSWORD");
            if (tidbUrl != null) pb.environment().put("TIDB_URL", tidbUrl);
            if (tidbUser != null) pb.environment().put("TIDB_USERNAME", tidbUser);
            if (tidbPass != null) pb.environment().put("TIDB_PASSWORD", tidbPass);

            String rdsUrl = System.getenv("RDS_URL");
            String rdsUser = System.getenv("RDS_USERNAME");
            String rdsPass = System.getenv("RDS_PASSWORD");
            if (rdsUrl != null) pb.environment().put("RDS_URL", rdsUrl);
            if (rdsUser != null) pb.environment().put("RDS_USERNAME", rdsUser);
            if (rdsPass != null) pb.environment().put("RDS_PASSWORD", rdsPass);

            // 로컬(tidb 프로필): RDS_URL 없으면 TIDB_URL 값을 RDS_URL에도 복사하여 Python 크롤러가 인식할 수 있도록
            if (rdsUrl == null && tidbUrl != null) {
                pb.environment().put("RDS_URL", tidbUrl);
                if (tidbUser != null) pb.environment().put("RDS_USERNAME", tidbUser);
                if (tidbPass != null) pb.environment().put("RDS_PASSWORD", tidbPass);
            }
            // 서버(rds 프로필): TIDB_URL 없으면 RDS_URL 값을 TIDB_URL에도 복사
            if (tidbUrl == null && rdsUrl != null) {
                pb.environment().put("TIDB_URL", rdsUrl);
                if (rdsUser != null) pb.environment().put("TIDB_USERNAME", rdsUser);
                if (rdsPass != null) pb.environment().put("TIDB_PASSWORD", rdsPass);
            }

            // 크롤러 스크립트가 있는 디렉토리를 작업 디렉토리로 설정
            pb.directory(scriptPath.getParent().toFile());

            Process process = pb.start();

            // stderr (로그) 출력 스레드
            Thread errThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("[크롤러 로그] {}", line);
                    }
                } catch (Exception ignored) {}
            });
            errThread.start();

            // stdout 소비 (Python이 직접 DB 저장하므로 여기선 버림)
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                while (reader.readLine() != null) { /* drain */ }
            }

            int exitCode = process.waitFor();
            errThread.join(5000);

            if (exitCode == 0) {
                log.info("[크롤러] 크롤링 완료 (market={})", market);
                return true;
            } else {
                log.error("[크롤러] 크롤러 비정상 종료 (exitCode={})", exitCode);
                return false;
            }

        } catch (Exception e) {
            log.error("[크롤러] 크롤러 실행 오류: {}", e.getMessage(), e);
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────
    // 오늘의 할인 재료 조회 (AI 추천 연동)
    // ─────────────────────────────────────────────────────────

    /**
     * 오늘의 할인 식재료 목록을 DB에서 조회합니다.
     * AI 추천 엔진(predict.py)에 전달할 할인 재료 리스트 구성에 사용합니다.
     *
     * @return 오늘의 할인 DiscountItem 목록
     */
    public List<DiscountItem> getTodayDiscountItems() {
        List<DiscountItem> items = discountItemRepository.findTodayItems();
        log.debug("[크롤러] 오늘의 할인 재료 {}건 조회", items.size());
        return items;
    }

    /**
     * 오늘의 할인 식재료명 문자열 목록을 반환합니다.
     * AI 추천 시 할인 재료에 가중치를 부여하기 위해 사용합니다.
     *
     * @return 식재료명 리스트 (예: ["삼겹살", "양파", "감자"])
     */
    public List<String> getTodayDiscountIngredientNames() {
        return getTodayDiscountItems().stream()
                .map(DiscountItem::getIngredientName)
                .distinct()
                .toList();
    }

    /**
     * 마트별 오늘의 할인 식재료를 조회합니다.
     *
     * @param marketName 마트명 (EMART | LOTTEMART | HOMEPLUS)
     * @return 해당 마트 오늘 할인 아이템 목록
     */
    public List<DiscountItem> getTodayItemsByMarket(String marketName) {
        return discountItemRepository.findTodayItemsByMarket(marketName);
    }

    // ─────────────────────────────────────────────────────────
    // 데이터 정리
    // ─────────────────────────────────────────────────────────

    /**
     * 7일 이상 오래된 할인 데이터를 삭제합니다.
     * CrawlerScheduler에서 크롤링 후 자동 호출됩니다.
     */
    public void cleanupOldData() {
        LocalDate cutoff = LocalDate.now().minusDays(7);
        discountItemRepository.deleteOlderThan(cutoff);
    }

    // ─────────────────────────────────────────────────────────
    // 크롤링 상태 확인
    // ─────────────────────────────────────────────────────────

    /**
     * 오늘 크롤링이 완료된 마트 목록을 반환합니다.
     *
     * @return 오늘 데이터가 있는 마트명 목록
     */
    public List<String> getCrawledMarketsToday() {
        return discountItemRepository.findCrawledMarketsToday();
    }

    /**
     * 오늘 크롤링이 아직 안 된 경우 여부를 확인합니다.
     *
     * @return 오늘 할인 데이터가 없으면 true
     */
    public boolean isTodayCrawlNeeded() {
        return discountItemRepository.findCrawledMarketsToday().isEmpty();
    }
}
