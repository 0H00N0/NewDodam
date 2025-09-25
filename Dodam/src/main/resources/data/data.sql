-- Dodam Pay 상품 도메인 초기 데이터
-- 개발 및 테스트용 샘플 데이터

-- 카테고리 초기 데이터 (계층형 구조)
INSERT INTO CATEGORY (category_name, parent_category_id, category_path, display_order, is_active) VALUES
-- 대분류
('전자제품', NULL, '/전자제품/', 1, true),
('의류', NULL, '/의류/', 2, true),
('식품', NULL, '/식품/', 3, true),
('도서', NULL, '/도서/', 4, true),
('생활용품', NULL, '/생활용품/', 5, true),

-- 중분류 (전자제품)
('스마트폰', 1, '/전자제품/스마트폰/', 1, true),
('노트북', 1, '/전자제품/노트북/', 2, true),
('태블릿', 1, '/전자제품/태블릿/', 3, true),
('웨어러블', 1, '/전자제품/웨어러블/', 4, true),

-- 중분류 (의류)
('상의', 2, '/의류/상의/', 1, true),
('하의', 2, '/의류/하의/', 2, true),
('아우터', 2, '/의류/아우터/', 3, true),
('신발', 2, '/의류/신발/', 4, true);

-- 브랜드 초기 데이터
INSERT INTO BRAND (brand_name, brand_logo_url, description, is_active) VALUES
('삼성', 'https://logo.samsung.com/logo.png', '글로벌 전자기기 브랜드', true),
('애플', 'https://logo.apple.com/logo.png', '혁신적인 기술 브랜드', true),
('LG', 'https://logo.lg.com/logo.png', '생활가전 및 전자기기 브랜드', true),
('나이키', 'https://logo.nike.com/logo.png', '글로벌 스포츠 브랜드', true),
('아디다스', 'https://logo.adidas.com/logo.png', '스포츠웨어 브랜드', true),
('유니클로', 'https://logo.uniqlo.com/logo.png', '캐주얼 의류 브랜드', true),
('도담', 'https://logo.dodam.com/logo.png', '도담페이 자체 브랜드', true);

