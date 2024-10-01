# Jackson 직렬화 역직렬화 과정에 대해

- 이 글에서는 스프링 부트(3.3.x) 버전을 사용하여 테스트를 진행하였습니다.
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

### 🧐 사용자의 요청은 어떤 과정을 통해 객체의 속성과 매칭될까?

#### AbstractMessageConverterMethodArgumentResolver 클래스에 대해 살펴보야 합니다.

- 해당 클래스에서 봐야할 부분은 많지만 그 중 가장 중요한 역할을 수행하는 메서드는 readWithMessageConverters() 메서드입니다.
- while 문을 순회하면서 MappingJackson2HttpMessageConverter가 선택이 되며, MappingJackson2HttpMessageConverter 클래스의 canRead(...) 메서드가 수행되지만 MappingJackson2HttpMessageConverter 클래스에는 canRead(...) 메서드가 선언되어 있지 않아 상위 클래스인 AbstractJackson2HttpMessageConverter canRead(...) 메서드가 수행되게 됩니다.

<img width="1032" alt="스크린샷 2024-10-01 오후 4 48 39" src="https://github.com/user-attachments/assets/9463b6a1-5f29-4675-80ee-500ac0fa37e0">

<br> 

#### 🚩 canRead() 메서드 이녀석이 가장 중요했습니다..!!

- canRead() 메서드 내부에서는 정~~~말 많은 일들이 일어나고 있었습니다. 처음에는 boolean 값만 반환하니 별 로직이 없겠거니 생각을 하고 깊게 안보고 있었는데.... 사실 클래스의 속성을 변환해주고 있었던것입니다. 그럼 이름이 canRead와 실제 로직이 이루어지는 행위가 분리되어야하는게 아닌가? 라는 생각이 들었습니다.

#### BeanDeserializerFactory 클래스에 대해 살펴보아야 합니다.

- canRead 메서드를 통해 buildBeanDeserializer() 메서드가 호출되며, 최종적으로는 BeanDeserializer를 반환하게 되며, property의 값은 memberId, password, isMarketingConsent만 담기게 됩니다.
- buildBeanDeserializer() 메서드를 보면 내부에서 addBeanProps(...) 메서드를 호출하고 있습니다.

<img width="1032" alt="스크린샷 2024-10-01 오후 4 00 47" src="https://github.com/user-attachments/assets/cacdd77c-2894-4a72-b3f5-a770cdf4db36">

<br>

#### addBeanProps() 메서드 중 일부

- 아래 사진은 addBeanProps() 메서드 중 일부입니다. propDef.hasField() 메서드를 통해 Boolean 타입이라면 if 분기의 내부 로직을 수행하고 boolean 타입이라면 else 분기문을 수행하게 됩니다.
- 하지만 여기서 문제가 prop 변수에 결과값을 할당하게 되는데, 이 prop 변수가 나중에 NULL이라면 builder.addProperty(prop)에 담아줄 수 없는 것입니다.

<img width="1032" alt="스크린샷 2024-10-01 오후 4 24 55" src="https://github.com/user-attachments/assets/8061e6cf-b3e0-41b4-b02b-833628742dd8">

<br><br>

- 위에서 언급했던거처럼 prop가 NULL이라면 BeanDeserializerBuilder의 property에 추가할 수 없으므로 객체의 속성이 boolean isPersonalInformationConsent라면 속성이 추가가 되지 않는 것입니다.

<img width="1029" alt="스크린샷 2024-10-01 오후 4 34 10" src="https://github.com/user-attachments/assets/d0484d17-eb4d-4729-af67-bb40b9b7277c">

<br><br>

### 🧐 이제 클래스의 속성은 알았으니 사용자의 요청값과 매칭할 시간입니다.

##### MappingJackson2HttpMessageConverter의 read 메서드 호출

- 위에서는 클래스의 속성값을 파악하였으며, 아래 로직에서는 사용자의 값과 클래스의 속성을 매치시키게 됩니다.
- genericConverter 변수는 MappingJackson2HttpMessageConverter가 할당받게 됩니다. 그리고 MappingJackson2HttpMessageConverter의 클래스의 read(...) 메서드가 호출되는데, MappingJackson2HttpMessageConverter 클래스는 read() 메서드를 가지고 있지 않아, 상위 클래스인 AbstractJsonHttpMessageConverter 클래스의 read() 메서드가 호출됩니다.

<img width="1032" alt="스크린샷 2024-10-01 오후 3 35 36" src="https://github.com/user-attachments/assets/f4050cd1-fffb-4db4-b6a8-edb9d0d9617e">

<br><br>

### BeanDeserializer 클래스

- BeanDeserializer클래스의 deserializeFromObject() 메서드에서 사용자가 입력한 값을 this._beanProperties.find(...) 메서드를 통해 찾습니다. (내부 로직은 해시를 이용하고 있습니다.)
- 이렇게 사용자의 요청값과 객체의 속성이 매칭이 되는 것이였습니다. 하지만 _beanProperties에는 boolean 타입인 isPersonalInformationConsent 속성은 없기 때문에 결국 찾지 못하여 기본값인 false가 할당되는 것입니다.

<img width="1036" alt="스크린샷 2024-10-01 오후 5 13 45" src="https://github.com/user-attachments/assets/dde727ac-6e65-4e60-8d66-fbb28916597f">










