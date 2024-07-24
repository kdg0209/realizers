# CAS - 동기화와 원자적 연산

- 컴퓨터 과학에서 원자적 연산이란 해당 연산이 더 이상 나눌 수 없는 단위로 수행된다는 것을 의미합니다. 즉 원자적 연산은 중단되지 않고 다른 연산과 관섭없이 완전히 실행되거나 아예 실행되지 않는 성질을 가지고 있습니다.

<br>

## 여러 스레드가 하나의 변수를 공유하는 경우

#### 예제 코드

- 아래는 BasicInteger 클래스의 increment() 메서드를 멀티 스레드환경에서 호출하는 예제입니다. result 값을 보면 정상적으로 10000이 나와야 하는데, 9999가 출력된것을 볼 수 있는데, 여기서 동시성 문제가 발생한것을 확인할 수 있습니다.
- this.value++; 연산은 원자적 연산이 아닌 3단계를 걸쳐 값이 증가하게 되는데 다음과 같이 진행되게 됩니다. (1. 메모리에서 value 변수를 불러온다 > 2. value 변수를 1증가 시킨다 > 3. value 변수를 메모리에 저장한다.)

```java
public interface IncrementInteger {

    void increment();

    int get();
}

public class BasicInteger implements IncrementInteger {

    private int value;

    @Override
    public void increment() {
        this.value++;
    }

    @Override
    public int get() {
        return this.value;
    }
}

public class Main {

    private static final int THREAD_COUNT = 10000;

    public static void main(String[] args) throws InterruptedException {

        IncrementInteger incrementInteger = new BasicInteger();
        test(incrementInteger);
    }

    private static void test(IncrementInteger incrementInteger) throws InterruptedException {
        Runnable runnable = incrementInteger::increment;

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            Thread thread = new Thread(runnable);
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        int result = incrementInteger.get();
        System.out.println("result: " + result);
    }
}

// 결과
result: 9999
```

<br>

### 🧐 volatile 키워드를 사용하면 해결할 수 있을까?

- 사실 volatile 키워드를 선언하더라도 동시성은 보장받을 수 없습니다. volatile는 가시성만 보장받을 수 있으며, 멀티 스레드 환경에서 하나의 변수에 읽기 스레드가 여러개이고, 쓰기 스레드가 여러개인 상황에서는 volatile 키워드는 무용지물이 됩니다.
- volatile는 CPU와 메인 메모리 사이에 있는 캐시 메모리를 무시하고, 메인 메모리를 사용하도록 하지만 메인 메모리에 접근하는 여러 스레드로 인해 발생되게 됩니다.

```java
public class BasicInteger implements IncrementInteger {

    private volatile int value;

    @Override
    public void increment() {
        this.value++;
    }

    @Override
    public int get() {
        return this.value;
    }
}
```

<br>

### 🧐 synchronized 키워드를 사용하면 해결할 수 있을까?

- synchronized 키워드는 가시성 보장뿐만 아니라 동시성 또한 보장받을 수 있습니다.

```java
public class BasicInteger implements IncrementInteger {

    private int value;

    @Override
    public synchronized void increment() {
        this.value++;
    }

    @Override
    public int get() {
        return this.value;
    }
}
```

<br>

## AtomicInteger에 대해

- 자바에서는 java.util.concurrent.atomic 패키지에 있는 여러 AtomicXXX 클래스를 제공하고 있으므로 상황에 맞게 사용할 수 있습니다.

```java
public class MyAtomicInteger implements IncrementInteger {

    private AtomicInteger value = new AtomicInteger();

    @Override
    public void increment() {
        this.value.incrementAndGet();
    }

    @Override
    public int get() {
        return this.value.get();
    }
}
```

<br>

### 각 성능은 어떨까?

#### 1. 아무것도 하지 않은 경우

- ms: 100 정도 소요가 됩니다.
- CPU 캐시를 적극적으로 사용하고, 동시성을 보장하지 않기 때문입니다.

```java
public class BasicInteger implements IncrementInteger {

    private int value;

    @Override
    public void increment() {
        this.value++;
    }

    @Override
    public int get() {
        return this.value;
    }
}
```

<br>

#### 2. volatile 키워드를 사용하는 경우

- ms: 804 정도 소요가 됩니다.
- 캐시 메모리를 사용하지 않고 메인 메모리에 직접적으로 접근하기 때문에 시간이 다소 소요됩니다.
- 안전한 임계 영역이 없기 때문에 멀티 스레드 환경에서는 사용될 수 없습니다.

```java
public class VolatileInteger implements IncrementInteger {

    private volatile int value;

    @Override
    public void increment() {
        this.value++;
    }

    @Override
    public int get() {
        return this.value;
    }
}
```

<br>

#### 3. synchronized 키워드를 사용하는 경우

- ms: 1613 정도 소요가 됩니다.
- 안전한 임계 영역이 존재하고, 멀티 스레드 환경에서 사용될 수 있습ㄴ디ㅏ.

