# EAV 패턴 설계

#### 전통적인 테이블 설계

- 하나의 테이블에 관련된 속성의 컬럼들이 정의되어 있음
- 새로운 속성이 생기면 `ALTER TABLE`을 수행해야 함

```sql
CREATE TABLE product_phone (
  product_id BIGINT PRIMARY KEY,
  name VARCHAR(200),
  screen_size VARCHAR(20),
  battery_capacity VARCHAR(20),
  camera_pixels VARCHAR(20),
  ram VARCHAR(20),
  storage VARCHAR(20)
);
```

<br>

#### EAV 설계

- 속성 이름과 값을 컬럼이 아니라 행에 저장함

```sql
CREATE TABLE product (
  product_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(200) NOT NULL,
  created_at DATETIME NOT NULL
);

-- Attribute-Value: 상품의 속성과 값
CREATE TABLE product_attribute (
  attribute_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  attr_name VARCHAR(100) NOT NULL, -- 속성 이름
  attr_value VARCHAR(500), -- 속성 값
  FOREIGN KEY (product_id) REFERENCES product(product_id)
);
```

<br>

#### EAV 요소

- **Entity**: 속성을 가지는 주체(상품, 주문 등)
- **Attribute**: 엔티티가 가지는 속성(사이즈, 색상 등)
- **Value**: 해당 속성을 실제 값

<br>

#### EAV의 특징

- 새로운 속성 추가가 자유로움
- 엔티티마다 다른 속성 정의 가능

<br>

#### EAV 패턴 개선

- 속성을 행에 저장하다보니 속성 명칭에 오타가 발생할 수 있음
- product_attribute 테이블에 속성이 정의되어 있지 않고, attribute_definition 테이블에 정의되어 있음

```sql
CREATE TABLE attribute_definition (
  attr_def_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category_id BIGINT,
  attr_name VARCHAR(100) NOT NULL,
  attr_label VARCHAR(100) NOT NULL,
  attr_type VARCHAR(20) NOT NULL,  -- TEXT, NUMBER, BOOLEAN, SELECT
  is_required BOOLEAN DEFAULT FALSE,
  is_searchable BOOLEAN DEFAULT FALSE,
  display_order INT DEFAULT 0,
  options VARCHAR(500),        -- SELECT 타입일 때 선택 옵션
  created_at DATETIME NOT NULL,
  FOREIGN KEY (category_id) REFERENCES category(category_id)
);

-- 상품 속성 테이블 (개선)
CREATE TABLE product_attribute (
  attribute_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  attr_def_id BIGINT NOT NULL, -- attribute_definition 테이블의 PK
  attr_value VARCHAR(500),
  created_at DATETIME NOT NULL,
  FOREIGN KEY (product_id) REFERENCES product(product_id),
  FOREIGN KEY (attr_def_id) REFERENCES attribute_definition(attr_def_id)
);

CREATE INDEX idx_product_attribute_product ON product_attribute(product_id);
CREATE INDEX idx_product_attribute_def ON product_attribute(attr_def_id);

-- 스마트폰 카테고리 속성 정의
INSERT INTO attribute_definition
(category_id, attr_name, attr_label, attr_type, is_required, is_searchable, display_order, options, created_at) VALUES
(2, 'screen_size', '화면 크기', 'TEXT', TRUE, TRUE, 1, NULL, NOW()),
(2, 'battery_capacity', '배터리 용량', 'TEXT', TRUE, TRUE, 2, NULL, NOW()),
(2, 'ram', 'RAM', 'SELECT', TRUE, TRUE, 3, '4GB,6GB,8GB,12GB,16GB', NOW()),
(2, 'storage', '저장 용량', 'SELECT', TRUE, TRUE, 4, '64GB,128GB,256GB,512GB,1TB', NOW()),
(2, 'processor', '프로세서', 'TEXT', TRUE, FALSE, 5, NULL, NOW()),
(2, 'main_camera', '메인 카메라', 'TEXT', FALSE, TRUE, 6, NULL, NOW()),
(2, 'color', '색상', 'TEXT', TRUE, TRUE, 7, NULL, NOW()),
(2, 'water_resistance', '방수 등급', 'SELECT', FALSE, TRUE, 8, 'IP67,IP68', NOW()),
(2, '5g_support', '5G 지원', 'BOOLEAN', FALSE, TRUE, 9, NULL, NOW());

-- 의류 카테고리 속성 정의
INSERT INTO attribute_definition
(category_id, attr_name, attr_label, attr_type, is_required, is_searchable, display_order, options, created_at) VALUES
(5, 'material', '소재', 'TEXT', TRUE, TRUE, 1, NULL, NOW()),
(5, 'size', '사이즈', 'SELECT', TRUE, TRUE, 2, 'XS,S,M,L,XL,XXL', NOW()),
(5, 'color', '색상', 'TEXT', TRUE, TRUE, 3, NULL, NOW()),
(5, 'origin', '원산지', 'TEXT', FALSE, FALSE, 4, NULL, NOW()),
(5, 'washing', '세탁 방법', 'TEXT', FALSE, FALSE, 5, NULL, NOW()),
(5, 'season', '시즌', 'SELECT', FALSE, TRUE, 6, 'S/S,F/W,사계절', NOW());
```

<br>

#### EAV 사용 시 주의사항

#### 장점

1. 스키마 유연성

    > 테이블 구조 변경 없이 새로운 속성 추가 가능

2. 엔티티별 다른 속성 지원

    > 같은 카테고리라도 상품마다 다른 속성을 가질 수 있음

3. 런타임 속성 지원

    > 관리자 화면에서 새로운 속성 정의 후 바로 사용 가능

#### 단점

1. 쿼리 복잡성

    > 여러 속성을 조회하거나 조건으로 검색하면 복잡한 조인이나 서브쿼리 발생

2. 데이터 타입 제약 불가

    > 모든 값이 문자열로 저장됨

3. 참조 무결성 제약 어려움

    > 외래 키 제약조건 설정 어려움

4. 인덱싱 제한

    > 특정 속성에 대한 효율적인 인덱스 생성 어려움

5. 조인 성능 저하

    > 속성이 많아질수록 쿼리나 다중 조인으로 인해 성능 저하 발생 

<br>

#### 정리

- EAV 설계는 최후의 수단이여야 한다.
  - Json 타입 고려


