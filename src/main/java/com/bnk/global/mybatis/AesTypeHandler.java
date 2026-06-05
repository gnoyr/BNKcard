package com.bnk.global.mybatis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.Alias;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import com.bnk.global.util.AesCryptoUtil;

/**
 * AES-256-GCM 암호화/복호화 TypeHandler.
 *
 * MyBatisConfig에서 Spring Bean으로 생성 후 registry에 직접 등록. XML에서 기본 생성자로 new 하지 않으므로
 * NoSuchMethodException 없음.
 *
 * XML 사용법: resultMap: typeHandler="aesTypeHandler" parameter: #{phone,
 * typeHandler=aesTypeHandler}
 */
@Alias("aesTypeHandler")
@MappedTypes(String.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class AesTypeHandler extends BaseTypeHandler<String> {

	private final AesCryptoUtil aesCryptoUtil;

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
		return aesCryptoUtil.decrypt(rs.getString(columnName));
	}

	@Override
	public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		return aesCryptoUtil.decrypt(rs.getString(columnIndex));
	}

	@Override
	public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		return aesCryptoUtil.decrypt(cs.getString(columnIndex));
	}
}