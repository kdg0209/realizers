# 동기화 - synchronized

#### 예제 코드

- 아래 간단한 코드를 보면 계좌에서 출금을 하는 예제입니다. BankAccountV1 클래스의 withdraw 메서드를 보면 내가 가진 금액이 출금 금액보다 적으면 false를 그게 아니라면 내가 가진 금액에서 출금 금액을 감소하고 true를 반환하고 있습니다.
- 그리고 Main 클래스에서 출금을 하고 있는데, 문제가 발생합니다. 한번은 출금이 실패되어야 하는데, 둘다 출금이 되고 나의 계좌 금액은 마이너스가 되어버립니다.

```java
public interface BankAccount {

    // 출금
    boolean withdraw(int amount);

    // 잔고 확인
    int getBalance();
}

public class BankAccountV1 implements BankAccount {

    private int balance;

    public BankAccountV1(int balance) {
        this.balance = balance;
    }

    @Override
    public boolean withdraw(int amount) {
        if (this.balance < amount) {
            System.out.println("출금 실패");
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

public class WithdrawTask implements Runnable {

    private final BankAccount bankAccount;
    private final int amount;

    public WithdrawTask(BankAccount bankAccount, int amount) {
        this.bankAccount = bankAccount;
        this.amount = amount;
    }

    @Override
    public void run() {
        this.bankAccount.withdraw(this.amount);
    }
}

public class Main {

    public static void main(String[] args) throws InterruptedException {

        BankAccountV1 bankAccount = new BankAccountV1(1000);

        Thread threadA = new Thread(new WithdrawTask(bankAccount, 800));
        Thread threadB = new Thread(new WithdrawTask(bankAccount, 800));

        threadA.start();
        threadB.start();

        threadA.join();
        threadB.join();

        System.out.println("최종 잔액: " + bankAccount.getBalance());
    }
}

// 결과
출금 성공: -600
출금 성공: -600
최종 잔액: -600
```

<br>

## synchronized 키워드

- 자바의 synchronized 키워드를 사용하면 한번에 하나의 스레드만 접근할 수 있도록 하는 임계 영역을 구성할 수 있습니다.

#### 🚗 synchronized의 동작 원리

- 자바에서 모든 객체는 내부에 자신만의 락을 가지고 있습니다.
- 스레드가 synchronized 블럭에 진입하기 위해서는 해당 인스턴스의 락을 획득하고 임계 구역에 진입할 수 있습니다.

#### 예제 코드

- withdraw 메서드와 getBalance 메서드에 synchronized 키워드를 선언된걸 확인할 수 있습니다. 그리고 결과를 보면 우리가 원했던 결과가 나온것을 확인할 수 있습니다.

```java
public class BankAccountV2 implements BankAccount {

    private int balance;

    public BankAccountV2(int balance) {
        this.balance = balance;
    }

    @Override
    public synchronized boolean withdraw(int amount) {
        if (this.balance < amount) {
            System.out.println("출금 실패");
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
    public synchronized int getBalance() {
        return this.balance;
    }
}

// 결과
출금 성공: 200
출금 실패
최종 잔액: 200
```

<br>

## synchronized 코드 블럭에 사용하기

- synchronized의 가장 큰 장점이자 단점은 한 번에 하나의 스레드만 임계 구역에 진입하고 실행된다는 것입니다. 여러 스레드가 동시에 진입하여 실행되지 못하기 때문에 성능이 조금 떨어질 수 있습니다. 그래서 synchronized를 꼭 필요한 임계구역에만 사용하는게 성능상 유리합니다.

#### 예제 코드

- withdraw() 메서드에서 synchronized 키워드를 메서드 레벨에 선언하는게 아니라 코드 블럭에 사용함으로써 임계 영역을 조금 더 짧게 가져갈 수 있습니다.

```java
public class BankAccountV2 implements BankAccount {

    private int balance;

    public BankAccountV2(int balance) {
        this.balance = balance;
    }

    @Override
    public boolean withdraw(int amount) {

        // 임계 구역 시작
        synchronized(this) {
            if (this.balance < amount) {
                System.out.println("출금 실패");
                return false;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}
            this.balance -= amount;
        }
        // 임계 구역 완료

        System.out.println("출금 성공: " + this.balance);
        return true;
    }

    @Override
    public synchronized int getBalance() {
        return this.balance;
    }
}
```

<br>

#### 🧨 synchronized의 단점

- BLOCKED 상태가 된 스레드는 락이 획득할 때까지 무한정으로 대기하게 됩니다. 또한 중간에 인터럽트가 발생하더라도 깨어나지 않게됩니다.
- BLOCKED 상태의 여러 스레드들 중에서 어떤 스레드가 락을 획득할지 알 수 없으며, 최약의 경우 특정 스레드가 락을 오랫동안 획득하지 못할 수도 있습니다.


