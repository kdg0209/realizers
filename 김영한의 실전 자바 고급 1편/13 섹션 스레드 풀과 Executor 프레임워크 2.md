# 스레드 풀과 Executor 프레임워크 2

<br>

## ExecutorService의 종료 메서드

#### void shutdown()

- 새로운 작업을 받지 않고, 이미 제출된 작업을 완료한 후에 종료합니다.

#### List<Runnable> shutdownNow()

- 실행중인 작업을 중단하고, 대기 중인 작업을 반환하며 즉시 종료합니다.
- 실행중인 작업을 중단하기 위해 인터럽트를 발생시킵니다.

#### close()

- 자바 19부터 지원하는 서비스 종료 메서드입니다. 이 메서드는 shutdown()과 같습니다.

<br>

## ExecutorService의 우아한 종료

- shutdown() 메서드를 호출하여 이미 들어온 모든 작업을 다 완료하고 서비스를 종료하면 가장 이상적이지만 갑자기 요청이 너무 많이 들어오거나 작업이 너무 오래걸리거나, 또는 버그가 발생해 특정 작업이 끝나지 않을 수 있는데 이런 경우 문제가 발생할 수 있습니다.

```java
public class ExecutorShutdownMain {

    public static void main(String[] args) {
        ExecutorService es = Executors.newFixedThreadPool(2);
        es.submit(new Task("task-1", 1000));
        es.submit(new Task("task-2", 1000));
        es.submit(new Task("task-3", 1000));
        es.submit(new Task("task-4", 10000));

        printState(es);
        shutdownAndAwaitTermination(es);
        printState(es);
    }

    private static void shutdownAndAwaitTermination(ExecutorService es) {
        es.shutdown(); // non-blocking 새로운 작업을 받지 않고, 큐에 있던 작업들만 완료함

        try {
            // 이미 대기중인 작업들을 모두 완료될때까지 10초 기다림
            if (!es.awaitTermination(10, TimeUnit.SECONDS)) {
                // 정상 종료가 너무 오래걸리면... 강제 종료
                es.shutdownNow();

                // 작이 취소될때까지 기다림
                if (!es.awaitTermination(10, TimeUnit.SECONDS)) {
                    System.out.println("서비스가 종료되지 않았습니다.");
                }
            }
        } catch (InterruptedException e) {
            es.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    static class Task implements Runnable {

        private final String name;
        private final int sleepMs;

        public Task(String name, int sleepMs) {
            this.name = name;
            this.sleepMs = sleepMs;
        }

        @Override
        public void run() {
            System.out.println(name + " 작업 시작");
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException e) {}
            System.out.println(name + " 작업 완료");
        }
    }
}
```

<br>

## Executor 스레드 풀 관리

#### corePoolSize

- 스레드 풀에서 기본적으로 관리되는 스레드 수

#### maximumPoolSize

- 스레드 풀에서 최대로 관리되는 스레드 수

#### keepAliveTime, TimeUnit

- 기본 스레드 수를 초과해서 만들어진 초과 스레드들이 생존할 수 있는 대기 시간, 이 시간동안 처리할 작업이 없다면 초과 스레드는 제거됩니다.

<br>

### PoolSize 동작원리

- corePoolSize는 3, maximumPoolSize는 4로 설정해서 기본 스레드는 3개 초과 스레드는 4개로 설정하였습니다.
    - 기본적으로는 3개의 스레드를 운영하다가 급한 경우 스레드는 4개까지 늘어나게 됩니다. 또한 ArrayBlockingQueue를 사용하여 작업 공간을 3으로 지정하였습니다.

