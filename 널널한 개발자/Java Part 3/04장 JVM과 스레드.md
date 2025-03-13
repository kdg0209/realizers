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

## 


























