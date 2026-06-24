package com.bnk.domain.admin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bnk.domain.admin.mapper.WatchlistMapper;
import com.bnk.domain.admin.model.Watchlist;
import com.bnk.domain.application.service.CddService;
import com.bnk.global.auth.CustomAdminDetails;
import com.bnk.global.response.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 관리자 Watchlist(요주의 인물) 관리 API
 *
 * GET /api/admin/watchlist 전체 목록 조회 POST /api/admin/watchlist 등록 DELETE
 * /api/admin/watchlist/{id} 삭제 PATCH /api/admin/users/{userId}/cdd 회원 CDD 상태 변경
 * PATCH /api/admin/users/{userId}/pep PEP 지정 (cdd_status_code → ENHANCED)
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AdminWatchlistController {

	private final WatchlistMapper watchlistMapper;
	private final CddService cddService;

	// ── Watchlist 전체 조회 ───────────────────────────────────────
	@GetMapping("/api/admin/watchlist")
	public ResponseEntity<ApiResponse<List<Watchlist>>> getAll() {
		return ResponseEntity.ok(ApiResponse.ok(watchlistMapper.findAll()));
	}

	// ── Watchlist 등록 ───────────────────────────────────────────
	@PostMapping("/api/admin/watchlist")
	public ResponseEntity<ApiResponse<Void>> register(@RequestBody @Valid WatchlistRegisterRequest request,
			@AuthenticationPrincipal CustomAdminDetails admin) {

		Watchlist raw = Watchlist.builder().name(request.getName()).birthDate(request.getBirthDate())
				.ciValue(request.getCiValue()).reason(request.getReason()).riskLevel(request.getRiskLevel())
				.registeredBy(admin.getAdminId()).build();

		// 해시 계산 + insert → CddService에서 처리
		cddService.registerWatchlist(raw);

		log.info("[Watchlist] 등록 adminId={} name={}", admin.getAdminId(), request.getName());
		return ResponseEntity.ok(ApiResponse.ok(null));
	}

	// ── Watchlist 삭제 ───────────────────────────────────────────
	@DeleteMapping("/api/admin/watchlist/{watchlistId}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long watchlistId,
			@AuthenticationPrincipal CustomAdminDetails admin) {

		watchlistMapper.delete(watchlistId, admin.getAdminId());
		log.info("[Watchlist] 삭제 adminId={} watchlistId={}", admin.getAdminId(), watchlistId);
		return ResponseEntity.ok(ApiResponse.ok(null));
	}

	// ── 회원 CDD 상태 변경 ─────────────────────────────────────
	@PatchMapping("/api/admin/users/{userId}/cdd")
	public ResponseEntity<ApiResponse<Void>> updateCddStatus(@PathVariable Long userId,
			@RequestBody CddStatusRequest request, @AuthenticationPrincipal CustomAdminDetails admin) {

		cddService.updateCddStatus(userId, request.getCddStatusCode());
		log.info("[CDD] 관리자 상태변경 adminId={} userId={} → {}", admin.getAdminId(), userId, request.getCddStatusCode());
		return ResponseEntity.ok(ApiResponse.ok(null));
	}

	// ── PEP 지정 (ENHANCED로 격상) ─────────────────────────────
	@PatchMapping("/api/admin/users/{userId}/pep")
	public ResponseEntity<ApiResponse<Void>> designatePep(@PathVariable Long userId,
			@AuthenticationPrincipal CustomAdminDetails admin) {

		cddService.updateCddStatus(userId, "ENHANCED");
		log.info("[PEP] 지정 adminId={} userId={}", admin.getAdminId(), userId);
		return ResponseEntity.ok(ApiResponse.ok(null));
	}

	// ── Request DTO ───────────────────────────────────────────────
	@Getter
	@Setter
	public static class WatchlistRegisterRequest {
		@NotBlank
		private String name;
		private String birthDate; // YYYY-MM-DD
		private String ciValue;
		@Size(max = 500)
		private String reason;
		private String riskLevel = "HIGH";
	}

	@Getter
	@Setter
	public static class CddStatusRequest {
		private String cddStatusCode; // PENDING / VERIFIED / ENHANCED / REJECTED
	}
}
