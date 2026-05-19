package com.bnk.global.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 정적 HTML 페이지 라우팅 컨트롤러
 * - /reset-password  : 비밀번호 재설정 페이지 (이메일 링크 랜딩)
 * - /auth-test       : 인증 테스트 콘솔
 */
@Controller
public class PageController {

    /**
     * 비밀번호 재설정 페이지
     * 이메일에서 전송된 링크: http://localhost:8080/reset-password?token=UUID
     * → templates/reset-password.html 렌더링
     */
    @GetMapping("/reset-password")
    public String resetPasswordPage() {
        return "reset-password";
    }
}