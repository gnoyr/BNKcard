package com.bnk.domain.terms.mapper;

import com.bnk.domain.terms.model.UserTermsAgreement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserTermsAgreementMapper {

    int insertAgreement(UserTermsAgreement agreement);

    /** 배치 INSERT */
    int insertAgreements(@Param("agreements") List<UserTermsAgreement> agreements);

    List<UserTermsAgreement> findByUserId(@Param("userId") Long userId);

    boolean existsAgreement(@Param("userId") Long userId,
                            @Param("termsId") Long termsId);
}
