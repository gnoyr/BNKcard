package com.bnk.domain.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bnk.domain.auth.dto.request.EmailVerifyRequest;
import com.bnk.domain.auth.dto.request.FindIdRequest;
import com.bnk.domain.auth.dto.request.FindPasswordRequest;
import com.bnk.domain.auth.dto.request.LoginRequest;
import com.bnk.domain.auth.dto.request.ResetPasswordRequest;
import com.bnk.domain.auth.dto.request.SendVerifyCodeRequest;
import com.bnk.domain.auth.dto.request.SignupRequest;
import com.bnk.domain.auth.dto.response.AuthTokenResult;
import com.bnk.domain.auth.dto.response.FindIdResponse;
import com.bnk.domain.auth.service.AuthService;
import com.bnk.global.auth.CustomUserDetails;
import com.bnk.global.response.ApiResponse;
import com.bnk.global.util.ClientIpUtil;
import com.bnk.global.util.CookieUtil;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final CookieUtil cookieUtil;

	/**
	 * 이메일 인증코드 발송 (회원가입 전 단계) 비로그인 허용 — 이메일 중복 체크 후 6자리 코드 발송
	 */
	@PostMapping("/send-verify-code")
	public ResponseEntity<ApiResponse<Void>> sendVerifyCode(@RequestBody @Valid SendVerifyCodeRequest request) {
		authService.sendVerifyCode(request);
		return ApiResponse.toOk(null);
	}

	/**
	 * 이메일 인증코드 확인
	 */
	@PostMapping("/verify-email")
	public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestBody @Valid EmailVerifyRequest request) {
		authService.verifyEmail(request);
		return ApiResponse.toOk(null);
	}

	/**
	 * 회원가입 — 이메일 인증 완료 후 호출
	 */
	@PostMapping("/signup")
	public ResponseEntity<ApiResponse<Long>> signup(
			@RequestBody @Valid SignupRequest request,
			HttpServletRequest httpRequest) {
		Long userId = authService.signup(request);
		// 회원가입 완료 후 최초 접속 IP 자동 등록 (이후 로그인과 동일하게 정규화된 IP로 저장)
		authService.registerInitialIp(userId, ClientIpUtil.resolve(httpRequest));
		return ApiResponse.toCreated(userId);
	}

	/**
	 * 로그인 — Access + Refresh 쿠키 발급. HttpServletRequest 를 Service 로 전달하여 ip_address
	 * / user_agent 를 LOGIN_HISTORIES 에 기록.
	 */
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<?>> login(
	        @RequestBody @Valid LoginRequest request,
	        HttpServletRequest httpRequest,
	        HttpServletResponse response) {

	    AuthTokenResult result = authService.login(request, httpRequest);

	    // IP 챌린지 체크 — result에서 userId를 꺼내기 위해 AccessCookie의 subject 대신
	    // login()이 이미 user.getUserId()를 사용하므로, AuthService에 위임
	    // 정규화된 IP를 사용해야 같은 단말이 매번 '새 기기'로 오인되지 않는다.
	    String clientIp = ClientIpUtil.resolve(httpRequest);
	    // AuthTokenResult에 userId가 없으므로 이메일로 userId 재조회
	    // (login() 내부에서 이미 조회한 user 객체를 재활용하지 않으므로 최소 비용으로 재조회)
	    Long userId = authService.findUserIdByEmail(request.getEmail());

	    Optional<String> challengeOpt = authService.checkIpChallenge(userId, clientIp);
	    if (challengeOpt.isPresent()) {
	        return ResponseEntity.ok(ApiResponse.ok(Map.of(
	            "requireIpVerify",   true,
	            "userId",            userId,
	            "challengeToken",    challengeOpt.get(),
	            "availableMethods",  List.of("EMAIL", "CI")
	        )));
	    }

	    // 신뢰 IP → 기존 쿠키 발급 흐름
	    // 신규 쿠키 발급 전, 구버전 path 쿠키 잔류분 강제 삭제
	    // (CookieUtil path 변경 배포 과도기 대응 — 7일 후 제거 가능)
	    response.addHeader(HttpHeaders.SET_COOKIE,
	        cookieUtil.deleteLegacyRefreshCookie().toString());

	    response.addHeader(HttpHeaders.SET_COOKIE,
	        result.getAccessCookie().toString());
	    response.addHeader(HttpHeaders.SET_COOKIE,
	        result.getRefreshCookie().toString());

	    return ResponseEntity.ok(ApiResponse.message("로그인에 성공했습니다."));
	}

	/**
	 * Access Token 재발급 — Refresh 쿠키로 새 Access 쿠키 발급
	 */
	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<Void>> refresh(
			@CookieValue(value = "refresh_token", required = false) String refreshToken, HttpServletResponse response) {
		if (refreshToken == null || refreshToken.isBlank()) {
	        // 쿠키 자체가 없는 경우 → 재로그인 유도
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                .body(ApiResponse.message("세션이 만료되었습니다. 다시 로그인해 주세요."));
	    }
		response.addHeader(HttpHeaders.SET_COOKIE, authService.refresh(refreshToken).toString());
		return ApiResponse.toOk(null);
	}

	/**
	 * 로그아웃 — DB 세션 revoke + 쿠키 삭제. 비로그인 상태의 로그아웃 요청은 쿠키만 삭제하고 정상 응답 반환한다.
	 */
	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(
			@AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails ud, HttpServletResponse response) {

		// ud == null: 비로그인(토큰 없음 또는 만료) 상태 → DB revoke 생략, 쿠키만 삭제
		if (ud != null) {
			authService.logout(ud.getUserId());
		}

		// 쿠키는 인증 상태와 무관하게 항상 삭제 (브라우저 잔류 쿠키 제거)
		response.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.deleteAccessCookie().toString());
		response.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.deleteRefreshCookie().toString());
		return ApiResponse.toNoContent();
	}

	/**
	 * 아이디 찾기 (F-20) 명세 경로: /api/auth/find-id
	 */
	@PostMapping("/find-id")
	public ResponseEntity<ApiResponse<FindIdResponse>> findId(@RequestBody @Valid FindIdRequest request) {
		return ApiResponse.toOk(authService.findId(request));
	}

	/**
	 * 비밀번호 재설정 링크 요청 (F-22) 명세 경로: /api/auth/find-password
	 */
	@PostMapping("/find-password")
	public ResponseEntity<ApiResponse<Void>> findPassword(@RequestBody @Valid FindPasswordRequest request) {
		authService.findPassword(request);
		return ApiResponse.toOk(null);
	}

	/**
	 * 비밀번호 재설정
	 */
	@PostMapping("/reset-password")
	public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
		authService.resetPassword(request);
		return ApiResponse.toOk(null);
	}
}