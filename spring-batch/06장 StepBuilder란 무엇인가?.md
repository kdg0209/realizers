# StepBuilder란 무엇인가?

<br>

## 1. 기본 개념

- Step을 어떻게 구성하느냐에 따라 다섯개의 하위 빌더 클래스를 생성하고 해당 빌더 클래스에게 작업을 위임합니다.

#### TaskletStepBuilder

- tasklet() 메서드를 통해 TaskletStepBuilder 클래스를 생성합니다.

#### SimpleStepBuilder

- chunk() 메서드를 통해 내부적으로 청크기반의 작업을 처리하는 ChunkOrientedTasklet 클래스를 생성합니다.

#### PartitionStepBuilder

- partitioner() 메서드를 통해 멀티 스레드방식으로 Job을 실행합니다.

#### JobStepBuidler

- job() 메서드를 통해 Step 내부에서 Job을 실행합니다.

#### FlowStepBuilder

- flow() 메서드를 통해 Step 내부에서 Flow를 실행합니다.

<br>

![스크린샷 2024-09-03 오후 9 15 22](https://github.com/user-attachments/assets/7afc6735-f5ab-4b1d-a71a-94320a9da842)

<br>
<br>

## 2. 간단한 사용 방법

































