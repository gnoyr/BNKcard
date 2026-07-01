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

    /** 소유권 검증용 단건 조회 (본인 카드만, 미삭제) */
    UserCard findByIdAndUserId(@Param("userCardId") Long userCardId,
                               @Param("userId") Long userId);

    int updateCardStatus(@Param("userCardId") Long userCardId,
                         @Param("cardStatus") String cardStatus);

    /**
     * 보유 카드 부분 업데이트 (전달된 non-null 필드만).
     * WHERE user_card_id AND user_id AND deleted_yn='N' 로 소유권을 한 번 더 보장한다.
     */
    int updateOwnedCard(UserCard userCard);
    
    // 신용/체크카드 통합 신청 현황 조회 (날짜순)
    List<MyCardApplicationResponse> findMyAllApplications(@Param("userId") Long userId);
    // 이건 마이페이지 쪽 서비스에서 구현해야함 !!
}
