# JVM과 스레드

<br>

## 플랫폼 스레드와 커널 스레드

#### 플랫폼 스레드

- 자바의 스레드는 `플랫폼 스레드`라고도 불립니다.
- 자바에서 기본적으로 `Thread` 클래스를 사용하여 만들어지는 스레드는 `플랫폼 스레드`입니다. 이러한 플랫폼 스레드는 운영체제의 커널 스레드와 1:1 매핑되어 실행됩니다. (`One-to-One Threading Model`)
- 즉, JVM이 스레드를 관리하지만 실질적인 실행은 1:1 매핑된 커널 스레드가 합니다.
- 사용자 공간에서만 실행 가능한 스레드입니다. 운영체제의 도움이 필요한다면 커널 스레드에게 system call을 보내고 커널 스레드가 특정 작업을 수행 후 결과값을 반환해줍니다.

#### 커널 스레드

- 커널 스레드는 운영체제 커널이 직접 관리하는 스레드입니다. 즉, 운영체제의 스케줄러가 생성하고 관리하고 컨텍스트 스위칭하는 스레드를 의미합니다.
- 커널 공간에서만 실행가능합니다.

#### 가상 스레드

- 가상 스레드는 운영체제의 커널 스레드와 1:1 매핑되지 않고, JVM이 자체적으로 관리하는 경량 스레드입니다.
- 가상 스레드는 커널 스레드를 직접적으로 사용하는게 아니고 다수의 가상 스레드를 소수의 커널 스레드에서 실행하는 방식입니다.(M:N 매핑)
- 가상 스레드의 상태가 `BLOCKED` 되더라도 커널 스레드를 점유하고 있는게 아니기 때문에 1:1 매핑에서의 블로킹 I/O 문제를 해결할 수 있습니다.

<br>

## Java 객체에서 Lock은 어디에 있나?

- JVM에서 모든 자바 객체는 `Header`라는 메타데이터를 포함하고 있습니다. 객체의 Header에는 JVM이 관리하는데 필요한 정보가 저장되는데 여기에 `Mark word`라는 헤더 부분에 Lock 정보가 담겨져 있습니다.

#### Mark word란?

- 자바 객체의 `Mark word`는 64비트 또는 32비트 크기의 필드로 여러가지 정보를 저장하는데 이 정보는 JVM이 실행중에 동적으로 변경될 수 있습니다. Mark word의 저장 내용은 아래와 같습니다.

<img width="1032" alt="스크린샷 2025-03-13 오후 10 09 04" src="https://github.com/user-attachments/assets/b0655e88-2d36-429b-9483-eb860515c4a7" />

<br>

#### Lock과 Mark word의 관계

- 자바에서 `synchronized` 키워드나 `Lock`을 사용할 때 객체의 Mark word가 변경되면서 Lock이 설정됩니다.
  - 처음에는 Mark word에 아무런 Lock정보가 없습니다. (`Unlocked` 상태)
  - 스레드가 임계 영역에 진입하면 Mark word에 Lock 정보가 저장됩니다. 이때 Lock 종류에 따라 `Thin Lock`, `Fat Lock`으로 설정될 수 있습니다.
  - Lock이 해제되면 Mark word의 Lock 정보는 `Unlocked` 상태가 됩니다.

#### 자바의 Monitor란?

- 운영체제에서 `Monitor`는 Lock보다 높은 추상화로 임계 영역에 한 번에 하나의 스레드만 진입할 수 있도록 프로그래밍 언어에서 제공하는 동기화 기법을 의미합니다.
- `synchronized` 키워드나 `Lock`을 사용할 때 객체의 `Monitor`가 활성화되어 동기화가 이루어집니다.
- 즉, Monitor는 특정 객체에 의해 동적으로 사용되며 Mark word의 Lock 정보를 활용하여 동작하게 됩니다. (처음에는 모든 객체에 Monitor가 있는 줄 알았는데 임계 영역에 접근할 때만 Monitor가 활성화 된다고 합니다.) 

```java
// synchronized 블록 사용 시
class Example {

    private final Object lock = new Object(); // 일반 객체

    public void syncMethod() {
        synchronized (lock) { // lock 객체가 Monitor를 가지게 됨
            System.out.println("임계 영역 실행 중...");
        }
    }
}
```

<br>

#### synchronized 메서드 사용 시

- this 객체의 Monitor가 활성화됩니다.
- 즉, 각각의 인스턴스별로 존재하게 됩니다.

```java
public class Main {

    public static void main(String[] args) {
        Test obj1 = new Test();
        Test obj2 = new Test();

        new Thread(() -> obj1.syncMethod(), "Thread-1").start();
        new Thread(() -> obj2.syncMethod(), "Thread-2").start();
    }
}

class Test {

    public synchronized void syncMethod() { // this 객체에 Lock
        System.out.println(Thread.currentThread().getName() + " 실행 중...");
        try { Thread.sleep(1000); } catch (InterruptedException e) { }
    }
}

// 결과
Thread-2 실행 중...
Thread-1 실행 중...
```

