package com.bnk.domain.application.mapper;

import com.bnk.domain.application.model.CreditCardApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CreditCardApplicationMapper {

    // STEP 1 - 약관 동의
    int insertApplication(CreditCardApplication application);

    // STEP 2 - 본인확인
    int updateIdVerified(@Param("creditAppId") Long creditAppId,
                         @Param("idVerifiedYn") String idVerifiedYn);

    // STEP 3 - 기본정보 + 직업/소득
    int updateApplicantInfo(CreditCardApplication application);

    // STEP 4 - 신청정보
    int updatePaymentInfo(CreditCardApplication application);

    // STEP 5 - 서류
    int updateDocs(CreditCardApplication application);

    // STEP 6 - 1차 심사
    int updateScreeningResult(CreditCardApplication application);

    // STEP 7 - 한도 검증
    int updateLimitCheck(CreditCardApplication application);

    // STEP 8 - 추가 심사
    int updateReviewResult(CreditCardApplication application);

    // STEP 9 - 발급
    int updateStatus(@Param("creditAppId") Long creditAppId,
                     @Param("applicationStatus") String applicationStatus);

    // 조회
    CreditCardApplication findById(@Param("creditAppId") Long creditAppId);

    List<CreditCardApplication> findByUserId(@Param("userId") Long userId);

    // DRAFT 정리 배치 - 7일 이상 DRAFT 삭제
    List<Long> findExpiredDraftIds();

    int deleteByIds(@Param("creditAppIds") List<Long> creditAppIds);
}