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
 */
public class AesBirthDateTypeHandler extends BaseTypeHandler<LocalDate> {

	private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

	private static final DateTimeFormatter ORACLE_TS_FMT = new DateTimeFormatterBuilder()
			.parseCaseInsensitive()
			.appendPattern("dd-MMM-yy hh.mm.ss.SSSSSSSSS a")
			.toFormatter(Locale.ENGLISH);

	private static final DateTimeFormatter ORACLE_DATE_FMT = new DateTimeFormatterBuilder()
			.parseCaseInsensitive()
			.appendPattern("dd-MMM-yy")
			.toFormatter(Locale.ENGLISH);

	private AesCryptoUtil aesCryptoUtil;

	public AesBirthDateTypeHandler() {}

	public AesBirthDateTypeHandler(AesCryptoUtil aesCryptoUtil) {
		this.aesCryptoUtil = aesCryptoUtil;
	}

	// ── Write (암호화) ────────────────────────────────────────────────────────
	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, LocalDate parameter, JdbcType jdbcType)
			throws SQLException {
		ps.setString(i, aesCryptoUtil.encrypt(parameter.format(ISO_FMT)));
	}

	// ── Read (복호화) ─────────────────────────────────────────────────────────
	@Override
	public LocalDate getNullableResult(ResultSet rs, String columnName) throws SQLException {
		return decryptAndParse(rs.getString(columnName));
	}

	@Override
	public LocalDate getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		return decryptAndParse(rs.getString(columnIndex));
	}

	@Override
	public LocalDate getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		return decryptAndParse(cs.getString(columnIndex));
	}

	// ── 내부 헬퍼 ─────────────────────────────────────────────────────────────
	/**
	 * 암호문이면 복호화 후 파싱, 평문이면 바로 파싱.
	 * isEncrypted() 판별을 AesCryptoUtil에 위임하여 단일 기준 유지.
	 */
	private LocalDate decryptAndParse(String value) {
		if (value == null || value.isBlank()) return null;

		if (aesCryptoUtil != null && aesCryptoUtil.isEncrypted(value)) {
			String decrypted = aesCryptoUtil.decrypt(value);
			if (decrypted == null || decrypted.isBlank()) return null;
			return parseFlexible(decrypted);
		}

		// 평문(미암호화 레거시 데이터, 마이그레이션 전 데이터 등)
		return parseFlexible(value);
	}

	private LocalDate parseFlexible(String text) {
		try { return LocalDate.parse(text, ISO_FMT);       } catch (Exception ignored) {}
		try { return LocalDate.parse(text, ORACLE_TS_FMT); } catch (Exception ignored) {}
		try { return LocalDate.parse(text, ORACLE_DATE_FMT);} catch (Exception ignored) {}
		return null;
	}
}
