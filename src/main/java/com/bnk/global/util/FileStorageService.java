package com.bnk.global.util;

import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * 파일 메타데이터 추출 서비스.
 * <p>
 * 변경 전: save() — 로컬 디스크 저장 + 메타데이터 반환.
 * 변경 후: extractMeta() — 메타데이터만 추출. 실제 저장은 ObjectStorageService가 담당.
 * </p>
 * UploadResult 내부 클래스는 그대로 유지 — AdminTermsService에서 계속 사용.
 */
@Service
public class FileStorageService {

    // uploadDir 필드 제거 — 더 이상 로컬 저장 안 함

    /**
     * MultipartFile에서 저장에 필요한 메타데이터를 추출.
     * <p>
     * 기존 save()를 대체. 로컬 저장은 하지 않고 메타만 반환.
     * filePath는 이 시점에서 비어있고, ObjectStorageService.upload() 완료 후
     * AdminTermsService에서 반환된 URL로 채워 TermsFile에 저장.
     * </p>
     *
     * @param file    업로드된 MultipartFile
     * @param subDir  Object Storage 내 하위 경로 (예: "terms")
     * @return 파일 메타데이터 (storedName, originalName, extension, size, mimeType)
     */
    public UploadResult extractMeta(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 존재하지 않습니다.");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // Object Storage objectName: "terms/UUID.pdf"
        String storedName = UUID.randomUUID() + extension;
        String objectName = subDir + "/" + storedName;  // 버킷 내 저장 경로

        // MIME 타입: MultipartFile의 contentType 우선 사용
        String mimeType = (file.getContentType() != null && !file.getContentType().isBlank())
                ? file.getContentType()
                : "application/octet-stream";

        return UploadResult.builder()
                .originalName(originalFilename)
                .storedName(storedName)
                .objectName(objectName)   // Object Storage 경로 추가
                .filePath("")             // 업로드 완료 후 URL로 설정
                .fileExtension(extension.replace(".", ""))
                .fileSize(file.getSize())
                .mimeType(mimeType)
                .build();
    }

    /**
     * 파일 메타데이터 결과 DTO.
     * 기존 UploadResult 구조 유지 + objectName 필드 추가.
     * AdminTermsService에서 계속 이 타입을 사용.
     */
    @Getter
    @Builder
    public static class UploadResult {
        private final String originalName;
        private final String storedName;
        private final String objectName;   // Object Storage 내 경로 (예: "terms/UUID.pdf")
        private final String filePath;     // Object Storage URL (업로드 후 채워짐)
        private final String fileExtension;
        private final Long fileSize;
        private final String mimeType;
    }
}