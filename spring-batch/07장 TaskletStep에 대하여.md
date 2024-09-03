# TaskletStep에 대하여

<br>

## 1. 기본 개념

- 스프링 배치에서 제공하는 Step 구현체 중 하나로서 Tasklet을 실행시키는 객체입니다.
- RepeatTemplete을 사용하여 Tasklet 작업을 트랜잭션 내에서 반복적으로 수행합니다.
- Task 기반과 Chunk 기반으로 나누어 Tasklet을 실행합니다. 

<br>

### Task Vs Chunk 기반 비교

#### Task

- ItemReader와 ItemWriter와 같은 청크 기반의 작업보다 단일 작업 기반으로 처리되는 것이 더 효율적인 경우에 사용됩니다.
- 주로 Tasklet 구현체를 만들어 사용합니다.
- 대량 처리를 해야하는 경우 Chunk 기반보다 복잡한 구현이 필요합니다.

![스크린샷 2024-09-03 오후 9 47 15](https://github.com/user-attachments/assets/cc20f24e-75d5-4dd0-b2ad-899ffaeff03c)

<br>

#### Chunk

- 하나의 큰 덩어리를 N개씩 나누어 실행한다는 의미로 대량 처리에 적합합니다.
- ItemReader, ItemProcessor, ItemWriter를 사용하며 청크 기반 전용 Tasklet인 ChunkOrientedTasklet 구현체가 제공됩니다.

![스크린샷 2024-09-03 오후 9 48 09](https://github.com/user-attachments/assets/9f348f8d-688a-41b6-9d0a-79383859e076)

<br>

## 2. 주요 메서드들 

### tasklet()

- Step 내에서 구성되고 실행되는 객체로서 주로 '단일 태스크'를 수행하기 위한것입니다.
- TaskletStep에 의해 반복적으로 수행되며, 반환값에 따라 계속 수행되거나 한번만 수행 후 종료되기도 합니다.
  - RepeatStatus.FINISHED: Tasklet을 한번만 수행하고 종료합니다.
  - RepeatStatus.CONTINUABLE: Tasklet을 반복합니다. 예외가 발생하기 전까지 TaskletStep에 의해 반복적으로 수행되므로 무한 루프에 주의해야합니다.




















