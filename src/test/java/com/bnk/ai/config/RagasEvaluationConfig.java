package com.bnk.ai.config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.bnk.ai.qa.solutions.chatclient.ChatClientStore;
import com.bnk.ai.qa.solutions.embedding.EmbeddingModelStore;
import com.bnk.ai.qa.solutions.execution.MultiModelExecutor;
import com.bnk.ai.qa.solutions.metrics.general.AspectCriticMetric;
import com.bnk.ai.qa.solutions.metrics.retrieval.ContextPrecisionMetric;
import com.bnk.ai.qa.solutions.metrics.retrieval.ContextRecallMetric;
import com.bnk.ai.qa.solutions.metrics.retrieval.FaithfulnessMetric;
import com.bnk.ai.qa.solutions.metrics.retrieval.ResponseRelevancyMetric;

@Configuration
public class RagasEvaluationConfig {

	@Bean
	Clock clock() {
	    return Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
	}

    @Bean
    ChatClientStore chatClientStore(ChatClient.Builder builder) {
        ChatClient myChatClient = builder.build();
        Map<String, ChatClient> clients = new ConcurrentHashMap<>();
        clients.put("gemini-model", myChatClient);
        return new ChatClientStore(clients, myChatClient);
    }

    @Bean(name = "metricExecutor")
    AsyncTaskExecutor metricExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setThreadNamePrefix("Metric-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "httpExecutor")
    AsyncTaskExecutor httpExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setThreadNamePrefix("Http-");
        executor.initialize();
        return executor;
    }

    @Bean
    MultiModelExecutor multiModelExecutor(
            ChatClientStore chatClientStore,
            @Nullable EmbeddingModelStore embeddingModelStore,
            @Qualifier("metricExecutor") AsyncTaskExecutor metricExecutor,
            @Qualifier("httpExecutor") AsyncTaskExecutor httpExecutor,
            Clock clock) {
        return new MultiModelExecutor(
            chatClientStore,
            embeddingModelStore,
            metricExecutor,
            httpExecutor,
            clock
        );
    }

    @Bean
    AspectCriticMetric aspectCriticMetric(MultiModelExecutor multiModelExecutor, Clock clock) {
        return AspectCriticMetric.builder()
                .executor(multiModelExecutor)
                .clock(clock)
                .build();
    }

    @Bean
    ContextRecallMetric contextRecallMetric(MultiModelExecutor multiModelExecutor, Clock clock) {
        return ContextRecallMetric.builder()
                .executor(multiModelExecutor)
                .clock(clock)
                .build();
    }

    @Bean
    ResponseRelevancyMetric responseRelevancyMetric(MultiModelExecutor multiModelExecutor, Clock clock) {
        return ResponseRelevancyMetric.builder()
                .executor(multiModelExecutor)
                .clock(clock)
                .build();
    }

    @Bean
    FaithfulnessMetric faithfulnessMetric(MultiModelExecutor multiModelExecutor, Clock clock) {
        return FaithfulnessMetric.builder()
                .executor(multiModelExecutor)
                .clock(clock)
                .build();
    }

    @Bean
    ContextPrecisionMetric contextPrecisionMetric(MultiModelExecutor multiModelExecutor, Clock clock) {
        return ContextPrecisionMetric.builder()
                .executor(multiModelExecutor)
                .clock(clock)
                .build();
    }
}