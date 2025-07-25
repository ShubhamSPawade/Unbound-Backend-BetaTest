package com.unbound.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UnboundPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(UnboundPlatformApplication.class, args);
		System.out.println("\n########################################################");
		System.out.println("Unbound Platform Application started");
		System.out.println("########################################################");
	}

}
