package com.bnk.domain.application.mapper;

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
}
