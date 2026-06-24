package com.bnk.domain.ai.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bnk.domain.ai.model.AiChatLog;

import java.util.List;

@Mapper
public interface AiChatLogMapper {

    /** user_id nullable — 비로그인 허용 */
    int insertChatLog(AiChatLog chatLog);

    List<AiChatLog> findBySessionId(@Param("sessionId") String sessionId);
}
