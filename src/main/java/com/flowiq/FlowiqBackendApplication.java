package com.flowiq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FlowiqBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(FlowiqBackendApplication.class, args);
	}

}
