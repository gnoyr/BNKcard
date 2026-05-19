package com.bnk;

import org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration;
import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration;
import org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(exclude = {
    QdrantVectorStoreAutoConfiguration.class,   // Qdrant 벡터 DB 미실행
    GoogleGenAiChatAutoConfiguration.class,     // Gemini API 키 미설정
    ChatClientAutoConfiguration.class           // ChatModel 빈 없음
})
@ComponentScan(
    basePackages = "com.bnk",
    excludeFilters = {
        // AI 기능 전체 비활성화 (Gemini API + Qdrant 연동 전까지)
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.bnk\\.domain\\.ai\\..*"
        ),
        // CardSearchService 도 Qdrant VectorStore 의존 → 함께 제외
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.bnk\\.domain\\.spending\\.service\\.CardSearchService"
        )
    }
)
public class BnKcardApplication {

    public static void main(String[] args) {
        SpringApplication.run(BnKcardApplication.class, args);
    }

}