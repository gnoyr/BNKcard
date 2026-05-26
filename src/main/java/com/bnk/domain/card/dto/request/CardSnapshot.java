package com.bnk.domain.card.dto.request;

import java.util.List;

import com.bnk.domain.card.model.Card;
import com.bnk.domain.card.model.CardBenefit;
import com.bnk.domain.card.model.CardImage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @Builder
@NoArgsConstructor @AllArgsConstructor
public class CardSnapshot {
    private Card card;
    private List<CardBenefit> benefits;
    private List<CardImage> images;  // 이미지 URL 리스트
}