package com.bnk.global.util;

import java.io.ByteArrayInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.requests.CreatePreauthenticatedRequestRequest;
import com.oracle.bmc.objectstorage.model.CreatePreauthenticatedRequestDetails;
import com.oracle.bmc.objectstorage.responses.CreatePreauthenticatedRequestResponse;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObjectStorageService {

    private final ObjectStorageClient objectStorageClient;

    @Value("${oci.namespace}")
    private String namespace;

    @Value("${oci.bucket-name}")
    private String bucketName;

    @Value("${oci.region}")
    private String region;

    public String upload(String objectName, byte[] content, String contentType) {
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
            
          
            @SuppressWarnings("unused")
			String url = buildUrl(objectName);
            
            log.info("[ObjectStorage] 업로드 완료: objectName={}", objectName);
            return objectName;

        } catch (Exception e) {
            log.error("[ObjectStorage] 업로드 실패: objectName={}", objectName, e);
            throw new RuntimeException("Object Storage 업로드 실패: " + objectName, e);
        }
    }
    
    /**
     * 다운로드용 Pre-Authenticated Request URL 생성 (유효기간 24시간).
     * Public 버킷이 아닌 경우 외부 접근에 사용.
     */
    public String createDownloadUrl(String objectName) {
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
            // PAR 실패 시 기본 URL 폴백 (내부 서비스 접근용)
            return buildUrl(objectName);
        }
    }

    private String buildUrl(String objectName) {
        return String.format(
            "https://objectstorage.%s.oraclecloud.com/n/%s/b/%s/o/%s",
            region, namespace, bucketName, objectName   // ← %2F 인코딩 제거
        );
    }
    
    public String getPublicUrl(String objectName) {
        return String.format(
            "https://objectstorage.%s.oraclecloud.com/n/%s/b/%s/o/%s",
            region, namespace, bucketName,
            objectName  // 슬래시 그대로 유지 (%2F 제거)
        );
    }
}