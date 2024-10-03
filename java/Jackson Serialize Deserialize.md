# Jackson 직렬화 역직렬화 과정에 대해

#### 테스트 환경

- 스프링 부트(3.3.x) 버전
- jackson.databind(2.17.2) 버전

<br>

#### 클래스를 사용하여 JSON 형태의 요청을 받는 과정

- 클래스를 사용하여 JSON 요청을 받고자 한다면 흔히 아래과 같은 과정을 거치게 됩니다. 근데 사용자의 요청값 중에서 isPersonalInformationConsent는 true로 보냈지만 출력 결과를 보면 false인 것을 확인할 수 있는데, 무슨 일이 발생한걸까요?

```java
@Getter
public class MemberSignUpRequest {

    private String memberId;
    private String password;
    private Boolean isMarketingConsent;
    private boolean isPersonalInformationConsent;
}

@RestController
@RequestMapping("/members")
public class MemberApi {

    @PostMapping("/sign-up")
    public void signUp(@RequestBody MemberSignUpRequest request) {
        System.out.println("사용자 아이디: " + request.getMemberId()); // test123
        System.out.println("사용자 비밀번호: " + request.getPassword()); // 1234567890
        System.out.println("사용자 마케팅 동의여부: " + request.getIsMarketingConsent()); // true
        System.out.println("사용자 개인정보 동의여부: " + request.isPersonalInformationConsent()); // false
    }
}

// 사용자의 요청값
{
    "memberId": "test123",
    "password": "1234567890",
    "isMarketingConsent": true,
    "isPersonalInformationConsent": true
}
```

<br>

#### 레코드를 사용하여 JSON 형태의 요청을 받는 과정

- 레코드를 사용하여 JSON 형태의 요청값을 받는다면 흔히 아래와 같은 과정을 거치는데 이번에는 isPersonalInformationConsent값이 정상적으로 출력되는 것을 알 수 있습니다. 클래스와 무슨 차이가 있길래 잘 받을 수 있는걸까요?


```java
public record MemberSignUpRequest(String memberId, String password, Boolean isMarketingConsent, boolean isPersonalInformationConsent) {

}

@RestController
@RequestMapping("/members")
public class MemberApi {

    @PostMapping("/sign-up")
    public void signUp(@RequestBody MemberSignUpRequest request) {
        System.out.println("사용자 아이디: " + request.memberId()); // test123
        System.out.println("사용자 비밀번호: " + request.password()); // 1234567890
        System.out.println("사용자 마케팅 동의여부: " + request.isMarketingConsent()); // true
        System.out.println("사용자 개인정보 동의여부: " + request.isPersonalInformationConsent()); // true
    }
}

// 사용자의 요청값
{
    "memberId": "test123",
    "password": "1234567890",
    "isMarketingConsent": true,
    "isPersonalInformationConsent": true
}
```

<br>

## 🧐 사용자의 요청은 어떤 과정을 통해 객체의 속성과 매칭될까?

### 1. AbstractMessageConverterMethodArgumentResolver 클래스에 대해 살펴보야 합니다.

#### AbstractMessageConverterMethodArgumentResolver 클래스

- 해당 클래스에서 봐야할 부분은 많지만 그 중 가장 중요한 역할을 수행하는 메서드는 readWithMessageConverters() 메서드입니다.
- while 문을 순회하면서 MappingJackson2HttpMessageConverter가 선택이 되며, MappingJackson2HttpMessageConverter 클래스의 canRead(...) 메서드가 수행되지만 MappingJackson2HttpMessageConverter 클래스에는 canRead(...) 메서드가 선언되어 있지 않아 상위 클래스인 AbstractJackson2HttpMessageConverter canRead(...) 메서드가 수행되게 됩니다.

<img width="1032" alt="스크린샷 2024-10-01 오후 4 48 39" src="https://github.com/user-attachments/assets/9463b6a1-5f29-4675-80ee-500ac0fa37e0">

<br> 

#### 🎈 canRead() 메서드 이녀석이 가장 중요했습니다..!!

- canRead() 메서드 내부에서는 정~~~말 많은 일들이 일어나고 있었습니다. 처음에는 boolean 값만 반환하니 별 로직이 없겠거니 생각을 하고 깊게 안보고 있었는데.... 사실 클래스의 속성을 변환해주고 있었던것입니다. 그럼 이름이 canRead와 실제 로직이 이루어지는 행위가 분리되어야하는게 아닌가? 라는 생각이 들었습니다.

#### POJOPropertiesCollector 클래스에 대해 살펴보아야 합니다.

- POJOPropertiesCollector 클래스의 collectAll() 메서드는 클래스의 정보(변수, 메서드, 생성자)를 수집합니다. 그리고 _addMethods(...) 메서드를 통해 props의 결과가 
- _addFields(props), _addMethods(props), _removeUnwantedProperties(props) 메서드를 주의 깊게 살펴봐야 합니다.

