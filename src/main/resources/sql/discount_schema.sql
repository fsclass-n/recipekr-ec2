-- ============================================================
-- 냉장고 파먹기 - 마트 할인 식재료 테이블
-- ============================================================
-- 대상 마트: 이마트(EMART), 롯데마트(LOTTEMART), 홈플러스(HOMEPLUS)
-- 매일 새벽 크롤러가 갱신 (UPSERT 방식)
-- ============================================================

USE recipekr;

CREATE TABLE IF NOT EXISTS market_discount (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '고유 ID',
    market_name     VARCHAR(30)     NOT NULL                COMMENT '마트명 (EMART | LOTTEMART | HOMEPLUS)',
    product_name    VARCHAR(255)    NOT NULL                COMMENT '상품명',
    ingredient_name VARCHAR(100)    NOT NULL                COMMENT '정규화된 식재료명 (AI 매칭용)',
    original_price  INT             NULL                    COMMENT '정가 (원)',
    discount_price  INT             NULL                    COMMENT '할인가 (원)',
    discount_rate   DECIMAL(5,2)    NULL                    COMMENT '할인율 (%)',
    discount_period VARCHAR(100)    NULL                    COMMENT '할인 기간 (예: ~5/28)',
    image_url       VARCHAR(500)    NULL                    COMMENT '상품 이미지 URL',
    product_url     VARCHAR(500)    NULL                    COMMENT '상품 상세 페이지 URL',
    crawled_date    DATE            NOT NULL                COMMENT '크롤링 날짜',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '최초 등록일시',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '최종 수정일시',

    PRIMARY KEY (id),
    UNIQUE KEY uk_market_product_date (market_name, product_name, crawled_date),
    INDEX idx_market_ingredient (ingredient_name),
    INDEX idx_market_crawled_date (crawled_date),
    INDEX idx_market_name (market_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='대형마트 할인 식재료 정보 (RPA 크롤러 적재)';
