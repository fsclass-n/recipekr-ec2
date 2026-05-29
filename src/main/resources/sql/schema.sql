-- ============================================================
-- 냉장고 파먹기 AI 레시피 추천 시스템 - 데이터베이스 통합 스키마
-- ============================================================

-- 1. users 테이블 (회원 정보)
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT          NOT NULL AUTO_INCREMENT COMMENT '사용자 고유 ID',
    username    VARCHAR(50)     NOT NULL                COMMENT '로그인 아이디 (중복 불가)',
    password    VARCHAR(255)    NOT NULL                COMMENT '비밀번호 (BCrypt 해시)',
    email       VARCHAR(100)    NOT NULL                COMMENT '이메일 주소',
    nickname    VARCHAR(50)     NOT NULL                COMMENT '화면에 표시될 닉네임',
    role        VARCHAR(20)     NOT NULL DEFAULT 'USER' COMMENT '권한 (USER, ADMIN)',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '가입일시',
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',

    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원 정보 테이블';

CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_created_at ON users (created_at);

-- 테스트용 관리자 계정 삽입 (password: Admin1234!)
INSERT INTO users (username, password, email, nickname, role)
VALUES (
    'admin',
    '$2a$10$N4jMhFR9XiJWwPVhp0hQzuP0E6GFNhJoJEf9yk9P2PKJB1NsBN/qO',
    'admin@recipekr.com',
    '관리자',
    'ADMIN'
) ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 2. recipes 테이블 (레시피 정보)
CREATE TABLE IF NOT EXISTS recipes (
    id          BIGINT          NOT NULL AUTO_INCREMENT COMMENT '레시피 ID',
    title       VARCHAR(255)    NOT NULL                COMMENT '레시피 제목',
    ingredients TEXT            NOT NULL                COMMENT '필요 식재료 목록',
    calories    INT             NOT NULL                COMMENT '칼로리',
    health_type VARCHAR(50)     NULL                    COMMENT '건강 유형',
    recipe_text TEXT            NOT NULL                COMMENT '레시피 상세 조리과정',
    username    VARCHAR(50)     NULL                    COMMENT '작성자 아이디',
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='추천 레시피 정보';

-- 3. market_discount 테이블 (마트 할인 식재료 정보)
CREATE TABLE IF NOT EXISTS market_discount (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '고유 ID',
    market_name     VARCHAR(30)     NOT NULL                COMMENT '마트명 (EMART | LOTTEMART | HOMEPLUS)',
    product_name    VARCHAR(255)    NOT NULL                COMMENT '상품명',
    ingredient_name VARCHAR(100)    NOT NULL                COMMENT '정규화된 식재료명 (AI 매칭용)',
    original_price  INT             NULL                    COMMENT '정가 (원)',
    discount_price  INT             NULL                    COMMENT '할인가 (원)',
    discount_rate   DECIMAL(5,2)    NULL                    COMMENT '할인율 (%)',
    discount_period VARCHAR(100)    NULL                    COMMENT '할인 기간',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='대형마트 할인 식재료 정보 (RPA 크롤러 적재)';
