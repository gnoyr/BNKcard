package com.bnk.global.util;

import java.io.ByteArrayInputStream;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
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

import lombok.extern.slf4j.Slf4j;

/**
 * OCI Object Storage 업로드 및 파일 URL 서비스.
 *
 *
 *          → OCI 연동 장애를 감사 로그 테이블에서 추적 가능
 *          → adminId/userId 는 호출 시점에서 알 수 없으므로 null 처리
 *            (AuditLogger 내부에서 actor=- 로 기록됨)
 *
 * 운영 설정 필수:
 *   OCI 콘솔 → Object Storage → 버킷 → 가시성: 비공개(Private) 로 변경
 */
@Slf4j
@Service
public class ObjectStorageService {

    /** OCI 미설정 환경에서도 서버 기동 가능 */
    @Autowired(required = false)
    private ObjectStorageClient objectStorageClient;

    @Autowired
    private AuditLogger auditLogger;

    @Value("${oci.namespace:}")
    private String namespace;

    @Value("${oci.bucket-name:}")
    private String bucketName;

    @Value("${oci.region:ap-chuncheon-1}")
    private String region;

    /**
     * PAR 기본 만료 시간: 15분.
     * URL 유출 시 피해 창을 최소화 (60분 → 15분, 75% 단축).
     */
    private static final long DEFAULT_PAR_EXPIRY_MS = 15L * 60 * 1000;

    // ── 업로드 ────────────────────────────────────────────────────────

    /**
     * Object Storage에 파일 업로드.
     * OCI 미연결 시 로컬 개발 환경 폴백 — objectName을 그대로 반환.
     *
     * @return objectName (버킷 내 경로). DB에는 이 값을 저장하고 URL은 요청 시 생성.
     */
    public String upload(String objectName, byte[] content, String contentType) {
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

            // [추가] 업로드 성공 감사 로그
            auditLogger.success(
                AuditLogger.FILE,
                AuditLogger.UPLOAD,
                null,
                objectName,
                "OCI 업로드 성공: bucket=" + bucketName
            );

            return objectName;

        } catch (Exception e) {
            log.error("[ObjectStorage] 업로드 실패: objectName={}", objectName, e);

            // [변경] log.error → auditLogger.failure 추가 (기존 log.error 는 위에 유지)
            auditLogger.failure(
                AuditLogger.FILE,
                AuditLogger.UPLOAD,
                null,
                objectName,
                "OCI 업로드 실패: bucket=" + bucketName + " | " + e.getMessage()
            );

            throw new RuntimeException("Object Storage 업로드 실패: " + objectName, e);
        }
    }

    // ── URL 생성 ─────────────────────────────────────────────────────

    /**
     * PAR(Pre-Authenticated Request) URL 생성 — 기본 15분 만료.
     *
     * 버킷은 반드시 Private으로 설정해야 하며, 모든 파일 접근은
     * 이 메서드로 생성한 시간 제한 URL을 통해서만 가능.
     *
     * OCI 미연결(개발 환경) 시 로컬 더미 URL 반환.
     *
     * @param objectName 버킷 내 파일 경로 (예: "terms/UUID.pdf")
     * @return 시간 제한 다운로드 URL
     * @throws BusinessException OCI 연결 상태에서 PAR 생성 실패 시 (폴백 없음)
     */
    public String createDownloadUrl(String objectName) {
        return createDownloadUrl(objectName, DEFAULT_PAR_EXPIRY_MS);
    }

    /**
     * PAR URL 생성 — 만료 시간 지정 버전.
     *
     * @param objectName  버킷 내 파일 경로
     * @param expiryMs    만료 시간 (밀리초)
     * @return 시간 제한 다운로드 URL
     */
    public String createDownloadUrl(String objectName, long expiryMs) {
        // OCI 미연결 (개발 환경)
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

            // [추가] PAR 생성 성공 감사 로그
            auditLogger.success(
                AuditLogger.FILE,
                AuditLogger.DOWNLOAD,
                null,
                objectName,
                "PAR URL 생성 완료: expiresAt=" + expiry
            );

            return parUrl;

        } catch (Exception e) {
            log.error("[ObjectStorage] PAR URL 생성 실패: objectName={}", objectName, e);

            // [변경] log.error → auditLogger.failure 추가 (기존 log.error 는 위에 유지)
            auditLogger.failure(
                AuditLogger.FILE,
                AuditLogger.DOWNLOAD,
                null,
                objectName,
                "PAR URL 생성 실패: bucket=" + bucketName + " | " + e.getMessage()
            );

            // PAR 실패 시 Public URL로 폴백하지 않고 예외 전파
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                "파일 다운로드 URL 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    /**
     * objectName이 저장된 DB 레코드에서 다운로드 URL을 생성하는 헬퍼.
     * AdminTermsService 등에서 기존 filePath(objectName) 기반으로 URL이 필요할 때 사용.
     *
     * filePath가 이미 완전한 URL(https://로 시작)인 경우 그대로 반환
     * (기존 Public URL 레코드 마이그레이션 완료 전 하위 호환).
     */
    public String resolveUrl(String filePathOrObjectName) {
        if (filePathOrObjectName == null || filePathOrObjectName.isBlank()) return "";
        if (filePathOrObjectName.startsWith("https://") || filePathOrObjectName.startsWith("http://")) {
            return filePathOrObjectName;
        }
        return createDownloadUrl(filePathOrObjectName);
    }
}