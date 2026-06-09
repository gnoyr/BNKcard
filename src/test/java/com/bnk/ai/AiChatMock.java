package com.bnk.ai;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.bnk.BnKcardApplication;
import com.bnk.ai.config.RagasEvaluationConfig;
import com.bnk.ai.qa.solutions.execution.ModelResult;
import com.bnk.ai.qa.solutions.execution.MultiModelExecutor;
import com.bnk.ai.qa.solutions.metrics.retrieval.ContextPrecisionMetric;
import com.bnk.ai.qa.solutions.metrics.retrieval.ContextPrecisionMetric.RelevanceResponse;
import com.bnk.ai.qa.solutions.metrics.retrieval.ContextRecallMetric;
import com.bnk.ai.qa.solutions.metrics.retrieval.FaithfulnessMetric;
import com.bnk.ai.qa.solutions.metrics.retrieval.ResponseRelevancyMetric;
import com.bnk.ai.qa.solutions.sample.Sample;

@SpringBootTest(classes = {BnKcardApplication.class, RagasEvaluationConfig.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.ai.google.genai.project-id=gen-lang-client-0998457737",
    "spring.ai.google.genai.api-key=AIzaSyD1qOdIPZBWjVR9r28J5C64ViQEDLU7opw",
    "spring.ai.google.genai.embedding.project-id=gen-lang-client-0998457737",
    "spring.ai.google.genai.embedding.api-key=AIzaSyD1qOdIPZBWjVR9r28J5C64ViQEDLU7opw"
})
public class AiChatMock {
	
    @MockitoBean private com.bnk.domain.card.scheduler.CardScheduler cardScheduler;
    @MockitoBean private Clock clock;
    private Sample sample;
    @Mock private MultiModelExecutor mockExecutor;
    private ResponseRelevancyMetric responseRelevancy;
    private FaithfulnessMetric faithfulness;
    private ContextRecallMetric contextRecall;
    private ContextPrecisionMetric contextPrecision;

    private static final Instant FIXED_INSTANT = Instant.parse("2026-06-09T10:00:00Z");
    private static final Duration FIXED_DURATION = Duration.ZERO;
    
    
    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Clock mock 스텁 — 시스템 Clock 사용 금지
        given(clock.instant()).willReturn(FIXED_INSTANT);
        given(clock.getZone()).willReturn(java.time.ZoneOffset.UTC);

        this.responseRelevancy = ResponseRelevancyMetric.builder().executor(mockExecutor).build();
        this.faithfulness = FaithfulnessMetric.builder().executor(mockExecutor).build();
        this.contextRecall = ContextRecallMetric.builder().executor(mockExecutor).build();
        this.contextPrecision = ContextPrecisionMetric.builder().executor(mockExecutor).build();

        this.sample = Sample.builder()
                .userInput("연회비 알려줘")
                .reference("연회비는 1만원입니다.")
                .retrievedContexts(List.of("연회비 정보: 1만원"))
                .response("연회비는 1만원입니다.")
                .build();

        // ===== 핵심: 모든 메서드 호출을 로깅하는 spy 사용 =====
        Mockito.doAnswer(invocation -> {
            System.out.println(">>> [!!! 캐치되지 않은 메서드 호출] " + invocation.getMethod().getName());
            System.out.println(">>>   인자: " + java.util.Arrays.toString(invocation.getArguments()));
            return null; // 기본 반환값
        }).when(mockExecutor).executeLlmOnModelAsync(any(), any(), any());

        // 1. runAsync
        doAnswer(invocation -> {
            System.out.println(">>> [Mock 호출] runAsync");
            Object arg = invocation.getArgument(0);
            
            if (arg instanceof java.util.concurrent.Callable) {
                System.out.println(">>> [runAsync] Callable 실행 중...");
                try {
                    ((java.util.concurrent.Callable<?>) arg).call();
                } catch (Exception e) {
                    System.err.println(">>> [runAsync] Callable 실행 중 에러!");
                    e.printStackTrace();
                }
            }
            return CompletableFuture.completedFuture(null);
        }).when(mockExecutor).runAsync(any());

