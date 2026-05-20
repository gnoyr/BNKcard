package com.bnk.global.util;

import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

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

            String url = buildUrl(objectName);
            log.info("[ObjectStorage] 업로드 완료: objectName={}", objectName);
            return url;

        } catch (Exception e) {
            log.error("[ObjectStorage] 업로드 실패: objectName={}", objectName, e);
            throw new RuntimeException("Object Storage 업로드 실패: " + objectName, e);
        }
    }

    private String buildUrl(String objectName) {
        String encoded = objectName.replace("/", "%2F");
        return String.format(
            "https://objectstorage.%s.oraclecloud.com/n/%s/b/%s/o/%s",
            region, namespace, bucketName, encoded
        );
    }
}