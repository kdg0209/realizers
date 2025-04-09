# Model I/O 언어 모델을 다루기 쉽게 만들기

<br>

## 1. Model I/O는 랭체인의 가장 기본적인 모듈이다.

### 1-1. Language models

- `Language models` 모듈은 다양한 언어 모델을 동일한 인터페이스에서 호출할 수 있는 기능을 제공합니다. OpenAI뿐만 아니라 PerplexityAI 언어 모델도 호출이 가능하다.

<br>

### 1-2. Prompts

- `Prompts` 모듈은 언어 모델을 호출하기 위한 프롬프트를 구축하는데 유용한 기능을 제공합니다.
- `spring-ai` 라이브러리를 사용하면 `AbstractMessage` 클래스가 있으며 이를 상속하고 있는 주요 3가지 클래스가 있습니다.

#### UserMessage

- 사용자의 질문을 담는 Prompts 클래스 중 하나입니다.

#### SystemMessage

- AI의 행동과 응답 스타일 정의하며, 대화를 시작하기 전에 AI에게 지시 사항을 제공합니다.
- 응답 포맷이나 역할을 부여할 수 있습니다.

#### AssistantMessage

- 대화의 흐름을 유지하고, 일관성과 대화 히스토리 기반으로 답변을 할 수 있도록 도와줍니다.

#### 예시

- 아래 2가지 엔드 포인트를 가지는 메서드가 있습니다.
- 하나는 `AssistantMessage` 사용하고 있고, 다른 API는 `AssistantMessage`를 사용하고 있습니다. 두 번째 요청을 통해 `Assistant`가 어떤 도움을 주는지 알 수 있습니다.

```java
@RestController
@RequestMapping("/prompt")
@RequiredArgsConstructor
public class PromptController {

    private final OpenAiChatModel openAiChatModel;

    @GetMapping("/ai/use-assistant")
    public Map<String,String> useAssistant(@RequestParam(value = "question") String question) {
        PromptTemplate promptTemplate = new PromptTemplate(question);
        promptTemplate.add("question", question);

        String render = promptTemplate.render();
        Message userMessage = new UserMessage(render);
        Message systemMessage = new SystemMessage(
                "당신은 항상 존댓말을 사용해야 합니다. " +
                        "그리고 상대를 '형님'이라고 부르며 공손하게 대해야 합니다. " +
                        "답변의 끝에는 항상 '행님!!'을 붙여야 합니다."
        );

        // 이전 AssistantMessage를 활용하는 로직 추가 (간단한 메모리 저장 방식)
        Message latestAssistantMessage = MessageHistory.getLatestAssistantMessage();

        String response;
        if (latestAssistantMessage != null) {
            response = openAiChatModel.call(systemMessage, latestAssistantMessage, userMessage);
        } else {
            response = openAiChatModel.call(systemMessage, userMessage);
        }

        // 새로운 AssistantMessage 저장
        MessageHistory.saveAssistantMessage(new AssistantMessage(response));
        return Map.of("response", response);
    }

    @GetMapping("/ai/not-used-assistant")
    public Map<String,String> notUsedAssistant(@RequestParam(value = "question") String question) {
        PromptTemplate promptTemplate = new PromptTemplate(question);
        promptTemplate.add("question", question);

        String render = promptTemplate.render();
        Message userMessage = new UserMessage(render);
        Message systemMessage = new SystemMessage(
                "당신은 항상 존댓말을 사용해야 합니다. " +
                        "그리고 상대를 '형님'이라고 부르며 공손하게 대해야 합니다. " +
                        "답변의 끝에는 항상 '행님!!'을 붙여야 합니다."
        );

        String response = openAiChatModel.call(userMessage, systemMessage);
        return Map.of("response", response);
    }

    class MessageHistory {
        
        private static final Deque<Message> MESSAGE_HISTORY = new ArrayDeque<>();

        // 마지막 AssistantMessage 가져오기
        public static Message getLatestAssistantMessage() {
            return MESSAGE_HISTORY.stream()
                    .filter(msg -> msg instanceof AssistantMessage)
                    .reduce((first, second) -> second) // 가장 최신 메시지 가져오기
                    .orElse(null);
        }

        // 새로운 AssistantMessage 저장
        public static void saveAssistantMessage(Message message) {
            if (message instanceof AssistantMessage) {
                MESSAGE_HISTORY.addLast(message);

                // 저장된 메시지가 너무 많아지면 오래된 것 삭제 (예: 10개 유지)
                if (MESSAGE_HISTORY.size() > 10) {
                    MESSAGE_HISTORY.removeFirst();
                }
            }
        }
    }
}
```

<br>

#### 💡 AssistantMessage를 사용한 경우

- `Assistant`를 사용하면 AI와 티키타카가 가능해집니다.

1. 첫 번째 요청

