# ReentrantLock

## 주요 API

#### void lock()

- lock을 다른 스레드가 가지고 있지 않다면 즉시 lock을 획득하고, lock의 보유 횟수를 1로 설정합니다.
- lock을 다른 스레드가 가지고 있다면 현재 스레드는 lock을 획득할 때까지 대기하게 되며 이후에 lock을 성공적으로 획득하면 lock 보유 횟수가 1 증가합니다.
- lock() 메서드를 호출한 스레드가 이미 lock을 가지고 있다면 lock 보유 횟수가 1 증가하고 메서드는 즉시 반환됩니다.
- 다른 스레드가 lock 메서드를 호출한 스레드에게 인터럽트를 걸더라도 인터럽트가 발생하지 않습니다.

#### void lockInterruptibly() throws InterruptedException

- lock을 다른 스레드가 가지고 있지 않다면 즉시 lock을 획득하고, lock의 보유 횟수를 1로 설정합니다.
- lock을 다른 스레드가 가지고 있다면 현재 스레드는 lock을 획득할 때까지 대기하게 되며 이후에 lock을 성공적으로 획득하면 lock 보유 횟수가 1 증가합니다.
- 현재 스레드가 lockInterruptibly() 메서드를 통해 lock을 획득하는 도중 또는 시도하는 중에 인터럽트할 수 있게 해줍니다. 따라서 인터럽트가 걸린 스레드는 InterruptedException를 처리하게 됩니다.
- lock의 획득보다 인터럽트가 우선적으로 처리됩니다.

#### boolean tryLock()

- tryLock() 메서드를 호출한 시점에 다른 스레드가 lock을 가지고 있지 않다면 lock을 획득하고 보유 횟수를 1로 설정하고, true를 반환합니다.
- lock이 공정성을 가지도록 설정되어 있더라도 현재 다른 스레드가 lock을 기다리는지 여부와 상관없이 lock이 사용 가능한 경우 즉시 lock을 획득합니다.
- lock이 다른 스레드에 의해 보유되고 있다면 즉시 false를 반환하며, tryLock 메서드를 호출한 스레드는 더 이상 대기하지 않습니다.

#### boolean tryLock(long time, TimeUnit unit) throws InterruptedException

- 주어진 대기 시간동안 lock을 다른 스레드가 보유하지 않았다면 즉시 lock을 획득하고, 보유 횟수를 1로 설정합니다.
- 현재 스레드가 이미 lock을 보유하고 있다면 보유 횟수가 1 증가하고, 만약 다른 스레드에 의해 보유되고 있다면 주어진 시간 동안만 lock을 보유할 때까지 대기합니다.
- lock의 획득 및 대기 시간 경과보다 인터럽트가 우선적으로 처리됩니다.

#### void unlock()

- lock을 해제하려면 동일한 스레드에서 lock() 메서드가 호출된 횟수와 동일한 횟수로 호출되어야 합니다. 즉 unlock() 메서드가 호출될 때마다 lock 카운트가 감소되며 카운트가 0이 될때 lock이 해제됩니다.

<br>

## 주요 API 이해

#### void lockInterruptibly()

- 위에서 언급했다시피 해당 메서드를 통해 락을 획득할 때 인터럽트가 걸린다면 인터럽트를 우선적으로 처리하게 됩니다. 만약 lock 메서드라면 인터럽트가 걸리지 않습니다.

```java
public class LockExample {

    private static final Lock LOCK = new ReentrantLock();

    public static void main(String[] args) throws InterruptedException {

        Thread threadA = new Thread(() -> {
            try {
                LOCK.lockInterruptibly();
                System.out.println("락을 획득하였습니다.");
            } catch (InterruptedException e) {
                System.out.println("락 획 시도중 인터럽트가 발생하였습니다.");
            } finally {
                LOCK.unlock();
            }
        });

        Thread threadB = new Thread(threadA::interrupt);

        threadA.start();
        threadB.start();

        threadA.join();
        threadB.join();
    }
}
// 결과
락 획 시도중 인터럽트가 발생하였습니다. 또는 락을 획득하였습니다.가 결과로 출력될 수 있습니다.
```
<br>

#### boolean tryLock()

