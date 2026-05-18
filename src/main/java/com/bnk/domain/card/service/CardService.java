package com.bnk.domain.card.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.bnk.domain.card.dto.request.CardCompareRequest;
import com.bnk.domain.card.dto.request.CardSearchRequest;
import com.bnk.domain.card.dto.request.CardSimulationRequest;
import com.bnk.domain.card.dto.response.BannerDto;
import com.bnk.domain.card.dto.response.CardCompareResponse;
import com.bnk.domain.card.dto.response.CardDetailResponse;
import com.bnk.domain.card.dto.response.CardListResponse;
import com.bnk.domain.card.dto.response.SimulationResponse;
import com.bnk.domain.card.mapper.CardBenefitMapper;
import com.bnk.domain.card.mapper.CardImageMapper;
import com.bnk.domain.card.mapper.CardMapper;
import com.bnk.domain.card.model.Card;
import com.bnk.domain.card.model.CardBenefit;
import com.bnk.domain.card.model.CardImage;
import com.bnk.domain.search.mapper.SearchLogMapper;
import com.bnk.domain.spending.mapper.SpendingPatternMapper;
import com.bnk.global.response.PageResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class CardService {
	
	private final CardMapper cardMapper;
    private final CardBenefitMapper cardBenefitMapper;
    private final SpendingPatternMapper spendingPatternMapper; // 추가
    private final SearchLogMapper searchLogMapper; 
	private final CardImageMapper cardImageMapper;
	/**
     * 카드 비교 (최대 3개).
     * CARD_BENEFITS JOIN, 시뮬레이션 선택.
     */
	public List<CardCompareResponse> compareCards(@Valid CardCompareRequest request) {
		// TODO Auto-generated method stub
		
		return null;
	}
	
	 /**
     * 홈 배너 조회.
     * 비로그인: view_count 상위 3개 CARD_PROMOTIONS.banner_image_url
     * 우수회원: USER_SPENDING_PATTERNS 최대 카테고리 → CARD_IMAGES(FRONT)
     * required=false → ud=null 이면 비로그인 처리
     */
	@Transactional(readOnly = true)
	public List<BannerDto> getHomeBanners(Long userId) {
		List<Card> cards;
		
		if(userId == null) {
			cards =cardMapper.findTop3ByViewCount();
		}
		else {
			Long topCategoryId = spendingPatternMapper.findTopCategoryIdByUserId(userId);
			
			if(topCategoryId != null) {
				cards =cardMapper.findTop3ByCategoryId(topCategoryId);
			}
			else {
				cards =cardMapper.findTop3ByViewCount();
			}
		}
		
		// 1. 조회된 카드들의 ID 목록만 쏙 뽑아냅니다. (ex: [1, 5, 12])
	    List<Long> cardIds = cards.stream()
	            .map(Card::getCardId)
	            .collect(Collectors.toList());

	    // 2. [핵심] 이미 만들어두신 매퍼 메서드로 FRONT 이미지들을 '한 번에' 가져옵니다.
	    // 만약 조회된 카드가 없다면 빈 리스트 처리를 해줍니다.
	    List<CardImage> frontImages = cardIds.isEmpty() ? 
	            Collections.emptyList() : cardImageMapper.findFrontImagesByCardIds(cardIds);

	    // 3. 자바 Map을 이용해 카드ID를 Key로, 이미지URL을 Value로 만들어 둡니다. (빠른 매핑을 위해)
	    Map<Long, String> imageUrlMap = frontImages.stream()
	            .collect(Collectors.toMap(CardImage::getCardId, CardImage::getImageUrl, (existing, replacement) -> existing));

	    // 4. 최종 변환 및 조립
	    return cards.stream()
	            .map(card -> BannerDto.builder()
	                    .cardId(card.getCardId())
	                    .cardName(card.getCardName())
	                    // Map에서 현재 카드의 ID에 해당하는 이미지 URL을 쏙 꺼내옵니다. 없으면 null
	                    .bannerImageUrl(imageUrlMap.get(card.getCardId()))
	                    .companyName(card.getCompanyName())
	                    .build())
	            .collect(Collectors.toList());
	}
	
	/**
     * 카드 상세 조회.
     * CARDS.view_count++ UPDATE.
     */
	public CardDetailResponse getCardDetail(Long cardId) {
		Card card = cardMapper.findById(cardId)
		        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카드입니다. cardId=" + cardId));
	 
	        // 조회수 +1
	        cardMapper.incrementViewCount(cardId);
	 
	        // 혜택 목록 조회
	        List<CardBenefit> benefits = cardBenefitMapper.findByCardId(cardId);
	        
	        //카드이미지 조회
	        List<CardImage> a =cardImageMapper.findByCardId(cardId);
	        //CardImage => ImageDto로 변경 해서 다시 List<ImageDto> 설정
	        
	        return CardDetailResponse.builder()
	                .cardId(card.getCardId())
	                .cardName(card.getCardName())
	                .cardType(card.getCardType())
	                .companyName(card.getCompanyName())
	                .annualFeeDomestic(card.getAnnualFeeDomestic())
	                .annualFeeOverseas(card.getAnnualFeeOverseas())
	                .summaryDescription(card.getSummaryDescription())
	                .benefits(benefits)
	                .build();
	}
	/**
     * 카드 목록 + 검색.
     * 검색어 있으면 SEARCH_LOGS INSERT.
     */
	public PageResponse<CardListResponse> getCardList(@Valid CardSearchRequest request, Long userId) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
     * TOP3 추천.
     * ① 비회원: view_count DESC 상위 3
     * ② 신규회원: 설문결과 기반
     * ③ 우수회원: USER_SPENDING_PATTERNS → CARD_BENEFITS JOIN
     */
	public List<CardListResponse> getTop3Cards(Long userId, String surveyResult) {
		// TODO Auto-generated method stub
		return null;
	}
	/**
     * 혜택 시뮬레이션.
     * MIN(monthlyAmount * discountRate, monthlyLimitAmount) 공식.
     */
	public List<SimulationResponse> simulateBenefits(@Valid CardSimulationRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

}
