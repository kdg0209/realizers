# SOLID

- SOLID의 목표는 소프트웨어 유지보수성과 확장성을 높이는 것입니다.
- SOLID는 아래 질문에 답을 주는 원칙입니다.
  - 영향 범위: 코드 변경으로 인한 영향의 범위가 어디까지 인가?
  - 의존성: 소프트웨어의 의존성 관리가 제대로 이루어지고 있는가?
  - 확장성: 쉽게 확장할 수 있는가?
 
<br>

## 1. SOLID 소개

### 단일 책임 원칙(SRP: Single Responsibility Principle)

- SRP는 클래스는 하나의 책임만을 가지고 있어야 한다는 의미입니다.
- 클래스는 하나의 책임만을 가질 떄 변경이 쉬워집니다.
- 하나의 클래스에 너무 많은 라인의 코드가 있다면 특정 범위의 라인을 수정할 때 영향의 범위를 가늠할 수 없으니 변경하기 어려워집니다.

#### 🧐 책임이란 무엇일까?

- 아래 Teacher 클래스는 영어, 수학, 과학을 가르칩니다. 이 Teacher 클래스는 SRP 원칙을 준수하고 있을꺼요?
- 우선 저는 SRP를 위배한다고 생각합니다.
    1. 영어 선생님의 책임, 수학 선생님의 책임, 과학 선생님의 책임은 구분되어야 한다고 생각합니다. 그 이유는 각각의 메서드가 같은 속도로 변하는가? 저는 요구사항에 따라 다르게 변한다고 생각합니다.
 
- SRP를 준수한다고 생각하는 입장
    1. 가르치는 선생님 입장으로 보는 경우  

```java
public class Teacher {

    public void english() {

    }

    public void math() {

    }

    public void science() {
        
    }
}
```

<br>

#### 액터(Actor)

- 책임을 설명하기 위해 새로운 개념이 등장했는데, 바로 액터입니다.
- 액터란 메시지를 전달하는 주체입니다. 그리고 SRP에서 말하는 책임은 액터에 대한 책임입니다. 메시지를 요청하는 주체가 누구냐에 따라 책임이 달라질 수 있습니다.
- 예를들어 Teacher라는 클래스를 사용하는 액터가 하나라면 SRP를 잘 준수하고 있는것이고, 만약 3가지라면 SRP 원칙을 준수하고 있지 못하는 것입니다.
- SRP에서 말하는 책임은 결국 액터에 대한 책임입니다.

<br> 

### 개방 폐쇄 원칙(OCP: Open Closed Principle)

- OCP 원칙은 확장에는 열려있고, 수정에는 닫혀있어야 한다는 원칙입니다. 즉 기존 코드를 수정하지 않으면서도 확장이 가능한 소프트웨어를 만드는 것입니다.
- OCP 원칙은 SOLID 원칙에서 가장 어렵습니다. 그 이유는 기존 코드를 수정할 때 그 변경사항을 수정으로 보아야하는지, 확장으로 보아야하는지 명확하게 구분짓기 어렵기 때문입니다.

#### 🧐 기존 코드를 수정한다면 OCP를 위반하는 걸까?

- 우리는 개발을 하다보면 기존 코드에 속성을 추가한다던가, 메서드를 추가하는 일이 종종 발생하곤 합니다. 근데 이런 경우에 OCP를 위반한다고 볼 수 있을까요?
- 기존 코드에 속성과 메서드가 추가되었다고 해서 수정되었다고 판단할 수 있습니다. 하지만 이 추가사항이 기존 속성을 변경하거나 메서드를 수정한것이 아니기 때문에 확장으로 간주될 수 있습니다.
- 코드의 수정이 기존에 작성되어 있던 로직이나, 테스트를 깨트리지 않는 한 OCP를 위반하지 않았다고 판단해도 무방합니다.
- 즉 새로운 기능을 추가할 때 기존 코드를 전혀 수정하지 않는 것은 불가능합니다. 따라서 우리는 수정을 아예 안하는 것이 아니라 수정을 가능한 한 내부 구현에 대한 수정이 아닌 상위 수준의 코드에서 진행되어야 합니다.

#### 어떻게 OCP를 달성할 수 있을까?

