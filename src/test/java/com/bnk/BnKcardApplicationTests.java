package com.bnk;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.bnk.domain.card.scheduler.CardScheduler;
import com.bnk.domain.spending.service.CardSearchService;
import com.oracle.bmc.objectstorage.ObjectStorageClient;

/**
 * Spring Boot 컨텍스트 로딩 테스트
 *
 * [수정 이력]
 * - @ActiveProfiles("test") 추가
 * - @MockitoBean ObjectStorageClient 추가
 *   ObjectStorageConfig @Profile("!test") 비활성 → ObjectStorageService 주입 실패 방지
 * - @MockitoBean CardScheduler 추가 (DB 의존 스케줄러 즉시 실행 방지)
 * - TestConfig 내부 클래스 추가: DataSource Bean 수동 등록
 *   이유: spring.autoconfigure.exclude로 DataSourceAutoConfiguration을 제외하지만
 *         MyBatisConfig.sqlSessionFactory(DataSource dataSource)는 DataSource 파라미터를 직접 요구.
 *         AutoConfiguration 제외 시 DataSource 빈이 없어 SqlSessionFactory 생성 실패.
 *         → H2 EmbeddedDatabase를 명시적으로 등록하여 해결.
 */
@SpringBootTest
@ActiveProfiles("test")
class BnKcardApplicationTests {

    @MockitoBean
    private ObjectStorageClient objectStorageClient;

    @MockitoBean
    private CardScheduler cardScheduler;
    
    @MockitoBean
    private CardSearchService cardSearchService;

    /**
     * DataSourceAutoConfiguration 제외로 인한 DataSource 빈 부재 보완.
     * MyBatisConfig가 DataSource를 직접 파라미터로 요구하므로 H2 DataSource를 수동 등록.
     */
    @TestConfiguration
    static class TestDataSourceConfig {
        @Bean
        DataSource dataSource() {
            return new EmbeddedDatabaseBuilder()
                    .setType(EmbeddedDatabaseType.H2)
                    .build();
        }
    }

    @Test
    void contextLoads() {
    }
}
