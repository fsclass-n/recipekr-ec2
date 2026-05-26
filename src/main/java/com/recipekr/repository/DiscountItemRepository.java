package com.recipekr.repository;

import com.recipekr.domain.DiscountItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * DiscountItemRepository - 마트 할인 식재료 데이터 접근 계층
 * -----------------------------------------------------------
 * JdbcTemplate 기반으로 market_discount 테이블에 CRUD를 수행합니다.
 *
 * 주요 사용 흐름:
 * 1. [배치] CrawlerScheduler → saveBatch() : 크롤링 결과 UPSERT 저장
 * 2. [실시간] AiRecommendService → findTodayItems() : 오늘의 할인 재료 조회
 * 3. [화면] HomeController → findTodayItemsByMarket() : 마트별 할인 재료 노출
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DiscountItemRepository {

    private final JdbcTemplate jdbcTemplate;

    // ─────────────────────────────────────────────────────────
    // RowMapper
    // ─────────────────────────────────────────────────────────
    private static final RowMapper<DiscountItem> DISCOUNT_ITEM_MAPPER = (rs, rowNum) ->
            DiscountItem.builder()
                    .id(rs.getLong("id"))
                    .marketName(rs.getString("market_name"))
                    .productName(rs.getString("product_name"))
                    .ingredientName(rs.getString("ingredient_name"))
                    .originalPrice(nullableInt(rs, "original_price"))
                    .discountPrice(nullableInt(rs, "discount_price"))
                    .discountRate(rs.getBigDecimal("discount_rate"))
                    .discountPeriod(rs.getString("discount_period"))
                    .imageUrl(rs.getString("image_url"))
                    .productUrl(rs.getString("product_url"))
                    .crawledDate(rs.getDate("crawled_date") != null
                            ? rs.getDate("crawled_date").toLocalDate() : null)
                    .build();

    private static Integer nullableInt(ResultSet rs, String col) throws SQLException {
        int val = rs.getInt(col);
        return rs.wasNull() ? null : val;
    }

    // ─────────────────────────────────────────────────────────
    // WRITE
    // ─────────────────────────────────────────────────────────

    /**
     * 크롤러 결과를 UPSERT 저장합니다.
     * 같은 날짜·마트·상품명이 이미 있으면 가격/할인율만 갱신합니다.
     *
     * @param item 저장할 할인 아이템
     */
    public void save(DiscountItem item) {
        String sql = """
                INSERT INTO market_discount (
                    market_name, product_name, ingredient_name,
                    original_price, discount_price, discount_rate,
                    discount_period, image_url, product_url, crawled_date
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    ingredient_name  = VALUES(ingredient_name),
                    original_price   = VALUES(original_price),
                    discount_price   = VALUES(discount_price),
                    discount_rate    = VALUES(discount_rate),
                    discount_period  = VALUES(discount_period),
                    image_url        = VALUES(image_url),
                    product_url      = VALUES(product_url),
                    updated_at       = NOW()
                """;
        jdbcTemplate.update(sql,
                item.getMarketName(),
                item.getProductName(),
                item.getIngredientName(),
                item.getOriginalPrice(),
                item.getDiscountPrice(),
                item.getDiscountRate(),
                item.getDiscountPeriod(),
                item.getImageUrl(),
                item.getProductUrl(),
                item.getCrawledDate()
        );
    }

    /**
     * 크롤러 결과 리스트를 배치 UPSERT 저장합니다.
     *
     * @param items 저장할 할인 아이템 목록
     * @return 저장된 건수
     */
    public int saveBatch(List<DiscountItem> items) {
        int saved = 0;
        for (DiscountItem item : items) {
            try {
                save(item);
                saved++;
            } catch (Exception e) {
                log.warn("[DiscountItemRepository] 저장 실패: {} - {}", item.getProductName(), e.getMessage());
            }
        }
        log.info("[DiscountItemRepository] 배치 저장 완료: {}건", saved);
        return saved;
    }

    // ─────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────

    /**
     * 오늘의 전체 할인 식재료 목록을 조회합니다.
     * AI 추천 엔진이 이 목록을 할인 가중치 계산에 활용합니다.
     *
     * @return 오늘 날짜 기준 전체 할인 아이템 목록
     */
    public List<DiscountItem> findTodayItems() {
        String sql = """
                SELECT * FROM market_discount
                WHERE crawled_date = (SELECT MAX(crawled_date) FROM market_discount)
                ORDER BY discount_rate DESC
                """;
        return jdbcTemplate.query(sql, DISCOUNT_ITEM_MAPPER);
    }

    /**
     * 오늘의 할인 식재료를 마트별로 조회합니다.
     *
     * @param marketName 마트명 (EMART | LOTTEMART | HOMEPLUS)
     * @return 해당 마트의 오늘 할인 아이템 목록
     */
    public List<DiscountItem> findTodayItemsByMarket(String marketName) {
        String sql = """
                SELECT * FROM market_discount
                WHERE crawled_date = (SELECT MAX(crawled_date) FROM market_discount)
                  AND market_name = ?
                ORDER BY discount_rate DESC
                """;
        return jdbcTemplate.query(sql, DISCOUNT_ITEM_MAPPER, marketName);
    }

    /**
     * 특정 식재료명으로 오늘의 할인 정보를 조회합니다.
     * AI 추천 결과와 할인 정보를 결합할 때 사용합니다.
     *
     * @param ingredientName 식재료명 (예: "삼겹살", "양파")
     * @return 해당 식재료의 오늘 할인 아이템 목록 (복수 마트 가능)
     */
    public List<DiscountItem> findTodayByIngredient(String ingredientName) {
        String sql = """
                SELECT * FROM market_discount
                WHERE crawled_date = (SELECT MAX(crawled_date) FROM market_discount)
                  AND ingredient_name = ?
                ORDER BY discount_price ASC
                """;
        return jdbcTemplate.query(sql, DISCOUNT_ITEM_MAPPER, ingredientName);
    }

    /**
     * 오늘 크롤링된 마트 목록을 조회합니다. (크롤링 성공 여부 확인용)
     *
     * @return 오늘 데이터가 있는 마트명 목록
     */
    public List<String> findCrawledMarketsToday() {
        String sql = """
                SELECT DISTINCT market_name
                FROM market_discount
                WHERE crawled_date = (SELECT MAX(crawled_date) FROM market_discount)
                """;
        return jdbcTemplate.queryForList(sql, String.class);
    }

    /**
     * 지정 날짜 이전 오래된 할인 정보를 삭제합니다. (데이터 정리 배치용)
     *
     * @param before 이 날짜 이전 데이터를 삭제
     * @return 삭제된 건수
     */
    public int deleteOlderThan(LocalDate before) {
        String sql = "DELETE FROM market_discount WHERE crawled_date < ?";
        int deleted = jdbcTemplate.update(sql, before);
        log.info("[DiscountItemRepository] 오래된 할인 데이터 {}건 삭제 (기준: {})", deleted, before);
        return deleted;
    }

    /**
     * 총 할인 식재료 수 조회
     * @return 식재료 수
     */
    public long count() {
        String sql = "SELECT COUNT(*) FROM market_discount";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }
}
