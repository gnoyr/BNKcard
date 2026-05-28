package com.bnk.domain.card.scheduler;

import com.bnk.domain.card.mapper.CardMapper; 
import com.bnk.domain.card.mapper.CardStatusHistoryMapper;
import com.bnk.domain.card.model.Card;
import com.bnk.domain.card.model.CardStatusHistory;

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

    private final CardMapper              cardMapper;
    private final CardStatusHistoryMapper cardStatusHistoryMapper;

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void publishApprovedCards() {
        List<Card> readyCards = cardMapper.findApprovedReadyCards();
        if (readyCards.isEmpty()) return;

        List<Long> cardIds = readyCards.stream()
                .map(Card::getCardId)
                .collect(Collectors.toList());

        cardMapper.publishCards(cardIds);
        cardMapper.publishCardVersions(cardIds);

        // 상태 이력 insert
        readyCards.forEach(card ->
            cardStatusHistoryMapper.insertCardStatusHistory(
                CardStatusHistory.builder()
                    .cardId(card.getCardId())
                    .previousStatus("APPROVED")
                    .changedStatus("PUBLISHED")
                    .changedReason("스케줄러 자동 전환")
                    .build()
            )
        );

        log.info("[스케줄러] APPROVED → PUBLISHED 전환 완료: {}건", cardIds.size());
    }

    @Scheduled(fixedDelay = 600_000)
    @Transactional
    public void expirePublishedCards() {
        List<Card> expiredCards = cardMapper.findExpiredCards();
        if (expiredCards.isEmpty()) return;

        List<Long> cardIds = expiredCards.stream()
                .map(Card::getCardId)
                .collect(Collectors.toList());

        cardMapper.expireCards(cardIds);
        cardMapper.expireCardVersions(cardIds);

        // 상태 이력 insert
        expiredCards.forEach(card ->
            cardStatusHistoryMapper.insertCardStatusHistory(
                CardStatusHistory.builder()
                    .cardId(card.getCardId())
                    .previousStatus("PUBLISHED")
                    .changedStatus("EXPIRED")
                    .changedReason("스케줄러 자동 만료")
                    .build()
            )
        );

        log.info("[스케줄러] PUBLISHED → EXPIRED 전환 완료: {}건", cardIds.size());
    }
}