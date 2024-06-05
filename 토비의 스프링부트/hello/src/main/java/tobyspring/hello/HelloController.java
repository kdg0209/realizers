package tobyspring.hello;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hello")
public class HelloController {

	private final HelloService helloService;

	public HelloController(HelloService helloService) {
		this.helloService = helloService;
	}

	@GetMapping
	public String hello(String name) {
		if (name == null || name.isEmpty()) {
			throw new IllegalArgumentException();
		}
		return helloService.hello(name);
	}

}
