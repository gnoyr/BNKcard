package com.bnk.domain.card.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.bnk.domain.card.dto.request.CardCompareRequest;
import com.bnk.domain.card.dto.request.CardSearchRequest;
import com.bnk.domain.card.dto.request.CardSimulationRequest;
import com.bnk.domain.card.dto.response.BannerDto;
import com.bnk.domain.card.dto.response.CardCompareResponse;
import com.bnk.domain.card.dto.response.CardDetailResponse;
import com.bnk.domain.card.dto.response.CardListResponse;
import com.bnk.domain.card.dto.response.SimulationResponse;
import com.bnk.global.response.PageResponse;

import jakarta.validation.Valid;

@Service
@Validated
public class CardService {

	public List<CardCompareResponse> compareCards(@Valid CardCompareRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<BannerDto> getHomeBanners(Long userId) {
		// TODO Auto-generated method stub
		return null;
	}

	public CardDetailResponse getCardDetail(Long cardId) {
		// TODO Auto-generated method stub
		return null;
	}

	public PageResponse<CardListResponse> getCardList(@Valid CardSearchRequest request, Long userId) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<CardListResponse> getTop3Cards(Long userId, String surveyResult) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<SimulationResponse> simulateBenefits(@Valid CardSimulationRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

}
