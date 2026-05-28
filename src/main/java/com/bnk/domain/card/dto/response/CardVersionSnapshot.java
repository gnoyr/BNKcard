package com.bnk.domain.card.dto.response;

import java.util.List;

import com.bnk.domain.card.model.Card;
import com.bnk.domain.card.model.CardBenefit;
import com.bnk.domain.card.model.CardImage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CardVersionSnapshot {
	
	private Card card;
    private List<CardBenefit> benefits;
    private List<CardImage> images;
}
