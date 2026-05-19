package com.bnk.domain.card.dto.response2;

import java.util.List;

import com.bnk.domain.card.model2.Card;
import com.bnk.domain.card.model2.CardBenefit;
import com.bnk.domain.card.model2.CardImage;

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
