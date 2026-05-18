package com.bnk.domain.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.bnk.domain.admin.dto.request.ApprovalActionRequest;
import com.bnk.domain.admin.dto.request.ApprovalSearchRequest;
import com.bnk.domain.admin.dto.response.ApprovalListResponse;
import com.bnk.global.response.PageResponse;

import jakarta.validation.Valid;

@Service
@Validated
public class ApprovalService {

	public void approve(Long approvalId, @Valid ApprovalActionRequest request, Long adminId) {
		// TODO Auto-generated method stub
		
	}

	public PageResponse<ApprovalListResponse> getApprovals(ApprovalSearchRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	public void reject(Long approvalId, @Valid ApprovalActionRequest request, Long adminId) {
		// TODO Auto-generated method stub
		
	}

}
