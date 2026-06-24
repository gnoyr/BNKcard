package com.bnk.global.config;

import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimplePrivateKeySupplier;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.Region;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;

@Slf4j
@Configuration
@Profile("!test")
public class ObjectStorageConfig {

    // 수정 전: :instance  (기본값이 instance → 로컬에서 타임아웃)
    // 수정 후: :simple    (기본값을 simple로 → local.properties 없어도 안전하게 시작)
    @Value("${oci.auth.type:simple}")
    private String authType;

    @Value("${oci.tenancy-id:}")
    private String tenancyId;

    @Value("${oci.user-id:}")
    private String userId;

    @Value("${oci.fingerprint:}")
    private String fingerprint;

    @Value("${oci.private-key-path:}")
    private String privateKeyPath;

    @Value("${oci.region:ap-chuncheon-1}")
    private String region;

    @Bean
    AbstractAuthenticationDetailsProvider ociAuthProvider() throws IOException {

        if ("simple".equalsIgnoreCase(authType)) {
            // privateKeyPath가 비어있으면 빈 생성 실패 방지
            if (privateKeyPath == null || privateKeyPath.isBlank()) {
                log.warn("[OCI] private-key-path 미설정 — OCI 기능 비활성화");
                return null;
            }
            log.info("[OCI] 인증 방식: SimpleAuthenticationDetailsProvider (로컬 API Key)");
            return SimpleAuthenticationDetailsProvider.builder()
                    .tenantId(tenancyId)
                    .userId(userId)
                    .fingerprint(fingerprint)
                    .privateKeySupplier(new SimplePrivateKeySupplier(privateKeyPath))
                    .region(Region.fromRegionId(region))
                    .build();
        }

        if ("instance".equalsIgnoreCase(authType)) {
            log.info("[OCI] 인증 방식: InstancePrincipals (OCI VM)");
            return InstancePrincipalsAuthenticationDetailsProvider.builder().build();
        }

        log.warn("[OCI] oci.auth.type 미인식 ({}) — OCI 기능 비활성화", authType);
        return null;
    }

    @Bean
    ObjectStorageClient objectStorageClient(
            AbstractAuthenticationDetailsProvider authProvider) {

        if (authProvider == null) {
            log.warn("[OCI] ObjectStorageClient 초기화 건너뜀 (authProvider=null)");
            return null;
        }
        ObjectStorageClient client = ObjectStorageClient.builder()
                .region(Region.fromRegionId(region))
                .build(authProvider);
        log.info("[OCI] ObjectStorageClient 초기화 완료. region={}", region);
        return client;
    }
}