package com.bnk.domain.admin.controller;
 
import com.bnk.domain.notification.dto.request.NotificationBatchRequest;
import com.bnk.domain.notification.dto.response.NotificationBatchResponse;
import com.bnk.domain.notification.service.NotificationService;
import com.bnk.global.auth.CustomAdminDetails;
import com.bnk.global.response.ApiResponse;
import com.bnk.global.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
 
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {
 
    private final NotificationService notificationService;
 
    /** 배치 목록 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationBatchResponse>>> getBatches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.toOk(notificationService.getBatches(page, size));
    }
 
    /** 배치 상세 */
    @GetMapping("/{batchId}")
    public ResponseEntity<ApiResponse<NotificationBatchResponse>> getBatch(
            @PathVariable Long batchId) {
        return ApiResponse.toOk(notificationService.getBatch(batchId));
    }
 
    /** 배치 생성 (초안 저장 또는 예약 등록) */
    @PostMapping
    public ResponseEntity<ApiResponse<NotificationBatchResponse>> createBatch(
            @Valid @RequestBody NotificationBatchRequest req,
            @AuthenticationPrincipal CustomAdminDetails admin) {
        return ApiResponse.toOk(
                notificationService.createBatch(req, admin.getAdminId()));
    }
 
    /** 배치 즉시 발송 */
    @PostMapping("/{batchId}/send")
    public ResponseEntity<ApiResponse<NotificationBatchResponse>> sendBatch(
            @PathVariable Long batchId,
            @AuthenticationPrincipal CustomAdminDetails admin) {
        return ApiResponse.toOk(
                notificationService.sendBatch(batchId, admin.getAdminId()));
    }
}