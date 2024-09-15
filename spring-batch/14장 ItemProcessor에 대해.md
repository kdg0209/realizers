# ItemProcessor에 대해

<br>

## 1. CompositeItemProcessor이란 무엇인가?

- ItemProcessor를 연결하여 각 ItemProcessor들을 실행시킵니다.
- 이전 ItemProcessor의 반환값은 ItemProcessor의 입력값으로 주어집니다.

#### 흐름도
 
<img width="1010" alt="스크린샷 2024-09-15 오후 1 17 57" src="https://github.com/user-attachments/assets/a25dde8d-89e3-412b-aa36-42617dd27a8e">

<br>
<br>

#### 예제 코드

```java
@Configuration
@RequiredArgsConstructor
public class CompositeItemProcessorConfiguration {

    private static final int CHUNK_SIZE = 2;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager manager;

    @Bean
    public Job itemProcessorJob() {
        return new JobBuilder("itemProcessorJob", jobRepository)
                .start(itemProcessorStep())
                .build();
    }

    @Bean
    public Step itemProcessorStep() {
        return new StepBuilder("itemProcessorStep", jobRepository)
                .<String, String>chunk(CHUNK_SIZE, manager)
                .reader(new ListItemReader<>(Arrays.asList("item", "item", "item", "item", "item")))
                .processor(customCompositeItemProcessor())
                .writer(System.out::println)
                .build();
    }

    @Bean
    public CompositeItemProcessor customCompositeItemProcessor() {
        List<ItemProcessor> processors = new ArrayList<>();
        processors.add(new CustomItemProcessor1());
        processors.add(new CustomItemProcessor2());

        CompositeItemProcessor itemProcessor = new CompositeItemProcessor<>();
        itemProcessor.setDelegates(processors);

        return itemProcessor;
    }

    @Bean
    public ItemProcessor<String, String> itemProcessor1() {
        return new CustomItemProcessor1();
    }

    @Bean
    public ItemProcessor<String, String> itemProcessor2() {
        return new CustomItemProcessor2();
    }
}

public class CustomItemProcessor1 implements ItemProcessor<String, String> {

    private int index = 0;

    @Override
    public String process(String item) {
        return item + index++;
    }
}

public class CustomItemProcessor2 implements ItemProcessor<String, String> {

    private int index = 0;

    @Override
    public String process(String item) {
        return item.toUpperCase() + index++;
    }
}
```

<br>

## 2. ClassifierCompositeItemProcessor란 무엇인가?

- Classifier로 라우팅 패턴을 구현하여 ItemProcessor 구현체 중 하나를 호출할 수 있는 역할을 수행합니다.

#### 흐름도

<img width="1014" alt="스크린샷 2024-09-15 오후 1 48 20" src="https://github.com/user-attachments/assets/97407a57-260a-454b-859f-61267d415296">

<br>
<br>

#### 예제 코드

```java
public record ProcessorData(int id) {

}

public class ProcessorClassifier<C, T> implements Classifier<C, T> {

    private final Map<Integer, ItemProcessor<?, ? extends ProcessorData>> map;

    public ProcessorClassifier(Map<Integer, ItemProcessor<?, ? extends ProcessorData>> map) {
        this.map = map;
    }

    @Override
    public T classify(C c) {
        ProcessorData data = (ProcessorData) c;
        return (T) map.get(data.id());
    }
}

public class CustomItemProcessor1 implements ItemProcessor<ProcessorData, ProcessorData> {

    @Override
    public ProcessorData process(ProcessorData data) {
        System.out.println("CustomItemProcessor1: " + data);
        return data;
    }
}

public class CustomItemProcessor2 implements ItemProcessor<ProcessorData, ProcessorData> {

    @Override
    public ProcessorData process(ProcessorData data) {
        System.out.println("CustomItemProcessor2: " + data);
        return data;
    }
}

public class CustomItemProcessor3 implements ItemProcessor<ProcessorData, ProcessorData> {

    @Override
    public ProcessorData process(ProcessorData data) {
        System.out.println("CustomItemProcessor3: " + data);
        return data;
    }
}

@Configuration
@RequiredArgsConstructor
public class ClassifierCompositeItemProcessorConfiguration {

    private static final int CHUNK_SIZE = 2;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager manager;

    @Bean
    public Job itemClassifierProcessorJob() {
        return new JobBuilder("itemClassifierProcessorJob", jobRepository)
                .start(itemClassifierProcessorStep())
                .build();
    }

    @Bean
    public Step itemClassifierProcessorStep() {
        return new StepBuilder("itemClassifierProcessorStep", jobRepository)
                .<ProcessorData, ProcessorData>chunk(CHUNK_SIZE, manager)
                .reader(new ItemReader<>() {
                    int index = 0;

                    @Override
                    public ProcessorData read() {
                        index++;
                        ProcessorData data = new ProcessorData(index);
                        return index > 3 ? null : data;
                    }
                })
                .processor(customItemProcessor())
                .writer(System.out::println)
                .build();
    }

    @Bean
    public ItemProcessor customItemProcessor() {
        ClassifierCompositeItemProcessor<ProcessorData, ProcessorData> processor = new ClassifierCompositeItemProcessor<>();

        HashMap<Integer, ItemProcessor<?, ? extends ProcessorData>> map = new HashMap<>();
        map.put(1, new CustomItemProcessor1());
        map.put(2, new CustomItemProcessor2());
        map.put(3, new CustomItemProcessor3());

        ProcessorClassifier<ProcessorData, ItemProcessor<?, ? extends ProcessorData>> processorClassifier = new ProcessorClassifier<>(map);
        processor.setClassifier(processorClassifier);
        return processor;
    }
}
```


