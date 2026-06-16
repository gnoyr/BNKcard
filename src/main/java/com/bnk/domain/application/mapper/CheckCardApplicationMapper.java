package com.bnk.domain.application.mapper;

import com.bnk.domain.application.model.CheckCardApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CheckCardApplicationMapper {

    // STEP 1 - 약관 동의
    int insertApplication(CheckCardApplication application);

    // STEP 2 - 본인확인
    int updateIdVerified(@Param("checkAppId") Long checkAppId,
                         @Param("idVerifiedYn") String idVerifiedYn);

    // STEP 3 - 계좌 선택
    int updateLinkedAccount(@Param("checkAppId") Long checkAppId,
                            @Param("linkedAccountId") Long linkedAccountId);

    // STEP 4 - 기본정보 + 신청정보
    int updatePaymentInfo(CheckCardApplication application);

    // STEP 5 - 심사
    int updateReviewResult(CheckCardApplication application);

    // STEP 6 - 발급
    int updateStatus(@Param("checkAppId") Long checkAppId,
                     @Param("applicationStatus") String applicationStatus);

    // 조회
    CheckCardApplication findById(@Param("checkAppId") Long checkAppId);

    List<CheckCardApplication> findByUserId(@Param("userId") Long userId);

    // DRAFT 정리 배치 - 7일 이상 DRAFT 삭제
    List<Long> findExpiredDraftIds();

    int deleteByIds(@Param("checkAppIds") List<Long> checkAppIds);
}