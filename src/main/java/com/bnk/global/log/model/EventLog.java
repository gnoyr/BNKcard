package com.bnk.global.log.model;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EventLog {
    private Long   logId;
    private String eventType;
    private String eventStatus;   // SUCCESS / FAILURE
    private Long   userId;
    private String requestIp;
    private Long   durationMs;
}