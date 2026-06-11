package com.bnk.domain.card.scheduler;

import com.bnk.domain.ai.service.CardVectorService;
import com.bnk.domain.card.mapper.CardMapper;
import com.bnk.domain.card.model.Card;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CardScheduler {

    private final CardMapper cardMapper;

    // ai.enabled=false 환경에서는 빈이 없으므로 @Autowired(required=false)
    @Autowired(required = false)
    private CardVectorService cardVectorService;

    /**
     * APPROVED → PUBLISHED 자동 전환 + Qdrant upsert
     * 매 5분마다 실행
     */
    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void publishApprovedCards() {
        List<Card> readyCards = cardMapper.findApprovedReadyCards();

        if (readyCards.isEmpty()) {
            return;
        }

        List<Long> cardIds = readyCards.stream()
                .map(Card::getCardId)
                .collect(Collectors.toList());

        // 기존 로직 — 그대로 유지
        cardMapper.publishCards(cardIds);
        cardMapper.publishCardVersions(cardIds);

        log.info("[스케줄러] APPROVED → PUBLISHED 전환 완료: {}건, cardIds={}",
                cardIds.size(), cardIds);

        // ↓ 추가: PUBLISHED된 카드를 Qdrant에 upsert
        if (cardVectorService != null) {
            for (Long cardId : cardIds) {
                try {
                    cardVectorService.upsertCard(cardId);
                } catch (Exception e) {
                    // Qdrant 실패해도 메인 트랜잭션 롤백 안 함
                    log.warn("[스케줄러] Qdrant upsert 실패 (무시): cardId={}, error={}",
                            cardId, e.getMessage());
                }
            }
            log.info("[스케줄러] Qdrant upsert 완료: {}건", cardIds.size());
        }
    }

    /**
     * PUBLISHED → EXPIRED 자동 만료 + Qdrant 삭제
     * 매 10분마다 실행
     */
    @Scheduled(fixedDelay = 600_000)
    @Transactional
    public void expirePublishedCards() {
        List<Card> expiredCards = cardMapper.findExpiredCards();

        if (expiredCards.isEmpty()) {
            return;
        }

        List<Long> cardIds = expiredCards.stream()
                .map(Card::getCardId)
                .collect(Collectors.toList());

        // 기존 로직 — 그대로 유지
        cardMapper.expireCards(cardIds);
        cardMapper.expireCardVersions(cardIds);

        log.info("[스케줄러] PUBLISHED → EXPIRED 전환 완료: {}건, cardIds={}",
                cardIds.size(), cardIds);

        // ↓ 추가: EXPIRED된 카드를 Qdrant에서 삭제
        if (cardVectorService != null) {
            for (Long cardId : cardIds) {
                try {
                    cardVectorService.deleteCard(cardId);
                } catch (Exception e) {
                    log.warn("[스케줄러] Qdrant 삭제 실패 (무시): cardId={}, error={}",
                            cardId, e.getMessage());
                }
            }
            log.info("[스케줄러] Qdrant 삭제 완료: {}건", cardIds.size());
        }
    }
}