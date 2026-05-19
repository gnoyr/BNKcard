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
import com.bnk.domain.auth.mapper.UserSessionMapper;
import com.bnk.domain.auth.model.UserSession;
import com.bnk.domain.terms.mapper.TermsMapper;
import com.bnk.domain.terms.mapper.UserTermsAgreementMapper;
import com.bnk.domain.terms.model.Terms;
import com.bnk.domain.terms.model.UserTermsAgreement;
import com.bnk.domain.user.mapper.UserMapper;
import com.bnk.domain.user.model.User;
import com.bnk.global.auth.JwtTokenProvider;
import com.bnk.global.email.EmailService;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.util.CookieUtil;
import com.bnk.global.util.MaskingUtil;
import com.bnk.global.util.MemoryTokenStore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 인증/회원 서비스
 * 담당: 회원가입(F-01), 이메일인증(F-02), 로그인(F-03), 토큰재발급(F-04),
 *       로그아웃(F-05), 아이디찾기(F-21), 비밀번호재설정요청(F-22), 비밀번호재설정(F-23),
 *       관리자로그인(B-01)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final UserSessionMapper userSessionMapper;
    private final UserTermsAgreementMapper userTermsAgreementMapper;
    private final TermsMapper termsMapper;
    private final AdminUserMapper adminUserMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtil cookieUtil;
    private final PasswordEncoder passwordEncoder;
    private final MemoryTokenStore tokenStore;
    private final EmailService emailService;

    private static final String MEMORY_EMAIL_VERIFY  = "email:verify:";
    private static final String MEMORY_PW_RESET      = "pw:reset:";

    private static final int  MAX_LOGIN_FAIL          = 5;
    private static final long LOCK_DURATION_MIN        = 30;
    private static final long EMAIL_TOKEN_TTL_MIN      = 10;
    private static final long PW_RESET_TOKEN_TTL_MIN   = 30;

    // ──────────────────────────────────────────────────────────────────
    // F-01 | 회원가입 — 계정 생성 → 약관 저장 → 인증 이메일 발송
    // ──────────────────────────────────────────────────────────────────
    @Transactional
    public Long signup(SignupRequest request) {

        userMapper.findByEmail(request.getEmail())
                .ifPresent(u -> { throw new BusinessException(ErrorCode.DUPLICATE_EMAIL); });

        List<Terms> signupTerms = termsMapper.findByPackageType("SIGNUP");
        Set<Long> requiredTermsIds = signupTerms.stream()
                .filter(t -> "Y".equals(t.getRequiredYn()))
                .map(Terms::getTermsId)
                .collect(Collectors.toSet());

        if (!request.getAgreedTermsIds().containsAll(requiredTermsIds)) {
            throw new BusinessException(ErrorCode.REQUIRED_TERMS_NOT_AGREED);
        }

        LocalDate birthDate = null;
        if (request.getBirthDate() != null && !request.getBirthDate().isBlank()) {
            birthDate = LocalDate.parse(request.getBirthDate(), DateTimeFormatter.BASIC_ISO_DATE);
        }

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

        // 6자리 인증코드 생성 → 메모리 저장(10분) → 이메일 발송
        String verifyCode = UUID.randomUUID().toString()
                .replaceAll("-", "").substring(0, 6).toUpperCase();
        tokenStore.set(MEMORY_EMAIL_VERIFY + request.getEmail(), verifyCode, EMAIL_TOKEN_TTL_MIN);
        emailService.sendVerificationEmail(request.getEmail(), verifyCode);

        log.info("[회원가입] 이메일 인증코드 발송: userId={}", user.getUserId());
        return user.getUserId();
    }

    // ──────────────────────────────────────────────────────────────────
    // F-02 | 이메일 인증 — 코드 검증 → is_email_verified = 'Y'
    // ──────────────────────────────────────────────────────────────────
    @Transactional
    public void verifyEmail(EmailVerifyRequest request) {

        String key       = MEMORY_EMAIL_VERIFY + request.getEmail();
        String savedCode = tokenStore.get(key);

        if (savedCode == null || !savedCode.equals(request.getToken())) {
            throw new BusinessException(ErrorCode.VERIFY_TOKEN_INVALID);
        }

        User user = userMapper.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        userMapper.updateEmailVerified(user.getUserId(), "Y");
        tokenStore.delete(key);

        log.info("[이메일인증] 완료: userId={}", user.getUserId());
    }

    // ──────────────────────────────────────────────────────────────────
    // F-03 | 로그인 → Access + Refresh 쿠키 발급
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

        if (user.getLoginFailCount() > 0) {
            userMapper.resetLoginFailCount(user.getUserId());
        }

        // User 테이블에 roleCode 컬럼 없음 → 일반 사용자는 ROLE_USER 고정
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUserId(), "ROLE_USER");
        return buildAuthCookies(accessToken, user.getUserId(), request.getDeviceInfo());
    }

    // ──────────────────────────────────────────────────────────────────
    // F-04 | Access Token 재발급 → 새 Access 쿠키만 반환
    // ──────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public ResponseCookie refresh(String refreshToken) {

        UserSession session = userSessionMapper.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));

        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(session.getUserId(), "ROLE_USER");
        return cookieUtil.createAccessCookie(newAccessToken, jwtTokenProvider.getAccessExpirationSec());
    }

    // ──────────────────────────────────────────────────────────────────
    // F-05 | 로그아웃 — DB 세션 revoke (쿠키 삭제는 컨트롤러에서)
    // ──────────────────────────────────────────────────────────────────
    @Transactional
    public void logout(Long userId) {
        userSessionMapper.revokeAllByUserId(userId);
        log.info("[로그아웃] userId={}", userId);
    }

    // ──────────────────────────────────────────────────────────────────
    // F-21 | 아이디(이메일) 찾기
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
    // F-22 | 비밀번호 재설정 요청 — UUID 토큰 발급 → 이메일 발송
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

        String resetToken = UUID.randomUUID().toString();
        tokenStore.set(MEMORY_PW_RESET + resetToken, user.getEmail(), PW_RESET_TOKEN_TTL_MIN);
        emailService.sendPasswordResetEmail(user.getEmail(), resetToken);

        log.info("[비밀번호찾기] 재설정 링크 발송: userId={}", user.getUserId());
    }

    // ──────────────────────────────────────────────────────────────────
    // F-23 | 비밀번호 재설정 — 토큰 검증 → BCrypt → 전세션 revoke
    // ──────────────────────────────────────────────────────────────────
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {

        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        String key   = MEMORY_PW_RESET + request.getToken();
        String email = tokenStore.get(key);

        if (email == null) {
            throw new BusinessException(ErrorCode.VERIFY_TOKEN_INVALID);
        }

        User user = userMapper.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        userMapper.updatePassword(user.getUserId(),
                passwordEncoder.encode(request.getNewPassword()), LocalDateTime.now());

        userSessionMapper.revokeAllByUserId(user.getUserId());
        tokenStore.delete(key);

        log.info("[비밀번호재설정] 완료 — 전세션 무효화: userId={}", user.getUserId());
    }

    // ──────────────────────────────────────────────────────────────────
    // B-01 | 관리자 로그인
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

        String accessToken = jwtTokenProvider.generateAdminAccessToken(admin.getAdminId(), roles);
        return buildAuthCookies(accessToken, admin.getAdminId(), null);
    }

    // ──────────────────────────────────────────────────────────────────
    // private — Access + Refresh 쿠키 생성 및 세션 저장 공통 처리
    // ──────────────────────────────────────────────────────────────────
    private AuthTokenResult buildAuthCookies(String accessToken, Long userId, String deviceInfo) {

        String refreshToken  = jwtTokenProvider.generateRefreshToken(userId);
        long   refreshExpSec = jwtTokenProvider.getRefreshExpirationSec();

        userSessionMapper.insertSession(UserSession.builder()
                .userId(userId)
                .refreshToken(refreshToken)
                .deviceInfo(deviceInfo)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpSec))
                .build());

        AuthTokenResult result = new AuthTokenResult();
        result.setAccessCookie(cookieUtil.createAccessCookie(accessToken, jwtTokenProvider.getAccessExpirationSec()));
        result.setRefreshCookie(cookieUtil.createRefreshCookie(refreshToken, refreshExpSec));
        return result;
    }

    private void validateUserStatus(User user) {
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
        String status = user.getStatusCode();
        if ("SUSPENDED".equals(status)) throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
        if ("WITHDRAWN".equals(status))  throw new BusinessException(ErrorCode.ACCOUNT_WITHDRAWN);
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