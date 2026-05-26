package com.bnk.domain.ai.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.bnk.domain.ai.mapper.CardVectorMapper;
import com.bnk.domain.ai.model.CardVector;

import org.springframework.ai.document.Document;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
public class CardVectorService {

    private final CardVectorMapper cardVectorMapper;
    private final VectorStore vectorStore;

    public void indexAllCards() {
        List<CardVector> cards = 
            cardVectorMapper.findAllCardsWithBenefits();

        List<Document> documents = cards.stream()
            .map(this::cardToDocument)
            .collect(Collectors.toList());

        vectorStore.add(documents);
        log.info("총 {}개 카드 인덱싱 완료!", documents.size());
    }


    private Document cardToDocument(CardVector card) {

        StringBuilder text = new StringBuilder();

        text.append("카드명: ").append(card.getCardName()).append("\n");
        text.append("카드사: ").append(card.getCompanyName()).append("\n");
        text.append("카드종류: ").append(formatCardType(card.getCardType())).append("\n");
        text.append("국내연회비: ").append(formatAmount(card.getAnnualFeeDomestic())).append("원\n");
        text.append("해외연회비: ").append(formatAmount(card.getAnnualFeeOverseas())).append("원\n");
        text.append("전월실적: ").append(formatAmount(card.getPreviousMonthSpend())).append("원\n");

        if (card.getTargetUser() != null) {
            text.append("대상: ").append(card.getTargetUser()).append("\n");
        }
        if (card.getSummaryDescription() != null) {
            text.append("카드설명: ").append(card.getSummaryDescription()).append("\n");
        }

        if (card.getBenefits() != null && !card.getBenefits().isEmpty()) {
            text.append("\n주요혜택:\n");
            for (CardVector.BenefitVector benefit : card.getBenefits()) {
                text.append("- ");
                if (benefit.getDisplayText() != null) {
                    text.append(benefit.getDisplayText());
                } else {
                    text.append(benefit.getBenefitTitle());
                }
                if (benefit.getDiscountRate() != null) {
                    text.append(" (할인율: ")
                        .append(benefit.getDiscountRate()
                            .multiply(BigDecimal.valueOf(100)))
                        .append("%)");
                }
                if (benefit.getCashbackRate() != null) {
                    text.append(" (캐시백: ")
                        .append(benefit.getCashbackRate()
                            .multiply(BigDecimal.valueOf(100)))
                        .append("%)");
                }
                if (benefit.getPointRate() != null) {
                    text.append(" (포인트: ")
                        .append(benefit.getPointRate()
                            .multiply(BigDecimal.valueOf(100)))
                        .append("%)");
                }
                text.append("\n");
            }
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("card_id", card.getCardId());
        metadata.put("card_name", card.getCardName());
        metadata.put("card_type", card.getCardType());
        metadata.put("company_name", card.getCompanyName());
        metadata.put("annual_fee_domestic", card.getAnnualFeeDomestic());

        return new Document(text.toString(), metadata);
    }

    private String formatCardType(String type) {
        if (type == null) {
            return "알 수 없음";
        }
        
        return switch (type.toUpperCase().trim()) {
            case "CREDIT" -> "신용카드";
            case "CHECK" -> "체크카드";
            case "PREPAID" -> "선불카드";
            case "HYBRID" -> "하이브리드카드";
            default -> type;
        };
    }

    private String formatAmount(Number amount) {
        if (amount == null) return "0";
        return String.format("%,d", amount.longValue());
    }
}
