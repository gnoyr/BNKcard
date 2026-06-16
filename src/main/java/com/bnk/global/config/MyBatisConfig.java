package com.bnk.global.config;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.bnk.global.mybatis.AesBirthDateTypeHandler;
import com.bnk.global.mybatis.AesTypeHandler;
import com.bnk.global.util.AesCryptoUtil;

import lombok.RequiredArgsConstructor;

/**
 * MyBatis 설정.
 *
 * [TypeHandler 등록 전략]
 *  - AesTypeHandler / AesBirthDateTypeHandler 에 @MappedTypes({}) 선언
 *    → MyBatis가 String/LocalDate 타입 전체에 자동 바인딩하지 않음
 *    → XML에서 typeHandler="aesTypeHandler" 로 명시한 컬럼에만 적용
 *  - setTypeHandlers()로 aesCryptoUtil이 주입된 실제 인스턴스를 등록
 *    → XML에서 클래스명으로 찾을 때 기본 생성자 대신 이 인스턴스 재사용
 *
 * [암호화 대상 컬럼]
 *   - USERS.phone              → typeHandler="aesTypeHandler"
 *   - USERS.ci_value           → typeHandler="aesTypeHandler"
 *   - USERS.birth_date         → typeHandler="aesBirthDateTypeHandler"
 *   - ADMIN_USERS.phone        → typeHandler="aesTypeHandler"
 *   - WATCHLIST.ci_value       → typeHandler="aesTypeHandler"
 *   - WATCHLIST.birth_date     → typeHandler="aesTypeHandler"
 *   - USER_TRUSTED_IPS.ip_address → typeHandler="aesTypeHandler"
 */
@Configuration
@RequiredArgsConstructor
@MapperScan({
    "com.bnk.domain.**.mapper",
    "com.bnk.global.**.mapper"
})
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
        config.setJdbcTypeForNull(org.apache.ibatis.type.JdbcType.NULL);

        // 먼저 인스턴스 생성 → 정적 참조 세팅
        AesTypeHandler aesHandler = aesTypeHandler();
        AesBirthDateTypeHandler aesBdHandler = aesBirthDateTypeHandler();

        config.getTypeAliasRegistry().registerAlias("aesTypeHandler",          AesTypeHandler.class);
        config.getTypeAliasRegistry().registerAlias("aesBirthDateTypeHandler", AesBirthDateTypeHandler.class);

        // @MappedTypes({}) 덕분에 String 전역 바인딩 없이 인스턴스만 등록
        config.getTypeHandlerRegistry().register(aesHandler);
        config.getTypeHandlerRegistry().register(aesBdHandler);

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