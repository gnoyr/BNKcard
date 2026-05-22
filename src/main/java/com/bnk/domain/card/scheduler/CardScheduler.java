package com.bnk.domain.card.scheduler;

import com.bnk.domain.card.mapper.CardMapper2;
import com.bnk.domain.card.model2.Card;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CardScheduler {

    private final CardMapper2 cardMapper2;

    /**
     * APPROVED → PUBLISHED 자동 전환.
     * publish_start_at <= SYSTIMESTAMP 조건인 APPROVED 카드를 PUBLISHED로 전환.
     * 매 5분마다 실행.
     */
    @Scheduled(fixedDelay = 300_000)   // 5분마다
    @Transactional
    public void publishApprovedCards() {
        List<Card> readyCards = cardMapper2.getApprovedReadyCards();

        if (readyCards.isEmpty()) {
            return;
        }

        List<Long> cardIds = readyCards.stream()
                .map(Card::getCardId)
                .collect(Collectors.toList());

        cardMapper2.publishCards(cardIds);
        cardMapper2.publishCardVersions(cardIds);

        log.info("[스케줄러] APPROVED → PUBLISHED 전환 완료: {}건, cardIds={}",
                cardIds.size(), cardIds);
    }

    /**
     * PUBLISHED → EXPIRED 자동 만료.
     * publish_end_at < SYSTIMESTAMP 조건인 PUBLISHED 카드를 EXPIRED로 전환.
     * 매 10분마다 실행.
     */
    @Scheduled(fixedDelay = 600_000)   // 10분마다
    @Transactional
    public void expirePublishedCards() {
        List<Card> expiredCards = cardMapper2.getExpiredCards();

        if (expiredCards.isEmpty()) {
            return;
        }

        List<Long> cardIds = expiredCards.stream()
                .map(Card::getCardId)
                .collect(Collectors.toList());

        cardMapper2.expireCards(cardIds);
        cardMapper2.expireCardVersion(cardIds);

        log.info("[스케줄러] PUBLISHED → EXPIRED 전환 완료: {}건, cardIds={}",
                cardIds.size(), cardIds);
    }
}