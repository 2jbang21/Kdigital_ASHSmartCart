-- SmartCart 데이터베이스 설정
CREATE DATABASE IF NOT EXISTS smart_cart_db;
USE smart_cart_db;

-- 상품 테이블
CREATE TABLE IF NOT EXISTS product (
    product_id INT PRIMARY KEY AUTO_INCREMENT,
    product_name VARCHAR(255) NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    category VARCHAR(100) DEFAULT '기타',
    barcode VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 결제 테이블 (수정된 구조)
CREATE TABLE IF NOT EXISTS payment (
    payment_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(100) NOT NULL,
    gender CHAR(1) DEFAULT 'M',
    age INT DEFAULT 25,
    product_id VARCHAR(50) NOT NULL,
    quantity INT DEFAULT 1,
    unit_price DECIMAL(10,2) DEFAULT 0,
    total_price DECIMAL(10,2) DEFAULT 0,
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 샘플 상품 데이터 삽입
INSERT IGNORE INTO product (product_id, product_name, unit_price, category, barcode) VALUES
(1, '신선한 사과', 3000, '식품', '1'),
(2, '유기농 바나나', 2500, '식품', '2'),
(3, '프리미엄 우유', 2800, '식품', '3'),
(4, '수제 식빵', 4000, '식품', '4'),
(5, '천연 치즈', 5500, '식품', '5'),
(6, '할인 요거트', 1500, '식품', '6'),
(7, '생수 2L', 800, '식품', '7'),
(8, '오렌지 주스', 1800, '식품', '8'),
(9, '초콜릿 과자', 1200, '식품', '9'),
(10, '컵라면', 3800, '식품', '10'),
(11, '세제', 5000, '생활용품', '11'),
(12, '화장지', 3000, '생활용품', '12'),
(13, '스마트폰', 800000, '전자제품', '13'),
(14, '이어폰', 150000, '전자제품', '14');

-- 결제 테이블 확인 쿼리
SELECT * FROM payment ORDER BY payment_date DESC LIMIT 10;