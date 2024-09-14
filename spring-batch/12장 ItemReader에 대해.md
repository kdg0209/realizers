# ItemReader에 대해

- DB에 관련된 Cursor와 Paging ItemReader에 대해 살펴볼 예정입니다.

<br>

## 1. Cursor 기반에 대해서

### 1-1. 기본 설명

- JDBC ResltSet의 기본 매커니즘을 사용합니다.
- 현재 행에 cursor를 유지하며 다음 데이터를 요청하면 cursor를 한쪽으로 밀면서 데이터 반환이 이루어지는 Streaming 방식의 I/O입니다.
- DB Connection이 맺어지고 배치 처리가 완료될 때까지 데이터를 읽어오기 때문에 DB와 SocketTimeout을 충분히 큰 값으로 유지해야합니다.
- 모든 결과를 메모리에 할당하기 때문에 메모리 사용량이 증가하게 됩니다.
- 즉 DB와 Connection 유지 시간과 메모리 공간이 충분히 크다면 해당 방식으로 대용량의 처리를 할 수 있습니다.

<br>

### 1-2. JdbcCursorItemReader란 무엇인가?

- cursor 기반의 JDBC 구현체로서 ResultSet과 함께 사용되며 Datasource에서 Connection을 얻어와 SQL을 실행합니다.
- Thread-Safe 하지 않기 때문에 멀티 스레드 환경에서 동시성 문제가 발생하지 않도록 별도의 동기화 처리가 필요합니다.

<img width="1032" alt="스크린샷 2024-09-14 오후 2 52 11" src="https://github.com/user-attachments/assets/97cc1558-978d-4133-b269-7101e9090dce">

<br>
<br>

#### 예제 코드

```java
@Getter
@Setter
public class Product {

    private Long id;
    private String name;
    private Long price;
    private Long quantity;
    private LocalDateTime createDatetime;
}

@Configuration
@RequiredArgsConstructor
public class JdbcCursorJobConfiguration {

    private static final int CHUNK_SIZE = 10;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager manager;
    private final DataSource dataSource;

    @Bean
    public Job jdbcCursorJob() {
        return new JobBuilder("jdbcCursorJob", jobRepository)
                .start(jdbcCursorStepA())
                .build();
    }

    @Bean
    public Step jdbcCursorStepA() {
        return new StepBuilder("jdbcCursorStepA", jobRepository)
                .<Product, Product>chunk(5, manager)
                .reader(jdbcItemReader())
                .writer(jdbcitemWriter())
                .build();
    }

    @Bean
    public ItemReader<Product> jdbcItemReader() {
        return new JdbcCursorItemReaderBuilder<Product>()
                .name("jdbcCursorItemReader")
                .fetchSize(CHUNK_SIZE)
                .sql("select id, name, price, quantity, create_datetime from product")
                .beanRowMapper(Product.class)
                .dataSource(dataSource)
                .build();
    }

    @Bean
    public ItemWriter<Product> jdbcitemWriter() {
        return System.out::println;
    }
}
```

<br>

### 1-3. JpaCursorItemReader란 무엇인가?

- Spring Batch 4.3 버전부터 지원을 합니다.
- Cursor 기반의 JPA 구현체로서 EntityManagerFactory가 필요로 하며 쿼리는 JPQL을 사용합니다.

```java
@Getter
@Entity(name = "product")
@Table
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Long price;
    private Long quantity;
    private LocalDateTime createDatetime;
}

@Configuration
@RequiredArgsConstructor
public class JPACursorJobConfiguration {

    private static final int CHUNK_SIZE = 10;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager manager;
    private final EntityManagerFactory factory;

    @Bean
    public Job jpaCursorJob() {
        return new JobBuilder("jpaCursorJob", jobRepository)
                .start(jpaCursorStepA())
                .build();
    }

    @Bean
    public Step jpaCursorStepA() {
        return new StepBuilder("jpaCursorStepA", jobRepository)
                .<Product, Product>chunk(CHUNK_SIZE, manager)
                .reader(jpaItemReader())
                .writer(jpaItemWriter())
                .build();
    }

    @Bean
    public ItemReader<Product> jpaItemReader() {
        return new JpaCursorItemReaderBuilder<Product>()
                .name("jpaCursorItemReader")
                .entityManagerFactory(factory)
                .queryString("SELECT p FROM product p")
                .build();
    }

    @Bean
    public ItemWriter<Product> jpaItemWriter() {
        return System.out::println;
    }
}
```

<br>

## 2. Paging 기반에 대해서

### 2-1. 기본 개념

