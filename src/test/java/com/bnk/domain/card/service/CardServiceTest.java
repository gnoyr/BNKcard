package com.bnk.domain.card.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.bnk.domain.card.dto.request.CardCompareRequest;
import com.bnk.domain.card.dto.request.CardSearchRequest;
import com.bnk.domain.card.dto.request.CardSimulationRequest;
import com.bnk.domain.card.dto.response.BannerDto;
import com.bnk.domain.card.dto.response.CardCompareResponse;
import com.bnk.domain.card.dto.response.CardDetailResponse;
import com.bnk.domain.card.dto.response.CardListResponse;
import com.bnk.domain.card.dto.response.SimulationResponse;
import com.bnk.domain.card.mapper.CardBenefitMapper;
import com.bnk.domain.card.mapper.CardContentMapper;
import com.bnk.domain.card.mapper.CardImageMapper;
import com.bnk.domain.card.mapper.CardMapper;
import com.bnk.domain.card.model.Card;
import com.bnk.domain.card.model.CardBenefit;
import com.bnk.domain.card.model.CardImage;
import com.bnk.domain.search.mapper.SearchLogMapper;
import com.bnk.domain.spending.mapper.SpendingPatternMapper;
import com.bnk.domain.terms.mapper.TermsMapper;
import com.bnk.global.response.PageResponse;

