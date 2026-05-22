package com.bnk.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.bnk.domain.ai.dto.AiChatRequest;
import com.bnk.domain.ai.dto.AiChatResponse;
import com.bnk.domain.ai.service.AiChatService;
import com.bnk.domain.spending.service.CardSearchService;

import ai.qa.solutions.metrics.general.AspectCriticMetric;
import ai.qa.solutions.metrics.retrieval.ContextRecallMetric;
import ai.qa.solutions.sample.Sample;

@SpringBootTest
class AiChatEvaluationTest {

    @Autowired
    private AiChatService aiChatService; // 작성하신 실제 서비스

    @Autowired
    private CardSearchService cardSearchService; // 컨텍스트를 가져오기 위해 사용

    @Autowired
    private AspectCriticMetric aspectCritic;

    @Autowired
    private ContextRecallMetric contextRecall;

    @ParameterizedTest(name = "[{index}] 질문: {1} / 검증타입: {3}")
    @CsvFileSource(resources = "/rag-test-data.csv", numLinesToSkip = 1)
    @DisplayName("CSV 기반 RAG 파이프라인 대량 검증 테스트")
    void evaluateRagPipeline(String sessionId, String userInput, String reference, String ruleType) {
        
        // 1. 실제 서비스에 질의하여 응답 받기
        AiChatRequest request = new AiChatRequest(sessionId, userInput);
        AiChatResponse response = aiChatService.chat(request, 1L);

        // 2. Ragas 평가를 위해 검색되었던 컨텍스트 다시 가져오기 (실제 서비스와 동일 조건)
        List<String> contexts = cardSearchService.searchSimilarCards(userInput, 3)
                .stream()
                .map(Document::getText)
                .collect(Collectors.toList());

        // 3. Ragas 심사용 Sample 객체 조립
        Sample sample = Sample.builder()
                .userInput(userInput)
                .response(response.getResponse())
                .retrievedContexts(contexts)
                .reference(reference)
                .build();

        // 4. ruleType(테스트 시나리오)에 따른 맞춤형 채점 진행
        if ("COMPETITOR_BLOCK".equals(ruleType)) {
            // [타사 카드 차단 검증]
            AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
                    .definition("응답에 신한, 현대 등 타사 브랜드명이 포함되어서는 안 되며, 전속 상담원으로서 타사 카드는 안내할 수 없다는 정중한 거절이 포함되어야 한다.")
                    .build();
            Double score = aspectCritic.singleTurnScore(config, sample);
            assertThat(score).as("타사 카드 방어 실패").isEqualTo(1.0);

        } else if ("HALLUCINATION".equals(ruleType) || "NORMAL".equals(ruleType)) {
            // [환각 방지 및 정상 응답 검증]
            ContextRecallMetric.ContextRecallConfig config = ContextRecallMetric.ContextRecallConfig.builder().build();
            Double score = contextRecall.singleTurnScore(config, sample);
            assertThat(score).as("컨텍스트 외 환각 발생 또는 핵심 정보 누락").isGreaterThanOrEqualTo(0.9);
        }
    }
}
