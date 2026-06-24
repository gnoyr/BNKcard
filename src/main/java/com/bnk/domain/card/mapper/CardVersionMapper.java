package com.bnk.domain.card.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bnk.domain.card.model.CardVersion;

/**
 * CARD_VERSIONS 테이블 Mapper
 *
 * 변경 이력:
 *  - 이름 변경: CardVersionMapper2 → CardVersionMapper ("2" 제거)
 *  - 참조 모델: model2.CardVersion → model.CardVersion (패키지 이동)
 *  - 삭제 대상: com.bnk.domain.card.mapper.CardVersionMapper2
 */
@Mapper
public interface CardVersionMapper {

    /** 버전 INSERT — versionId Oracle 트리거(TRG_CARD_VERSIONS_BI) + SEQ_CARD_VERSIONS 자동 채번 */
    void insertCardVersion(CardVersion version);

    /** 결재 완료 시 version_status UPDATE */
    void updateVersionStatus(@Param("versionId") Long versionId,
                             @Param("versionStatus") String versionStatus,
                             @Param("approvedBy") Long approvedBy);

    /** versionId로 단건 조회 (결재 화면 snapshot 조회) */
    CardVersion getCardVersion(@Param("versionId") Long versionId);

    /** 카드별 최신 버전 순번 조회 (다음 버전번호 계산용) */
    int getLatestVersionSeq(@Param("cardId") Long cardId);
 // 카드의 전체 버전 목록 조회
    List<CardVersion> findByCardId(@Param("cardId") Long cardId);
    
}
