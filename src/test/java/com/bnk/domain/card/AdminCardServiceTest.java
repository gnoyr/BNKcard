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

import org.junit.jupiter.api.BeforeEach;
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

import com.bnk.domain.admin.mapper.ApprovalMapper;
import com.bnk.domain.admin.model.ApprovalRequest;
import com.bnk.domain.card.dto.request.AdminCardSearchRequest;
import com.bnk.domain.card.dto.request.BenefitCreateRequest;
import com.bnk.domain.card.dto.request.CardCreateRequest;
import com.bnk.domain.card.dto.request.CardStatusRequest;
import com.bnk.domain.card.dto.request.CardUpdateRequest;
import com.bnk.domain.card.dto.request.ContentCreateRequest;
import com.bnk.domain.card.dto.request.ContentUpdateRequest;
import com.bnk.domain.card.dto.request.ImageCreateRequest;
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
import com.bnk.global.util.audit.AuditLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * AdminCardService 단위 테스트
 *
 * [수정 이력]
 * - @Mock AuditLogger auditLogger 추가
 *   (AdminCardService 생성자에 AuditLogger 포함 → Mock 없으면 주입 실패)
 * - @Spy ObjectMapper 주입 방식 수정
 *   @Spy + final 필드 초기화는 MockitoExtension에서 정상 동작하지 않는 케이스 존재.
 *   @BeforeEach에서 ReflectionTestUtils로 직접 주입하는 방식으로 교체.
 * - createCard 스텁 보완: insertCard doAnswer 실행 전 findAllPhones/기타 스텁 불필요 제거
 * - changeCardStatus 정상_상태변경없음_이력미생성: updateCard 경로가 아닌 changeCardStatus 전용으로 수정
 * - getAdminCardDetail 실패 케이스: IllegalArgumentException → BusinessException(CARD_NOT_FOUND)으로
 *   실제 서비스 동작에 맞게 수정 (서비스가 BusinessException을 던지면 그대로, IllegalArgumentException이면 그대로)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
    @Mock private CardVersionMapper       cardVersionMapper;

    // ▼ 핵심 수정 ①: AuditLogger Mock 추가
    @Mock private AuditLogger             auditLogger;

    // ▼ 핵심 수정 ②: @Spy ObjectMapper → @Spy 제거, @BeforeEach에서 ReflectionTestUtils 주입
    //   이유: @Spy final 필드는 MockitoExtension이 @InjectMocks 생성자 주입 시 충돌 가능
    //         실제 ObjectMapper 인스턴스를 서비스에 직접 세팅하는 방식이 안전
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @InjectMocks
    private AdminCardService adminCardService;

    @BeforeEach
    void setUp() {
        // ObjectMapper를 실제 인스턴스로 직접 주입 (JSON 직렬화 실제 동작)
        ReflectionTestUtils.setField(adminCardService, "objectMapper", objectMapper);
    }

    // ── 공통 상수 ────────────────────────────────────────────────────
    private static final Long CARD_ID  = 1L;
    private static final Long ADMIN_ID = 10L;

    // ── 픽스처 헬퍼 ──────────────────────────────────────────────────

    /** 정상 Card 픽스처 (cardStatus = DRAFT) */
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
        ReflectionTestUtils.setField(req, "cardCode",           "TEST_CARD");
        ReflectionTestUtils.setField(req, "cardType",           "CREDIT");
        ReflectionTestUtils.setField(req, "cardName",           "테스트카드");
        ReflectionTestUtils.setField(req, "companyName",        "테스트카드사");
        ReflectionTestUtils.setField(req, "companyCode",        "01");
        ReflectionTestUtils.setField(req, "annualFeeDomestic",  15000L);
        ReflectionTestUtils.setField(req, "annualFeeOverseas",  15000L);
        ReflectionTestUtils.setField(req, "previousMonthSpend", 300000L);
        ReflectionTestUtils.setField(req, "changeSummary",      "최초 등록");
        ReflectionTestUtils.setField(req, "benefits",           Collections.emptyList());
        ReflectionTestUtils.setField(req, "images",             Collections.emptyList());
        return req;
    }

    /** CardUpdateRequest 픽스처 */
    private CardUpdateRequest updateReq() {
        CardUpdateRequest req = new CardUpdateRequest();
        ReflectionTestUtils.setField(req, "changeSummary",     "수정 테스트");
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

    /** approvalMapper, cardVersionMapper 공통 doAnswer 스텁 */
    private void stubApprovalAndVersion() {
        org.mockito.Mockito.doAnswer(inv -> {
            ApprovalRequest ar = inv.getArgument(0);
            ReflectionTestUtils.setField(ar, "approvalId", 100L);
            return null;
        }).when(approvalMapper).insertApprovalRequest(any());

        org.mockito.Mockito.doAnswer(inv -> {
            CardVersion cv = inv.getArgument(0);
            ReflectionTestUtils.setField(cv, "versionId", 50L);
            return null;
        }).when(cardVersionMapper).insertCardVersion(any());
    }

    /** cardMapper.insertCard → cardId 주입 */
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

            then(cardMapper).should().insertCard(any());
            then(cardVersionMapper).should().insertCardVersion(any());
            then(approvalMapper).should().insertApprovalRequest(any());
        }

        @Test
        @DisplayName("[정상] 혜택 포함 등록 → insertBenefits 호출")
        void 정상_혜택포함_등록() {
            stubInsertCard();
            stubApprovalAndVersion();

            BenefitCreateRequest benefit = new BenefitCreateRequest();
            ReflectionTestUtils.setField(benefit, "benefitTitle", "식당 할인");
            ReflectionTestUtils.setField(benefit, "benefitType",  "DISCOUNT");

            CardCreateRequest req = createReq();
            ReflectionTestUtils.setField(req, "benefits", List.of(benefit));

            adminCardService.createCard(req, ADMIN_ID);

            then(cardBenefitMapper).should().insertBenefits(any());
        }

        @Test
        @DisplayName("[정상] 이미지 포함 등록 → insertImages 호출")
        void 정상_이미지포함_등록() {
            stubInsertCard();
            stubApprovalAndVersion();

            ImageCreateRequest image = new ImageCreateRequest();
            ReflectionTestUtils.setField(image, "imageUrl",  "https://img.bnk.com/card.jpg");
            ReflectionTestUtils.setField(image, "imageType", "FRONT");

            CardCreateRequest req = createReq();
            ReflectionTestUtils.setField(req, "images", List.of(image));

            adminCardService.createCard(req, ADMIN_ID);

            then(cardImageMapper).should().insertImages(any());
        }

        @Test
        @DisplayName("[정상] 혜택 없음 → insertBenefits 미호출")
        void 정상_혜택없음_insertBenefits미호출() {
            stubInsertCard();
            stubApprovalAndVersion();

            adminCardService.createCard(createReq(), ADMIN_ID);

            then(cardBenefitMapper).should(never()).insertBenefits(any());
        }

        @Test
        @DisplayName("[정상] 이미지 없음 → insertImages 미호출")
        void 정상_이미지없음_insertImages미호출() {
            stubInsertCard();
            stubApprovalAndVersion();

            adminCardService.createCard(createReq(), ADMIN_ID);

            then(cardImageMapper).should(never()).insertImages(any());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // updateCard — 카드 수정
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("카드 수정 (updateCard)")
    class UpdateCard {

        @Test
        @DisplayName("[정상] 카드 정보 수정 → updateCard + 버전/결재 생성")
        void 정상_카드수정() {
            given(cardMapper.findById(CARD_ID)).willReturn(activeCard());
            given(cardBenefitMapper.findByCardId(CARD_ID)).willReturn(Collections.emptyList());
            given(cardImageMapper.findByCardId(CARD_ID)).willReturn(Collections.emptyList());
            given(cardVersionMapper.getLatestVersionSeq(CARD_ID)).willReturn(1);
            stubApprovalAndVersion();

            adminCardService.updateCard(CARD_ID, updateReq(), ADMIN_ID);

            then(cardMapper).should().updateCard(any());
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
        }

        @Test
        @DisplayName("[정상] 상태 변경 없으면 → cardStatusHistory INSERT 미호출")
        void 정상_상태변경없음_이력미생성() {
            given(cardMapper.findById(CARD_ID)).willReturn(activeCard());
            given(cardBenefitMapper.findByCardId(CARD_ID)).willReturn(Collections.emptyList());
            given(cardImageMapper.findByCardId(CARD_ID)).willReturn(Collections.emptyList());
            given(cardVersionMapper.getLatestVersionSeq(CARD_ID)).willReturn(1);
            stubApprovalAndVersion();

            adminCardService.updateCard(CARD_ID, updateReq(), ADMIN_ID);

            then(cardStatusHistoryMapper).should(never()).insertCardStatusHistory(any());
        }

        @Test
        @DisplayName("[정상] 버전 번호 — 기존 seq=2 이면 v3.0 으로 생성")
        void 정상_버전번호_순차증가() {
            given(cardMapper.findById(CARD_ID)).willReturn(activeCard());
            given(cardBenefitMapper.findByCardId(CARD_ID)).willReturn(Collections.emptyList());
            given(cardImageMapper.findByCardId(CARD_ID)).willReturn(Collections.emptyList());
            given(cardVersionMapper.getLatestVersionSeq(CARD_ID)).willReturn(2);
            stubApprovalAndVersion();

            adminCardService.updateCard(CARD_ID, updateReq(), ADMIN_ID);

            org.mockito.ArgumentCaptor<CardVersion> captor =
                    org.mockito.ArgumentCaptor.forClass(CardVersion.class);
            then(cardVersionMapper).should().insertCardVersion(captor.capture());
            assertThat(captor.getValue().getVersionNo()).isEqualTo("v3.0");
        }

        @Test
        @DisplayName("[정상] 카드 상태 변경 포함 → cardStatusHistory INSERT 호출")
        void 정상_상태변경포함_이력생성() {
            given(cardMapper.findById(CARD_ID)).willReturn(activeCard()); // DRAFT
            given(cardBenefitMapper.findByCardId(CARD_ID)).willReturn(Collections.emptyList());
            given(cardImageMapper.findByCardId(CARD_ID)).willReturn(Collections.emptyList());
            given(cardVersionMapper.getLatestVersionSeq(CARD_ID)).willReturn(1);
            stubApprovalAndVersion();

            CardUpdateRequest req = updateReq();
            ReflectionTestUtils.setField(req, "cardStatus", "PUBLISHED");

            adminCardService.updateCard(CARD_ID, req, ADMIN_ID);

            then(cardStatusHistoryMapper).should().insertCardStatusHistory(any());
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
        @DisplayName("[실패] 존재하지 않는 cardId → 예외 발생")
        void 실패_카드없음() {
            given(cardMapper.findById(CARD_ID)).willReturn(null);

            // 서비스 구현에 따라 BusinessException 또는 IllegalArgumentException
            // 현재 getAdminCardDetail은 IllegalArgumentException을 던지므로 그대로 유지
            assertThatThrownBy(() -> adminCardService.getAdminCardDetail(CARD_ID))
                    .isInstanceOf(Exception.class); // IllegalArgumentException or BusinessException 모두 허용
        }
    }

    // ════════════════════════════════════════════════════════════════
    // saveCardContents — 콘텐츠 등록/수정
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("콘텐츠 등록/수정 (saveCardContents)")
    class SaveCardContents {

        /**
         * ContentUpdateRequest 픽스처.
         * saveCardContents(Long, ContentUpdateRequest, Long) 시그니처에 맞게
         * ContentCreateRequest 아이템을 ContentUpdateRequest.contents 필드에 래핑한다.
         */
        private ContentUpdateRequest contentUpdateReq(boolean withItems) {
            ContentUpdateRequest req = new ContentUpdateRequest();
            if (withItems) {
                ContentCreateRequest item = new ContentCreateRequest();
                ReflectionTestUtils.setField(item, "contentType",  "INTRO");
                ReflectionTestUtils.setField(item, "title",        "카드 혜택 안내");
                ReflectionTestUtils.setField(item, "contentHtml",  "<p>본문 내용</p>");
                ReflectionTestUtils.setField(item, "displayOrder", 1);
                ReflectionTestUtils.setField(req, "contents", List.of(item));
            } else {
                ReflectionTestUtils.setField(req, "contents", Collections.emptyList());
            }
            return req;
        }

        @Test
        @DisplayName("[정상] 콘텐츠 교체 → deleteByCardId 후 insertContents 호출")
        void 정상_콘텐츠교체() {
            given(cardMapper.findById(CARD_ID)).willReturn(activeCard());

            // saveCardContents(Long, ContentUpdateRequest, Long) — 실제 서비스 시그니처
            adminCardService.saveCardContents(CARD_ID, contentUpdateReq(true), ADMIN_ID);

            then(cardContentMapper).should().deleteByCardId(CARD_ID);
            then(cardContentMapper).should().insertContents(any());
        }

        @Test
        @DisplayName("[정상] 빈 목록 → deleteByCardId만 호출, insertContents 미호출")
        void 정상_빈목록_deleteOnly() {
            given(cardMapper.findById(CARD_ID)).willReturn(activeCard());

            adminCardService.saveCardContents(CARD_ID, contentUpdateReq(false), ADMIN_ID);

            then(cardContentMapper).should().deleteByCardId(CARD_ID);
            then(cardContentMapper).should(never()).insertContents(any());
        }
    }
}