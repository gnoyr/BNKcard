package com.bnk.global.util;

import java.io.ByteArrayInputStream;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.CreatePreauthenticatedRequestDetails;
import com.oracle.bmc.objectstorage.requests.CreatePreauthenticatedRequestRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.CreatePreauthenticatedRequestResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * OCI Object Storage 업로드 및 파일 URL 서비스.
 *
 * [보안 패치] 2026-06-08
 *
 * 변경 전:
 *   - getPublicUrl() 이 만료 없는 Public URL 반환
 *   - 버킷이 Public 설정이어야 동작 → 인증 없이 URL만 알면 누구나 접근 가능
 *   - PAR 생성 실패 시 Public URL로 조용히 폴백 (보안 실패 → 덜 안전한 경로)
 *
 * 변경 후:
 *   - getPublicUrl() 제거
 *   - 모든 파일 접근은 createDownloadUrl() (PAR, 기본 1시간 만료)을 통해서만 가능
 *   - PAR 생성 실패 시 예외 전파 (조용한 폴백 금지)
 *   - 버킷을 Private으로 운영하도록 가이드
 *
 * 운영 설정 필수:
 *   OCI 콘솔 → Object Storage → 버킷 → 가시성: 비공개(Private) 로 변경
 *   DB의 기존 filePath(Public URL) 레코드는 배포 후 마이그레이션 필요
 */
@Slf4j
@Service
public class ObjectStorageService {

    /** OCI 미설정 환경에서도 서버 기동 가능 */
    @Autowired(required = false)
    private ObjectStorageClient objectStorageClient;

    @Value("${oci.namespace:}")
    private String namespace;

    @Value("${oci.bucket-name:}")
    private String bucketName;

    @Value("${oci.region:ap-chuncheon-1}")
    private String region;

    /**
     * PAR 기본 만료 시간 (밀리초). 1시간.
     * 다운로드 링크를 사용자에게 제공하는 경우 적절히 조정.
     */
    private static final long DEFAULT_PAR_EXPIRY_MS = 60L * 60 * 1000;

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
            return objectName;

        } catch (Exception e) {
            log.error("[ObjectStorage] 업로드 실패: objectName={}", objectName, e);
            throw new RuntimeException("Object Storage 업로드 실패: " + objectName, e);
        }
    }

    // ── URL 생성 ─────────────────────────────────────────────────────

    /**
     * [보안 패치] PAR(Pre-Authenticated Request) URL 생성 — 기본 1시간 만료.
     *
     * 변경 전 getPublicUrl()을 대체.
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
            return parUrl;

        } catch (Exception e) {
            // PAR 실패 시 Public URL로 폴백하지 않고 예외 전파
            // 폴백은 "보안 실패 → 덜 안전한 경로" 로 흐르는 잘못된 설계
            log.error("[ObjectStorage] PAR URL 생성 실패: objectName={}", objectName, e);
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
        // 이미 완전한 URL인 경우 (기존 레코드 하위 호환)
        if (filePathOrObjectName.startsWith("https://") || filePathOrObjectName.startsWith("http://")) {
            return filePathOrObjectName;
        }
        // objectName → PAR URL 생성
        return createDownloadUrl(filePathOrObjectName);
    }
}