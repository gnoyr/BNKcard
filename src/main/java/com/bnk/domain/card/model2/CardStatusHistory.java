package com.bnk.domain.card.model2;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardStatusHistory {

    private Long          historyId;
    private Long          cardId;
    private String        previousStatus;
    private String        changedStatus;
    private Long          changedBy;
    private String        changedReason;
    private LocalDateTime changedAt;
}
