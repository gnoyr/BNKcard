package com.bnk.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
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
import com.oracle.bmc.objectstorage.ObjectStorageClient;

/**
 * AI 평가 지표 Mock 테스트
 *
 * [수정 이력] - @MockitoBean ObjectStorageClient 추가
 * (ObjectStorageConfig @Profile("!test") 비활성) - TestDataSourceConfig 추가:
 * MyBatisConfig DataSource 파라미터 요구 → H2 수동 등록 - 메트릭 인스턴스 생성: builder() 패턴 +
 * clock 주입 (각 메트릭이 Clock 필드 보유) - Clock.fixed()로 고정 시간 사용 (시스템 Clock 의존 제거)
 */
@SpringBootTest(classes = { BnKcardApplication.class, RagasEvaluationConfig.class })
@ActiveProfiles("test")
@TestPropertySource(properties = { "spring.ai.google.genai.project-id=gen-lang-client-0998457737",
		"spring.ai.google.genai.api-key=AIzaSyD1qOdIPZBWjVR9r28J5C64ViQEDLU7opw",
		"spring.ai.google.genai.embedding.project-id=gen-lang-client-0998457737",
		"spring.ai.google.genai.embedding.api-key=AIzaSyD1qOdIPZBWjVR9r28J5C64ViQEDLU7opw" })
public class AiChatMock {

	@MockitoBean
	private ObjectStorageClient objectStorageClient;
	@MockitoBean
	private com.bnk.domain.card.scheduler.CardScheduler cardScheduler;

