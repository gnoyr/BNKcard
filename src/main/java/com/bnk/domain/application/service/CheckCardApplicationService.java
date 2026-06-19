package com.bnk.domain.application.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bnk.domain.application.dto.request.CheckCardApplicationRequest;
import com.bnk.domain.application.dto.request.ScreeningResultRequest;
import com.bnk.domain.application.mapper.CheckCardApplicationMapper;
import com.bnk.domain.application.mapper.UserCardMapper;
import com.bnk.domain.application.model.CheckCardApplication;
import com.bnk.domain.application.model.UserCard;
import com.bnk.domain.terms.mapper.TermsMapper;
import com.bnk.domain.terms.mapper.UserTermsAgreementMapper;
import com.bnk.domain.terms.model.Terms;
import com.bnk.domain.terms.model.UserTermsAgreement;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckCardApplicationService {

    private final CheckCardApplicationMapper checkCardApplicationMapper;
    private final UserTermsAgreementMapper   userTermsAgreementMapper;
    private final UserCardMapper             userCardMapper;
    private final TermsMapper 				 termsMapper;
    private final PasswordEncoder            passwordEncoder;
    private final ObjectMapper               objectMapper;

    // ----------------------------------------------------------------
    // STEP 1 - 약관 동의
    // ----------------------------------------------------------------
    @Transactional
    public Long agreeTerms(Long userId, CheckCardApplicationRequest request) {
        CheckCardApplication application = CheckCardApplication.builder()
                .userId(userId)
                .cardId(request.getCardId())
                .build();
        checkCardApplicationMapper.insertApplication(application);

        Long checkAppId = application.getCheckAppId();

        List<UserTermsAgreement> agreements = request.getAgreedTerms().stream()
                .map(agreedItem -> {
                    Terms terms = termsMapper.findById(agreedItem.getTermsId())
                            .orElseThrow(() -> new IllegalArgumentException("약관을 찾을 수 없습니다: termsId=" + agreedItem.getTermsId()));

                    return UserTermsAgreement.builder()
                            .userId(userId)
                            .termsId(agreedItem.getTermsId())
                            .agreedYn(agreedItem.getAgreedYn())
                            .agreementAction("AGREE")
                            .agreedVersion(terms.getVersion())
                            .agreementSource("CHECK_CARD_APPLY")
                            .agreedContentSnapshot(terms.getContentHtml())
                            .checkAppId(checkAppId)
                            .build();
                })
                .collect(Collectors.toList());

        userTermsAgreementMapper.insertAgreements(agreements);

        return checkAppId;
    }

    // ----------------------------------------------------------------
    // STEP 2 - 본인확인 결과 저장
    // ----------------------------------------------------------------
    public void verifyIdentity(Long checkAppId, String idVerifiedYn) {
        int updated = checkCardApplicationMapper.updateIdVerified(checkAppId, idVerifiedYn);
        if (updated == 0) {
            throw new IllegalStateException("본인확인 실패: checkAppId=" + checkAppId);
        }
    }

    // ----------------------------------------------------------------
    // STEP 3 - 계좌 선택
    // ----------------------------------------------------------------
    public void saveLinkedAccount(CheckCardApplicationRequest request) {
        validateIdVerified(request.getCheckAppId());
        checkCardApplicationMapper.updateLinkedAccount(request.getCheckAppId(), request.getLinkedAccountId());
    }

    // ----------------------------------------------------------------
    // STEP 4 - 기본정보 + 신청정보 저장 + 신청 완료(REQUESTED)
    // ----------------------------------------------------------------
    public void submitApplication(CheckCardApplicationRequest request) {
        validateIdVerified(request.getCheckAppId());

        // 신청 시점 현재 PUBLISHED 버전 조회
        Long versionId = checkCardApplicationMapper.findCurrentVersionId(request.getCardId());

        try {
            CheckCardApplication application = CheckCardApplication.builder()
                    .checkAppId(request.getCheckAppId())
                    .versionId(versionId)
                    .applicantSnapshot(objectMapper.writeValueAsString(request.getApplicantSnapshot()))
                    .paymentSnapshot(objectMapper.writeValueAsString(request.getPaymentSnapshot()))
                    .cardPasswordHash(passwordEncoder.encode(request.getCardPassword()))
                    .build();
            // applicantSnapshot { name, nameEn, mobileNo, address, email, jobType, transactionPurpose, fundSource }
            // paymentSnapshot { card_brand, card_design_id, payment_day, combined_transit_yn, tx_alert_type, statement_method, overseas_dcc_block_yn }
            checkCardApplicationMapper.updatePaymentInfo(application);
        } catch (Exception e) {
            throw new RuntimeException("snapshot 직렬화 실패", e);
        }
    }

    // ----------------------------------------------------------------
    // STEP 5 - 심사 결과 저장 (심사서버가 결과 전달)
    // ----------------------------------------------------------------
    public void saveScreeningResult(ScreeningResultRequest request) {
        CheckCardApplication application = CheckCardApplication.builder()
                .checkAppId(request.getAppId())
                .applicationStatus(request.getApplicationStatus())
                .rejectionReason(request.getRejectionReason())
                .reviewedBy(request.getReviewedBy())
                .build();
        checkCardApplicationMapper.updateReviewResult(application);

        // APPROVED → 자동 발급
        if ("APPROVED".equals(request.getApplicationStatus())) {
            issueCard(request.getAppId());
        }
    }

    // ----------------------------------------------------------------
    //  APPROVED 시 자동 발급
    // ----------------------------------------------------------------
    @Transactional
    public void issueCard(Long checkAppId) {
        CheckCardApplication app = findOrThrow(checkAppId);

        if (!"APPROVED".equals(app.getApplicationStatus())) {
            throw new IllegalStateException("승인 상태가 아닙니다: checkAppId=" + checkAppId);
        }

        LocalDate issueDate  = LocalDate.now();
        LocalDate expireDate = issueDate.plusYears(10);

        // 16자리 랜덤 카드번호 생성
        String cardNumber = String.format("%016d",
            (long)(Math.random() * 9_000_000_000_000_000L) + 1_000_000_000_000_000L);

        UserCard userCard = new UserCard();
        userCard.setUserId(app.getUserId());
        userCard.setVersionId(app.getVersionId());
        userCard.setCheckAppId(app.getCheckAppId());
        userCard.setCardPasswordHash(app.getCardPasswordHash());
        userCard.setCardNumber(cardNumber);
        userCard.setIssueDate(issueDate);
        userCard.setExpireDate(expireDate);
        userCard.setCardStatus("ACTIVE");
        userCardMapper.insertUserCard(userCard);

        checkCardApplicationMapper.updateStatus(checkAppId, "ISSUED");

        log.info("[체크카드] 발급 완료: checkAppId={}, userCardId={}", checkAppId, userCard.getUserCardId());
    }

    // ----------------------------------------------------------------
    // private helpers
    // ----------------------------------------------------------------
    private void validateIdVerified(Long checkAppId) {
        CheckCardApplication app = findOrThrow(checkAppId);
        if (!"Y".equals(app.getIdVerifiedYn())) {
            throw new IllegalStateException("본인확인이 완료되지 않았습니다: checkAppId=" + checkAppId);
        }
    }

    private CheckCardApplication findOrThrow(Long checkAppId) {
        CheckCardApplication app = checkCardApplicationMapper.findById(checkAppId);
        if (app == null) {
            throw new IllegalArgumentException("신청 정보를 찾을 수 없습니다: checkAppId=" + checkAppId);
        }
        return app;
    }
}