- OCP를 잘 준수하기 위해서 개인적으로 변하는 부분과 변하지 않는 부분을 잘 파악한 후 변하지 않는 부분을 추상화하고, 변하는 부분은 구현 클래스를 두어 캡슐화를 해야한다고 생각합니다.
- https://github.com/kdg0209/realizers/blob/main/%EB%94%94%EC%9E%90%EC%9D%B8%ED%8C%A8%ED%84%B4%EC%9D%98%20%EC%95%84%EB%A6%84%EB%8B%A4%EC%9B%80/03%EC%9E%A5-%EC%84%A4%EA%B3%84%20%EC%9B%90%EC%B9%99.md

<br>

### 리스코프 치환 원칙(LSP: Liskov Substitution Principle)

- LSP 원칙은 파생 클래스가 상위 클래스를 대체할 수 있어야 한다는 원칙입니다.
- LSP 원칙은 상속을 할 때 지켜져야 하는 원칙입니다.

#### 예제 코드

- 아래 Car, Train 클래스는 Vehicle 추상 클래스를 상속받고 있습니다. 이때 기차는 좌회전, 우회전을 할 수 있을까요? 할 수 없으므로 Train 클래스는 예외를 호출하고 있습니다.

```java
public abstract class Vehicle {

    abstract void goForward();

    abstract void turnRight();

    abstract void turnLeft();
}

public class Car extends Vehicle {

    @Override
    void goForward() {

    }

    @Override
    void turnRight() {

    }

    @Override
    void turnLeft() {

    }
}

public class Train extends Vehicle {

    @Override
    void goForward() {

    }

    @Override
    void turnRight() {
        throw new UnsupportedOperationException();
    }

    @Override
    void turnLeft() {
        throw new UnsupportedOperationException();
    }
}
```

<br>

#### 🧐 어떻게 개선할 수 있을까요?

- 상속보다는 인터페이스를 사용하여 메서드를 세분화하는 방식으로 개선할 수 있습니다.

```java
public interface TurnAble {

    void turnRight();

    void turnLeft();
}

public interface MoveAble {

    void goForward();
}

public class Car implements MoveAble, TurnAble {

    @Override
    public void goForward() {

    }

    @Override
    public void turnRight() {

    }

    @Override
    public void turnLeft() {

    }
}

public class Train implements MoveAble {

    @Override
    public void goForward() {

    }
}
```

<br>

### 인터페이스 분리 원칙(ISP: Interface Segregation Principle)

- 클라이언트는 자신이 사용하지 않는 인터페이스에 의존하지 않아야 한다는 원칙입니다.
- 즉, 어떤 클래스가 자신에게 필요하지 않는 인터페이스의 메서드를 구현하거나, 의존하지 않아야 한다는 의미입니다.

#### 예제 코드

- 아래 예제 코드를 보면 Train 클래스는 특정 메서드가 필요없음에도 불구하고, 메서드를 구현하고 예외를 던지고 있습니다. 즉 자신에게 필요하지 않은 메서드를 구현하고 있고, ISP 원칙을 위반하고 있습니다.

```java
public interface Vehicle {

    void goForward();

    void turnRight();

    void turnLeft();
}

public class Car implements Vehicle {

    @Override
    void goForward() {

    }

    @Override
    void turnRight() {

    }

    @Override
    void turnLeft() {

    }
}

public class Train implements Vehicle {

    @Override
    void goForward() {

    }

    @Override
    void turnRight() {
        throw new UnsupportedOperationException();
    }

    @Override
    void turnLeft() {
        throw new UnsupportedOperationException();
    }
}
```

<br>

#### 응집도와 LSP 

- 통합적인 인터페이스를 만들어 응집도를 높이겠다라는 생각이 들 수도 있습니다. 하지만 이러한 인터페이스는 응집도는 높아질 수 있겠지만 변경이 어려워지게 됩니다.
- 응집도를 높이는 것은 언뜻보면 좋을 수 있겠지만 그만큼 희생해야 하는 부분도 있습니다. 트레이드 오프가 아닐까? 생각이 듭니다.

<br>

### 의존성 역전 원칙(DIP: Dependency Inversion Principle)

- 상위 모듈은 하위 모듈에 의존해서는 안됩니다. 상위 모듈과 하위 모듈 모두 추상화에 의존해야 합니다.
- 추상화는 세부 사항에 의존해서 안됩니다. 세부 사항이 추상화에 의존해야 합니다.

<br>

## 2. 의존성




















