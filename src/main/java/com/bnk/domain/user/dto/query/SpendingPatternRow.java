package com.bnk.domain.user.dto.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 소비 패턴 MyBatis 쿼리 결과 매핑 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpendingPatternRow {
	private Long categoryId;
	private String categoryName;
	private BigDecimal monthlyAmount;
	private String iconCode;
}
