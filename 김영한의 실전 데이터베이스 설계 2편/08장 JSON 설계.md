# JSON 설계

#### 사용 방법

- 테이블에 `JSON` 타입의 컬럼을 만들면 됨
- 자세한 사용 방법은 문서 참고

```sql
CREATE TABLE product_json (
  product_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(200) NOT NULL,
  category VARCHAR(100) NOT NULL,
  attributes JSON, -- json 타입으로 설정
  created_at DATETIME NOT NULL
);
```

#### 실무 팁

- **JSON_TABLE**: JSON ARRAY로 저장되어 있는 데이터를 `JSON_TABLE` 함수를 사용하여 row 형식으로 볼 수 있음
- **Virtual Column**: JSON에서 자주 사용되는 컬럼을 `Virtual Column`을 사용하여 특정 컬럼으로 사용하고 인덱스 설정 가능

<br>

#### 💡 Virtual Column

- `Virtual`컬럼에 인덱스를 설정하면 그 인덱스 정보는 디스크에 물리적으로 저장됨
- 즉, `Virtual`컬럼에 인덱스를 설정하면 **B-Tree** 형태로 디스크에 정렬됨
- `Stored`를 사용하면 Json 데이터를 별도의 컬럼에 또 저장해야하므로 중복 저장이 발생함

<br>

#### 💡 함수 기반 인덱스

- `WHERE`절의 검색 조건으로만 사용되고, `SELECT`절에 사용되지 않는다면 추천

```sql
-- 가상 컬럼 생성 과정 없이 바로 인덱스 생성
CREATE INDEX idx_func_storage
ON product_json (( CAST(attributes->'$.storage' AS UNSIGNED) ));
```

<br>

#### 💡 Multi-Valued Index

- MySQL 8.0.17부터 도입
- `Virtual Column`은 값이 1:1 매핑이 되는 경우에는 유용
- JSON 배열안에 있는 값을 해야하는 경우에는 `Multi-Valued Index` 사용

```sql
CREATE INDEX idx_ports
ON product_json (( CAST(attributes->'$.ports' AS CHAR(20) ARRAY) ));
```

<br>

#### Json 컬럼 사용시 주의 사항

- 데이터 무결성 부재
- 대규모 분석 작업에서 Json 데이터를 기준으로 분석하는 경우 비용이 많이듬
- 거대한 Json 데이터를 애플리케이션으로 옮길 때 네트워크 비용과 CPU 사용률 급격히 증가될 수 있음

<br>

#### 참고

- https://dev.mysql.com/doc/refman/8.4/en/json.html
