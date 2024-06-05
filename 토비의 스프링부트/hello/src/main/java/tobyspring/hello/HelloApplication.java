package tobyspring.hello;

import org.springframework.boot.SpringApplication;

import tobyspring.config.MySpringBootApplication;

@MySpringBootApplication
public class HelloApplication {

	public static void main(String[] args) {
		SpringApplication.run(HelloApplication.class, args);
	}
}
