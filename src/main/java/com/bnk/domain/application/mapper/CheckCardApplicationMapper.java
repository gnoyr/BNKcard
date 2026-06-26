package com.bnk.domain.application.mapper;

import com.bnk.domain.application.model.CheckCardApplication;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CheckCardApplicationMapper {

    // STEP 1 - 약관 동의
    // INSERT: user_id, card_id, application_status('DRAFT')
    int insertApplication(CheckCardApplication application);

    // STEP 2 - 본인확인
    // UPDATE: id_verified_yn
    int updateIdVerified(CheckCardApplication application);

    // STEP 3 - 기본정보
    // UPDATE: applicant_snapshot(AES)
    int updateApplicantInfo(CheckCardApplication application);

    // STEP 4 - 신청정보
    // UPDATE: payment_snapshot, linked_account_id, application_status('REQUESTED'), applied_at
    int updatePaymentInfo(CheckCardApplication application);
    
    Long findCurrentVersionId(@Param("cardId") Long cardId);  //신청 시점 현재 PUBLISHED 버전 조회

    // STEP 5 - 심사
    // UPDATE: application_status, reviewed_at, reviewed_by
    //         거절 시 → rejection_reason
    int updateReviewResult(CheckCardApplication application);

    // STEP 6 - 발급
    // UPDATE: application_status('ISSUED')
    int updateStatus(@Param("checkAppId") Long checkAppId,
                     @Param("applicationStatus") String applicationStatus);

    // 조회
    CheckCardApplication findById(@Param("checkAppId") Long checkAppId);

    List<CheckCardApplication> findByUserId(@Param("userId") Long userId);
    
    // 임시 저장
    CheckCardApplication findDraftByCardIdAndUserId(@Param("cardId") Long cardId, @Param("userId") Long userId);

    // DRAFT 정리 배치 - 7일 이상 DRAFT 삭제
    List<Long> findExpiredDraftIds();

    int deleteByIds(@Param("checkAppIds") List<Long> checkAppIds);
    
    // 계좌 상태 조회 (한도 산정용)
    String findAccountStatus(@Param("accountId") Long accountId);
}