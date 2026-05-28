package com.bnk.domain.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bnk.domain.admin.mapper.ApprovalMapper;
import com.bnk.domain.admin.model.ApprovalRequest;
import com.bnk.domain.card.dto.request.AdminCardSearchRequest;
import com.bnk.domain.card.dto.request.BenefitCreateRequest;
import com.bnk.domain.card.dto.request.BenefitUpdateRequest;
import com.bnk.domain.card.dto.request.CardCreateRequest;
import com.bnk.domain.card.dto.request.CardStatusRequest;
import com.bnk.domain.card.dto.request.CardUpdateRequest;
import com.bnk.domain.card.dto.request.ContentCreateRequest;
import com.bnk.domain.card.dto.request.ContentUpdateRequest;
import com.bnk.domain.card.dto.request.ImageCreateRequest;
import com.bnk.domain.card.dto.request.ImageUpdateRequest;
import com.bnk.domain.card.dto.response.CardDetailResponse;
import com.bnk.domain.card.mapper.CardBenefitMapper;
import com.bnk.domain.card.mapper.CardContentMapper;
import com.bnk.domain.card.mapper.CardImageMapper;
import com.bnk.domain.card.mapper.CardMapper;
import com.bnk.domain.card.mapper.CardStatusHistoryMapper;
import com.bnk.domain.card.mapper.CardVersionMapper;
import com.bnk.domain.card.model.Card;
import com.bnk.domain.card.model.CardVersion;
import com.bnk.domain.card.service.AdminCardService;
import com.bnk.domain.terms.mapper.TermsMapper;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.response.PageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminCardService 단위 테스트")
class AdminCardServiceTest {

    // ── Mocks ────────────────────────────────────────────────────────
    @Mock private CardMapper              cardMapper;
    @Mock private CardBenefitMapper       cardBenefitMapper;
    @Mock private CardImageMapper         cardImageMapper;
    @Mock private CardContentMapper       cardContentMapper;
    @Mock private TermsMapper             termsMapper;
    @Mock private ApprovalMapper          approvalMapper;
    @Mock private CardStatusHistoryMapper cardStatusHistoryMapper;
    @Mock private CardVersionMapper cardVersionMapper;

    // ObjectMapper는 실제 인스턴스 사용 (JSON 직렬화 실제 동작 필요)
    @Spy
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @InjectMocks
    private AdminCardService adminCardService;

    // ── 공통 상수 ────────────────────────────────────────────────────
    private static final Long   CARD_ID  = 1L;
    private static final Long   ADMIN_ID = 10L;

    // ────────────────────────────────────────────────────────────────
    // 픽스처 헬퍼
    // ────────────────────────────────────────────────────────────────

    /** 정상 Card 픽스처 (model — CardMapper 반환용) */
    private Card activeCard() {
        Card card = Card.builder()
                .cardCode("TEST_CARD")
                .cardType("CREDIT")
                .cardName("테스트카드")
                .companyName("테스트카드사")
                .companyCode("01")
                .brandName("VISA")
                .annualFeeDomestic(15000L)
                .annualFeeOverseas(15000L)
                .previousMonthSpend(300000L)
                .minimumAge(19)
                .maximumAge(65)
                .creditLimitMin(1000000L)
                .creditLimitMax(50000000L)
                .searchableYn("Y")
                .visibleYn("Y")
                .cardStatus("DRAFT")
                .createdBy(ADMIN_ID)
                .build();
        ReflectionTestUtils.setField(card, "cardId", CARD_ID);
        return card;
    }

    /** CardCreateRequest 픽스처 */
    private CardCreateRequest createReq() {
        CardCreateRequest req = new CardCreateRequest();
        ReflectionTestUtils.setField(req, "cardCode",          "TEST_CARD");
        ReflectionTestUtils.setField(req, "cardType",          "CREDIT");
        ReflectionTestUtils.setField(req, "cardName",          "테스트카드");
        ReflectionTestUtils.setField(req, "companyName",       "테스트카드사");
        ReflectionTestUtils.setField(req, "companyCode",       "01");
        ReflectionTestUtils.setField(req, "annualFeeDomestic", 15000L);
        ReflectionTestUtils.setField(req, "annualFeeOverseas", 15000L);
        ReflectionTestUtils.setField(req, "previousMonthSpend",300000L);
        ReflectionTestUtils.setField(req, "changeSummary",     "최초 등록");
        ReflectionTestUtils.setField(req, "benefits",          Collections.emptyList());
        ReflectionTestUtils.setField(req, "images",            Collections.emptyList());
        return req;
    }

