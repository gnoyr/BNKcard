package com.bnk.domain.auth.dto.response;

import org.springframework.http.ResponseCookie;

import lombok.Data;

@Data
public class AuthTokenResult{
	private TokenResponse token;
	private ResponseCookie cookie;
}
