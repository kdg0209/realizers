# Soft Delete

#### Hard Delete를 하는 경우의 문제 상황

1. 복구 불가

    > 삭제된 데이터는 일반적으로 복구할 수 있는 방법이 없음

2. 법적 요구사항

    > 일정 기간 데이터 보관 의무 충족하기 어려움

3. 분석 불가

    > 삭제된 데이터는 분석이나 통계에 쓰일 수 없음
    
4. 감사 추적 불가

    > 언제, 누가 삭제했는지 추적 불가

5. 삭제 시점 불명확

    > 데이터가 언제 삭제되엇는지 알 수 없음

6. 연관 데이터 처리 어려움

    > 외래 키로 연결된 데이터 처리 복잡성 증가

<br>

## 1. Soft Delete란?

- `Soft Delete`는 데이터를 실제로 삭제하지 않고, 삭제되었다라는 표시만 해두는 방식

<br>

### 1-1. `is_deleted` 컬럼을 사용하는 방식

- 삭제가 되었으면 `is_deleted`의 데이터는 **true**로 변경

#### 예시

```sql
CREATE TABLE member (
    member_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

#### 한계

1. 삭제 시점을 알 수 없음

    > `deleted_at`과 같은 컬럼이 없기 때문에 삭제 시점을 알 수 없음

2. 일정 기간 후 영구 삭제 정책 적용 어려움

    > `deleted_at`과 같은 컬럼이 없기 때문에 삭제 시점을 알 수 없어서 정책 적용 어려움

3. 복구 이력을 관리할 수 없음

<br>

### 1-2. `deleted_at` 컬럼을 사용하는 방식

- 삭제가 되었으면 `deleted_at`의 데이터는 **해당 시점**으로 변경

#### 예시

```sql
CREATE TABLE member (
    member_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted_at DATETIME DEFAULT NULL
);
```

#### 장점

1. 일정 기간 후의 데이터를 영구 삭제 가능
2. 삭제 시점 명확
3. 특정 기간 탈퇴 현황 분석 가능

<br>

## 2. 실무에서의 선택 기준

### 2-1. Soft Delete를 사용하는 경우

1. 복구 요청이 자주 발생하는 경우
2. 법적 보관 의무 데이터가 있는 경우
3. 연관 데이터가 많은 핵심 데이터인 경우
4. 감사 추적이 필요한 데이터인 경우

<br>

### 2-2. Hard Delete를 사용하는 경우 

1. 로그성 데이터인 경우
2. 임시 데이터인 경우
3. 개인정보 완적 삭제 요청인 경우

<br>

### 2-3. 상태 기반 관리

- 비지니스 프로세스가 복잡하고, 데이터의 생명주기가 중요한 경우 사용됨
- 예시
  - 상품(판매중/판매중지), 주문(결제대기/완료/취소/환불)등
  - `status`컬럼 사용


