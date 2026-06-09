package com.bnk.global.util.audit;

import lombok.Builder;
import lombok.Getter;

/**
 * ADMIN_ACTIVITY_LOG 테이블 매핑 모델. PK(activity_id)는 TRG_ADMIN_ACTIVITY_LOG_BI 트리거가
 * 자동 채번.
 */
@Getter
@Builder
public class AdminActivityLog {

	private Long activityId; // PK — 트리거 자동 채번
	private Long adminId; // 행위 주체 (NOT NULL)
	private String action; // CARD_CREATE / CARD_APPROVAL_APPROVE / ADMIN_USER_STATUS_CHANGE 등
	private String result; // S=SUCCESS, F=FAILURE
	private String targetId; // 대상 리소스 ID (카드ID, 신청ID, 유저ID 등 범용 문자열)
	private String detail; // 성공·실패 상세 메시지
	private String clientIp;
	private String requestUri;
}
