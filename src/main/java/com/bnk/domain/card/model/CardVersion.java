package com.bnk.domain.card.model;

import lombok.*;
import java.time.LocalDateTime;

/**
 * CARD_VERSIONS 테이블 모델
 *
 * 변경 이력:
 *  - 패키지 이동: com.bnk.domain.card.model2 → com.bnk.domain.card.model
 *  - 내용 변경 없음
 *  - 삭제 대상: com.bnk.domain.card.model2.CardVersion
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardVersion {
    private Long          versionId;
    private Long          cardId;
    private String        versionNo;
    private String        versionStatus;   // DRAFT / REVIEW / APPROVED / PUBLISHED / ARCHIVED
    private String        snapshotJson;    // ObjectMapper로 직렬화한 JSON
    private String        changeSummary;
    private Long          createdBy;
    private LocalDateTime createdAt;
    private Long          approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime publishedAt;
}