```java
public class PoolSizeMain {

    public static void main(String[] args) {
        ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(3);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(3, 4, 3000, TimeUnit.MILLISECONDS, queue);

        executor.submit(new Task("task-1", 1000));
        printState(executor, "task-1");

        executor.submit(new Task("task-2", 1000));
        printState(executor, "task-2");

        executor.submit(new Task("task-3", 1000));
        printState(executor, "task-3");

        executor.submit(new Task("task-4", 1000));
        printState(executor, "task-4");

        executor.submit(new Task("task-5", 1000));
        printState(executor, "task-5");

        executor.submit(new Task("task-6", 1000));
        printState(executor, "task-6");

        executor.submit(new Task("task-7", 1000));
        printState(executor, "task-7");

        executor.submit(new Task("task-8", 1000));
        printState(executor, "task-8");
    }

    static class Task implements Runnable {

        private final String name;
        private final int sleepMs;

        public Task(String name, int sleepMs) {
            this.name = name;
            this.sleepMs = sleepMs;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException e) {}
        }
    }

    private static void printState(ExecutorService executorService, String taskName) {
        if (executorService instanceof ThreadPoolExecutor poolExecutor) {
            int poolSize = poolExecutor.getPoolSize();
            int activeCount = poolExecutor.getActiveCount();
            int queueSize = poolExecutor.getQueue().size();
            long completedTaskCount = poolExecutor.getCompletedTaskCount();

            System.out.println(taskName + " -> [poolSize= " + poolSize + ", activeCount= " + activeCount + ", queueSize= " + queueSize + ", completedTaskCount= " + completedTaskCount + "]");
        }
    }
}

// 결과
task-1 -> [poolSize= 1, activeCount= 1, queueSize= 0, completedTaskCount= 0]
task-2 -> [poolSize= 2, activeCount= 2, queueSize= 0, completedTaskCount= 0]
task-3 -> [poolSize= 3, activeCount= 3, queueSize= 0, completedTaskCount= 0]
task-4 -> [poolSize= 3, activeCount= 3, queueSize= 1, completedTaskCount= 0]
task-5 -> [poolSize= 3, activeCount= 3, queueSize= 2, completedTaskCount= 0]
task-6 -> [poolSize= 3, activeCount= 3, queueSize= 3, completedTaskCount= 0]
task-7 -> [poolSize= 4, activeCount= 4, queueSize= 3, completedTaskCount= 0]
task-8 -> 예외 발생함
```

<br>

#### 🧐 초과 스레드는 언제 만들어질까?

- 초과 스레드가 만들어지는 시점은 기본 스레드가 다 사용중이고, queue의 공간도 다 찬 경우 초과 스레드가 만들어집니다.
- 위 예제에서 queue의 사이즈가 3이고, 기본 스레드 수가 3이며, 초과 스레드 수가 4개 일때, task-1 ~ task-3까지는 기본 스레드가 작업을 수행하며 task-4 ~ task-6은 queue에 담기게 됩니다. 이후에 task-7이 들어오면 queue 또한 꽉 찬 상태이기 때문에 초과 스레드가 1개 더 만들어지는 것입니다.
그 후 task-8이 들어오면 초과 스레드의 범위를 넘어서기 때문에 reject 예외가 발생하게 되는것입니다.

<br>

### Excutor 고정 풀 전략

- newFixedThreadPool을 통해 고정 풀 전략을 사용할 수 있으며, 스레드 풀에 기본 설정만큼의 스레드를 생성하고, 초과 스레드는 생성하지 않습니다.
- 스레드 수가 고정되어 있기 때문에 CPU와 메모리 리스소가 예측 가능한 안정적인 방식입니다.

#### 예제 코드

- 기본 스레드 수를 2개로 지정하면, 초과 스레드 수 또한 2개가 됩니다. 이때 작업을 6개를 준다면 task-0, task-1는 스레드 두 개에 의해 작업이 시작되며, 나머지 task-2 ~ task-5는 queue에서 대기하게 되고, 이후 스레드가 작업을 완료하면 queue에서 task를 가져가게 됩니다.

