package org.tk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = {"org.tk", "org.ltk.connector"})
@SpringBootApplication
public class RnsliveApplication {

	public static void main(String[] args) {
		SpringApplication.run(RnsliveApplication.class, args);
	}

}
