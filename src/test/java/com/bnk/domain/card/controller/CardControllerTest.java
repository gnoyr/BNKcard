package com.bnk.domain.card.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.bnk.domain.card.dto.request.CardCompareRequest;
import com.bnk.domain.card.dto.request.CardSearchRequest;
import com.bnk.domain.card.dto.request.CardSimulationRequest;
import com.bnk.domain.card.dto.response.BannerDto;
import com.bnk.domain.card.dto.response.CardCompareResponse;
import com.bnk.domain.card.dto.response.CardDetailResponse;
import com.bnk.domain.card.dto.response.CardListResponse;
import com.bnk.domain.card.dto.response.SimulationResponse;
import com.bnk.domain.card.mapper.CardCategoryMapper;
import com.bnk.domain.card.model.CardCategory;
import com.bnk.domain.card.service.CardService;
import com.bnk.global.auth.CustomUserDetails;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.exception.GlobalExceptionHandler;
import com.bnk.global.response.PageResponse;
import com.bnk.global.util.audit.AuditLogger;

/**
 * CardController 단위 테스트 (SonarQube 커버리지 대상)
 *
 * ── 설계 원칙 ────────────────────────────────────────────────────────
 * · CardController 의존성: CardService, CardCategoryMapper (AuditLogger 없음)
 * · 비로그인(X-No-Auth) → userId=null 경로 검증
 * · POST /api/cards/simulate: categoryAmounts(Map<Long,Long>) JSON 전달
 * · CARD_NOT_FOUND ErrorCode 코드값: "C002" 확인 필요 (프로젝트 ErrorCode 기준)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CardController 단위 테스트")
class CardControllerTest {

    @Mock private CardService        cardService;
    @Mock private CardCategoryMapper cardCategoryMapper;
    @Mock private AuditLogger        auditLogger;    // GlobalExceptionHandler 생성자 필요

    @InjectMocks private CardController cardController;

    private MockMvc mvc;
    private CustomUserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockUserDetails = Mockito.mock(CustomUserDetails.class);

        HandlerMethodArgumentResolver resolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter p) {
                return p.hasParameterAnnotation(AuthenticationPrincipal.class);
            }
            @Override
            public Object resolveArgument(MethodParameter p, ModelAndViewContainer mvc,
                                          NativeWebRequest req, WebDataBinderFactory binder) {
                return req.getHeader("X-No-Auth") != null ? null : mockUserDetails;
            }
        };

        mvc = MockMvcBuilders.standaloneSetup(cardController)
                .setValidator(validator)
                .setCustomArgumentResolvers(resolver)
                .setControllerAdvice(new GlobalExceptionHandler(auditLogger))
                .build();
    }

    // ════════════════════════════════════════════════════════════════
    // 홈 배너 조회  GET /api/home/banners
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("홈 배너 조회 API [GET /api/home/banners]")
    class GetHomeBanners {

        @Test
        @DisplayName("[성공] 로그인 사용자 → userId 전달 + 200")
        void 정상_로그인_200() throws Exception {
            given(mockUserDetails.getUserId()).willReturn(1L);
            given(cardService.getHomeBanners(1L)).willReturn(
                    List.of(BannerDto.builder().cardId(1L).cardName("BNK 체크카드").build()));

            mvc.perform(get("/api/home/banners"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].cardId").value(1));
        }

        @Test
        @DisplayName("[성공] 비로그인(userId=null) → view_count 기반 200")
        void 정상_비로그인_200() throws Exception {
            given(cardService.getHomeBanners(null)).willReturn(
                    List.of(BannerDto.builder().cardId(2L).cardName("BNK 인기카드").build()));

            mvc.perform(get("/api/home/banners").header("X-No-Auth", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].cardId").value(2));
        }

        @Test
        @DisplayName("[성공] 배너 없음 → 빈 배열 200")
        void 정상_배너없음_200() throws Exception {
            given(cardService.getHomeBanners(any())).willReturn(Collections.emptyList());

            mvc.perform(get("/api/home/banners").header("X-No-Auth", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 카드 목록 + 검색  GET /api/cards
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("카드 목록 조회 API [GET /api/cards]")
    class GetCardList {

        @Test
        @DisplayName("[성공] 전체 조회 → 200 + PageResponse")
        void 정상_전체조회_200() throws Exception {
            given(mockUserDetails.getUserId()).willReturn(1L);
            given(cardService.getCardList(any(CardSearchRequest.class), eq(1L)))
                    .willReturn(PageResponse.of(
                            List.of(CardListResponse.builder().cardId(1L).cardName("BNK카드").build()),
                            1L, 0, 10));

            mvc.perform(get("/api/cards"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].cardId").value(1));
        }

        @Test
        @DisplayName("[성공] 검색어 포함 → 200")
        void 정상_검색어_200() throws Exception {
            given(mockUserDetails.getUserId()).willReturn(1L);
            given(cardService.getCardList(any(), eq(1L)))
                    .willReturn(PageResponse.of(Collections.emptyList(), 0L, 0, 10));

            mvc.perform(get("/api/cards").queryParam("q", "체크"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("[성공] 비로그인 조회 → userId=null 전달 200")
        void 정상_비로그인_200() throws Exception {
            given(cardService.getCardList(any(), isNull()))
                    .willReturn(PageResponse.of(Collections.emptyList(), 0L, 0, 10));

            mvc.perform(get("/api/cards").header("X-No-Auth", "true"))
                    .andExpect(status().isOk());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // TOP3 추천  GET /api/cards/top3
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("TOP3 추천 API [GET /api/cards/top3]")
    class GetTop3Cards {

        @Test
        @DisplayName("[성공] 로그인 사용자(소비패턴 기반) → 200")
        void 정상_로그인_200() throws Exception {
            given(mockUserDetails.getUserId()).willReturn(1L);
            given(cardService.getTop3Cards(eq(1L), anyString()))
                    .willReturn(List.of(
                            CardListResponse.builder().cardId(1L).cardName("A카드").build(),
                            CardListResponse.builder().cardId(2L).cardName("B카드").build()));

            mvc.perform(get("/api/cards/top3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("[성공] 비로그인(surveyResult 없음) → view_count 기반 200")
        void 정상_비로그인_200() throws Exception {
            given(cardService.getTop3Cards(isNull(), eq("")))
                    .willReturn(List.of(CardListResponse.builder().cardId(3L).cardName("C카드").build()));

            mvc.perform(get("/api/cards/top3").header("X-No-Auth", "true"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("[성공] 신규 회원(surveyResult 포함) → 설문 기반 200")
        void 정상_설문결과_200() throws Exception {
            given(cardService.getTop3Cards(isNull(), eq("5")))
                    .willReturn(Collections.emptyList());

            mvc.perform(get("/api/cards/top3")
                    .header("X-No-Auth", "true")
                    .queryParam("surveyResult", "5"))
                    .andExpect(status().isOk());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 카드 카테고리 목록  GET /api/cards/categories
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("카드 카테고리 목록 API [GET /api/cards/categories]")
    class GetCardCategories {

        @Test
        @DisplayName("[성공] 카테고리 전체 → 200")
        void 정상_200() throws Exception {
            given(cardCategoryMapper.getAllCategories())
                    .willReturn(List.of(new CardCategory(), new CardCategory()));

            mvc.perform(get("/api/cards/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 카드 상세 조회  GET /api/cards/{cardId}
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("카드 상세 조회 API [GET /api/cards/{cardId}]")
    class GetCardDetail {

        @Test
        @DisplayName("[성공] 유효한 cardId → 200 + 상세 정보")
        void 정상_200() throws Exception {
            given(cardService.getCardDetail(1L))
                    .willReturn(CardDetailResponse.builder()
                            .cardId(1L)
                            .cardName("BNK 체크카드")
                            .build());

            mvc.perform(get("/api/cards/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.cardId").value(1))
                    .andExpect(jsonPath("$.data.cardName").value("BNK 체크카드"));
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 cardId → 404 + code=C002")
        void 실패_카드없음_404() throws Exception {
            given(cardService.getCardDetail(999L))
                    .willThrow(new BusinessException(ErrorCode.CARD_NOT_FOUND));

            mvc.perform(get("/api/cards/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("CD001"));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 카드 비교  POST /api/cards/compare
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("카드 비교 API [POST /api/cards/compare]")
    class CompareCards {

        @Test
        @DisplayName("[성공] 2개 카드 비교 → 200")
        void 정상_200() throws Exception {
            given(cardService.compareCards(any(CardCompareRequest.class)))
                    .willReturn(List.of(
                            CardCompareResponse.builder().cardId(1L).build(),
                            CardCompareResponse.builder().cardId(2L).build()));

            mvc.perform(post("/api/cards/compare")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"cardIds\":[1,2]}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("[실패] cardIds 비어 있음 → 400 (Bean Validation)")
        void 실패_cardIds비어있음_400() throws Exception {
            mvc.perform(post("/api/cards/compare")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"cardIds\":[]}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 혜택 시뮬레이션  POST /api/cards/simulate
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("혜택 시뮬레이션 API [POST /api/cards/simulate]")
    class SimulateBenefits {

        @Test
        @DisplayName("[성공] cardIds + categoryAmounts → 200 + totalBenefitAmount 포함 응답")
        void 정상_200() throws Exception {
            given(cardService.simulateBenefits(any(CardSimulationRequest.class)))
                    .willReturn(List.of(
                            SimulationResponse.builder()
                                    .cardId(1L)
                                    .totalBenefitAmount(5000L)
                                    .build()));

            mvc.perform(post("/api/cards/simulate")
                    .contentType(MediaType.APPLICATION_JSON)
                    // categoryAmounts: { "1": 300000 }
                    .content("{\"cardIds\":[1],\"categoryAmounts\":{\"1\":300000}}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].cardId").value(1))
                    .andExpect(jsonPath("$.data[0].totalBenefitAmount").value(5000));
        }
    }
}
