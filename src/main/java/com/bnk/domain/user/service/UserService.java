package com.bnk.domain.user.service;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.bnk.domain.user.dto.request.PasswordChangeRequest;
import com.bnk.domain.user.dto.request.UserUpdateRequest;
import com.bnk.domain.user.dto.response.UserResponse;

import jakarta.validation.Valid;

@Service
@Validated
public class UserService {

	public void changePassword(Long userId, @Valid PasswordChangeRequest request) {
		// TODO Auto-generated method stub
		
	}

	public UserResponse getMyInfo(Long userId) {
		// TODO Auto-generated method stub
		return null;
	}

	public void updateMyInfo(Long userId, @Valid UserUpdateRequest request) {
		// TODO Auto-generated method stub
		
	}

}
