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
 *  - typeAliasRegistry 에 별칭만 등록
 *    → XML에서 typeHandler="aesTypeHandler" 로 명시한 컬럼에만 적용
 *  - register() 호출 제거
 *    → 이전에 register()를 호출하면 @MappedTypes({})와 무관하게
 *       MyBatis가 제네릭 타입(String/LocalDate) 전역에 바인딩하여
 *       typeHandler 미명시 String 컬럼(cdd_status_code, ip_address_hash 등)까지
 *       AES 암호화가 적용되는 버그가 발생했음 → 제거로 해결
 *  - STATIC_UTIL 패턴
 *    → @Bean 으로 먼저 생성된 인스턴스가 STATIC_UTIL 에 aesCryptoUtil 을 저장
 *    → 이후 MyBatis가 별칭으로 기본 생성자를 호출해도 STATIC_UTIL 을 통해 정상 동작
 *
 * [암호화 대상 컬럼]
 *   - USERS.phone              → typeHandler="aesTypeHandler"
 *   - USERS.ci_value           → typeHandler="aesTypeHandler"
 *   - USERS.birth_date         → typeHandler="aesBirthDateTypeHandler"
 *   - ADMIN_USERS.phone        → typeHandler="aesTypeHandler"
 *   - WATCHLIST.ci_value       → typeHandler="aesTypeHandler"
 *   - WATCHLIST.birth_date     → typeHandler="aesTypeHandler"
 *   - USER_TRUSTED_IPS.ip_address → typeHandler="aesTypeHandler"
 *   
 *   // - CREDIT_CARD_APPLICATIONS.applicant_snapshot → typeHandler="aesTypeHandler"
	 // - CREDIT_CARD_APPLICATIONS.payment_snapshot   → typeHandler="aesTypeHandler"
	 // - CHECK_CARD_APPLICATIONS.applicant_snapshot  → typeHandler="aesTypeHandler"
	 // - CHECK_CARD_APPLICATIONS.payment_snapshot    → typeHandler="aesTypeHandler"
 */
@Configuration
@RequiredArgsConstructor
@MapperScan({
    "com.bnk.domain.**.mapper",   // domain 하위 모든 mapper 패키지
    "com.bnk.global.log.mapper",  // EventLogMapper
    "com.bnk.global.util.audit",  // AuditLogMapper
    "com.bnk.global.migration"    // TrustedIpMigrationMapper
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

        // @Bean 호출로 인스턴스 생성 → STATIC_UTIL 에 aesCryptoUtil 저장
        aesTypeHandler();
        aesBirthDateTypeHandler();

        // 별칭만 등록 — XML에서 명시한 컬럼에만 적용됨
        // register() 를 호출하지 않으므로 String/LocalDate 전역 바인딩 없음
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