<img width="1032" alt="스크린샷 2024-10-03 오후 3 09 48" src="https://github.com/user-attachments/assets/421c15c5-729d-4433-9202-204d1a6d0b6c">

<br> <br> 

#### _addFields() 메서드를 살펴봅시다.

- _addFields(...) 메서드 내부에서는 일련의 로직을 수행한 후 POJOPropertyBuilder 객체를 만든 후 POJOPropertyBuilder 클래스의 addField(...) 메서드를 호출하게 됩니다.
- 해당 메서드를 통해 결국 클래스의 변수들이 property로 만들어지게 됩니다.

<img width="1032" alt="스크린샷 2024-10-03 오후 3 12 55" src="https://github.com/user-attachments/assets/be6b1075-6e46-4f30-8508-d6eb33f3889e">

<br><br> 

#### _addMethods() 메서드를 살펴봅시다.

- _addMethods(...) 메서드는 클래스의 메서드를 파악하게 됩니다. 그리고 boolean 타입의 isPersonalInformationConsent 메서드도 is가 없어진 personalInformationConsent의 Property가 만들어지는 순간입니다.

<img width="1032" alt="스크린샷 2024-10-03 오후 3 19 26" src="https://github.com/user-attachments/assets/51b000f2-6335-4021-a760-c6e799b05a05">

<br><br> 

#### 💡 _addGetterMethod() 메서드

- _addMethods() 메서드 내부에서 _addGetterMethod()를 호출하게 되고, 내부적으로 DefaultAccessorNamingStrategy 클래스의 legacyManglePropertyName(...) 메서드를 통해 personalInformationConsent 이름을 가진 Property가 만들어지게 되는 것입니다.

<img width="1033" alt="스크린샷 2024-10-03 오후 3 28 26" src="https://github.com/user-attachments/assets/76562990-b906-4c1d-9331-edb9017be3d1">

#### DefaultAccessorNamingStrategy 클래스

- legacyManglePropertyName(...) 메서드 내부에서 isPersonalInformationConsent를 personalInformationConsent로 바꾸게 됩니다. 

<img width="1032" alt="스크린샷 2024-10-03 오후 3 25 01" src="https://github.com/user-attachments/assets/dfe651bd-7e2c-44ba-bbd1-d13f48634460">

<br><br> 

#### 중간 정리

- _addFields(...) 메서드와 _addMethods(...) 메서드를 통해 현재 만들어진 property의 개수가 5개인것을 파악했습니다. 이제 _removeUnwantedProperties(...) 메서드에 진입하게 됩니다!

<img width="1025" alt="스크린샷 2024-10-03 오후 3 32 46" src="https://github.com/user-attachments/assets/a0764395-7d27-434e-a7c0-010bcb408654">

<br><br> 

#### _removeUnwantedProperties() 메서드에 진입하다

- 메서드 내부에서는 Property를 순회하면서 visible이 false로 되어있는 객체들을 지우게 되는데 이때 isPersonalInformationConsent Property는 삭제됩니다.

<img width="1032" alt="스크린샷 2024-10-03 오후 3 40 09" src="https://github.com/user-attachments/assets/3245cebb-4411-487e-a3f4-bec3ba604b9d">

<br><br> 

#### 최종적으로 남아있는 Property

- 최종적으로 남아있게 되는 Property는 총 4개가 됩니다.

<img width="1032" alt="스크린샷 2024-10-03 오후 3 42 09" src="https://github.com/user-attachments/assets/61985d6f-c8d8-46bd-8194-fbe38c5b61e1">

<br><br>

### 2. BeanDeserializerFactory에 대해 살펴보자.

#### buildBeanDeserializer()

- 위 과정까지 거치면 이후 BeanDeserializerFactory 클래스의 buildBeanDeserializer(...) 메서드가 호출됩니다. 이때 BeanDescription 클래스에는 위 과정을 거진 클래스의 Property들이 존재하게 됩니다.

<img width="1038" alt="스크린샷 2024-10-03 오후 3 49 37" src="https://github.com/user-attachments/assets/1f41a8d5-f4a9-4dd8-bbd8-f3903c51dc40">

<br><br>

#### addBeanProps() 메서드에 주목해야 합니다.

- buildBeanDeserializer() 메서드를 보면 내부에서 addBeanProps(...) 메서드를 호출하고 있습니다.

<img width="1032" alt="스크린샷 2024-10-01 오후 4 00 47" src="https://github.com/user-attachments/assets/cacdd77c-2894-4a72-b3f5-a770cdf4db36">

<br><br>

#### addBeanProps() 메서드 중 일부

