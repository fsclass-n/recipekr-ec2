-- ============================================================
-- 냉장고 파먹기 AI 레시피 추천 시스템 - 데이터베이스 스키마
-- ============================================================

-- 데이터베이스 생성 (없을 경우)
CREATE DATABASE IF NOT EXISTS recipekr
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE recipekr;

-- ============================================================
-- 1. users 테이블 (회원 정보)
-- ============================================================
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

-- ============================================================
-- 2. 인덱스 (검색 성능 최적화)
-- ============================================================
CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_created_at ON users (created_at);

-- ============================================================
-- 3. 테스트용 관리자 계정 삽입
--    password: Admin1234! (BCrypt 해시값)
-- ============================================================
INSERT INTO users (username, password, email, nickname, role)
VALUES (
    'admin',
    '$2a$10$N4jMhFR9XiJWwPVhp0hQzuP0E6GFNhJoJEf9yk9P2PKJB1NsBN/qO',
    'admin@recipekr.com',
    '관리자',
    'ADMIN'
) ON DUPLICATE KEY UPDATE updated_at = NOW();
