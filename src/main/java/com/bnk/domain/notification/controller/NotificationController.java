package com.bnk.domain.notification.controller;
 
import com.bnk.domain.notification.dto.response.NotificationListResponse;
import com.bnk.domain.notification.service.NotificationService;
import com.bnk.global.auth.CustomUserDetails;
import com.bnk.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
 
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
 
    private final NotificationService notificationService;
 
    /** 내 알림 목록 + 미읽음 수 */
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationListResponse>> getMyNotifications(
            @AuthenticationPrincipal CustomUserDetails ud) {
        return ApiResponse.toOk(notificationService.getMyNotifications(ud.getUserId()));
    }
 
    /** 미읽음 수만 (헤더 뱃지 폴링용) */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Integer>> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails ud) {
        return ApiResponse.toOk(notificationService.getUnreadCount(ud.getUserId()));
    }
 
    /** 단건 읽음 처리 */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal CustomUserDetails ud) {
        notificationService.markAsRead(notificationId, ud.getUserId());
        return ApiResponse.toOk(null);
    }
 
    /** 전체 읽음 처리 */
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails ud) {
        notificationService.markAllAsRead(ud.getUserId());
        return ApiResponse.toOk(null);
    }
}