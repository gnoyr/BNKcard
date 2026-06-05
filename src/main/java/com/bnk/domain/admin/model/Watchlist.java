package com.bnk.domain.admin.model;

import lombok.*;
import java.time.LocalDateTime;

/**
 * WATCHLIST 테이블 모델. 미가입 요주의 인물 사전 차단 목록. 회원가입 시 CI값(우선) 또는 이름+생년월일로 대조.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Watchlist {
	private Long watchlistId;
	private String name;
	private String birthDate; // AES 복호화 후 "YYYY-MM-DD"
	private String ciValue; // AES 복호화 후 원본 CI값
	private String reason;
	private String riskLevel; // HIGH / MEDIUM
	private LocalDateTime registeredAt;
	private Long registeredBy;
	private String deletedYn;
}