```java
public class Main {

    public static void main(String[] args) {
        ExecutorService es = Executors.newFixedThreadPool(2);

        for (int i = 0; i < 6; i++) {
            String taskName = "task-" + i;
            es.submit(new Task(1000));
            printState(es, taskName);
        }
    }

    static class Task implements Runnable {

        private final int sleepMs;

        public Task(int sleepMs) {
            this.sleepMs = sleepMs;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException e) {}
        }
    }

    private static void printState(ExecutorService executorService, String taskName) {
        if (executorService instanceof ThreadPoolExecutor poolExecutor) {
            int poolSize = poolExecutor.getPoolSize();
            int activeCount = poolExecutor.getActiveCount();
            int queueSize = poolExecutor.getQueue().size();
            long completedTaskCount = poolExecutor.getCompletedTaskCount();

            System.out.println(taskName + " -> [poolSize= " + poolSize + ", activeCount= " + activeCount + ", queueSize= " + queueSize + ", completedTaskCount= " + completedTaskCount + "]");
        }
    }
}

// 결과
task-0 -> [poolSize= 1, activeCount= 1, queueSize= 0, completedTaskCount= 0]
task-1 -> [poolSize= 2, activeCount= 2, queueSize= 0, completedTaskCount= 0]
task-2 -> [poolSize= 2, activeCount= 2, queueSize= 1, completedTaskCount= 0]
task-3 -> [poolSize= 2, activeCount= 2, queueSize= 2, completedTaskCount= 0]
task-4 -> [poolSize= 2, activeCount= 2, queueSize= 3, completedTaskCount= 0]
task-5 -> [poolSize= 2, activeCount= 2, queueSize= 4, completedTaskCount= 0]
```

<br>

### Excutor 캐싱 풀 전략

- newCachedThreadPool은 기본 스레드를 사용하지 않고, 60초 생존 주기를 가진 초과 스레드만 사용합니다.
- 초과 스레드 수에는 제한이 없습니다.
- 큐에 작업을 저장하지 않고, 스레드가 받아서 바로 처리합니다. 즉 모든 요청이 대기하지 않고 스레드가 바로바로 처리하기 때문에 성능이 빠릅니다.

#### 예제 코드

- 로그를 보면 poolSize=1000, activeCount=1000인 걸 볼 수 있으며, 이것은 스레드가 요청을 받을때마다 생성이 된다는 것을 알 수 있습니다.

```java
public class Main {

    public static void main(String[] args) {
        ExecutorService es = Executors.newCachedThreadPool();

        for (int i = 0; i < 1000; i++) {
            String taskName = "task-" + i;
            es.submit(new Task(1000));
            printState(es, taskName);
        }
    }

    static class Task implements Runnable {

        private final int sleepMs;

        public Task(int sleepMs) {
            this.sleepMs = sleepMs;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException e) {}
        }
    }

    private static void printState(ExecutorService executorService, String taskName) {
        if (executorService instanceof ThreadPoolExecutor poolExecutor) {
            int poolSize = poolExecutor.getPoolSize();
            int activeCount = poolExecutor.getActiveCount();
            int queueSize = poolExecutor.getQueue().size();
            long completedTaskCount = poolExecutor.getCompletedTaskCount();

            System.out.println(taskName + " -> [poolSize= " + poolSize + ", activeCount= " + activeCount + ", queueSize= " + queueSize + ", completedTaskCount= " + completedTaskCount + "]");
        }
    }
}

// 결과
task-995 -> [poolSize= 996, activeCount= 996, queueSize= 0, completedTaskCount= 0]
task-996 -> [poolSize= 997, activeCount= 997, queueSize= 0, completedTaskCount= 0]
task-997 -> [poolSize= 998, activeCount= 998, queueSize= 0, completedTaskCount= 0]
task-998 -> [poolSize= 999, activeCount= 999, queueSize= 0, completedTaskCount= 0]
task-999 -> [poolSize= 1000, activeCount= 1000, queueSize= 0, completedTaskCount= 0]
```

#### 🧨 문제점

- 트래픽이 한 순간에 몰린다면 그 만큼의 스레드가 생성이 되는데, 이 때 CPU와 메모리 사용량이 급증하기 때문에 시스템이 다운될 가능성이 발생합니다.

<br>

### Excutor 사용자 정의 풀 전략

#### 예제 코드

- ThreadPoolExecutor 설정을 corePoolSize를 100개, maximumPoolSize를 200개, keepAliveTime을 1초, queue 사이즈를 1000로 설정하였습니다.
- corePoolSize의 설정 100을 통해서 기본 스레드 수를 100 유지합니다. 그리고 queue의 사이즈를 1000개로 설정합으로써 기본 스레드가 다 작업중이라면 요청들은 우선 queue에 담기게 되며, queue 사이즈가 꽉 차면 그제서야 maximumPoolSize을 설정한 스레드 100개들이 활성화됩니다. 왜 200이 아니고 100이냐면 (maximumPoolSize - corePoolSize)를 계산하면 100이기 때문입니다.
- 그리고 해당 설정을 통해 한 번에 받을 수 있는 작업의 총량은 queue 사이즈 + maximumPoolSize 입니다.(즉 1200), 한 번에 1200개의 작업이 넘어간다면 RejectedExecutionException가 발생하며, 아래 예제에서 볼 수 있듯이 task-1201은 예외가 발생한 것을 확인할 수 있습니다.

