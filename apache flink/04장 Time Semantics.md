# 4. Time Semantics

<br>

## 1. Time Concepts

### 1-1. Processing time

#### 정의 

- `Processing time`은 연산자가 이벤트를 처리하는 시점의 시간을 의미
- 이벤트가 `Task Manager`에 도착해 처리되는 순간의 시스템 시간
- 이벤트에 타임스탬프가 있더라도 시스템 시간(System.currentTimeMillis)을 사용함

#### 특징

- 타임스탬프 추출이나 Watermark 설정이 필요없기 때문에 가장 단순함
- **결과의 재현성**이 낮음
  - 같은 데이터를 다시 돌려도 시스템 시간에 따라 결과가 달라질 수 있음

#### 용도

- 정확성처리 보다 레이턴시가 낮은 시스템(ex: 모니터링)

<br>

### 1-2. Event Time

#### 정의

- `Event Time`은 이벤트가 실제로 발생한 그 순간의 시간을 의미
- `Event Time`을 사용하기 위해서는 이벤트에 타임스탬프를 포함시켜야함
- 타임스탬프는 이벤트 자체에 포함되어 있기 때문에 Flink에서는 이를 추출하여 사용

#### 특징

- **Out-of-order**, 네트워크 지연, 재처리와 같은 문제를 다루기 위함
- `Event Time`을 사용한다고 해서 **Out-of-order** 문제를 해결하는게 아니라 `Watermark`를 함께 사용하여 문제를 해결하는 것
- 실무에서 기본적으로 많이 사용한다고 함

<br>

### 1-3. Ingestion Time

- `Ingestion Time`은 이벤트가 Flink의 **Source 연산자**에 도착한 시간을 의미
- 개념적으로 **Processing time**와 **Event Time**의 중간 개념
- `Source 연산자`가 자동으로 타임스탬프 할당

#### 특징

- **Out-of-order**, 네트워크 지연의 문제를 해결할 수 없음
- Flink 1.12 이후 Ingestion Time 설정이 deprecated

<br>

### 1-4. 비교

#### 이벤트 발생 예시

- 이벤트가 실제 발생: 10:00
- 네트워크 지연으로 Flink 도착: 10:05
- 태스크가 바빠서 처리 시작: 10:07

#### Time별 기준

- Event Time = 10:00 (발생 기준)
- Ingestion Time = 10:05 (Flink 도착 기준)
- Processing time = 10:07 (실제 처리 기준)

<br>

## 2. Event Time과 Watermark

- **Event Time**이 실무에서 가장 많이 쓰이니 Event Time과 Watermark에 대해 살펴봄

### 2-1. Event Time만으로 모든걸 해결할 수 없다

- Event Time은 타임스탬프만 가지고 있기 때문에 `Timestamp Assigner + Watermark Strategy`가 함께 있어야 함
- 이유
  - 배치와 달리 스트리밍에서는 끝을 알 수 없기 때문에 타임스탬프만으로 끝을 판단하기 어려움
  - **Out-of-order**가 발생하므로 타임스탬프만 보고 윈도우를 닫아야할지 기다려야할지 판단 불가

<br>

### 2-2. Watermark와 관계

- `Watermark`는 이 시점 이전의 이벤트는 더 이상 오지 않을 것이라고 가정

#### 💡 [Timestamp Assigner의 역할](https://nightlies.apache.org/flink/flink-docs-release-2.2/docs/dev/datastream/event-time/generating_watermarks/)

- 이벤트 객체에서 **Event Time** 기준 타임스탬프 추출
- Flink는 이벤트의 어떤 필드가 타임스탬프인지 모르기 때문에 명시해야함
- `Timestamp Assigner`가 없으면 **Event Time**을 계산할 수 없고, **Window/Watermark/Late** 판단 전부 불가

```java
DataStream<Event> eventsWithWatermark = env
    .fromSource(
        source,
        WatermarkStrategy
            .<Event>forBoundedOutOfOrderness(Duration.ofSeconds(10)) // 이벤트가 최대 10초까지 늦게올 수 있다라고 명시
            .withTimestampAssigner((event, timestamp) -> event.getCreatedAt()), // 이벤트에서 createdAt필드가 타임스탬프라고 명시
        "Source"
    )
    .keyBy(event -> event.getSensorId())
    .window(TumblingEventTimeWindows.of(Time.minutes(1)))
    .allowedLateness(Time.seconds(30))   // 30초까지 늦은 데이터 허용
    .sideOutputLateData(lateDataTag)     // 더 늦은 건 side output으로 분리
    .sum("temperature")
    .print();
```

<br>

#### 💡 [Watermark Strategy의 역할](https://github.com/kdg0209/realizers/blob/main/apache%20flink/03%EC%9E%A5%20Watermark.md#3-watermark-strategy)

- `Event Time`이 얼마나 늦게까지 올 수 있는지 허용하는 것
- `Watermark Strategy`가 없다면 **Watermark**를 생성하지 않기 때문에 **Event Time**의 집행 기준이 없어지고, Flink는 **Event Time**를 신뢰할 수 없다고 판단하여 윈도우 종료나 트리거를 발동시키지 않음 그 결과 **Event Time**기반 연산에서 출력이 발생하지 않음

#### 🚗 과정

```txt
Source
  ↓
Timestamp Assigner (언제 발생했는지)
  ↓
Watermark Strategy (어디까지 왔다고 볼지)
  ↓
Window / Trigger
  ↓
Result
```

<br>

#### 참고

- https://nightlies.apache.org/flink/flink-docs-release-2.2/docs/concepts/time/


