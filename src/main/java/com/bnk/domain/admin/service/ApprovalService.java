package com.bnk.domain.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.bnk.domain.admin.dto.request.ApprovalActionRequest;
import com.bnk.domain.admin.dto.request.ApprovalSearchRequest;
import com.bnk.domain.admin.dto.response.ApprovalListResponse;
import com.bnk.domain.admin.mapper.ApprovalMapper;
import com.bnk.domain.card.mapper.CardMapper;
import com.bnk.global.response.PageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class ApprovalService {
	
	private final ApprovalMapper approvalMapper;
	private final CardMapper cardMapper;
	private final ObjectMapper objectMapper; // JSON 파싱용 Jackson ObjectMapper

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
