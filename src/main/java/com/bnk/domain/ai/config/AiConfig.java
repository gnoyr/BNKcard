package com.bnk.domain.ai.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


@Configuration
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
public class AiConfig {
	@Bean
    ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20) 
                .build();
    }
	
	@Bean
    @Primary
    ChatModel primaryChatModel(@Qualifier("googleGenAiChatModel") ChatModel chatModel) {
        return chatModel;
    }
	
	@Bean
	@Primary
	public EmbeddingModel primaryEmbeddingModelgenai(
	        @Qualifier("googleGenAiTextEmbedding") EmbeddingModel embeddingModel
	) {
	    return embeddingModel;
	}
	
	//@Bean
    //@Primary	
    public EmbeddingModel primaryEmbeddingModel(@Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel) {
        return embeddingModel;
    }
}