/**
 * CardService 단위 테스트 (SonarQube 커버리지 대상)
 *
 * ── 주의 사항 ────────────────────────────────────────────────────────
 * · CardBenefitMapper.findByCardIds(List) — 비교·시뮬레이션 일괄 조회
 * · SimulationResponse 필드: totalBenefitAmount (estimatedBenefit 아님)
 * · CardSimulationRequest: cardIds + categoryAmounts(Map<Long,Long>)
 * · getCardDetail: card==null → IllegalArgumentException (BusinessException 아님)
 * · getTop3Cards: surveyResult 숫자 문자열 → parseLong → categoryId
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CardService 단위 테스트")
class CardServiceTest {

	@Mock
	private CardMapper cardMapper;
	@Mock
	private CardBenefitMapper cardBenefitMapper;
	@Mock
	private CardImageMapper cardImageMapper;
	@Mock
	private CardContentMapper cardContentMapper;
	@Mock
	private SpendingPatternMapper spendingPatternMapper;
	@Mock
	private SearchLogMapper searchLogMapper;
	@Mock
	private TermsMapper termsMapper;

    @InjectMocks
    private CardService cardService;

    // ── Fixture ──────────────────────────────────────────────────────

    private Card card(Long id, String name) {
        Card c = new Card();
        ReflectionTestUtils.setField(c, "cardId",      id);
        ReflectionTestUtils.setField(c, "cardName",    name);
        ReflectionTestUtils.setField(c, "cardType",    "CHECK");
        ReflectionTestUtils.setField(c, "cardStatus",  "PUBLISHED");
        ReflectionTestUtils.setField(c, "companyName", "BNK부산은행");
        return c;
    }

    private CardImage frontImage(Long cardId) {
        CardImage img = new CardImage();
        ReflectionTestUtils.setField(img, "cardId",   cardId);
        ReflectionTestUtils.setField(img, "imageUrl", "https://cdn.bnk.co.kr/" + cardId + ".png");
        ReflectionTestUtils.setField(img, "imageType","FRONT");
        return img;
    }

    /** RATE_DISCOUNT 타입 혜택 — 할인율+한도 적용 */
    private CardBenefit rateDiscountBenefit(Long cardId, Long categoryId,
                                             double rate, long monthlyLimit) {
        CardBenefit b = new CardBenefit();
        ReflectionTestUtils.setField(b, "cardId",             cardId);
        ReflectionTestUtils.setField(b, "categoryId",         categoryId);
        ReflectionTestUtils.setField(b, "benefitType",        "RATE_DISCOUNT");
        ReflectionTestUtils.setField(b, "discountRate",       BigDecimal.valueOf(rate));
        ReflectionTestUtils.setField(b, "monthlyLimitAmount", monthlyLimit);
        ReflectionTestUtils.setField(b, "displayText",        "할인 혜택");
        return b;
    }

    /** display_order=1 대표 혜택 (findTop1ByCardIds 용) */
    private CardBenefit displayBenefit(Long cardId) {
        CardBenefit b = new CardBenefit();
        ReflectionTestUtils.setField(b, "cardId",      cardId);
        ReflectionTestUtils.setField(b, "displayText", "대표 혜택");
        return b;
    }

    private CardSearchRequest searchReq(String q, int page, int size) {
        CardSearchRequest req = new CardSearchRequest();
        ReflectionTestUtils.setField(req, "q",    q);
        ReflectionTestUtils.setField(req, "page", page);
        ReflectionTestUtils.setField(req, "size", size);
        return req;
    }

    private CardCompareRequest compareReq(List<Long> ids) {
        CardCompareRequest req = new CardCompareRequest();
        ReflectionTestUtils.setField(req, "cardIds", ids);
        return req;
    }

    /** CardSimulationRequest: cardIds + categoryAmounts(Map<Long,Long>) */
    private CardSimulationRequest simulateReq(List<Long> ids, Map<Long, Long> categoryAmounts) {
        CardSimulationRequest req = new CardSimulationRequest();
        ReflectionTestUtils.setField(req, "cardIds",         ids);
        ReflectionTestUtils.setField(req, "categoryAmounts", categoryAmounts);
        return req;
    }

    // ════════════════════════════════════════════════════════════════
    // 홈 배너 조회
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("홈 배너 조회 [getHomeBanners]")
    class GetHomeBanners {

        @Test
        @DisplayName("[정상] 비로그인(null) → view_count 상위 3개 배너 반환")
        void 정상_비로그인() {
            given(cardMapper.findTop3ByViewCount()).willReturn(List.of(card(1L, "A카드")));
            given(cardImageMapper.findByCardIdAndType(1L, "THUMBNAIL")).willReturn(null);
            given(cardImageMapper.findByCardIdAndType(1L, "FRONT")).willReturn(frontImage(1L));
            given(cardBenefitMapper.findTop1ByCardIds(List.of(1L))).willReturn(List.of(displayBenefit(1L)));

            List<BannerDto> result = cardService.getHomeBanners(null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCardId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("[정상] 로그인 + 소비패턴 있음 → 카테고리 기반 배너")
        void 정상_로그인_소비패턴있음() {
            given(spendingPatternMapper.findTopCategoryIdByUserId(1L)).willReturn(5L);
            given(cardMapper.findTop3ByCategoryId(5L)).willReturn(List.of(card(2L, "B카드")));
            given(cardImageMapper.findByCardIdAndType(2L, "THUMBNAIL")).willReturn(frontImage(2L));
            given(cardBenefitMapper.findTop1ByCardIds(List.of(2L))).willReturn(Collections.emptyList());

            List<BannerDto> result = cardService.getHomeBanners(1L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("[정상] 로그인 + 소비패턴 없음 → view_count 폴백")
        void 정상_로그인_소비패턴없음() {
            given(spendingPatternMapper.findTopCategoryIdByUserId(1L)).willReturn(null);
            given(cardMapper.findTop3ByViewCount()).willReturn(List.of(card(3L, "C카드")));
            given(cardImageMapper.findByCardIdAndType(3L, "THUMBNAIL")).willReturn(null);
            given(cardImageMapper.findByCardIdAndType(3L, "FRONT")).willReturn(null);
            given(cardBenefitMapper.findTop1ByCardIds(List.of(3L))).willReturn(Collections.emptyList());

            List<BannerDto> result = cardService.getHomeBanners(1L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("[정상] 카드 0건 → 빈 리스트")
        void 정상_카드없음() {
            given(cardMapper.findTop3ByViewCount()).willReturn(Collections.emptyList());

            List<BannerDto> result = cardService.getHomeBanners(null);

            assertThat(result).isEmpty();
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 카드 목록 + 검색
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("카드 목록 조회 [getCardList]")
    class GetCardList {

        @Test
        @DisplayName("[정상] 검색 결과 존재 → PageResponse 반환")
        void 정상_목록조회() {
            CardSearchRequest req = searchReq("", 0, 10);
            given(cardMapper.countAll(req)).willReturn(1L);
            given(cardMapper.findAll(req)).willReturn(List.of(card(1L, "A카드")));
            given(cardImageMapper.findByCardIdAndType(1L, "THUMBNAIL")).willReturn(null);
            given(cardImageMapper.findByCardIdAndType(1L, "FRONT")).willReturn(frontImage(1L));
            given(cardBenefitMapper.findTop1ByCardIds(List.of(1L))).willReturn(List.of(displayBenefit(1L)));

            PageResponse<CardListResponse> result = cardService.getCardList(req, null);

            assertThat(result.getTotalCount()).isEqualTo(1L);
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("[정상] 검색 결과 0건 → 빈 PageResponse")
        void 정상_결과없음() {
            CardSearchRequest req = searchReq("없는카드", 0, 10);
            given(cardMapper.countAll(req)).willReturn(0L);

            PageResponse<CardListResponse> result = cardService.getCardList(req, 1L);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalCount()).isZero();
        }
    }

    // ════════════════════════════════════════════════════════════════
    // TOP3 추천
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("TOP3 추천 [getTop3Cards]")
    class GetTop3Cards {

        @Test
        @DisplayName("[정상] 비회원 + surveyResult 없음 → view_count TOP3")
        void 정상_비회원() {
            given(cardMapper.findTop3ByViewCount()).willReturn(List.of(card(1L, "A"), card(2L, "B")));
            given(cardImageMapper.findByCardIdAndType(anyLong(), anyString())).willReturn(null);
            given(cardBenefitMapper.findTop1ByCardIds(any())).willReturn(Collections.emptyList());

            List<CardListResponse> result = cardService.getTop3Cards(null, "");

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("[정상] 신규회원 + surveyResult='5' → 카테고리 기반 TOP3")
        void 정상_신규회원_설문() {
            given(cardMapper.findTop3ByCategoryId(5L)).willReturn(List.of(card(3L, "C")));
            given(cardImageMapper.findByCardIdAndType(anyLong(), anyString())).willReturn(null);
            given(cardBenefitMapper.findTop1ByCardIds(any())).willReturn(Collections.emptyList());

            List<CardListResponse> result = cardService.getTop3Cards(null, "5");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("[정상] 우수회원(소비패턴 있음) → 소비패턴 기반 TOP3")
        void 정상_우수회원() {
            given(spendingPatternMapper.findTopCategoryIdByUserId(1L)).willReturn(3L);
            given(cardMapper.findTop3ByCategoryId(3L)).willReturn(List.of(card(4L, "D")));
            given(cardImageMapper.findByCardIdAndType(anyLong(), anyString())).willReturn(null);
            given(cardBenefitMapper.findTop1ByCardIds(any())).willReturn(Collections.emptyList());

            List<CardListResponse> result = cardService.getTop3Cards(1L, "");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("[정상] 우수회원(소비패턴 없음) → view_count 폴백 + 빈 리스트")
        void 정상_우수회원_소비패턴없음() {
            given(spendingPatternMapper.findTopCategoryIdByUserId(1L)).willReturn(null);
            given(cardMapper.findTop3ByViewCount()).willReturn(Collections.emptyList());

            List<CardListResponse> result = cardService.getTop3Cards(1L, "");

            assertThat(result).isEmpty();
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 카드 상세 조회
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("카드 상세 조회 [getCardDetail]")
    class GetCardDetail {

        @Test
        @DisplayName("[정상] 유효 cardId → CardDetailResponse 반환 + incrementViewCount 호출")
        void 정상_상세조회() {
            given(cardMapper.findById(1L)).willReturn(card(1L, "A카드"));
            given(cardBenefitMapper.findByCardId(1L)).willReturn(List.of(displayBenefit(1L)));
            given(cardImageMapper.findByCardId(1L)).willReturn(List.of(frontImage(1L)));
            given(cardContentMapper.findByCardId(1L)).willReturn(Collections.emptyList());
            given(termsMapper.findTermsFilesByCardId(1L)).willReturn(Collections.emptyList());
            given(cardMapper.incrementViewCount(1L)).willReturn(1);

            CardDetailResponse result = cardService.getCardDetail(1L);

            assertThat(result).isNotNull();
            assertThat(result.getCardId()).isEqualTo(1L);
            then(cardMapper).should().incrementViewCount(1L);
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 cardId → IllegalArgumentException")
        void 실패_카드없음() {
            given(cardMapper.findById(999L)).willReturn(null);

            // CardService: if (card == null) throw new IllegalArgumentException(...)
            assertThatThrownBy(() -> cardService.getCardDetail(999L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 카드 비교
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("카드 비교 [compareCards]")
    class CompareCards {

        @Test
        @DisplayName("[정상] 2개 카드 비교 → CardCompareResponse 2건 반환")
        void 정상_2개비교() {
            List<Long> ids = List.of(1L, 2L);
            given(cardBenefitMapper.findByCardIds(ids))
                    .willReturn(List.of(displayBenefit(1L), displayBenefit(2L)));
            given(cardImageMapper.findByCardIdAndType(1L, "THUMBNAIL")).willReturn(null);
            given(cardImageMapper.findByCardIdAndType(1L, "FRONT")).willReturn(frontImage(1L));
            given(cardImageMapper.findByCardIdAndType(2L, "THUMBNAIL")).willReturn(null);
            given(cardImageMapper.findByCardIdAndType(2L, "FRONT")).willReturn(frontImage(2L));
            given(cardMapper.findById(1L)).willReturn(card(1L, "A카드"));
            given(cardMapper.findById(2L)).willReturn(card(2L, "B카드"));

            List<CardCompareResponse> result = cardService.compareCards(compareReq(ids));

            assertThat(result).hasSize(2);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 혜택 시뮬레이션
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("혜택 시뮬레이션 [simulateBenefits]")
    class SimulateBenefits {

        @Test
        @DisplayName("[정상] 50000*0.1=5000 < limit 10000 → totalBenefitAmount=5000")
        void 정상_할인율적용() {
            List<Long> ids = List.of(1L);
            CardBenefit b = rateDiscountBenefit(1L, 1L, 0.1, 10000L);

            given(cardBenefitMapper.findByCardIds(ids)).willReturn(List.of(b));
            given(cardMapper.findById(1L)).willReturn(card(1L, "A카드"));

            List<SimulationResponse> result = cardService.simulateBenefits(
                    simulateReq(ids, Map.of(1L, 50000L)));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTotalBenefitAmount()).isEqualTo(5000L);
        }

        @Test
        @DisplayName("[정상] 200000*0.1=20000 > limit 5000 → totalBenefitAmount=5000(캡)")
        void 정상_한도캡적용() {
            List<Long> ids = List.of(1L);
            CardBenefit b = rateDiscountBenefit(1L, 1L, 0.1, 5000L);

            given(cardBenefitMapper.findByCardIds(ids)).willReturn(List.of(b));
            given(cardMapper.findById(1L)).willReturn(card(1L, "A카드"));

            List<SimulationResponse> result = cardService.simulateBenefits(
                    simulateReq(ids, Map.of(1L, 200000L)));

            assertThat(result.get(0).getTotalBenefitAmount()).isEqualTo(5000L);
        }

        @Test
        @DisplayName("[정상] 해당 카테고리 지출 없음(0) → totalBenefitAmount=0")
        void 정상_해당카테고리없음() {
            List<Long> ids = List.of(1L);
            // 카테고리 99L 혜택이지만 categoryAmounts에 99L 없음 → 0원
            CardBenefit b = rateDiscountBenefit(1L, 99L, 0.1, 5000L);

            given(cardBenefitMapper.findByCardIds(ids)).willReturn(List.of(b));
            given(cardMapper.findById(1L)).willReturn(card(1L, "A카드"));

            List<SimulationResponse> result = cardService.simulateBenefits(
                    simulateReq(ids, Map.of(1L, 300000L)));

            assertThat(result.get(0).getTotalBenefitAmount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("[정상] 혜택 없는 카드 → totalBenefitAmount=0")
        void 정상_혜택없음() {
            List<Long> ids = List.of(1L);
            given(cardBenefitMapper.findByCardIds(ids)).willReturn(Collections.emptyList());
            given(cardMapper.findById(1L)).willReturn(card(1L, "A카드"));

            List<SimulationResponse> result = cardService.simulateBenefits(
                    simulateReq(ids, Map.of(1L, 100000L)));

            assertThat(result.get(0).getTotalBenefitAmount()).isEqualTo(0L);
        }
    }
}
