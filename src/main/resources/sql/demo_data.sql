INSERT INTO users (username, password, email, nickname, role)
VALUES
    ('demo', '$2a$10$N4jMhFR9XiJWwPVhp0hQzuP0E6GFNhJoJEf9yk9P2PKJB1NsBN/qO', 'demo@recipekr.local', 'Demo User', 'USER'),
    ('test', '$2a$10$2qz22fvERlwiT8Ygnwi1A.LN0DpwAUQJ98vf5LJf5X21.OmY10m6y', 'test@recipekr.local', 'Test User', 'ADMIN'),
    ('admin', '$2a$10$N4jMhFR9XiJWwPVhp0hQzuP0E6GFNhJoJEf9yk9P2PKJB1NsBN/qO', 'admin@recipekr.local', 'Admin', 'ADMIN');

INSERT INTO recipes (title, ingredients, calories, health_type, recipe_text, username)
VALUES
    ('Demo Tofu Rice Bowl', 'tofu, egg, green onion, rice', 520, 'balanced', '1. Sear tofu. 2. Add egg and green onion. 3. Serve over warm rice.', 'demo'),
    ('Demo Tomato Pasta', 'tomato, garlic, onion, pasta', 610, 'general', '1. Saute garlic and onion. 2. Add tomato sauce. 3. Toss with pasta.', 'demo'),
    ('Demo Chicken Salad', 'chicken breast, lettuce, cucumber', 430, 'diet', '1. Grill chicken. 2. Slice vegetables. 3. Mix with light dressing.', 'demo');

INSERT INTO market_discount (
    market_name, product_name, ingredient_name,
    original_price, discount_price, discount_rate,
    discount_period, image_url, product_url, crawled_date
)
VALUES
    ('EMART', 'Demo Fresh Onion', 'onion', 3200, 2400, 25.00, 'demo today', '', '', CURRENT_DATE),
    ('EMART', 'Demo Garlic Pack', 'garlic', 5500, 3900, 29.09, 'demo today', '', '', CURRENT_DATE),
    ('LOTTEMART', 'Demo Tomato Box', 'tomato', 7900, 5900, 25.32, 'demo today', '', '', CURRENT_DATE),
    ('LOTTEMART', 'Demo Pasta Noodles', 'pasta', 4200, 3200, 23.81, 'demo today', '', '', CURRENT_DATE),
    ('HOMEPLUS', 'Demo Tofu', 'tofu', 2800, 1900, 32.14, 'demo today', '', '', CURRENT_DATE),
    ('HOMEPLUS', 'Demo Chicken Breast', 'chicken', 9800, 7600, 22.45, 'demo today', '', '', CURRENT_DATE),
    ('EMART', 'Demo Egg Pack', 'egg', 6900, 5400, 21.74, 'demo today', '', '', CURRENT_DATE),
    ('LOTTEMART', 'Demo Lettuce', 'lettuce', 3600, 2600, 27.78, 'demo today', '', '', CURRENT_DATE),
    ('HOMEPLUS', 'Demo Cucumber', 'cucumber', 3100, 2300, 25.81, 'demo today', '', '', CURRENT_DATE);
