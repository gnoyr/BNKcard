package com.bnk.global.migration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 암호화 마이그레이션 전용 DTO. TypeHandler 없이 평문 그대로 받기 위해 단순 String 매핑.
 */
@Getter
@Setter
@NoArgsConstructor
public class MigrationUserRow {
	private Long userId;
	private String phone;
	private String ciValue;
	private String birthDate;
}
