# Java의 날짜/시간 API에 대해

<br>

## 1. 기존 날짜/시간의 문제점들

#### 1-1. Date 클래스

- java 1.0에는 Date 클래스를 통해서 날짜와 시간관련 기능을 제공했는데, 클래스의 이름은 Date이지만 이름과 달리 특정 시점을 날짜가 아닌 밀리초로 나타내는 모호함과 월의 시작점이 0이였습니다. 그리고레력 달력의 월의 시작점은 1입니다.
- 시간을 가져와도 이 값은 에포크 시간(Unix Epoch time)이라 하여 세계 표준시(UTC)로 1970년 1월 1일 00시 00분 00초를 기준으로 지금까지 흐른 모든 초 단위로 표현한 것입니다.

#### 1-2. Calendar 클래스

- java 1.1에서 Date 클래스의 여러 메서드를 deprecated하면서 Calendar 클래스를 대안으로 제공했지만 Calendar 클래스에도 여전히 문제는 존재했습니다. 여전의 달의 시작점은 0이였습니다.

#### 1-3. java.util.Date와 java.sql.Date

- java.util.Date는 날짜와 시간을 모두 가지지만 java.sql.Date는 날짜값만 가집니다. 또한 두 클래스명은 동일하여 이는 좋지 않은 설계입니다.

#### 1-4. 멀티 스레드 환경에 안전하지 않았다

- 가장 큰 문제는 Date 및 Calender 클래스는 멀티 스레드 환경에서 안전하지 않았습니다.

<br>

## 2. Java 8 날짜/시간 디자인 원칙

#### 2-1. 불변성

- java 8부터 등장한 시간관련 API는 멀티 스레드에 안전합니다.

#### 2-1. 관심사의 분리

- 사람이 읽을 수 있는 날짜 시간과 기계 시간(Unix 타임스탬프)을 명확히 분리합니다.

#### 2-3. 확장 가능성

- 새로운 날짜 시간 API는 ISO-8601 달력 시스템에서도 작동하지만 다른 비 ISO 달력에서도 사용할 수 있습니다.

<br>

## 3. 다양한 API

- 상대적으로 자주 사용하는 LocalDate, LocalTime, LocalDateTime에 대해서는 알아보지 않겠습니다.

### 3-1. Instant

- 인간은 월, 주, 날짜, 시간으로 날짜와 시간을 계산하는데, 기계는 이러한 단위로 시간을 표현하기 어렵다고 합니다.
- 기계의 관점에서는 연속된 시간에서 특정 지점을 하나의 큰 수로 표현하는 것이 가장 자연스러운 시간 표현 방법인데, java.time.Instatnt 클래스에서는 이와 같은 기계적인 관점에서 시간을 표현한다고 합니다.
- Instant 클래스는 Unix Epoch Time을 기준으로 특정 지점까지의 시간을 초로 표현합니다.
- Instant 클래스는 나노초(10억분의 1초) 정밀도를 제공한다고 합니다.

### 3-2. Period

- 두 날짜 사이의 간격을 년-월-일 단위로 나타냅니다.

### 3-3. Duration

- 두 시간 사이의 간격을 나노초 단위로 나타냅니다.

### 3.4 OffsetDateTime

- LocalDateTime에 offset(시간대)의 정보가 추가되었습니다.
- 시차 정보가 있기 때문에 다른 지역의 서버에서 저장되는 경우 시차 정보를 통해서 같은 시간인지 다른 시간인지 파악할 수 있습니다.

### 3-5. ZonedDateTime

- OffsetDateTime에 TimeZone 정보가 추가되었습니다.
- offset(시간대)와 함께 TimeZone(지역정보)가 함께 저장됩니다.

<br>

#### ZonedDateTime과 LocalDateTime의 차이점

