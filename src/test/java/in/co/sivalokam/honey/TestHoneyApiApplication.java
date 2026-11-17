package in.co.sivalokam.honey;

import org.springframework.boot.SpringApplication;

public class TestHoneyApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(HoneyApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