-- 상품 초기 데이터
INSERT INTO PRODUCT (product_name, category_id, brand_id, price, image_url, status, created_at, updated_at) VALUES
-- 스마트폰
('갤럭시 S24 Ultra', 6, 1, 1398000.00, 'https://images.samsung.com/galaxy-s24-ultra.jpg', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('아이폰 15 Pro', 6, 2, 1550000.00, 'https://images.apple.com/iphone-15-pro.jpg', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('갤럭시 Z 플립5', 6, 1, 1247000.00, 'https://images.samsung.com/galaxy-z-flip5.jpg', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- 노트북
('맥북 프로 14인치', 7, 2, 2690000.00, 'https://images.apple.com/macbook-pro-14.jpg', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('LG 그램 17', 7, 3, 1890000.00, 'https://images.lg.com/gram-17.jpg', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- 의류
('에어 조던 1', 13, 4, 169000.00, 'https://images.nike.com/air-jordan-1.jpg', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('스탠 스미스', 13, 5, 109000.00, 'https://images.adidas.com/stan-smith.jpg', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('히트텍 크루넥 긴소매 티셔츠', 10, 6, 19900.00, 'https://images.uniqlo.com/heattech-tshirt.jpg', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- 테스트용 비활성 상품
('테스트 상품 (비활성)', 6, 7, 50000.00, 'https://images.dodam.com/test-product.jpg', 'INACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 상품 상세 정보
INSERT INTO PRODUCT_DETAIL (product_id, description, specifications, features, care_instructions, warranty_info, origin_country) VALUES
(1, '차세대 AI 기능이 탑재된 프리미엄 스마트폰', '6.8인치 Dynamic AMOLED 2X, Snapdragon 8 Gen 3, 12GB RAM, 256GB 저장공간', 'S펜 지원, 200MP 카메라, 5000mAh 배터리', '부드러운 천으로 청소, 방수 기능 있음', '2년 품질보증', '한국'),
(2, '프로페셔널을 위한 혁신적인 스마트폰', '6.1인치 Super Retina XDR, A17 Pro 칩, 8GB RAM, 128GB 저장공간', 'ProRAW 지원, 액션 버튼, 티타늄 디자인', '마이크로파이버 천으로 청소', '1년 제한보증', '중국'),
(3, '접을 수 있는 혁신적인 스마트폰', '6.7인치 Dynamic AMOLED 2X (펼침 시), Snapdragon 8 Gen 2, 8GB RAM', '플렉스 모드, 커버 스크린, 컴팩트 디자인', '접힘부 주의, 부드럽게 사용', '2년 품질보증', '한국'),
(4, '프로를 위한 강력한 성능의 노트북', '14.2인치 Liquid Retina XDR, M3 Pro 칩, 18GB 통합 메모리', 'Thunderbolt 4 포트, Magic Keyboard, Force Touch 트랙패드', '키보드 청소 시 주의', '1년 제한보증', '중국'),
(5, '초경량 프리미엄 노트북', '17인치 WQXGA IPS, Intel Core i7, 16GB LPDDR5, 512GB SSD', '1.35kg 초경량, MIL-STD 810G 인증, 80Wh 배터리', '알코올 성분 청소제 사용 금지', '2년 품질보증', '한국'),
(6, '아이코닉한 농구화의 전설', '사이즈: 230-300mm, 소재: 천연가죽+합성소재', '에어쿠션, 고무 아웃솔, 클래식 디자인', '직사광선 피해 보관, 물세탁 금지', '6개월 품질보증', '베트남'),
(7, '클래식한 테니스화', '사이즈: 230-300mm, 소재: 천연가죽', '전설적인 디자인, 편안한 착화감', '가죽 전용 클리너 사용', '6개월 품질보증', '베트남'),
(8, '혁신적인 발열 기능성 티셔츠', '사이즈: XS-XXL, 소재: 아크릴 39%, 레이온 35%, 폴리에스테르 20%', '히트텍 기술, 보온성, 습기 흡수', '세탁기 사용 가능, 다림질 낮은 온도', '1년 품질보증', '베트남');

-- 상품 옵션 데이터
INSERT INTO PRODUCT_OPTION (product_id, option_type, option_name, option_value, additional_price, stock_quantity, display_order, is_available) VALUES
-- 갤럭시 S24 Ultra 옵션
(1, 'COLOR', '색상', '티타늄 그레이', 0.00, 50, 1, true),
(1, 'COLOR', '색상', '티타늄 블랙', 0.00, 30, 2, true),
(1, 'COLOR', '색상', '티타늄 바이올렛', 0.00, 20, 3, true),
(1, 'CAPACITY', '저장용량', '256GB', 0.00, 60, 1, true),
(1, 'CAPACITY', '저장용량', '512GB', 200000.00, 40, 2, true),
(1, 'CAPACITY', '저장용량', '1TB', 400000.00, 20, 3, true),

-- 아이폰 15 Pro 옵션
(2, 'COLOR', '색상', '내추럴 티타늄', 0.00, 40, 1, true),
(2, 'COLOR', '색상', '블루 티타늄', 0.00, 35, 2, true),
(2, 'COLOR', '색상', '화이트 티타늄', 0.00, 25, 3, true),
(2, 'COLOR', '색상', '블랙 티타늄', 0.00, 30, 4, true),
(2, 'CAPACITY', '저장용량', '128GB', 0.00, 50, 1, true),
(2, 'CAPACITY', '저장용량', '256GB', 150000.00, 40, 2, true),
(2, 'CAPACITY', '저장용량', '512GB', 350000.00, 30, 3, true),
(2, 'CAPACITY', '저장용량', '1TB', 550000.00, 15, 4, true),

-- 에어 조던 1 옵션
(6, 'SIZE', '사이즈', '250mm', 0.00, 10, 1, true),
(6, 'SIZE', '사이즈', '255mm', 0.00, 15, 2, true),
(6, 'SIZE', '사이즈', '260mm', 0.00, 20, 3, true),
(6, 'SIZE', '사이즈', '265mm', 0.00, 25, 4, true),
(6, 'SIZE', '사이즈', '270mm', 0.00, 30, 5, true),
(6, 'SIZE', '사이즈', '275mm', 0.00, 25, 6, true),
(6, 'SIZE', '사이즈', '280mm', 0.00, 15, 7, true),

-- 히트텍 티셔츠 옵션
(8, 'SIZE', '사이즈', 'XS', 0.00, 20, 1, true),
(8, 'SIZE', '사이즈', 'S', 0.00, 50, 2, true),
(8, 'SIZE', '사이즈', 'M', 0.00, 80, 3, true),
(8, 'SIZE', '사이즈', 'L', 0.00, 70, 4, true),
(8, 'SIZE', '사이즈', 'XL', 0.00, 40, 5, true),
(8, 'SIZE', '사이즈', 'XXL', 0.00, 20, 6, true),
(8, 'COLOR', '색상', '블랙', 0.00, 60, 1, true),
(8, 'COLOR', '색상', '화이트', 0.00, 50, 2, true),
(8, 'COLOR', '색상', '네이비', 0.00, 40, 3, true),
(8, 'COLOR', '색상', '그레이', 0.00, 35, 4, true);

-- 상품 이미지 데이터
INSERT INTO PRODUCT_IMAGE (product_id, image_type, image_url, alt_text, image_order, is_active) VALUES
-- 갤럭시 S24 Ultra 이미지
(1, 'THUMBNAIL', 'https://images.samsung.com/galaxy-s24-ultra-thumb.jpg', '갤럭시 S24 울트라 썸네일', 1, true),
(1, 'DETAIL', 'https://images.samsung.com/galaxy-s24-ultra-front.jpg', '갤럭시 S24 울트라 정면', 2, true),
(1, 'DETAIL', 'https://images.samsung.com/galaxy-s24-ultra-back.jpg', '갤럭시 S24 울트라 후면', 3, true),
(1, 'GALLERY', 'https://images.samsung.com/galaxy-s24-ultra-spen.jpg', '갤럭시 S24 울트라 S펜', 4, true),

-- 아이폰 15 Pro 이미지
(2, 'THUMBNAIL', 'https://images.apple.com/iphone-15-pro-thumb.jpg', '아이폰 15 프로 썸네일', 1, true),
(2, 'DETAIL', 'https://images.apple.com/iphone-15-pro-front.jpg', '아이폰 15 프로 정면', 2, true),
(2, 'DETAIL', 'https://images.apple.com/iphone-15-pro-back.jpg', '아이폰 15 프로 후면', 3, true),

-- 에어 조던 1 이미지
(6, 'THUMBNAIL', 'https://images.nike.com/air-jordan-1-thumb.jpg', '에어 조던 1 썸네일', 1, true),
(6, 'DETAIL', 'https://images.nike.com/air-jordan-1-side.jpg', '에어 조던 1 측면', 2, true),
(6, 'DETAIL', 'https://images.nike.com/air-jordan-1-sole.jpg', '에어 조던 1 밑창', 3, true);

-- 재고 정보 초기 데이터
INSERT INTO INVENTORY (product_id, quantity, reserved_quantity, available_quantity, min_stock_level, last_restocked_at, updated_at) VALUES
(1, 100, 5, 95, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 80, 8, 72, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 60, 3, 57, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 40, 2, 38, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 50, 0, 50, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 200, 15, 185, 20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 150, 10, 140, 15, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, 500, 25, 475, 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9, 0, 0, 0, 0, NULL, CURRENT_TIMESTAMP);  -- 비활성 상품은 재고 0