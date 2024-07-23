# 고급 동기화 - concurrent.Lock

- synchronized 키워드는 사용하긴 단순하지만 무한 대기가 발생할 여지와 어떤 스레드가 락을 획득할지, 또는 특정 스레드는 오랜 기간동안 락을 획득하지 못하는 문제가 발생하곤 했습니다. 결국 더 유연하고 더 세밀한 제어가 필요로 하게되어
자바 1.5부터 java.util.concurrent 패키지에 다양한 라이브러리들이 추가되었습니다.

<br>

## LockSupport

- LockSupprt는 스레드를 WAITING 상태로 변경합니다.
- WAITING 상태는 누가 깨워주기 전까지는 계속 대기하고, 그리고 CPU 실행 스케줄링에 들어가지 않습니다.

#### LockSupport의 대표 기능

- park(): 스레드를 WAITING 상태로 변경합니다.
- parkNanos(): 스레드를 나노초 동안만 TIMED WAITING 상태로 변경합니다. 지정한 나노초가 지나면 TIMED WAITING 상태를 빠져나오고 RUNNABLE 상태가 됩니다.
- unpark(): WAITING 상태의 스레드를 RUNNABLE 상태로 변경합니다.

```java
public class Main {

    public static void main(String[] args) throws InterruptedException {
        Thread thread = new Thread(new Task());
        thread.start();

        Thread.sleep(100);

        LockSupport.unpark(thread); // WAITING 상태에서 RUNNABLE 상태로 변경합니다.
        System.out.println(thread.getState()); // RUNNABLE
    }

    static class Task implements Runnable {

        @Override
        public void run() {
            System.out.println("park 시작");
            LockSupport.park(); // RUNNABLE 상태에서 WAITING 상태가 됩니다.
            System.out.println(Thread.currentThread().getState()); // WAITING

        }
    }
}
```

<br>

#### 시간 대기

- parkNanos 메서드를 사용하여 특정 시간만큼만 대기하고 RUNNABLE 상태가 됩니다.

```java
public class Main {

    public static void main(String[] args) throws InterruptedException {
        Thread thread = new Thread(new Task());
        thread.start();

        Thread.sleep(100);
        System.out.println(thread.getState()); // RUNNABLE
    }

    static class Task implements Runnable {

        @Override
        public void run() {
            System.out.println("park 시작");
            LockSupport.parkNanos(2000_000000); // RUNNABLE 상태에서 TIMED_WAITING 상태가 됩니다.
            System.out.println(Thread.currentThread().getState()); // TIMED_WAITING

        }
    }
}
```

<br>

### BLOCKED와 WAITING(TIMED_WAITING)의 차이

#### 인터럽트

- BLOCKED 상태에 있는 스레드에 인터럽트를 걸어도 빠져나오지 못하고 여전히 BLOCKED 상태입니다.
- WAITING, TIMED_WAITING 상태에 있는 스레드에 인터럽트를 걸면 RUNNABLE 상태가 됩니다.

#### 용도

- BLOCKED 상태는 자바의 synchronized에서 락을 획득하기 위해 대기할 때 사용됩니다.
- WAITING, TIMED_WAITING 상태는 스레드가 특정 조건이나 특정 시간만큼만 대기할 때 사용됩니다.
- WAITING는 다양한 상황에서 사용되는데 join(), park(), wait()와 같은 메서드 호출 시 WAITING 상태가 됩니다.
- TIMED_WAITING 상태는 sleep(), wait(). join(), parkNanos() 메서드와 같은 시간 제한이 있는 메서드를 호출할 때 사용됩니다.

<br>

## ReentrantLock

### 대표 메서드

#### Lock()

- 락을 다른 스레드가 가지고 있지 않다면 즉시 락을 획득하고, 락의 보유 횟수를 1로 설정합니다.
- 락을 다른 스레드가 가지고 있다면 현재 스레드는 락을 획득할 때까지 대기하며, 이후에 락을 성공적으로 획득하면 락 보유 횟수를 1로 설정합니다.
- lock() 메서드를 호출한 스레드가 이미 락을 가지고 있다면 락 보유 횟수가 1 증가하고 메서드는 즉시 반환됩니다.
- 다른 스레드가 lock() 메서드를 호출한 스레드에게 인터럽트를 걸더라도 인터럽트가 발생하지 않습니다.

#### lockInterruptibly()

- 락을 다른 스레드가 가지고 있지 않다면 즉시 락을 획득하고, 락의 보유 횟수를 1로 설정합니다.
- 현재 스레드가 해당 메서드를 통해 락을 획득하는 도중에 인터럽트가 걸릴 수 있습니다. 만약 인터럽트가 걸리면 락 획득을 포기하게 됩니다.

#### tryLock()

