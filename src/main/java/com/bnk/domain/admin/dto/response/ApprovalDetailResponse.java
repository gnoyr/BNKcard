package com.bnk.domain.admin.dto.response;

import com.bnk.domain.card.dto.CardSnapshot;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 결재 상세 조회 응답 DTO
 */
@Getter
@Builder
public class ApprovalDetailResponse {

	private Long approvalId;
	private String requestTypeCode; // CARD_PUBLISH / CARD_UPDATE / TERMS_PUBLISH
	private String requesterName;
	private Long targetId; // version_id
	private String targetName; // 카드명 or 약관명 (JOIN으로 조회)
	private String statusCode; // PENDING / APPROVED / REJECTED
	private String requestComment;
	private LocalDateTime requestedAt;
	private LocalDateTime completedAt;

	private CardSnapshot snapshot;
	private List<ApprovalLineItem> lines;

	@Getter
	@Builder
	public static class ApprovalLineItem {
		private Long approvalLineId;
		private Integer approvalOrder;
		private String approverName;
		private String statusCode; // PENDING / APPROVED / REJECTED
		private String commentText;
		private LocalDateTime approvedAt;
		private boolean isCurrentUser; // 현재 로그인 관리자가 처리해야 할 라인인지
	}
}
