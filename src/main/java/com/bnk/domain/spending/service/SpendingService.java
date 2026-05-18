package com.bnk.domain.spending.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.bnk.domain.spending.dto.request.AiChatRequest;
import com.bnk.domain.spending.dto.request.SpendingPatternRequest;
import com.bnk.domain.spending.dto.response.AiChatResponse;
import com.bnk.domain.spending.dto.response.SpendingChartResponse;

import jakarta.validation.Valid;

@Service
@Validated
public class SpendingService {

	public AiChatResponse chat(@Valid AiChatRequest request, Long userId) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<SpendingChartResponse> getMySpendingPatterns(Long userId) {
		// TODO Auto-generated method stub
		return null;
	}

	public int updateSpendingPatterns(Long userId, @Valid SpendingPatternRequest request) {
		// TODO Auto-generated method stub
		return 0;
	}
	
}
