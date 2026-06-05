package com.bnk.global.config;

import java.time.LocalDate;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.bnk.global.mybatis.AesBirthDateTypeHandler;
import com.bnk.global.mybatis.AesTypeHandler;
import com.bnk.global.util.AesCryptoUtil;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class MyBatisConfig {

	private final AesCryptoUtil aesCryptoUtil;

	@Bean
	AesTypeHandler aesTypeHandler() {
		return new AesTypeHandler(aesCryptoUtil);
	}

	@Bean
	AesBirthDateTypeHandler aesBirthDateTypeHandler() {
		return new AesBirthDateTypeHandler(aesCryptoUtil);
	}

	@Bean
	SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
		SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
		factoryBean.setDataSource(dataSource);
		factoryBean.setMapperLocations(
				new PathMatchingResourcePatternResolver().getResources("classpath:mappers/**/*.xml"));

		org.apache.ibatis.session.Configuration config = new org.apache.ibatis.session.Configuration();
		config.setMapUnderscoreToCamelCase(true);
		config.setDefaultStatementTimeout(30);
		config.setCacheEnabled(false);

		// ── TypeHandler 인스턴스를 registry에 직접 등록 ──────────────
		// Spring Bean으로 만든 인스턴스를 주입하므로 MyBatis가
		// new AesTypeHandler() 기본 생성자를 호출하지 않음.
		AesTypeHandler aesHandler = aesTypeHandler();
		AesBirthDateTypeHandler aesBirthHandler = aesBirthDateTypeHandler();

		config.getTypeHandlerRegistry().register(aesHandler);
		config.getTypeHandlerRegistry().register(LocalDate.class, aesBirthHandler);

		// ── XML에서 alias로 참조할 수 있도록 TypeAliasRegistry에도 등록 ──
		// XML: typeHandler="aesTypeHandler" 로 참조 가능해짐
		config.getTypeAliasRegistry().registerAlias("aesTypeHandler", AesTypeHandler.class);
		config.getTypeAliasRegistry().registerAlias("aesBirthDateTypeHandler", AesBirthDateTypeHandler.class);

		factoryBean.setConfiguration(config);
		return factoryBean.getObject();
	}

	@Bean
	SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
		return new SqlSessionTemplate(sqlSessionFactory);
	}
}