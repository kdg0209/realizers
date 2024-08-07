# volatile 메모리 가시성

- 멀티 스레드 환경에서 한 스레드가 변경한 값이 다른 스레드에서 언제 보이는지에 대한 문제를 메모리 가시성이라 합니다. 이름 그대로 메모리에 변경한 값이 보이는가, 보이지 않는가에 대한 문제입니다.
- 자바에서는 volatile 키워드를 사용함으로써 데이터의 일관성과 가시성을 보장할 수 있습니다.

<br>

## CPU 캐시와 메인 메모리

- CPU 캐시는 CPU와 메모리 사이에 위치하며, 데이터 흐름을 최적화시키고 성능을 향상시키기 위해 사용됩니다.
- CPU는 캐시 메모리에서 값이 존재하지 않는 경우 메인 메모리에 접근하게 되며 값을 가져옵니다. 또한 이때 참조 지역성의 원리로 인해 근처의 값도 함께 가져오게 됩니다.

#### 🚗 동작 원리

1. CPU는 메인 메모리로부터 필요한 데이터를 CPU 캐시로 읽어들입니다.
2. CPU 캐시에 있는 데이터를 CPU Register로 읽어들이고, 읽어온 데이터를 기반으로 ALU, 제어장치로부터 필요한 연산을 수행하게 됩니다.
3. 작업한 결과를 CPU Register에서 캐시 메모리로 내보내고, 적절한 시점에 CPU 캐시 메모리에서 메인 메모리로 보내게 됩니다.
4. 이러한 과정을 계속하여 반복하게 됩니다.

<img width="1032" alt="스크린샷 2024-07-22 오후 12 23 28" src="https://github.com/user-attachments/assets/f718d871-142a-4fe5-a75c-b148f0819944">

<br>

#### ❓ 문제점

- 멀티 스레드 환경에서는 CPU에 할당된 스레드가 메인 메모리가 아닌 CPU 캐시로부터 데이터를 읽어 데이터를 가공하고 다시 CPU 캐시나 메인 메모리에 데이터를 적재할 때 다른 스레드가 해당 변수를 참조하게 된다면 데이터 일관성이 깨지게 됩니다.


#### 🧐 언제 데이터가 메인 메모리에 반영될까?

- 이 부분은 CPU 설계 방식과 실행 환경에 따라 다를 수 있습니다. 즉시 반영될 수도 있고, 몇 밀리초 후에 반영될 수도 있고, 몇 초후에 될 수도 있고, 평생 반영되지 않을 수도 있습니다.
- 컨텍스트 스위칭이 발생할 때, 캐시 메모리도 갱신이 되는데, 이러한 부분도 실행 환경에 따라 달라집니다.

<br>

## volatile의 원리

- 공유 변수에 volatile 키워드를 사용함으로써 CPU가 변수를 읽을 때 캐시 메모리를 거치지 않고, 메인 메모리에서 직접 변수를 읽고, 수정된 변수를 즉시 메인 메모리에 반영하게 됩니다.

<img width="1032" alt="스크린샷 2024-07-22 오후 1 13 33" src="https://github.com/user-attachments/assets/7820cc19-c570-4ae9-961b-65e246cfe734">

<br>

### volatile의 한계점

- volatile 키워드는 가시성은 보장해주지만 동시성은 해결할 수 없습니다.
- volatile 키워드는 읽기 작업을 하는 스레드가 N개이고, 쓰기 작업을 하는 스레드가 1개라면 동시성을 보장할 순 있지만 N:M의 상황이라면 동시성을 보장할 순 없습니다.

<img width="1032" alt="스크린샷 2024-07-22 오후 1 15 41" src="https://github.com/user-attachments/assets/022d0910-4e23-4f46-9b56-7772f998b53f">

<br>

#### race condition

- volatile 키워드를 사용함으로써 race condition이 발생하지 않을꺼 같지만 실제로는 발생하게 됩니다.
- 스레드 A와 스레드 B가 동시에 메인 메모리로부터 1이라는 값을 읽어오고, 1을 더한 뒤 메인 메모리에 반영을 하더라도 2가 메인 메모리에 반영될 수 있습니다. 정상적으로는 3이 반영되어야 하는데 말이죠.

<img width="1032" alt="스크린샷 2024-07-22 오후 1 18 24" src="https://github.com/user-attachments/assets/62ae7ffe-fa06-4164-8912-d0f4407b1950">

<br>
<br>

## 자바 메모리 모델(Java Memory Model)

- Java Memory Model(JMM)은 자바 프로그램이 어떻게 메모리에 접근하고 수정할 수 있는지를 규정하며, 특히 멀티 스레드 프로그램에서 스래드 간의 상호작용을 정의합니다.
- JMM에는 여러가지 내용이 있지만 핵심은 스레드들의 작업 순서를 보장하는 happens-before 관계에 대한 정의입니다.

### happens-before란?

- happens-before 관계는 자바 메모리 모델에서 스레드 간의 작업 순서를 정의하는 개념입니다. 만약 A 작업이 B 작업보다 happens-before 관계에 있다면 A 작업에서의 모든 메모리 변경 사항은 B 작업에서 볼 수 있습니다. 즉 A 작업에서 변경된 내용은 B 작업이 시작되기 전에 모두 메모리에 반영됩니다.
- happens-before 관계는 이름 그대로, 한 동작이 다른 동작보다 먼저 발생함을 보장합니다.
- happens-before 관계는 스레드 간의 메모리 가시성을 보장하는 규칙입니다.
- happens-before 관계가 성립하면 한 스레드의 작업을 다른 스레드에서 볼 수 있습니다.
- 즉 한 스레드에서 수행한 작업을 다른 스레드가 참조할 때 최신 상태가 보장됩니다.

### happens-before 관계가 발생하는 경우

#### 프로그램 순서 규칙

- <b> 단일 스레드 </b> 내에서 프로그램의 순서대로 작성된 모든 명령어들은 happens-before 순서로 실행됩니다.

#### volatile 변수 규칙

- 한 스레드에서 volatile 변수에 대한 쓰기 작업은 해당 변수를 읽는 모든 스레드에게 보이도록 합니다.

#### 스레드 시작 규칙

- 한 스레드에서 Thread.start() 메서드를 호출하면 해당 스레드 내의 모든 작업은 start() 메서드를 호출 이후에 실행된 작업보다 happens-before 관계가 성립합니다.

#### 스레드 종료 규칙

- 한 스레드에서 Thread.join() 메서드를 호출하면 join 대상 스레드의 모든 작업은 join이 반환된 후의 작업보다 happens-before 관계를 가집니다.


















