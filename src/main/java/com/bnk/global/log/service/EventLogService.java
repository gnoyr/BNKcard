package com.bnk.global.log.service;

import com.bnk.global.log.mapper.EventLogMapper;
import com.bnk.global.log.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventLogService {

    private final EventLogMapper eventLogMapper;

    // @Async: 로그 저장이 메인 트랜잭션을 느리게 만들지 않도록 비동기 처리
    // Propagation.REQUIRES_NEW: 메인 로직 실패해도 로그는 독립적으로 저장
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveCardLog(EventLog parent, CardEventLog child) {
        try {
            eventLogMapper.insertParent(parent);
            child.setLogId(parent.getLogId());
            eventLogMapper.insertCardLog(child);
        } catch (Exception e) {
            log.error("[EventLog] 카드 로그 저장 실패", e);
        }
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveTermsLog(EventLog parent, TermsEventLog child) {
        try {
            eventLogMapper.insertParent(parent);
            child.setLogId(parent.getLogId());
            eventLogMapper.insertTermsLog(child);
        } catch (Exception e) {
            log.error("[EventLog] 약관 로그 저장 실패", e);
        }
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveChatLog(EventLog parent, ChatEventLog child) {
        try {
            eventLogMapper.insertParent(parent);
            child.setLogId(parent.getLogId());
            eventLogMapper.insertChatLog(child);
        } catch (Exception e) {
            log.error("[EventLog] 챗봇 로그 저장 실패", e);
        }
    }
    
    // EventLogService.java에 추가
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveParentOnly(EventLog parent) {
        try {
            eventLogMapper.insertParent(parent);
        } catch (Exception e) {
            log.error("[EventLog] 로그 저장 실패", e);
        }
    }
}