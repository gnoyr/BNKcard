package com.bnk.global.log.mapper;

import com.bnk.global.log.model.*;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EventLogMapper {
    void insertParent(EventLog log);
    void insertCardLog(CardEventLog log);
    void insertTermsLog(TermsEventLog log);
    void insertChatLog(ChatEventLog log);
}