- tryLock() 메서드를 호출한 시점에 다른 스레드가 락을 가지고 있지 않다면 락을 획득하고, 락 보유 횟수를 1로 설정하고 true를 반환합니다.
- 락이 공정성을 가지도록 설정되어 있더라도 현재 다른 스레드가 락을 기다리는지 여부와 상관없이 락을 사용가능한 경우 즉시 락을 획득합니다.(불공정성)
- 락이 다른 스레드에게 점유되어 있다면 즉시 false를 반환하고, 해당 스레드는 더 이상 락을 기다리지 않습니다.

#### tryLock(long time, TimeUnit unit)

- 주어진 시간동안만 락을 획득하려고 노력합니다.
- 락의 획득 및 대기 시간 경과보다 인터럽트가 우선적으로 처리됩니다.

#### unlock()

- 락을 해제합니다. 락을 해제하면 락을 획득하고자 하는 다른 스레드들은 락을 획득할 수 있습니다.
- unlock() 메서드가 호출될 때마다 락 카운트가 감소되며 카운트가 0이 될 때 락이 해제됩니다.

<br>

### 공정성

- synchronized 키워드를 사용하면 공정성을 잃을 수도 있습니다. 왜냐하면 BLOCKED 상태가 된 스레드 중에서 어떤 스레드가 락을 획득할지도 모르고, 최악의 경우 특정 스레드는 너무 오랜기간 동안 락을 획득하지 못할수도 있기 때문입니다.
- ReentrantLock은 공정하게 락을 획득할 수 있는 모드를 제공하고 있습니다.

#### 비공정 모드

- 비공정 모드는 ReentrantLock의 기본 모드이며, 이 모드에서는 락을 먼저 요청한 스레드가 락을 먼저 획득한다는 보장이 없습니다. 즉 락이 해제되었을 때 대기 중인 스레드 중 아무나 락을 획득할 수 있습니다.
- 이는 대기중인 아무 스레드가 락을 빨리 획득할 수 있지만 특정 스레드가 장기간 락을 획득하지 못할 수도 있게 됩니다.

#### 공정 모드

- 공정 모드는 락을 요청한 순서대로 스레드가 락을 획득할 수 있게 됩니다. 이는 먼저 대기한 스레드가 락을 획득하게 되어 스레드 간 공정성을 보장합니다. 하지만 비공정 모드에 비해 성능이 저하됩니다.
- tryLock() 메서드는 공정성을 따르지 않고 대기열에서 대기중인 스레드와 상관없이 락을 즉시 획득하며, tryLock(long time, TimeUnit unit) 메서드의 경우는 공정성을 따르게 됩니다.

<br>

### ReentrantLock 대기 중단

- ReentrantLock을 사용하면 락을 무한 대기하지 않고, 중간에 빠져나오는 것이 가능합니다. 심지어 락을 얻을 수 없다면 즉시 빠져나오는것도 가능합니다.

#### tryLock()

- 락 획득을 시도하고, 즉시 성공 여부를 반환합니다. 만약 다른 스레드가 락을 획득했다면 false를 반환하고, 그렇지 않다면 true를 반환합니다.

```java
public class BankAccountV2 implements BankAccount {

    private int balance;
    private final ReentrantLock lock = new ReentrantLock();

    public BankAccountV2(int balance) {
        this.balance = balance;
    }

    @Override
    public boolean withdraw(int amount) {
        if (!lock.tryLock()) {
            System.out.println("이미 처리중인 작업이 있습니다.");
            return false;
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {}
        
        this.balance -= amount;
        System.out.println("출금 성공: " + this.balance);
        return true;
    }

    @Override
    public int getBalance() {
        return this.balance;
    }
}

public class Main {

    public static void main(String[] args) throws InterruptedException {
        BankAccount bankAccount = new BankAccountV2(1000);
        Thread threadA = new Thread(new WithdrawTask(bankAccount, 800));
        Thread threadB = new Thread(new WithdrawTask(bankAccount, 800));

        threadA.start();
        threadB.start();

        threadA.join();
        threadB.join();

        System.out.println("결과: " + bankAccount.getBalance());
    }
}

// 결과 
이미 처리중인 작업이 있습니다.
출금 성공: 200
결과: 200
```

<br>

#### tryLock(long time, TimeUnit unit)

- 주어진 시간동안 락 획득을 시도합니다. 동작 방식은 tryLock() 메서드와 동일하지만 추가적으로 이 메서드를 사용할 때 인터럽트가 걸리면 InterruptedException 예외가 발생하고 락 획득을 포기하게 됩니다.


<br>

#### 참고

- https://github.com/kdg0209/realizers/blob/main/inflearn-java-concurrency-programming/Java%20ReentrantLock.md



