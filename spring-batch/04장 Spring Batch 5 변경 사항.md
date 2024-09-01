# Spring Batch 5 변경 사항들

<br>

## 1. JobLauncherApplicationRunner 실행 변경점

### 기존

- Spring Batch 5버전 이전에는 @EnableBatchProcessing 어노테이션을 사용하여 스프링 배치를 작동시키고, 스프링 배치의 자동 설정 클래스가 실행됨으로써 Bean으로 등록된 Job들을 조회해서 초기화와 동시에 Job을 수행하도록 구성되어 있었다고 합니다.

### 변경

- Spring Batch 5버전 이후부터는 @EnableBatchProcessing 어노테이션이 필수가 아니게 되었습니다. 다만 @ConditionalOnMissingBean 어노테이션에 선언되어 있는 DefaultBatchConfiguration 클래스와 EnableBatchProcessing 어노테이션이 선언되어 있지 않은 경우에만 활성화가 됩니다.
- 또한 yml 파일이나 properties 파일에 spring.batch.job.enabled가 false로 설정되어 있다면 비활성화 됩니다. 이 또한 spring.batch.job.enabled가 true로 설정되어 있거나 아니면 설정 파일에 해당 설정값이 선언되어 있지 않아야 활성화가 됩니다.
- 즉, 프로그램 코드 어디선가 DefaultBatchConfiguration 클래스를 상속받고 있거나 @EnableBatchProcessing 어노테이션이 선언되어 있거나 yml 파일이나 properties 파일에 spring.batch.job.enabled가 false로 설정되어 있다면 JobLauncherApplicationRunner 클래스의 jobLauncherApplicationRunner() 메서드는 호출되지 않습니다.

```java
@ConditionalOnMissingBean(
    value = {DefaultBatchConfiguration.class},
    annotation = {EnableBatchProcessing.class}
)
public class BatchAutoConfiguration {
    public BatchAutoConfiguration() {
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
        prefix = "spring.batch.job",
        name = {"enabled"},
        havingValue = "true",
        matchIfMissing = true
    )
    public JobLauncherApplicationRunner jobLauncherApplicationRunner(JobLauncher jobLauncher, JobExplorer jobExplorer, JobRepository jobRepository, BatchProperties properties) {
        JobLauncherApplicationRunner runner = new JobLauncherApplicationRunner(jobLauncher, jobExplorer, jobRepository);
        String jobName = properties.getJob().getName();
        if (StringUtils.hasText(jobName)) {
            runner.setJobName(jobName);
        }

        return runner;
    }

    // ... 이하 코드 생략
}
```

<br>

## 2. Multiple Batch Jobs

- 이 내용을 알게된 계기는 Job을 여러개 실핼시킬 수 있나? 궁금해서 테스트하던 중 알게된 내용입니다.

### 기존

- 저는 Spring Batch 5이전 버전을 사용해본적이 없어서 잘 모르겠지만 기존에는 Multiple Batch Jobs이 가능했던건가? 라는 생각이 듭니다.

### 변경점 

- https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide#multiple-batch-jobs 링크에 Multiple Batch Jobs은 더이상 지원되지 않는다고 명시되어 있습니다.

#### 🧐 그럼 어떻게 여러 Job들을 구동할 수 있을까요?

- JobLauncherApplicationRunner 클래스의 jobLauncherApplicationRunner() 메서드가 실행되지 않도록 해야합니다. 그렇게 하기 위해서는 DefaultBatchConfiguration 클래스를 상속받고 있거나 @EnableBatchProcessing 어노테이션이 선언되어 있거나 yml 파일이나 properties 파일에 spring.batch.job.enabled가 false로 설정되어 있다면 
JobLauncherApplicationRunner 클래스의 jobLauncherApplicationRunner() 메서드가 실행되지 않는데요. 저는 spring.batch.job.enabled를 false로 설정을 하고 스케줄러를 이용했습니다.
- 스케줄러에서 JobLauncher의 run() 메서드를 직접 호출해서 여러 Job들을 실행시킬 수 있습니다.
- 다만 왜 Spring Batch 5에서는 무슨 문제가 있었길래 Multiple Batch Jobs을 지원하지 않는지?에 대해서는 더 공부를 해봐야할 거 같습니다.
  - https://github.com/spring-projects/spring-boot/issues/23411 해당 글에서 Multiple Batch Jobs 지원을 멈춘 이유가 나옵니다.
    1. 로그가 섞이고, 작업 실행 순서가 모호해지고, 종료 코드가 혼란스럽습니다.
    2. 한 가지 작업을 한 가지 작업으로 잘 수행하도록 하는 유닉스 철학에 따라 현재 동작을 변경하여 한 번에 하나의 작업을 실행하도록 하겠습니다.

