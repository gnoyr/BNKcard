package com.bnk.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.bnk.domain.user.dto.request.PasswordChangeRequest;
import com.bnk.domain.user.dto.request.UserUpdateRequest;
import com.bnk.domain.user.dto.response.CardStatusResponse;
import com.bnk.domain.user.dto.response.UserResponse;
import com.bnk.domain.user.mapper.UserMapper;
import com.bnk.domain.user.model.User;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 단위 테스트")
class UserServiceTest {

    @Mock private UserMapper      userMapper;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    // ── 공통 상수 ──────────────────────────────────────────────────
    private static final Long   USER_ID = 1L;
    private static final String CURRENT = "Current123!";
    private static final String NEW_PW  = "NewSecure123!";
    private static final String ENC_PW  = "$2a$10$encodedPasswordHashValue";

    // ── 픽스처 ────────────────────────────────────────────────────

    /** getMyInfo / updateMyInfo에서 MaskingUtil 정상 동작에 필요한 필드 포함 */
    private User activeUser() {
        User user = new User();
        ReflectionTestUtils.setField(user, "userId",       USER_ID);
        // [수정] "password" → "passwordHash" (User 모델의 실제 필드명)
        ReflectionTestUtils.setField(user, "passwordHash", ENC_PW);
        ReflectionTestUtils.setField(user, "statusCode",   "ACTIVE");
        ReflectionTestUtils.setField(user, "email",        "test@bnk.co.kr");
        ReflectionTestUtils.setField(user, "name",         "홍길동");
        ReflectionTestUtils.setField(user, "phone",        "010-1234-5678");
        return user;
    }

    private PasswordChangeRequest pwChangeReq(String current, String newPw, String confirm) {
        PasswordChangeRequest req = new PasswordChangeRequest();
        ReflectionTestUtils.setField(req, "currentPassword",    current);
        ReflectionTestUtils.setField(req, "newPassword",        newPw);
        ReflectionTestUtils.setField(req, "newPasswordConfirm", confirm);
        return req;
    }

    /**
     * @param name        null이면 해당 필드 미설정
     * @param phone       null이면 phone 변경 없음 → currentPassword 검증 생략
     * @param currentPw   phone 변경 시 현재 비밀번호 확인용
     */
    private UserUpdateRequest updateReq(String name, String phone, String currentPw) {
        UserUpdateRequest req = new UserUpdateRequest();
        if (name      != null) ReflectionTestUtils.setField(req, "name",            name);
        if (phone     != null) ReflectionTestUtils.setField(req, "phone",           phone);
        if (currentPw != null) ReflectionTestUtils.setField(req, "currentPassword", currentPw);
        return req;
    }

    // ════════════════════════════════════════════════════════════════
    // [신규] F-24 | 내 정보 조회
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("내 정보 조회")
    class GetMyInfo {