```java
public class SynchronizedInteger implements IncrementInteger {

    private int value;

    @Override
    public synchronized void increment() {
        this.value++;
    }

    @Override
    public int get() {
        return this.value;
    }
}
```

<br>

#### 4. ReentrantLock을 사용하는 경우

- ms: 1753 정도 소요가 됩니다.
- synchronized 키워드를 사용한 경우와 비슷한 소요시간이 걸리는 것을 알 수 있습니다. 동작하는게 비슷하기 때문입니다.

```java
public class ReentrantLockInteger implements IncrementInteger {

    private int value;
    private final Lock lock = new ReentrantLock();

    @Override
    public void increment() {
        lock.lock();

        try {
            this.value++;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int get() {
        return this.value;
    }
}
```

<br>

#### 5. AtomicInteger를 사용하는 경우

- ms: 673 정도 소요가 됩니다.
- 성능은 synchronized와 Lock(ReentrantLock)을 사용한 경우보다 빠릅니다.

```java
public class MyAtomicInteger implements IncrementInteger {

    private AtomicInteger value = new AtomicInteger();

    @Override
    public void increment() {
        this.value.incrementAndGet();
    }

    @Override
    public int get() {
        return this.value.get();
    }
}
```

<br>

#### 🤔 왜 AtomicInteger가 더 빠를까?

- AtomicInteger는 락을 사용하지 않고 원자적 연산을 만들어 냅니다. 이제 CAS 연산에 대해 알아볼 시간입니다.

<br>

## CAS 연산에 대해

### 락 기반 방식의 문제점

- 락을 사용하면 왜 느릴까요? 락을 사용하면 특정 자원을 사용하는 것을 보호허기 위해 임계 영역에 접근하는 것을 제한합니다. 또한 락을 획득하여 사용하는 동안 다른 스레드들은 락을 얻기 위해 대기하게 되는데 이때 오버헤드가 발생하여 성능적으로 느려지게 됩니다.

### CAS

- CAS(Compare-And-SWAP, Compare-And-Set)연산이라고 하는데, 이 방법은 락을 사용하지 않기 때문에 락 프리(Lock Free)기법이라고도 합니다.
- 참고로 CAS 연산은 락을 완전히 대체하는 것은 아니고, 작은 단위의 일부 영역에 적용할 수 있습니다.
- CAS 연산은 현재 스레드가 가진 값을 CPU 캐시와 메인 메모리의 두 값과 비교하고, 그 값이 모두 동일한 경우에만 메인 메모리에 새로운 값을 저장하고, 일치 하지 않으면 재시도를 하게 됩니다.
- CAS 연산은 메인 메모리에 값을 저장할 때 실패 후 재시도할 수 있으므로 동시적으로 접근하는 스레드들이 많다면 효율성이 저하될 수 있습니다.
- CAS 연산은 하드웨어 수준에서 지원하는 단일 연산입니다.

#### CPU 하드웨어의 지원

- CAS 연산은 원자적이지 않은 두 연산을 CPU 하드웨어 차원에서 특별하게 하나의 원자적인 연산으로 묶어서 제공하는 기능입니다.
- 이것은 소프트웨어 기능이 제공하는게 아니라 하드웨어가 제공하는 기능입니다. 대부분은 현대 CPU들은 CAS 연산을 제공하고 있습니다.

#### Lock 방식

- 비관적 접근법입니다.
- 데이터에 접근하기 전에 락을 획득하고, 다른 스레드의 접근을 막습니다.

#### CAS 방식

- 낙관적 접근법입니다.
- 락을 사용하지 않고 바로 데이터에 접근하고, 스레드 간 충돌이 발생하면 재시도합니다.

<br>

### CAS 락 구현

#### 잘못된 예제 코드

- 아래 결과를 보면 시도 시도 -> 성공 성공 -> 실행 실행 -> 반납 반납 이렇게 동작이 됩니다. 정상적으로 될려면 시도 -> 시도 -> 실행 -> 반납 -> 시도 -> 실행 -> 반납 이렇게 흐름을 가져가야하는데 말이죠.
- 즉, 멀티 스레드 환경에서 임계 영역이 안전하게 지켜지지 못 했습니다.