```java
public class LockExample {

    private static final Lock LOCK = new ReentrantLock();

    public static void main(String[] args) throws InterruptedException {

        Thread threadA = new Thread(() -> {
            boolean hasLock = false;

            while (!hasLock) {
                hasLock = LOCK.tryLock();
                if (hasLock) {
                    try {
                        Thread.sleep(1000);
                        System.out.println(Thread.currentThread().getName() + "가 락을 획득하여 작업을 수행하고 있습니다.");
                    } catch (InterruptedException e) {

                    } finally {
                        LOCK.unlock();
                        System.out.println(Thread.currentThread().getName() + "가 락을 해제하였습니다.");
                    }
                } else {
                    System.out.println(Thread.currentThread().getName() + "가 락을 획득하지 못하였습니다.");
                }
            }
        });

        Thread threadB = new Thread(() -> {
            boolean hasLock = false;

            while (!hasLock) {
                hasLock = LOCK.tryLock();
                if (hasLock) {
                    try {
                        Thread.sleep(1000);
                        System.out.println(Thread.currentThread().getName() + "가 락을 획득하여 작업을 수행하고 있습니다.");
                    } catch (InterruptedException e) {

                    } finally {
                        LOCK.unlock();
                        System.out.println(Thread.currentThread().getName() + "가 락을 해제하였습니다.");
                    }
                } else {
                    System.out.println(Thread.currentThread().getName() + "가 락을 획득하지 못하였습니다.");
                }
            }
        });

        threadA.start();
        threadB.start();

        threadA.join();
        threadB.join();
    }
}
// 결과
Thread-1가 락을 획득하지 못하였습니다.
Thread-0가 락을 획득하여 작업을 수행하고 있습니다.
Thread-1가 락을 획득하지 못하였습니다.
Thread-0가 락을 해제하였습니다.
Thread-1가 락을 획득하여 작업을 수행하고 있습니다.
Thread-1가 락을 해제하였습니다.
```

<br>

## 코드로 살펴보기

### 💡 기본적인 사용법

- 기본적인 사용법은 아래 코드와 같습니다. 그리고 동작방식은 synchronized와 유사합니다.
- 대신 unlock() 메서드 같은 경우 반드시 lock을 해제해야 하므로 finally 블럭 내부에서 진행해야합니다. 그리고 lock 해제를 까먹지 않도록 주의해야합니다.

```java
public class Counter {

    private int count;
    private Lock lock;

    public Counter() {
        this.count = 0;
        this.lock = new ReentrantLock();
    }

    public void increment() {
        this.lock.lock();

        try {
            this.count++;
        } finally {
            this.lock.unlock();
        }
    }

    public int getCount() {
        return this.count;
    }
}

public class LockExample {

    public static void main(String[] args) throws InterruptedException {
        Counter counter = new Counter();

        Thread threadA = new Thread(() -> {
            for (int i = 0; i < 100000; i++) {
                counter.increment();
            }
        });

        Thread threadB = new Thread(() -> {
            for (int i = 0; i < 100000; i++) {
                counter.increment();
            }
        });

        threadA.start();
        threadB.start();

        threadA.join();
        threadB.join();

        System.out.println(counter.getCount()); // 200000
    }
}

```

<br>

### 🤔 unlock을 호출하지 않는다면?

#### 💡 정상적인 상황

- Thread-1이 작업을 완료하고 모든 락을 해제 후 Thread-0이 수행되는 것을 확인할 수 있습니다.

```java
public class LockExample {

    private static final Lock LOCK = new ReentrantLock();

    public static void main(String[] args) throws InterruptedException {

        Thread threadA = new Thread(() -> {
            LOCK.lock();
            System.out.println(Thread.currentThread().getName() + "가 락을 1번 획득하였습니다.");

            try {
                LOCK.lock();
                System.out.println(Thread.currentThread().getName() + "가 락을 2번 획득하였습니다.");
                try {
                    System.out.println(Thread.currentThread().getName() + "가 모든 로직을 수행하였습니다.");
                } finally {
                    LOCK.unlock();
                    System.out.println(Thread.currentThread().getName() + "가 락을 2번 해제하였습니다.");
                }
            } finally {
                LOCK.unlock();
                System.out.println(Thread.currentThread().getName() + "가 락을 1번 해제하였습니다.");
            }
        });


        Thread threadB = new Thread(() -> {
            LOCK.lock();
            System.out.println(Thread.currentThread().getName() + "가 락을 1번 획득하였습니다.");

            try {
                LOCK.lock();
                System.out.println(Thread.currentThread().getName() + "가 락을 2번 획득하였습니다.");
                try {
                    System.out.println(Thread.currentThread().getName() + "가 모든 로직을 수행하였습니다.");
                } finally {
                    LOCK.unlock();
                    System.out.println(Thread.currentThread().getName() + "가 락을 2번 해제하였습니다.");
                }
            } finally {
                LOCK.unlock();
                System.out.println(Thread.currentThread().getName() + "가 락을 1번 해제하였습니다.");
            }
        });

        threadA.start();
        threadB.start();

        threadA.join();
        threadB.join();
    }
}
// 결과
Thread-1가 락을 1번 획득하였습니다.
Thread-1가 락을 2번 획득하였습니다.
Thread-1가 모든 로직을 수행하였습니다.
Thread-1가 락을 2번 해제하였습니다.
Thread-1가 락을 1번 해제하였습니다.
Thread-0가 락을 1번 획득하였습니다.
Thread-0가 락을 2번 획득하였습니다.
Thread-0가 모든 로직을 수행하였습니다.
Thread-0가 락을 2번 해제하였습니다.
Thread-0가 락을 1번 해제하였습니다.
```

