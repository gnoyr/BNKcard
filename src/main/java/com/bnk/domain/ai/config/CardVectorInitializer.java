package com.bnk.domain.ai.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.bnk.domain.ai.service.CardVectorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
public class CardVectorInitializer implements ApplicationRunner {

    private final CardVectorService cardVectorService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[CardVector] 서버 시작 시 카드 벡터 인덱싱 시작...");
        try {
            cardVectorService.indexAllCards();
        } catch (Exception e) {
            log.error("[CardVector] 인덱싱 실패 — 챗봇 검색 품질이 저하될 수 있습니다: {}", e.getMessage());
        }
    }
}
