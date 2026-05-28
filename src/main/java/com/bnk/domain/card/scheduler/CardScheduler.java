package com.bnk.domain.card.scheduler;

import com.bnk.domain.card.mapper.CardMapper;   // ← CardMapper2 → CardMapper
import com.bnk.domain.card.model.Card;          // ← model2.Card → model.Card
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 카드 상태 자동 전환 스케줄러 (리팩토링)
 *
 * 변경 이력:
 *  - CardMapper2 → CardMapper (단일 Mapper)
 *  - model2.Card → model.Card
 *  - 메서드명 변경:
 *      getApprovedReadyCards() → findApprovedReadyCards()
 *      publishCards()          → publishCards()           (동일)
 *      publishCardVersions()   → publishCardVersions()    (동일)
 *      getExpiredCards()       → findExpiredCards()
 *      expireCards()           → expireCards()            (동일)
 *      expireCardVersion()     → expireCardVersions()     (복수형 통일)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardScheduler {

    private final CardMapper cardMapper;   // ← CardMapper2 대체

    /**
     * APPROVED → PUBLISHED 자동 전환
     * publish_start_at <= SYSTIMESTAMP && APPROVED 상태 카드를 PUBLISHED로 전환
     * 매 5분마다 실행
     */
    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void publishApprovedCards() {
        List<Card> readyCards = cardMapper.findApprovedReadyCards();   // ← getApprovedReadyCards()

        if (readyCards.isEmpty()) {
            return;
        }

        List<Long> cardIds = readyCards.stream()
                .map(Card::getCardId)
                .collect(Collectors.toList());

        cardMapper.publishCards(cardIds);
        cardMapper.publishCardVersions(cardIds);

        log.info("[스케줄러] APPROVED → PUBLISHED 전환 완료: {}건, cardIds={}",
                cardIds.size(), cardIds);
    }

    /**
     * PUBLISHED → EXPIRED 자동 만료
     * publish_end_at < SYSTIMESTAMP && PUBLISHED 상태 카드를 EXPIRED로 전환
     * 매 10분마다 실행
     */
    @Scheduled(fixedDelay = 600_000)
    @Transactional
    public void expirePublishedCards() {
        List<Card> expiredCards = cardMapper.findExpiredCards();       // ← getExpiredCards()

        if (expiredCards.isEmpty()) {
            return;
        }

        List<Long> cardIds = expiredCards.stream()
                .map(Card::getCardId)
                .collect(Collectors.toList());

        cardMapper.expireCards(cardIds);
        cardMapper.expireCardVersions(cardIds);                        // ← expireCardVersion() (복수형)

        log.info("[스케줄러] PUBLISHED → EXPIRED 전환 완료: {}건, cardIds={}",
                cardIds.size(), cardIds);
    }
}