        @Test
        @DisplayName("[정상] userId로 유저 조회 → UserResponse 반환")
        void 정상_내정보조회() {
            given(userMapper.findById(USER_ID)).willReturn(Optional.of(activeUser()));

            UserResponse response = userService.getMyInfo(USER_ID);

            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(USER_ID);
            // 이메일·전화번호는 마스킹 처리됨
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
    // [신규] F-25 | 내 정보 수정
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("내 정보 수정")
    class UpdateMyInfo {

        @Test
        @DisplayName("[정상] phone 미포함 수정 → 비밀번호 검증 없이 updateUser 호출")
        void 정상_phone없이수정() {
            given(userMapper.findById(USER_ID)).willReturn(Optional.of(activeUser()));

            userService.updateMyInfo(USER_ID, updateReq("이순신", null, null));

            then(userMapper).should().updateUser(any());
            then(passwordEncoder).should(never()).matches(any(), any());
        }

        @Test
        @DisplayName("[정상] phone 변경 + 비밀번호 검증 통과 → updateUser 호출")
        void 정상_phone변경_비밀번호검증통과() {
            given(userMapper.findById(USER_ID)).willReturn(Optional.of(activeUser()));
            given(passwordEncoder.matches(CURRENT, ENC_PW)).willReturn(true);

            userService.updateMyInfo(USER_ID, updateReq("이순신", "01099998888", CURRENT));

            then(userMapper).should().updateUser(any());
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 userId → USER_NOT_FOUND")
        void 실패_사용자없음() {
            given(userMapper.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateMyInfo(USER_ID, updateReq("이순신", null, null)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }

        @Test
        @DisplayName("[실패] phone 변경 요청 + currentPassword 누락 → INVALID_INPUT")
        void 실패_phone변경_비밀번호누락() {
            given(userMapper.findById(USER_ID)).willReturn(Optional.of(activeUser()));

            // phone이 있는데 currentPassword를 null로 넘김
            assertThatThrownBy(() -> userService.updateMyInfo(USER_ID, updateReq(null, "01099998888", null)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_INPUT));
        }

        @Test
        @DisplayName("[실패] phone 변경 + 현재 비밀번호 불일치 → INVALID_PASSWORD")
        void 실패_phone변경_비밀번호불일치() {
            given(userMapper.findById(USER_ID)).willReturn(Optional.of(activeUser()));
            given(passwordEncoder.matches("WrongPw!", ENC_PW)).willReturn(false);

            assertThatThrownBy(() -> userService.updateMyInfo(USER_ID, updateReq(null, "01099998888", "WrongPw!")))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_PASSWORD));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // [기존 + 보완] F-26 | 비밀번호 변경
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("비밀번호 변경")
    class ChangePassword {

        @Test
        @DisplayName("[정상] 검증 통과 → updatePassword + revokeAllSessions 호출")
        void 정상_변경성공() {
            given(userMapper.findById(USER_ID)).willReturn(Optional.of(activeUser()));
            given(passwordEncoder.matches(CURRENT, ENC_PW)).willReturn(true);
            given(passwordEncoder.encode(NEW_PW)).willReturn("$2a$10$newHash");

            userService.changePassword(USER_ID, pwChangeReq(CURRENT, NEW_PW, NEW_PW));

            then(userMapper).should().updatePassword(eq(USER_ID), eq("$2a$10$newHash"), any());
            // [신규] 비밀번호 변경 후 전 기기 세션 파기 검증
            then(userMapper).should().revokeAllSessions(USER_ID);
        }

        @Test
        @DisplayName("[실패] 새 비밀번호 확인 불일치 → PASSWORD_CONFIRM_MISMATCH")
        void 실패_확인불일치() {
            assertThatThrownBy(() ->
                    userService.changePassword(USER_ID, pwChangeReq(CURRENT, NEW_PW, "WrongConfirm!")))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.PASSWORD_CONFIRM_MISMATCH));

            then(userMapper).should(never()).findById(any());
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
        @DisplayName("[실패] 존재하지 않는 유저 → USER_NOT_FOUND")
        void 실패_사용자없음() {
            given(userMapper.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    userService.changePassword(USER_ID, pwChangeReq(CURRENT, NEW_PW, NEW_PW)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // [신규] RQ-F17 | 보유 카드 및 신청 현황
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("보유 카드 및 신청 현황")
    class GetMyCards {

        @Test
        @DisplayName("[정상] 보유 카드·신청 이력 정상 반환 (빈 리스트도 포함)")
        void 정상_카드현황조회() {
            // selectOwnedCards / selectCardApplications → 빈 리스트 반환
            given(userMapper.selectOwnedCards(USER_ID)).willReturn(Collections.emptyList());
            given(userMapper.selectCardApplications(USER_ID)).willReturn(Collections.emptyList());

            CardStatusResponse response = userService.getMyCards(USER_ID);

            assertThat(response).isNotNull();
            assertThat(response.getOwnedCards()).isEmpty();
            assertThat(response.getApplications()).isEmpty();
        }
    }
}