    /** CardUpdateRequest 픽스처 */
    private CardUpdateRequest updateReq() {
        CardUpdateRequest req = new CardUpdateRequest();
        ReflectionTestUtils.setField(req, "changeSummary",    "수정 테스트");
        ReflectionTestUtils.setField(req, "annualFeeDomestic", 20000L);
        return req;
    }

    /** CardStatusRequest 픽스처 */
    private CardStatusRequest statusReq(String status) {
        CardStatusRequest req = new CardStatusRequest();
        ReflectionTestUtils.setField(req, "cardStatus",    status);
        ReflectionTestUtils.setField(req, "changedReason", "테스트 상태 변경");
        return req;
    }

    // 공통 스텁 — createCard / updateCard 에서 approval/version 자동 ID 주입
    private void stubApprovalAndVersion() {
        // approvalMapper.insertApprovalRequest → approvalId = 100L 주입
        org.mockito.Mockito.doAnswer(inv -> {
            ApprovalRequest ar = inv.getArgument(0);
            ReflectionTestUtils.setField(ar, "approvalId", 100L);
            return null;
        }).when(approvalMapper).insertApprovalRequest(any());

        // cardVersionMapper2.insertCardVersion → versionId = 50L 주입
        org.mockito.Mockito.doAnswer(inv -> {
            CardVersion cv = inv.getArgument(0);
            ReflectionTestUtils.setField(cv, "versionId", 50L);
            return null;
        }).when(cardVersionMapper).insertCardVersion(any());
    }