        // 2. executeLlm
        doAnswer(invocation -> {
            Class<?> responseClass = invocation.getArgument(2);
            System.out.println(">>> [executeLlm 호출됨!] 응답 클래스: " + responseClass.getSimpleName());
            
            Object dummyResponse;

            if (ResponseRelevancyMetric.GeneratedQuestionsResponse.class.isAssignableFrom(responseClass)) {
                var dummyQuestion = new ResponseRelevancyMetric.GeneratedQuestion("연회비가 얼마인가요?", 0);
                dummyResponse = new ResponseRelevancyMetric.GeneratedQuestionsResponse(List.of(dummyQuestion));

            } else if (RelevanceResponse.class.isAssignableFrom(responseClass)) {
                dummyResponse = new RelevanceResponse(true, "Reasoning is fine");
                
            } else {
                dummyResponse = Mockito.mock(responseClass, invocation2 -> {
                    String methodName = invocation2.getMethod().getName();
                    Class<?> returnType = invocation2.getMethod().getReturnType();
                    
                    System.out.println(">>>   [Mock getter] " + methodName + " -> " + returnType.getSimpleName());
                    
                    if (methodName.startsWith("get") || methodName.startsWith("is")) {
                        if (List.class.isAssignableFrom(returnType)) {
                            Object listItem = Mockito.mock(Object.class, inv -> {
                                String itemMethod = inv.getMethod().getName();
                                Class<?> itemReturnType = inv.getMethod().getReturnType();
                                
                                System.out.println(">>>     [List item getter] " + itemMethod);
                                
                                if (itemMethod.equals("getVerdict") || itemMethod.equals("isVerified") || 
                                    itemMethod.startsWith("is")) {
                                    return true;
                                }
                                if (itemMethod.startsWith("get") && String.class.isAssignableFrom(itemReturnType)) {
                                    return "dummy statement";
                                }
                                if (itemMethod.equals("getAttributable") || itemMethod.equals("getClassification")) {
                                    return "attributable";
                                }
                                return null;
                            });
                            return List.of(listItem);
                        }
                        
                        if (Double.class.isAssignableFrom(returnType) || double.class.equals(returnType)) {
                            return 0.95;
                        }
                        if (Boolean.class.isAssignableFrom(returnType) || boolean.class.equals(returnType)) {
                            return true;
                        }
                        if (String.class.isAssignableFrom(returnType)) {
                            return "dummy_string";
                        }
                    }
                    return null;
                });
            }
            
            return List.of(ModelResult.success("gemini-model", dummyResponse, FIXED_DURATION, "dummy"));
            
        }).when(mockExecutor).executeLlm(any(), anyString(), any());

        // 3. executeLlmOnModelAsync - doAnswer로 변경
        doAnswer(invocation -> {
            String modelId = invocation.getArgument(0, String.class);
            invocation.getArgument(1, String.class);
            Class<?> responseClass = invocation.getArgument(2);
            
            System.out.println(">>> [executeLlmOnModelAsync 호출됨!!!]");
            System.out.println(">>>   모델: " + modelId);
            System.out.println(">>>   응답 클래스: " + responseClass.getSimpleName());
            
            Object dummyResponse = Mockito.mock(responseClass, inv2 -> {
                String methodName = inv2.getMethod().getName();
                Class<?> returnType = inv2.getMethod().getReturnType();
                
                System.out.println(">>>   [Async Mock getter] " + methodName);
                
                if (methodName.startsWith("get") || methodName.startsWith("is")) {
                    if (List.class.isAssignableFrom(returnType)) {
                        Object listItem = Mockito.mock(Object.class, inv3 -> {
                            String itemMethod = inv3.getMethod().getName();
                            
                            if (itemMethod.equals("getVerdict") || itemMethod.startsWith("is")) {
                                return true;
                            }
                            if (itemMethod.startsWith("get")) {
                                return "verdict statement";
                            }
                            return null;
                        });
                        return List.of(listItem);
                    }
                    if (Boolean.class.isAssignableFrom(returnType) || boolean.class.equals(returnType)) {
                        return true;
                    }
                    if (Double.class.isAssignableFrom(returnType) || double.class.equals(returnType)) {
                        return 0.95;
                    }
                }
                return null;
            });
            
            CompletableFuture<ModelResult<Object>> future = CompletableFuture.completedFuture(
                ModelResult.success(modelId, dummyResponse, FIXED_DURATION, "dummy")
            );
            
            System.out.println(">>> [executeLlmOnModelAsync 반환] CompletableFuture 생성");
            return future;
            
        }).when(mockExecutor).executeLlmOnModelAsync(anyString(), anyString(), any());

