package com.bnk.domain.user.controller;

import com.bnk.domain.user.dto.request.AddressAliasUpdateRequest;
import com.bnk.domain.user.dto.request.AddressCreateRequest;
import com.bnk.domain.user.dto.response.AddressResponse;
import com.bnk.domain.user.service.UserAddressService;
import com.bnk.global.auth.CustomUserDetails;
import com.bnk.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 마이페이지 주소록(배송지) 관리 CRUD */
@RestController
@RequestMapping("/api/users/me/addresses")
@RequiredArgsConstructor
public class UserAddressController {

    private final UserAddressService addressService;

    /** GET /api/users/me/addresses — 주소 목록 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(
            @AuthenticationPrincipal CustomUserDetails ud) {
        return ApiResponse.toOk(addressService.getAddresses(ud.getUserId()));
    }

    /** POST /api/users/me/addresses — 주소 등록 */
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> addAddress(
            @AuthenticationPrincipal CustomUserDetails ud,
            @Valid @RequestBody AddressCreateRequest req) {
        return ApiResponse.toCreated(addressService.addAddress(ud.getUserId(), req));
    }

    /** PATCH /api/users/me/addresses/{addressId} — 별칭 수정 */
    @PatchMapping("/{addressId}")
    public ResponseEntity<ApiResponse<Void>> updateAlias(
            @AuthenticationPrincipal CustomUserDetails ud,
            @PathVariable Long addressId,
            @Valid @RequestBody AddressAliasUpdateRequest req) {
        addressService.updateAlias(ud.getUserId(), addressId, req.getAlias());
        return ApiResponse.toOk(null);
    }

    /** PATCH /api/users/me/addresses/{addressId}/default — 기본 배송지 설정 */
    @PatchMapping("/{addressId}/default")
    public ResponseEntity<ApiResponse<Void>> setDefault(
            @AuthenticationPrincipal CustomUserDetails ud,
            @PathVariable Long addressId) {
        addressService.setDefault(ud.getUserId(), addressId);
        return ApiResponse.toOk(null);
    }

    /** DELETE /api/users/me/addresses/{addressId} — 주소 삭제 */
    @DeleteMapping("/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal CustomUserDetails ud,
            @PathVariable Long addressId) {
        addressService.deleteAddress(ud.getUserId(), addressId);
        return ApiResponse.toNoContent();
    }
}
