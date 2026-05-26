CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    nickname VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS recipes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    ingredients TEXT NOT NULL,
    calories INT NOT NULL,
    health_type VARCHAR(50),
    recipe_text TEXT NOT NULL,
    username VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS market_discount (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    market_name VARCHAR(30) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    ingredient_name VARCHAR(100) NOT NULL,
    original_price INT,
    discount_price INT,
    discount_rate DECIMAL(5,2),
    discount_period VARCHAR(100),
    image_url VARCHAR(500),
    product_url VARCHAR(500),
    crawled_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (market_name, product_name, crawled_date)
);

CREATE INDEX IF NOT EXISTS idx_market_ingredient ON market_discount (ingredient_name);
CREATE INDEX IF NOT EXISTS idx_market_crawled_date ON market_discount (crawled_date);
CREATE INDEX IF NOT EXISTS idx_market_name ON market_discount (market_name);
