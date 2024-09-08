# FlowJob이란 무엇인가?

<br>

## 1. 기본 개념

- Step을 순차적으로만 구성하는게 아니라 특정 상태에 따라 흐름 전환을 할 수 있도록 구성할 수 있습니다.
- Step이 실패하거나 성공할 때 특정한 흐름으로 이어나갈 수 있도록 합니다. 

#### 예제 코드

- flowJob의 step 작업이 성공적으로 완료된다면 successStep으로 넘어기지만 작업이 실패된다면 failedStep 작업이 실행됩니다.
- 아래 예제는 실패되는 예제 코드입니다.

```java
@Configuration
@RequiredArgsConstructor
public class FlowJobConfiguration {

    @Bean
    public Job flowJob(JobRepository jobRepository, Step step, Step successStep, Step failedStep) {
        return new JobBuilder("flowJob", jobRepository)
                .start(step)
                    .on("COMPLETED")
                    .to(successStep)
                .from(step)
                    .on("FAILED")
                    .to(failedStep)
                .end()
                .build();
    }

    @Bean
    public Step step(JobRepository jobRepository, PlatformTransactionManager manager) {
        return new StepBuilder("step", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("first step execute !");
                    throw new RuntimeException("exception!");
                }, manager)
                .build();
    }

    @Bean
    public Step successStep(JobRepository jobRepository, PlatformTransactionManager manager) {
        return new StepBuilder("successStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("successStep execute !");
                    return RepeatStatus.FINISHED;
                }, manager)
                .build();
    }

    @Bean
    public Step failedStep(JobRepository jobRepository, PlatformTransactionManager manager) {
        return new StepBuilder("failedStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("failedStep execute !");
                    return RepeatStatus.FINISHED;
                }, manager)
                .build();
    }
}
```

<br>

#### 흐름도

- Job의 첫번째 Step에서 예외가 발생하지 않아 Step의 Status가 'COMPLETED'라면 그 다음 Step인 successStep으로 넘어가게 됩니다. 만약 Step에서 예외가 발생하여 해당 Step의 Status가 'FAILED'라면 failedStep이 실행됩니다.
- 참고로 첫번째 Step에서 예외가 발생하여 failedStep이 실행되었는데, failedStep에서 작업이 성공적으로 끝난다면 Job의 status는 'COMPLETED'가 됩니다.

