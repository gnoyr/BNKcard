package com.bnk.domain.spending.mapper;

import com.bnk.domain.spending.model.AiChatLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiChatLogMapper {

    /** user_id nullable — 비로그인 허용 */
    int insertChatLog(AiChatLog chatLog);

    List<AiChatLog> findBySessionId(@Param("sessionId") String sessionId);
}
