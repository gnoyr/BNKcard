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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;

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
    	log.info("[verifyIdentity] verificationServerUrl={}", verificationServerUrl);
        Map response = restTemplate.postForObject(
            verificationServerUrl + "/api/mydata/id-verification",
            Map.of(
                "appId",  request.getCreditAppId(),
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
	
	        ResponseEntity<ScreeningResultRequest> response = restTemplate.postForEntity(  // ← 변수에 담기
	                verificationServerUrl + "/review/request/credit/" + creditAppId,
	                Map.of(
	                    "creditAppId",      creditAppId,
	                    "ciValue",          app.getCiValue(),
	                    "requestedLimit",   app.getRequestedLimit(),
	                    "creditScoreBand",  app.getCreditScoreBand(),
	                    "annualIncomeBand", app.getAnnualIncomeBand(),
	                    "incomeDocKey",     app.getIncomeDocKey() != null ? app.getIncomeDocKey() : "",
	                    "assetDocKey",      app.getAssetDocKey()  != null ? app.getAssetDocKey()  : "",
	                    "jobDocKey",        app.getJobDocKey()    != null ? app.getJobDocKey()    : ""
	                ),
	                ScreeningResultRequest.class
	            );
	        
	        if (response.getBody() != null) {
	            saveScreeningResult(response.getBody());  // ← 바로 처리
	        }
	        
	    } catch (BusinessException e) {
	        throw e;  // 추가심사 실패 등 이미 처리된 예외는 그냥 다시 던짐
	    } catch (Exception e) {
	        log.error("[신용카드] 심사 의뢰 실패: creditAppId={}", creditAppId, e);
	        creditCardApplicationMapper.updateStatus(creditAppId, "SCREENING_FAILED");
	        throw new BusinessException(ErrorCode.SCREENING_REQUEST_FAILED);
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
        
        Long estimatedMonthlyIncome = app.getEstimatedMonthlyIncome();  // 월추정소득
        Integer creditScore           = screeningData.getCreditScore();  // 신용점수
        Integer vehicleCount          = screeningData.getVehicleCount();  // 차량 보유수
        Long   loanBalance            = screeningData.getLoanBalance();  // 대출 잔액
        Double delinquencyRate        = screeningData.getDelinquencyRate();  // 연체율
        Integer multipleDebtCount     = screeningData.getMultipleDebtCount();  // 다중채무 건수
        String jobType                = screeningData.getJobType();  // 직업유형. EMPLOYED=직장인 / SELF_EMPLOYED=자영업자 / STUDENT=학생 / UNEMPLOYED=무직·전업주부 / OTHER=기타 
        
        // ── 1단계. 즉시 추가심사 조건 ────────────────────────────
        // 연체율 5% 초과 → 스코어링 없이 바로 추가심사
        if (delinquencyRate != null && delinquencyRate > 5.0) {
            app.setLimitCheckResult("MANUAL_REQUIRED");
            app.setApplicationStatus("REVIEWING");
            creditCardApplicationMapper.updateLimitCheck(app);
            requestAdditionalReview(creditAppId);
            return;
        }

        // 다중채무 5건 이상 → 스코어링 없이 바로 추가심사
        if (multipleDebtCount != null && multipleDebtCount >= 5) {
            app.setLimitCheckResult("MANUAL_REQUIRED");
            app.setApplicationStatus("REVIEWING");
            creditCardApplicationMapper.updateLimitCheck(app);
            requestAdditionalReview(creditAppId);
            return;
        }

        // ── 2단계. 추정소득 없음 → 추가심사 ─────────────────────
        // 홈택스 소득 없는 완전 신규 → 서류 기반 추가심사
        if (estimatedMonthlyIncome == null || estimatedMonthlyIncome == 0) {
            app.setLimitCheckResult("MANUAL_REQUIRED");
            app.setApplicationStatus("REVIEWING");
            creditCardApplicationMapper.updateLimitCheck(app);
            requestAdditionalReview(creditAppId);
            return;
        }

        // ── 3단계. 추정소득 있으면 스코어링 (총 100점) ───────────────────────────
        int score = 0;

        // [신용점수 - 최대 30점]
        // 신용상태 및 금융거래 실적 반영
        if (creditScore != null) {
            if      (creditScore >= 900) score += 30;
            else if (creditScore >= 800) score += 25;  // ID 62: 820점 → 25점
            else if (creditScore >= 700) score += 15;  // ID 66: 680점 → 10점
            else if (creditScore >= 650) score += 10;
            else                         score += 0;   // ID 64: 580점 → 0점
        }

        // [직업 안정성 - 최대 25점]
        // 소득 지속성 반영
        if (jobType != null) {
            switch (jobType) {
                case "REGULAR"    -> score += 25;  // 정규직 - 가장 안정적
                case "CONTRACT"   -> score += 18;  // 계약직
                case "BUSINESS"   -> score += 15;  // 사업자 - 소득 변동성 있음
                case "FREELANCER" -> score += 10;  // 프리랜서
                case "UNEMPLOYED" -> score += 0;   // 무직
                default           -> score += 10;
            }
        }

        // [연체율 - 최대 20점]
        // 연체이력 반영 (5% 초과는 1단계에서 이미 걸러짐)
        if (delinquencyRate != null) {
            if      (delinquencyRate == 0)          score += 20;  // ID 62: 0% → 20점
            else if (delinquencyRate <= 1.0)        score += 15;
            else if (delinquencyRate <= 3.0)        score += 10;
            else if (delinquencyRate <= 5.0)        score += 5;   // ID 66: 3.5% → 5점
        } else {
            score += 20;  // 연체 정보 없음 → 최고점
        }

        // [대출잔액 - 최대 15점]
        // 연소득 대비 대출 부담 반영
        if (loanBalance != null) {
            long annualIncome = estimatedMonthlyIncome * 12;
            double loanRatio  = (double) loanBalance / annualIncome;
            if      (loanRatio < 1.0) score += 15;  // 연소득 미만       ID 62: 15000000/120000000 → 0.125 → 15점
            else if (loanRatio < 3.0) score += 10;  // 연소득 1~3배
            else if (loanRatio < 5.0) score += 5;   // 연소득 3~5배      ID 66: 85000000/33600000 → 2.53 → 10점
            else                      score += 0;   // 연소득 5배 초과   ID 64: 120000000/42000000 → 2.86 → 10점
        } else {
            score += 15;  // 대출 없음 → 최고점
        }

        // [차량 보유 - 최대 10점]
        // 재산상황 반영
        if (vehicleCount != null && vehicleCount > 0) {
            score += 10;  // ID 62: 차량 2대 → 10점 / ID 66: 차량 1대 → 10점
        }
        
        // ── 4단계. 총점 기반 한도 비율 결정 ──────────────────────
        // 추정 월소득 대비 승인 가능 한도 비율
        double limitRatio;
        String limitCheckResult;
        if (score >= 75) {
            limitRatio      = 0.40;  // 우수  → 월소득 × 40%
            limitCheckResult = "PASS";
        } else if (score >= 55) {
            limitRatio      = 0.30;  // 양호  → 월소득 × 30%
            limitCheckResult = "PASS";
        } else if (score >= 35) {
            limitRatio      = 0.20;  // 보통  → 월소득 × 20%
            limitCheckResult = "PASS";
        } else {
            limitRatio      = 0;     // 미흡  → 추가심사
            limitCheckResult = "MANUAL_REQUIRED";
        }

        // ── 5단계. 신청한도 vs 최대한도 비교 ─────────────────────
        if ("MANUAL_REQUIRED".equals(limitCheckResult)) {
            app.setLimitCheckResult("MANUAL_REQUIRED");
            app.setApplicationStatus("REVIEWING");
            creditCardApplicationMapper.updateLimitCheck(app);
            requestAdditionalReview(creditAppId);
            return;
        }

        long maxLimit = (long)(estimatedMonthlyIncome * limitRatio);

        if (app.getRequestedLimit() <= maxLimit) {
            // 신청한도가 최대한도 이내 → 승인
            app.setLimitCheckResult("PASS");
            app.setApprovedLimit(app.getRequestedLimit());
            app.setApplicationStatus("APPROVED");
        } else {
            // 신청한도가 최대한도 초과 → 추가심사
            app.setLimitCheckResult("MANUAL_REQUIRED");
            app.setApplicationStatus("REVIEWING");
        }

        creditCardApplicationMapper.updateLimitCheck(app);

        if ("APPROVED".equals(app.getApplicationStatus())) {
            issueCard(creditAppId);
        } else {
            requestAdditionalReview(creditAppId);
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
            // REVIEWING 상태 유지 → 관리자가 수동으로 재처리
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
    // 임시 저장
    // ----------------------------------------------------------------     
    public CreditApplicationResponse findDraftByCardId(Long cardId, Long userId) {
        CreditCardApplication app = creditCardApplicationMapper.findDraftByCardIdAndUserId(cardId, userId);
        if (app == null) return null;
        return toResponse(app);
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
                    .idVerifiedYn(app.getIdVerifiedYn())
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