![스크린샷 2024-09-05 오후 10 05 40](https://github.com/user-attachments/assets/c3c416ac-8832-4c81-bb41-821b4b5044cf)

<br>

### 심화 구성

- FlowA는 내부적으로 Step1과 Step2를 가지고 있고 이 Step들이 작업이 완료되면 StepA가 실행되며 StepA의 작업이 완료되면 FlowB가 실행되고 또 내부적으로 Step3, Step4가 실행되고 최종적으로 StepB가 실행됩니다.

![스크린샷 2024-09-05 오후 10 34 09](https://github.com/user-attachments/assets/1e8499da-a395-48fa-9f64-f1926cb02761)

#### 예제 코드

<details>
<summary>예제 코드 살펴보기</summary>

```java
@Configuration
@RequiredArgsConstructor
public class FlowJobConfiguration {

    @Bean
    public Job flowJob(JobRepository jobRepository, PlatformTransactionManager manager) {
        return new JobBuilder("flowJob", jobRepository)
                .start(flowA(jobRepository, manager))
                .next(stepA(jobRepository, manager))
                .next(flowB(jobRepository, manager))
                .next(stepB(jobRepository, manager))
                .end()
                .build();
    }

    @Bean
    public Flow flowA(JobRepository jobRepository, PlatformTransactionManager manager) {
        FlowBuilder<Flow> flowA = new FlowBuilder<>("flowA");
        return flowA
                .start(step1(jobRepository, manager))
                .next(step2(jobRepository, manager))
                .build();
    }

    @Bean
    public Step stepA(JobRepository jobRepository, PlatformTransactionManager manager) {
        return new StepBuilder("stepA", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("stepA execute !");
                    return RepeatStatus.FINISHED;
                }, manager)
                .build();
    }

    @Bean
    public Flow flowB(JobRepository jobRepository, PlatformTransactionManager manager) {
        FlowBuilder<Flow> flowB = new FlowBuilder<>("flowB");
        return flowB
                .start(step3(jobRepository, manager))
                .next(step4(jobRepository, manager))
                .build();
    }

    @Bean
    public Step step1(JobRepository jobRepository, PlatformTransactionManager manager) {
        return new StepBuilder("step1", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("step1 execute !");
                    return RepeatStatus.FINISHED;
                }, manager)
                .build();
    }

    @Bean
    public Step step2(JobRepository jobRepository, PlatformTransactionManager manager) {
        return new StepBuilder("step2", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("step2 execute !");
                    return RepeatStatus.FINISHED;
                }, manager)
                .build();
    }

    @Bean
    public Step step3(JobRepository jobRepository, PlatformTransactionManager manager) {
        return new StepBuilder("step3", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("step3 execute !");
                    return RepeatStatus.FINISHED;
                }, manager)
                .build();
    }

    @Bean
    public Step step4(JobRepository jobRepository, PlatformTransactionManager manager) {
        return new StepBuilder("step4", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("step4 execute !");
                    return RepeatStatus.FINISHED;
                }, manager)
                .build();
    }

    @Bean
    public Step stepB(JobRepository jobRepository, PlatformTransactionManager manager) {
        return new StepBuilder("stepB", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("stepB execute !");
                    return RepeatStatus.FINISHED;
                }, manager)
                .build();
    }
}
```

</details>

<br>

## 2. Transition

- transition은 작업의 전환 또는 전이를 의미합니다.

### 2-1. 여러 배치 상태 유형

#### 1. BatchStatus

- BatchStatus는 JobExecution과 StepExecution의 속성으로 Job과 Step의 최종 작업의 결과가 어떤 상태인지를 나타내는 객체입니다.

&ensp; SimpleJob

&emsp;&ensp; - 최종 Step의 BatchStatus를 Job의 최종 BatchStatus로 반영합니다.

&ensp; FlowJob

&emsp;&ensp; - Flow내 최종 Step의 BatchStatus를 FlowExecutionStatus로 반영합니다.

<br>

#### 2. ExitStatus

- JobExecution과 StepExecution의 속성으로 Job과 Step이 실행 후 어떤 상태로 종료되었는지 정의하는 객체입니다.
- 기본적으로 ExitStatus와 BatchStatus는 같은 값으로 설정됩니다.

 &ensp; SimpleJob

 &emsp;&ensp; - 최종 Step의 ExitStatus를 Job의 최종 ExitStatus로 반영합니다.

 &ensp; FlowJob

 &emsp;&ensp; - Flow내 최종 Step의 ExitStatus를 FlowExecutionStatus로 반영합니다.

<br>

#### 3. FlowExecutionStatus

- FlowExecution의 속성으로 Flow 실행 후 최종 결과 상태가 무엇인지 정의하는 객체입니다.
- Flow내에 있는 Step이 실행되고 나서 ExitStatus 값을 FlowExecutionStatus 값으로 반영합니다.

<br>

#### 4. BatchStatus와 ExitStatus의 흐름도

<img width="1032" alt="스크린샷 2024-09-08 오후 2 17 37" src="https://github.com/user-attachments/assets/42072214-7100-4a16-9f9e-ded68f161cf8">

<br>
<br>

## 3. 사용자 정의 ExitStatus

- ExitStatus에 존재하지 않는 exitCode를 새롭게 정의해서 설정하는 방법입니다.
- StepExecutionListener의 afterStep() 메서드에서 Custom exitCode를 생성 후 새로운 ExitCode를 반환하면됩니다.

#### 예제 코드

- 아래 예제 코드는 stepA가 'FAILED' 상태로 작업을 종료되면 stepB가 실행됩니다. 그리고 stepB의 작업은 상태는 CustomStepExecutionListener 클래스의 afterStep() 메서드에의해 재정의 되는데, 내부에서 'FAILED'가 아닌 경우에 새롭게 정의한 'DO PASS' 상태가 됩니다. 그리고 on('DO PASS')에 부합하게 되고, 결국 stop() 메서드에 의해 JobExecution은 멈추게 됩니다.

```java
public class CustomStepExecutionListener implements StepExecutionListener {

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {

        String exitCode = stepExecution.getExitStatus().getExitCode();
        if (!exitCode.equals(ExitStatus.FAILED.getExitCode())) {
            return new ExitStatus("DO PASS");
        }
        return ExitStatus.FAILED;
    }
}

@Configuration
@RequiredArgsConstructor
public class TestJobConfiguration {

    @Bean
    public Job testJob(JobRepository jobRepository, Step stepA, Step stepB) {
        return new JobBuilder("testJob", jobRepository)
                .start(stepA)
                    .on("FAILED")
                    .to(stepB)
                    .on("DO PASS")
                    .stop()
                .end()
                .build();
    }

    @Bean
    public Step stepA(JobRepository jobRepository, PlatformTransactionManager manager) {
        return new StepBuilder("stepA", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    contribution.getStepExecution().setExitStatus(ExitStatus.FAILED);
                    System.out.println("stepA execute !");
                    return RepeatStatus.FINISHED;
                }, manager)
                .build();
    }

    @Bean
    public Step stepB(JobRepository jobRepository, PlatformTransactionManager manager) {
        return new StepBuilder("stepB", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    return RepeatStatus.FINISHED;
                }, manager)
                .listener(new CustomStepExecutionListener())
                .build();
    }
}
```

<br>

## 4. JobExecutionDecider

### 기본 개념

- ExitStatus를 조작하거나 StepExecutionListener를 등록할 필요없이 Transition 처리를 위한 전용 클래스입니다.
- Step과 Transition 역할을 분리해서 사용할 수 있습니다.
- Step의 ExitStatus가 아닌 JobExecutionDecider의 FlowExecutionStatus 상태값을 새롭게 정의해서 반환합니다.

#### 예제 코드

```java
public class CustomDecider implements JobExecutionDecider {

    private int count = 0;

    @Override
    public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {

        count++;
        if (count % 2 == 0) {
            return new FlowExecutionStatus("EVEN");
        }
        return new FlowExecutionStatus("ODD");
    }
}

@Configuration
@RequiredArgsConstructor
public class FlowJobConfiguration {

    @Bean
    public JobExecutionDecider decider() {
        return new CustomDecider();
    }

    @Bean
    public Job testJob(JobRepository jobRepository, Step stepA, Step oddStep, Step evenStep) {
        return new JobBuilder("testJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(stepA)
                .next(decider())
                .from(decider()).on("ODD").to(oddStep)
                .from(decider()).on("EVEN").to(evenStep)
                .end()
                .build();
    }

    @Bean
    public Step stepA(JobRepository jobRepository, PlatformTransactionManager manager) {
        return new StepBuilder("stepA", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("stepA execute !");
                    return RepeatStatus.FINISHED;
                }, manager)
                .build();
    }

    @Bean
    public Step oddStep(JobRepository jobRepository, PlatformTransactionManager manager) {
        return new StepBuilder("oddStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("oddStep execute !");
                    return RepeatStatus.FINISHED;
                }, manager)
                .build();
    }

    @Bean
    public Step evenStep(JobRepository jobRepository, PlatformTransactionManager manager) {
        return new StepBuilder("evenStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("evenStep execute !");
                    return RepeatStatus.FINISHED;
                }, manager)
                .build();
    }
}
```

<br>

## 5. FlowJob의 아키텍처

![스크린샷 2024-09-08 오후 7 09 58](https://github.com/user-attachments/assets/b899673a-1694-4f39-8eb1-389c3187d41a)

<br>

### 참고

- https://docs.spring.io/spring-batch/reference/step/controlling-flow.html#SequentialFlow
- https://docs.spring.io/spring-batch/docs/current/api/deprecated-list.html
- https://github.com/KMGeon/SpringBatch-playground



