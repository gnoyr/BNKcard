package com.bnk.domain.application.service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;

import com.bnk.domain.account.mapper.AccountMapper;
import com.bnk.domain.account.model.Account;
import com.bnk.domain.application.dto.CheckApplicantSnapshotDto;
import com.bnk.domain.application.dto.PaymentSnapshotDto;
import com.bnk.domain.application.dto.request.CheckCardApplicationRequest;
import com.bnk.domain.application.dto.request.ReviewResultRequest;
import com.bnk.domain.application.dto.request.ScreeningResultRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import com.bnk.domain.notification.service.NotificationService;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.util.AesCryptoUtil;
import com.bnk.global.util.MaskingUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;

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
    private final RestTemplate               restTemplate;
    private final AesCryptoUtil              aesCryptoUtil;
    private final AccountMapper              accountMapper;
    private final CheckCardLimitService      checkCardLimitService;
    private final NotificationService        notificationService;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${verification.server.url}")
    private String verificationServerUrl;

    // ----------------------------------------------------------------
    // STEP 1 - 약관 동의
    // ----------------------------------------------------------------
    @Transactional
    public Long agreeTerms(Long userId, CheckCardApplicationRequest request) {
        if (checkCardApplicationMapper.hasActiveApplication(userId, request.getCardId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_APPLICATION);
        }
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
    @Transactional
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
    @Transactional
    public void saveApplicantInfo(CheckCardApplicationRequest request) {
        validateIdVerified(request.getCheckAppId());

        try {
            CheckCardApplication application = CheckCardApplication.builder()
                    .checkAppId(request.getCheckAppId())
                    .applicantSnapshot(objectMapper.writeValueAsString(request.getApplicantSnapshot()))
                    .build();
            checkCardApplicationMapper.updateApplicantInfo(application);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("applicantSnapshot 직렬화 실패", e);
        }
    }

    // ----------------------------------------------------------------
    // STEP 4 - 신청정보 저장 + 신청 완료(REQUESTED)
    // ----------------------------------------------------------------
    @Transactional
    public void submitApplication(CheckCardApplicationRequest request, Long userId) {
        // H3: 이미 진행/완료된 신청 건은 재제출 불가
        CheckCardApplication existing = findOrThrow(request.getCheckAppId());
        String currentStatus = existing.getApplicationStatus();
        if ("REQUESTED".equals(currentStatus) || "REVIEWING".equals(currentStatus)
                || "APPROVED".equals(currentStatus) || "ISSUED".equals(currentStatus)
                || "REJECTED".equals(currentStatus)) {
            throw new BusinessException(ErrorCode.INVALID_APPLICATION_STATUS);
        }
        validateIdVerified(request.getCheckAppId());

        // Fix 1: 계좌 소유 및 상태 검증
        if (request.getLinkedAccountId() != null) {
            Account account = accountMapper.findByAccountId(request.getLinkedAccountId());
            if (account == null) {
                throw new BusinessException(ErrorCode.INVALID_ACCOUNT);
            }
            if (!account.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.ACCOUNT_NOT_OWNED);
            }
            if (!"ACTIVE".equals(account.getAccountStatus())) {
                throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVE);
            }
        }

        Long versionId = checkCardApplicationMapper.findCurrentVersionId(request.getCardId());

        try {
            CheckCardApplication application = CheckCardApplication.builder()
                    .checkAppId(request.getCheckAppId())
                    .versionId(versionId)
                    .paymentSnapshot(objectMapper.writeValueAsString(request.getPaymentSnapshot()))
                    .linkedAccountId(request.getLinkedAccountId())
                    .cardPasswordHash(passwordEncoder.encode(request.getCardPassword()))
                    .build();
            checkCardApplicationMapper.updatePaymentInfo(application);
        } catch (BusinessException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("snapshot 직렬화 실패", e);
        }
<<<<<<< HEAD
       
        // 심사 서버 없이 바로 APPROVED 처리
        checkCardApplicationMapper.updateStatus(request.getCheckAppId(), "APPROVED");
        issueCard(request.getCheckAppId());