```text
질문: http://localhost:8080/prompt/ai/use-assistant?question=애플이 개발한 대표적인 제품은?

응답: {
    "response": "애플이 개발한 대표적인 제품으로는 아이폰, 아이패드, 맥북, 애플 워치 등이 있습니다. 이외에도 애플은 애플 뮤직, 애플 TV+와 같은 서비스도 제공하고 있습니다. 형님!!"
}
```

2. 두 번째 요청

```text
질문: http://localhost:8080/prompt/ai/use-assistant?question=그중에서 가장 인기 있는 제품은?"

응답: {
    "response": "형님, 애플 제품 중에서 가장 인기 있는 제품은 아이폰이라고 할 수 있습니다. 아이폰은 세계적으로 높은 판매량을 기록하며 많은 사람들에게 사랑받고 있습니다. 그 외에도 아이패드와 맥북도 많은 인기를 끌고 있습니다. 형님!!"
}
```

3. 세 번째 요청

```text
질문: http://localhost:8080/prompt/ai/use-assistant?question=아이폰? 그게 먼데

응답: {
    "response": "형님, 아이폰은 애플에서 제작한 스마트폰입니다. 다양한 기능을 갖추고 있어 통화, 문자, 인터넷 검색, 사진 촬영 등 여러 가지 용도로 사용할 수 있습니다. 또한, 앱스토어를 통해 다양한 어플리케이션을 다운로드하여 사용할 수 있는 점이 큰 장점입니다. 형님!!"
}
```

<br>

#### 💡 AssistantMessage를 사용하지 않는 경우

1. 첫 번째 요청

```text
질문: http://localhost:8080/prompt/ai/not-used-assistant?question=애플이 개발한 대표적인 제품은?

응답: {
    "response": "애플이 개발한 대표적인 제품으로는 아이폰, 아이패드, 맥북, 애플워치, 그리고 아이맥 등이 있습니다. 이들 제품은 모두 혁신적인 디자인과 기능으로 많은 사랑을 받고 있습니다. 형님!!"
}
```

2. 두 번째 요청

```text
질문: http://localhost:8080/prompt/ai/not-used-assistant?question=그중에서 가장 인기 있는 제품은?"

응답: {
    "response": "형님, 가장 인기 있는 제품은 특정 카테고리나 시장에 따라 다를 수 있습니다. 예를 들어, 전자제품 분야에서는 스마트폰이나 노트북이 인기가 많고, 패션 분야에서는 특정 브랜드의 의류나 액세서리가 인기를 끌 수 있습니다. 좀 더 구체적인 정보가 필요하시면 말씀해 주시면 더욱 자세히 알려드리겠습니다. 행님!!"
}
```

<br>

### 1-3. Output parsers

- `Output parsers` 모듈은 언어 모델에서 얻은 출력을 분석해 애플리케이션에서 사용하기 쉬운 형태로 변환하는 기능을 제공합니다.

<br>

## 2. Language models에 대해 조금 더 알아보기

- Model I/O 모듈의 하위 모듈인 `Language models`의 목적은 다양한 종류의 언어 모델을 통일된 인터페이스를 통해 쉽게 다룰 수 있도록 하는 것입니다.

### 2-1. Chat models와 LLMs

- `Language models`에는 사용하는 언어 모델에 따라 크게 두 가지 모듈이 있습니다.

#### Chat models

- OpenAI의 `Chat` 모델과 같은 대화 형식으로 사용하는 언어 모델을 다루는 `Chat models`입니다.
- 지금까지 위에서 본 예제들이 `Chat models`에 해당합니다.

#### LLMs

- OpenAI의 `Complete` 모델과 같은 문장의 연속을 준비하는 언어 모델을 다루는 `LLMs`입니다.
- `LLMs`는 대화가 아닌 문장의 연속을 예측한다고 합니다.

<br>

### 2-2. 캐싱

- OpenAI는 사용한 토큰 수에 따라 요금과 제한이 발생합니다. 따라서 같은 프롬프트를 재사용할 필요가 있으며, `In-Memory-Cache`등을 활용하면 캐싱된 응답을 받거나, 더 효율적으로 작업할 수 있습니다.

<br>

## 3. 프롬프트 구축의 효율성 향상

### 3-1. 프롬프트 엔지니어링을 통한 결과 최적화

- 프롬프트를 최적화하면 단순한 명령어로는 어려웠던 작업을 수행할 수 있게 되는데, 이 과정을 프롬프트 최적화라고하고 `프롬프트 엔지니어링`이라 합니다.

#### 출력 예제가 포함된 프롬프트

- `UserMessage`나 `UserMessage`를 통해서 답변 양식을 지정하면 언어 모델이 양식에 맞춰 답변을 제공합니다. 예를들어 답변은 JSON 형식이고 속성값은 xxx이 있다. 라고 하면 그에 맞춰 답변을 제공해줍니다.
- 또한 최신에는 구조화된 응답을 받아올 수 있는 기능이 최신으로 나왔습니다.
    - https://platform.openai.com/docs/guides/structured-outputs


