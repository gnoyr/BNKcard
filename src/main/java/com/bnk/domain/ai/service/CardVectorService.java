package com.bnk.domain.ai.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.bnk.domain.ai.mapper.CardVectorMapper;
import com.bnk.domain.ai.model.CardVector;
import com.bnk.domain.ai.model.CardVector.BenefitVector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 카드 데이터를 Vector DB(Qdrant)에 인덱싱하는 서비스.
 *
 * <p>주요 책임:
 * <ul>
 *   <li>DB에서 카드 + 혜택 데이터 조회</li>
 *   <li>LLM 검색에 최적화된 자연어 Document 변환</li>
 *   <li>VectorStore(Qdrant)에 벌크 업서트</li>
 * </ul>
 *
 * <p>비고: {@code ai.enabled=true} 환경에서만 빈 등록됨.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
public class CardVectorService {

    // ── 상수 ─────────────────────────────────────────────────────────

    /** 카드타입 코드 → 한글 매핑 */
    private static final Map<String, String> CARD_TYPE_LABELS = Map.of(
            "CREDIT",  "신용카드",
            "CHECK",   "체크카드",
            "PREPAID", "선불카드",
            "HYBRID",  "하이브리드카드"
    );

    private static final String UNKNOWN_CARD_TYPE = "알 수 없음";

    /** BigDecimal → % 변환 배율 */
    private static final BigDecimal PERCENT_MULTIPLIER = BigDecimal.valueOf(100);

    // ── 의존성 ────────────────────────────────────────────────────────

    private final CardVectorMapper cardVectorMapper;
    private final VectorStore      vectorStore;

    // ══════════════════════════════════════════════════════════════════
    // Public API
    // ══════════════════════════════════════════════════════════════════

    /**
     * PUBLISHED 상태의 모든 카드를 Vector DB에 인덱싱한다.
     *
     * <p>빈 목록인 경우 VectorStore 호출 없이 조기 반환.
     *
     * @throws org.springframework.dao.DataAccessException  DB 조회 실패 시
     * @throws org.springframework.web.client.RestClientException Qdrant 연동 실패 시
     */
    @Transactional(readOnly = true)
    public void indexAllCards() {
        List<CardVector> cards = cardVectorMapper.findAllCardsWithBenefits();

        if (CollectionUtils.isEmpty(cards)) {
            log.warn("[CardVector] 인덱싱할 카드가 없습니다. (PUBLISHED + visible_yn=Y 조건 재확인 필요)");
            return;
        }

        List<Document> documents = cards.stream()
                .map(this::toDocument)
                .collect(Collectors.toList());

        vectorStore.add(documents);
        log.info("[CardVector] {}개 카드 인덱싱 완료", documents.size());
    }

    // ══════════════════════════════════════════════════════════════════
    // Document 변환
    // ══════════════════════════════════════════════════════════════════

    /**
     * {@link CardVector}를 Qdrant에 저장할 {@link Document}로 변환한다.
     *
     * <ul>
     *   <li>text  : LLM이 유사도 검색에 활용하는 자연어 텍스트</li>
     *   <li>metadata : 검색 결과 필터링·후처리에 사용하는 구조화 데이터</li>
     * </ul>
     */
    private Document toDocument(CardVector card) {
        String text     = buildDocumentText(card);
        Map<String, Object> metadata = buildMetadata(card);
        return new Document(text, metadata);
    }

    // ── 텍스트 빌더 ───────────────────────────────────────────────────

    private String buildDocumentText(CardVector card) {
        StringBuilder sb = new StringBuilder(256);

        appendCardBasicInfo(sb, card);
        appendCardOptionalInfo(sb, card);
        appendBenefits(sb, card.getBenefits());

        return sb.toString();
    }

