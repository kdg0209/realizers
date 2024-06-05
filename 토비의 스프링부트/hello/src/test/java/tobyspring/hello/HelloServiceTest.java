package tobyspring.hello;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HelloServiceTest {

	@Test
	void simpleHelloService() {

		// given
		SimpleHelloService service = new SimpleHelloService();

		// when
		String result = service.hello("Spring");

		// then
		assertThat(result).isEqualTo("Hello Spring");
	}

	@Test
	void helloDecorator() {

		// given
		HelloDecorator helloDecorator = new HelloDecorator(name -> name);

		// when
		String result = helloDecorator.hello("Test");

		// then
		assertThat(result).isEqualTo("*Test*");
	}
}