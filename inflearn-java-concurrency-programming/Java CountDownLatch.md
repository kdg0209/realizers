# CountDownLatch

## CountDownLatch란?

- 멀티 프로그래밍 환경에서 다른 스레드들의 모든 작업이 마친 후에 특정 작업을 해야하는 경우 모든 작업이 끝날때까지 대기할 수 있도록 자바에서는 CountDownLatch라는 기능을 제공하고 있습니다.
- CountDownLatch는 생성시 주어진 카운트로 초기화되며, await 메서드는 현재 카운트가 countDown 메서드 호출로 인해 0이 될때까지 블록킹되며 그 이후에 모든 대기 중인 스레드가 해제되고 await 이후 처리가 진행됩니다.
- CountDownLatch는 일회성으로 처리도며 재사용할 수 없습니다. 만약 카운트를 재설정하는 버전이 필요한 경우 CyclicBarrier를 사용해야 합니다.
- 하나의 스레드가 여러번 CountDownLatch의 countDown 메서드를 호출해도 됩니다.

<br>

### 예제

- 아래 예제는 Worker 스레드 5개를 만든 후 run 메서드 내부에서 작업이 완료되면 finally 블럭에서 completedLatch의 갯수를 하나씩 차감하고 있습니다.
- 그리고 try 블럭 내부에서는 startLatch의 await 메서드를 호출함으로써 5개의 Worker 스레드들을 WAIT 상태로 만든 후 main 메서드 내부의 startLatch.countDown();를 호출 함으로써 동시에 시작시킬 수 있도록 하는 트리거의 역할을 수행하고 있습니다.
- 그리고 main 메서드의 completedLatch.await(); 메서드로 인해 Worker 스레드들이 작업을 완료하기 까지 블럭킹이 됩니다. 

```java
public class CountDownLatchExample {

    public static void main(String[] args) throws InterruptedException {

        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completedLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            Worker worker = new Worker(startLatch, completedLatch);
            Thread thread = new Thread(worker);
            thread.start();
        }

        Thread.sleep(3000);

        System.out.println("스레드들의 작업이 시작되었습니다.");
        startLatch.countDown();
        completedLatch.await();

        System.out.println("모든 작업이 완료되었습니다.");
    }

    static class Worker implements Runnable {

        private final CountDownLatch startLatch;
        private final CountDownLatch completedLatch;

        public Worker(CountDownLatch startLatch, CountDownLatch completedLatch) {
            this.startLatch = startLatch;
            this.completedLatch = completedLatch;
        }

        @Override
        public void run() {
            try {
                this.startLatch.await();
                System.out.println(Thread.currentThread().getName() + "가 작업을 수행하고 있습니다.");
                Thread.sleep(1000L);
                System.out.println(Thread.currentThread().getName() + "가 작업을 완료하였습니다.");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                this.completedLatch.countDown();
            }
        }
    }
}
```