    /** 카드 기본 정보 (항상 존재) */
    private void appendCardBasicInfo(StringBuilder sb, CardVector card) {
        sb.append("카드명: ").append(card.getCardName()).append('\n');
        sb.append("카드사: ").append(card.getCompanyName()).append('\n');
        sb.append("카드종류: ").append(resolveCardTypeLabel(card.getCardType())).append('\n');
        sb.append("국내연회비: ").append(formatAmount(card.getAnnualFeeDomestic())).append("원\n");
        sb.append("해외연회비: ").append(formatAmount(card.getAnnualFeeOverseas())).append("원\n");
        sb.append("전월실적: ").append(formatAmount(card.getPreviousMonthSpend())).append("원\n");
    }

    /** 카드 선택 정보 (null 가능) */
    private void appendCardOptionalInfo(StringBuilder sb, CardVector card) {
        if (card.getTargetUser() != null) {
            sb.append("대상: ").append(card.getTargetUser()).append('\n');
        }
        if (card.getSummaryDescription() != null) {
            sb.append("카드설명: ").append(card.getSummaryDescription()).append('\n');
        }
    }

    /** 혜택 목록 (비어있으면 생략) */
    private void appendBenefits(StringBuilder sb, List<BenefitVector> benefits) {
        if (CollectionUtils.isEmpty(benefits)) {
            return;
        }
        sb.append("\n주요혜택:\n");
        benefits.forEach(benefit -> appendBenefit(sb, benefit));
    }

    /** 혜택 단건 — 할인율/캐시백/포인트율 중 존재하는 것만 출력 */
    private void appendBenefit(StringBuilder sb, BenefitVector benefit) {
        sb.append("- ");

        // 표시 텍스트 우선, 없으면 혜택 제목 fallback
        String label = Optional.ofNullable(benefit.getDisplayText())
                .orElse(benefit.getBenefitTitle());
        sb.append(label);

        appendRateIfPresent(sb, "할인율",  benefit.getDiscountRate());
        appendRateIfPresent(sb, "캐시백",  benefit.getCashbackRate());
        appendRateIfPresent(sb, "포인트",  benefit.getPointRate());

        sb.append('\n');
    }

    /** 비율 값이 존재할 때만 "(레이블: X%)" 형식으로 추가 */
    private void appendRateIfPresent(StringBuilder sb, String label, BigDecimal rate) {
        if (rate == null) {
            return;
        }
        sb.append(" (")
          .append(label).append(": ")
          .append(rate.multiply(PERCENT_MULTIPLIER).stripTrailingZeros().toPlainString())
          .append("%)");
    }

    // ── 메타데이터 빌더 ───────────────────────────────────────────────

    /**
     * Qdrant 메타데이터로 저장할 카드 식별 정보.
     * AiChatService 후처리(카드 ID 추출 등)에서 활용된다.
     */
    private Map<String, Object> buildMetadata(CardVector card) {
        Map<String, Object> metadata = new HashMap<>(8);
        metadata.put("card_id",            card.getCardId());
        metadata.put("card_name",          card.getCardName());
        metadata.put("card_type",          card.getCardType());
        metadata.put("company_name",       card.getCompanyName());
        metadata.put("annual_fee_domestic", card.getAnnualFeeDomestic());
        return metadata;
    }

    // ══════════════════════════════════════════════════════════════════
    // 포맷 유틸
    // ══════════════════════════════════════════════════════════════════

    /**
     * 카드타입 코드를 한글 레이블로 변환.
     * 매핑 테이블에 없는 코드는 원본 값 그대로 반환한다.
     */
    private String resolveCardTypeLabel(String type) {
        if (type == null) {
            return UNKNOWN_CARD_TYPE;
        }
        return CARD_TYPE_LABELS.getOrDefault(type.toUpperCase().trim(), type);
    }

    /**
     * 숫자 금액을 천 단위 콤마 포맷으로 변환.
     * null인 경우 "0" 반환.
     */
    private String formatAmount(Number amount) {
        if (amount == null) {
            return "0";
        }
        return String.format("%,d", amount.longValue());
    }
}