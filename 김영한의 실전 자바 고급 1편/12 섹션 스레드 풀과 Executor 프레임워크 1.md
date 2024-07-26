# 스레드 풀과 Executor 프레임워크 1

<br>

## 스레드를 직접 생성하여 사용할 때의 문제점들

### 1. 스레드 생성 비용으로 인한 성능 문제

#### 메모리 할당

- 각 스레드는 자신만의 call stack을 가지고 있어야 하며, 이 call stack은 스레드가 실행되는 동안 사용되는 메모리 공간입니다. 따라서 스레드를 생성할 때는 이 call stack을 위해 메모리를 할당해줘야 합니다.

#### 운영체제의 자원 사용

- 스레드를 생성하는 작업은 운영체제의 커널 수준에서 이루어지며 system call을 통해 처리되는데 이는 CPU와 메모리 자원을 소모시킵니다.

#### 운영체제 스케줄러 설정

- 새로운 스레드가 생성되면 운영체제의 스케줄러는 이 스레드를 관리하고 실행 순서를 조정해야하는데. 이때 운영체제 스케줄러 알고리즘에 따라 오버헤드가 발생합니다.

<br>

### 2, 스레드 관리 문제

- 서버의 CPU와 메모리 자원은 한정되어 있기 때문에 스레드를 무한정 생성할 수 없습니다.

<br>

### 3. Runnable 인터페이스의 불편함

- Runnable 인터페이스는 반환값을 가질 수 없으며, 예외를 밖으로 던질 수 없고 매서드 내부에서 처리해야하는 불편함이 있습니다.

```java
public interface Runnable {

    void run();
}
```

<br>

## Future에 대해

#### Future를 사용하지 않고 Runnable 인터페이스만으로 구성

- Future를 사용하지 않고 Runnable 인터페이스를 사용하면 아래와 같이 작성할 수 있습니다.

```java
public class SumTask implements Runnable {

    private final int start;
    private final int end;
    private long result;

    public SumTask(int start, int end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public void run() {
        for (int i = start; i <= end; i++) {
            result += i;
        }
    }

    public long getResult() {
        return result;
    }
}

public class SumTaskMain {

    public static void main(String[] args) throws InterruptedException {
        SumTask sumTaskA = new SumTask(0, 50);
        SumTask sumTaskB = new SumTask(51, 100);

        Thread threadA = new Thread(sumTaskA);
        Thread threadB = new Thread(sumTaskB);

        threadA.start();
        threadB.start();

        threadA.join();
        threadB.join();

        long result = sumTaskA.getResult() + sumTaskB.getResult();
        System.out.println("result: " + result); // 5050
    }
}
```
 
<br>

#### ExecutorService와 Callable 인터페이스를 사용하여 구성

- ExecutorService와 Callable 인터페이스를 사용하면 더욱 더 쉽게 구성할 수 있습니다.

```java
public class SumTaskMainV2 {

    public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        SumTask sumTaskA = new SumTask(0, 50);
        SumTask sumTaskB = new SumTask(51, 100);

        long start = System.currentTimeMillis();
        Future<Long> submitA = executor.submit(sumTaskA);
        Future<Long> submitB = executor.submit(sumTaskB);
        Long resultA = submitA.get();
        Long resultB = submitB.get();
        executor.close();
        
        System.out.println("result: " + (resultA + resultB)); // 5050
    }

    static class SumTask implements Callable<Long> {

        private final int start;
        private final int end;

        public SumTask(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public Long call() throws Exception {
            long result = 0;
            Thread.sleep(1000);

            for (int i = start; i <= end; i++) {
                result += i;
            }
            return result;
        }
    }
}
```
