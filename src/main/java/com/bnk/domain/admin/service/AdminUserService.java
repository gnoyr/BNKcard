package com.bnk.domain.admin.service;

import org.springframework.stereotype.Service;

import com.bnk.domain.admin.dto.request.AdminUserSearchRequest;
import com.bnk.domain.admin.dto.response.AdminUserResponse;
import com.bnk.domain.admin.dto.response.DashboardResponse;
import com.bnk.global.response.PageResponse;

@Service
public class AdminUserService {

	public DashboardResponse getDashboard() {
		// TODO Auto-generated method stub
		return null;
	}

	public AdminUserResponse getUserDetail(Long userId, Long adminId) {
		// TODO Auto-generated method stub
		return null;
	}

	public PageResponse<AdminUserResponse> getUserList(AdminUserSearchRequest request, Long adminId) {
		// TODO Auto-generated method stub
		return null;
	}

}
