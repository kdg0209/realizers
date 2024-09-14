# ItemWriter에 대해

<br>

## 1. JdbcBatchItemWriter란 무엇인가?

#### 1-1. 기본 개념

- JDBC의 배치 기능을 통해 bulk insert/update/delete 방식으로 처리합니다.
- 단일 처리가 아닌 일괄 처리이기 때문에 I/O 횟수를 줄여 성능상 이점이 있습니다.

#### 1-2. 아키텍처

- JdbcBatchItemWriter 클래스에서 어떤 메서드를 호출하느냐에 따라 어떻게 SQL의 쿼리로 변환하는지 아래 그림으로 확인할 수 있습니다.

<img width="1032" alt="스크린샷 2024-09-14 오후 5 32 38" src="https://github.com/user-attachments/assets/52a6c039-0be4-47d8-97ca-23adfd03d517">

<br>
<br>

#### 1-3. 예제 코드

```java

@Configuration
@RequiredArgsConstructor
public class CustomJdbcBatchItemWriter {

    private static final int CHUNK_SIZE = 10;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager manager;
    private final DataSource dataSource;

    @Bean
    public Job jdbcBatchItemWriterJob() throws Exception {
        return new JobBuilder("jdbcBatchItemWriterJob", jobRepository)
                .start(jdbcBatchItemWriterStep())
                .build();
    }

    @Bean
    public Step jdbcBatchItemWriterStep() throws Exception {
        return new StepBuilder("jdbcBatchItemWriterStep", jobRepository)
                .<Product, Product>chunk(CHUNK_SIZE, manager)
                .reader(jdbcBatchItemReader())
                .writer(jdbcBatchItemWriter())
                .build();
    }

    @Bean
    public ItemReader<Product> jdbcBatchItemReader() throws Exception {
        return new JdbcPagingItemReaderBuilder<Product>()
                .name("jdbcCursorItemReader")
                .fetchSize(CHUNK_SIZE)
                .pageSize(CHUNK_SIZE)
                .queryProvider(jdbcBatchItemQueryProvider())
                .beanRowMapper(Product.class)
                .dataSource(dataSource)
                .build();
    }

    @Bean
    public ItemWriter<Product> jdbcBatchItemWriter() {
        return new JdbcBatchItemWriterBuilder<Product>()
                .dataSource(dataSource)
                .sql("INSERT INTO product2 (id, name, price, quantity, create_datetime) VALUES (:id, :name, :price, :quantity, :createDatetime)")
                .beanMapped()
                .build();
    }

    @Bean
    public PagingQueryProvider jdbcBatchItemQueryProvider() throws Exception {
        SqlPagingQueryProviderFactoryBean queryProvider = new SqlPagingQueryProviderFactoryBean();
        queryProvider.setDataSource(dataSource);
        queryProvider.setSelectClause("id, name, price, quantity, create_datetime");
        queryProvider.setFromClause("from product");

        Map<String, Order> sortKeys = new HashMap<>(1);
        sortKeys.put("id", Order.DESCENDING);

        queryProvider.setSortKeys(sortKeys);

        return queryProvider.getObject();
    }
}
```

<br>

## 2. JpaItemWriter란 무엇인가?

#### 2-1. 기본 개념

- JPA Entity를 기반으로 데이터를 처리하며 EntityManagerFactory를 주입받아 사용합니다.
- Entity를 하나씩 Chunk 크기만큼 Insert 혹은 Merge한 다음 flush 합니다.
- ItemReader나 ItemProcessor로부터 데이터를 전달받을 때 Entity 클래스 타입으로 받아야 합니다.

<br>

#### 참고

- https://jojoldu.tistory.com/339


