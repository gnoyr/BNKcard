package com.bnk.global.config;

import com.bnk.global.web.PageGuardInterceptor;
import com.bnk.global.web.PageGuardInterceptor.Guard;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC 인터셉터 등록.
 *
 * 페이지(HTML) 접근 제어는 SecurityConfig가 아닌 PageGuardInterceptor로 처리한다.
 * SecurityConfig는 API(/api/**) 보호만 담당하고,
 * HTML 페이지 경로 보호는 이 설정에서 인터셉터로 처리한다.
 *
 * ┌────────────────────────────────────────────────────────────┐
 * │ 경로 → Guard 매핑                                            │
 * │  /mypage/**          → USER_ONLY   (일반 사용자 전용)         │
 * │  /admin/users        → OPERATOR    (세 role 모두 허용)        │
 * │  /admin/dashboard    → MANAGER     (MANAGER 이상)            │
 * │  /admin/cards        → MANAGER     (MANAGER 이상)            │
 * │  /admin/approvals/** → SUPER_ADMIN (SUPER_ADMIN 전용)        │
 * └────────────────────────────────────────────────────────────┘
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // ── 일반 사용자 전용 페이지 ─────────────────────────────────
        // 비로그인 → /login
        // 관리자 접근 → 관리자 role 기본 페이지로 redirect
        registry.addInterceptor(new PageGuardInterceptor(Guard.USER_ONLY))
                .addPathPatterns(
                    "/mypage",
                    "/mypage/edit",
                    "/mypage/password",
                    "/mypage/spending",
                    "/mypage/credit-score",
                    "/mypage/trusted-devices",
                    "/mypage/accounts"
                );

        // ── 관리자 공통 페이지 (OPERATOR 이상) ─────────────────────
        // 비로그인 → /admin/login
        // 일반 사용자 → /login
        // OPERATOR, MANAGER, SUPER_ADMIN 모두 허용
        registry.addInterceptor(new PageGuardInterceptor(Guard.OPERATOR))
                .addPathPatterns("/admin/users");

        // ── MANAGER 이상 페이지 ─────────────────────────────────────
        // OPERATOR → /admin/users 로 redirect
        registry.addInterceptor(new PageGuardInterceptor(Guard.MANAGER))
                .addPathPatterns(
                    "/admin/cards",
                    "/admin/dashboard"
                );

        // ── SUPER_ADMIN 전용 페이지 ─────────────────────────────────
        // OPERATOR → /admin/users 로 redirect
        // MANAGER  → /admin/cards 로 redirect
        registry.addInterceptor(new PageGuardInterceptor(Guard.SUPER_ADMIN))
                .addPathPatterns(
                    "/admin/approvals",
                    "/admin/approvals/*"
                );
    }
}