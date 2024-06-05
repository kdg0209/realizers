package tobyspring.hello;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HelloControllerTest {

	@Test
	void helloController() {

		// given
		HelloController helloController = new HelloController(name -> name);

		// when
		String result = helloController.hello("test");

		// then
		assertThat(result).isEqualTo("test");
	}

	@Test
	void failsHelloController() {

		// given
		HelloController helloController = new HelloController(name -> null);

		// when && then
		assertThatThrownBy(() -> helloController.hello(null))
			.isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> helloController.hello(""))
			.isInstanceOf(IllegalArgumentException.class);
	}
}