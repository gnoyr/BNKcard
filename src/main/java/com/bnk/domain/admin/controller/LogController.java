package com.bnk.domain.admin.controller;

import com.bnk.domain.admin.service.LogService;
import com.bnk.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    /** 관리자 활동 로그 */
    @GetMapping("/admin-activity")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminActivityLogs(
            @RequestParam(required = false) String adminName,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page) {
        return ApiResponse.toOk(
                logService.getAdminActivityLogs(
                        adminName, roleCode, result, from, to, page));
    }

    /** 감사 로그 */
    @GetMapping("/audit")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page) {
        return ApiResponse.toOk(
                logService.getAuditLogs(
                        action, result, ip, from, to, page));
    }

    /** 회원 활동 로그 */
    @GetMapping("/user-activity")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserActivityLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page) {
        return ApiResponse.toOk(
                logService.getUserActivityLogs(
                        action, result, from, to, page));
    }
}