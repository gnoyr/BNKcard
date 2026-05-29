package com.bnk.domain.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class DashboardResponse {
    private long pendingApprovalCount;
    private List<CardRankItem> topCards;
    private List<LoginHistoryItem> recentAdminLogins;
    
    // ── (SUPER_ADMIN/MANAGER용) ──
    private long totalCards;
    private long publishedCards;
    private long draftCards;

    // ── (SUPER_ADMIN/OPERATOR용) ──
    private long totalUsers;
    private long lockedUsers;
    private long todaySignups;

    // ── (MANAGER용) ──
    private long totalTerms;
    private long publishedTerms;

    @Getter @Builder
    public static class CardRankItem {
        private Long cardId;
        private String cardName;
        private Long viewCount;
        private Long applicationCount;
    }

    @Getter @Builder
    public static class LoginHistoryItem {
        private String adminName;
        private LocalDateTime loginAt;
        private String ipAddress;
    }
}
