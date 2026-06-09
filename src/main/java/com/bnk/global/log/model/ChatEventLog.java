package com.bnk.global.log.model;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ChatEventLog {
    private Long    logId;
    private String  sessionId;
    private String  queryText;
    private Integer qdrantHitCount;
    private Double  topScore;
    private String  usedFallback;  // Y / N
    private String  errorMessage;
}