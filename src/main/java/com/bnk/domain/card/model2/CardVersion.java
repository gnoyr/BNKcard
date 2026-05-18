package com.bnk.domain.card.model2;
import lombok.*;
import java.time.LocalDateTime;

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
