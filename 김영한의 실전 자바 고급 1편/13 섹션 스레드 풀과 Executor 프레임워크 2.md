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















