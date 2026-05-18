package com.bnk.domain.admin.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ApprovalActionRequest {

    /**
     * 승인(B-07): 선택, 반려(B-08): 필수.
     * 반려 시 null이면 서비스단에서 BusinessException(REJECT_COMMENT_REQUIRED).
     */
    @Size(max = 2000, message = "의견은 2000자 이내로 입력해주세요.")
    private String comment;
}
