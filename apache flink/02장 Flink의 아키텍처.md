# 2. Flink 아키텍처

- Flink는 분산 시스템이므로 스트리밍 애플리케이션을 실행하려면 리소스를 효율적으로 관리해야 하는데 이때 [hadoop-yarn](https://hadoop.apache.org/docs/stable/hadoop-yarn/hadoop-yarn-site/YARN.html)이나 [kubernetes](https://kubernetes.io/)와 같은 외부 클러스터 리소스 관리자를 사용할 수도 있고, Standalone cluster나 라이브러리(Application Mode)로 독립 실행시킬 수 있습니다.

<br>

## 1. Flink의 아키텍처

### 1-1. JobManager

#### 역할

- `JobManager`는 Flink Cluster의 전체 작업을 관리하는 중앙 컨트롤러 역할을 수행.(마스터 노드 역할 수행)
- `JobManager`는 최소 하나 이상 존재 해야하며, 고가용성 구성 시 N개의 `JobManager`가 있을 수 있으며 그중 하나는 리더 역할을 수행하고 나머지는 대기 상태로 존재.

#### 작업 계획 및 스케줄링

- Client가 제출한 Job을 수신하고 실행을 관리
- Job을 Execution Graph로 변환하고 Task 단위로 분할
- 각 Task를 적절한 `TaskManager`에게 스케줄링

#### 리소스 관리

- `ResourceManager`를 통해 클러스터의 리소스를 관리
- 리소스 스케줄링 단위인 태스크 슬롯 관리
- `TaskManager` 슬롯(Slot) 할당을 조정
- 외부 클러스터 리소스 관리자(hadoop-yarn, kubernetes) 또는 Standalone cluster이나 라이브러리 연동

#### 장애 복구 및 체크포인트 관리

- JobManager 내 `CheckpointCoordinator`가 체크포인트를 주기적으로 트리거하고 관리
- 장애 발생 시 저장된 체크포인트를 기반으로 작업 복구
- 클러스터의 전체 상태를 추적하고 장애 감지를 수행

<br>

### 1-2. TaskManager

#### 역할

- `TaskManager`는 실제 데이터 처리 작업을 수행하는 `워커 노드`
- 분산 환경에서 실제 연산을 담당하는 핵심 컴포넌트

#### 데이터 스트림 처리 및 연산 실행

- `JobMaster`로부터 할당받은 Task를 실행
- 스트리밍 데이터에 대한 변환, 집계, 필터링 등의 연산 수행
- 결과 데이터를 다음 단계의 `Taskmanager`나 외부 시스템으로 전달

#### Task Slot을 통한 병렬 처리

- TaskManager는 `Task Slot`이라는 개념을 통해 병렬 처리 지원
- 각 TaskManager는 N개의 `Task Slot`을 가짐
- 하나의 Slot은 하나의 Task(Operator Chain 단위)를 실행
- `Slot Sharing`을 통해 여러 연산이 하나의 Slot을 공유할 수 있음
- Slot의 개수는 TaskManager가 동시에 실행할 수 있는 Task의 개수를 결정

#### 메모리 및 상태 관리

- 데이터 처리 과정에서 필요한 상태(state)를 `State Backend`를 통해 관리
- 메모리 기반 또는 RocksDB 기반 상태 저장을 지원
- `RocksDB State Backend`를 통해 대용량 상태를 디스크 기반으로 처리 가능

#### 장애 발생 시 복구 지원

- 체크포인트에 저장된 상태를 기반으로 장애 복구 수행
- 장애 발생 시 `JobMaster`의 지시에 따라 `TaskManager`가 가장 최근 체크포인트의 상태를 로드하여 작업을 재시작
- TaskManager간 상태 데이터 재분배를 통한 동적 복구 지원

<br>

### 1-3. JobClient

#### 역할

- `JobClient`는 Flink 애플리케이션에서 제출된 Job과 상호작용하기 위한 클라이언트 API

#### 주요 기능

- Job 제출 후 Job과 통신할 수 있는 핸들 제공
- Job 상태 조회 (RUNNING, FINISHED, FAILED 등)
- Job 취소(cancel) 요청
- Batch Job의 실행 결과(JobExecutionResult) 수신

#### 실행 모드

- Detached Mode: 작업 제출 후 JobClient가 종료되는 모드
- Attached Mode: 작업이 완료될 때까지 JobClient가 대기하는 모드

<br>

## 2. State Backend

- `State Backend`란 스트림 처리에서 연산 결과로 유지되는 상태(`state`)를 어떻게 저장/복구/관리할지 정의하는 구성 요소
- `State Backend`는 체크포인트 시 상태를 어떤 방식의 스냅샷으로 저장할지 정의

<img width="1032" height="604" alt="스크린샷 2026-01-06 오후 9 11 43" src="https://github.com/user-attachments/assets/6af92564-fbd8-47ff-97d6-6bb0ad22ea8f" />

<br>
<br>

### 2-1. HashMapStateBackend

- 상태를 JVM Heap에 Java 객체로 저장하는 방식
- 메모리에 올려놓기 때문에 빠르지만 메모리 크기와 GC에 직접적인 영향 받음
- 상태를 거의 저장하지 않는 환경에서 유리하지만 상태가 점점 커져 집계 또는 윈도우 처리를 해야하는 경우 부적합 (대규모 상태를 저장하는 운영 환경에는 부적합할 수 있음)

<br>

### 2-2. EmbeddedRocksDBStateBackend

- 상태를 `TaskManager`의 로컬 디스크의 `RocksDB`에 저장하는 방식
- 모든 상태는 직렬화된 byte 배열 형태로 저장되며, 필요할 경우 역직렬화하여 사용
- RocksDB 기반 상태는 `incremental checkpoint`를 통해 대규모 상태에서 체크포인트 비용을 줄일 수 있음
- 직렬화/역직렬화시 비용과 압축으로 인해 디스크 I/O 발생

<br>

### 2-3. ForStStateBackend

- ForStStateBackend는 RocksDBStateBackend를 대체하기 위한 Flink-native embedded state engine
- 상태는 `TaskManager`의 로컬 디스크에 `LSM-tree` 구조로 저장되며, 체크포인트 시 상태 스냅샷이 원격 저장소(S3, HDFS)에 저장

#### 📌 중요한 점

- 상태를 원격 저장소에 직접 두는게 아니라 상태는 여전히 `TaskManager`의 로컬 디스크에 위치해 있음
- 정리하자면, 상태는 `TaskManager`의 로컬 디스크에 있으며, 원격 저장소에는 체크포인트의 스냅샷이 저장되어 있음. 장애 발생 시 스냅샷을 다운로드하여 상태를 복원하는 것임

#### 💡 ForStStateBackend의 등장 배경

- [등장 배경](https://nightlies.apache.org/flink/flink-docs-master/docs/ops/state/disaggregated_state/)
- 로컬 디스크의 제약 조건
- 급격한 리소스 사용량 증가
- 대용량 복구

<br>

## 3. Checkpoint와 Savepoint

- Checkpoint는 장애 복구를 위한 스냅샷이고, Savepoint는 운영·배포를 위한 사용자가 명시적으로 생성한 스냅샷
- Checkpoint와 Savepoint 매커니즘은 비동기 방식으로 애플리케이션이 이벤트를 계속 처리하는 동안 **모든 작업을 중지**하지 않고 상태의 스냅샷 찍음

### 3-1. Checkpoint란?

#### 정의

- 장애 복구를 위해 Flink가 자동으로 주기적으로 생성하는 상태 스냅샷
- 장애 복구에 활용
  - TaskManager 장애 발생 → Flink가 자동으로 마지막 체크포인트 복원 → 사용자 개입 없음

<br>

### 3-2. Savepoint란?

#### 정의

- 운영자가 명시적(수동)으로 트리거하는 상태 스냅샷
- Job을 의도적으로 중단/재시작/업그레이드할 때 사용
- 운영·배포에 활용(애플리케이션 재시작 시)
  - Savepoint 생성 → Job 중단 → 배포 → Savepoint로 Job 재시작

<br>

## 4. Data Flow

- Flink의 DataFlow란 Source로부터 데이터를 읽어 변환(`Transformation`)을 거쳐 싱크(`Sink`)로 보내는 일련의 연속적인 데이터 처리 파이프라인

#### Source

- 외부로부터 데이터를 읽어오는 역할
- 장애 복구를 위해 어디까지 읽었는지 상태(`state`)를 관리
- 장애 발생 시 마지막 성공한 체크포인트 기준으로 데이터를 다시 읽음

#### Transformation

- Source에서 읽은 데이터를 가공, 변환, 집계

#### Sink

- 처리된 데이터를 외부 시스템으로 전달

<br>

## 5. Failover & Restart 아키텍처

### 5-1. Task Failure Recovery

- Flink는 Task 단위의 실패를 감지하고, 데이터의 일관성을 해치지 않는 최소 범위만 재시작하는 방식으로 장애 복구
- 쉽게 표현하자면, Flink는 작업을 정상 상태로 복구하기 위해 오류가 발생한 작업과 영향을 받는 다른 작업을 다시 시작

<br>

### 5-2. Failover Region

- `Failover Region`은 장애 발생 시 함께 재시작되어야 하는 Task들의 최소 집합체
- Task 하나가 실패하면 Job 전체를 재시작 하는게 아니라 해당 Task가 속한 `Failover Region`만 재시작
- Forward(1:1)로 연결된 Task들은 같은 Region
- Shuffle(KeyBy, Rebalance) 지점에서 Region이 분리됨

#### 💡 Region을 나누는 기준

- 데이터 교환 방식에 따라 나뉨
- 상태(`state`)를 강하게 공유하는 Task들은 같은 Region

<br>

### 5-3. 재시작 대상 지역 선정 기준

#### 기준 1

- 작업이 실패한 영역이 다시 시작됨

#### 기준 2

- 재시작될 영역에서 필요한 결과 파티션을 사용할 수 없는 경우 결과 파티션을 생성하는 해당 영역도 재시작됨
- 즉, Region A가 재시작하려고 보니 이전 단계인 Region B가 만든 데이터 파티션이 이미 깨져있어 A는 B가 만든 데이터를 신뢰할 수 없기 때문에 Region B도 함께 재시작됨

```txt
[Region B] --(결과 파티션 X)--> [Region A ❌]
```

#### 기준 3

- 리전을 재시작해야 하는 경우 해당 리전의 모든 소비자 리전도 함께 재시작됨
- 즉, **비결정적 처리**로 인해 같은 데이터를 다시 처리해도 이전과 정확히 같은 파티션이 만들어진다는 보장이 없기 때문에 소비자 리전도 재시작함
  - 비결정적 처리란
    - 처리 순서가 바뀔 수 있거나
    - 병렬 처리 타이밍이 달라지거나
    - 해시 파티셔닝 결과가 달라질 수 있는 경우

<br>

#### 참고

- https://nightlies.apache.org/flink/flink-docs-master/docs/ops/state/state_backends/
- https://nightlies.apache.org/flink/flink-docs-master/docs/ops/state/disaggregated_state/
- https://nightlies.apache.org/flink/flink-docs-release-2.2/docs/dev/datastream/overview/
- https://nightlies.apache.org/flink/flink-docs-master/docs/ops/state/task_failure_recovery/
- https://aws.amazon.com/ko/what-is/apache-flink/#:~:text=Flink%EB%8A%94%20%EB%86%92%EC%9D%80%20%EC%B2%98%EB%A6%AC%EB%9F%89%2C%20%EB%82%AE%EC%9D%80,%EC%9D%B4%EC%83%81%EC%9D%98%20%EB%8C%80%EC%83%81%EC%9C%BC%EB%A1%9C%20%EC%A0%84%EC%86%A1%EB%90%A9%EB%8B%88%EB%8B%A4.


