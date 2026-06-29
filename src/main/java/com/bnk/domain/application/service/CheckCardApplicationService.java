package com.bnk.domain.application.service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.bnk.domain.application.dto.CheckApplicantSnapshotDto;
import com.bnk.domain.application.dto.PaymentSnapshotDto;
import com.bnk.domain.application.dto.request.CheckCardApplicationRequest;
import com.bnk.domain.application.dto.request.ScreeningResultRequest;
import com.bnk.domain.application.dto.response.CheckApplicationResponse;
import com.bnk.domain.application.mapper.CheckCardApplicationMapper;
import com.bnk.domain.application.mapper.UserCardMapper;
import com.bnk.domain.application.model.CheckCardApplication;
import com.bnk.domain.application.model.UserCard;
import com.bnk.domain.application.policy.CheckCardLimitContext;
import com.bnk.domain.application.policy.CheckCardLimitPolicy;
import com.bnk.domain.card.mapper.CardImageMapper;
import com.bnk.domain.card.mapper.CardMapper;
import com.bnk.domain.card.model.Card;
import com.bnk.domain.card.model.CardImage;
import com.bnk.domain.terms.mapper.TermsMapper;
import com.bnk.domain.terms.mapper.UserTermsAgreementMapper;
import com.bnk.domain.terms.model.Terms;
import com.bnk.domain.terms.model.UserTermsAgreement;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.util.AesCryptoUtil;
import com.bnk.global.util.MaskingUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;

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
    private final RestTemplate restTemplate;
    private final AesCryptoUtil aesCryptoUtil;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final CheckCardLimitService checkCardLimitService;

    @Value("${verification.server.url}")
    private String verificationServerUrl;

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
                            .orElseThrow(() -> new BusinessException(ErrorCode.TERMS_NOT_FOUND));

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
    public String verifyIdentity(CheckCardApplicationRequest request) {
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(
            verificationServerUrl + "/api/mydata/id-verification",
            Map.of(
                "appId",   request.getCheckAppId(),
                "idType",       request.getIdType(),
                "idName",       request.getIdName(),
                "idResidentNo", request.getIdResidentNo(),
                "idAddress",    request.getIdAddress(),
                "idIssueDate",  request.getIdIssueDate()
            ),
            Map.class
        );

        String idVerifiedYn = (String) response.get("idVerifiedYn");
        String ciValue      = (String) response.get("ciValue");

        CheckCardApplication application = CheckCardApplication.builder()
                .checkAppId(request.getCheckAppId())
                .idVerifiedYn(idVerifiedYn)
                .ciValue(ciValue)
                .build();

        int updated = checkCardApplicationMapper.updateIdVerified(application);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.APP_NOT_FOUND);
        }

        return idVerifiedYn;
    }

    // ----------------------------------------------------------------
    // STEP 3 - 기본정보 저장
    // ----------------------------------------------------------------
    public void saveApplicantInfo(CheckCardApplicationRequest request) {
        validateIdVerified(request.getCheckAppId());

        try {
            CheckCardApplication application = CheckCardApplication.builder()
                    .checkAppId(request.getCheckAppId())
                    .applicantSnapshot(objectMapper.writeValueAsString(request.getApplicantSnapshot()))
                    .build();
            checkCardApplicationMapper.updateApplicantInfo(application);
        } catch (Exception e) {
            throw new RuntimeException("applicantSnapshot 직렬화 실패", e);
        }
    }

    // ----------------------------------------------------------------
    // STEP 4 - 신청정보 저장 + 신청 완료(REQUESTED)
    // ----------------------------------------------------------------
    public void submitApplication(CheckCardApplicationRequest request) {
        validateIdVerified(request.getCheckAppId());

        // 신청 시점 현재 PUBLISHED 버전 조회
        Long versionId = checkCardApplicationMapper.findCurrentVersionId(request.getCardId());

        try {
            CheckCardApplication application = CheckCardApplication.builder()
                    .checkAppId(request.getCheckAppId())
                    .versionId(versionId)
                    .paymentSnapshot(objectMapper.writeValueAsString(request.getPaymentSnapshot()))
                    .linkedAccountId(request.getLinkedAccountId())
                    .cardPasswordHash(passwordEncoder.encode(request.getCardPassword()))
                    .build();
            // applicantSnapshot { name, nameEn, mobileNo, address, email, jobType, transactionPurpose, fundSource }
            // paymentSnapshot { card_brand, card_design_id, payment_day, combined_transit_yn, tx_alert_type, statement_method }
            checkCardApplicationMapper.updatePaymentInfo(application);
        } catch (Exception e) {
            throw new RuntimeException("snapshot 직렬화 실패", e);
        }
       
        requestScreeningReview(request.getCheckAppId());  // 심사 의뢰
    }
    
    private void requestScreeningReview(Long checkAppId) {
        try {
            CheckCardApplication app = findOrThrow(checkAppId);

            restTemplate.postForEntity(
                verificationServerUrl + "/review/request/check/" + checkAppId,
                Map.of(
                    "checkAppId", checkAppId,
                    "ciValue",    app.getCiValue()
                ),
                Void.class
            );
        } catch (Exception e) {
            log.error("[체크카드] 심사 의뢰 실패: checkAppId={}", checkAppId, e);
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
        	throw new BusinessException(ErrorCode.NOT_APPROVED_STATUS);
        }

        LocalDate issueDate  = LocalDate.now();
        LocalDate expireDate = issueDate.plusYears(10);

        // 16자리 랜덤 카드번호 생성
        long min = 1_000_000_000_000_000L;
        long max = 9_999_999_999_999_999L;
        long rawNumber = min + (long)(SECURE_RANDOM.nextDouble() * (max - min + 1));
        String rawCardNumber = String.format("%016d", rawNumber);

        // payment_snapshot 파싱
        PaymentSnapshotDto snap;
        try { 
            snap = objectMapper.readValue(app.getPaymentSnapshot(), PaymentSnapshotDto.class);
        } catch (Exception e) {
            throw new RuntimeException("paymentSnapshot 역직렬화 실패", e);
        }
 
        UserCard userCard = new UserCard();
 
        // ── 식별자
        userCard.setUserId(app.getUserId());
        userCard.setVersionId(app.getVersionId());
        userCard.setCheckAppId(app.getCheckAppId());  // 체크카드 전용
 
        userCard.setEncryptedCardNumber(aesCryptoUtil.encrypt(rawCardNumber));
        userCard.setMaskedCardNumber(MaskingUtil.maskCardNumber(rawCardNumber));
 
        userCard.setIssueDate(issueDate);
        userCard.setExpireDate(expireDate);
        userCard.setCardStatus("ACTIVE");
        userCard.setUsableYn("Y");
        userCard.setCardPasswordHash(app.getCardPasswordHash());
        userCard.setLinkedAccountId(app.getLinkedAccountId());
        CheckCardLimitContext limitCtx = buildLimitContext(app);
        CheckCardLimitPolicy  policy   = checkCardLimitService.determineLimit(limitCtx);

        userCard.setDailyLimitAmount(policy.getDailyLimit());
        userCard.setMonthlyLimitAmount(policy.getMonthlyLimit());

        log.info("[체크카드] 한도 산정: policy={}, daily={}, monthly={}",
                policy.getDescription(),
                policy.getDailyLimit(),
                policy.getMonthlyLimit());
 
        // ── payment_snapshot 항목
        userCard.setCardBrand(snap.getCardBrand());
        userCard.setCardDesignId(snap.getCardDesignId() != null
            ? Long.parseLong(snap.getCardDesignId()) : null);
        userCard.setPaymentDay(snap.getPaymentDay());
        userCard.setCombinedTransitYn(snap.getCombinedTransitYn());
        userCard.setTxAlertType(snap.getTxAlertType());
        userCard.setStatementMethod(snap.getStatementMethod());
 
        userCardMapper.insertUserCard(userCard);
        checkCardApplicationMapper.updateStatus(checkAppId, "ISSUED");
 
        log.info("[체크카드] 발급 완료: checkAppId={}, userCardId={}", checkAppId, userCard.getUserCardId());
    }
    
    

    // ----------------------------------------------------------------
    // 사용자 조회
    // ---------------------------------------------------------------- 
    private final CardMapper      cardMapper;
    private final CardImageMapper cardImageMapper;
    
    // 신청 단건 조회(상세)
    public CheckApplicationResponse findOne(Long checkAppId, Long userId) {
        CheckCardApplication app = findOrThrow(checkAppId);
        if (!app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.APP_NOT_FOUND);
        }
        return toResponse(app);
    }
    
	// 내 신청 목록 조회
    public List<CheckApplicationResponse> findMyApplications(Long userId) {
        List<CheckCardApplication> apps = checkCardApplicationMapper.findByUserId(userId);
        return apps.stream().map(this::toResponse).collect(Collectors.toList());
    }
    
    // ----------------------------------------------------------------
    // 임시 저장
    // ---------------------------------------------------------------- 
    public CheckApplicationResponse findDraftByCardId(Long cardId, Long userId) {
        CheckCardApplication app = checkCardApplicationMapper.findDraftByCardIdAndUserId(cardId, userId);
        if (app == null) return null;
        return toResponse(app);
    }
    
    // ----------------------------------------------------------------
    // private helpers
    // ----------------------------------------------------------------
    private void validateIdVerified(Long checkAppId) {
        CheckCardApplication app = findOrThrow(checkAppId);
        if (!"Y".equals(app.getIdVerifiedYn())) {
        	throw new BusinessException(ErrorCode.IDENTITY_NOT_VERIFIED);
        }
    }

    private CheckCardApplication findOrThrow(Long checkAppId) {
        CheckCardApplication app = checkCardApplicationMapper.findById(checkAppId);
        if (app == null) {
        	throw new BusinessException(ErrorCode.APP_NOT_FOUND);
        }
        return app;
    }
    
    private CheckApplicationResponse toResponse(CheckCardApplication app) {
        try {
            Card card = cardMapper.findById(app.getCardId());

            PaymentSnapshotDto paymentSnapshot = app.getPaymentSnapshot() != null
                    ? objectMapper.readValue(app.getPaymentSnapshot(), PaymentSnapshotDto.class)
                    : null;

            String cardImageUrl = null;
            if (paymentSnapshot != null && paymentSnapshot.getCardDesignId() != null) {
                CardImage selectedImage = cardImageMapper.findByImageId(Long.parseLong(paymentSnapshot.getCardDesignId()));
                cardImageUrl = selectedImage != null ? selectedImage.getImageUrl() : null;
            }

            CheckApplicantSnapshotDto applicantSnapshot = app.getApplicantSnapshot() != null
                    ? objectMapper.readValue(app.getApplicantSnapshot(), CheckApplicantSnapshotDto.class)
                    : null;

            return CheckApplicationResponse.builder()
                    .checkAppId(app.getCheckAppId())
                    .cardId(app.getCardId())
                    .cardName(card != null ? card.getCardName() : null)
                    .cardImageUrl(cardImageUrl)
                    .applicationStatus(app.getApplicationStatus())
                    .idVerifiedYn(app.getIdVerifiedYn())
                    .applicantSnapshot(applicantSnapshot)
                    .paymentSnapshot(paymentSnapshot)
                    .rejectionReason(app.getRejectionReason())
                    .appliedAt(app.getAppliedAt())
                    .createdAt(app.getCreatedAt())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("snapshot 역직렬화 실패", e);
        }
    }
    
    private CheckCardLimitContext buildLimitContext(CheckCardApplication app) {

        // 생년월일로 만 나이 계산
        // applicantSnapshot에서 생년월일 파싱 필요
        int age = calculateAge(app);

        // 계좌 상태 조회
        String accountStatus = checkCardApplicationMapper
                .findAccountStatus(app.getLinkedAccountId());

        // 우수 거래 조건 조회 (각 Mapper에서 조회)
        // 실제 구현 시 각 조건별 Mapper 메서드 추가 필요
        return CheckCardLimitContext.builder()
                .age(age)
                .accountStatus(accountStatus != null ? accountStatus : "NORMAL")
                // 우수 조건 (현재는 false로 초기화, 추후 실제 조회로 교체)
                .hasSalaryTransfer(false)
                .hasAutoTransfer3M(false)
                .hasCardUsage3M(false)
                .hasSavingsProduct(false)
                // 한도제한 완화 조건
                .hasSalaryTransfer1M(false)
                .hasAutoTransfer3Count(false)
                .hasSavingsAutoTransfer3(false)
                .hasCardUsage3M2(false)
                .build();
    }

    private int calculateAge(CheckCardApplication app) {
        try {
            CheckApplicantSnapshotDto snap = objectMapper.readValue(
                    app.getApplicantSnapshot(), CheckApplicantSnapshotDto.class);
            // snapshot에 birthDate 있으면 계산
            if (snap.getBirthDate() != null) {
                LocalDate birth = LocalDate.parse(snap.getBirthDate());
                return LocalDate.now().getYear() - birth.getYear()
                        - (LocalDate.now().getDayOfYear() < birth.getDayOfYear() ? 1 : 0);
            }
        } catch (Exception e) {
            log.warn("[체크카드] 생년월일 파싱 실패, 성인 기본 한도 적용");
        }
        // 파싱 실패 시 성인 기본값
        return 19;
    }
    


}