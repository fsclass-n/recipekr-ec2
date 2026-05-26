USE recipekrrds;

-- 1. 사용자(users) 테이블
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT          NOT NULL AUTO_INCREMENT COMMENT '고유 ID',
    username    VARCHAR(50)     NOT NULL                COMMENT '로그인 아이디',
    password    VARCHAR(255)    NOT NULL                COMMENT '비밀번호 (BCrypt)',
    email       VARCHAR(100)    NOT NULL                COMMENT '이메일 주소',
    nickname    VARCHAR(50)     NOT NULL                COMMENT '화면 표시 닉네임',
    role        VARCHAR(20)     NOT NULL DEFAULT 'USER' COMMENT '권한',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '가입일시',
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_created_at ON users (created_at);

-- 관리자 초기 계정 생성 (비밀번호: Admin1234!)
INSERT INTO users (username, password, email, nickname, role)
VALUES ('admin', '$2a$10$N4jMhFR9XiJWwPVhp0hQzuP0E6GFNhJoJEf9yk9P2PKJB1NsBN/qO', 'admin@recipekr.com', '관리자', 'ADMIN') 
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 2. 레시피(recipes) 테이블
CREATE TABLE IF NOT EXISTS recipes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    ingredients TEXT NOT NULL,
    calories INT NOT NULL,
    health_type VARCHAR(50),
    recipe_text TEXT NOT NULL,
    username VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 마트 할인(market_discount) 테이블
CREATE TABLE IF NOT EXISTS market_discount (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '고유 ID',
    market_name     VARCHAR(30)     NOT NULL                COMMENT '마트명',
    product_name    VARCHAR(255)    NOT NULL                COMMENT '상품명',
    ingredient_name VARCHAR(100)    NOT NULL                COMMENT '식재료명',
    original_price  INT             NULL                    COMMENT '원가',
    discount_price  INT             NULL                    COMMENT '할인가',
    discount_rate   DECIMAL(5,2)    NULL                    COMMENT '할인율(%)',
    discount_period VARCHAR(100)    NULL                    COMMENT '할인 기간',
    image_url       VARCHAR(500)    NULL                    COMMENT '이미지 URL',
    product_url     VARCHAR(500)    NULL                    COMMENT '상품 상세 URL',
    crawled_date    DATE            NOT NULL                COMMENT '크롤링 날짜',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '최초 등록일시',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '최종 수정일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_market_product_date (market_name, product_name, crawled_date),
    INDEX idx_market_ingredient (ingredient_name),
    INDEX idx_market_crawled_date (crawled_date),
    INDEX idx_market_name (market_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