- 페이징 단위로 데이터를 조회하는 방식으로 page size 만큼 한번에 메모리로 가져온 후 한개씩 데이터를 읽습니다.
- 한 page size 만큼 데이터를 읽고 connection을 끊기 때문에 대량의 데이터를 처리하더라도 SocketTimeout 예외가 거의 발생하지 않습니다.
- 페이징 단위의 결과만 메모리에 적재하므로 메모리 사용량이 상대적으로 적습니다.
- DB와 connection 유지 시간이 길지 않고 메모리 공간을 효율적으로 사용해야하는 경우에 적합합니다.

<br>

### 2-2. JdbcPagingItemReader란 무엇일까?

- Paging 기반의 JDBC의 구현체로서 쿼리에 offet(시작점)과 limit(반환할 행 수 )를 사용하여 SQL을 실행시킵니다.
- 스프링 배치에서 page와 limit를 page size에 맞게 자동으로 생성해주며 페이징 단위로 데이터를 조회할 수 있도록 지원합니다.
- 페이지마다 새로운 쿼리를 실행시키기 때문에 페이징 시 결과 데이터의 순서가 보장될 수 있도록 order by 구문이 작성되어야 합니다.
- 멀티 스레드 환경에서 thread-safe 하므로 별도의 동기화 작업이 필요없습니다.

#### 예제 코드
```java
@Configuration
@RequiredArgsConstructor
public class JdbcPagingJobConfiguration {

    private static final int CHUNK_SIZE = 10;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager manager;
    private final DataSource dataSource;

    @Bean
    public Job jdbcPagingJob() throws Exception {
        return new JobBuilder("jdbcPagingJob", jobRepository)
                .start(jdbcPagingStepA())
                .build();
    }

    @Bean
    public Step jdbcPagingStepA() throws Exception {
        return new StepBuilder("jdbcPagingStepA", jobRepository)
                .<Product, Product>chunk(CHUNK_SIZE, manager)
                .reader(jdbcPagingItemReader())
                .writer(jdbcPagingItemWriter())
                .build();
    }

    @Bean
    public ItemReader<Product> jdbcPagingItemReader() throws Exception {
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("id", 45);

        return new JdbcPagingItemReaderBuilder<Product>()
                .name("jdbcPagingItemReader")
                .fetchSize(CHUNK_SIZE)
                .pageSize(CHUNK_SIZE)
                .queryProvider(jdbcPagingQueryProvider())
                .parameterValues(parameterValues)
                .beanRowMapper(Product.class)
                .dataSource(dataSource)
                .build();
    }

    @Bean
    public ItemWriter<Product> jdbcPagingItemWriter() {
        return System.out::println;
    }

    @Bean
    public PagingQueryProvider jdbcPagingQueryProvider() throws Exception {
        SqlPagingQueryProviderFactoryBean queryProvider = new SqlPagingQueryProviderFactoryBean();
        queryProvider.setDataSource(dataSource);
        queryProvider.setSelectClause("id, name, price, quantity, create_datetime");
        queryProvider.setFromClause("from product");
        queryProvider.setWhereClause("where id >= :id");

        Map<String, Order> sortKeys = new HashMap<>(1);
        sortKeys.put("id", Order.DESCENDING);

        queryProvider.setSortKeys(sortKeys);

        return queryProvider.getObject();
    }
}
```

<br>

### 2-3. JdbcPagingItemReader란 무엇일까?

- Paging 기반의 JPA 구현체로서 EntityManagerFactory가 필요로 하며 쿼리는 JPQL을 사용합니다.

#### 예제 코드

```java
@Configuration
@RequiredArgsConstructor
public class JPAPagingJobConfiguration {

    private static final int CHUNK_SIZE = 10;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager manager;
    private final EntityManagerFactory factory;

    @Bean
    public Job jpaPagingJob() {
        return new JobBuilder("jpaPagingJob", jobRepository)
                .start(jpaPagingStepA())
                .build();
    }

    @Bean
    public Step jpaPagingStepA() {
        return new StepBuilder("jdbcPagingStepA", jobRepository)
                .<Product, Product>chunk(CHUNK_SIZE, manager)
                .reader(jpaPagingItemReader())
                .writer(jpaPagingItemWriter())
                .build();
    }

    @Bean
    public ItemReader<Product> jpaPagingItemReader(){
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("id", 45);

        return new JpaPagingItemReaderBuilder<Product>()
                .name("jpaPagingItemReader")
                .queryString("SELECT p FROM product p WHERE id >= :id")
                .parameterValues(parameterValues)
                .pageSize(CHUNK_SIZE)
                .entityManagerFactory(factory)
                .build();
    }

    @Bean
    public ItemWriter<Product> jpaPagingItemWriter() {
        return System.out::println;
    }
}
```

<br>

#### 참고

- https://jojoldu.tistory.com/337


