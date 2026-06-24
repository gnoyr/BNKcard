package com.bnk.domain.auth.dto.response;

import org.springframework.http.ResponseCookie;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthTokenResult {
    private ResponseCookie accessCookie;
    private ResponseCookie refreshCookie;
}