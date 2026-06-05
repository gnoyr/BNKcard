package com.bnk.domain.auth.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bnk.domain.admin.mapper.AdminUserMapper;
import com.bnk.domain.application.service.CddService;
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
import com.bnk.global.util.CiValueGenerator;
import com.bnk.global.util.CookieUtil;
import com.bnk.global.util.MaskingUtil;
import com.bnk.global.util.TokenStore;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserMapper userMapper;
	private final AdminUserMapper adminUserMapper;
	private final UserSessionMapper userSessionMapper;
	private final TermsMapper termsMapper;
	private final UserTermsAgreementMapper userTermsAgreementMapper;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final CookieUtil cookieUtil;
	private final TokenStore tokenStore;
	private final EmailService mailService;
	private final CiValueGenerator ciValueGenerator;
	private final CddService cddService;

	// ──────────────────────────────────────────────────────────────────
	// KEY_VERIFY : 인증코드 임시 저장 "email:verify:{email}"
	// KEY_VERIFIED : 인증 완료 플래그 "email:verified:{email}"
	// KEY_RESET : 비밀번호 재설정 "pw:reset:{token}"
	// ──────────────────────────────────────────────────────────────────
	private static final String KEY_VERIFY = "email:verify:";
	private static final String KEY_VERIFIED = "email:verified:";
	private static final String KEY_RESET = "pw:reset:";

	private static final long TTL_VERIFY_CODE_SEC = 600L; // 10분 (이메일 본문 안내와 일치)
	private static final long TTL_VERIFIED_FLAG_SEC = 1800L; // 30분 (회원가입 완료 전까지 유효)
	private static final long TTL_PW_RESET_SEC = 1800L; // 30분 (이메일 본문 안내와 일치)

	private static final int MAX_LOGIN_FAIL = 5;
	private static final int LOCK_DURATION_MIN = 30;

	// ──────────────────────────────────────────────────────────────────
	// F-01 | 이메일 인증코드 발송
	// ──────────────────────────────────────────────────────────────────
	@Transactional(readOnly = true)
	public void sendVerifyCode(SendVerifyCodeRequest request) {
		if (userMapper.existsByEmail(request.getEmail()) > 0)
			throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);

		String code = generateCode();

		tokenStore.set(KEY_VERIFY + request.getEmail(), code, TTL_VERIFY_CODE_SEC);
		mailService.sendVerificationEmail(request.getEmail(), code);
		log.info("[인증코드발송] email={}", request.getEmail());
	}

	// ──────────────────────────────────────────────────────────────────
	// F-02 | 이메일 인증코드 확인
	// ──────────────────────────────────────────────────────────────────
	@Transactional
	public void verifyEmail(EmailVerifyRequest request) {
		// KEY_VERIFY 키에서 인증코드 조회
		String stored = tokenStore.get(KEY_VERIFY + request.getEmail());
		if (stored == null || !stored.equals(request.getCode()))
			throw new BusinessException(ErrorCode.VERIFY_TOKEN_INVALID);

		// 사용된 인증코드 삭제 후 인증 완료 플래그를 별도 KEY_VERIFIED 키에 저장
		tokenStore.delete(KEY_VERIFY + request.getEmail());
		tokenStore.set(KEY_VERIFIED + request.getEmail(), "Y", TTL_VERIFIED_FLAG_SEC);
		log.info("[이메일인증완료] email={}", request.getEmail());
	}

	// ──────────────────────────────────────────────────────────────────
	// F-03 | 회원가입
	// ──────────────────────────────────────────────────────────────────
	@Transactional
    public Long signup(SignupRequest request) {
 
        // ① 이메일 중복
        if (userMapper.existsByEmail(request.getEmail()) > 0)
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
 
        // ② phone 중복 (암호화로 인해 전체 조회 후 비교)
        String formattedPhone = MaskingUtil.formatPhone(request.getPhone());
        boolean phoneExists = userMapper.findAllPhones().stream()
                .anyMatch(u -> formattedPhone.equals(u.getPhone()));
        if (phoneExists)
            throw new BusinessException(ErrorCode.DUPLICATE_PHONE);
 
        // ③ 이메일 인증 확인
        if (tokenStore.get(KEY_VERIFIED + request.getEmail()) == null)
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
 
        // ④ 필수 약관 체크
        List<Terms> signupTerms = termsMapper.findByPackageType("SIGNUP");
        Set<Long> requiredIds = signupTerms.stream()
                .filter(t -> "Y".equals(t.getRequiredYn()))
                .map(Terms::getTermsId)
                .collect(Collectors.toSet());
        if (!request.getAgreedTermsIds().containsAll(requiredIds))
            throw new BusinessException(ErrorCode.REQUIRED_TERMS_NOT_AGREED);
 
        // ⑤ birthDate 파싱
        LocalDate birthDate = null;
        String birthDateStr = null;
        if (request.getBirthDate() != null && !request.getBirthDate().isBlank()) {
            String raw = request.getBirthDate().replace("-", "");
            birthDate    = LocalDate.parse(raw, DateTimeFormatter.BASIC_ISO_DATE);
            birthDateStr = birthDate.toString();  // "YYYY-MM-DD"
        }
 
        // ⑥ CI값 생성 (이름 + 생년월일 + 전화번호 조합 → SHA-256 Mock CI)
        String ciValue = ciValueGenerator.generate(
                request.getName(), birthDateStr, formattedPhone);
 
        // ⑦ Watchlist 대조 (미가입 요주의 인물 차단)
        cddService.checkWatchlist(ciValue, request.getName(), birthDateStr);
 
        // ⑧ marketingAgree 변환
        String marketingAgreeYN = Boolean.TRUE.equals(request.getMarketingAgree()) ? "Y" : "N";
 
        // ⑨ User INSERT
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phone(formattedPhone)
                .birthDate(birthDate)
                .ciValue(ciValue)           // CI값 저장 (AES 암호화는 TypeHandler가 자동 처리)
                .job(request.getJob())
                .incomeLevelCode(request.getIncomeLevelCode())
                .creditScore(request.getCreditScore())
                .marketingAgree(marketingAgreeYN)
                .build();
 
        userMapper.insertUser(user);
 
        // ⑩ 이메일 인증 완료 처리
        userMapper.updateEmailVerified(user.getUserId(), "Y");
 
        // ⑪ CDD 초기화 (회원가입 = SIMPLE CDD 통과 → VERIFIED)
        cddService.initializeCdd(user.getUserId());
 
        // ⑫ 약관 동의 저장
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
                        .agreedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                        .build())
                .collect(Collectors.toList());
 
        if (!agreements.isEmpty()) {
            userTermsAgreementMapper.insertAgreements(agreements);
        }
 
        tokenStore.delete(KEY_VERIFIED + request.getEmail());
 
        log.info("[회원가입] 완료: userId={}, email={}", user.getUserId(), request.getEmail());
        return user.getUserId();
    }

	/**
	 * F-04 | 사용자 로그인
	 *
	 * [중요] @Transactional(noRollbackFor = BusinessException.class)
	 * 로그인 실패 시 incrementLoginFailCount() DB 쓰기가 예외 이후에도
	 * 반드시 커밋되어야 하므로 BusinessException 발생 시 롤백을 허용하지 않음.
	 * 이 선언을 제거하거나 readOnly = true 로 변경하면
	 * 로그인 실패 횟수가 항상 0으로 초기화되어 잠금 기능이 동작하지 않음.
	 */
	@Transactional(noRollbackFor = BusinessException.class)
	public AuthTokenResult login(LoginRequest request, HttpServletRequest httpRequest) {
		String ipAddress = resolveClientIp(httpRequest);
		String userAgent = httpRequest.getHeader("User-Agent");

		User user = userMapper.findByEmail(request.getEmail())
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		validateUserStatus(user);

		if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
			handleLoginFail(user);
			adminUserMapper.insertLoginHistory("USER", user.getUserId(), "FAIL", "\ube44\ubc00\ubc88\ud638 \ubd88\uc77c\uce58",
					ipAddress, request.getDeviceInfo(), userAgent);
			throw new BusinessException(ErrorCode.INVALID_PASSWORD);
		}

		if (user.getLoginFailCount() > 0)
			userMapper.resetLoginFailCount(user.getUserId());

		adminUserMapper.insertLoginHistory("USER", user.getUserId(), "SUCCESS", null,
				ipAddress, request.getDeviceInfo(), userAgent);
		userMapper.updateLastLoginAt(user.getUserId(), LocalDateTime.now(ZoneId.of("Asia/Seoul")));

		String accessToken = jwtTokenProvider.generateAccessToken(user.getUserId(), "ROLE_USER");
		
		return buildAuthCookies(accessToken, user.getUserId(), request.getDeviceInfo(), ipAddress, userAgent);
	}

	// ──────────────────────────────────────────────────────────────────
	// F-05 | Access Token 재발급
	// ──────────────────────────────────────────────────────────────────
	@Transactional(readOnly = true)
	public ResponseCookie refresh(String refreshToken) {
		UserSession session = userSessionMapper.findByRefreshToken(refreshToken)
				.orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));

		if (session.getExpiresAt().isBefore(LocalDateTime.now()))
			throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);

		String newAccessToken = jwtTokenProvider.generateAccessToken(session.getUserId(), "ROLE_USER");
		return cookieUtil.createAccessCookie(newAccessToken, jwtTokenProvider.getAccessExpirationSec());
	}

	// ──────────────────────────────────────────────────────────────────
	// F-06 | 로그아웃
	// ──────────────────────────────────────────────────────────────────
	@Transactional
	public void logout(Long userId) {
		userSessionMapper.revokeAllByUserId(userId);
		log.info("[로그아웃] userId={}", userId);
	}

	// ──────────────────────────────────────────────────────────────────
	// F-20 | 아이디 찾기
	// ──────────────────────────────────────────────────────────────────
	@Transactional(readOnly = true)
    public FindIdResponse findId(FindIdRequest request) {
        String formattedPhone = MaskingUtil.formatPhone(request.getPhone());
 
        List<User> candidates = userMapper.findByName(request.getName());
 
        User user = candidates.stream()
            .filter(u -> u.getPhone() != null
                      && formattedPhone.equals(u.getPhone()))
            .findFirst()
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
 
        return FindIdResponse.builder()
            .maskedEmail(MaskingUtil.maskEmail(user.getEmail()))
            .message("이메일 조회 완료")
            .build();
    }

	// ──────────────────────────────────────────────────────────────────
	// F-22 | 비밀번호 재설정 링크 발송
	// ──────────────────────────────────────────────────────────────────
	@Transactional(readOnly = true)
	public void findPassword(FindPasswordRequest request) {
		User user = userMapper.findByEmailAndName(request.getEmail(), request.getName())
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		String token = generateCode() + generateCode();
		
		tokenStore.set(KEY_RESET + token, String.valueOf(user.getUserId()), TTL_PW_RESET_SEC);
		mailService.sendPasswordResetEmail(user.getEmail(), token);
		log.info("[비밀번호재설정링크발송] userId={}", user.getUserId());
	}

	// ──────────────────────────────────────────────────────────────────
	// F-23 | 비밀번호 재설정
	// ──────────────────────────────────────────────────────────────────
	@Transactional
	public void resetPassword(ResetPasswordRequest request) {
		if (!request.getNewPassword().equals(request.getNewPasswordConfirm()))
			throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);

		String userIdStr = tokenStore.get(KEY_RESET + request.getToken());
		if (userIdStr == null)
			throw new BusinessException(ErrorCode.VERIFY_TOKEN_INVALID);

		Long userId = Long.parseLong(userIdStr);
		userMapper.updatePassword(userId,
				passwordEncoder.encode(request.getNewPassword()),
				LocalDateTime.now());
		userMapper.revokeAllSessions(userId);
		tokenStore.delete(KEY_RESET + request.getToken());
		log.info("[비밀번호재설정] userId={}", userId);
	}

	// ──────────────────────────────────────────────────────────────────
	// 관리자 로그인
	// ──────────────────────────────────────────────────────────────────
	@Transactional(noRollbackFor = BusinessException.class)
	public AuthTokenResult adminLogin(AdminLoginRequest request, HttpServletRequest httpRequest) {
		String ipAddress = resolveClientIp(httpRequest);
		String userAgent = httpRequest.getHeader("User-Agent");

		com.bnk.domain.admin.model.AdminUser admin =
				adminUserMapper.findByUsername(request.getUsername())
						.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash()))
			throw new BusinessException(ErrorCode.INVALID_PASSWORD);

		adminUserMapper.updateLastLoginAt(admin.getAdminId(), LocalDateTime.now());

		String roles = (admin.getRoleCodes() != null && !admin.getRoleCodes().isEmpty())
				? String.join(",", admin.getRoleCodes())
				: "ADMIN";

		String accessToken = jwtTokenProvider.generateAdminAccessToken(admin.getAdminId(), roles);
		
		return buildAuthCookies(accessToken, admin.getAdminId(), null, ipAddress, userAgent);
	}

	// ──────────────────────────────────────────────────────────────────
	// private helpers
	// ──────────────────────────────────────────────────────────────────

	private AuthTokenResult buildAuthCookies(String accessToken, Long userId,
			String deviceInfo, String ipAddress, String userAgent) {
		String refreshToken = jwtTokenProvider.generateRefreshToken(userId);
		long refreshExpSec  = jwtTokenProvider.getRefreshExpirationSec();

		userSessionMapper.insertSession(UserSession.builder()
				.userId(userId)
				.refreshToken(refreshToken)
				.deviceInfo(deviceInfo)
				.ipAddress(ipAddress)
				.userAgent(userAgent)
				.expiresAt(LocalDateTime.now().plusSeconds(refreshExpSec))
				.build());

		AuthTokenResult result = new AuthTokenResult();
		result.setAccessCookie(cookieUtil.createAccessCookie(accessToken, jwtTokenProvider.getAccessExpirationSec()));
		result.setRefreshCookie(cookieUtil.createRefreshCookie(refreshToken, refreshExpSec));
		return result;
	}

	private void validateUserStatus(User user) {
		if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now()))
			throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
		String status = user.getStatusCode();
		if ("SUSPENDED".equals(status))
			throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
		if ("WITHDRAWN".equals(status))
			throw new BusinessException(ErrorCode.ACCOUNT_WITHDRAWN);
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

	private String generateCode() {
		return String.format("%06d", (int) (Math.random() * 1_000_000));
	}

	private String resolveClientIp(HttpServletRequest request) {
		String xForwardedFor = request.getHeader("X-Forwarded-For");
		if (xForwardedFor != null && !xForwardedFor.isBlank())
			return xForwardedFor.split(",")[0].trim();
		return request.getRemoteAddr();
	}
}
