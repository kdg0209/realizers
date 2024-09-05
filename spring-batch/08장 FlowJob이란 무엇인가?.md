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

<br>

#### 예제 코드

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




















<br>

### 참고

- https://docs.spring.io/spring-batch/reference/step/controlling-flow.html#SequentialFlow






