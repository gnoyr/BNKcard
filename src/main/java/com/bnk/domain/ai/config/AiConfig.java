package com.bnk.domain.ai.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class AiConfig {
	@Bean
    public ChatMemory chatMemory() {
        // 최근 20개의 메시지만 유지하여 토큰 낭비를 막는 윈도우 방식 메모리 사용
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20) 
                .build();
    }
}
