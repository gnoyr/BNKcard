package com.bnk.domain.spending.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.bnk.domain.spending.dto.request.SpendingPatternRequest;
import com.bnk.domain.spending.dto.response.SpendingChartResponse;
import com.bnk.domain.spending.mapper.SpendingPatternMapper;
import com.bnk.domain.spending.model.SpendingPattern;

/**
 * SpendingService 단위 테스트 (SonarQube 커버리지 대상)
 *
 * ── 설계 원칙 ────────────────────────────────────────────────────────
 * · MockitoExtension — Spring Context 없이 경량 실행
 * · SpendingChartResponse.of() 의 ratio 계산 로직까지 간접 검증
 * · UPSERT 배치 횟수(upsertPattern 호출 수) ArgumentCaptor 검증
 *
 * ── 커버리지 대상 메서드 ──────────────────────────────────────────────
 * · getMySpendingPatterns(Long) — 정상/빈목록/단일/total=0
 * · updateSpendingPatterns(Long, SpendingPatternRequest) — 정상/단건/다건
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SpendingService 단위 테스트")
class SpendingServiceTest {

    /* ── Mocks ─────────────────────────────────────────────────── */
    @Mock
    private SpendingPatternMapper spendingPatternMapper;

    @InjectMocks
    private SpendingService spendingService;

    // ════════════════════════════════════════════════════════════════
    // Fixture 헬퍼
    // ════════════════════════════════════════════════════════════════

    /** SpendingPattern 픽스처 */
    private SpendingPattern pattern(Long categoryId, String categoryName, long monthlyAmount) {
        SpendingPattern p = new SpendingPattern();
        ReflectionTestUtils.setField(p, "categoryId",    categoryId);
        ReflectionTestUtils.setField(p, "categoryName",  categoryName);
        ReflectionTestUtils.setField(p, "monthlyAmount", monthlyAmount);
        ReflectionTestUtils.setField(p, "source",        "MANUAL");
        return p;
    }

    /** SpendingPatternRequest 픽스처 빌더 */
    private SpendingPatternRequest spendingReq(List<SpendingPatternRequest.PatternItem> items) {
        SpendingPatternRequest req = new SpendingPatternRequest();
        ReflectionTestUtils.setField(req, "patterns", items);
        return req;
    }

    /** SpendingPatternRequest.PatternItem 픽스처 */
    private SpendingPatternRequest.PatternItem patternItem(Long categoryId, long monthlyAmount) {
        SpendingPatternRequest.PatternItem item = new SpendingPatternRequest.PatternItem();
        ReflectionTestUtils.setField(item, "categoryId",    categoryId);
        ReflectionTestUtils.setField(item, "monthlyAmount", monthlyAmount);
        return item;
    }

    // ════════════════════════════════════════════════════════════════
    // 1. getMySpendingPatterns
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("소비 패턴 조회 [getMySpendingPatterns]")
    class GetMySpendingPatterns {

        @Test
        @DisplayName("[정상] 2개 카테고리 → ratio 합산 검증 (식비 60%, 교통 40%)")
        void 정상_2개카테고리_ratio계산() {
            // given
            Long userId = 1L;
            SpendingPattern food    = pattern(1L, "식비", 600_000L);
            SpendingPattern transit = pattern(2L, "교통", 400_000L);
            given(spendingPatternMapper.findByUserId(userId)).willReturn(List.of(food, transit));

            // when
            List<SpendingChartResponse> result = spendingService.getMySpendingPatterns(userId);

            // then
            assertThat(result).hasSize(2);
            // total = 1,000,000 → 식비 60.0%, 교통 40.0%
            SpendingChartResponse foodChart = result.stream()
                    .filter(r -> r.getCategoryId().equals(1L))
                    .findFirst().orElseThrow();
            assertThat(foodChart.getMonthlyAmount()).isEqualTo(600_000L);
            // ratio 검증 (반올림 오차 허용 ±0.1)
            assertThat(foodChart.getRatio()).isCloseTo(60.0, org.assertj.core.data.Offset.offset(0.1));
        }

        @Test
        @DisplayName("[정상] 단일 카테고리 → ratio = 100.0")
        void 정상_단일카테고리_ratio100() {
            // given
            Long userId = 2L;
            given(spendingPatternMapper.findByUserId(userId))
                    .willReturn(List.of(pattern(3L, "쇼핑", 300_000L)));

            // when
            List<SpendingChartResponse> result = spendingService.getMySpendingPatterns(userId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRatio()).isCloseTo(100.0, org.assertj.core.data.Offset.offset(0.01));
        }

        @Test
        @DisplayName("[정상] 소비 패턴 없음 → 빈 목록 반환")
        void 정상_빈목록반환() {
            // given
            Long userId = 3L;
            given(spendingPatternMapper.findByUserId(userId)).willReturn(Collections.emptyList());

            // when
            List<SpendingChartResponse> result = spendingService.getMySpendingPatterns(userId);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("[정상] total=0 (monthlyAmount=0) → ratio=0.0, 예외 없음")
        void 정상_total0_ratio0() {
            Long userId = 4L;
            SpendingPattern zero = pattern(5L, "의료", 0L);
            given(spendingPatternMapper.findByUserId(userId)).willReturn(List.of(zero));

            List<SpendingChartResponse> result = spendingService.getMySpendingPatterns(userId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRatio()).isZero();
        }

        @Test
        @DisplayName("[정상] 3개 카테고리 → Mapper 1회 호출 & 결과 크기 검증")
        void 정상_3개카테고리_mapper호출검증() {
            // given
            Long userId = 5L;
            given(spendingPatternMapper.findByUserId(userId)).willReturn(List.of(
                    pattern(1L, "식비",   500_000L),
                    pattern(2L, "교통",   200_000L),
                    pattern(3L, "문화",   300_000L)
            ));

            // when
            List<SpendingChartResponse> result = spendingService.getMySpendingPatterns(userId);

            // then
            assertThat(result).hasSize(3);
            then(spendingPatternMapper).should(times(1)).findByUserId(userId);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 2. updateSpendingPatterns
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("소비 패턴 수동 입력 UPSERT [updateSpendingPatterns]")
    class UpdateSpendingPatterns {

        @Test
        @DisplayName("[정상] 3개 항목 → upsertPattern 3회 호출 & 반환값=3")
        void 정상_3개항목_upsert3회() {
            // given
            Long userId = 10L;
            SpendingPatternRequest req = spendingReq(List.of(
                    patternItem(1L, 500_000L),
                    patternItem(2L, 200_000L),
                    patternItem(3L, 300_000L)
            ));

            // when
            int result = spendingService.updateSpendingPatterns(userId, req);

            // then
            assertThat(result).isEqualTo(3);
            then(spendingPatternMapper).should(times(3)).upsertPattern(any(SpendingPattern.class));
        }

        @Test
        @DisplayName("[정상] 단건 항목 → upsertPattern 1회 & 반환값=1")
        void 정상_단건항목() {
            // given
            Long userId = 11L;
            SpendingPatternRequest req = spendingReq(List.of(patternItem(10L, 150_000L)));

            // when
            int result = spendingService.updateSpendingPatterns(userId, req);

            // then
            assertThat(result).isEqualTo(1);
            then(spendingPatternMapper).should(times(1)).upsertPattern(any(SpendingPattern.class));
        }

        @Test
        @DisplayName("[정상] ArgumentCaptor — upsert 파라미터 userId·source·categoryId 검증")
        void 정상_upsert파라미터검증() {
            // given
            Long userId = 12L;
            SpendingPatternRequest req = spendingReq(List.of(patternItem(7L, 250_000L)));

            // when
            spendingService.updateSpendingPatterns(userId, req);

            // then
            ArgumentCaptor<SpendingPattern> captor = ArgumentCaptor.forClass(SpendingPattern.class);
            then(spendingPatternMapper).should().upsertPattern(captor.capture());

            SpendingPattern captured = captor.getValue();
            assertThat(captured.getUserId()).isEqualTo(userId);
            assertThat(captured.getCategoryId()).isEqualTo(7L);
            assertThat(captured.getMonthlyAmount()).isEqualTo(250_000L);
            assertThat(captured.getSource()).isEqualTo("MANUAL"); // source 고정값 검증
        }

        @Test
        @DisplayName("[정상] 빈 패턴 목록 → upsertPattern 0회 & 반환값=0")
        void 정상_빈목록_upsert0회() {
            // given
            Long userId = 13L;
            SpendingPatternRequest req = spendingReq(Collections.emptyList());

            // when
            int result = spendingService.updateSpendingPatterns(userId, req);

            // then
            assertThat(result).isEqualTo(0);
            then(spendingPatternMapper).should(times(0)).upsertPattern(any());
        }

        @Test
        @DisplayName("[정상] 10개 항목 → 순서대로 upsert 10회 처리")
        void 정상_10개항목_순서처리() {
            // given
            Long userId = 14L;
            List<SpendingPatternRequest.PatternItem> items = new java.util.ArrayList<>();
            for (long i = 1; i <= 10; i++) {
                items.add(patternItem(i, i * 10_000L));
            }
            SpendingPatternRequest req = spendingReq(items);

            // when
            int result = spendingService.updateSpendingPatterns(userId, req);

            // then
            assertThat(result).isEqualTo(10);
            then(spendingPatternMapper).should(times(10)).upsertPattern(any(SpendingPattern.class));
        }
    }
}