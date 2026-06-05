package com.bnk.global.mybatis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import com.bnk.global.util.AesCryptoUtil;

/**
 * birth_date 전용 TypeHandler. LocalDate ↔ AES 암호화 String 변환. DB 컬럼:
 * VARCHAR2(200) User.java birthDate 타입은 LocalDate 그대로 유지.
 */
public class AesBirthDateTypeHandler extends BaseTypeHandler<LocalDate> {

	private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;

	private final AesCryptoUtil aesCryptoUtil;

	public AesBirthDateTypeHandler(AesCryptoUtil aesCryptoUtil) {
		this.aesCryptoUtil = aesCryptoUtil;
	}

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, LocalDate parameter, JdbcType jdbcType)
			throws SQLException {
		ps.setString(i, aesCryptoUtil.encrypt(parameter.format(FMT)));
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
		String decrypted = aesCryptoUtil.decrypt(value);
		if (decrypted == null || decrypted.isBlank())
			return null;
		return LocalDate.parse(decrypted, FMT);
	}
}