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

import com.bnk.domain.application.dto.CreditApplicantSnapshotDto;
import com.bnk.domain.application.dto.PaymentSnapshotDto;
import com.bnk.domain.application.dto.request.CreditCardApplicationRequest;
import com.bnk.domain.application.dto.request.ReviewResultRequest;
import com.bnk.domain.application.dto.request.ScreeningResultRequest;
import com.bnk.domain.application.dto.response.CreditApplicationResponse;
import com.bnk.domain.application.mapper.CreditCardApplicationMapper;
import com.bnk.domain.application.mapper.UserCardMapper;
import com.bnk.domain.application.model.CreditCardApplication;
import com.bnk.domain.application.model.UserCard;
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
import com.google.api.client.util.Value;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditCardApplicationService {
	
	private final CreditCardApplicationMapper creditCardApplicationMapper;
    private final UserTermsAgreementMapper userTermsAgreementMapper;
    private final UserCardMapper userCardMapper;
    private final TermsMapper termsMapper;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final AesCryptoUtil aesCryptoUtil;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
    @Value("${verification.server.url}")
    private String verificationServerUrl;
	
	// ----------------------------------------------------------------
    // STEP 1 - 약관 동의
    // ----------------------------------------------------------------
    @Transactional
    public Long agreeTerms(Long userId, CreditCardApplicationRequest request) {
    	CreditCardApplication application = CreditCardApplication.builder()
                .userId(userId)
                .cardId(request.getCardId())
                .build();
        creditCardApplicationMapper.insertApplication(application);  // 상태 draft
 
        Long creditAppId = application.getCreditAppId();
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
                            .agreementSource("CREDIT_CARD_APPLY")
                            .agreedContentSnapshot(terms.getContentHtml())
                            .creditAppId(creditAppId)
                            .build();
                })
                .collect(Collectors.toList());

        userTermsAgreementMapper.insertAgreements(agreements);
 
        return creditAppId;
    }
 
    // ----------------------------------------------------------------
    // STEP 2 - 본인확인 결과 저장 (심사 서버에서 받아서)
    // ----------------------------------------------------------------
    public String verifyIdentity(CreditCardApplicationRequest request) {
        Map response = restTemplate.postForObject(
            verificationServerUrl + "/api/mydata/id-verification",
            Map.of(
                "creditAppId",  request.getCreditAppId(),
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
        
        CreditCardApplication application = CreditCardApplication.builder()
                .creditAppId(request.getCreditAppId())
                .idVerifiedYn(idVerifiedYn)
                .ciValue(ciValue)
                .build();

        int updated = creditCardApplicationMapper.updateIdVerified(application);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.APP_NOT_FOUND);
        }

        return idVerifiedYn;
    }
 
    // ----------------------------------------------------------------
    // STEP 3 - 기본정보 + 직업/소득 저장
    // ----------------------------------------------------------------
    public void saveApplicantInfo(CreditCardApplicationRequest request) {
        validateIdVerified(request.getCreditAppId()); 
        
        try {
            CreditCardApplication application = CreditCardApplication.builder()
                    .creditAppId(request.getCreditAppId())
                    .applicantSnapshot(objectMapper.writeValueAsString(request.getApplicantSnapshot()))
                    .annualIncomeBand(request.getAnnualIncomeBand())
                    .creditScoreBand(request.getCreditScoreBand())
                    .linkedAccountId(request.getLinkedAccountId())
                    .build();
            
            // applicantSnapshot { name, nameEn,mobileNo, address, email,
            // 					   incomeType, healthInsuranceType, hasRealEstate, hasOwnVehicle }
            creditCardApplicationMapper.updateApplicantInfo(application);
        } catch (Exception e) {
            throw new RuntimeException("applicantSnapshot 직렬화 실패", e);
        }
    }
 
	// ----------------------------------------------------------------
	// STEP 4 - 신청정보 저장 + 신청 완료(REQUESTED)
	// ----------------------------------------------------------------
    @Transactional
	public void submitApplication(CreditCardApplicationRequest request) {
	    validateIdVerified(request.getCreditAppId());
	    
	    // STEP 5 - 기존고객 여부 체크
	    boolean isExisting = checkExistingCustomer(request.getCreditAppId());
	    if (!isExisting) {
	    	// 신규고객 - 서류 확인
	        if (request.getIncomeDocKey() == null || request.getJobDocKey() == null) {
	        	throw new BusinessException(ErrorCode.DOCS_REQUIRED);
	        }
	        // 서류 저장
	        CreditCardApplication docs = CreditCardApplication.builder()
	                .creditAppId(request.getCreditAppId())
	                .incomeDocKey(request.getIncomeDocKey())
	                .assetDocKey(request.getAssetDocKey())
	                .jobDocKey(request.getJobDocKey())
	                .build();
	        creditCardApplicationMapper.updateDocs(docs);
	    }
	
	    // 신청 시점 현재 PUBLISHED 버전 조회
	    Long versionId = creditCardApplicationMapper.findCurrentVersionId(request.getCardId());
	
	    try {
	        CreditCardApplication application = CreditCardApplication.builder()
	                .creditAppId(request.getCreditAppId())
	                .versionId(versionId)
	                .paymentSnapshot(objectMapper.writeValueAsString(request.getPaymentSnapshot()))
	                .requestedLimit(request.getRequestedLimit())
	                .cardPasswordHash(passwordEncoder.encode(request.getCardPassword()))
	                .build();
	        // payment_snapshot { card_brand, card_design_id, payment_day, combined_transit_yn, tx_alert_type, statement_method }
	        creditCardApplicationMapper.updatePaymentInfo(application);	        

	    } catch (Exception e) {
	        throw new RuntimeException("paymentSnapshot 직렬화 실패", e);
	    }	    

        // 마이데이터로 심사 의뢰 추가
        requestScreeningReview(request.getCreditAppId());
	}
    

	private void requestScreeningReview(Long creditAppId) {
	    try {
	        CreditCardApplication app = findOrThrow(creditAppId);
	
	        restTemplate.postForEntity(
	            verificationServerUrl + "/review/request/credit/" + creditAppId,
	            Map.of(
	                    "creditAppId",     creditAppId,
	                    "ciValue",         app.getCiValue(),
	                    "requestedLimit",  app.getRequestedLimit(),
	                    "creditScoreBand", app.getCreditScoreBand(),
	                    "annualIncomeBand", app.getAnnualIncomeBand(),
	                    "incomeDocKey",    app.getIncomeDocKey() != null ? app.getIncomeDocKey() : "",
	                    "assetDocKey",     app.getAssetDocKey()  != null ? app.getAssetDocKey()  : "",
	                    "jobDocKey",       app.getJobDocKey()    != null ? app.getJobDocKey()    : ""
	                ),
	            Void.class
	        );
	    } catch (Exception e) {
	        log.error("[신용카드] 심사 의뢰 실패: creditAppId={}", creditAppId, e);
	    }
	}
    
    // ----------------------------------------------------------------
    // STEP 6 - 1차 심사 결과 저장 (심사서버 콜백)
    // ----------------------------------------------------------------
	public void saveScreeningResult(ScreeningResultRequest request) {
	    CreditCardApplication application = CreditCardApplication.builder()
	            .creditAppId(request.getAppId())
	            .screeningResult(request.getScreeningResult())
	            .docVerifiedYn(request.getDocVerifiedYn())
	            .rejectionReason(request.getRejectionReason())
	            .applicationStatus(request.getApplicationStatus())
	            .reviewedBy(request.getReviewedBy())
	            .estimatedMonthlyIncome(request.getEstimatedMonthlyIncome())
	            .build();
	    creditCardApplicationMapper.updateScreeningResult(application);

	    if (!"REJECTED".equals(request.getApplicationStatus())) {
	        checkLimit(request.getAppId(), request);  // ← request 통째로 넘김
	    }
	}
    
    // ----------------------------------------------------------------
    // STEP 7 - 한도 검증 (심사서버에서 데이터 받아서)
    // ----------------------------------------------------------------
    public void checkLimit(Long creditAppId, ScreeningResultRequest screeningData) {
        CreditCardApplication app = findOrThrow(creditAppId);
        
        Long estimatedMonthlyIncome = app.getEstimatedMonthlyIncome();
        Long   creditScore            = screeningData.getCreditScore();
        Integer vehicleCount          = screeningData.getVehicleCount();
        Long   loanBalance            = screeningData.getLoanBalance();
        Double delinquencyRate        = screeningData.getDelinquencyRate();
        Integer multipleDebtCount     = screeningData.getMultipleDebtCount();
        String jobType                = screeningData.getJobType();
        
        if (estimatedMonthlyIncome == null || estimatedMonthlyIncome == 0) {
            // 추정소득 없음(완전 신규) → 심사서버가 서류 보고 판단
            app.setLimitCheckResult("MANUAL_REQUIRED");
            app.setApplicationStatus("REVIEWING");
            creditCardApplicationMapper.updateLimitCheck(app);
            requestAdditionalReview(creditAppId);
            return;
        }

        // 추정소득 있으면 한도 검증
        long threshold = (long)(estimatedMonthlyIncome * 0.3);

        if (app.getRequestedLimit() <= threshold) {
            app.setLimitCheckResult("PASS");
            app.setApprovedLimit(app.getRequestedLimit());
            app.setApplicationStatus("APPROVED");
        } else {
            app.setLimitCheckResult("MANUAL_REQUIRED");
            app.setApplicationStatus("REVIEWING");
        }
        creditCardApplicationMapper.updateLimitCheck(app);

        if ("APPROVED".equals(app.getApplicationStatus())) {
            issueCard(creditAppId);
        } else {
            requestAdditionalReview(creditAppId);  // 한도 초과시 심사서버 전달
        }
    }
    
    // RestTemplate 심사서버 전달
    private void requestAdditionalReview(Long creditAppId) {
        try {
        	CreditCardApplication app = findOrThrow(creditAppId);
        	 
            restTemplate.postForEntity(
                verificationServerUrl + "/review/request/" + creditAppId,
                Map.of(
                    "creditAppId", creditAppId,  // 1차 심사와 동일한 서버
                    "ciValue",     app.getCiValue()
                ),
                Void.class
            );
        } catch (Exception e) {
            log.error("[신용카드] 추가심사 요청 실패: creditAppId={}", creditAppId, e);
        }
    }
   
    // ----------------------------------------------------------------
    // STEP 8 - 추가 심사 결과 저장 (심사서버 콜백, REVIEWING 케이스만)
    // ----------------------------------------------------------------
    public void saveReviewResult(ReviewResultRequest request) {
        CreditCardApplication application = CreditCardApplication.builder()
                .creditAppId(request.getAppId())
                .applicationStatus(request.getApplicationStatus())
                .approvedLimit(request.getApprovedLimit())
                .rejectionReason(request.getRejectionReason())
                .reviewedBy(request.getReviewedBy())
                .build();
        creditCardApplicationMapper.updateReviewResult(application);
        
        // APPROVED → 자동 발급
        if ("APPROVED".equals(request.getApplicationStatus())) {
            issueCard(request.getAppId());
        }
    }
    
    // ----------------------------------------------------------------
    // APPROVED 시 자동 발급
    // ----------------------------------------------------------------
    @Transactional
    public void issueCard(Long creditAppId) {
        CreditCardApplication app = findOrThrow(creditAppId);
 
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

        PaymentSnapshotDto snap = null;
        try {
            snap = objectMapper.readValue(app.getPaymentSnapshot(), PaymentSnapshotDto.class);
        } catch (Exception e) {
            throw new RuntimeException("paymentSnapshot 역직렬화 실패", e);
        }

	     UserCard userCard = new UserCard();
	     userCard.setUserId(app.getUserId());
	     userCard.setVersionId(app.getVersionId());
	     userCard.setCreditAppId(app.getCreditAppId());   // 신용카드
	     // ── 카드번호
	     userCard.setEncryptedCardNumber(aesCryptoUtil.encrypt(rawCardNumber));
	     userCard.setMaskedCardNumber(MaskingUtil.maskCardNumber(rawCardNumber));
	     userCard.setIssueDate(issueDate);
	     userCard.setExpireDate(expireDate);
	     userCard.setCardStatus("ACTIVE");
	     userCard.setUsableYn("Y");
	     userCard.setCardPasswordHash(app.getCardPasswordHash());
	     userCard.setLinkedAccountId(app.getLinkedAccountId());
	     userCard.setDailyLimitAmount(1_000_000L);
	     userCard.setMonthlyLimitAmount(app.getApprovedLimit());
	     // ── payment_snapshot
	     userCard.setCardBrand(snap.getCardBrand());
	     userCard.setCardDesignId(snap.getCardDesignId() != null ? Long.parseLong(snap.getCardDesignId()) : null);
	     userCard.setPaymentDay(snap.getPaymentDay());
	     userCard.setCombinedTransitYn(snap.getCombinedTransitYn());
	     userCard.setTxAlertType(snap.getTxAlertType());
	     userCard.setStatementMethod(snap.getStatementMethod());

        userCardMapper.insertUserCard(userCard);
        creditCardApplicationMapper.updateStatus(creditAppId, "ISSUED");

        log.info("[신용카드] 발급 완료: creditAppId={}, userCardId={}", creditAppId, userCard.getUserCardId());
    }
    
    // 기존고객 여부 체크 (페이지 진입 시 UI 결정용)
    public boolean checkExistingCustomer(Long creditAppId) {
        CreditCardApplication app = findOrThrow(creditAppId);
        return creditCardApplicationMapper.isExistingCustomer(app.getUserId());
    }
    
    
    // ----------------------------------------------------------------
    // 사용자 조회
    // ---------------------------------------------------------------- 
    private final CardMapper      cardMapper;
    private final CardImageMapper cardImageMapper;
    
    // 신청 단건 조회(상세)
    public CreditApplicationResponse findOne(Long creditAppId, Long userId) {
        CreditCardApplication app = findOrThrow(creditAppId);
        if (!app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.APP_NOT_FOUND);
        }
        return toResponse(app);
    }
    
	// 내 신청 목록 조회
    public List<CreditApplicationResponse> findMyApplications(Long userId) {
        List<CreditCardApplication> apps = creditCardApplicationMapper.findByUserId(userId);
        return apps.stream().map(this::toResponse).collect(Collectors.toList());
    }

    
 
    // ----------------------------------------------------------------
    // private helpers
    // ----------------------------------------------------------------
 
    /** id_verified_yn = 'Y' 검증 */
    private void validateIdVerified(Long creditAppId) {
        CreditCardApplication app = findOrThrow(creditAppId);
        if (!"Y".equals(app.getIdVerifiedYn())) {
        	throw new BusinessException(ErrorCode.IDENTITY_NOT_VERIFIED);
        }
    }
 
    private CreditCardApplication findOrThrow(Long creditAppId) {
        CreditCardApplication app = creditCardApplicationMapper.findById(creditAppId);
        if (app == null) {
        	throw new BusinessException(ErrorCode.APP_NOT_FOUND);
        }
        return app;
    }
    
    // 사용자 신청서 조회
    private CreditApplicationResponse toResponse(CreditCardApplication app) {
        try {
            Card card = cardMapper.findById(app.getCardId());

            PaymentSnapshotDto paymentSnapshot = app.getPaymentSnapshot() != null
                    ? objectMapper.readValue(app.getPaymentSnapshot(), PaymentSnapshotDto.class)
                    : null;

            // 신청 시 선택한 카드 디자인 이미지 조회
            String cardImageUrl = null;
            if (paymentSnapshot != null && paymentSnapshot.getCardDesignId() != null) {
                CardImage selectedImage = cardImageMapper.findByImageId(Long.parseLong(paymentSnapshot.getCardDesignId()));
                cardImageUrl = selectedImage != null ? selectedImage.getImageUrl() : null;
            }

            CreditApplicantSnapshotDto applicantSnapshot = app.getApplicantSnapshot() != null
                    ? objectMapper.readValue(app.getApplicantSnapshot(), CreditApplicantSnapshotDto.class)
                    : null;

            return CreditApplicationResponse.builder()
                    .creditAppId(app.getCreditAppId())
                    .cardId(app.getCardId())
                    .cardName(card != null ? card.getCardName() : null)
                    .cardImageUrl(cardImageUrl)
                    .applicationStatus(app.getApplicationStatus())
                    .applicantSnapshot(applicantSnapshot)
                    .paymentSnapshot(paymentSnapshot)
                    .approvedLimit(app.getApprovedLimit())
                    .requestedLimit(app.getRequestedLimit())
                    .rejectionReason(app.getRejectionReason())
                    .appliedAt(app.getAppliedAt())
                    .createdAt(app.getCreatedAt())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("snapshot 역직렬화 실패", e);
        }
    }   
    

    

}
