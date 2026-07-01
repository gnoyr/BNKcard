package com.bnk.domain.deviceauth.controller;

import com.bnk.domain.deviceauth.dto.request.TrustedDeviceNameUpdateRequest;
import com.bnk.domain.deviceauth.dto.response.TrustedDeviceResponse;
import com.bnk.domain.deviceauth.service.DeviceTrustService;
import com.bnk.global.auth.CustomUserDetails;
import com.bnk.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 마이페이지 신뢰 기기 관리 CRUD */
@RestController
@RequestMapping("/api/users/me/trusted-devices")
@RequiredArgsConstructor
public class TrustedDeviceController {

    private final DeviceTrustService deviceTrustService;

    /** GET /api/users/me/trusted-devices */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TrustedDeviceResponse>>> getTrustedDevices(
            @AuthenticationPrincipal CustomUserDetails ud) {
        return ApiResponse.toOk(deviceTrustService.getTrustedDevices(ud.getUserId()));
    }

    /** PATCH /api/users/me/trusted-devices/{deviceTrustId} */
    @PatchMapping("/{deviceTrustId}")
    public ResponseEntity<ApiResponse<Void>> updateDeviceName(
            @AuthenticationPrincipal CustomUserDetails ud,
            @PathVariable Long deviceTrustId,
            @Valid @RequestBody TrustedDeviceNameUpdateRequest req) {
        deviceTrustService.updateDeviceName(ud.getUserId(), deviceTrustId, req.getDeviceName());
        return ApiResponse.toOk(null);
    }

    /** DELETE /api/users/me/trusted-devices/{deviceTrustId} */
    @DeleteMapping("/{deviceTrustId}")
    public ResponseEntity<ApiResponse<Void>> deleteTrustedDevice(
            @AuthenticationPrincipal CustomUserDetails ud,
            @PathVariable Long deviceTrustId) {
        deviceTrustService.deleteTrustedDevice(ud.getUserId(), deviceTrustId);
        return ApiResponse.toNoContent();
    }
}
