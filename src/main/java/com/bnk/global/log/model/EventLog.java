package com.bnk.global.log.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventLog {
	private Long logId;
	private String eventType;
	private String eventStatus;
	private Long userId;
	private String requestIp;
	private Long durationMs;
	private LocalDateTime createdAt;
}