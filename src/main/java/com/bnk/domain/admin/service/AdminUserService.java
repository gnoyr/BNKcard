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
import com.bnk.domain.card.dto.request.AdminCardSearchRequest;
import com.bnk.domain.card.mapper.CardMapper;
import com.bnk.domain.card.model.Card;
import com.bnk.domain.user.model.User;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.response.PageResponse;
import com.bnk.global.util.MaskingUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final AdminUserMapper adminUserMapper;
    private final ApprovalMapper  approvalMapper;
    private final CardMapper      cardMapper;

    /** 대시보드 최근 관리자 로그인 표시 건수 */
    private static final int DASHBOARD_LOGIN_LIMIT = 5;

    /** 회원 상세 로그인 이력 표시 건수 */
    private static final int DETAIL_LOGIN_LIMIT = 5;

    // ================================================================
    // B-02/B-03 | 관리자 대시보드
    // ================================================================

    /**
     * pendingApprovalCount  : APPROVAL_REQUESTS.status='PENDING' COUNT
     * topCards              : view_count DESC 상위 10개
     * recentAdminLogins     : LOGIN_HISTORIES JOIN ADMIN_USERS 최근 5건 (성공만)
     */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {

        // ① 결재 대기 건수
        int pendingCount = approvalMapper.countPendingApprovals();

        // ② 인기 카드 상위 10 — findTop3ByViewCount 대신 전용 쿼리 사용
        //    CardMapper에 findTop3ByViewCount 이미 있으므로 그대로 재활용
        //    (3개만 반환되므로 대시보드용으로 적절)
        List<Card> topCardList = cardMapper.findTop3ByViewCount();

        List<DashboardResponse.CardRankItem> topCards = topCardList.stream()
                .map(c -> DashboardResponse.CardRankItem.builder()
                        .cardId(c.getCardId())
                        .cardName(c.getCardName())
                        .viewCount(c.getViewCount())
                        .applicationCount(c.getApplicationCount())
                        .build())
                .collect(Collectors.toList());

        // ③ 관리자 로그인 이력
        List<DashboardResponse.LoginHistoryItem> logins =
                adminUserMapper.findRecentAdminLogins(DASHBOARD_LOGIN_LIMIT).stream()
                        .map(r -> DashboardResponse.LoginHistoryItem.builder()
                                .adminName(r.getAdminName())
                                .loginAt(r.getLoginAt())
                                .ipAddress(r.getIpAddress())
                                .build())
                        .collect(Collectors.toList());

        // ④ 카드 현황
        long totalCards     = adminUserMapper.countCardsByStatus(null);
        long publishedCards = adminUserMapper.countCardsByStatus("PUBLISHED");
        long draftCards     = adminUserMapper.countCardsByStatus("DRAFT");

        // ⑤ 회원 현황
        long totalUsers   = adminUserMapper.countUsersByStatus(null);
        long lockedUsers  = adminUserMapper.countUsersByStatus("LOCKED");
        long todaySignups = adminUserMapper.countTodaySignups();

        // ⑥ 약관 현황
        long totalTerms     = adminUserMapper.countTermsByStatus(null);
        long publishedTerms = adminUserMapper.countTermsByStatus("PUBLISHED");

        return DashboardResponse.builder()
                .pendingApprovalCount(pendingCount)
                .topCards(topCards)
                .recentAdminLogins(logins)
                .totalCards(totalCards)
                .publishedCards(publishedCards)
                .draftCards(draftCards)
                .totalUsers(totalUsers)
                .lockedUsers(lockedUsers)
                .todaySignups(todaySignups)
                .totalTerms(totalTerms)
                .publishedTerms(publishedTerms)
                .build();
    }

    // ================================================================
    // B-14 | 관리자 회원 목록 검색
    // ================================================================

    @Transactional
    public PageResponse<AdminUserResponse> getUserList(AdminUserSearchRequest request,
                                                       Long adminId) {
        long totalCount = adminUserMapper.countUsers(request);

        if (totalCount == 0) {
            return PageResponse.of(Collections.emptyList(), 0L,
                    request.getPage(), request.getSize());
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

        adminUserMapper.insertAuditLog(
                "ADMIN", adminId, "USER_LIST_VIEW",
                "USERS", null, "관리자 회원 목록 조회", null);

        return PageResponse.of(content, totalCount, request.getPage(), request.getSize());
    }

    // ================================================================
    // B-15 | 관리자 회원 상세 조회
    // ================================================================

    /**
     * 기본정보 + 로그인 이력(5건) + 약관 동의 이력 + 카드 신청 이력
     * AUDIT_LOGS INSERT (admin_id 기록) / 개인정보 마스킹 필수
     */
    @Transactional
    public AdminUserResponse getUserDetail(Long userId, Long adminId) {

        User user = adminUserMapper.findUserDetailById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // ① 로그인 이력 최근 5건 (LOGIN_HISTORIES)
        List<AdminUserResponse.LoginHistoryItem> loginHistories =
                adminUserMapper.findLoginHistoriesByUserId(userId, DETAIL_LOGIN_LIMIT)
                        .stream()
                        .map(r -> AdminUserResponse.LoginHistoryItem.builder()
                                .loginAt(r.getLoginAt())
                                .loginResultCode(r.getLoginResultCode())
                                .ipAddress(r.getIpAddress())
                                .build())
                        .collect(Collectors.toList());

        // ② 약관 동의 이력 전체 (USER_TERMS_AGREEMENTS)
        List<AdminUserResponse.AgreementItem> agreements =
                adminUserMapper.findAgreementsByUserId(userId)
                        .stream()
                        .map(r -> AdminUserResponse.AgreementItem.builder()
                                .termsId(r.getTermsId())
                                .agreedYn(r.getAgreedYn())
                                .agreedAt(r.getAgreedAt())
                                .build())
                        .collect(Collectors.toList());

        // ③ 카드 신청 이력 전체 (CARD_APPLICATIONS JOIN CARDS)
        List<AdminUserResponse.ApplicationItem> applications =
                adminUserMapper.findApplicationsByUserId(userId)
                        .stream()
                        .map(r -> AdminUserResponse.ApplicationItem.builder()
                                .cardId(r.getCardId())
                                .cardName(r.getCardName())
                                .applicationStatus(r.getApplicationStatus())
                                .appliedAt(r.getAppliedAt())
                                .build())
                        .collect(Collectors.toList());

        // ④ AUDIT_LOGS INSERT
        adminUserMapper.insertAuditLog(
                "ADMIN", adminId, "USER_DETAIL_VIEW",
                "USERS", userId, "관리자 회원 상세 조회", null);

        return AdminUserResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .maskedEmail(MaskingUtil.maskEmail(user.getEmail()))
                .maskedPhone(MaskingUtil.maskPhone(user.getPhone()))
                .birthDate(user.getBirthDate())
                .job(user.getJob())                                          // 추가
                .incomeLevelCode(user.getIncomeLevelCode())                  // 추가
                .statusCode(user.getStatusCode())
                .creditScore(user.getCreditScore())
                .loginFailCount(user.getLoginFailCount())                    // 추가
                .lockedUntil(user.getLockedUntil())                          // 추가
                .lastLoginAt(user.getLastLoginAt())
                .lastPasswordChangedAt(user.getLastPasswordChangedAt())      // 추가
                .isEmailVerified(user.getIsEmailVerified())                  // 추가
                .isPhoneVerified(user.getIsPhoneVerified())                  // 추가
                .pushEnabled(user.getPushEnabled())                          // 추가
                .marketingAgree(user.getMarketingAgree())                    // 추가
                .privacyAgree(user.getPrivacyAgree())                        // 추가
                .dormantAt(user.getDormantAt())                              // 추가
                .withdrawnAt(user.getWithdrawnAt())                          // 추가
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())                              // 추가
                .deletedYn(user.getDeletedYn())                             // 추가
                .loginHistories(loginHistories)
                .agreements(agreements)
                .applications(applications)
                .build();
    }

    // ================================================================
    // 계정 잠금 강제 해제 (기존 그대로)
    // ================================================================

    @Transactional
    public void unlockUser(Long userId, Long adminId) {
        int affected = adminUserMapper.unlockUser(userId);
        if (affected == 0) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND,
                    "잠금 해제 대상 유저를 찾을 수 없습니다. userId=" + userId);
        }
        adminUserMapper.insertAuditLog(
                "ADMIN", adminId, "UNLOCK_USER",
                "USER", userId, "계정 잠금 강제 해제", null);
        log.info("[계정잠금해제] adminId={} → userId={}", adminId, userId);
    }
    
    
    // ================================================================
    // 회원 상태 변경
    // ================================================================    
    @Transactional
    public void changeUserStatus(Long userId, String statusCode, Long adminId) {
        int affected = adminUserMapper.updateUserStatus(userId, statusCode);
        if (affected == 0) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND,
                    "상태 변경 대상 유저를 찾을 수 없습니다. userId=" + userId);
        }
        adminUserMapper.insertAuditLog(
                "ADMIN", adminId, "USER_STATUS_CHANGE",
                "USER", userId, "회원 상태 변경: " + statusCode, null);
        log.info("[회원상태변경] adminId={} → userId={}, status={}", adminId, userId, statusCode);
    }
}