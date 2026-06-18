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
    int updateIdVerified(@Param("checkAppId") Long checkAppId,
                         @Param("idVerifiedYn") String idVerifiedYn);

    // STEP 3 - 계좌 선택
    // UPDATE: linked_account_id
    int updateLinkedAccount(@Param("checkAppId") Long checkAppId,
                            @Param("linkedAccountId") Long linkedAccountId);

    // STEP 4 - 기본정보 + 신청정보
    // UPDATE: applicant_snapshot(AES), payment_snapshot, application_status('REQUESTED'), applied_at
    int updatePaymentInfo(CheckCardApplication application);
    
    Long findCurrentVersionId(@Param("cardId") Long cardId);

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

    // DRAFT 정리 배치 - 7일 이상 DRAFT 삭제
    List<Long> findExpiredDraftIds();

    int deleteByIds(@Param("checkAppIds") List<Long> checkAppIds);
}