```java
spring:
  main:
    allow-bean-definition-overriding: true
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3307/spring_batch?serverTimezone=Asia/Seoul
    username: tester
    password: 1234567890

  batch:
    job:
      enabled: false # false로 지정


@Configuration
@RequiredArgsConstructor
public class ExampleJobConfiguration {

    @Bean
    public Job firstJob(JobRepository jobRepository, Step firstStep) {
        return new JobBuilder("firstJob", jobRepository)
                .start(firstStep)
                .build();
    }

    @Bean
    public Job secondJob(JobRepository jobRepository, Step secondStep) {
        return new JobBuilder("secondJob", jobRepository)
                .start(secondStep)
                .build();
    }


    @Bean
    public Step firstStep(JobRepository jobRepository, PlatformTransactionManager manager) {
        return new StepBuilder("firstJob", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("First Step Execute !!!!");
                    return RepeatStatus.FINISHED;
                }, manager)
                .build();
    }

    @Bean
    public Step secondStep(JobRepository jobRepository, PlatformTransactionManager manager) {
        return new StepBuilder("secondJob", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("Second Step Execute !!!!");
                    return RepeatStatus.FINISHED;
                }, manager)
                .build();
    }
}

@Component
@RequiredArgsConstructor
public class ProductScheduler {

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;

    @Bean
    public JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor() {
        var jobRegistryBeanPostProcessor = new JobRegistryBeanPostProcessor();
        jobRegistryBeanPostProcessor.setJobRegistry(jobRegistry);

        return jobRegistryBeanPostProcessor;
    }

    @Scheduled(cron = "0/5 * * * * *") // 5초마다 실행
    public void firstJobRun() {
        try {
            var job = jobRegistry.getJob("firstJob");

            var jobParam = new JobParametersBuilder();
            jobLauncher.run(job, jobParam.toJobParameters());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Scheduled(cron = "0/8 * * * * *") // 8초마다 실행
    public void secondJobRun() {
        try {
            var job = jobRegistry.getJob("secondJob");

            var jobParam = new JobParametersBuilder();
            jobLauncher.run(job, jobParam.toJobParameters());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

<br>

## 3. Execution context serialization Updates

### 기존

- ExecutionContext에 JSON으로 저장이 되었습니다.

### 변경

- Base64로 인코딩된 데이터가 저장됩니다. JSON형식으로 저장하고 싶다면 아래처럼 설정해야합니다.
- jackson-core 의존성을 추가한 뒤 사용할 수 있습니다. 또한 데이터베이스에서 BATCH_~로 시작하는 테이블에 Base64 인코딩된 데이터가 있는 상태에서 JSON 형식으로 저장하고자 한다면 예외가 발생합니다. 즉 해당 설정은 처음부터 설정을 하는게 편리한거 같습니다.

```java
> implementation 'org.springframework.boot:spring-boot-starter-json'

@Configuration
public class BatchConfig {

    @Bean
    public ExecutionContextSerializer jacksonSerializer() {
        return new Jackson2ExecutionContextStringSerializer();
    }
}
```

<br>

## 4. JobBuilderFactory 클래스의 Deprecated

### 기존

- JobBuilderFactory 클래스를 활용하여 Job을 생성했다고 합니다.

### 변경

- JobBuilder 클래스를 활용하여 Job을 생성합니다.























