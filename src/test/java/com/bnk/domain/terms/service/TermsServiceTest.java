package com.bnk.domain.terms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.bnk.domain.terms.dto.request.AgreedTermsItem;
import com.bnk.domain.terms.dto.request.TermsAgreementRequest;
import com.bnk.domain.terms.dto.response.TermsFileResponse;
import com.bnk.domain.terms.dto.response.TermsPackageResponse;
import com.bnk.domain.terms.mapper.TermsMapper;
import com.bnk.domain.terms.mapper.UserTermsAgreementMapper;
import com.bnk.domain.terms.model.Terms;
import com.bnk.domain.terms.model.TermsFile;
import com.bnk.domain.terms.model.UserTermsAgreement;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.util.ObjectStorageService;

/**
 * TermsService 단위 테스트 (SonarQube 커버리지 대상)
 *
 * ── 설계 원칙 ────────────────────────────────────────────────────────
 * · MockitoExtension — Spring Context 없이 경량 실행
 * · Nested 클래스로 메서드별 시나리오 그룹화
 * · 정상(Happy Path) + 예외(Edge/Error) 모두 커버
 * · ArgumentCaptor로 실제 INSERT 파라미터 검증
 *
 * ── 커버리지 대상 메서드 ──────────────────────────────────────────────
 * · getTermsPackage(String)          — 정상/빈목록 예외
 * · agreeTerms(TermsAgreementRequest, Long) — 정상/필수미동의/약관없음/배치처리
 * · getTermsFiles(Long)              — 정상/약관없음/빈파일목록
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TermsService 단위 테스트")
class TermsServiceTest {

    /* ── Mocks ─────────────────────────────────────────────────── */
    @Mock private TermsMapper              termsMapper;
    @Mock private UserTermsAgreementMapper userTermsAgreementMapper;
    @Mock private ObjectStorageService     objectStorageService;

    @InjectMocks
    private TermsService termsService;

    // ════════════════════════════════════════════════════════════════
    // Fixture 헬퍼
    // ════════════════════════════════════════════════════════════════

    /** Terms 픽스처 */
    private Terms terms(Long termsId, String requiredYn) {
        Terms t = new Terms();
        ReflectionTestUtils.setField(t, "termsId",     termsId);
        ReflectionTestUtils.setField(t, "requiredYn",  requiredYn);
        ReflectionTestUtils.setField(t, "status",      "PUBLISHED");
        ReflectionTestUtils.setField(t, "termsMasterId", 10L);
        return t;
    }

    /** TermsFile 픽스처 */
    private TermsFile termsFile(Long fileId, Long termsId) {
        TermsFile f = new TermsFile();
        ReflectionTestUtils.setField(f, "fileId",        fileId);
        ReflectionTestUtils.setField(f, "termsId",       termsId);
        ReflectionTestUtils.setField(f, "fileType",      "PDF");
        ReflectionTestUtils.setField(f, "filePath",      "oci://bucket/terms/" + fileId + ".pdf");
        ReflectionTestUtils.setField(f, "originalName",  "terms_" + fileId + ".pdf");
        ReflectionTestUtils.setField(f, "fileExtension", "pdf");
        ReflectionTestUtils.setField(f, "fileSize",      102400L);
        ReflectionTestUtils.setField(f, "mimeType",      "application/pdf");
        ReflectionTestUtils.setField(f, "isPrimary",     "Y");
        return f;
    }

    /** TermsAgreementRequest 픽스처 빌더 */
    private TermsAgreementRequest agreementRequest(List<AgreedTermsItem> items) {
        TermsAgreementRequest req = new TermsAgreementRequest();
        ReflectionTestUtils.setField(req, "agreementSource",  "SIGNUP");
        ReflectionTestUtils.setField(req, "agreementChannel", "WEB");
        ReflectionTestUtils.setField(req, "agreedTerms",      items);
        return req;
    }

    /** AgreedTermsItem 픽스처 */
    private AgreedTermsItem agreedItem(Long termsId, String agreedYn) {
        AgreedTermsItem item = new AgreedTermsItem();
        ReflectionTestUtils.setField(item, "termsId",  termsId);
        ReflectionTestUtils.setField(item, "agreedYn", agreedYn);
        return item;
    }

    // ════════════════════════════════════════════════════════════════
    // 1. getTermsPackage
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("약관 패키지 조회 [getTermsPackage]")
    class GetTermsPackage {

        @Test
        @DisplayName("[정상] SIGNUP 패키지 → TermsPackageResponse 반환")
        void 정상_SIGNUP패키지_조회() {
            // given
            Terms t1 = terms(1L, "Y");
            Terms t2 = terms(2L, "N");
            given(termsMapper.findByPackageType("SIGNUP")).willReturn(List.of(t1, t2));

            // when
            TermsPackageResponse response = termsService.getTermsPackage("SIGNUP");

            // then
            assertThat(response).isNotNull();
            assertThat(response.getPackageType()).isEqualTo("SIGNUP");
            assertThat(response.getTerms()).hasSize(2);
            then(termsMapper).should().findByPackageType("SIGNUP");
        }

        @Test
        @DisplayName("[정상] CARD_APPLY 패키지 → 단일 항목 반환")
        void 정상_CARD_APPLY패키지_단일항목() {
            // given
            given(termsMapper.findByPackageType("CARD_APPLY"))
                    .willReturn(List.of(terms(3L, "Y")));

            // when
            TermsPackageResponse response = termsService.getTermsPackage("CARD_APPLY");

            // then
            assertThat(response.getTerms()).hasSize(1);
            assertThat(response.getPackageType()).isEqualTo("CARD_APPLY");
        }

        @Test
        @DisplayName("[예외] 해당 패키지 약관 없음 → TERMS_NOT_FOUND 예외")
        void 예외_패키지없음() {
            // given
            given(termsMapper.findByPackageType("UNKNOWN")).willReturn(Collections.emptyList());

            // when & then
            assertThatThrownBy(() -> termsService.getTermsPackage("UNKNOWN"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.TERMS_NOT_FOUND);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 2. agreeTerms
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("약관 동의 처리 [agreeTerms]")
    class AgreeTerms {

        @Test
        @DisplayName("[정상] 필수+선택 약관 모두 동의 → insertAgreements 호출 & termsId 목록 반환")
        void 정상_전체동의() {
            // given
            Long userId = 100L;
            Terms required = terms(1L, "Y");
            Terms optional = terms(2L, "N");

            given(termsMapper.findById(1L)).willReturn(Optional.of(required));
            given(termsMapper.findById(2L)).willReturn(Optional.of(optional));

            List<AgreedTermsItem> items = List.of(
                    agreedItem(1L, "Y"),
                    agreedItem(2L, "Y")
            );
            TermsAgreementRequest req = agreementRequest(items);

            // when
            List<Long> result = termsService.agreeTerms(req, userId);

            // then
            assertThat(result).containsExactlyInAnyOrder(1L, 2L);
            then(userTermsAgreementMapper).should(times(1))
                    .insertAgreements(any());
        }

        @Test
        @DisplayName("[정상] 필수 동의 + 선택 미동의(N) → 정상 처리 (선택 약관은 미동의 허용)")
        void 정상_선택약관미동의() {
            // given
            Long userId = 101L;
            Terms required = terms(1L, "Y");
            Terms optional = terms(2L, "N");

            given(termsMapper.findById(1L)).willReturn(Optional.of(required));
            given(termsMapper.findById(2L)).willReturn(Optional.of(optional));

            List<AgreedTermsItem> items = List.of(
                    agreedItem(1L, "Y"),   // 필수 — 동의
                    agreedItem(2L, "N")    // 선택 — 미동의 (허용)
            );
            TermsAgreementRequest req = agreementRequest(items);

            // when
            List<Long> result = termsService.agreeTerms(req, userId);

            // then — 두 termsId 모두 반환 (동의 여부와 무관하게 처리된 항목)
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("[예외] 필수 약관 미동의(N) → REQUIRED_TERMS_NOT_AGREED 예외")
        void 예외_필수약관미동의() {
            // given
            Long userId = 102L;
            Terms required = terms(1L, "Y"); // requiredYn = "Y"

            given(termsMapper.findById(1L)).willReturn(Optional.of(required));

            List<AgreedTermsItem> items = List.of(agreedItem(1L, "N")); // 필수인데 미동의
            TermsAgreementRequest req = agreementRequest(items);

            // when & then
            assertThatThrownBy(() -> termsService.agreeTerms(req, userId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.REQUIRED_TERMS_NOT_AGREED);

            then(userTermsAgreementMapper).should(never()).insertAgreements(any());
        }

        @Test
        @DisplayName("[예외] 존재하지 않는 termsId → TERMS_NOT_FOUND 예외")
        void 예외_존재하지않는약관() {
            // given
            Long userId = 103L;
            given(termsMapper.findById(99L)).willReturn(Optional.empty());

            List<AgreedTermsItem> items = List.of(agreedItem(99L, "Y"));
            TermsAgreementRequest req = agreementRequest(items);

            // when & then
            assertThatThrownBy(() -> termsService.agreeTerms(req, userId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.TERMS_NOT_FOUND);
        }

        @Test
        @DisplayName("[정상] 배치 경계 초과 (51건) → 2회 배치 INSERT 실행")
        void 정상_배치경계초과_51건() {
            // given — BATCH_SIZE = 50, 51개 항목 → 2번 insertAgreements 호출 예상
            Long userId = 104L;
            List<AgreedTermsItem> items = new java.util.ArrayList<>();
            for (long i = 1; i <= 51; i++) {
                Terms t = terms(i, "N"); // 선택 약관
                given(termsMapper.findById(i)).willReturn(Optional.of(t));
                items.add(agreedItem(i, "Y"));
            }
            TermsAgreementRequest req = agreementRequest(items);

            // when
            List<Long> result = termsService.agreeTerms(req, userId);

            // then
            assertThat(result).hasSize(51);
            // 50건 첫 배치 + 1건 두 번째 배치 = 2회 호출
            then(userTermsAgreementMapper).should(times(2)).insertAgreements(any());
        }

        @Test
        @DisplayName("[정상] 정확히 BATCH_SIZE(50건) → 1회 배치 INSERT 실행")
        void 정상_정확히배치사이즈() {
            // given
            Long userId = 105L;
            List<AgreedTermsItem> items = new java.util.ArrayList<>();
            for (long i = 1; i <= 50; i++) {
                Terms t = terms(i, "N");
                given(termsMapper.findById(i)).willReturn(Optional.of(t));
                items.add(agreedItem(i, "Y"));
            }
            TermsAgreementRequest req = agreementRequest(items);

            // when
            List<Long> result = termsService.agreeTerms(req, userId);

            // then
            assertThat(result).hasSize(50);
            then(userTermsAgreementMapper).should(times(1)).insertAgreements(any());
        }

        @Test
        @DisplayName("[정상] ArgumentCaptor — INSERT 파라미터 userId·agreementSource 검증")
        void 정상_INSERT파라미터검증() {
            // given
            Long userId = 106L;
            Terms optional = terms(10L, "N");
            given(termsMapper.findById(10L)).willReturn(Optional.of(optional));

            TermsAgreementRequest req = agreementRequest(List.of(agreedItem(10L, "Y")));

            // when
            termsService.agreeTerms(req, userId);

            // then — insertAgreements 호출 시 전달된 리스트 검증
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<UserTermsAgreement>> captor =
                    ArgumentCaptor.forClass(List.class);
            then(userTermsAgreementMapper).should().insertAgreements(captor.capture());

            List<UserTermsAgreement> captured = captor.getValue();
            assertThat(captured).hasSize(1);
            assertThat(captured.get(0).getUserId()).isEqualTo(userId);
            assertThat(captured.get(0).getAgreementSource()).isEqualTo("SIGNUP");
            assertThat(captured.get(0).getAgreementAction()).isEqualTo("AGREE");
        }

        @Test
        @DisplayName("[정상] agreedYn=N 항목 → agreementAction=DISAGREE 매핑")
        void 정상_DISAGREE_매핑() {
            // given
            Long userId = 107L;
            Terms optional = terms(20L, "N");
            given(termsMapper.findById(20L)).willReturn(Optional.of(optional));

            TermsAgreementRequest req = agreementRequest(List.of(agreedItem(20L, "N")));

            // when
            termsService.agreeTerms(req, userId);

            // then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<UserTermsAgreement>> captor =
                    ArgumentCaptor.forClass(List.class);
            then(userTermsAgreementMapper).should().insertAgreements(captor.capture());
            assertThat(captor.getValue().get(0).getAgreementAction()).isEqualTo("DISAGREE");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 3. getTermsFiles
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("약관 파일 URL 조회 [getTermsFiles]")
    class GetTermsFiles {

        @Test
        @DisplayName("[정상] 유효한 termsId → 파일 목록 반환 & presigned URL 변환")
        void 정상_파일목록반환() {
            // given
            Long termsId = 1L;
            given(termsMapper.findById(termsId)).willReturn(Optional.of(terms(termsId, "Y")));
            given(termsMapper.findFilesByTermsId(termsId))
                    .willReturn(List.of(termsFile(100L, termsId)));
            given(objectStorageService.resolveUrl(anyString()))
                    .willReturn("https://cdn.example.com/terms/100.pdf");

            // when
            List<TermsFileResponse> result = termsService.getTermsFiles(termsId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getFileId()).isEqualTo(100L);
            assertThat(result.get(0).getFilePath())
                    .isEqualTo("https://cdn.example.com/terms/100.pdf");
            assertThat(result.get(0).getMimeType()).isEqualTo("application/pdf");
            assertThat(result.get(0).getIsPrimary()).isEqualTo("Y");
        }

        @Test
        @DisplayName("[정상] 파일이 여러 개인 경우 → 모두 반환")
        void 정상_다수파일반환() {
            // given
            Long termsId = 2L;
            given(termsMapper.findById(termsId)).willReturn(Optional.of(terms(termsId, "Y")));
            given(termsMapper.findFilesByTermsId(termsId))
                    .willReturn(List.of(
                            termsFile(201L, termsId),
                            termsFile(202L, termsId)
                    ));
            given(objectStorageService.resolveUrl(anyString()))
                    .willAnswer(inv -> "https://cdn.example.com/" + inv.getArgument(0));

            // when
            List<TermsFileResponse> result = termsService.getTermsFiles(termsId);

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("[정상] 약관은 있으나 파일 없음 → 빈 목록 반환")
        void 정상_파일없음_빈목록() {
            // given
            Long termsId = 3L;
            given(termsMapper.findById(termsId)).willReturn(Optional.of(terms(termsId, "Y")));
            given(termsMapper.findFilesByTermsId(termsId)).willReturn(Collections.emptyList());

            // when
            List<TermsFileResponse> result = termsService.getTermsFiles(termsId);

            // then
            assertThat(result).isEmpty();
            then(objectStorageService).should(never()).resolveUrl(anyString());
        }

        @Test
        @DisplayName("[예외] 존재하지 않는 termsId → TERMS_NOT_FOUND 예외")
        void 예외_약관없음() {
            // given
            given(termsMapper.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> termsService.getTermsFiles(999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.TERMS_NOT_FOUND);

            then(termsMapper).should(never()).findFilesByTermsId(anyLong());
        }
    }
}