	@TestConfiguration
	static class TestDataSourceConfig {
		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();
		}
	}

	private Sample sample;
	@Mock
	private MultiModelExecutor mockExecutor;

	private ResponseRelevancyMetric responseRelevancy;
	private FaithfulnessMetric faithfulness;
	private ContextRecallMetric contextRecall;
	private ContextPrecisionMetric contextPrecision;

	private static final Instant FIXED_INSTANT = Instant.parse("2026-06-09T10:00:00Z");
	private static final Duration FIXED_DURATION = Duration.ZERO;

	@BeforeEach
	void setUp() throws Exception {
		MockitoAnnotations.openMocks(this);

		// Clock.fixed()으로 고정 시간 생성 — Mock 불필요
		Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

		// 메트릭 인스턴스 생성 시 clock 주입
		this.responseRelevancy = ResponseRelevancyMetric.builder().executor(mockExecutor).clock(fixedClock).build();
		this.faithfulness = FaithfulnessMetric.builder().executor(mockExecutor).clock(fixedClock).build();
		this.contextRecall = ContextRecallMetric.builder().executor(mockExecutor).clock(fixedClock).build();
		this.contextPrecision = ContextPrecisionMetric.builder().executor(mockExecutor).clock(fixedClock).build();

		this.sample = Sample.builder().userInput("연회비 알려줘").reference("연회비는 1만원입니다.")
				.retrievedContexts(List.of("연회비 정보: 1만원")).response("연회비는 1만원입니다.").build();

		// 1. executeLlm (동기)
		doAnswer(invocation -> {
			@SuppressWarnings("unchecked")
			Class<Object> responseClass = (Class<Object>) invocation.getArgument(2);
			String modelId = invocation.getArgument(0);

			Object dummyResponse;
			if (ResponseRelevancyMetric.GeneratedQuestionsResponse.class.isAssignableFrom(responseClass)) {
				var dummyQuestion = new ResponseRelevancyMetric.GeneratedQuestion("연회비가 얼마인가요?", 0);
				dummyResponse = new ResponseRelevancyMetric.GeneratedQuestionsResponse(List.of(dummyQuestion));
			} else if (RelevanceResponse.class.isAssignableFrom(responseClass)) {
				dummyResponse = new RelevanceResponse(true, "Reasoning is fine");
			} else {
				dummyResponse = Mockito.mock(responseClass, inv2 -> {
					String methodName = inv2.getMethod().getName();
					Class<?> returnType = inv2.getMethod().getReturnType();
					if (methodName.startsWith("get") || methodName.startsWith("is")) {
						if (List.class.isAssignableFrom(returnType)) {
							Object listItem = Mockito.mock(Object.class, inv3 -> {
								String itemMethod = inv3.getMethod().getName();
								Class<?> itemReturnType = inv3.getMethod().getReturnType();
								if (itemMethod.equals("getVerdict") || itemMethod.equals("isVerified")
										|| itemMethod.startsWith("is"))
									return true;
								if (itemMethod.startsWith("get") && String.class.isAssignableFrom(itemReturnType))
									return "dummy statement";
								if (itemMethod.equals("getAttributable") || itemMethod.equals("getClassification"))
									return "attributable";
								return null;
							});
							return List.of(listItem);
						}
						if (Double.class.isAssignableFrom(returnType) || double.class.equals(returnType))
							return 0.95;
						if (Boolean.class.isAssignableFrom(returnType) || boolean.class.equals(returnType))
							return true;
						if (String.class.isAssignableFrom(returnType))
							return "dummy_string";
					}
					return null;
				});
			}
			return List.of(ModelResult.success(modelId, dummyResponse, FIXED_DURATION, "dummy"));
		}).when(mockExecutor).executeLlm(any(), anyString(), any());

		// 2. executeLlmOnModelAsync (비동기)
		doAnswer(invocation -> {
			@SuppressWarnings("unchecked")
			Class<Object> responseClass = (Class<Object>) invocation.getArgument(2);
			String modelId = invocation.getArgument(0);

			Object dummyResponse = Mockito.mock(responseClass, inv2 -> {
				String methodName = inv2.getMethod().getName();
				Class<?> returnType = inv2.getMethod().getReturnType();
				if (methodName.startsWith("get") || methodName.startsWith("is")) {
					if (List.class.isAssignableFrom(returnType)) {
						Object listItem = Mockito.mock(Object.class, inv3 -> {
							String itemMethod = inv3.getMethod().getName();
							if (itemMethod.equals("getVerdict") || itemMethod.startsWith("is"))
								return true;
							if (itemMethod.startsWith("get"))
								return "verdict statement";
							return null;
						});
						return List.of(listItem);
					}
					if (Boolean.class.isAssignableFrom(returnType) || boolean.class.equals(returnType))
						return true;
					if (Double.class.isAssignableFrom(returnType) || double.class.equals(returnType))
						return 0.95;
				}
				return null;
			});

			return CompletableFuture
					.completedFuture(ModelResult.success(modelId, dummyResponse, FIXED_DURATION, "dummy"));
		}).when(mockExecutor).executeLlmOnModelAsync(anyString(), anyString(), any());

		// 3. executeEmbeddingsAsync
		float[] dummy = new float[] { 0.1f, 0.1f };
		given(mockExecutor.executeEmbeddingsAsync(any())).willReturn(CompletableFuture.completedFuture(
				List.of(ModelResult.success("gemini-model", List.of(dummy, dummy), FIXED_DURATION, "dummy"))));
	}

	@Test
	@DisplayName("1. 답변 관련성(Response Relevancy) 평가")
	void testResponseRelevancy() throws Exception {
		try {
			ResponseRelevancyMetric.ResponseRelevancyConfig config = ResponseRelevancyMetric.ResponseRelevancyConfig
					.builder().language("ko").build();
			var future = responseRelevancy.singleTurnScoreAsync(config, sample);
			assertThat(future).isNotNull();
			Double score = future.join();
			System.out.printf("[답변 관련성] 점수: %.2f%n", score);
		} catch (Exception e) {
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
		try {
			var config = ContextPrecisionMetric.ContextPrecisionConfig.builder().language("ko").build();
			Double score = contextPrecision.singleTurnScore(config, sample);
			System.out.printf("[문맥 정밀도] 점수: %.2f%n", score);
			assertThat(score).isNotNull();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
}