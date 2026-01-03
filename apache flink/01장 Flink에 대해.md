## 1. Flink에 대해 

- [Apache Flink](https://flink.apache.org/)는 유한 및 무한 데이터 스트림에 대한 상태 유지 연산을 위한 프레임워크이자 분산 처리 엔진이라고 합니다.

<br>

### 1-1. Flink의 목표

- Flink의 목표는 대규모로 유입되는 스트림 데이터를 실시간으로 처리하면서 `세션 윈도우`(Session Window: 특정 시간동안의 사용자 활동)나 집계(Aggregation: 평균, 총합 등)와 같은 `상태 기반 연산`을 메모리 내에서 효율적으로 유지 및 관리하고 장애 상황에서도 정확한 결과를 보장하는 것입니다.

<br>


### 1-2. 데이터 스트림 종류

#### 1. 유한 데이터 스트림이란 무엇일까?

- 유한 데이터 스트림이란 입력 데이터가 명확한 끝을 가지는 스트림으로 전체 데이터셋이 한정되어 있는 데이터를 의미합니다.
  - 예시 데이터
    - S3에 저장된 하루치의 로그 파일
    - 데이터베이스에 저장된 하루치의 결제 데이터

#### 2. 무한 데이터 스트림이란 무엇일까?

- 무한 데이터 스트림이란 개념적으로 입력이 절대 끝나지 않을 수 있으므로 데이터가 도착하는 즉시 지속적으로 처리해야하며, 전체 데이터의 완료 시점을 알 수 없기 때문에 시간(window) 기반으로 연산 결과를 정의해야 하는 스트림을 의미합니다.
  - 예시 데이터
    - Kafka 토픽에서 실시간으로 들어오는 주문 데이터
    - 웹에서 끝없이 발생하는 클릭 이벤트

<br>

### 1-3. 좋은 스트림 시스템의 조건

#### 1. 입력 데이터가 어느 빈도로 발생하는지 예측할 수 없습니다. 즉, 발생하는 부하를 정확하게 예측하기 어렵습니다.

- 부하에 유동적으로 대응할 수 있어야 하며, 데이터 유입이 급증하더라도 성능이 저하되어서는 안됩니다.
- 급증한 데이터에 대해 스트림 프로세싱을 수행할 수 없다면 데이터는 계속 지연되기 때문에 실시간이라 할 수 없습니다.

#### 2. 데이터가 잘못된 순서로 유입되거나, 데이터가 발생한 시간(`Event Time`)과 시스템이 데이터를 인식한 시간(`Processing Time`)이 다를 수 있습니다.

- 데이터가 잘못된 순서로 유입되거나, 지연되어 들어온 경우에도 스트림 프로세싱을 할 수 있어야 하거나, 적어도 얼마나 지연되면 데이터를 버릴 것인지 기준을 제시할 수 있어야 합니다. 이러한 기준을 `워터 마크`라 부릅니다.

#### 3. 시스템은 내결함성을 가져야 합니다.

#### 4. 정확히 한 번 수행 보장

- 중복 없이, 누락 없이 정확한 상태와 결과를 유지해야 합니다.

<br>

### 1-4. Flink Layered APIs

#### 1. SQL API

- SQL API는 표준 SQL 구문을 제공하여 프레임워크에 대한 특별한 지식이 없는 사용자에게 높은 접근성을 제공해줍니다.

#### 2. Table API

- Table API는 프로그래밍 언어를 통해 SQL과 유사한 선언적 방식으로 사용할 수 있습니다.

#### 3. DataStream API

- DataStream API는 실시간 데이터 스트림 처리를 위한 명령형 인터페이스입니다.

#### 4. ProcessFunction API

- ProcessFunction API는 Flink에서 제공하는 가장 풍부한 인터페이스입니다.
- ProcessFunction API는 상태 저장 스트림 처리 또는 이벤트 시간 관리와 같은 세밀한 제어가 필요한 경우 사용할 수 있습니다.
- 순서가 뒤바뀐 이벤트 처리(`out-of-order`)와 같은 정교한 기능에 사용할 수 있습니다.

<br>

### 1-5. Flink의 사요 사례

- [Apache Flink Use-cases](https://flink.apache.org/what-is-flink/use-cases/)

#### 1. 이벤트 기반 애플리케이션

- https://nightlies.apache.org/flink/flink-docs-release-2.2/docs/learn-flink/event_driven/

<img width="1032" height="270" alt="스크린샷 2026-01-03 오후 2 28 04" src="https://github.com/user-attachments/assets/e70cf0da-4047-4362-b24f-5d43bf3a9fd3" />

<br>

#### 2. 데이터 분석 애플리케이션

- https://nightlies.apache.org/flink/flink-docs-release-2.2/docs/learn-flink/streaming_analytics/

<img width="1032" height="270" alt="스크린샷 2026-01-03 오후 2 29 34" src="https://github.com/user-attachments/assets/32cadd0d-ad49-4d86-aa09-f895b64e69a0" />

<br>

#### 3. 데이터 파이프라인 애플리케이션

- https://nightlies.apache.org/flink/flink-docs-release-2.2/docs/learn-flink/etl/

<img width="1032" height="270" alt="스크린샷 2026-01-03 오후 2 30 50" src="https://github.com/user-attachments/assets/5851bff5-6438-47fa-89ef-fbb4427ec022" />

<br>
<br>

#### 참고

- https://sungjk.github.io/2024/09/18/apache-flink.html
- https://medium.com/@BitrockIT/apache-flink-api-levels-of-abstraction-32b32d38292b
- https://flink.apache.org/what-is-flink/flink-applications/#layered-apis
- https://medium.com/rate-labs/%EC%8A%A4%ED%8A%B8%EB%A6%BC-%ED%94%84%EB%A1%9C%EC%84%B8%EC%8B%B1%EC%9D%98-%EA%B8%B4-%EC%97%AC%EC%A0%95%EC%9D%84-%EC%9C%84%ED%95%9C-%EC%9D%B4%EC%A0%95%ED%91%9C-with-flink-8e3953f97986



