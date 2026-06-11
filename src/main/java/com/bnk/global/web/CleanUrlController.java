package com.bnk.global.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class CleanUrlController {

    @GetMapping("/login")
    public String login() {
        return "forward:/auth/login.html";
    }

    @GetMapping("/signup")
    public String signup() {
        return "forward:/auth/signup.html";
    }

    @GetMapping("/find-id")
    public String findId() {
        return "forward:/auth/find-id.html";
    }

    @GetMapping("/reset-password")
    public String resetPassword() {
        return "forward:/auth/reset-password.html";
    }

    @GetMapping("/admin/login")
    public String adminLogin() {
        return "forward:/auth/admin-login.html";
    }

    @GetMapping("/mypage")
    public String mypage() {
        return "forward:/mypage/index.html";
    }

    @GetMapping("/mypage/edit")
    public String mypageEdit() {
        return "forward:/mypage/edit.html";
    }

    @GetMapping("/mypage/password")
    public String mypagePassword() {
        return "forward:/mypage/password.html";
    }

    @GetMapping("/mypage/spending")
    public String mypageSpending() {
        return "forward:/mypage/spending.html";
    }

    @GetMapping("/copy-code")
    public String copyCode() {
        return "forward:/auth/copy-code.html";
    }

    @GetMapping("/card/{cardId:\\d+}")
    public String cardDetail(@PathVariable String cardId) {
        return "forward:/card/index.html";
    }

    @GetMapping("/admin/cards")
    public String adminCards() {
        return "forward:/admin/cardManage.html";
    }

    @GetMapping("/admin/users")
    public String adminUsers() {
        return "forward:/admin/userManage.html";
    }

    @GetMapping("/admin/approvals")
    public String adminApprovals() {
        return "forward:/admin/approvalManage.html";
    }

    @GetMapping("/admin/approvals/{approvalId}")
    public String adminApprovalDetail(@PathVariable String approvalId) {
        return "forward:/admin/approvalManage.html";
    }
}