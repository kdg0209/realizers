# ThreadLocal

- 자바에서 스레드는 오직 자신만 접근하여 읽고 쓸 수 있는 로컬 저장소를 제공하는데 이를 ThreadLocal이라 합니다.
- ThreadLocal은 스레드 간 격리되어 있습니다.
- 스레드는 ThreadLocal에 저장된 값을 특정 위치나 시점에 상관없이 어디서나 전역변수처럼 접근하여 사용할 수 있습니다.

<img width="1030" alt="스크린샷 2024-03-03 오후 1 05 29" src="https://github.com/kdg0209/realizers/assets/80187200/1b51a5d5-e5ec-44ab-bc03-df037762eb89">

<br>
<br>

## ThreadLocal의 동작 원리

#### 데이터 저장시

<img width="1030" alt="스크린샷 2024-03-02 오후 4 54 34" src="https://github.com/kdg0209/realizers/assets/80187200/e49c5d63-6d11-4754-94ca-244f479c296a">

<br>
<br>

```java
public class ThreadExample {

    private static final ThreadLocal<String> THREAD_LOCAL = new ThreadLocal<>();

    public static void main(String[] args) throws InterruptedException {

        Thread threadA = new Thread(() -> {
            System.out.println(Thread.currentThread().getName() + "의 값: " + THREAD_LOCAL.get());
            THREAD_LOCAL.set("threadA에 값 추가 hello world?!");
            System.out.println(Thread.currentThread().getName() + "의 값: " + THREAD_LOCAL.get());

            // 사용 후 반드시 remove 호출
            THREAD_LOCAL.remove();
        });

        Thread threadB = new Thread(() -> {
            System.out.println(Thread.currentThread().getName() + "의 값: " + THREAD_LOCAL.get());
            THREAD_LOCAL.set("threadB에 값 추가 HELLO WORLD?!");
            System.out.println(Thread.currentThread().getName() + "의 값: " + THREAD_LOCAL.get());

            // 사용 후 반드시 remove 호출
            THREAD_LOCAL.remove();
        });

        threadA.setName("threadA");
        threadA.start();

        threadB.setName("threadB");
        threadB.start();
    }
}
// 결과
threadA의 값: null
threadB의 값: null
threadA의 값: threadA에 값 추가 hello world?!
threadB의 값: threadB에 값 추가 HELLO WORLD?!
```
<br>

#### 데이터 조회시

<img width="1030" alt="스크린샷 2024-03-02 오후 5 02 04" src="https://github.com/kdg0209/realizers/assets/80187200/fa9ff63a-c5ab-43da-831c-7efa881e074a">

<br>
<br>

## ThreadLocal의 주의사항

- ThreadLocal에 저장된 값은 스레드마다 독립적으로 저장되기 때문에 데이터를 삭제하지 않아도 메모리를 점유하는 것 외에 문제가 발생하지 않습니다. (삭제 안하면 메모리 누수 ^_^)
- 그러나 스프링은 스레드 풀 기반이므로 스레드가 ThreadLocal을 다 사용하면 반드시 remove 메서드를 호출해줘야 합니다.
- remove 메서드를 호출하지 않는다면 스레드 풀은 스레드를 재사용하기 때문에 현재 스레드는 이전 스레드에서 삭제하지 않았던 데이터를 참조할 수 있기 때문에 문제가 발생할 수 있습니다.

### ThreadLocal에 데이터 저장되는 흐름

- ThreadLocal에 데이터를 저장하는 순서는 대략적으로 아래 사진처럼 흘러갑니다. Spring Security도 내부적으로 ThreadLocal을 사용하여 각 사용자의 정보를 개별 스레드의 로컬에 저장하고 있습니다.
- 클라이언트A의 요청이 왔을 때 WAS는 스레드 풀에서 하나의 스레드를 조회하게 됩니다. 그리고 Thread-A가 할당되고 클라이언트A의 정보는 스레드 로컬에 저장됩니다.

<img width="1030" alt="스크린샷 2024-03-03 오후 1 14 39" src="https://github.com/kdg0209/realizers/assets/80187200/581844ba-da08-4a5a-a05d-64a75e93a434">

<br>
<br>

### ThreadLocal의 remove를 호출하지 않으면 어떻게 될까?

- 클라이언트B의 요청이 들어온 경우 이번에도 똑같이 WAS는 스레드 풀에서 가용 스레드 하나를 조회하게 됩니다. 여기서 Thread-A가 할당될 수도 있고, 다른 스레드가 할당될 수도 있지만 편의상 Thread-A가 할당된다고 가정하겠습니다. 이때 Thread-A는 기존 클라이언트A의 정보를 저장하고 있었지만 remove 메서드를 호출하지 않아 더미 데이터를 보관하고 있었습니다. 이때 get 메서드를 통해 조회를 하면 예기치 않게 클라이언트A의 정보를 반환하게 됩니다.
- 하지만 remove 메서드를 호출한다면 기존 Thread-A는 스레드 풀에 반납할때 자신이 저장한 데이터는 제거한 상태이므로 재사용하더라도 문제가 발생하지 않게됩니다.

<img width="1030" alt="스크린샷 2024-03-03 오후 1 20 56" src="https://github.com/kdg0209/realizers/assets/80187200/2ab815f1-cf86-448e-830e-802956a09357">

<br>
<br>

## InheritableThreadLocal

- InheritableThreadLocal은 ThreadLocal의 확장 버전으로서 부모 스레드로부터 자식 스레드로 값을 전달하고 싶은 경우에 사용할 수 있습니다.

#### 값의 상속

- 부모 스레드가 InheritableThreadLocal 변수에 값을 설정하면, 해당 부모 스레드로부터 생성된 자식 스레드는 부모의 값을 상속받게 됩니다.

#### 독립성

- 자식 스레드가 상속받은 값을 변경하더라도 부모 스레드의 값에는 영향을 주지 않습니다.

```java
public class ThreadExample {

    public static void main(String[] args) throws InterruptedException {

        InheritableThreadLocal<Integer> inheritableThreadLocal = new InheritableThreadLocal<>();
        inheritableThreadLocal.set(100);

        new Thread(() -> {
            System.out.println("부모 스레드로부터 상속받은 값: " + inheritableThreadLocal.get()); // 100

            // 값 변경
            inheritableThreadLocal.set(999);
            System.out.println("자식 스레드에서 변경한 값: " + inheritableThreadLocal.get()); // 999
        }).start();

        System.out.println("부모 스레드의 값: " + inheritableThreadLocal.get()); // 100
    }
}
```






  
