# Future와 Callback에 대해

- 자바에서는 Future(및 CompletableFuture)와 Callback은 비동기 프로그래밍에서 사용할 수 있으며, 비동기 작업의 결과를 처리하거나 비동기 작업이 완료되었을 때 특정 작업을 수행할 수 있습니다.

### Future(CompletableFuture)

- 비동기 작업의 결과를 제네릭하게 받을 수 있습니다.
- 비동기 작업이 완료될 때까지 블럭킹됩니다. (get 메서드 호출 시)
- 비동기 작업이 완료되면 결과를 반환받을 수 있습니다.

<img width="1032" alt="스크린샷 2024-03-16 오후 1 18 47" src="https://github.com/kdg0209/realizers/assets/80187200/a9148f70-d03b-4a0e-b269-38c3e0874fa9">

<br>
<br>

#### 예제 코드

```java
public class FutureExample {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(1);

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        Future<Integer> future = EXECUTOR.submit(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            return 111;
        });

        Integer result = future.get();
        EXECUTOR.shutdown();

        System.out.println("작업의 결과는 " + result + "입니다.");
    }
}
```

<br>
<br>

### Callback

- 비동기 작업이 완료되었을 때 특정 동작을 수행할 인터페이스 및 클래스를 정의할 수 있습니다.
- 블럭킹되지 않고 비동기 작업이 완료되면 콜백 메서드가 호출됩니다.
- 콜백 메서드를 통해 작업 결과를 처리합니다.

<img width="1032" alt="스크린샷 2024-03-16 오후 1 30 10" src="https://github.com/kdg0209/realizers/assets/80187200/04d5aceb-1510-40c3-8409-b25c9b711aa6">

<br>
<br>

#### 예제 코드

```java
public class CallbackExample {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(1);

    public static void main(String[] args) {

        EXECUTOR.execute(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            int result = 111;
            Callback callback = new CallbackWorker();
            callback.onComplete(result);
        });

        EXECUTOR.shutdown();
    }

    static class CallbackWorker implements Callback {

        @Override
        public void onComplete(int result) {
            System.out.println("작업의 결과는 " + result + "입니다.");
        }
    }
    interface Callback {

        void onComplete(int result);
    }
}
```

<br>
<br>

### Future와 Callback를 혼합한 비동기 작업

<img width="1032" alt="스크린샷 2024-03-16 오후 1 40 38" src="https://github.com/kdg0209/realizers/assets/80187200/8de019df-9eb7-4cd2-8e6d-76c6d18fc3d7">

<br>
<br>

#### 예제

```java
public class Example {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(1);

    public static void main(String[] args) {

        Callable<String> callable = () -> {
            System.out.println("작업을 수행 중입니다.");
            Thread.sleep(1000);
            System.out.println("작업을 완료하였습니다.");

            return "success";
        };

        Future<String> future = EXECUTOR.submit(callable);
        System.out.println("비동기 작업 시작..");

        then(future, result -> {
            System.out.println("비동기 작업의 결과: " + result);
        });
    }

    private static void then(Future<String> future, Callback callback) {
        Thread thread = new Thread(() -> {
            try {
                String result = future.get();
                callback.onComplete(result);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        thread.start();
    }

    interface Callback {

        void onComplete(String result);
    }
}
```


