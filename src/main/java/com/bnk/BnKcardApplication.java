package com.bnk;

import org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {QdrantVectorStoreAutoConfiguration.class})
public class BnKcardApplication {

	public static void main(String[] args) {
		SpringApplication.run(BnKcardApplication.class, args);
	}

}
