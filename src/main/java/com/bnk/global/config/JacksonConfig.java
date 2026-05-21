package com.bnk.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    /**
     * @Bean ObjectMapper를 직접 정의하면 Spring Boot 자동 구성(Jackson2ObjectMapperAutoConfiguration)이
     * 무시된다. JavaTimeModule을 수동 등록하지 않으면 LocalDateTime 필드(LoginResponse,
     * ApprovalListResponse.requestedAt, AdminCardSearchRequest.publishStartFrom 등 전체)에서
     * InvalidDefinitionException → 500 오류 발생.
     * ErrorResponse의 @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")도 함께 무효화되므로
     * 반드시 아래 두 줄이 필요하다.
     */
    @Bean
    ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // ① Java 8+ 날짜/시간 타입(LocalDateTime, LocalDate 등) 직렬화 지원
        objectMapper.registerModule(new JavaTimeModule());

        // ② [1234567890123] 타임스탬프 숫자 대신 "2025-01-15 14:30:00" 문자열로 출력
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return objectMapper;
    }
}