package com.bnk.ai.config;

import com.bnk.ai.qa.solutions.chatclient.ChatClientStore;
import com.bnk.ai.qa.solutions.embedding.EmbeddingModelStore;
import com.bnk.ai.qa.solutions.execution.MultiModelExecutor;
import com.bnk.ai.qa.solutions.metrics.general.AspectCriticMetric;
import com.bnk.ai.qa.solutions.metrics.retrieval.ContextPrecisionMetric;
import com.bnk.ai.qa.solutions.metrics.retrieval.ContextRecallMetric;
import com.bnk.ai.qa.solutions.metrics.retrieval.FaithfulnessMetric;
import com.bnk.ai.qa.solutions.metrics.retrieval.ResponseRelevancyMetric;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

@Configuration
public class RagasEvaluationConfig {

	@Bean
    public ChatClientStore chatClientStore(ChatClient.Builder builder) {

        ChatClient myChatClient = builder.build();

        Map<String, ChatClient> clients = new ConcurrentHashMap<>();
        clients.put("gemini-model", myChatClient);

        return new ChatClientStore(clients, myChatClient); 
    }

	@Bean(name = "metricExecutor")
    public AsyncTaskExecutor metricExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1); 
        executor.setMaxPoolSize(1);
        executor.setThreadNamePrefix("Metric-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "httpExecutor")
    public AsyncTaskExecutor httpExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setThreadNamePrefix("Http-");
        executor.initialize();
        return executor;
    }

    @Bean
    public MultiModelExecutor multiModelExecutor(
            ChatClientStore chatClientStore,
            @Nullable EmbeddingModelStore embeddingModelStore,
            @Qualifier("metricExecutor") AsyncTaskExecutor metricExecutor,
            @Qualifier("httpExecutor") AsyncTaskExecutor httpExecutor) {
        
        return new MultiModelExecutor(
            chatClientStore, 
            embeddingModelStore, 
            metricExecutor, 
            httpExecutor
        );
    }
    
    @Bean
    public AspectCriticMetric aspectCriticMetric(MultiModelExecutor multiModelExecutor) {
        return AspectCriticMetric.builder()
                .executor(multiModelExecutor)
                .build();
    }
    
    @Bean
    public ContextRecallMetric contextRecallMetric(MultiModelExecutor multiModelExecutor) {
        return ContextRecallMetric.builder()
                .executor(multiModelExecutor)
                .build();
    }
    
    @Bean
    public ResponseRelevancyMetric responseRelevancyMetric(MultiModelExecutor multiModelExecutor) {
        return ResponseRelevancyMetric.builder()
                .executor(multiModelExecutor)
                .build();
    }

    @Bean
    public FaithfulnessMetric faithfulnessMetric(MultiModelExecutor multiModelExecutor) {
        return FaithfulnessMetric.builder()
                .executor(multiModelExecutor)
                .build();
    }

    @Bean
    public ContextPrecisionMetric contextPrecisionMetric(MultiModelExecutor multiModelExecutor) {
        return ContextPrecisionMetric.builder()
                .executor(multiModelExecutor)
                .build();
    }
}