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
 */
@Alias("aesTypeHandler")
public class AesTypeHandler extends BaseTypeHandler<String> {

	private AesCryptoUtil aesCryptoUtil;

	public AesTypeHandler() {}

	public AesTypeHandler(AesCryptoUtil aesCryptoUtil) {
		this.aesCryptoUtil = aesCryptoUtil;
	}

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
			throws SQLException {
		ps.setString(i, aesCryptoUtil.encrypt(parameter));
	}

	@Override
	public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
	    String value = rs.getString(columnName);
	    if (value == null) return null;
	    // aesCryptoUtil이 null이거나 암호화된 값이 아니면 원본 반환
	    if (aesCryptoUtil == null) return value;
	    try {
	        return aesCryptoUtil.decrypt(value);
	    } catch (Exception e) {
	        return value; // 복호화 실패 시 원본값 반환
	    }
	}

	@Override
	public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
	    String value = rs.getString(columnIndex);
	    if (value == null) return null;
	    if (aesCryptoUtil == null) return value;
	    try {
	        return aesCryptoUtil.decrypt(value);
	    } catch (Exception e) {
	        return value;
	    }
	}

	@Override
	public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
	    String value = cs.getString(columnIndex);
	    if (value == null) return null;
	    if (aesCryptoUtil == null) return value;
	    try {
	        return aesCryptoUtil.decrypt(value);
	    } catch (Exception e) {
	        return value;
	    }
	}
}