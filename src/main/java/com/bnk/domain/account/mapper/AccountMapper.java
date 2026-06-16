package com.bnk.domain.account.mapper;

import com.bnk.domain.account.model.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AccountMapper {

    /** 계좌 등록 */
    void insertAccount(Account account);

    /** 사용자 계좌 목록 조회 */
    List<Account> findByUserId(@Param("userId") Long userId);

    /** 계좌 단건 조회 */
    Account findByAccountId(@Param("accountId") Long accountId);

    /** 다음 계좌 일련번호 조회 (채번용) */
    Long nextAccountSeq();
    
    /** 계좌 비밀번호 저장 */
    void insertAccountPassword(@Param("accountId") Long accountId,
                               @Param("passwordHash") String passwordHash);
}