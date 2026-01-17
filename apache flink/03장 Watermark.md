# 3. Watermark

<br>

## 1. 워터마크란 무엇인가?

- `Watermark`는 데이터 스트림에 포함되어 흐르는 **특수한 이벤트**
- 시스템에 타임스탬프가 T인 워터마크가 도착했다면, 이제 이 스트림에서 타임스탬프가 T보다 작거나 같은 이벤트는 모두 도착했다라고 가정
- 특정 시간 범위의 데이터를 묶어 처리하는 윈도우 연산을 언제 마감하고 결과를 출력할지 결정하기 위해 사용됨

<br>

## 2. 왜 필요할까?

- `Watermark`는 이벤트가 순서대로 도착하지 않는 스트리밍 환경에서 `Out-Of-Order` 해결하기 위해 필요
- `Watermark`는 **2026-01-11T14:00:00.000**와 같은 시간 정보만을 담고 있는데, `Watermark`가 Data Stream을 흐르면서 Data Stream을 구성하는 모든 Task에게 중요한 정보 전달
  1. `Watermark`에 명시된 이전 시간의 이벤트들은 모두 처리됨
  2. `Watermark`에 명시된 시간보다 과거의 이벤트는 더이상 Data Stream으로 유입되지 않음
  3. 지금부터는 `Watermark`에 명시된 시간보다 미래의 이벤트만 유입될 것임

<br>

## 3. Watermark Strategy

- 어떠한 방식으로 `Watermark`를 생성할건지 결정하는 전략

### 3-1. forMonotonousTimestamps

- 이벤트의 타임스탬프가 단조 증가한다고 가정하는 전략.
- 즉, Event Time 기준으로 절대 과거로 되돌아가지 않는 스트림을 전제로 함
- 각 이벤트의 타임스탬프를 `Watermark`의 타임스탬프로 사용

#### 특징

- `Out-Of-Order`가 없다고 가정
- Watermark 지연 없음
- Window 결과가 매우 빠르게 출력됨
- 순서가 한번이라도 깨지면 Late Event 발생

<br>

### 3-2. forBoundedOutOfOrderness

- 이벤트가 최대 N 시간만큼 지연되어 도착할 수 있다고 가정하는 전략
- 현재까지 발생한 이벤트의 타임스탬프 중 가장 최신의 이벤트 타임스탬프의 값에서 N만큼 뺀 값의 타임스탬프를 `Watermark`로 설정하겠다는 의미

```txt
Watermark = maxTimestamp - maxOutOfOrderness
```

#### 특징

- 제한된 `Out-Of-Order` 허용
- 권장하는 전략 방식
- **maxOutOfOrderness** 설정 값이 크면 결과 지연, 값이 작으면 Late Event 증가될 수 있음

<br>

## 4. Window Strategy

