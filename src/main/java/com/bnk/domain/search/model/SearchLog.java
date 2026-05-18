package com.bnk.domain.search.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class SearchLog {
    private Long searchLogId;
    private Long userId;            // nullable — 비로그인 허용
    private String keywordRaw;
    private Long matchedKeywordId;  // nullable — 매칭 실패 시 NULL
    private Integer resultCount;
    private LocalDateTime searchAt;
    private String ipAddress;
    private String deviceInfo;
}
