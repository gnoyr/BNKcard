package com.bnk.global.util;

import java.io.ByteArrayInputStream;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.util.audit.AuditLogger;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.CreatePreauthenticatedRequestDetails;
import com.oracle.bmc.objectstorage.requests.CreatePreauthenticatedRequestRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.CreatePreauthenticatedRequestResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OCI Object Storage 업로드 및 파일 URL 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ObjectStorageService {

    // 의존성 필드를 final로 선언하여 불변성 보장
    private final ObjectStorageClient objectStorageClient; // @Autowired(required = false) 대응 필요 시 별도 설정
    private final AuditLogger auditLogger;

    @Value("${oci.namespace:}")
    private String namespace;

    @Value("${oci.bucket-name:}")
    private String bucketName;

    @Value("${oci.region:ap-chuncheon-1}")
    private String region;

    private static final long DEFAULT_PAR_EXPIRY_MS = 15L * 60 * 1000;

    // ── 업로드 ────────────────────────────────────────────────────────

    public String upload(String objectName, byte[] content, String contentType) {
        // 객체 생성자 주입 시 client가 null일 수 있는 경우(Optional 주입) 로직 유지
        if (objectStorageClient == null) {
            log.warn("[ObjectStorage] 클라이언트 미연결 — 업로드 건너뜀: objectName={}", objectName);
            return objectName;
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

            auditLogger.success(AuditLogger.FILE, AuditLogger.UPLOAD, null, objectName, "OCI 업로드 성공: bucket=" + bucketName);

            return objectName;

        } catch (Exception e) {
            log.error("[ObjectStorage] 업로드 실패: objectName={}", objectName, e);
            auditLogger.failure(AuditLogger.FILE, AuditLogger.UPLOAD, null, objectName, "OCI 업로드 실패: bucket=" + bucketName + " | " + e.getMessage());
            throw new RuntimeException("Object Storage 업로드 실패: " + objectName, e);
        }
    }

    // ── URL 생성 ─────────────────────────────────────────────────────

    public String createDownloadUrl(String objectName) {
        return createDownloadUrl(objectName, DEFAULT_PAR_EXPIRY_MS);
    }

    public String createDownloadUrl(String objectName, long expiryMs) {
        if (objectStorageClient == null) {
            log.warn("[ObjectStorage] 클라이언트 미연결 — 개발 환경 더미 URL 반환: objectName={}", objectName);
            return "https://localhost/dev-only/" + objectName;
        }

        try {
            Date expiry = new Date(System.currentTimeMillis() + expiryMs);

            CreatePreauthenticatedRequestDetails details =
                    CreatePreauthenticatedRequestDetails.builder()
                            .name("par-" + System.currentTimeMillis() + "-" + objectName.replace("/", "-"))
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
            String parUrl  = "https://objectstorage." + region + ".oraclecloud.com" + parPath;

            log.info("[ObjectStorage] PAR URL 생성 완료: objectName={} expiresAt={}", objectName, expiry);
            auditLogger.success(AuditLogger.FILE, AuditLogger.DOWNLOAD, null, objectName, "PAR URL 생성 완료: expiresAt=" + expiry);

            return parUrl;

        } catch (Exception e) {
            log.error("[ObjectStorage] PAR URL 생성 실패: objectName={}", objectName, e);
            auditLogger.failure(AuditLogger.FILE, AuditLogger.DOWNLOAD, null, objectName, "PAR URL 생성 실패: bucket=" + bucketName + " | " + e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "파일 다운로드 URL 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    public String resolveUrl(String filePathOrObjectName) {
        if (filePathOrObjectName == null || filePathOrObjectName.isBlank()) return "";
        if (filePathOrObjectName.startsWith("https://") || filePathOrObjectName.startsWith("http://")) {
            return filePathOrObjectName;
        }
        return createDownloadUrl(filePathOrObjectName);
    }
}