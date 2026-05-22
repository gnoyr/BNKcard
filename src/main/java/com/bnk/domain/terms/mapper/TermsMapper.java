package com.bnk.domain.terms.mapper;

import com.bnk.domain.card.dto.response.CardDetailResponse;
import com.bnk.domain.terms.model.Terms;
import com.bnk.domain.terms.model.TermsFile;
import com.bnk.domain.terms.model.TermsMaster;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface TermsMapper {

    /** PACKAGE_TERMS → TERMS(status=PUBLISHED) → TERMS_MASTERS JOIN */
    List<Terms> findByPackageType(@Param("packageType") String packageType);

    Optional<Terms> findById(@Param("termsId") Long termsId);

    int insertTerms(Terms terms);

    int updateTermsStatus(@Param("termsId") Long termsId,
                          @Param("status") String status,
                          @Param("changedBy") Long changedBy);
    // AdminTermsService에서 파일 정보를 DB에 밀어 넣을 때 사용하는 메서드
    int insertTermsFile(TermsFile termsFile);
    
    int insertStatusHistory(@Param("termsId") Long termsId,
                            @Param("previousStatus") String previousStatus,
                            @Param("changedStatus") String changedStatus,
                            @Param("changedBy") Long changedBy,
                            @Param("changedReason") String changedReason);

    /** reconsent_required_yn='Y' 약관에 이전 동의이력 있는 userId 목록 */
    List<Long> findUserIdsForReconsent(@Param("termsId") Long termsId);

    int insertNotificationHistory(@Param("termsId") Long termsId,
                                  @Param("userId") Long userId,
                                  @Param("notificationType") String notificationType);
    
    List<CardDetailResponse.TermsFileDto> findTermsFilesByCardId(@Param("cardId") Long cardId);
    
    // TermsMapper.java
    /** termsId로 파일 목록 조회 (PDF + IMAGE 전체) */
    List<TermsFile> findFilesByTermsId(@Param("termsId") Long termsId);
    
    /** 관리자 약관 전체 목록 (status 필터 가능) */
    List<Terms> findAllForAdmin(@Param("status") String status);

    /** 약관 상세 + 마스터 JOIN */
    Optional<Terms> findByIdWithMaster(@Param("termsId") Long termsId);

    /** TERMS_MASTERS 전체 목록 */
    List<TermsMaster> findAllMasters();
}
