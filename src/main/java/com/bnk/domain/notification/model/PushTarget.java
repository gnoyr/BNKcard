package com.bnk.domain.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FCM 푸시 발송 대상 1건.
 * NotificationMapper.findPushTargets() 결과 매핑용.
 * (push_enabled='Y' AND push_token IS NOT NULL 인 회원만 조회된다)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushTarget {
    private Long userId;
    private String pushToken;
}