        // 4. executeEmbeddingsAsync
        float[] dummy = new float[]{0.1f, 0.1f};
        given(mockExecutor.executeEmbeddingsAsync(any())).willReturn(
            CompletableFuture.completedFuture(
                List.of(ModelResult.success("gemini-model", List.of(dummy, dummy), FIXED_DURATION, "dummy"))
            )
        );
        
        System.out.println(">>> [setUp 완료] Mock 설정 완료");
    }
    
    
    @Test
    @DisplayName("1. 답변 관련성(Response Relevancy) 평가")
    void testResponseRelevancy() throws Exception {

        System.out.println(">>> [디버깅] 메서드 호출 시작");

        try {
            ResponseRelevancyMetric.ResponseRelevancyConfig config = 
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder().language("ko").build();

            var future = responseRelevancy.singleTurnScoreAsync(config, sample);
            
            System.out.println(">>> [디버깅] singleTurnScoreAsync 결과: " + future);
            assertThat(future).isNotNull();
            
            Double score = future.join();
            System.out.printf("[답변 관련성] 점수: %.2f%n", score);

        } catch (Exception e) {
            System.out.println(">>> [!!!] 에러를 잡았습니다!");
            e.printStackTrace();
        }
    }
    
    
    
    @Test
    @DisplayName("2. 사실 부합도(Faithfulness) 평가")
    void testFaithfulness() {
        var config = FaithfulnessMetric.FaithfulnessConfig.builder().language("ko").build();
        Double score = faithfulness.singleTurnScore(config, sample);
        System.out.printf("[사실 부합도] 점수: %.2f%n", score);
        assertThat(score).isNotNull();
    }

    @Test
    @DisplayName("3. 문맥 재현율(Context Recall) 평가")
    void testContextRecall() {
        var config = ContextRecallMetric.ContextRecallConfig.builder().language("ko").build();
        Double score = contextRecall.singleTurnScore(config, sample);
        System.out.printf("[문맥 재현율] 점수: %.2f%n", score);
        assertThat(score).isNotNull();
    }

    @Test
    @DisplayName("4. 문맥 정밀도(Context Precision) 평가")
    void testContextPrecision() {
        System.out.println(">>> [디버깅] ContextPrecision 평가 시작");
        try {
            var config = ContextPrecisionMetric.ContextPrecisionConfig.builder().language("ko").build();
            
            System.out.println(">>> [디버깅] singleTurnScore 호출 직전"); 
            
            Double score = contextPrecision.singleTurnScore(config, sample);
            
            System.out.println(">>> [디버깅] singleTurnScore 결과 = " + score); // 실제 값 확인
            System.out.println(">>> [디버깅] score가 null인가? " + (score == null)); // null 체크
            
            if (score != null) {
                System.out.printf("[문맥 정밀도] 점수: %.2f%n", score);
            } else {
                System.out.println("[문맥 정밀도] 점수가 null입니다!");
            }
            assertThat(score).isNotNull();
            
        } catch (Exception e) {
            System.err.println(">>> [!!!] ContextPrecision에서 에러를 잡았습니다!");
            e.printStackTrace();
            throw e; 
        }
    }
}