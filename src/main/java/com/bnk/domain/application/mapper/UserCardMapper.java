package com.bnk.domain.application.mapper;

import com.bnk.domain.application.dto.response.MyCardApplicationResponse;
import com.bnk.domain.application.model.UserCard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserCardMapper {

    int insertUserCard(UserCard userCard);

    List<UserCard> findByUserId(@Param("userId") Long userId);

    int updateCardStatus(@Param("userCardId") Long userCardId,
                         @Param("cardStatus") String cardStatus);
    
    // 신용/체크카드 통합 신청 현황 조회 (날짜순)
    List<MyCardApplicationResponse> findMyAllApplications(@Param("userId") Long userId);
    // 이건 마이페이지 쪽 서비스에서 구현해야함 !!
}