    // cardMapper.insertCard → cardId = CARD_ID 주입
    private void stubInsertCard() {
        org.mockito.Mockito.doAnswer(inv -> {
            Card c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "cardId", CARD_ID);
            return 1;
        }).when(cardMapper).insertCard(any());
    }


    // ════════════════════════════════════════════════════════════════
    // createCard — B-03 카드 신규 등록
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("카드 신규 등록 (createCard)")
    class CreateCard {

        @Test
        @DisplayName("[정상] 혜택/이미지 없이 최소 정보 → cardId·versionId·approvalId 반환")
        void 정상_최소정보_등록() {
            stubInsertCard();
            stubApprovalAndVersion();

            Map<String, Long> result = adminCardService.createCard(createReq(), ADMIN_ID);

            assertThat(result).containsKeys("cardId", "versionId", "approvalId");
            assertThat(result.get("cardId")).isEqualTo(CARD_ID);

            // insertCard, insertCardVersion, insertApprovalRequest, insertApprovalLine 호출 검증
            then(cardMapper).should().insertCard(any());
            then(cardVersionMapper).should().insertCardVersion(any());
            then(approvalMapper).should().insertApprovalRequest(any());
            then(approvalMapper).should().insertApprovalLine(any());
            then(cardStatusHistoryMapper).should().insertCardStatusHistory(any());
        }

        @Test
        @DisplayName("[정상] 혜택 포함 → cardBenefitMapper.insertBenefits 호출")
        void 정상_혜택포함_등록() {
            stubInsertCard();
            stubApprovalAndVersion();

            BenefitCreateRequest benefit = new BenefitCreateRequest();
            ReflectionTestUtils.setField(benefit, "benefitTitle", "주유 할인");
            ReflectionTestUtils.setField(benefit, "benefitType",  "DISCOUNT");
            ReflectionTestUtils.setField(benefit, "discountRate",  BigDecimalOrDouble(5.0));

            CardCreateRequest req = createReq();
            ReflectionTestUtils.setField(req, "benefits", List.of(benefit));

            adminCardService.createCard(req, ADMIN_ID);

            then(cardBenefitMapper).should().insertBenefits(any());
        }

        @Test
        @DisplayName("[정상] companyCode null → 기본값 '01' 적용")
        void 정상_companyCode_기본값() {
            stubInsertCard();
            stubApprovalAndVersion();

            CardCreateRequest req = createReq();
            ReflectionTestUtils.setField(req, "companyCode", null);

            adminCardService.createCard(req, ADMIN_ID);

            // insertCard 호출 시 companyCode = "01" 인지 ArgumentCaptor로 검증
            org.mockito.ArgumentCaptor<Card> captor =
                    org.mockito.ArgumentCaptor.forClass(Card.class);
            then(cardMapper).should().insertCard(captor.capture());
            assertThat(captor.getValue().getCompanyCode()).isEqualTo("01");
        }

        @Test
        @DisplayName("[정상] previousMonthSpend null → 기본값 0 적용")
        void 정상_previousMonthSpend_기본값() {
            stubInsertCard();
            stubApprovalAndVersion();

            CardCreateRequest req = createReq();
            ReflectionTestUtils.setField(req, "previousMonthSpend", null);

            adminCardService.createCard(req, ADMIN_ID);

            org.mockito.ArgumentCaptor<Card> captor =
                    org.mockito.ArgumentCaptor.forClass(Card.class);
            then(cardMapper).should().insertCard(captor.capture());
            assertThat(captor.getValue().getPreviousMonthSpend()).isEqualTo(0L);
        }

        @Test
        @DisplayName("[정상] searchableYn null → 기본값 'Y' 적용")
        void 정상_searchableYn_기본값() {
            stubInsertCard();
            stubApprovalAndVersion();

            CardCreateRequest req = createReq();
            ReflectionTestUtils.setField(req, "searchableYn", null);

            adminCardService.createCard(req, ADMIN_ID);

            org.mockito.ArgumentCaptor<Card> captor =
                    org.mockito.ArgumentCaptor.forClass(Card.class);
            then(cardMapper).should().insertCard(captor.capture());
            assertThat(captor.getValue().getSearchableYn()).isEqualTo("Y");
        }
    }


    // ════════════════════════════════════════════════════════════════
    // updateCard — B-04 카드 수정
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("카드 수정 (updateCard)")
    class UpdateCard {

        @Test
        @DisplayName("[정상] 존재하는 카드 → 버전·결재 생성 후 IDs 반환")
        void 정상_카드수정() {
            given(cardMapper.findById(CARD_ID)).willReturn(activeCard());
            given(cardBenefitMapper.findByCardId(CARD_ID)).willReturn(Collections.emptyList());
            given(cardImageMapper.findByCardId(CARD_ID)).willReturn(Collections.emptyList());
            given(cardVersionMapper.getLatestVersionSeq(CARD_ID)).willReturn(1);
            stubApprovalAndVersion();

            Map<String, Long> result = adminCardService.updateCard(CARD_ID, updateReq(), ADMIN_ID);

            assertThat(result).containsKeys("cardId", "versionId", "approvalId");
            then(cardVersionMapper).should().insertCardVersion(any());
            then(approvalMapper).should().insertApprovalRequest(any());
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 cardId → CARD_NOT_FOUND")
        void 실패_카드없음() {
            given(cardMapper.findById(CARD_ID)).willReturn(null);

            assertThatThrownBy(() -> adminCardService.updateCard(CARD_ID, updateReq(), ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.CARD_NOT_FOUND));

            then(cardVersionMapper).should(never()).insertCardVersion(any());
        }

        @Test
        @DisplayName("[정상] 상태 변경 시 → cardStatusHistory INSERT 호출")
        void 정상_상태변경_이력생성() {
            given(cardMapper.findById(CARD_ID)).willReturn(activeCard()); // DRAFT
            given(cardBenefitMapper.findByCardId(CARD_ID)).willReturn(Collections.emptyList());
            given(cardImageMapper.findByCardId(CARD_ID)).willReturn(Collections.emptyList());
            given(cardVersionMapper.getLatestVersionSeq(CARD_ID)).willReturn(1);
            stubApprovalAndVersion();

            CardUpdateRequest req = updateReq();
            ReflectionTestUtils.setField(req, "cardStatus", "PUBLISHED"); // DRAFT → PUBLISHED

            adminCardService.updateCard(CARD_ID, req, ADMIN_ID);

            then(cardStatusHistoryMapper).should().insertCardStatusHistory(any());
        }

        @Test
        @DisplayName("[정상] 상태 변경 없으면 → cardStatusHistory INSERT 미호출")
        void 정상_상태변경없음_이력미생성() {
            given(cardMapper.findById(CARD_ID)).willReturn(activeCard()); // DRAFT
            given(cardBenefitMapper.findByCardId(CARD_ID)).willReturn(Collections.emptyList());
            given(cardImageMapper.findByCardId(CARD_ID)).willReturn(Collections.emptyList());
            given(cardVersionMapper.getLatestVersionSeq(CARD_ID)).willReturn(1);
            stubApprovalAndVersion();

            // cardStatus 미설정 (null) → 기존 상태 유지
            adminCardService.updateCard(CARD_ID, updateReq(), ADMIN_ID);

            then(cardStatusHistoryMapper).should(never()).insertCardStatusHistory(any());
        }

        @Test
        @DisplayName("[정상] 버전 번호 — 기존 seq=2 이면 v3.0 으로 생성")
        void 정상_버전번호_순차증가() {
            given(cardMapper.findById(CARD_ID)).willReturn(activeCard());
            given(cardBenefitMapper.findByCardId(CARD_ID)).willReturn(Collections.emptyList());
            given(cardImageMapper.findByCardId(CARD_ID)).willReturn(Collections.emptyList());
            given(cardVersionMapper.getLatestVersionSeq(CARD_ID)).willReturn(2); // 기존 2개
            stubApprovalAndVersion();

            adminCardService.updateCard(CARD_ID, updateReq(), ADMIN_ID);

            org.mockito.ArgumentCaptor<CardVersion> captor =
                    org.mockito.ArgumentCaptor.forClass(CardVersion.class);
            then(cardVersionMapper).should().insertCardVersion(captor.capture());
            assertThat(captor.getValue().getVersionNo()).isEqualTo("v3.0");
        }
    }


    // ════════════════════════════════════════════════════════════════
    // saveCardBenefits — 혜택 등록/수정
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("혜택 등록/수정 (saveCardBenefits)")
    class SaveCardBenefits {

        @Test
        @DisplayName("[정상] 혜택 교체 → deleteByCardId 후 insertBenefits 호출")
        void 정상_혜택교체() {
            given(cardMapper.findById(CARD_ID)).willReturn(activeCard());
            given(cardBenefitMapper.findByCardId(CARD_ID)).willReturn(Collections.emptyList());
            given(cardImageMapper.findByCardId(CARD_ID)).willReturn(Collections.emptyList());
            given(cardVersionMapper.getLatestVersionSeq(CARD_ID)).willReturn(1);
            stubApprovalAndVersion();

            BenefitCreateRequest b = new BenefitCreateRequest();
            ReflectionTestUtils.setField(b, "benefitTitle", "식당 할인");
            ReflectionTestUtils.setField(b, "benefitType",  "DISCOUNT");

            BenefitUpdateRequest req = new BenefitUpdateRequest();
            ReflectionTestUtils.setField(req, "benefits",      List.of(b));
            ReflectionTestUtils.setField(req, "changeSummary", "혜택 수정");

            Map<String, Long> result = adminCardService.saveCardBenefits(CARD_ID, req, ADMIN_ID);

            assertThat(result.get("cardId")).isEqualTo(CARD_ID);
            then(cardBenefitMapper).should().deleteByCardId(CARD_ID);
            then(cardBenefitMapper).should().insertBenefits(any());
        }

        @Test
        @DisplayName("[정상] 혜택 목록 비어있으면 → deleteByCardId만 호출, insertBenefits 미호출")
        void 정상_혜택빈목록() {
            given(cardMapper.findById(CARD_ID)).willReturn(activeCard());
            given(cardBenefitMapper.findByCardId(CARD_ID)).willReturn(Collections.emptyList());
            given(cardImageMapper.findByCardId(CARD_ID)).willReturn(Collections.emptyList());
            given(cardVersionMapper.getLatestVersionSeq(CARD_ID)).willReturn(1);
            stubApprovalAndVersion();

            BenefitUpdateRequest req = new BenefitUpdateRequest();
            ReflectionTestUtils.setField(req, "benefits",      Collections.emptyList());
            ReflectionTestUtils.setField(req, "changeSummary", "혜택 전체 삭제");

            adminCardService.saveCardBenefits(CARD_ID, req, ADMIN_ID);

            then(cardBenefitMapper).should().deleteByCardId(CARD_ID);
            then(cardBenefitMapper).should(never()).insertBenefits(any());
        }
    }


    // ════════════════════════════════════════════════════════════════
    // saveCardImages — 이미지 등록/수정
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("이미지 등록/수정 (saveCardImages)")
    class SaveCardImages {

        @Test
        @DisplayName("[정상] 이미지 교체 → deleteByCardId 후 insertImages 호출")
        void 정상_이미지교체() {
            given(cardMapper.findById(CARD_ID)).willReturn(activeCard());
            given(cardBenefitMapper.findByCardId(CARD_ID)).willReturn(Collections.emptyList());
            given(cardImageMapper.findByCardId(CARD_ID)).willReturn(Collections.emptyList());
            given(cardVersionMapper.getLatestVersionSeq(CARD_ID)).willReturn(1);
            stubApprovalAndVersion();

            ImageCreateRequest img = new ImageCreateRequest();
            ReflectionTestUtils.setField(img, "imageType", "FRONT");
            ReflectionTestUtils.setField(img, "imageUrl",  "https://test.com/img.png");

            ImageUpdateRequest req = new ImageUpdateRequest();
            ReflectionTestUtils.setField(req, "images",        List.of(img));
            ReflectionTestUtils.setField(req, "changeSummary", "이미지 수정");

            Map<String, Long> result = adminCardService.saveCardImages(CARD_ID, req, ADMIN_ID);

            assertThat(result.get("cardId")).isEqualTo(CARD_ID);
            then(cardImageMapper).should().deleteByCardId(CARD_ID);
            then(cardImageMapper).should().insertImages(any());
        }

        @Test
        @DisplayName("[정상] 이미지 목록 비어있으면 → deleteByCardId만 호출, insertImages 미호출")
        void 정상_이미지빈목록() {
            given(cardMapper.findById(CARD_ID)).willReturn(activeCard());
            given(cardBenefitMapper.findByCardId(CARD_ID)).willReturn(Collections.emptyList());
            given(cardImageMapper.findByCardId(CARD_ID)).willReturn(Collections.emptyList());
            given(cardVersionMapper.getLatestVersionSeq(CARD_ID)).willReturn(1);
            stubApprovalAndVersion();

            ImageUpdateRequest req = new ImageUpdateRequest();
            ReflectionTestUtils.setField(req, "images",        Collections.emptyList());
            ReflectionTestUtils.setField(req, "changeSummary", "이미지 전체 삭제");

            adminCardService.saveCardImages(CARD_ID, req, ADMIN_ID);

            then(cardImageMapper).should().deleteByCardId(CARD_ID);
            then(cardImageMapper).should(never()).insertImages(any());
        }
    }


    // ════════════════════════════════════════════════════════════════
    // saveCardContents — 콘텐츠 등록/수정
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("콘텐츠 등록/수정 (saveCardContents)")
    class SaveCardContents {

        @Test
        @DisplayName("[정상] 콘텐츠 교체 → deleteByCardId 후 insertContents 호출")
        void 정상_콘텐츠교체() {
        	ContentCreateRequest item = new ContentCreateRequest();
        	ReflectionTestUtils.setField(item, "contentType",  "INTRO");
        	ReflectionTestUtils.setField(item, "title",         "소개");
        	ReflectionTestUtils.setField(item, "contentHtml",   "<p>내용</p>");
        	ReflectionTestUtils.setField(item, "displayOrder",  1);

        	ContentUpdateRequest req = new ContentUpdateRequest();
        	ReflectionTestUtils.setField(req, "contents", List.of(item));

            adminCardService.saveCardContents(CARD_ID, req, ADMIN_ID);

            then(cardContentMapper).should().deleteByCardId(CARD_ID);
            then(cardContentMapper).should().insertContents(any());
        }

        @Test
        @DisplayName("[정상] 콘텐츠 목록 비어있으면 → deleteByCardId만 호출, insertContents 미호출")
        void 정상_콘텐츠빈목록() {
            ContentUpdateRequest req = new ContentUpdateRequest();
            ReflectionTestUtils.setField(req, "contents", Collections.emptyList());

            adminCardService.saveCardContents(CARD_ID, req, ADMIN_ID);

            then(cardContentMapper).should().deleteByCardId(CARD_ID);
            then(cardContentMapper).should(never()).insertContents(any());
        }
    }


    // ════════════════════════════════════════════════════════════════
    // changeCardStatus — 카드 상태 강제 변경
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("카드 상태 강제 변경 (changeCardStatus)")
    class ChangeCardStatus {

        @Test
        @DisplayName("[정상] DRAFT → PUBLISHED 변경 → updateCardStatus + 이력 INSERT")
        void 정상_상태변경() {
            given(cardMapper.findById(CARD_ID)).willReturn(activeCard()); // DRAFT

            adminCardService.changeCardStatus(CARD_ID, statusReq("PUBLISHED"), ADMIN_ID);

            then(cardMapper).should().updateCardStatus(CARD_ID, "PUBLISHED");
            then(cardStatusHistoryMapper).should().insertCardStatusHistory(any());
        }

        @Test
        @DisplayName("[정상] 동일 상태 요청 → updateCardStatus·이력 미호출 (no-op)")
        void 정상_동일상태_noOp() {
            given(cardMapper.findById(CARD_ID)).willReturn(activeCard()); // DRAFT

            adminCardService.changeCardStatus(CARD_ID, statusReq("DRAFT"), ADMIN_ID);

            then(cardMapper).should(never()).updateCardStatus(anyLong(), anyString());
            then(cardStatusHistoryMapper).should(never()).insertCardStatusHistory(any());
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 cardId → CARD_NOT_FOUND")
        void 실패_카드없음() {
            given(cardMapper.findById(CARD_ID)).willReturn(null);

            assertThatThrownBy(() ->
                    adminCardService.changeCardStatus(CARD_ID, statusReq("PUBLISHED"), ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.CARD_NOT_FOUND));
        }
    }


    // ════════════════════════════════════════════════════════════════
    // getAdminCardList — 관리자 카드 목록 검색
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("관리자 카드 목록 검색 (getAdminCardList)")
    class GetAdminCardList {

        @Test
        @DisplayName("[정상] 결과 있음 → PageResponse 반환")
        void 정상_목록반환() {
            AdminCardSearchRequest req = new AdminCardSearchRequest();
            ReflectionTestUtils.setField(req, "page", 1);
            ReflectionTestUtils.setField(req, "size", 10);

            given(cardMapper.countAdminCards(req)).willReturn(1L);
            given(cardMapper.findAdminCards(req)).willReturn(List.of(activeCard()));

            PageResponse<?> result = adminCardService.getAdminCardList(req, ADMIN_ID);

            assertThat(result.getTotalCount()).isEqualTo(1L);
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("[정상] 결과 없음 → 빈 PageResponse 반환 (findAdminCards 미호출)")
        void 정상_결과없음_빈페이지() {
            AdminCardSearchRequest req = new AdminCardSearchRequest();
            ReflectionTestUtils.setField(req, "page", 1);
            ReflectionTestUtils.setField(req, "size", 10);

            given(cardMapper.countAdminCards(req)).willReturn(0L);

            PageResponse<?> result = adminCardService.getAdminCardList(req, ADMIN_ID);

            assertThat(result.getContent()).isEmpty();
            then(cardMapper).should(never()).findAdminCards(any());
        }
    }


    // ════════════════════════════════════════════════════════════════
    // getAdminCardDetail — 관리자 카드 상세 조회
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("관리자 카드 상세 조회 (getAdminCardDetail)")
    class GetAdminCardDetail {

        @Test
        @DisplayName("[정상] 존재하는 카드 → CardDetailResponse 반환")
        void 정상_상세조회() {
            given(cardMapper.findById(CARD_ID)).willReturn(activeCard());
            given(cardBenefitMapper.findByCardId(CARD_ID)).willReturn(Collections.emptyList());
            given(cardImageMapper.findByCardId(CARD_ID)).willReturn(Collections.emptyList());
            given(cardContentMapper.findByCardId(CARD_ID)).willReturn(Collections.emptyList());
            given(termsMapper.findTermsFilesByCardId(CARD_ID)).willReturn(Collections.emptyList());

            CardDetailResponse result = adminCardService.getAdminCardDetail(CARD_ID);

            assertThat(result).isNotNull();
            assertThat(result.getCardId()).isEqualTo(CARD_ID);
            assertThat(result.getCardName()).isEqualTo("테스트카드");
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 cardId → IllegalArgumentException")
        void 실패_카드없음() {
            given(cardMapper.findById(CARD_ID)).willReturn(null);

            assertThatThrownBy(() -> adminCardService.getAdminCardDetail(CARD_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 카드");
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 유틸 — BigDecimal or Double 필드 대응 (혜택 discountRate)
    // ────────────────────────────────────────────────────────────────
    private Object BigDecimalOrDouble(double val) {
        // discountRate 필드 타입이 BigDecimal인지 Double인지 프로젝트 모델 확인 후 맞게 수정
        return java.math.BigDecimal.valueOf(val);
    }
}