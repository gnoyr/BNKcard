package com.bnk.domain.application.mapper;

import com.bnk.domain.application.model.CardApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CardApplicationMapper {

    int insertApplication(CardApplication application);

    Optional<CardApplication> findById(@Param("applicationId") Long applicationId);

    List<CardApplication> findByUserId(@Param("userId") Long userId);

    /** 관리자 신청 목록 동적 필터 */
    List<CardApplication> findAdminList(CardApplication filter);

    long countAdminList(CardApplication filter);

    int updateStatus(CardApplication application);
}
