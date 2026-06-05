package com.bnk.global.mybatis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import com.bnk.global.util.AesCryptoUtil;

/**
 * birth_date 전용 TypeHandler. LocalDate ↔ AES 암호화 String 변환.
 *
 * DB 컬럼 혼재 상황 대응: 1. AES 암호화값 → AES 복호화 후 ISO 파싱 (정상 상태) 2. ISO 평문 → 그대로 ISO 파싱
 * (마이그레이션 대상) 3. Oracle TIMESTAMP 문자열 → 전용 파서로 파싱 (타입 변환 잔류 데이터)
 */
public class AesBirthDateTypeHandler extends BaseTypeHandler<LocalDate> {

	// 정상 저장 포맷
	private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_LOCAL_DATE; // "YYYY-MM-DD"

	// Oracle TIMESTAMP 문자열 포맷: "16-JUL-03 12.00.00.000000000 AM"
	// → Oracle NLS_DATE_FORMAT 기본값 DD-MON-YY 패턴
	private static final DateTimeFormatter ORACLE_TS_FMT = new DateTimeFormatterBuilder().parseCaseInsensitive()
			.appendPattern("dd-MMM-yy hh.mm.ss.SSSSSSSSS a").toFormatter(Locale.ENGLISH);

	// Oracle 날짜만 있는 경우: "16-JUL-03" (TIMESTAMP 없는 단순 DATE)
	private static final DateTimeFormatter ORACLE_DATE_FMT = new DateTimeFormatterBuilder().parseCaseInsensitive()
			.appendPattern("dd-MMM-yy").toFormatter(Locale.ENGLISH);

	private final AesCryptoUtil aesCryptoUtil;

	public AesBirthDateTypeHandler(AesCryptoUtil aesCryptoUtil) {
		this.aesCryptoUtil = aesCryptoUtil;
	}

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, LocalDate parameter, JdbcType jdbcType)
			throws SQLException {
		ps.setString(i, aesCryptoUtil.encrypt(parameter.format(ISO_FMT)));
	}

	@Override
	public LocalDate getNullableResult(ResultSet rs, String columnName) throws SQLException {
		return decrypt(rs.getString(columnName));
	}

	@Override
	public LocalDate getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		return decrypt(rs.getString(columnIndex));
	}

	@Override
	public LocalDate getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		return decrypt(cs.getString(columnIndex));
	}

	private LocalDate decrypt(String value) {
		if (value == null || value.isBlank())
			return null;

		// ① AES 암호화값 → 복호화 후 파싱
		if (aesCryptoUtil.isEncrypted(value)) {
			String decrypted = aesCryptoUtil.decrypt(value);
			if (decrypted == null || decrypted.isBlank())
				return null;
			return parseFlexible(decrypted);
		}

		// ② 평문 → 그대로 파싱 (ISO 또는 Oracle 형식)
		return parseFlexible(value);
	}

	/**
	 * ISO / Oracle TIMESTAMP / Oracle DATE 형식을 순서대로 시도
	 */
	private LocalDate parseFlexible(String text) {
		// 1순위: ISO "YYYY-MM-DD"
		try {
			return LocalDate.parse(text, ISO_FMT);
		} catch (Exception ignored) {
		}

		// 2순위: Oracle TIMESTAMP "16-JUL-03 12.00.00.000000000 AM"
		try {
			return LocalDate.parse(text, ORACLE_TS_FMT);
		} catch (Exception ignored) {
		}

		// 3순위: Oracle DATE "16-JUL-03"
		try {
			return LocalDate.parse(text, ORACLE_DATE_FMT);
		} catch (Exception ignored) {
		}

		// 모두 실패 → null 반환 (파싱 불가 데이터는 마이그레이션으로 처리)
		return null;
	}
}