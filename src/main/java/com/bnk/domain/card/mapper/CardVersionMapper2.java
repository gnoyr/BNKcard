package com.bnk.domain.card.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bnk.domain.card.model2.CardVersion;

@Mapper
public interface CardVersionMapper2 {

	// 버전 INSERT — versionId 트리거 자동 채번
    void insertCardVersion(CardVersion version);

    // 결재 완료 시 APPROVED로 UPDATE
    void updateVersionStatus(@Param("versionId") Long versionId,
                             @Param("versionStatus") String versionStatus,
                             @Param("approvedBy") Long approvedBy);


    // versionId로 단건 조회 (결재 화면에서 snapshot 조회)
    CardVersion getCardVersion(@Param("versionId") Long versionId);
   
    int getLatestVersionSeq(@Param("cardId") Long cardId);
   
}
