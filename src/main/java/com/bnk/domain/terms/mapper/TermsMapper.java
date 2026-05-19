package com.bnk.domain.terms.mapper;

import com.bnk.domain.card.dto.response.CardDetailResponse;
import com.bnk.domain.terms.model.Terms;
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
}
