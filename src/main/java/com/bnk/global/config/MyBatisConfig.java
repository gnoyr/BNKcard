package com.bnk.global.config;

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

/**
 * MyBatis 설정.
 *
 *   암호화 대상 컬럼은 각 XML resultMap / parameter에 명시:
 *     - USERS.phone           → typeHandler="aesTypeHandler"
 *     - USERS.ci_value        → typeHandler="aesTypeHandler"
 *     - USERS.birth_date      → typeHandler="aesBirthDateTypeHandler"
 *     - ADMIN_USERS.phone     → typeHandler="aesTypeHandler"
 *     - WATCHLIST.ci_value    → typeHandler="aesTypeHandler"
 *     - WATCHLIST.birth_date  → typeHandler="aesTypeHandler"
 * ─────────────────────────────────────────────────────────────────
 */
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

        org.apache.ibatis.session.Configuration config =
                new org.apache.ibatis.session.Configuration();
        config.setMapUnderscoreToCamelCase(true);
        config.setDefaultStatementTimeout(30);
        config.setCacheEnabled(false);

        config.getTypeAliasRegistry().registerAlias("aesTypeHandler",          AesTypeHandler.class);
        config.getTypeAliasRegistry().registerAlias("aesBirthDateTypeHandler", AesBirthDateTypeHandler.class);
        
        factoryBean.setConfiguration(config);
        factoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath:mappers/**/*.xml"));

        return factoryBean.getObject();
    }

    @Bean
    SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}