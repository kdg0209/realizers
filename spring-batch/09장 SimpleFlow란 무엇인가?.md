# SimpleFlow란 무엇인가?

<br>

## 1. 기본 개념

- 스프링 배치에서 제공하는 Flow 인터페이스의 구현체이며, Step, Flow, JobExecutionDecider를 담고 있으며, State를 실행시키는 객체입니다.
- FlowBuilder를 사용해서 생성하며 Transition과 조합하여 여러 로직의 Flow를 구성하거나 중첩으로 만들 수 있습니다.

#### 간단한 예제 코드

- 아래 코드를 보면 flowA에서 stepA, stepB를 실행시키고, 작업이 'COMPLETED' 상태가 된다면 flowB로 넘어갑니다. 하지만 작업이 'FAILED' 상태 라면 failStep으로 넘어가게 됩니다.
- flowB에서는 flowC를 실행시키며, flowC에서는 stepC, stepD를 실행시키고 있습니다. 이 작업이 완료되면 최종적으로 stepE가 실행됩니다.
- 하지만 아래 코드는 최종적으로 flowA에서 stepA로 진행되었지만 'FAILED' 상태가 되어 failStep으로 넘어가게 됩니다.

```java
@Configuration
@RequiredArgsConstructor
public class FlowJobConfiguration {

    @Bean
    public Job flowJob(JobRepository jobRepository, Flow flowA, Flow flowB, Step failStep) {
        return new JobBuilder("flowJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(flowA)
                    .on("COMPLETED")
                    .to(flowB)
                .from(flowA)
                    .on("FAILED")
                    .to(failStep)
                .end()
                .build();
    }

    @Bean
    public Flow flowA(Step stepA, Step stepB) {
        FlowBuilder<Flow> flow = new FlowBuilder<>("flowA");
        return flow
                .start(stepA)
                .next(stepB)
                .build();
    }

    @Bean
    public Flow flowB(Flow flowC, Step stepE) {
        FlowBuilder<Flow> flow = new FlowBuilder<>("flowB");
        return flow
                .start(flowC)
                .next(stepE)
                .build();
    }

    @Bean
    public Flow flowC(Step stepC, Step stepD) {
        FlowBuilder<Flow> flow = new FlowBuilder<>("flowC");
        return flow
                .start(stepC)
                .next(stepD)
                .build();
    }

    @Bean
    public Step stepA(JobRepository jobRepository, PlatformTransactionManager manager) {
        return new StepBuilder("stepA", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("stepA execute !");
                    contribution.setExitStatus(ExitStatus.FAILED);
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

    @Bean
    public Step stepC(JobRepository jobRepository, PlatformTransactionManager manager) {
        return new StepBuilder("stepC", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("stepC execute !");
                    return RepeatStatus.FINISHED;
                }, manager)
                .build();
    }

    @Bean
    public Step stepD(JobRepository jobRepository, PlatformTransactionManager manager) {
        return new StepBuilder("stepD", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("stepD execute !");
                    return RepeatStatus.FINISHED;
                }, manager)
                .build();
    }

    @Bean
    public Step stepE(JobRepository jobRepository, PlatformTransactionManager manager) {
        return new StepBuilder("stepE", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("stepE execute !");
                    return RepeatStatus.FINISHED;
                }, manager)
                .build();
    }

    @Bean
    public Step failStep(JobRepository jobRepository, PlatformTransactionManager manager) {
        return new StepBuilder("failStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("failStep execute !");
                    return RepeatStatus.FINISHED;
                }, manager)
                .build();
    }
}
```
<br>

## 2. SimpleFlow의 아키텍처

- StepState, FlowState, DesicionState, SplitState는 자신이 가지고 있는 flow를 실행시키고, 작업이 완료되면 FlowExecutionStatus를 SimpleFlow로 반환합니다.

![스크린샷 2024-09-09 오후 11 26 13](https://github.com/user-attachments/assets/650ce6d7-0013-43ab-9f45-a611aef7f321)















