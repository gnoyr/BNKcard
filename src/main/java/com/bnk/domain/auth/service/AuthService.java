package com.bnk.domain.auth.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bnk.domain.admin.mapper.AdminUserMapper;
import com.bnk.domain.admin.model.AdminUser;
import com.bnk.domain.auth.dto.request.AdminLoginRequest;
import com.bnk.domain.auth.dto.request.EmailVerifyRequest;
import com.bnk.domain.auth.dto.request.FindIdRequest;
import com.bnk.domain.auth.dto.request.FindPasswordRequest;
import com.bnk.domain.auth.dto.request.LoginRequest;
import com.bnk.domain.auth.dto.request.ResetPasswordRequest;
import com.bnk.domain.auth.dto.request.SignupRequest;
import com.bnk.domain.auth.dto.response.AuthTokenResult;
import com.bnk.domain.auth.dto.response.FindIdResponse;
import com.bnk.domain.auth.dto.response.TokenResponse;
import com.bnk.domain.auth.mapper.UserSessionMapper;
import com.bnk.domain.auth.model.UserSession;
import com.bnk.domain.terms.mapper.TermsMapper;
import com.bnk.domain.terms.mapper.UserTermsAgreementMapper;
import com.bnk.domain.terms.model.Terms;
import com.bnk.domain.terms.model.UserTermsAgreement;
import com.bnk.domain.user.mapper.UserMapper;
import com.bnk.domain.user.model.User;
import com.bnk.global.auth.JwtTokenProvider;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.util.CookieUtil;
import com.bnk.global.util.MaskingUtil;
import com.bnk.global.util.MemoryTokenStore; // 내장 메모리 임포트

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 인증/회원 서비스 — AuthController, AdminAuthController 전용
 * 담당: 회원가입(F-01), 이메일인증(F-02), 로그인(F-03), 토큰재발급(F-04),
 * 로그아웃(F-05), 아이디찾기(F-21), 비밀번호재설정요청(F-22), 비밀번호재설정(F-23),
 * 관리자로그인(B-01)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper               userMapper;
    private final UserSessionMapper        userSessionMapper;
    private final UserTermsAgreementMapper userTermsAgreementMapper;
    private final TermsMapper              termsMapper;
    private final AdminUserMapper          adminUserMapper;
    private final JwtTokenProvider         jwtTokenProvider;
    private final CookieUtil               cookieUtil;
    private final PasswordEncoder          passwordEncoder;
    private final MemoryTokenStore         tokenStore; // RedisTemplate 제거 후 커스텀 저장소 주입

    // 내장 메모리용 키 접두사
    private static final String MEMORY_EMAIL_VERIFY = "email:verify:";
    private static final String MEMORY_PW_RESET     = "pw:reset:";

    // 계정 잠금 기준 및 만료 설정 값
    private static final int  MAX_LOGIN_FAIL     = 5;
    private static final long LOCK_DURATION_MIN  = 30;
    private static final long EMAIL_TOKEN_TTL_MIN = 10;
    private static final long PW_RESET_TOKEN_TTL_MIN = 30;

    // ──────────────────────────────────────────────────────────────────
    // F-01 | 회원가입 (RQ-F05, RQ-F06)
    // USERS INSERT → USER_TERMS_AGREEMENTS 배치 INSERT → 이메일 인증 토큰 발행
    // ──────────────────────────────────────────────────────────────────
    @Transactional
    public Long signup(SignupRequest request) {

        // 이메일 중복 검사 (U002)
        userMapper.findByEmail(request.getEmail())
                .ifPresent(u -> { throw new BusinessException(ErrorCode.DUPLICATE_EMAIL); });

        // 필수 약관 동의 검증 — RQ-F05
        List<Terms> signupTerms = termsMapper.findByPackageType("SIGNUP");
        Set<Long> requiredTermsIds = signupTerms.stream()
                .filter(t -> "Y".equals(t.getRequiredYn()))
                .map(Terms::getTermsId)
                .collect(Collectors.toSet());

        boolean allRequiredAgreed = request.getAgreedTermsIds().containsAll(requiredTermsIds);
        if (!allRequiredAgreed) {
            throw new BusinessException(ErrorCode.REQUIRED_TERMS_NOT_AGREED);
        }

        // 생년월일 변환 yyyyMMdd → LocalDate
        LocalDate birthDate = null;
        if (request.getBirthDate() != null && !request.getBirthDate().isBlank()) {
            birthDate = LocalDate.parse(request.getBirthDate(), DateTimeFormatter.BASIC_ISO_DATE);
        }

        // USERS INSERT
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phone(request.getPhone())
                .birthDate(birthDate)
                .marketingAgree(request.getMarketingAgree() != null ? request.getMarketingAgree() : "N")
                .privacyAgree("Y")
                .build();

        userMapper.insertUser(user);

        // 약관 동의 이력 배치 저장 — USER_TERMS_AGREEMENTS (RQ-F05)
        List<UserTermsAgreement> agreements = signupTerms.stream()
                .filter(t -> request.getAgreedTermsIds().contains(t.getTermsId()))
                .map(t -> UserTermsAgreement.builder()
                        .userId(user.getUserId())
                        .termsId(t.getTermsId())
                        .agreedYn("Y")
                        .agreementAction("AGREE")
                        .agreedVersion(t.getVersion())
                        .agreementChannel("WEB")
                        .agreementSource("SIGNUP")
                        .agreedAt(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());

        if (!agreements.isEmpty()) {
            userTermsAgreementMapper.insertAgreements(agreements);
        }

        // 이메일 인증 토큰 발행 → 로컬 메모리 스토어 TTL 10분 저장 (RQ-F06)
        String verifyToken = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        tokenStore.set(
                MEMORY_EMAIL_VERIFY + request.getEmail(),
                verifyToken,
                EMAIL_TOKEN_TTL_MIN
        );

        log.info("[회원가입] 이메일 인증 토큰 발행 (로컬메모리): email={}, token={}", request.getEmail(), verifyToken);

        return user.getUserId();
    }

    // ──────────────────────────────────────────────────────────────────
    // F-02 | 이메일 인증 (RQ-F06)
    // 메모리 토큰 검증 → USERS.is_email_verified='Y' 업데이트 → 토큰 삭제
    // ──────────────────────────────────────────────────────────────────
    @Transactional
    public void verifyEmail(EmailVerifyRequest request) {

        String key        = MEMORY_EMAIL_VERIFY + request.getEmail();
        String savedToken = tokenStore.get(key);

        // 토큰 불일치 또는 만료 (U008)
        if (savedToken == null || !savedToken.equals(request.getToken())) {
            throw new BusinessException(ErrorCode.VERIFY_TOKEN_INVALID);
        }

        User user = userMapper.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        userMapper.updateEmailVerified(user.getUserId(), "Y");
        tokenStore.delete(key);

        log.info("[이메일인증] 완료: userId={}", user.getUserId());
    }

    // ──────────────────────────────────────────────────────────────────
    // F-03 | 로그인 (RQ-F03)
    // 계정 검증 → BCrypt 비교 → 실패카운트 관리 → JWT 발급 → USER_SESSIONS INSERT
    // ──────────────────────────────────────────────────────────────────
    @Transactional
    public AuthTokenResult login(LoginRequest request) {

        User user = userMapper.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateUserStatus(user);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleLoginFail(user);
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        if ("N".equals(user.getIsEmailVerified())) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        userMapper.resetLoginFailCount(user.getUserId());
        userMapper.updateLastLoginAt(user.getUserId(), LocalDateTime.now());

        return buildTokenResult(user.getUserId(), "ROLE_USER", request.getDeviceInfo());
    }

    // ──────────────────────────────────────────────────────────────────
    // F-04 | Access Token 재발급
    // ──────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public AuthTokenResult refresh(String refreshToken) {

        UserSession session = userSessionMapper.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));

        String newAccessToken = jwtTokenProvider.generateAccessToken(session.getUserId(), "ROLE_USER");

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getRefreshExpirationSec())
                .userId(session.getUserId())
                .role("ROLE_USER")
                .build();

        ResponseCookie cookie = cookieUtil.createRefreshCookie(
                refreshToken, jwtTokenProvider.getRefreshExpirationSec());

        AuthTokenResult result = new AuthTokenResult();
        result.setToken(tokenResponse);
        result.setCookie(cookie);
        return result;
    }

    // ──────────────────────────────────────────────────────────────────
    // F-05 | 로그아웃 (RQ-F05)
    // ──────────────────────────────────────────────────────────────────
    @Transactional
    public ResponseCookie logout(Long userId) {
        userSessionMapper.revokeAllByUserId(userId);
        log.info("[로그아웃] userId={}", userId);
        return cookieUtil.deleteRefreshCookie();
    }

    // ──────────────────────────────────────────────────────────────────
    // F-21 | 아이디(이메일) 찾기 (RQ-F20)
    // ──────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public FindIdResponse findId(FindIdRequest request) {

        User user = userMapper.findByNameAndPhone(request.getName(), request.getPhone())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return FindIdResponse.builder()
                .maskedEmail(MaskingUtil.maskEmail(user.getEmail()))
                .message("이메일 조회 완료")
                .build();
    }

    // ──────────────────────────────────────────────────────────────────
    // F-22 | 비밀번호 재설정 요청 (RQ-F21)
    // 이메일+이름 검증 → 로컬 메모리 UUID 토큰 TTL 30분
    // ──────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public void findPassword(FindPasswordRequest request) {

        User user = userMapper.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!user.getName().equals(request.getName())) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if ("N".equals(user.getIsEmailVerified())) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        // 로컬 메모리 스토어에 UUID 토큰 저장 (TTL 30분)
        String resetToken = UUID.randomUUID().toString();
        tokenStore.set(
                MEMORY_PW_RESET + resetToken,
                user.getEmail(),
                PW_RESET_TOKEN_TTL_MIN
        );

        log.info("[비밀번호찾기] 재설정 링크 발송 (로컬메모리): email={}, token={}", user.getEmail(), resetToken);
    }

    // ──────────────────────────────────────────────────────────────────
    // F-23 | 비밀번호 재설정 (RQ-F22)
    // 메모리 토큰 검증 → BCrypt → USERS.password_hash UPDATE → 전세션 revoke
    // ──────────────────────────────────────────────────────────────────
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {

        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        // 메모리 토큰 검증 및 유효성 확인
        String key   = MEMORY_PW_RESET + request.getToken();
        String email = tokenStore.get(key);

        if (email == null) {
            throw new BusinessException(ErrorCode.VERIFY_TOKEN_INVALID);
        }

        User user = userMapper.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String encodedPw = passwordEncoder.encode(request.getNewPassword());
        userMapper.updatePassword(user.getUserId(), encodedPw, LocalDateTime.now());

        userSessionMapper.revokeAllByUserId(user.getUserId());

        // 사용한 토큰 즉시 제거
        tokenStore.delete(key);

        log.info("[비밀번호재설정] 완료 — 전세션 무효화: userId={}", user.getUserId());
    }

    // ──────────────────────────────────────────────────────────────────
    // B-01 | 관리자 로그인 (RQ-B01)
    // ──────────────────────────────────────────────────────────────────
    @Transactional
    public AuthTokenResult adminLogin(AdminLoginRequest request) {

        AdminUser admin = adminUserMapper.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        if (admin.getLockedUntil() != null && admin.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.ADMIN_ACCOUNT_LOCKED);
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            adminUserMapper.incrementLoginFailCount(admin.getAdminId());
            int newFailCount = admin.getLoginFailCount() + 1;
            if (newFailCount >= MAX_LOGIN_FAIL) {
                LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(LOCK_DURATION_MIN);
                adminUserMapper.updateLockedUntil(admin.getAdminId(), lockUntil);
                log.warn("[관리자로그인] 계정 잠금: adminId={}, until={}", admin.getAdminId(), lockUntil);
            }
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        adminUserMapper.resetLoginFailCount(admin.getAdminId());
        adminUserMapper.updateLastLoginAt(admin.getAdminId(), LocalDateTime.now());

        String roles = admin.getRoleCodes() != null
                ? String.join(",", admin.getRoleCodes())
                : "ADMIN";

        String accessToken  = jwtTokenProvider.generateAdminAccessToken(admin.getAdminId(), roles);
        String refreshToken = jwtTokenProvider.generateRefreshToken(admin.getAdminId());

        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(jwtTokenProvider.getRefreshExpirationSec());
        UserSession session = UserSession.builder()
                .userId(admin.getAdminId())
                .refreshToken(refreshToken)
                .expiresAt(expiresAt)
                .build();
        userSessionMapper.insertSession(session);

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getRefreshExpirationSec())
                .userId(admin.getAdminId())
                .role(roles)
                .build();

        ResponseCookie cookie = cookieUtil.createRefreshCookie(
                refreshToken, jwtTokenProvider.getRefreshExpirationSec());

        AuthTokenResult result = new AuthTokenResult();
        result.setToken(tokenResponse);
        result.setCookie(cookie);
        return result;
    }

    // ──────────────────────────────────────────────────────────────────
    // private 헬퍼 메서드
    // ──────────────────────────────────────────────────────────────────

    private AuthTokenResult buildTokenResult(Long userId, String role, String deviceInfo) {

        String accessToken  = jwtTokenProvider.generateAccessToken(userId, role);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);

        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(jwtTokenProvider.getRefreshExpirationSec());

        UserSession session = UserSession.builder()
                .userId(userId)
                .refreshToken(refreshToken)
                .deviceInfo(deviceInfo)
                .expiresAt(expiresAt)
                .build();
        userSessionMapper.insertSession(session);

        ResponseCookie cookie = cookieUtil.createRefreshCookie(
                refreshToken, jwtTokenProvider.getRefreshExpirationSec());

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getRefreshExpirationSec())
                .userId(userId)
                .role(role)
                .build();

        AuthTokenResult result = new AuthTokenResult();
        result.setToken(tokenResponse);
        result.setCookie(cookie);
        return result;
    }

    private void validateUserStatus(User user) {
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
        String status = user.getStatusCode();
        if ("SUSPENDED".equals(status)) throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
        if ("WITHDRAWN".equals(status)) throw new BusinessException(ErrorCode.ACCOUNT_WITHDRAWN);
    }

    private void handleLoginFail(User user) {
        userMapper.incrementLoginFailCount(user.getUserId());
        int newFailCount = user.getLoginFailCount() + 1;
        if (newFailCount >= MAX_LOGIN_FAIL) {
            LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(LOCK_DURATION_MIN);
            userMapper.updateLockedUntil(user.getUserId(), lockUntil);
            log.warn("[로그인실패] 계정 잠금: userId={}, until={}", user.getUserId(), lockUntil);
        }
    }
}