```java
public class SpinLock {

    private volatile boolean lock = false;

    public void lock() {
        System.out.println(Thread.currentThread().getName() + " 락 획득 시도");

        while (true) {
            if (!this.lock) {
                sleep();
                this.lock = true;
                break;
            }
            System.out.println(Thread.currentThread().getName() + " 락 획득 실패 - 스핀 대기");
        }
        System.out.println(Thread.currentThread().getName() + " 락 획득 성공");
    }

    public void unlock() {
        this.lock = false;
        System.out.println(Thread.currentThread().getName() + " 락 반납 완료");
    }

    private void sleep() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {}
    }
}

public class SpinLockMain {

    public static void main(String[] args) {
        SpinLock lock = new SpinLock();

        Runnable runnable = () -> {
            lock.lock();

            try {
                System.out.println(Thread.currentThread().getName() + " 비지니스 로직 실행");
            } finally {
                lock.unlock();
            }
        };

        Thread threadA = new Thread(runnable);
        Thread threadB = new Thread(runnable);

        threadA.start();
        threadB.start();
    }
}

// 결과
Thread-0 락 획득 시도
Thread-1 락 획득 시도
Thread-0 락 획득 성공
Thread-1 락 획득 성공
Thread-1 비지니스 로직 실행
Thread-0 비지니스 로직 실행
Thread-0 락 반납 완료
Thread-1 락 반납 완료
```

<br>

#### 다시 제대로 구현해봅시다.

- AtomicBoolean 클래스를 사용하여 락을 얻는 과정을 원자적으로 수행함으로써 위 문제를 해결할 수 있습니다.

```java
public class SpinLock {

    private final AtomicBoolean lock = new AtomicBoolean(false);

    public void lock() {
        System.out.println(Thread.currentThread().getName() + " 락 획득 시도");

        while (!lock.compareAndSet(false, true)) {
            System.out.println(Thread.currentThread().getName() + " 락 획득 실패 - 스핀 대기");
        }

        System.out.println(Thread.currentThread().getName() + " 락 획득 성공");
    }

    public void unlock() {
        lock.set(false);
        System.out.println(Thread.currentThread().getName() + " 락 반납 완료");
    }
}
// 결과
Thread-0 락 획득 시도
Thread-1 락 획득 시도
Thread-0 락 획득 성공
Thread-1 락 획득 실패 - 스핀 대기
Thread-1 락 획득 실패 - 스핀 대기
Thread-1 락 획득 실패 - 스핀 대기
Thread-0 비지니스 로직 실행
Thread-1 락 획득 실패 - 스핀 대기
Thread-1 락 획득 성공
Thread-1 비지니스 로직 실행
Thread-1 락 반납 완료
Thread-0 락 반납 완료
```

<br>

#### CAS 단점

- 락을 기다리는 스레드가 BLOCKED, WAITING 상태로 변경되진 않지만 RUNNABLE 상태에서 락을 획득하고자 반복문을 계속 순회하는 문제가 발생합니다. 따라서 락을 기다리는 스레드가 CPU를 계속 사용하면서 대기하게 되고 CPU 사용률이 증가하게 됩니다.
- BLOCKED, WAITING 상태의 스레드는 CPU를 거의 사용하지 않습니다.
- 개인적인 생각으로는 스레드가 많은 상황에서 락을 획득해야한다면 Lock 방식을 사용하는 게 나을거 같고, 스레드가 적은 상황에서는 CAS 연산을 사용해도 될거 같다. 또한 I/O 버스트 보다는 CPU 버스트를 더 많이 사용하는 경우에 CAS 연산이 더 적절할 거 같다!

<br>

## 정리

### CAS 연산

#### 장점

- 낙관적 동기화
    - 락을 걸지 않고도 값을 안전하게 변경할 수 있습니다. CAS는 스레드간 충돌이 자주 발생하지 않을 거라 가정하고, 스레드간 충돌이 적은 경우에 적합합니다.
- 락 프리
    - CAS는 락을 사용하지 않기 때문에 락을 획득하기 위해 대기하는 시간이 없습니다. 따라서 스레드가 블록킹되지 않으며, 병렬 처리가 더 효율적일 수 있습니다.

#### 단점

- 충돌이 빈번한 경우
    - 여러 스레드가 동시에 동일한 변수에 접근하여 값 변경을 시도할 때 충돌이 발생할 수 있습니다. 충돌이 발생하면 CAS는 재시도를 해야하며, 이에 따라 CPU 자원을 추가적으로 소모할 수 있습니다. 이로인해 오버헤드가 증가됩니다.
- 스핀락과 유사한 오버헤드
    - CAS는 충돌시 재시도 하므로 이 과정이 반복되면 스핀락과 유사한 성능 저하가 발생할 수 있습니다.

<br>

### 동기화락(synchronized, ReentrantLock)

#### 장점

- 임계 영역에 하나의 스레드만 접근할 수 있기 때문에 충돌이 발생하지 않으며, 안정적입니다. 또한 스핀락을 사용하지 않기 때문에 CPU 사용률이 증가되지 않습니다.

#### 단점

- 스레드가 임계 영역에 진입하기 위해서는 락이 필요한데, 락을 얻고자 대기하는 시간이 증가됨에 따라 오버헤드가 증가되고, 락을 사용하고 반납하고 이러한 과정에서 스레드의 상태가 변경되기 때문에 컨텍스트 스위칭 비용이 증가되게 돱니다.

<br>

#### 참고

- https://github.com/kdg0209/realizers/blob/main/inflearn-java-concurrency-programming/CAS(Compare%20and%20Swap).md