<br>

#### 🧨 비정상적인 상황

- Thread-0이 락을 얻은 횟수만큼 다시 해제를 하지 않아 Thread-1은 더 이상 수행되지 않는 상황이 발생하게 됩니다.


```java
public class LockExample {

    private static final Lock LOCK = new ReentrantLock();

    public static void main(String[] args) throws InterruptedException {

        Thread threadA = new Thread(() -> {
            LOCK.lock();
            System.out.println(Thread.currentThread().getName() + "가 락을 1번 획득하였습니다.");

            try {
                LOCK.lock();
                System.out.println(Thread.currentThread().getName() + "가 락을 2번 획득하였습니다.");
                try {
                    System.out.println(Thread.currentThread().getName() + "가 모든 로직을 수행하였습니다.");
                } finally {
                    // 락을 반환하지 않습니다.
//                    LOCK.unlock();
//                    System.out.println(Thread.currentThread().getName() + "가 락을 2번 해제하였습니다.");
                }
            } finally {
                LOCK.unlock();
                System.out.println(Thread.currentThread().getName() + "가 락을 1번 해제하였습니다.");
            }
        });


        Thread threadB = new Thread(() -> {
            LOCK.lock();
            System.out.println(Thread.currentThread().getName() + "가 락을 1번 획득하였습니다.");

            try {
                LOCK.lock();
                System.out.println(Thread.currentThread().getName() + "가 락을 2번 획득하였습니다.");
                try {
                    System.out.println(Thread.currentThread().getName() + "가 모든 로직을 수행하였습니다.");
                } finally {
                    LOCK.unlock();
                    System.out.println(Thread.currentThread().getName() + "가 락을 2번 해제하였습니다.");
                }
            } finally {
                LOCK.unlock();
                System.out.println(Thread.currentThread().getName() + "가 락을 1번 해제하였습니다.");
            }
        });

        threadA.start();
        threadB.start();

        threadA.join();
        threadB.join();
    }
}
// 결과
Thread-0가 락을 1번 획득하였습니다.
Thread-0가 락을 2번 획득하였습니다.
Thread-0가 모든 로직을 수행하였습니다.
Thread-0가 락을 1번 해제하였습니다.
```

<br>

## ReentrantLock의 공정성 정책

- ReentrantLock은 두 종류의 락 공정성 설정을 지원합니다.

### 불 공정성

- 불 공정한 락으로 생성된 경우 락을 얻고자 하는 스레드의 순서는 정해지지 않으며, 일반적으로 공정한 락보다 더 높은 처리량을 가집니다.
- 불 공정성은 락을 획득하려는 시점에 락이 사용중이라면 얻고자 하는 스레드는 대기열에 진입하게 되고, 락이 해제되면 대기열에서 대기 중인 스레드에게 락이 획득될 수도 있고, 방금 막 진입한 스레드가 락을 획득할 수도 있습니다.

<img width="1032" alt="스크린샷 2024-03-10 오후 3 13 57" src="https://github.com/kdg0209/realizers/assets/80187200/2f918368-d2af-460e-a001-dd80bd09fff9">

<br>
<br>

### 공정성

- 공정한 락으로 생성된 경우 스레드는 순서대로 락을 사용하게 됩니다.
- 공정성 락은 처리량은 감수하더라도 기아상태를 방지해야하는 상황에서 좋은 선택이 될 수 있습니다.
- tryLock() 메서드는 공정성을 따르지 않고 대기열에서 대기중인 스레드와 상관없이 락을 즉시 획득하며, tryLock(long time, TimeUnit unit) 메서드의 경우는 공정성을 따릅니다.



