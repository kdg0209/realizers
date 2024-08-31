# Step이란 무엇인가?

<br>

## 1. Step이란 무엇인가?

### 기본 개념

- Job을 구성하는 독립적인 하나의 단계로서 실제 배치 처리를 정의하고, 컨트롤하는데 필요한 모든 정보를 가지고 있는 객체입니다.
- 단순한 단일 태스트뿐만 아니라 복작한 비지니스 로직을 포함하는 모든 설정을 답고 있습니다.
- 모든 Job은 하나 이상의 Step을 가지고 있습니다.

<br>

### 기본 구현체

#### TaskletStep

- 가장 기본이 되는 클래스로서 Tasklet 타입의 구현체들을 제어합니다.

#### PartitionStep

- 멀티 스레드 방식으로 Step을 여러개로 분리하여 실행합니다.

#### JobStep

- Step내에서 Job을 실행하도록 합니다.

#### FlowStep

- Step내에서 Flow를 실행하도록 합니다.

<br>

### 흐름도

<img width="1040" alt="스크린샷 2024-08-31 오후 1 09 34" src="https://github.com/user-attachments/assets/217f9fa1-50ec-4baa-9948-e9f7dc1527d3">

<br>
<br>

## 2. StepExecution이란 무엇인가?

### 기본 개념

- Step에 대한 한번의 시도를 의미하는 객체로서 Step 실행중에 발생한 정보들을 저장하는 객체입니다.
- Step이 매번 시도될때마다 StepExecution은 생성되며, 각 Step별로 생성됩니다.
- Job이 FAILED 상태여서 재시작하더라도 이미 성공적으로 완료된 Step은 재실행되지 않고 실패한 Step만 재실행됩니다.
  - 예를들어 Step1, Step2, Step3이 있을 때 Step1과 Step2는 작업이 정상적으로 수행되었지만 Step3에서 실패를 하게 된다면 JobInstance가 재시도될 때 Step1과 Step2는 재시도하지 않고, Step3만 재시도 됩니다.
- 이전 단계 Step이 실패해서 현재 Step을 실행하지 않았다면 StepExecution을 생성하지 않습니다.
  - 에를들어 Step1, Step2, Step3이 있을 때 Step1은 정상적으로 수행이 완료되고, Step2에서 작업이 실패된다면 Step3의 StepExecution는 생성이 되지 않았다는 것입니다. (Step1, Step2의 StepExecution은 생성이 되어 있습니다.)
- Step의 StepExecution이 모두 정상적으로 완료되어야 JobExecution이 완료됩니다.
- Step의 StepExecution이 하나라도 실패한다면 JobExecution은 실패합니다.

<br>

### 흐름 예제

#### 모든 StepExecution이 성공된 경우

<img width="1043" alt="스크린샷 2024-08-31 오후 1 53 26" src="https://github.com/user-attachments/assets/c48cca33-36af-4c24-b57e-ab3a116ba134">

<br>

#### 하나의 StepExecution이 실패된 경우

- StepExecution이 하나라도 실패하게 된다면 JobExecution의 상태는 FAILED가 됩니다.

<img width="1050" alt="스크린샷 2024-08-31 오후 1 57 45" src="https://github.com/user-attachments/assets/e51d0624-ca37-4201-81f2-908bef7735e3">

<br>
<br>

## 3. StepContribution이란 무엇인가?

### 기본 개념

- 청크 프로세스의 변경사항을 버퍼링한 후 StepExecution 상태를 업데이트하는 객체입니다.
- 청크 commit 직전 StepExecution의 apply() 메서드를 호출하여 상태를 업데이트합니다.
- ExitStatus의 기본 종료코드 외에 사용자 정의 종료코드를 만들어 적용할 수 있습니다.

<br>

## 4. ExecutionContext 무엇인가?

### 기본 개념

- 프레임워크에서 유지 및 관리하는 키-값으로 된 컬렉션으로 StepExecution 또는 JobExecution 객체의 상태를 저장하는 공유 객체입니다.
- DB에는 직렬화된 값을 저장합니다.
- JobInstance를 재시작할 때 이미 처리한 row 데이터는 건너뛰고 이후로 수행할 수 있도록 할 때 상태 정보를 활용합니다.
- 공유 범위
  - Job 범위: 각 Job의 JobExecution에 저장되며 Job간에는 서로 공유되진 않지만 Job의 Step간에는 서로 공유할 수 있습니다.
  - Step 범위: 각 Step의 StepExecution에 저장되며 Step간에는 서로 공유되지 않습니다.

<br>

### 예제 코드

- 아래 코드에서 CustomTasklet1, CustomTasklet2 클래스가 있는데 CustomTasklet1 클래스에서 JobExecutionContext에 특정 값을 넣고, StepExecutionContext에도 특정 값을 넣었지만
CustomTasklet2 클래스에서는 JobExecutionContext에 넣은 값을 가져올 수 있지만 StepExecutionContext에 넣은 값은 가져올 수 없습니다. 그 이유는 Step간에는 ExecutionContext를 공유할 수 없기 때문입니다.

```java
@Configuration
@RequiredArgsConstructor
public class ProductJobConfiguration extends DefaultBatchConfiguration {

    @Bean
    public Job job(JobRepository jobRepository, Step step1, Step step2) {
        return new JobBuilder("productJob", jobRepository)
                .start(step1)
                .next(step2)
                .build();
    }

    @Bean
    public Step step1(JobRepository jobRepository, PlatformTransactionManager manager) {
        return new StepBuilder("productStep", jobRepository)
                .tasklet(new CustomTasklet1(), manager)
                .build();
    }

    @Bean
    public Step step2(JobRepository jobRepository, PlatformTransactionManager manager) {
        return new StepBuilder("productStep", jobRepository)
                .tasklet(new CustomTasklet2(), manager)
                .build();
    }
}

@Component
public class CustomTasklet1 implements Tasklet {

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        System.out.println("step1 execute");
        ExecutionContext JobExecutionContext = contribution.getStepExecution().getJobExecution().getExecutionContext();
        ExecutionContext stepExecutionContext = contribution.getStepExecution().getExecutionContext();

        JobExecutionContext.put("job message", "hello spring batch");
        stepExecutionContext.put("step message", "hello??");
        return RepeatStatus.FINISHED;
    }
}

@Component
public class CustomTasklet2 implements Tasklet {

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        System.out.println("step2 execute");
        ExecutionContext JobExecutionContext = contribution.getStepExecution().getJobExecution().getExecutionContext();
        ExecutionContext stepExecutionContext = contribution.getStepExecution().getExecutionContext();

        Object jobMessage = JobExecutionContext.get("job message");
        Object stepMessage = stepExecutionContext.get("step message");

        System.out.println("jobMessage = " + jobMessage);   // hello spring batch 출력됨
        System.out.println("stepMessage = " + stepMessage); // null 출력됨

        return RepeatStatus.FINISHED;
    }
}
```


