=======

        // Fix 3: 심사 의뢰 비동기 처리 — DB 커밋 후 실행 보장
        final Long checkAppId = request.getCheckAppId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                CompletableFuture.runAsync(() -> {
                    try {
                        requestScreeningReview(checkAppId);
                    } catch (Exception e) {
                        log.error("[체크카드] 비동기 심사 의뢰 처리 중 예외: checkAppId={}", checkAppId, e);
                    }
                });
            }
        });
    }

    private void requestScreeningReview(Long checkAppId) {
        try {
            CheckCardApplication app = findOrThrow(checkAppId);

            ResponseEntity<ScreeningResultRequest> response = restTemplate.postForEntity(
                verificationServerUrl + "/review/request/check/" + checkAppId,
                Map.of(
                    "checkAppId", checkAppId,
                    "ciValue",    app.getCiValue()
                ),
                ScreeningResultRequest.class
            );

            if (response.getBody() != null) {
                saveScreeningResult(response.getBody());
            }

        } catch (Exception e) {
            log.error("[체크카드] 심사 의뢰 실패: checkAppId={}", checkAppId, e);
            checkCardApplicationMapper.updateStatus(checkAppId, "SCREENING_FAILED");
        }
    }

    // ----------------------------------------------------------------
    // STEP 5 - 심사 결과 저장 (심사서버가 결과 전달)
    // ----------------------------------------------------------------
    @Transactional
    public void saveScreeningResult(ScreeningResultRequest request) {
        // M2: 존재하지 않는 appId 콜백 시 silent no-op 방지
        findOrThrow(request.getAppId());
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
>>>>>>> 080b26aafa7226cb3f1e113b2c3e204b3cc9bf39
    }

    // ----------------------------------------------------------------
    // 관리자 추가 심사 결과 콜백 (체크카드)
    // ----------------------------------------------------------------
    @Transactional
    public void saveReviewResult(ReviewResultRequest request) {
        if ("REJECTED".equals(request.getApplicationStatus())) {
            CheckCardApplication application = CheckCardApplication.builder()
                    .checkAppId(request.getAppId())
                    .applicationStatus("REJECTED")
                    .rejectionReason(request.getRejectionReason())
                    .reviewedBy(request.getReviewedBy())
                    .build();
            checkCardApplicationMapper.updateReviewResult(application);
            return;
        }

        if ("APPROVED".equals(request.getApplicationStatus())) {
            // M2: 존재하지 않는 appId 콜백 시 silent no-op 방지
            CheckCardApplication app = findOrThrow(request.getAppId());
            CheckCardApplication application = CheckCardApplication.builder()
                    .checkAppId(request.getAppId())
                    .applicationStatus("APPROVED")
                    .reviewedBy(request.getReviewedBy())
                    .build();
            checkCardApplicationMapper.updateReviewResult(application);
            issueCard(request.getAppId());
            // L4: 발급 완료 알림
            try {
                notificationService.notifyReviewResult(app.getUserId(), request.getAppId(), true);
            } catch (Exception e) {
                log.error("[체크카드] 추가심사 승인 알림 발송 실패: checkAppId={}", request.getAppId(), e);
            }
        }
    }

    // ----------------------------------------------------------------
    //  APPROVED 시 자동 발급
    // ----------------------------------------------------------------
    @Transactional
    public void issueCard(Long checkAppId) {
        CheckCardApplication app = findOrThrow(checkAppId);

        // H4: 멱등성 보장 — 이미 발급된 경우 중복 발급 방지
        if ("ISSUED".equals(app.getApplicationStatus())) {
            log.warn("[체크카드] 이미 발급된 신청 건 중복 호출 차단: checkAppId={}", checkAppId);
            return;
        }
        if (!"APPROVED".equals(app.getApplicationStatus())) {
        	throw new BusinessException(ErrorCode.NOT_APPROVED_STATUS);
        }

        LocalDate issueDate  = LocalDate.now();
        LocalDate expireDate = issueDate.plusYears(10);

        // 16자리 랜덤 카드번호 생성
        long min = 1_000_000_000_000_000L;
        long max = 9_999_999_999_999_999L;
        long rawNumber = min + (SECURE_RANDOM.nextLong(max - min + 1));
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

        // H3: 나이 조건 미충족(만 12세 미만) 시 발급 거절 처리
        CheckCardLimitPolicy policy;
        try {
            policy = checkCardLimitService.determineLimit(limitCtx);
        } catch (IllegalStateException e) {
            log.warn("[체크카드] 나이 조건 미충족으로 발급 거절: checkAppId={}", checkAppId);
            CheckCardApplication rejected = CheckCardApplication.builder()
                    .checkAppId(checkAppId)
                    .applicationStatus("REJECTED")
                    .rejectionReason("나이 조건 미충족 (만 12세 미만)")
                    .build();
            checkCardApplicationMapper.updateReviewResult(rejected);
            return;
        }

        // M3: LIMIT_ACCOUNT → 일반계좌 전환 조건 충족 시 DB 반영
        if ("LIMIT_ACCOUNT".equals(limitCtx.getAccountStatus())
                && policy != CheckCardLimitPolicy.LIMIT_ACCOUNT_BASIC
                && policy != CheckCardLimitPolicy.LIMIT_ACCOUNT_RELAXED
                && app.getLinkedAccountId() != null) {
            accountMapper.updateAccountStatus(app.getLinkedAccountId(), "NORMAL");
            log.info("[체크카드] 한도제한계좌 → 일반계좌 전환: accountId={}", app.getLinkedAccountId());
        }

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
    // Fix 4: SCREENING_FAILED 재시도
    // ----------------------------------------------------------------
    @Transactional
    public void retryScreening(Long checkAppId, Long userId) {
        CheckCardApplication app = findOrThrow(checkAppId);
        if (!app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (!"SCREENING_FAILED".equals(app.getApplicationStatus())) {
            throw new BusinessException(ErrorCode.SCREENING_RETRY_NOT_ALLOWED);
        }
        checkCardApplicationMapper.updateStatus(checkAppId, "REQUESTED");
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                CompletableFuture.runAsync(() -> {
                    try {
                        requestScreeningReview(checkAppId);
                    } catch (Exception e) {
                        log.error("[체크카드] 심사 재시도 실패: checkAppId={}", checkAppId, e);
                    }
                });
            }
        });
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

    public void validateOwnership(Long checkAppId, Long userId) {
        CheckCardApplication app = findOrThrow(checkAppId);
        if (!app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
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

        // Fix 2: 가능한 조건은 실제 DB에서 조회
        // 급여이체·자동이체·카드사용실적은 거래내역(TRANSACTIONS) 테이블 필요 → 향후 구현
        boolean hasSavings = checkCardApplicationMapper.hasSavingsProduct(app.getUserId());

        return CheckCardLimitContext.builder()
                .age(age)
                .accountStatus(accountStatus != null ? accountStatus : "NORMAL")
                .hasSalaryTransfer(false)      // 거래내역 테이블 구축 시 구현
                .hasAutoTransfer3M(false)      // 거래내역 테이블 구축 시 구현
                .hasCardUsage3M(false)         // 거래내역 테이블 구축 시 구현
                .hasSavingsProduct(hasSavings) // ACCOUNTS 테이블에서 실 조회
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