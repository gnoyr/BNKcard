package com.bnk.domain.spending.service;

import com.bnk.domain.spending.dto.request.SpendingPatternRequest;
import com.bnk.domain.spending.dto.response.SpendingChartResponse;
import com.bnk.domain.spending.mapper.SpendingPatternMapper;
import com.bnk.domain.spending.model.SpendingPattern;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class SpendingService {

    private final SpendingPatternMapper spendingPatternMapper;

    // ================================================================
    // RQ-F16 | 소비 패턴 조회 (도넛 차트 데이터)
    // ================================================================

    /**
     * USER_SPENDING_PATTERNS JOIN CARD_CATEGORIES 조회.
     * 전체 합산 대비 각 카테고리 ratio(%) 계산.
     * SpendingChartResponse.of() 정적 팩터리가 비중 계산 담당.
     */
    @Transactional(readOnly = true)
    public List<SpendingChartResponse> getMySpendingPatterns(Long userId) {
        List<SpendingPattern> patterns = spendingPatternMapper.findByUserId(userId);

        long total = patterns.stream()
                .mapToLong(SpendingPattern::getMonthlyAmount)
                .sum();

        List<SpendingChartResponse> result = patterns.stream()
                .map(p -> SpendingChartResponse.of(p, total))
                .collect(Collectors.toList());

        log.debug("[소비패턴조회] userId={} count={} total={}", userId, result.size(), total);
        return result;
    }

    // ================================================================
    // RQ-F18 | 소비 패턴 수동 입력 UPSERT
    // ================================================================

    /**
     * SpendingPatternRequest (기존 spending 도메인 DTO) 재사용.
     * source='MANUAL' 고정 / Oracle MERGE INTO — SpendingPatternMapper.xml 처리.
     *
     * @return UPSERT 처리된 항목 수
     */
    @Transactional
    public int updateSpendingPatterns(Long userId, @Valid SpendingPatternRequest request) {
        int count = 0;
        for (SpendingPatternRequest.PatternItem item : request.getPatterns()) {
            SpendingPattern pattern = SpendingPattern.builder()
                    .userId(userId)
                    .categoryId(item.getCategoryId())
                    .monthlyAmount(item.getMonthlyAmount())
                    .source("MANUAL")
                    .build();
            spendingPatternMapper.upsertPattern(pattern);
            count++;
        }
        log.info("[소비패턴저장] userId={} count={}", userId, count);
        return count;
    }
}
