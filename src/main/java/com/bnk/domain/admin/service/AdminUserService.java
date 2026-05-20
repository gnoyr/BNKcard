package com.bnk.domain.admin.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bnk.domain.admin.dto.request.AdminUserSearchRequest;
import com.bnk.domain.admin.dto.response.AdminUserResponse;
import com.bnk.domain.admin.dto.response.DashboardResponse;
import com.bnk.domain.admin.mapper.AdminUserMapper;
import com.bnk.domain.admin.mapper.ApprovalMapper;
import com.bnk.domain.card.mapper.CardMapper;
import com.bnk.domain.card.model.Card;
import com.bnk.domain.user.model.User;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.response.PageResponse;
import com.bnk.global.util.MaskingUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final AdminUserMapper adminUserMapper;
    private final ApprovalMapper approvalMapper;
    private final CardMapper cardMapper;

    /**
     * B-02/B-03 관리자 대시보드
     * pendingApprovalCount / topCards(view_count DESC 10) / recentAdminLogins
     */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        // 결재 대기 건수
        int pendingCount = approvalMapper.countPendingApprovals();

        // 인기 카드 상위 10개 (findAdminCards 재사용, sort=applicationCount는 없으므로 기본 정렬)
        // CardMapper에 dashboard용 별도 쿼리가 없어 findTop3ByViewCount 확장 불가
        // AdminCardSearchRequest로 size=10 전달
        com.bnk.domain.card.dto.request.AdminCardSearchRequest dashReq =
                new com.bnk.domain.card.dto.request.AdminCardSearchRequest();
        dashReq.setSize(10);
        List<Card> topCards = cardMapper.findAdminCards(dashReq);

        List<DashboardResponse.CardRankItem> cardRankItems = topCards.stream()
                .map(c -> DashboardResponse.CardRankItem.builder()
                        .cardId(c.getCardId())
                        .cardName(c.getCardName())
                        .viewCount(c.getViewCount())
                        .applicationCount(c.getApplicationCount())
                        .build())
                .collect(Collectors.toList());

        return DashboardResponse.builder()
                .pendingApprovalCount(pendingCount)
                .topCards(cardRankItems)
                .recentAdminLogins(Collections.emptyList()) // LOGIN_HISTORIES Mapper 없음
                .build();
    }

    /**
     * B-14 관리자 회원 목록 검색
     * 다중 조건 동적 검색, phone·email 마스킹 필수, AUDIT_LOGS INSERT
     */
    @Transactional
    public PageResponse<AdminUserResponse> getUserList(AdminUserSearchRequest request, Long adminId) {
        long totalCount = adminUserMapper.countUsers(request);

        if (totalCount == 0) {
            return PageResponse.of(Collections.emptyList(), 0L, request.getPage(), request.getSize());
        }

        List<User> users = adminUserMapper.findUsers(request);

        List<AdminUserResponse> content = users.stream()
                .map(u -> AdminUserResponse.builder()
                        .userId(u.getUserId())
                        .name(u.getName())
                        .maskedEmail(MaskingUtil.maskEmail(u.getEmail()))
                        .maskedPhone(MaskingUtil.maskPhone(u.getPhone()))
                        .birthDate(u.getBirthDate())
                        .statusCode(u.getStatusCode())
                        .creditScore(u.getCreditScore())
                        .lastLoginAt(u.getLastLoginAt())
                        .createdAt(u.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        // AUDIT_LOGS INSERT (RQ-B14: 관리자 조회 이벤트 기록)
        adminUserMapper.insertAuditLog(
                "ADMIN", adminId, "USER_LIST_VIEW",
                "USERS", null, "관리자 회원 목록 조회", null);

        return PageResponse.of(content, totalCount, request.getPage(), request.getSize());
    }

    /**
     * B-15 관리자 회원 상세 조회
     * 기본정보 + AUDIT_LOGS INSERT, 개인정보 마스킹 필수
     */
    @Transactional
    public AdminUserResponse getUserDetail(Long userId, Long adminId) {
        User user = adminUserMapper.findUserDetailById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // AUDIT_LOGS INSERT (RQ-B15: admin_id 기록)
        adminUserMapper.insertAuditLog(
                "ADMIN", adminId, "USER_DETAIL_VIEW",
                "USERS", userId, "관리자 회원 상세 조회", null);

        return AdminUserResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .maskedEmail(MaskingUtil.maskEmail(user.getEmail()))
                .maskedPhone(MaskingUtil.maskPhone(user.getPhone()))
                .birthDate(user.getBirthDate())
                .statusCode(user.getStatusCode())
                .creditScore(user.getCreditScore())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                // loginHistories, agreements, applications는 별도 Mapper 없어 생략
                .loginHistories(Collections.emptyList())
                .agreements(Collections.emptyList())
                .applications(Collections.emptyList())
                .build();
    }
}