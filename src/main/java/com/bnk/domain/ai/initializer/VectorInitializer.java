package com.bnk.domain.ai.initializer;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.bnk.domain.ai.service.CardVectorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class VectorInitializer {

    private final CardVectorService cardVectorService;

    // 앱 시작 시 자동 실행
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        log.info("Qdrant 카드 데이터 인덱싱 시작...");
        cardVectorService.indexAllCards();
    }
}