```java
public class MainV2 {

//    private static final int TASK_SIZE = 1000; 
//    private static final int TASK_SIZE = 1100; 
//    private static final int TASK_SIZE = 1200;
    private static final int TASK_SIZE = 1201; // RejectedExecutionException 발생

    public static void main(String[] args) {
        ExecutorService es = new ThreadPoolExecutor(100, 200, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000));

        long startMs = System.currentTimeMillis();

        for (int i = 1; i <= TASK_SIZE; i++) {
            String taskName = "task-" + i;
            try {
                es.submit(new Task());
                printState(es, taskName);
            } catch (RejectedExecutionException e) {
                System.err.println(taskName + " -> " + e);
            }
        }

        System.out.println("endMs: " + (System.currentTimeMillis() - startMs));
    }

    static class Task implements Runnable {

        @Override
        public void run() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
        }
    }

    public static void printState(ExecutorService executorService, String taskName) {
        if (executorService instanceof ThreadPoolExecutor poolExecutor) {
            int poolSize = poolExecutor.getPoolSize();
            int activeCount = poolExecutor.getActiveCount();
            int queueSize = poolExecutor.getQueue().size();
            long completedTaskCount = poolExecutor.getCompletedTaskCount();

            System.out.println(taskName + " -> [poolSize= " + poolSize + ", activeCount= " + activeCount + ", queueSize= " + queueSize + ", completedTaskCount= " + completedTaskCount + "]");
        } else {
            System.out.println(executorService);
        }
    }
}

// 결과
task-1200 -> [poolSize= 200, activeCount= 200, queueSize= 1000, completedTaskCount= 0]
task-1201 -> java.util.concurrent.RejectedExecutionException: Task java.util.concurrent.FutureTask@4567f35d[Not completed, task = java.util.concurrent.Executors$RunnableAdapter@6356695f[Wrapped task = section11.MainV2$Task@4f18837a]] rejected from java.util.concurrent.ThreadPoolExecutor@3d8c7aca[Running, pool size = 200, active threads = 200, queued tasks = 1000, completed tasks = 0]
```

<br>

#### 🧨 실무에서 하는 실수

- 아래는 queue의 사이즈를 지정하지 않았습니다. 즉 사용자의 요청을 무한대로 처리할 수 있긴 하지만, 시스템이 어느정도 감당할 수 있는지, 지금 상황이 긴급상황인지 파악할 수 없기 때문에 항상 queue의 사이즈를 지정하여 사용하도록 해야합니다.

```java
ExecutorService es = new ThreadPoolExecutor(100, 200, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
```

<br>

## Excutor 예외 정책

- ThreadPoolExecutor에 요청할 때 queue도 꽉 차고, 초과 스레드도 더 이상 할당할 수 없다면 예외가 발생합니다.

### AbortPolicy

- 새로운 작업을 제출할 때 RejectedExecutionException 예외가 발생합니다. (기본 정책입니다.)
- 한 번에 120개까지 처리할 수 있지만 121번째 요청이 전달되면 예외가 발생합니다.

```java
public class Main {

    private static final int TASK_SIZE = 121; // RejectedExecutionException 발생

    public static void main(String[] args) {
        ExecutorService es = new ThreadPoolExecutor(10, 20, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100), new ThreadPoolExecutor.AbortPolicy());

        for (int i = 1; i <= TASK_SIZE; i++) {
            String taskName = "task-" + i;
            try {
                es.submit(new Task(taskName));
            } catch (RejectedExecutionException e) {
                System.err.println(taskName + " -> " + e);
            }
        }
    }

    static class Task implements Runnable {

        private final String name;

        public Task(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            try {
                System.out.println(name + ": 실행");
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
        }
    }
}

task-121 -> java.util.concurrent.RejectedExecutionException: Task java.util.concurrent.FutureTask@4883b407[Not completed, task = java.util.concurrent.Executors$RunnableAdapter@39c0f4a[Wrapped task = section11.AbortPolicyMain$Task@1794d431]] rejected from java.util.concurrent.ThreadPoolExecutor@5ebec15[Running, pool size = 20, active threads = 20, queued tasks = 100, completed tasks = 0]
```

