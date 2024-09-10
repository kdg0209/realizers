# Scope란 무엇인가?

<br>

## 1. 기본 개념

- Scope란 스프링 컨테이너에서 빈이 관리되는 범위를 말합니다. 그럼 스프링 배치에서 말하는 Scope는 무엇일까요?

#### 스프링 배치에서 Scope란?

- 스프링 배치에서는 @JobScope, @StepScope 어노테이션을 사용하여 Scope를 만들 수 있습니다.
- @JobScope, @StepScope 어노테이션은 기본적으로 프록시 모드로 동작하게 됩니다. 따라서 빈 생성이 애플리케이션 구동시점이 아닌 빈의 실행시점에 이루어지게 됩니다.

#### 🧐 프록시 모드로인한 Late Binding은 무엇이 이점인가?

- ApplicationContext가 실행되는 시점이 아닌 Service와 같은 비지니스 로직 처리 단계에서 Job Parameter를 할당할 수 있습니다.
- 병렬처리시 각 스레드마다 생성된 Scope 빈이 할당되기 때문에 멀티 스레드에 안전합니다.
  - Step안에 Tasklet이 있고, Tasklet에서 멤버 변수에 접근하여 해당 변수를 수정하는 로직이 있다고 가정했을 경우 서로 다른 Step에서 하나의 Tasklet을 두고 멤버 변수 상태를 변경한다면 동시선 이슈가 발생하게 됩니다. 하지만 @StepScope 내부에 있다면
각각의 Step에서 별도의 Tasklet을 생성하고 실행하기 때문에 동시성 이슈가 발생하지 않게 됩니다.

#### 주의사항

- @Value 어노테이션을 사용할 경우 빈 선언문에 @JobScope, @StepScope를 정의하지 않으면 예외가 발생하기 때문에 반드시 선언해야 합니다.

#### @JobScope란?

- Step 선언문에 정의합니다.
- @Value: jobParameters, jobExecutionContext만 사용 가능합니다.

#### @StepScope란?

- Tasklet이나 ItemReader, ItemProcessor, ItemWriter 선언문에 정의합니다. 
- @Value: jobParameters, jobExecutionContext, stepExecutionContext 사용 가능합니다.

#### 예제 코드

```java
@Configuration
@RequiredArgsConstructor
public class FlowJobConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager manager;

    @Bean
    public Job job() {
        return new JobBuilder("job", jobRepository)
                .start(stepA(null))
                .next(stepB())
                .build();
    }

    @Bean
    @JobScope
    public Step stepA(@Value("#{jobParameters[message]}") String message) {
        return new StepBuilder("stepA", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("stepA execute and message = " + message);
                    return RepeatStatus.FINISHED;
                }, manager)
                .build();
    }

    @Bean
    public Step stepB() {
        return new StepBuilder("stepB", jobRepository)
                .tasklet(tasklet(null), manager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet tasklet(@Value("#{jobParameters[name]}") String name) {
        return (contribution, chunkContext) -> {
            System.out.println("stepA execute and name = " + name);
            return RepeatStatus.FINISHED;
        };
    }
}

@Component
@RequiredArgsConstructor
public class ProductScheduler {

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;

    @Scheduled(cron = "0/5 * * * * *") // 5초마다 실행
    public void run() {
        try {
            var job = jobRegistry.getJob("job");
            var jobParam = new JobParametersBuilder()
                    .addString("message", "hello? spring batch 5!")
                    .addString("name", "KDG")
                    .addString("UUID", UUID.randomUUID().toString())
                    .toJobParameters();
            jobLauncher.run(job, jobParam);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```


<br>

#### 참고

- https://jojoldu.tistory.com/330
- https://jojoldu.tistory.com/132
- https://gngsn.tistory.com/187


