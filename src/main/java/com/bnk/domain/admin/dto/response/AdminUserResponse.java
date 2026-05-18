package com.bnk.domain.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AdminUserResponse {
    private Long userId;
    private String name;
    private String maskedEmail;
    private String maskedPhone;
    private LocalDate birthDate;
    private String statusCode;
    private Integer creditScore;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    // 상세 조회 추가 필드
    private List<LoginHistoryItem> loginHistories;
    private List<AgreementItem> agreements;
    private List<ApplicationItem> applications;

    @Getter @Builder
    public static class LoginHistoryItem {
        private LocalDateTime loginAt;
        private String loginResultCode;
        private String ipAddress;
    }

    @Getter @Builder
    public static class AgreementItem {
        private Long termsId;
        private String agreedYn;
        private LocalDateTime agreedAt;
    }

    @Getter @Builder
    public static class ApplicationItem {
        private Long cardId;
        private String cardName;
        private String applicationStatus;
        private LocalDateTime appliedAt;
    }
}
