package com.bnk.domain.ipauth.controller;

import com.bnk.domain.ipauth.dto.request.TrustedIpNicknameUpdateRequest;
import com.bnk.domain.ipauth.dto.response.TrustedIpResponse;
import com.bnk.domain.ipauth.service.IpTrustService;
import com.bnk.global.auth.CustomUserDetails;
import com.bnk.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 마이페이지 신뢰 IP 관리 CRUD */
@RestController
@RequestMapping("/api/users/me/trusted-ips")
@RequiredArgsConstructor
public class TrustedIpController {

    private final IpTrustService ipTrustService;

    /** GET /api/users/me/trusted-ips */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TrustedIpResponse>>> getTrustedIps(
            @AuthenticationPrincipal CustomUserDetails ud) {
        return ApiResponse.toOk(ipTrustService.getTrustedIps(ud.getUserId()));
    }

    /** PATCH /api/users/me/trusted-ips/{trustId} */
    @PatchMapping("/{trustId}")
    public ResponseEntity<ApiResponse<Void>> updateNickname(
            @AuthenticationPrincipal CustomUserDetails ud,
            @PathVariable Long trustId,
            @Valid @RequestBody TrustedIpNicknameUpdateRequest req) {
        ipTrustService.updateNickname(ud.getUserId(), trustId, req.getNickname());
        return ApiResponse.toOk(null);
    }

    /** DELETE /api/users/me/trusted-ips/{trustId} */
    @DeleteMapping("/{trustId}")
    public ResponseEntity<ApiResponse<Void>> deleteTrustedIp(
            @AuthenticationPrincipal CustomUserDetails ud,
            @PathVariable Long trustId) {
        ipTrustService.deleteTrustedIp(ud.getUserId(), trustId);
        return ApiResponse.toNoContent();
    }
}
