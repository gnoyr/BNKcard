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

import java.io.IOException;

@Slf4j
@Configuration
public class ObjectStorageConfig {

    @Value("${oci.auth.type:instance}")
    private String authType;

    @Value("${oci.tenancy-id:}")
    private String tenancyId;

    @Value("${oci.user-id:}")
    private String userId;

    @Value("${oci.fingerprint:}")
    private String fingerprint;

    @Value("${oci.private-key-path:}")
    private String privateKeyPath;

    @Value("${oci.region}")
    private String region;

    // 반환 타입을 AuthenticationDetailsProvider → AbstractAuthenticationDetailsProvider 로 변경
    @Bean
    AbstractAuthenticationDetailsProvider ociAuthProvider() throws IOException {

        if ("simple".equalsIgnoreCase(authType)) {
            log.info("[OCI] 인증 방식: SimpleAuthenticationDetailsProvider (로컬 API Key)");

            return SimpleAuthenticationDetailsProvider.builder()
                    .tenantId(tenancyId)
                    .userId(userId)
                    .fingerprint(fingerprint)
                    .privateKeySupplier(new SimplePrivateKeySupplier(privateKeyPath))
                    .region(Region.fromRegionId(region))
                    .build();
        }

        log.info("[OCI] 인증 방식: InstancePrincipalsAuthenticationDetailsProvider (OCI VM)");
        return InstancePrincipalsAuthenticationDetailsProvider.builder().build();
    }

    @Bean
    ObjectStorageClient objectStorageClient(
            AbstractAuthenticationDetailsProvider authProvider) {

        ObjectStorageClient client = ObjectStorageClient.builder()
                .region(Region.fromRegionId(region))
                .build(authProvider);

        log.info("[OCI] ObjectStorageClient 초기화 완료. region={}", region);
        return client;
    }
}