- 아래 사진은 addBeanProps() 메서드 중 일부입니다. propDef.hasField() 메서드를 통해 Boolean 타입이라면 if 분기의 내부 로직을 수행하고 boolean 타입이라면 else 분기문을 수행하게 됩니다.
- 하지만 여기서 문제가 prop 변수에 결과값을 할당하게 되는데, 이 prop 변수가 나중에 NULL이라면 builder.addProperty(prop)에 담아줄 수 없는 것입니다.

<img width="1032" alt="스크린샷 2024-10-03 오후 4 02 10" src="https://github.com/user-attachments/assets/e5701302-b6a0-496f-9c52-e605db387d7f">

<br><br>

- 위에서 언급했던거처럼 prop가 NULL이라면 BeanDeserializerBuilder의 property에 추가할 수 없으므로 객체의 속성이 personalInformationConsent라면 속성이 추가가 되지 않는 것입니다.

<img width="1029" alt="스크린샷 2024-10-01 오후 4 34 10" src="https://github.com/user-attachments/assets/d0484d17-eb4d-4729-af67-bb40b9b7277c">

<br><br>

#### addBeanProps() 메서드 수행 후 

- addBeanProps(...) 메서드 수행 후 Property의 값이 수정된 갓을 확인할 수 있습니다.

<img width="1032" alt="스크린샷 2024-10-03 오후 4 05 21" src="https://github.com/user-attachments/assets/feeca20d-ecf7-4fb3-ab83-3d9403e101a6">

<br><br>

### 3. 이제 클래스의 속성은 알았으니 사용자의 요청값과 매칭할 시간입니다.

##### MappingJackson2HttpMessageConverter의 read 메서드 호출

- 위에서는 클래스의 속성값을 파악하였으며, 아래 로직에서는 사용자의 값과 클래스의 속성을 매치시키게 됩니다.
- genericConverter 변수는 MappingJackson2HttpMessageConverter가 할당받게 됩니다. 그리고 MappingJackson2HttpMessageConverter의 클래스의 read(...) 메서드가 호출되는데, MappingJackson2HttpMessageConverter 클래스는 read() 메서드를 가지고 있지 않아, 상위 클래스인 AbstractJsonHttpMessageConverter 클래스의 read() 메서드가 호출됩니다.

<img width="1032" alt="스크린샷 2024-10-03 오후 2 05 39" src="https://github.com/user-attachments/assets/17df428d-3379-4ad1-94ad-3b0cd119aad8">

<br><br>

#### BeanDeserializer 클래스

- BeanDeserializer클래스의 deserializeFromObject() 메서드에서 사용자가 입력한 값을 this._beanProperties.find(...) 메서드를 통해 찾습니다. (내부 로직은 해시를 이용하고 있습니다.)
- 이렇게 사용자의 요청값과 객체의 속성이 매칭이 되는 것이였습니다. 하지만 _beanProperties에는 boolean 타입인 isPersonalInformationConsent 속성은 없기 때문에 결국 찾지 못하여 기본값인 false가 할당되는 것입니다.

<img width="1036" alt="스크린샷 2024-10-01 오후 5 13 45" src="https://github.com/user-attachments/assets/dde727ac-6e65-4e60-8d66-fbb28916597f">

<br><br>

## 🧐 Record는 왜 매핑이 잘되는걸까?

#### POJOPropertiesCollector 클래스를 다시보자.

- POJOPropertiesCollector 클래스의 collectAll() 메서드 내부에서 레코드 클래스라면 _addFields(...) 메서드가 호출되지 않습니다.
- 이후 로직에서도 Property 들도 삭제가 되지 않고 그대로 남아있게 됩니다. 결국 남아있는 Property들은 BeanDeserializer 클래스의 deserializeFromObject() 메서드 내부에서 사용자가 입력한 값과 매칭이 이루어지게 되는 것입니다.

<img width="1026" alt="스크린샷 2024-10-03 오후 4 22 09" src="https://github.com/user-attachments/assets/23f8a0a4-32a3-4a20-b46c-edc0796c27c5">

<br><br>

#### 정리

- 평소 사용자의 입력값을 받을 때 boolean 타입으로 받게되면 문제가 발생하니, Boolean 타입을 사용하자라고 그냥 넘어갔었는데, 이번 기회에 이러한 이유로 제대로 매핑이 안되었구나라는 것을 알게 되었습니다. 그리고 코틀린에서는 버그를 수정했다고 하는데 왜 자바는.... 자바도 해줬으면 좋겠지만 자바 17 버전 이후부터는 DTO 클래스를 record 타입을 사용하니 이러한 버그를 만나는 횟수는 줄어든거 같습니다.
- 디버깅은 험난하고 힘들다라는 것도 알게되었습니다...