- `Window Strategy`는 Event Time기반 스트림에서 어떤 범위의 데이터를 묶고, 언제 결과를 확정지을지 결정하는 전략
- `Event Time Window`는 watermark ≥ window_end 시점에 on-time firing 된다
- 스트리밍 데이터에는 끝이 없기 때문에 언제까지를 하나의 계산 단위로 볼지, 언제 결과를 내고 확정할지 필요한데 이때 사용됨
- [참고 사이트](https://medium.com/rate-labs/%EC%8A%A4%ED%8A%B8%EB%A6%BC-%ED%94%84%EB%A1%9C%EC%84%B8%EC%8B%B1%EC%9D%98-%EA%B8%B4-%EC%97%AC%EC%A0%95%EC%9D%84-%EC%9C%84%ED%95%9C-%EC%9D%B4%EC%A0%95%ED%91%9C-with-flink-8e3953f97986)

### 4-1. Window Strategy의 구성 요소

#### 1. Window Assigner

- 이벤트를 어떤 `Window`에 넣을지 결정
- 기본적으로 텀블링 윈도우, 슬라이딩 윈도우, 세션 윈도우, 글로벌 윈도우가 존재

#### 💡 [텀블링 윈도우](https://nightlies.apache.org/flink/flink-docs-master/docs/dev/datastream/operators/windows/#tumbling-windows)

- 고정 크기 윈도우라고도 불리며 일정한 크기로 윈도우를 할당함
- 크기가 고정되어 있으며, 서로 겹치지 않음
- 데이터 개수 또는 시간이 기준이 될 수 있음
- 예를들어 텀블링 윈도우를 시간 기반으로 5분을 지정하면 아래 그림과 같이 5분마다 현재 윈도우가 평가되고 새로운 윈도우가 시작됨

<img width="1032" height="409" alt="스크린샷 2026-01-11 오후 4 07 39" src="https://github.com/user-attachments/assets/a0a18bbe-2584-4f33-8335-3ccb115c861f" />

<br><br>

#### 💡 [슬라이딩 윈도우](https://nightlies.apache.org/flink/flink-docs-master/docs/dev/datastream/operators/windows/#sliding-windows)

- 슬라이딩 윈도우 또한 고정 길이를 가짐
- `window slide parameter` 매개변수를 사용하여 윈도우가 생성되는 빈도 결정
- `window slide parameter`의 값이 윈도우 크기보다 작으면 슬라이딩 윈도우가 겹쳐 중복된 이벤트 발생할 수 있음

<img width="1032" height="411" alt="스크린샷 2026-01-11 오후 4 14 00" src="https://github.com/user-attachments/assets/122268a6-9447-4c2a-807b-2f909f67aea3" />

<br><br>

#### 💡 [세션 윈도우](https://nightlies.apache.org/flink/flink-docs-master/docs/dev/datastream/operators/windows/#session-windows)

- 세션 윈도우는 할동 세션별로 이벤트를 그룹화함
- 세션 윈도우는 텀블링 윈도우나 슬라이딩 윈도우와는 달리 서로 겹치지 않으며, 고정된 시작 및 종료 시간이 없음
- 세션 윈도우는 일정 시간 동안 이벤트가 수신되지 않으면 닫힘(즉, 비활성 상태가 되면 닫힘)
- 세션 윈도우는 `고정된 세션 간격` 또는 `비활성 기간`을 정의하여 세션 간격 추출 함수를 통해 구성할 수 있음
- 세션이 만료되면 현재 세션이 닫히고 이후의 이벤트는 새로운 세션에 할당됨

<img width="1032" height="387" alt="스크린샷 2026-01-11 오후 4 22 17" src="https://github.com/user-attachments/assets/3861a761-6bb1-44a5-a2d2-d38eb160d94d" />

<br><br>

#### 💡 [글로벌 윈도우](https://nightlies.apache.org/flink/flink-docs-master/docs/dev/datastream/operators/windows/#global-windows)

- 글로벌 윈도우는 같은 키를 가진 모든 이벤트를 하나의 전역 윈도우에 할당함
- 글로벌 윈도우는 반드시 **커스텀 트리거**를 정의해서 사용해야함, 그렇지 않으면 아무 역할도 하지 않음. 이유는 윈도우의 끝 지점이라는게 존재하지 않아서 기본 트리거는 아무런 Firing도 하지 않음

<img width="1032" height="406" alt="스크린샷 2026-01-11 오후 4 26 18" src="https://github.com/user-attachments/assets/2a15c99f-9421-47d0-bf04-6b7387d3433b" />

<br><br>

#### 2. Trigger

- 윈도우는 하나의 `트리거`와 `윈도우 함수`를 가지고 있음
- 함수에는 윈도우에 들어온 이벤트에 대해 어떤 연산을 할지 정의
- 트리거에는 언제 윈도우에 있는 데이터로 연산을 수행할지 결정

#### 3. Evictor

- `Trigger`가 발생하기 전 데이터 일부를 연산에서 제외하거나 `Trigger`가 발생하여 결과 데이터에 대해 특정 데이터 제외

#### 4. Allowed Lateness

- 이미 닫힌 윈도우에 대해 `Late Event`를 일정 시간까지 허용
- 현실 세계에서는 네트워크 지연, 재시도, 부하로 인해 이벤트 순서가 보장되지 않을 수 있음
- Flink Window Operator는 `Allowed Lateness`라는 기능 제공
- `Allowed Lateness`는 이미 Watermark가 흐른 뒤 Watermark가 명시한 시간보다 과거의 이벤트가 유입되더라도 특정 시간만큼 딜레이를 허용해 주는 것

```java
// allowedLateness(Time.hour(1)) 설정을 통해 Watermark보다 1시간 까지만 딜레이를 허용하겠다는 의미
inputStream
    .keyBy(...)
    .window(TumblingEventTimeWindows.of(Time.minutes(10)))
    .allowedLateness(Time.hour(1)) 
    .apply(new MyWindowFunction());
```

#### 예시

- 위 코드에서 allowedLateness가 1시간으로 설정되어 있음
- Watermark(2시 15분)가 이미 흐른 뒤 5개의 이벤트(1시 10분, 1시 11분, 1시 12분, 1시 15분, 1시 16분의 데이터)가 유입됨
- 사실상 다 버려야하는 데이터이지만 1시 15분 이후 이벤트는 허용 범위에 포함되어 닫힌 Window를 다시 열어 상태를 갱신하고 결과를 재출력함, 1시 15분 이전 이벤트는 버림(Drop) 또는 따로 빼서(Side Output)처리 됨

<img width="1032" height="416" alt="스크린샷 2026-01-11 오후 2 24 45" src="https://github.com/user-attachments/assets/609d1d82-1d10-486d-948d-e2c65386e8a4" />

<br>

#### 5. Side Output

- 허용 범위를 초과한 `Late Event`에 대해 재처리 전략
- `Late Event`는 별도 스트림으로 분리하여 다룸 (ex: DLQ)

#### 💡 Allowed Lateness와 Side Output의 관계

- `Late Event`는 Watermark가 닫히고 나서 도착한 이벤트
- `Allowed Lateness`는 이미 닫힌 윈도우에 대해 `Late Event`를 다시 받아서 윈도우를 갱신해주는 것
- 즉, `Allowed Lateness`는 윈도우를 다시 열어서 재처리를 할 수는 있지만 `Side Output`은 재처리할 수 없어 데이터를 잃지 않기 위한 처리

<br>

#### 참고

- https://nightlies.apache.org/flink/flink-docs-master/docs/dev/datastream-v2/watermark/
- https://docs.confluent.io/cloud/current/flink/concepts/timely-stream-processing.html
- https://nightlies.apache.org/flink/flink-docs-master/docs/dev/datastream/event-time/built_in/
- https://nightlies.apache.org/flink/flink-docs-master/docs/dev/datastream/operators/windows/#windows
- https://medium.com/rate-labs/%EC%8A%A4%ED%8A%B8%EB%A6%BC-%ED%94%84%EB%A1%9C%EC%84%B8%EC%8B%B1%EC%9D%98-%EA%B8%B4-%EC%97%AC%EC%A0%95%EC%9D%84-%EC%9C%84%ED%95%9C-%EC%9D%B4%EC%A0%95%ED%91%9C-with-flink-8e3953f97986



