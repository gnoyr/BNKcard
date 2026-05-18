package com.bnk.domain.auth.service;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.bnk.domain.auth.dto.request.AdminLoginRequest;
import com.bnk.domain.auth.dto.request.EmailVerifyRequest;
import com.bnk.domain.auth.dto.request.FindIdRequest;
import com.bnk.domain.auth.dto.request.FindPasswordRequest;
import com.bnk.domain.auth.dto.request.LoginRequest;
import com.bnk.domain.auth.dto.request.ResetPasswordRequest;
import com.bnk.domain.auth.dto.request.SignupRequest;
import com.bnk.domain.auth.dto.response.AuthTokenResult;
import com.bnk.domain.auth.dto.response.FindIdResponse;

import jakarta.validation.Valid;

@Service
@Validated
public class AuthService {

	public AuthTokenResult adminLogin(@Valid AdminLoginRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	public void findPassword(@Valid ResetPasswordRequest request) {
		// TODO Auto-generated method stub
		
	}

	public FindIdResponse findId(@Valid FindIdRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	public AuthTokenResult login(@Valid LoginRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	public ResponseCookie logout(Long userId) {
		// TODO Auto-generated method stub
		return null;
	}

	public AuthTokenResult refresh(String refreshToken) {
		// TODO Auto-generated method stub
		return null;
	}

	public Long signup(@Valid SignupRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	public void verifyEmail(@Valid EmailVerifyRequest request) {
		// TODO Auto-generated method stub
		
	}

	public void findPassword(@Valid FindPasswordRequest request) {
		// TODO Auto-generated method stub
		
	}

	public void resetPassword(@Valid ResetPasswordRequest request) {
		// TODO Auto-generated method stub
		
	}

}
