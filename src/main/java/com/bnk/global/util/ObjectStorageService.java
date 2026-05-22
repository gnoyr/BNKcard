package com.bnk.global.util;

import java.io.ByteArrayInputStream;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.CreatePreauthenticatedRequestDetails;
import com.oracle.bmc.objectstorage.requests.CreatePreauthenticatedRequestRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.CreatePreauthenticatedRequestResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ObjectStorageService {

    // required = false — OCI 미설정 환경에서도 서버 기동 가능
    @Autowired(required = false)
    private ObjectStorageClient objectStorageClient;

    @Value("${oci.namespace:}")
    private String namespace;

    @Value("${oci.bucket-name:}")
    private String bucketName;

    @Value("${oci.region:ap-chuncheon-1}")
    private String region;

    /**
     * Object Storage 업로드.
     * OCI 미연결 시 로컬 폴백 — objectName을 그대로 반환 (개발 환경용).
     */
    public String upload(String objectName, byte[] content, String contentType) {
        if (objectStorageClient == null) {
            log.warn("[ObjectStorage] 클라이언트 미연결 — 업로드 건너뜀: objectName={}", objectName);
            return objectName;   // 개발 환경에서는 objectName만 반환
        }
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(bucketName)
                    .objectName(objectName)
                    .contentLength((long) content.length)
                    .contentType(contentType)
                    .putObjectBody(new ByteArrayInputStream(content))
                    .build();

            objectStorageClient.putObject(putRequest);
            log.info("[ObjectStorage] 업로드 완료: objectName={}", objectName);
            return objectName;

        } catch (Exception e) {
            log.error("[ObjectStorage] 업로드 실패: objectName={}", objectName, e);
            throw new RuntimeException("Object Storage 업로드 실패: " + objectName, e);
        }
    }

    /**
     * Public URL 생성 (만료 없음).
     * OCI 미연결 시 더미 URL 반환.
     */
    public String getPublicUrl(String objectName) {
        if (namespace == null || namespace.isBlank()) {
            return "https://localhost/dev-only/" + objectName;  // 개발 환경 더미
        }
        return String.format(
            "https://objectstorage.%s.oraclecloud.com/n/%s/b/%s/o/%s",
            region, namespace, bucketName, objectName
        );
    }

    /**
     * Pre-Authenticated Request URL 생성 (24시간 만료).
     * Public 버킷이 아닌 경우 사용.
     */
    public String createDownloadUrl(String objectName) {
        if (objectStorageClient == null) {
            log.warn("[ObjectStorage] 클라이언트 미연결 — PAR URL 생성 건너뜀");
            return getPublicUrl(objectName);
        }
        try {
            Date expiry = new Date(System.currentTimeMillis() + 24L * 60 * 60 * 1000);
            CreatePreauthenticatedRequestDetails details =
                    CreatePreauthenticatedRequestDetails.builder()
                            .name("par-" + objectName.replace("/", "-"))
                            .objectName(objectName)
                            .accessType(CreatePreauthenticatedRequestDetails.AccessType.ObjectRead)
                            .timeExpires(expiry)
                            .build();

            CreatePreauthenticatedRequestResponse response =
                    objectStorageClient.createPreauthenticatedRequest(
                            CreatePreauthenticatedRequestRequest.builder()
                                    .namespaceName(namespace)
                                    .bucketName(bucketName)
                                    .createPreauthenticatedRequestDetails(details)
                                    .build()
                    );

            String parPath = response.getPreauthenticatedRequest().getAccessUri();
            String parUrl = "https://objectstorage." + region + ".oraclecloud.com" + parPath;
            log.info("[ObjectStorage] PAR URL 생성: objectName={}", objectName);
            return parUrl;

        } catch (Exception e) {
            log.error("[ObjectStorage] PAR URL 생성 실패: objectName={}", objectName, e);
            return buildUrl(objectName);
        }
    }

    private String buildUrl(String objectName) {
        return String.format(
            "https://objectstorage.%s.oraclecloud.com/n/%s/b/%s/o/%s",
            region, namespace, bucketName, objectName
        );
    }
}