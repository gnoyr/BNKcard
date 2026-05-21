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
import com.bnk.domain.auth.dto.request.SendVerifyCodeRequest;
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

    // 인증코드 저장 키 : 코드 검증용 (10분 TTL)
    private static final String KEY_VERIFY_CODE = "email:verify:";
    // 인증 완료 플래그 키 : 회원가입 허용 증표 (30분 TTL)
    private static final String KEY_VERIFIED    = "email:verified:";
    // 비밀번호 재설정 토큰 키 (30분 TTL)
    private static final String KEY_PW_RESET    = "pw:reset:";

    private static final int  MAX_LOGIN_FAIL         = 5;
    private static final long LOCK_DURATION_MIN       = 30;
    private static final long CODE_TTL_MIN            = 10;
    private static final long VERIFIED_TTL_MIN        = 30;
    private static final long PW_RESET_TTL_MIN        = 30;

    // ──────────────────────────────────────────────────────────────────
    // F-00 | 이메일 인증코드 발송
    //        - 이미 가입된 이메일이면 거부
    //        - 6자리 코드 생성 → 메모리 저장(10분) → 이메일 발송
    // ──────────────────────────────────────────────────────────────────
    public void sendVerifyCode(SendVerifyCodeRequest request) {

    	if (userMapper.existsByEmail(request.getEmail()) > 0) {
    	    throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
    	}
    	
        String code = UUID.randomUUID().toString()
                .replaceAll("-", "").substring(0, 6).toUpperCase();

        tokenStore.set(KEY_VERIFY_CODE + request.getEmail(), code, CODE_TTL_MIN);
        emailService.sendVerificationEmail(request.getEmail(), code);

        log.info("[인증코드발송] email={}", request.getEmail());
    }

    // ──────────────────────────────────────────────────────────────────
    // F-02 | 이메일 인증코드 확인
    //        - 코드 일치 시 → 코드 삭제 + 인증완료 플래그 저장(30분)
    //        - 이후 signup() 에서 플래그를 확인해 DB 반영
    // ──────────────────────────────────────────────────────────────────
    public void verifyEmail(EmailVerifyRequest request) {

        String codeKey  = KEY_VERIFY_CODE + request.getEmail();
        String savedCode = tokenStore.get(codeKey);

        if (savedCode == null || !savedCode.equals(request.getCode())) {
            throw new BusinessException(ErrorCode.VERIFY_TOKEN_INVALID);
        }

        // 코드 삭제 + 인증완료 플래그 저장
        tokenStore.delete(codeKey);
        tokenStore.set(KEY_VERIFIED + request.getEmail(), "Y", VERIFIED_TTL_MIN);

        log.info("[이메일인증] 완료: email={}", request.getEmail());
    }

    // ──────────────────────────────────────────────────────────────────
    // F-01 | 회원가입
    //        - 인증완료 플래그 필수 → 없으면 400
    //        - DB INSERT (is_email_verified = 'N' 기본값)
    //        - updateEmailVerified('Y') 로 즉시 업데이트
    //        - 약관 동의 저장
    //        - 인증완료 플래그 삭제
    // ──────────────────────────────────────────────────────────────────
    @Transactional
    public Long signup(SignupRequest request) {

        // 중복 체크
    	if (userMapper.existsByEmail(request.getEmail()) > 0) {
    	    throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
    	}
    	if (userMapper.existsByPhone(request.getPhone()) > 0) {
    	    throw new BusinessException(ErrorCode.DUPLICATE_PHONE);
    	}
        // 이메일 인증 완료 여부 확인
        if (tokenStore.get(KEY_VERIFIED + request.getEmail()) == null) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        // 필수 약관 체크
        List<Terms> signupTerms = termsMapper.findByPackageType("SIGNUP");
        Set<Long> requiredIds = signupTerms.stream()
                .filter(t -> "Y".equals(t.getRequiredYn()))
                .map(Terms::getTermsId)
                .collect(Collectors.toSet());

        if (!request.getAgreedTermsIds().containsAll(requiredIds)) {
            throw new BusinessException(ErrorCode.REQUIRED_TERMS_NOT_AGREED);
        }

        LocalDate birthDate = null;
        if (request.getBirthDate() != null && !request.getBirthDate().isBlank()) {
            birthDate = LocalDate.parse(request.getBirthDate(), DateTimeFormatter.BASIC_ISO_DATE);
        }

        // 사용자 INSERT (is_email_verified = 'N' 으로 들어감)
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

        // is_email_verified = 'Y' 로 즉시 업데이트
        userMapper.updateEmailVerified(user.getUserId(), "Y");

        // 약관 동의 저장
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

        // 인증완료 플래그 제거
        tokenStore.delete(KEY_VERIFIED + request.getEmail());

        log.info("[회원가입] 완료: userId={}, email={}", user.getUserId(), request.getEmail());
        return user.getUserId();
    }

    // ──────────────────────────────────────────────────────────────────
    // F-03 | 로그인
    // ──────────────────────────────────────────────────────────────────
    // noRollbackFor: BusinessException(RuntimeException) 발생 시에도
    // incrementLoginFailCount / updateLockedUntil 이 롤백되지 않도록 보장
    @Transactional(noRollbackFor = BusinessException.class)
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

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUserId(), "ROLE_USER");
        return buildAuthCookies(accessToken, user.getUserId(), request.getDeviceInfo());
    }

    // ──────────────────────────────────────────────────────────────────
    // F-04 | Access Token 재발급
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
    // F-05 | 로그아웃
    // ──────────────────────────────────────────────────────────────────
    @Transactional
    public void logout(Long userId) {
        userSessionMapper.revokeAllByUserId(userId);
        log.info("[로그아웃] userId={}", userId);
    }

    // ──────────────────────────────────────────────────────────────────
    // F-21 | 아이디 찾기
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
    // F-22 | 비밀번호 재설정 요청
    // ──────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public void findPassword(FindPasswordRequest request) {

        User user = userMapper.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!user.getName().equals(request.getName())) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        String resetToken = UUID.randomUUID().toString();
        tokenStore.set(KEY_PW_RESET + resetToken, user.getEmail(), PW_RESET_TTL_MIN);
        emailService.sendPasswordResetEmail(user.getEmail(), resetToken);

        log.info("[비밀번호찾기] 재설정 링크 발송: userId={}", user.getUserId());
    }

    // ──────────────────────────────────────────────────────────────────
    // F-23 | 비밀번호 재설정
    // ──────────────────────────────────────────────────────────────────
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {

        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        String key   = KEY_PW_RESET + request.getToken();
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
    @Transactional(noRollbackFor = BusinessException.class)
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
    // private helpers
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