package com.bnk.domain.auth.dto.response;

import org.springframework.http.ResponseCookie;

import lombok.Data;

@Data
public class AuthTokenResult {
    private ResponseCookie accessCookie;
    private ResponseCookie refreshCookie;
}