package com.bnk.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * @Async 활성화 — EmailService의 비동기 이메일 발송에 사용
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}