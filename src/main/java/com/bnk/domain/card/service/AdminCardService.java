package com.bnk.domain.card.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.bnk.domain.card.dto.request.AdminCardSearchRequest;
import com.bnk.domain.card.dto.request.CardCreateRequest;
import com.bnk.domain.card.dto.request.CardUpdateRequest;
import com.bnk.global.response.PageResponse;

import jakarta.validation.Valid;

@Service
@Validated
public class AdminCardService {

	public Map<String, Long> createCard(@Valid CardCreateRequest request, Long adminId) {
		// TODO Auto-generated method stub
		return null;
	}

	public PageResponse<?> getAdminCardList(AdminCardSearchRequest request, Long adminId) {
		// TODO Auto-generated method stub
		return null;
	}

	public Map<String, Long> updateCard(Long cardId, @Valid CardUpdateRequest request, Long adminId) {
		// TODO Auto-generated method stub
		return null;
	}

}
