package com.bnk.domain.auth.service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bnk.domain.admin.mapper.AdminUserMapper;
import com.bnk.domain.admin.model.AdminUser;
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
import com.bnk.domain.auth.mapper.AdminSessionMapper;
import com.bnk.domain.auth.mapper.UserSessionMapper;
import com.bnk.domain.auth.model.AdminSession;
import com.bnk.domain.auth.model.UserSession;
import com.bnk.domain.ipauth.service.IpTrustService;
import com.bnk.domain.terms.mapper.TermsMapper;
import com.bnk.domain.terms.mapper.UserTermsAgreementMapper;
import com.bnk.domain.terms.model.Terms;
import com.bnk.domain.terms.model.UserTermsAgreement;
import com.bnk.domain.user.mapper.UserMapper;
import com.bnk.domain.user.mapper.UserAddressMapper;
import com.bnk.domain.user.model.User;
import com.bnk.domain.user.model.UserAddress;
import com.bnk.global.auth.JwtTokenProvider;
import com.bnk.global.email.EmailService;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.util.CiValueGenerator;
import com.bnk.global.util.ClientIpUtil;
import com.bnk.global.util.CookieUtil;
import com.bnk.global.util.MaskingUtil;
import com.bnk.global.util.TimeConstants;
import com.bnk.global.util.TokenSecurityService;
import com.bnk.global.util.TokenStore;
import com.bnk.global.util.audit.AuditLogger;

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
	private final TokenSecurityService tokenSecurityService;
	private final AuditLogger auditLogger;
	private final IpTrustService ipTrustService;
	private final AdminSessionMapper adminSessionMapper;
	private final UserAddressMapper userAddressMapper;

	// ──────────────────────────────────────────────────────────────────
	// KEY_VERIFY : 인증코드 임시 저장 "email:verify:{email}"
	// KEY_VERIFIED : 인증 완료 플래그 "email:verified:{email}"
	// KEY_RESET : 비밀번호 재설정 "pw:reset:{token}"
	// ──────────────────────────────────────────────────────────────────
	private static final String KEY_VERIFY = "email:verify:";
	private static final String KEY_VERIFIED = "email:verified:";
	private static final String KEY_RESET = "pw:reset:";

	private static final long TTL_VERIFY_CODE_MIN = 10L; // 10분 (이메일 본문 안내와 일치)
	private static final long TTL_VERIFIED_FLAG_MIN = 30L; // 30분 (회원가입 완료 전까지 유효)
	private static final long TTL_PW_RESET_MIN = 30L; // 30분 (이메일 본문 안내와 일치)

	private static final int MAX_LOGIN_FAIL = 5;
	private static final int LOCK_DURATION_MIN = 30;
	
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	// ──────────────────────────────────────────────────────────────────
	// 이메일 인증코드 발송
	// ──────────────────────────────────────────────────────────────────
	@Transactional
	public void sendVerifyCode(SendVerifyCodeRequest request) {
		if (userMapper.existsByEmail(request.getEmail()) > 0) {
			auditLogger.failure(AuditLogger.AUTH, AuditLogger.EMAIL_VERIFY,
					null, null, "이메일 중복: " + request.getEmail());
			throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
		}

		String code = generateCode();

		tokenStore.set(KEY_VERIFY + request.getEmail(), code, TTL_VERIFY_CODE_MIN);
		mailService.sendVerificationEmail(request.getEmail(), code);
		auditLogger.success(AuditLogger.AUTH, AuditLogger.EMAIL_VERIFY,
				null, null, "인증코드 발송: " + request.getEmail());
	}

	// ──────────────────────────────────────────────────────────────────
	// 이메일 인증코드 확인
	// ──────────────────────────────────────────────────────────────────
	@Transactional
	public void verifyEmail(EmailVerifyRequest request) {
		// KEY_VERIFY 키에서 인증코드 조회
		String stored = tokenStore.get(KEY_VERIFY + request.getEmail());
		if (stored == null || !stored.equals(request.getCode())) {
			auditLogger.failure(AuditLogger.AUTH, AuditLogger.EMAIL_VERIFY,
					null, null, "인증코드 불일치: " + request.getEmail());
			throw new BusinessException(ErrorCode.VERIFY_TOKEN_INVALID);
		}

		// 사용된 인증코드 삭제 후 인증 완료 플래그를 별도 KEY_VERIFIED 키에 저장
		tokenStore.delete(KEY_VERIFY + request.getEmail());
		tokenStore.set(KEY_VERIFIED + request.getEmail(), "Y", TTL_VERIFIED_FLAG_MIN);
		auditLogger.success(AuditLogger.AUTH, AuditLogger.EMAIL_VERIFY,
				null, null, "이메일 인증 완료: " + request.getEmail());
	}

	// ──────────────────────────────────────────────────────────────────
	// 회원가입
	// ──────────────────────────────────────────────────────────────────
	@Transactional
	public Long signup(SignupRequest request) {

		// ① 이메일 중복
		if (userMapper.existsByEmail(request.getEmail()) > 0) {
			auditLogger.failure(AuditLogger.AUTH, AuditLogger.SIGNUP,
					null, null, "이메일 중복: " + request.getEmail());
			throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
		}

		// ② phone 중복 (암호화로 인해 전체 조회 후 비교)
		String formattedPhone = MaskingUtil.formatPhone(request.getPhone());
		boolean phoneExists = userMapper.findAllPhones().stream().anyMatch(u -> formattedPhone.equals(u.getPhone()));
		if (phoneExists) {
			auditLogger.failure(AuditLogger.AUTH, AuditLogger.SIGNUP,
					null, null, "전화번호 중복: " + formattedPhone);
			throw new BusinessException(ErrorCode.DUPLICATE_PHONE);
		}

		// ③ 이메일 인증 확인
		if (tokenStore.get(KEY_VERIFIED + request.getEmail()) == null) {
			auditLogger.failure(AuditLogger.AUTH, AuditLogger.SIGNUP,
					null, null, "이메일 인증 미완료: " + request.getEmail());
			throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
		}

		// ④ 필수 약관 체크
		List<Terms> signupTerms = termsMapper.findByPackageType("SIGNUP");
		Set<Long> requiredIds = signupTerms.stream().filter(t -> "Y".equals(t.getRequiredYn())).map(Terms::getTermsId)
				.collect(Collectors.toSet());
		if (!request.getAgreedTermsIds().containsAll(requiredIds)) {
			auditLogger.failure(AuditLogger.AUTH, AuditLogger.SIGNUP,
					null, null, "필수 약관 미동의");
			throw new BusinessException(ErrorCode.REQUIRED_TERMS_NOT_AGREED);
		}

		// ⑤ birthDate 파싱
		LocalDate birthDate = null;
		String birthDateStr = null;
		if (request.getBirthDate() != null && !request.getBirthDate().isBlank()) {
			String raw = request.getBirthDate().replace("-", "");
			birthDate = LocalDate.parse(raw, DateTimeFormatter.BASIC_ISO_DATE);
			birthDateStr = birthDate.toString(); // "YYYY-MM-DD"
		}

		// ⑥ CI값 생성 — birthDate null 허용 (선택 필드)
		String ciValue = null;
		if (birthDateStr != null) {
			// CI = 이름 + 생년월일(주민번호 앞6 = YYMMDD) + 전화번호
			ciValue = ciValueGenerator.generate(request.getName(),
			        request.getResidentFront(),
			        formattedPhone);
		}

		// ⑦ Watchlist 대조 (ciValue가 있을 때만 CI 기반 대조 수행)
		cddService.checkWatchlist(ciValue, request.getName(), birthDateStr);

		// ⑧ marketingAgree 변환
		String marketingAgreeYN = Boolean.TRUE.equals(request.getMarketingAgree()) ? "Y" : "N";

		// ⑨ User INSERT
		User user = User.builder().email(request.getEmail()).passwordHash(passwordEncoder.encode(request.getPassword()))
				.name(request.getName()).phone(formattedPhone).birthDate(birthDate).ciValue(ciValue) // CI값 저장 (AES 암호화는
																										// TypeHandler가
																										// 자동 처리)
				.job(request.getJob()).incomeLevelCode(request.getIncomeLevelCode())
				.creditScore(request.getCreditScore()).marketingAgree(marketingAgreeYN).build();

		userMapper.insertUser(user);

		// ⑨-1 본인인증 주소를 기본 배송지로 주소록(USER_ADDRESSES)에 저장
		if (request.getAddress() != null && !request.getAddress().isBlank()) {
			userAddressMapper.insert(UserAddress.builder()
					.userId(user.getUserId())
					.alias("기본 배송지")
					.address(request.getAddress().trim())
					.isDefault("Y")
					.statusCode("ACTIVE")
					.build());
		}

		// ⑩ 이메일 인증 완료 처리
		userMapper.updateEmailVerified(user.getUserId(), "Y");

		// ⑪ CDD 초기화 (회원가입 = SIMPLE CDD 통과 → VERIFIED)
		cddService.initializeCdd(user.getUserId());

		// ⑫ 약관 동의 저장
		List<UserTermsAgreement> agreements = signupTerms.stream()
				.filter(t -> request.getAgreedTermsIds().contains(t.getTermsId()))
				.map(t -> UserTermsAgreement.builder().userId(user.getUserId()).termsId(t.getTermsId()).agreedYn("Y")
						.agreementAction("AGREE").agreedVersion(t.getVersion()).agreementChannel("WEB")
						.agreementSource("SIGNUP").agreedAt(LocalDateTime.now(TimeConstants.KST)).build())
				.collect(Collectors.toList());

		if (!agreements.isEmpty()) {
			userTermsAgreementMapper.insertAgreements(agreements);
		}

		tokenStore.delete(KEY_VERIFIED + request.getEmail());

		auditLogger.success(AuditLogger.AUTH, AuditLogger.SIGNUP,
				user.getUserId(), null, null);
		return user.getUserId();
	}

	/**
	 * 회원가입 최초 IP 등록.
	 * AuthController.signup()에서 signup() 완료 후 호출.
	 * AuthService.signup()은 HttpServletRequest를 받지 않으므로
	 * Controller 레이어에서 IP를 추출하여 전달.
	 */
	public void registerInitialIp(Long userId, String ip) {
		ipTrustService.registerInitialIp(userId, ip);
	}

	/**
	 * 이메일로 userId 조회 — AuthController.login()에서 IP 챌린지 체크 시 사용.
	 * login()이 AuthTokenResult를 반환하고 userId를 포함하지 않으므로 별도 조회.
	 * 이미 login()에서 사용자 검증이 완료된 후 호출되므로 USER_NOT_FOUND는 발생하지 않음.
	 */
	public Long findUserIdByEmail(String email) {
		return userMapper.findByEmail(email)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND))
				.getUserId();
	}

	/**
	 * 로그인 완료 후 IP 챌린지 여부 확인.
	 * AuthController.login()에서 AuthTokenResult 발급 후 호출.
	 *
	 * @return 빈 Optional → 신뢰 IP (쿠키 그대로 발급)
	 *         값 있음     → challengeToken (쿠키 미발급, 챌린지 응답 반환)
	 */
	public java.util.Optional<String> checkIpChallenge(Long userId, String ip) {
		return ipTrustService.checkIp(userId, ip);
	}

	/**
	 * 사용자 로그인
	 *
	 * [중요] @Transactional(noRollbackFor = BusinessException.class) 로그인 실패 시
	 * incrementLoginFailCount() DB 쓰기가 예외 이후에도 반드시 커밋되어야 하므로 BusinessException 발생 시
	 * 롤백을 허용하지 않음. 이 선언을 제거하거나 readOnly = true 로 변경하면 로그인 실패 횟수가 항상 0으로 초기화되어 잠금
	 * 기능이 동작하지 않음.
	 */
	@Transactional(noRollbackFor = BusinessException.class)
	public AuthTokenResult login(LoginRequest request, HttpServletRequest httpRequest) {
		String ipAddress = resolveClientIp(httpRequest);
		String userAgent = httpRequest.getHeader("User-Agent");

		User user = userMapper.findByEmail(request.getEmail())
				.orElseGet(() -> {
					auditLogger.failure(AuditLogger.AUTH, AuditLogger.LOGIN,
							null, null, "존재하지 않는 이메일: " + request.getEmail());
					throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
				});

		try {
			validateUserStatus(user);
		} catch (BusinessException e) {
			auditLogger.failure(AuditLogger.AUTH, AuditLogger.LOGIN,
					user.getUserId(), null, e.getErrorCode().getMessage());
			throw e;
		}

		if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
			handleLoginFail(user);
			adminUserMapper.insertLoginHistory("USER", user.getUserId(), "FAIL",
					"\ube44\ubc00\ubc88\ud638 \ubd88\uc77c\uce58", ipAddress, request.getDeviceInfo(), userAgent);
			auditLogger.failure(AuditLogger.AUTH, AuditLogger.LOGIN,
					user.getUserId(), null, "비밀번호 불일치");
			throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
		}

		if (user.getLoginFailCount() > 0)
			userMapper.resetLoginFailCount(user.getUserId());

		adminUserMapper.insertLoginHistory("USER", user.getUserId(), "SUCCESS", null, ipAddress,
				request.getDeviceInfo(), userAgent);
		userMapper.updateLastLoginAt(user.getUserId(), LocalDateTime.now(TimeConstants.KST));
		auditLogger.success(AuditLogger.AUTH, AuditLogger.LOGIN,
				user.getUserId(), null, null);

		String accessToken = jwtTokenProvider.generateAccessToken(user.getUserId(), "ROLE_USER");

		return buildAuthCookies(accessToken, user.getUserId(), request.getDeviceInfo(), ipAddress, userAgent);
	}

	// ──────────────────────────────────────────────────────────────────
	// Access Token 재발급
	// ──────────────────────────────────────────────────────────────────
	@Transactional
	public ResponseCookie refresh(String refreshToken) {
		UserSession session = userSessionMapper.findByRefreshToken(refreshToken).orElseGet(() -> {
			// 유효한 세션 없음 → 이미 revoke된 토큰인지 확인하여 탈취 감지
			tokenSecurityService.handleStolenToken(refreshToken);
			throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
		});

		if (session.getExpiresAt().isBefore(LocalDateTime.now(TimeConstants.KST))) {
			auditLogger.failure(AuditLogger.AUTH, AuditLogger.TOKEN_REFRESH,
					session.getUserId(), null, "Refresh Token 만료");
			throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
		}

		String newAccessToken = jwtTokenProvider.generateAccessToken(session.getUserId(), "ROLE_USER");
		long expirationSec = jwtTokenProvider.getAccessExpirationSec();
		auditLogger.success(AuditLogger.AUTH, AuditLogger.TOKEN_REFRESH,
				session.getUserId(), null, null);

		return cookieUtil.createAccessCookie(newAccessToken, expirationSec);
	}

	// ── 일반 회원 로그아웃 ────────────────────────────────────────────
	@Transactional
	public void logout(Long userId) {
	    userSessionMapper.revokeAllByUserId(userId);
	    auditLogger.success(AuditLogger.AUTH, AuditLogger.LOGOUT, userId, null, null);
	}
	
	// ── 관리자 로그아웃 ───────────────────────────────────────────────
	@Transactional
	public void adminLogout(Long adminId) {
	    adminSessionMapper.revokeAllByAdminId(adminId);
	    auditLogger.adminSuccess(AuditLogger.ADMIN, AuditLogger.LOGOUT, adminId, null, null);
	}


	// ──────────────────────────────────────────────────────────────────
	// 아이디 찾기
	// ──────────────────────────────────────────────────────────────────
	@Transactional(readOnly = true)
	public FindIdResponse findId(FindIdRequest request) {
		String formattedPhone = MaskingUtil.formatPhone(request.getPhone());

		List<User> candidates = userMapper.findByName(request.getName());

		User user = candidates.stream().filter(u -> u.getPhone() != null && formattedPhone.equals(u.getPhone()))
				.findFirst().orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		return FindIdResponse.builder().maskedEmail(MaskingUtil.maskEmail(user.getEmail())).message("이메일 조회 완료")
				.build();
	}

	// ──────────────────────────────────────────────────────────────────
	// 비밀번호 재설정 링크 발송
	// ──────────────────────────────────────────────────────────────────
	@Transactional
	public void findPassword(FindPasswordRequest request) {
		User user = userMapper.findByEmailAndName(request.getEmail(), request.getName())
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		String token = generateCode() + generateCode();

		tokenStore.set(KEY_RESET + token, String.valueOf(user.getUserId()), TTL_PW_RESET_MIN);
		mailService.sendPasswordResetEmail(user.getEmail(), token);
		auditLogger.success(AuditLogger.AUTH, AuditLogger.PASSWORD_CHANGE,
				user.getUserId(), null, "비밀번호 재설정 링크 발송");
	}

	// ──────────────────────────────────────────────────────────────────
	// 비밀번호 재설정
	// ──────────────────────────────────────────────────────────────────
	@Transactional
	public void resetPassword(ResetPasswordRequest request) {
		if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
			auditLogger.failure(AuditLogger.AUTH, AuditLogger.PASSWORD_CHANGE,
					null, null, "비밀번호 확인 불일치");
			throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
		}

		String userIdStr = tokenStore.get(KEY_RESET + request.getToken());
		if (userIdStr == null) {
			auditLogger.failure(AuditLogger.AUTH, AuditLogger.PASSWORD_CHANGE,
					null, null, "재설정 토큰 만료 또는 없음");
			throw new BusinessException(ErrorCode.VERIFY_TOKEN_INVALID);
		}

		Long userId = Long.parseLong(userIdStr);
		userMapper.updatePassword(userId, passwordEncoder.encode(request.getNewPassword()), LocalDateTime.now(TimeConstants.KST));
		userMapper.revokeAllSessions(userId);
		tokenStore.delete(KEY_RESET + request.getToken());
		auditLogger.success(AuditLogger.AUTH, AuditLogger.PASSWORD_CHANGE,
				userId, null, "비밀번호 재설정 완료");
	}

	// ── 관리자 로그인 ──────────────────────────────────────────────────
	@Transactional(noRollbackFor = BusinessException.class)
	public AuthTokenResult adminLogin(AdminLoginRequest request, HttpServletRequest httpRequest) {
	    String ipAddress = resolveClientIp(httpRequest);
	    String userAgent = httpRequest.getHeader("User-Agent");

	    AdminUser admin = adminUserMapper.findByUsername(request.getUsername())
	            .orElseGet(() -> {
	                auditLogger.adminFailure(AuditLogger.ADMIN, AuditLogger.LOGIN,
	                        null, null, "존재하지 않는 관리자 계정: " + request.getUsername());
	                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
	            });

	    if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
	        auditLogger.adminFailure(AuditLogger.ADMIN, AuditLogger.LOGIN,
	                admin.getAdminId(), null, "비밀번호 불일치");
	        throw new BusinessException(ErrorCode.INVALID_PASSWORD);
	    }

	    adminUserMapper.updateLastLoginAt(admin.getAdminId(), LocalDateTime.now(TimeConstants.KST));
	    auditLogger.adminSuccess(AuditLogger.ADMIN, AuditLogger.LOGIN,
	            admin.getAdminId(), null, null);

	    String roles = (admin.getRoleCodes() != null && !admin.getRoleCodes().isEmpty())
	            ? String.join(",", admin.getRoleCodes()) : "";

	    String accessToken = jwtTokenProvider.generateAdminAccessToken(admin.getAdminId(), roles);

	    return buildAdminAuthCookies(accessToken, admin.getAdminId(), ipAddress, userAgent);
	}
	
	@Transactional
	public ResponseCookie adminRefresh(String refreshToken) {
	    AdminSession session = adminSessionMapper.findByRefreshToken(refreshToken)
	            .orElseThrow(() -> {
	                // 탈취 감지 — ADMIN_SESSIONS 기준으로 조회
	                adminSessionMapper.findAnyByRefreshToken(refreshToken).ifPresent(s -> {
	                    adminSessionMapper.revokeAllByAdminId(s.getAdminId());
	                    auditLogger.adminFailure(AuditLogger.ADMIN, AuditLogger.TOKEN_REFRESH,
	                            s.getAdminId(), null, "Refresh Token 재사용 감지 — 전 세션 강제 만료");
	                });
	                throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
	            });

	    if (session.getExpiresAt().isBefore(LocalDateTime.now(TimeConstants.KST))) {
	        auditLogger.adminFailure(AuditLogger.ADMIN, AuditLogger.TOKEN_REFRESH,
	                session.getAdminId(), null, "Refresh Token 만료");
	        throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
	    }

	    // roles 재조회
	    AdminUser admin = adminUserMapper.findById(session.getAdminId())
	            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	    String roles = (admin.getRoleCodes() != null && !admin.getRoleCodes().isEmpty())
	            ? String.join(",", admin.getRoleCodes()) : "";

	    String newAccessToken = jwtTokenProvider.generateAdminAccessToken(session.getAdminId(), roles);
	    auditLogger.adminSuccess(AuditLogger.ADMIN, AuditLogger.TOKEN_REFRESH,
	            session.getAdminId(), null, null);

	    return cookieUtil.createAccessCookie(newAccessToken, jwtTokenProvider.getAccessExpirationSec());
	}


	// ──────────────────────────────────────────────────────────────────
	// private helpers
	// ──────────────────────────────────────────────────────────────────

	private void validateUserStatus(User user) {
	    if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now(TimeConstants.KST)))
	        throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
	    String status = user.getStatusCode();
	    if ("SUSPENDED".equals(status)) throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
	    if ("WITHDRAWN".equals(status)) throw new BusinessException(ErrorCode.ACCOUNT_WITHDRAWN);
	    if ("DORMANT".equals(status))   throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
	}

	private void handleLoginFail(User user) {
		userMapper.incrementLoginFailCount(user.getUserId());
		int newFailCount = user.getLoginFailCount() + 1;
		if (newFailCount >= MAX_LOGIN_FAIL) {
			LocalDateTime lockUntil = LocalDateTime.now(TimeConstants.KST).plusMinutes(LOCK_DURATION_MIN);
			userMapper.updateLockedUntil(user.getUserId(), lockUntil);
			auditLogger.failure(AuditLogger.AUTH, AuditLogger.LOGIN,
					user.getUserId(), null, "비밀번호 " + MAX_LOGIN_FAIL + "회 오류 — 계정 잠금: until=" + lockUntil);
		}
	}

	private String generateCode() {
		return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
	}

	private String resolveClientIp(HttpServletRequest request) {
	    return ClientIpUtil.normalize(request.getRemoteAddr());
	}
	
	/** 일반 회원용 — USER_SESSIONS */
	private AuthTokenResult buildAuthCookies(String accessToken, Long userId,
	        String deviceInfo, String ipAddress, String userAgent) {
	    String refreshToken  = jwtTokenProvider.generateRefreshToken(userId);
	    long   refreshExpSec = jwtTokenProvider.getRefreshExpirationSec();

	    userSessionMapper.insertSession(UserSession.builder()
	            .userId(userId)
	            .refreshToken(refreshToken)
	            .deviceInfo(deviceInfo)
	            .ipAddress(ipAddress)
	            .userAgent(userAgent)
	            .expiresAt(LocalDateTime.now(TimeConstants.KST).plusSeconds(refreshExpSec))
	            .build());

	    AuthTokenResult result = new AuthTokenResult();
	    result.setAccessCookie(cookieUtil.createAccessCookie(accessToken,
	            jwtTokenProvider.getAccessExpirationSec()));
	    result.setRefreshCookie(cookieUtil.createRefreshCookie(refreshToken, refreshExpSec));
	    return result;
	}

	/** 관리자용 — ADMIN_SESSIONS, Refresh 쿠키 path=/api/admin/auth */
	private AuthTokenResult buildAdminAuthCookies(String accessToken, Long adminId,
	        String ipAddress, String userAgent) {
	    String refreshToken  = jwtTokenProvider.generateRefreshToken(adminId);
	    long   refreshExpSec = jwtTokenProvider.getRefreshExpirationSec();

	    adminSessionMapper.insertSession(AdminSession.builder()
	            .adminId(adminId)
	            .refreshToken(refreshToken)
	            .ipAddress(ipAddress)
	            .userAgent(userAgent)
	            .expiresAt(LocalDateTime.now(TimeConstants.KST).plusSeconds(refreshExpSec))
	            .build());

	    AuthTokenResult result = new AuthTokenResult();
	    result.setAccessCookie(cookieUtil.createAccessCookie(accessToken,
	            jwtTokenProvider.getAccessExpirationSec()));
	    result.setRefreshCookie(cookieUtil.createAdminRefreshCookie(refreshToken, refreshExpSec));
	    return result;
	}
}