- ZonedDateTime과은 LocalDateTime에 TimeZone과 시차 개념이 추가되어 있습니다.
- ZonedDateTime.of(localDateTime, ZoneId.of("Asia/Seoul")); 인스턴스를 생성하면 '+09:00'가 출력되는 것을 알 수 있는데, +09:00는 UTC(협정 세계시)보다 9시간 빠르다는 것을 의미합니다.
  즉 UTC 시간은 2024-08-24T20:32:54.448445Z이며 아시아 시간은 이 시간보다 9시간 빠르다는 것입니다.

```java
LocalDate date = LocalDate.now();
LocalTime time = LocalTime.now();
LocalDateTime localDateTime = LocalDateTime.of(date, time);
System.out.println("localDateTime = " + localDateTime);

ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.of("Asia/Seoul"));
System.out.println("zonedDateTime = " + zonedDateTime);
System.out.println("toLocalDateTime" + zonedDateTime.toLocalDateTime());
System.out.println("toLocalDate" + zonedDateTime.toLocalDate());
System.out.println("toLocalTime" + zonedDateTime.toLocalTime());

// 결과
localDateTime = 2024-08-24T20:32:54.448445
zonedDateTime = 2024-08-24T20:32:54.448445+09:00[Asia/Seoul]
toLocalDateTime2024-08-24T20:32:54.448445
toLocalDate2024-08-24
toLocalTime20:32:54.448445
```

<br>

#### ZoneId와 ZoneOffset

- ZoneId은 타임존을 의미하고 ZoneOffset은 시차를 의미합니다. ZoneOffset은 UTC 기준으로 고정된 시간 차이를 양수나 음수로 나타내는 반면 ZoneId는 이 시간 차이를 타임존 코드로 나타냅니다.
- 타임존을 나타내는 ZoneId로 ZonedDateTime 인스턴스 객체를 생성하고, 시차를 나타내는 ZoneOffset으로 ZonedDateTime 인스턴스 객체를 생성하면 같은 것을 확인할 수 있습니다.

```java
ZoneOffset timeDiff = ZoneOffset.of("+09:00");
ZoneId zoneId = ZoneId.of("Asia/Seoul");
System.out.println(ZonedDateTime.now(timeDiff)); // 2024-08-24T20:40:27.259195+09:00
System.out.println(ZonedDateTime.now(zoneId));   // 2024-08-24T20:40:27.259249+09:00[Asia/Seoul]
```

<br>

#### 🧐 Summer time?

- 벤쿠버의 경우 보통은 시차가 -08:00 이지만 소위 Summer time 이라고 불리는 일괄절약타임을 시행하기 때문에 여름에는 한 시간 더 일찍 시간이 갑니다. 따라서 이런 도시를 대상으로 ZonedDateTime 인스턴스 객체를 만든다면 ZoneOffset(시차)을 사용하는 것보다 ZoneId(타임존)를 사용하는 편이 유리하다고합니다.
  그 이유는 계절에 따라 변하는 시차를 알아서 처리해주기 때문이라고 합니다.

<br>

#### 정리표

![스크린샷 2024-08-26 오후 4 51 41](https://github.com/user-attachments/assets/358586b4-3740-4fce-84be-505d55b1ef19)

<br>

#### 참고

- https://www.digitalocean.com/community/tutorials/java-8-date-localdate-localdatetime-instant
- https://catsbi.oopy.io/712f86f9-1601-4778-b1a3-7620ec0cb5f3
- https://medium.com/unibench/the-ultimate-guide-for-handling-date-and-time-in-java-6885c1409694
- https://springcamp.ksug.org/2023/static/media/track2.36ff2a00705b82fb8b46.pdf
- https://medium.com/publ-blog/%EA%B8%80%EB%A1%9C%EB%B2%8C-%EC%84%9C%EB%B9%84%EC%8A%A4%EC%97%90%EC%84%9C-%EC%8B%9C%EA%B0%84-%EB%8B%A4%EB%A3%A8%EA%B8%B0-1-%EC%8B%9C%EA%B0%84%EC%9D%80-%EC%96%B4%EB%96%BB%EA%B2%8C-%EC%A0%95%EC%9D%98%EB%90%98%EC%97%88%EC%9D%84%EA%B9%8C-4a1cc507b364


