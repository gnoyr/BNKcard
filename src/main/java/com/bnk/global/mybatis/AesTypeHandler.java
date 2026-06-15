package com.bnk.global.mybatis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.Alias;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import com.bnk.global.util.AesCryptoUtil;

/**
 * AES-256-GCM 암호화/복호화 TypeHandler.
 *
 * ※ @MappedTypes / @MappedJdbcTypes 제거 — 자동 적용 방지.
 *   암호화가 필요한 컬럼은 XML에서 typeHandler=aesTypeHandler 로 명시할 것.
 *
 * XML 사용법:
 *   resultMap:  typeHandler="aesTypeHandler"
 *   parameter:  #{phone, typeHandler=aesTypeHandler}
 *
 */
@Alias("aesTypeHandler")
public class AesTypeHandler extends BaseTypeHandler<String> {

	private AesCryptoUtil aesCryptoUtil;

	public AesTypeHandler() {}

	public AesTypeHandler(AesCryptoUtil aesCryptoUtil) {
		this.aesCryptoUtil = aesCryptoUtil;
	}

	// ── Write (암호화) ────────────────────────────────────────────────────────
	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
			throws SQLException {
		ps.setString(i, aesCryptoUtil.encrypt(parameter));
	}

	// ── Read (복호화) ─────────────────────────────────────────────────────────
	@Override
	public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
		return decryptSafely(rs.getString(columnName));
	}

	@Override
	public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		return decryptSafely(rs.getString(columnIndex));
	}

	@Override
	public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		return decryptSafely(cs.getString(columnIndex));
	}

	// ── 내부 복호화 헬퍼 ──────────────────────────────────────────────────────
	/**
	 * AesCryptoUtil.isEncrypted() 결과에 따라 복호화 여부를 결정한다.
	 *
	 * - null / 빈값  → 원문 반환
	 * - aesCryptoUtil == null → 원문 반환 (마이그레이션 전용 컨텍스트 등)
	 * - isEncrypted() == false → 평문이므로 원문 반환 (로그 없음)
	 * - isEncrypted() == true  → 복호화 수행 (실패 시 AesCryptoUtil에서 ERROR 로그)
	 */
	private String decryptSafely(String value) {
		if (value == null || value.isBlank()) return value;
		if (aesCryptoUtil == null)            return value;

		// isEncrypted()가 false면 평문 → 그대로 반환 (WARN 로그 없음)
		if (!aesCryptoUtil.isEncrypted(value)) return value;

		return aesCryptoUtil.decrypt(value);
	}
}
