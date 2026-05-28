package com.bnk.domain.card.dto.request;

import com.bnk.domain.card.model.Card;
import com.bnk.domain.card.model.CardBenefit;
import com.bnk.domain.card.model.CardImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 카드 버전 스냅샷 DTO
 *
 * CARD_VERSIONS.snapshot_json 컬럼에 Jackson으로 직렬화되어 저장.
 * 결재 승인 시 이 JSON을 역직렬화하여 CARDS 테이블을 복원.
 *
 * 변경 이력:
 *  - Card, CardBenefit, CardImage 모두 model2 → model 패키지로 통합 완료
 *    → 이 클래스의 import 경로는 이미 model을 가리키므로 변경 없음
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardSnapshot {

    private Card              card;
    private List<CardBenefit> benefits;
    private List<CardImage>   images;
}