<br>

#### 클래스(static) 수준의 synchronized 메서드 사용 시

- 모든 인스턴스가 공유하는 하나의 객체입니다.
- 클래스 전체에 Lock을 걸게되므로 모든 인스턴스가 공유하게 됩니다. 그렇기 때문에 여러 인스턴스가 동시에 접근할 수 없게 됩니다. 아래 결과를 보면 항상 똑같은 결과가 도출됩니다.

```java
public class Main {

    public static void main(String[] args) {
        Test obj1 = new Test();
        Test obj2 = new Test();

        new Thread(() -> obj1.syncMethod(), "Thread-1").start();
        new Thread(() -> obj2.syncMethod(), "Thread-2").start();
    }
}

class Test {

    public static synchronized void syncMethod() { // Test.class에 Lock
        System.out.println(Thread.currentThread().getName() + " 실행 중...");
        try { Thread.sleep(1000); } catch (InterruptedException e) { }
    }
}

// 결과
Thread-1 실행 중...
Thread-2 실행 중...
```

<br>

#### EntrySet

- `EntrySet`은 모니터 락을 얻기 위해 대기중인 스레드들의 집합입니다.
- 특정 스레드가 락을 획득하여 사용중이라면 락을 획득하고자 하는 스레드들은 `EntrySet`에 들어가 대기하게 됩니다.
- 락을 사용하던 스레드가 락을 반납하면 `EntrySet`에 있는 스레드 중 하나가 락을 획득하게 됩니다.

#### WaitSet

- 모니터를 소유하고 있던 스레드가 `wait()` 메서드를 호출하면 락을 해제한 후 `WaitSet`에 들어가 대기하게 됩니다.
- `notify()` 메서드나 `notifyAll()` 메서드가 호출되면 `WaitSet`에서 깨어난 스레드가 `EntrySet`으로 이동하여 다시 락을 획득하기 위해 대기하게 됩니다.

<br>

## 하드웨어 수준의 캐시 일관성 이슈

- 스레드 A,B가 메인 메모리에서 데이터를 가져오면 캐시 메모리를 거치게되고, 특정 스레드에서 값을 변경하면 바로 메인 메모리에 저장되는게 아니라 우선 캐시 메모리에 있다가 캐시 메모리에서 메인 메모리로 값이 이동하게 됩니다. 이때 다른 스레드가 해당 값을 참조하면 데이터가 불일치하게 되는 이슈가 발생하게 됩니다.

<img width="1032" alt="스크린샷 2025-03-13 오후 10 09 04" src="https://github.com/user-attachments/assets/3fe9199f-8541-455f-91b9-483d06c9c180" />

<br><br>

### JVM 메인 메모리와 작업 메모리

- 메인 메모리는 모든 스레드가 접근할 수 있는 공간이며, 작업 스레드는 스레드마다 부여되는 메모리 공간입니다. 작업 메모리는 스레드가 사용하는 변수의 사본이 저장되며 스레드 내부에 연산은 작업 메모리에만 반영됩니다.
- 스레드마다 독립적인 작업 메모리가 존재하며 다른 스레드에서는 접근할 수 없습니다. (Stack, PC Register, Native Method Stack, Working Memory)
- 작업 메모리의 변화는 메인 메모리에 즉시 반영되지 않고 일정 시간 지연됩니다. 물론 즉시 반영될 수 있도록할 수는 있지만 성능이 저하될 수도 있습니다.
- 스레드는 JVM 메인 메모리에 직접 접근할 수 없습니다.

#### 읽기(Read)

- 읽기는 메인 메모리에서 변수의 값을 읽어 작업 메모리로 전송하는 것입니다.

#### 적재(Load)

- 적재는 메인 메모리가 전송한 값을 작업 메모리(사본)에 저장하는 것입니다.

#### 저장(Store)

- 저장은 작업 메모리 변수의 값을 메인 메모리로 전송하는 것입니다.

#### 쓰기(Write)

- 쓰기는 작업 메모리가 보내준 값을 메인 메모리 변수에 저장하는 것입니다.

<br>

### Volatile 메모리 가시성

- 멀티 스레드 환경에서 여러 스레드가 접근하는 변수에 대해 가시성을 제공합니다.
- volatile 키워드가 선언되어 있는 변수에서 값을 읽을 때 캐시 메모리를 거치지 않고, 메인 메모리에서 직접 변수를 읽고 수정된 값을 즉시 메인 메모리에 반영하게 됩니다.

#### volatile의 한계점

- volatile 키워드는 가시성은 보장해주지만 동시성은 해결할 수 없습니다.
- volatile 키워드는 읽기 작업을 하는 스레드가 N개가 있고, 쓰기 작업을 하는 스레드가 1개라면 동시성을 보장할 순 있지만 N:M의 상황이라면 동시성을 보장할 수 없습니다.

<img width="1032" alt="스크린샷 2025-03-13 오후 10 09 04" src="https://github.com/user-attachments/assets/b659c449-f312-4875-93fe-7dc371fe2dd2" />


