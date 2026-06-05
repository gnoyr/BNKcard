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
	SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
		SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
		factoryBean.setDataSource(dataSource);
		factoryBean.setMapperLocations(
				new PathMatchingResourcePatternResolver().getResources("classpath:mappers/**/*.xml"));

		org.apache.ibatis.session.Configuration config = new org.apache.ibatis.session.Configuration();
		config.setMapUnderscoreToCamelCase(true);
		config.setDefaultStatementTimeout(30);
		config.setCacheEnabled(false);

		// AES TypeHandler 등록
		config.getTypeHandlerRegistry().register(String.class, new AesTypeHandler(aesCryptoUtil));

		// birth_date 전용 TypeHandler 등록
		config.getTypeHandlerRegistry().register(LocalDate.class, new AesBirthDateTypeHandler(aesCryptoUtil));

		factoryBean.setConfiguration(config);
		return factoryBean.getObject();
	}

	@Bean
	SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
		return new SqlSessionTemplate(sqlSessionFactory);
	}
}