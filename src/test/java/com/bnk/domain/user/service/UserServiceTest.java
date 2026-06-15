package com.bnk.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.bnk.domain.user.dto.query.CardApplicationRow;
import com.bnk.domain.user.dto.query.OwnedCardRow;
import com.bnk.domain.user.dto.request.PasswordChangeRequest;
import com.bnk.domain.user.dto.request.UserUpdateRequest;
import com.bnk.domain.user.dto.response.CardStatusResponse;
import com.bnk.domain.user.dto.response.UserResponse;
import com.bnk.domain.user.mapper.UserMapper;
import com.bnk.domain.user.model.User;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.util.audit.AuditLogger;

/**
 * UserService 단위 테스트 (SonarQube 커버리지 대상)
 *
 * ── 주의 사항 ───────────────────────────────────────────────────────
 * · User 모델 비밀번호 필드명: passwordHash  (password 아님)
 * · updatePassword 시그니처: (userId, passwordHash, lastPasswordChangedAt) — 3개 파라미터
 * · updateMyInfo 분기:
 *     - 개인정보(name/phone/job 등) 변경 → currentPassword 검증 필요
 *     - 알림 설정(pushEnabled/marketingAgree) 만 변경 → 검증 불필요
 * · getMyCards 반환 타입: CardStatusResponse(ownedCards, applications)
 *     - ownedCards = selectOwnedCards(userId) 결과
 *     - applications = selectCardApplications(userId) 결과
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserService 단위 테스트")
class UserServiceTest {

    @Mock private UserMapper      userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditLogger     auditLogger;

    @InjectMocks
    private UserService userService;

    // ── 상수 ─────────────────────────────────────────────────────────
    private static final Long   USER_ID = 1L;
    private static final String CURRENT = "Current123!";
    private static final String NEW_PW  = "NewSecure123!";
    private static final String ENC_PW  = "$2a$10$encodedPasswordHashValue";

    // ── Fixture ──────────────────────────────────────────────────────

    private User activeUser() {
        User u = new User();
        ReflectionTestUtils.setField(u, "userId",       USER_ID);
        ReflectionTestUtils.setField(u, "passwordHash", ENC_PW);   // 실제 필드명
        ReflectionTestUtils.setField(u, "statusCode",   "ACTIVE");
        ReflectionTestUtils.setField(u, "email",        "test@bnk.co.kr");
        ReflectionTestUtils.setField(u, "name",         "홍길동");
        ReflectionTestUtils.setField(u, "phone",        "010-1234-5678");
        return u;
    }

    private PasswordChangeRequest pwChangeReq(String current, String newPw, String confirm) {
        PasswordChangeRequest req = new PasswordChangeRequest();
        ReflectionTestUtils.setField(req, "currentPassword",    current);
        ReflectionTestUtils.setField(req, "newPassword",        newPw);
        ReflectionTestUtils.setField(req, "newPasswordConfirm", confirm);
        return req;
    }

    /** 개인정보(name/phone) 변경 요청 — currentPassword 필수 */
    private UserUpdateRequest updateReq(String name, String phone, String currentPw) {
        UserUpdateRequest req = new UserUpdateRequest();
        if (name != null)      ReflectionTestUtils.setField(req, "name",            name);
        if (phone != null)     ReflectionTestUtils.setField(req, "phone",           phone);
        if (currentPw != null) ReflectionTestUtils.setField(req, "currentPassword", currentPw);
        return req;
    }

    /** 알림 설정만 변경 — currentPassword 불필요 */
    private UserUpdateRequest notificationOnlyReq(Boolean push, Boolean mkt) {
        UserUpdateRequest req = new UserUpdateRequest();
        if (push != null) ReflectionTestUtils.setField(req, "pushEnabled",    push);
        if (mkt  != null) ReflectionTestUtils.setField(req, "marketingAgree", mkt);
        return req;
    }

    // ════════════════════════════════════════════════════════════════
    // F-24 | 내 정보 조회
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("내 정보 조회 [getMyInfo]")
    class GetMyInfo {

        @Test
        @DisplayName("[정상] 유효 userId → UserResponse(maskedEmail 포함) 반환")
        void 정상_내정보조회() {
            given(userMapper.findById(USER_ID)).willReturn(Optional.of(activeUser()));

            UserResponse response = userService.getMyInfo(USER_ID);

            assertThat(response.getUserId()).isEqualTo(USER_ID);
            assertThat(response.getMaskedEmail()).isNotBlank();
            assertThat(response.getMaskedPhone()).isNotBlank();
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 userId → USER_NOT_FOUND")
        void 실패_사용자없음() {
            given(userMapper.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getMyInfo(USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // F-25 | 내 정보 수정
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("내 정보 수정 [updateMyInfo]")
    class UpdateMyInfo {

        @Test
        @DisplayName("[정상] name 변경 + 비밀번호 검증 통과 → updateUser 호출")
        void 정상_name변경() {
            given(userMapper.findById(USER_ID)).willReturn(Optional.of(activeUser()));
            given(passwordEncoder.matches(CURRENT, ENC_PW)).willReturn(true);

            userService.updateMyInfo(USER_ID, updateReq("이순신", null, CURRENT));

            then(userMapper).should().updateUser(any(User.class));
        }

        @Test
        @DisplayName("[정상] phone 변경 + 비밀번호 검증 통과 → updateUser 호출")
        void 정상_phone변경() {
            given(userMapper.findById(USER_ID)).willReturn(Optional.of(activeUser()));
            given(passwordEncoder.matches(CURRENT, ENC_PW)).willReturn(true);

            userService.updateMyInfo(USER_ID, updateReq(null, "010-9999-8888", CURRENT));

            then(userMapper).should().updateUser(any(User.class));
        }

        @Test
        @DisplayName("[정상] 알림 설정만 변경 → 비밀번호 검증 없이 updateUser 호출")
        void 정상_알림설정만변경() {
            given(userMapper.findById(USER_ID)).willReturn(Optional.of(activeUser()));

            userService.updateMyInfo(USER_ID, notificationOnlyReq(true, false));

            then(userMapper).should().updateUser(any(User.class));
            then(passwordEncoder).should(never()).matches(any(), any());
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 userId → USER_NOT_FOUND")
        void 실패_사용자없음() {
            given(userMapper.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateMyInfo(USER_ID, updateReq("이순신", null, CURRENT)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }

        @Test
        @DisplayName("[실패] 현재 비밀번호 불일치 → INVALID_PASSWORD")
        void 실패_현재비밀번호불일치() {
            given(userMapper.findById(USER_ID)).willReturn(Optional.of(activeUser()));
            given(passwordEncoder.matches("WrongPw!", ENC_PW)).willReturn(false);

            assertThatThrownBy(() -> userService.updateMyInfo(USER_ID, updateReq("이순신", null, "WrongPw!")))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_PASSWORD));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // F-26 | 비밀번호 변경
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("비밀번호 변경 [changePassword]")
    class ChangePassword {

        @Test
        @DisplayName("[정상] 모든 조건 충족 → updatePassword + insertPasswordHistory + revokeAllSessions 호출")
        void 정상_비밀번호변경() {
            given(userMapper.findById(USER_ID)).willReturn(Optional.of(activeUser()));
            given(passwordEncoder.matches(CURRENT, ENC_PW)).willReturn(true);
            given(userMapper.findRecentPasswordHashes(USER_ID, 5)).willReturn(Collections.emptyList());
            given(passwordEncoder.encode(NEW_PW)).willReturn("$2a$10$newHash");

            userService.changePassword(USER_ID, pwChangeReq(CURRENT, NEW_PW, NEW_PW));

            // updatePassword(userId, hash, lastPasswordChangedAt) — 3개 파라미터
            then(userMapper).should().updatePassword(eq(USER_ID), eq("$2a$10$newHash"), any());
            then(userMapper).should().insertPasswordHistory(USER_ID, "$2a$10$newHash");
            then(userMapper).should().deleteOldPasswordHistories(USER_ID);
            then(userMapper).should().revokeAllSessions(USER_ID);
        }

        @Test
        @DisplayName("[실패] 새 비밀번호 확인 불일치 → PASSWORD_CONFIRM_MISMATCH (DB 조회 없음)")
        void 실패_확인불일치() {
            assertThatThrownBy(() ->
                    userService.changePassword(USER_ID, pwChangeReq(CURRENT, NEW_PW, "WrongConfirm!")))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.PASSWORD_CONFIRM_MISMATCH));

            then(userMapper).should(never()).findById(any());
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 userId → USER_NOT_FOUND")
        void 실패_사용자없음() {
            given(userMapper.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    userService.changePassword(USER_ID, pwChangeReq(CURRENT, NEW_PW, NEW_PW)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }

        @Test
        @DisplayName("[실패] 현재 비밀번호 불일치 → INVALID_PASSWORD")
        void 실패_현재비밀번호불일치() {
            given(userMapper.findById(USER_ID)).willReturn(Optional.of(activeUser()));
            given(passwordEncoder.matches("WrongCurrent!", ENC_PW)).willReturn(false);

            assertThatThrownBy(() ->
                    userService.changePassword(USER_ID, pwChangeReq("WrongCurrent!", NEW_PW, NEW_PW)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_PASSWORD));
        }

        @Test
        @DisplayName("[실패] 최근 사용한 비밀번호 재사용 → PASSWORD_RECENTLY_USED")
        void 실패_최근비밀번호재사용() {
            given(userMapper.findById(USER_ID)).willReturn(Optional.of(activeUser()));
            given(passwordEncoder.matches(CURRENT, ENC_PW)).willReturn(true);
            given(userMapper.findRecentPasswordHashes(USER_ID, 5))
                    .willReturn(List.of("$2a$old1", "$2a$old2", "$2a$old3"));
            // 새 비밀번호가 이전 이력 중 하나와 일치
            given(passwordEncoder.matches(eq(NEW_PW), anyString())).willReturn(true);

            assertThatThrownBy(() ->
                    userService.changePassword(USER_ID, pwChangeReq(CURRENT, NEW_PW, NEW_PW)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.PASSWORD_RECENTLY_USED));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // RQ-F17 | 보유 카드 및 신청 현황
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("보유 카드 및 신청 현황 [getMyCards]")
    class GetMyCards {

        @Test
        @DisplayName("[정상] ownedCards·applications 목록 포함 CardStatusResponse 반환")
        void 정상_카드현황조회() {
            given(userMapper.selectOwnedCards(USER_ID)).willReturn(Collections.emptyList());
            given(userMapper.selectCardApplications(USER_ID)).willReturn(Collections.emptyList());

            CardStatusResponse response = userService.getMyCards(USER_ID);

            assertThat(response).isNotNull();
            assertThat(response.getOwnedCards()).isNotNull();
            assertThat(response.getApplications()).isNotNull();
        }

        @Test
        @DisplayName("[정상] 발급 카드 1건 + 신청 이력 1건 포함 응답")
        void 정상_카드1건() {
            OwnedCardRow ownedRow = new OwnedCardRow();
            ReflectionTestUtils.setField(ownedRow, "userCardId", 10L);
            ReflectionTestUtils.setField(ownedRow, "cardId",     1L);
            ReflectionTestUtils.setField(ownedRow, "cardName",   "BNK카드");

            CardApplicationRow appRow = new CardApplicationRow();
            ReflectionTestUtils.setField(appRow, "applicationId",    100L);
            ReflectionTestUtils.setField(appRow, "applicationStatus","ISSUED");

            given(userMapper.selectOwnedCards(USER_ID)).willReturn(List.of(ownedRow));
            given(userMapper.selectCardApplications(USER_ID)).willReturn(List.of(appRow));

            CardStatusResponse response = userService.getMyCards(USER_ID);

            assertThat(response.getOwnedCards()).hasSize(1);
            assertThat(response.getApplications()).hasSize(1);
        }
    }
}