<br>

### DiscardPolicy

- 새로운 작업들을 버립니다.
- DiscardPolicy 클래스의 rejectedExecution 메서드를 보면 내부에서 아무 행위도 하지 않는걸 볼 수 있습니다.

```java
public class Main {

    private static final int TASK_SIZE = 121; 

    public static void main(String[] args) {
        ExecutorService es = new ThreadPoolExecutor(10, 20, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100), new ThreadPoolExecutor.DiscardPolicy());

        for (int i = 1; i <= TASK_SIZE; i++) {
            String taskName = "task-" + i;
            try {
                es.submit(new Task(taskName));
            } catch (RejectedExecutionException e) {
                System.err.println(taskName + " -> " + e);
            }
        }
    }

    static class Task implements Runnable {

        private final String name;

        public Task(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            try {
                System.out.println(name + ": 실행");
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
        }
    }
}

// DiscardPolicy의 내부
public static class DiscardPolicy implements RejectedExecutionHandler {

    public DiscardPolicy() { }

    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {

    }
}
```

<br>

### CallerRunsPolicy

- 새로운 작업을 제출한 스레드가 대신해서 작업을 처리합니다.
- 아래 로그를 보면 121 번째 작업은 메인 스레드에 의해 수행된 것을 알 수 있습니다.

```java
public class Main {

    private static final int TASK_SIZE = 121; 

    public static void main(String[] args) {
        ExecutorService es = new ThreadPoolExecutor(10, 20, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100), new ThreadPoolExecutor.CallerRunsPolicy());

        for (int i = 1; i <= TASK_SIZE; i++) {
            String taskName = "task-" + i;
            try {
                es.submit(new Task(taskName));
            } catch (RejectedExecutionException e) {
                System.err.println(taskName + " -> " + e);
            }
        }
    }

    static class Task implements Runnable {

        private final String name;

        public Task(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            try {
                System.out.println("[" + Thread.currentThread().getName() + " -> " + name + ": 살행]");
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
        }
    }
}

[pool-1-thread-16 -> task-116: 살행]
[main -> task-121: 살행]
[pool-1-thread-18 -> task-118: 살행]
[pool-1-thread-20 -> task-120: 살행]
```

<br>

### 사용자 정의 정책

- 개발자가 직접 정의한 거절 정책을 사용할 수 있습니다.
- 거절된 작업을 버리지만, 대신 로그를 남겨 개발자가 문제 상황을 인지할 수 있도록 합니다.
- MyRejectedExecutionHandler 클래스를 만든 후 RejectedExecutionHandler 인터페이스의 rejectedExecution 메서드를 구현한 뒤 ThreadPoolExecutor 선언시 마지막에 해당 클래스를 매개변수로 넘겨주었습니다.

```java
public class Main {

    private static final int TASK_SIZE = 121;

    public static void main(String[] args) {
        ExecutorService es = new ThreadPoolExecutor(10, 20, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100), new MyRejectedExecutionHandler());

        for (int i = 1; i <= TASK_SIZE; i++) {
            String taskName = "task-" + i;
            try {
                es.submit(new Task(taskName));
            } catch (RejectedExecutionException e) {
                System.err.println(taskName + " -> " + e);
            }
        }
    }

    static class MyRejectedExecutionHandler implements RejectedExecutionHandler {

        private static AtomicInteger count = new AtomicInteger(0);

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            int i = count.incrementAndGet();
            System.out.println("[경고] 누적된 작업 수: " + i);
        }
    }

    static class Task implements Runnable {

        private final String name;

        public Task(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            try {
                System.out.println("[" + Thread.currentThread().getName() + " -> " + name + ": 살행]");
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
        }
    }
}

[경고] 누적된 작업 수: 1
[pool-1-thread-14 -> task-114: 살행]
```



