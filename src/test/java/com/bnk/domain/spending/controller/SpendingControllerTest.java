package com.bnk.domain.spending.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.bnk.domain.spending.dto.response.SpendingChartResponse;
import com.bnk.domain.spending.service.SpendingService;
import com.bnk.global.auth.CustomUserDetails;
import com.bnk.global.exception.GlobalExceptionHandler;
import com.bnk.global.util.audit.AuditLogger;

/**
 * SpendingController 단위 테스트 (SonarQube 커버리지 대상)
 *
 * ── 설계 원칙 ────────────────────────────────────────────────────────
 * · standaloneSetup — Spring Context 없이 경량 실행
 * · @AuthenticationPrincipal → MockArgumentResolver 로 userId=1L 주입
 * · SpendingService 완전 Mocking — Controller 레이어 단독 검증
 *
 * ── 커버리지 대상 엔드포인트 ──────────────────────────────────────────
 * · GET /api/users/me/spending  — 정상/빈목록/단일
 * · PUT /api/users/me/spending  — 정상/Bean Validation 실패/Body 누락
 *
 * ── 수정 이력 ─────────────────────────────────────────────────────────
 * · 정상_빈배열_200 제거
 *   → SpendingPatternRequest.patterns 에 @NotEmpty 선언되어 있어
 *     빈 배열은 Bean Validation 에서 400 을 반환하므로 테스트 목적 변경.
 * · 예외_컨텐츠타입없음_415 → 예외_잘못된JSON_400 으로 교체
 *   → standaloneSetup 은 HttpMediaTypeNotSupportedException 핸들러 미등록으로
 *     415 대신 500 을 반환. 대신 JSON 파싱 실패(400) 케이스로 현실적인 시나리오 커버.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpendingController 단위 테스트")
class SpendingControllerTest {

    /* ── Mocks ─────────────────────────────────────────────────── */
    @Mock
    private SpendingService spendingService;

    @Mock
    private AuditLogger auditLogger;   // GlobalExceptionHandler(@RequiredArgsConstructor) 생성자 주입용

    @InjectMocks
    private SpendingController spendingController;

    private MockMvc mockMvc;

    /** @AuthenticationPrincipal → userId=1L 고정 주입 리졸버 */
    private static final HandlerMethodArgumentResolver USER_DETAILS_RESOLVER =
            new HandlerMethodArgumentResolver() {
                @Override
                public boolean supportsParameter(MethodParameter parameter) {
                    return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                            && parameter.getParameterType().isAssignableFrom(CustomUserDetails.class);
                }
                @Override
                public Object resolveArgument(MethodParameter parameter,
                        ModelAndViewContainer mavContainer,
                        NativeWebRequest webRequest,
                        WebDataBinderFactory binderFactory) {
                    CustomUserDetails mock = Mockito.mock(CustomUserDetails.class);
                    Mockito.when(mock.getUserId()).thenReturn(1L);
                    return mock;
                }
            };

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(spendingController)
                .setControllerAdvice(new GlobalExceptionHandler(auditLogger))
                .setCustomArgumentResolvers(USER_DETAILS_RESOLVER)
                .build();
    }

    // ════════════════════════════════════════════════════════════════
    // GET /api/users/me/spending
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("소비 패턴 조회 [GET /api/users/me/spending]")
    class GetMySpendingPatterns {

        private static final String URL = "/api/users/me/spending";

        @Test
        @DisplayName("[정상] 2개 카테고리 → 200 & 목록 반환")
        void 정상_2개카테고리_반환() throws Exception {
            // given
            SpendingChartResponse food = SpendingChartResponse.builder()
                    .categoryId(1L)
                    .categoryName("식비")
                    .monthlyAmount(600_000L)
                    .ratio(60.0)
                    .build();
            SpendingChartResponse transit = SpendingChartResponse.builder()
                    .categoryId(2L)
                    .categoryName("교통")
                    .monthlyAmount(400_000L)
                    .ratio(40.0)
                    .build();

            given(spendingService.getMySpendingPatterns(1L)).willReturn(List.of(food, transit));

            // when & then
            mockMvc.perform(get(URL))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].categoryId").value(1))
                    .andExpect(jsonPath("$.data[0].categoryName").value("식비"))
                    .andExpect(jsonPath("$.data[0].monthlyAmount").value(600000))
                    .andExpect(jsonPath("$.data[0].ratio").value(60.0));
        }

        @Test
        @DisplayName("[정상] 소비 패턴 없음 → 200 & 빈 배열")
        void 정상_빈목록_200() throws Exception {
            // given
            given(spendingService.getMySpendingPatterns(1L)).willReturn(Collections.emptyList());

            // when & then
            mockMvc.perform(get(URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("[정상] 단일 카테고리 → 200 & ratio=100.0")
        void 정상_단일카테고리() throws Exception {
            // given
            SpendingChartResponse single = SpendingChartResponse.builder()
                    .categoryId(3L)
                    .categoryName("쇼핑")
                    .monthlyAmount(300_000L)
                    .ratio(100.0)
                    .build();
            given(spendingService.getMySpendingPatterns(1L)).willReturn(List.of(single));

            // when & then
            mockMvc.perform(get(URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].ratio").value(100.0));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // PUT /api/users/me/spending
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("소비 패턴 UPSERT [PUT /api/users/me/spending]")
    class UpdateSpendingPatterns {

        private static final String URL = "/api/users/me/spending";

        @Test
        @DisplayName("[정상] 3개 항목 저장 → 200 & updatedCount=3 반환")
        void 정상_3개항목저장_200() throws Exception {
            // given
            given(spendingService.updateSpendingPatterns(anyLong(), any())).willReturn(3);

            String body = """
                    {
                      "patterns": [
                        { "categoryId": 1, "monthlyAmount": 500000 },
                        { "categoryId": 2, "monthlyAmount": 200000 },
                        { "categoryId": 3, "monthlyAmount": 300000 }
                      ]
                    }
                    """;

            // when & then
            mockMvc.perform(put(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(3));
        }

        @Test
        @DisplayName("[정상] 단건 항목 저장 → 200 & updatedCount=1")
        void 정상_단건저장_200() throws Exception {
            // given
            given(spendingService.updateSpendingPatterns(anyLong(), any())).willReturn(1);

            String body = """
                    {
                      "patterns": [
                        { "categoryId": 1, "monthlyAmount": 100000 }
                      ]
                    }
                    """;

            // when & then
            mockMvc.perform(put(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(1));
        }

        @Test
        @DisplayName("[예외] patterns 빈 배열(@NotEmpty 위반) → 400 Bad Request")
        void 예외_빈배열_Validation실패_400() throws Exception {
            // SpendingPatternRequest.patterns 에 @NotEmpty 선언 → 빈 배열은 Validation 실패
            String body = """
                    {
                      "patterns": []
                    }
                    """;

            mockMvc.perform(put(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[예외] 잘못된 JSON 형식(파싱 불가) → 400 Bad Request")
        void 예외_잘못된JSON_400() throws Exception {
            // GlobalExceptionHandler 의 HttpMessageNotReadableException 핸들러 커버
            String malformedBody = "{ patterns: [ invalid json }";

            mockMvc.perform(put(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedBody))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[예외] Body 없음 → 400 Bad Request")
        void 예외_바디없음_400() throws Exception {
            mockMvc.perform(put(URL)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }
}