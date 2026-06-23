package com.bnk.domain.application.mapper;

import com.bnk.domain.application.model.CreditCardApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CreditCardApplicationMapper {

    // STEP 1 - 약관 동의
	// INSERT: user_id, card_id, application_status('DRAFT')
    int insertApplication(CreditCardApplication application);

    // STEP 2 - 본인확인
    // UPDATE: id_verified_yn
    int updateIdVerified(CreditCardApplication application);

    // STEP 3 - 기본정보 + 직업/소득
    // UPDATE: applicant_snapshot(AES), annual_income_band, credit_score_band, linked_account_id
    int updateApplicantInfo(CreditCardApplication application);

    // STEP 4 - 신청정보
    // UPDATE: payment_snapshot(AES), requested_limit, application_status('REQUESTED'), applied_at
    int updatePaymentInfo(CreditCardApplication application);
    
    Long findCurrentVersionId(@Param("cardId") Long cardId);  // 신청 시점 현재 PUBLISHED 버전 조회

    // STEP 5 - 기존고객 여부 체크 (부산은행 서버 조회) + 서류 저장 (신규고객만)
    // UPDATE: income_doc_key, asset_doc_key(선택), job_doc_key
    int updateDocs(CreditCardApplication application);
    
    boolean isExistingCustomer(@Param("userId") Long userId);

    // STEP 6 - 1차 심사 (심사서버)
    // UPDATE: screening_result, doc_verified_yn
    //         거절 시 → rejection_reason, application_status('REJECTED')
    int updateScreeningResult(CreditCardApplication application);

    // STEP 7 - 한도 검증 (부산은행 서버)
    // UPDATE: estimated_monthly_income, limit_check_result
    //         PASS            → approved_limit, application_status('APPROVED')
    //         MANUAL_REQUIRED → application_status('REVIEWING')
    int updateLimitCheck(CreditCardApplication application);    
//    Long calculateEstimatedMonthlyIncome(@Param("userId") Long userId); // 결제내역 기반 월소득 추정

    // STEP 8 - 추가 심사 (심사서버, REVIEWING 케이스만)
    // UPDATE: application_status('APPROVED' or 'REJECTED'), reviewed_at, reviewed_by
    //         승인 → approved_limit
    //         거절 → rejection_reason
    int updateReviewResult(CreditCardApplication application);

    // STEP 9 - 발급
    // UPDATE: application_status('ISSUED')
    int updateStatus(@Param("creditAppId") Long creditAppId,
                     @Param("applicationStatus") String applicationStatus);

    // 조회
    CreditCardApplication findById(@Param("creditAppId") Long creditAppId);

    List<CreditCardApplication> findByUserId(@Param("userId") Long userId);

    // DRAFT 정리 배치 - 7일 이상 DRAFT 삭제
    List<Long> findExpiredDraftIds();

    int deleteByIds(@Param("creditAppIds") List<Long> creditAppIds);
}