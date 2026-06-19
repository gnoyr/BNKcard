package com.bnk.domain.application.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.fasterxml.jackson.databind.ObjectMapper;

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
    public void verifyIdentity(Long creditAppId, String idVerifiedYn) {
        int updated = creditCardApplicationMapper.updateIdVerified(creditAppId, idVerifiedYn);
        if (updated == 0) {
        	throw new BusinessException(ErrorCode.APP_NOT_FOUND);
        }
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
	        // payment_snapshot { card_brand, card_design_id, payment_day, combined_transit_yn, tx_alert_type, statement_method, overseas_dcc_block_yn }
	        creditCardApplicationMapper.updatePaymentInfo(application);
	    } catch (Exception e) {
	        throw new RuntimeException("paymentSnapshot 직렬화 실패", e);
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
                .build();
        creditCardApplicationMapper.updateScreeningResult(application);
    }
    
    // ----------------------------------------------------------------
    // STEP 7 - 한도 검증 (결제내역 기반 월소득 추정 후 한도 검증)
    // ----------------------------------------------------------------
    public void checkLimit(Long creditAppId) {
        CreditCardApplication app = findOrThrow(creditAppId);

        Long estimatedMonthlyIncome = creditCardApplicationMapper.calculateEstimatedMonthlyIncome(app.getUserId());

        long threshold = (long)(estimatedMonthlyIncome * 0.3);

        if (app.getRequestedLimit() <= threshold) {
            app.setLimitCheckResult("PASS");
            app.setApprovedLimit(app.getRequestedLimit());
            app.setApplicationStatus("APPROVED");
        } else {
            app.setLimitCheckResult("MANUAL_REQUIRED");
            app.setApplicationStatus("REVIEWING");
        }
        app.setEstimatedMonthlyIncome(estimatedMonthlyIncome);
        creditCardApplicationMapper.updateLimitCheck(app);
        
        // PASS → 자동 발급
        if ("APPROVED".equals(app.getApplicationStatus())) {
            issueCard(creditAppId);
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
        String cardNumber = String.format("%016d", 
            (long)(Math.random() * 9_000_000_000_000_000L) + 1_000_000_000_000_000L);

        UserCard userCard = new UserCard();
        userCard.setUserId(app.getUserId());
        userCard.setVersionId(app.getVersionId());
        userCard.setCreditAppId(app.getCreditAppId());
        userCard.setCardPasswordHash(app.getCardPasswordHash());
        userCard.setCardNumber(cardNumber);
        userCard.setIssueDate(issueDate);
        userCard.setExpireDate(expireDate);
        userCard.setCardStatus("ACTIVE");
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
