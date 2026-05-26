package com.bnk.ai;

import com.bnk.BnKcardApplication;
import com.bnk.ai.config.RagasEvaluationConfig;
import com.bnk.ai.qa.solutions.execution.MultiModelExecutor;
import com.bnk.ai.qa.solutions.metrics.retrieval.ContextPrecisionMetric;
import com.bnk.ai.qa.solutions.metrics.retrieval.ContextRecallMetric;
import com.bnk.ai.qa.solutions.metrics.retrieval.FaithfulnessMetric;
import com.bnk.ai.qa.solutions.metrics.retrieval.ResponseRelevancyMetric;
import com.bnk.ai.qa.solutions.sample.Sample;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = {BnKcardApplication.class, RagasEvaluationConfig.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.ai.google.genai.project-id=gen-lang-client-0998457737",
    "spring.ai.google.genai.api-key=AIzaSyD1qOdIPZBWjVR9r28J5C64ViQEDLU7opw",
    "spring.ai.google.genai.embedding.project-id=gen-lang-client-0998457737",
    "spring.ai.google.genai.embedding.api-key=AIzaSyD1qOdIPZBWjVR9r28J5C64ViQEDLU7opw",
    "spring.ai.ollama.chat.enabled=false"
})
class AiChatEvaluationTest {

    @MockitoBean private com.bnk.domain.card.scheduler.CardScheduler cardScheduler;
    @MockitoBean private com.bnk.global.auth.JwtTokenProvider jwtTokenProvider;
    @MockitoBean private com.bnk.domain.spending.service.CardSearchService cardSearchService;

    @Autowired private ResponseRelevancyMetric responseRelevancy;
    @Autowired private FaithfulnessMetric faithfulness;
    @Autowired private ContextRecallMetric contextRecall;
    @Autowired private ContextPrecisionMetric contextPrecision;
    @Autowired private MultiModelExecutor executor;

    private Sample sample;

    @BeforeEach
    void setUp() {
        String question = "BNK V카드의 연회비와 전월 실적 조건은 어떻게 되나요?";
        String reference = "BNK V카드의 연회비는 국내전용 10,000원, 마스터카드 12,000원입니다. 전월 실적 조건은 30만원 이상입니다.";
        
        List<String> retrievedContexts = List.of(
            "문서1: BNK V카드 기본 안내. 연회비: 국내(BC) 10,000원 / 해외겸용(Master) 12,000원.",
            "문서2: BNK V카드 혜택 조건. 통합할인한도는 전월 이용실적 30만원 이상 시 제공됩니다.",
            "문서3: 모바일 300 카드 안내 (관련 없는 노이즈 문서)"
        );
        
        String aiResponse = "BNK V카드의 연회비는 국내 1만원, 해외 1만 2천원이며, 혜택을 받기 위해서는 전월 실적 30만원이 필요합니다.";

        this.sample = Sample.builder()
                .userInput(question)
                .reference(reference)
                .retrievedContexts(retrievedContexts)
                .response(aiResponse)
                .build();
    }
    @AfterEach
    void tearDown() throws InterruptedException {
        System.out.println("⏳ Rate Limit(15 RPM) 방어 중... 다음 테스트를 위해 5초 대기합니다.");
        Thread.sleep(5000); 
    }

    @Test
    @DisplayName("1. 답변 관련성(Response Relevancy) 평가")
    void testResponseRelevancy() throws InterruptedException {
        ResponseRelevancyMetric.ResponseRelevancyConfig config = 
            ResponseRelevancyMetric.ResponseRelevancyConfig.builder().language("ko").build();
        
        try {
            Double score = responseRelevancy.singleTurnScore(config, sample);
            System.out.printf("[답변 관련성] 점수: %.2f%n", score);
            assertThat(score).isNotNull();
        } catch (Exception e) {
            System.err.println("!!! 평가 중 에러 발생 !!!");
            e.printStackTrace(); 
            throw e; 
        }
    }

    @Test
    @DisplayName("2. 사실 부합도(Faithfulness) 평가 - 환각(Hallucination) 여부")
    void testFaithfulness() {
        FaithfulnessMetric.FaithfulnessConfig config = FaithfulnessMetric.FaithfulnessConfig.builder().language("ko").build();
        Double score = faithfulness.singleTurnScore(config, sample);
        System.out.printf("[사실 부합도] 점수: %.2f%n", score);
        assertThat(score).isGreaterThan(0.8);
    }

    @Test
    @DisplayName("3. 문맥 재현율(Context Recall) 평가 - 필수 정보 검색 여부")
    void testContextRecall() {
        ContextRecallMetric.ContextRecallConfig config = ContextRecallMetric.ContextRecallConfig.builder().language("ko").build();
        Double score = contextRecall.singleTurnScore(config, sample);
        System.out.printf("[문맥 재현율] 점수: %.2f%n", score);
        assertThat(score).isGreaterThan(0.8);
    }

    @Test
    @DisplayName("4. 문맥 정밀도(Context Precision) 평가 - 노이즈 비율 및 랭킹")
    void testContextPrecision() {
        ContextPrecisionMetric.ContextPrecisionConfig config = ContextPrecisionMetric.ContextPrecisionConfig.builder().language("ko").build();
        Double score = contextPrecision.singleTurnScore(config, sample);
        System.out.printf("[문맥 정밀도] 점수: %.2f%n", score);
        assertThat(score).isGreaterThan(0.7);
    }
}