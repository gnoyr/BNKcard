package com.bnk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BnKcardApplication {

	public static void main(String[] args) {
		SpringApplication.run(BnKcardApplication.class, args);
	}

}
