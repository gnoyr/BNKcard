package com.bnk.global.util;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

/**
 * 파일 메타데이터 추출 서비스.
 * <p>
 * 변경 전: save() — 로컬 디스크 저장 + 메타데이터 반환.
 * 변경 후: extractMeta() — 메타데이터만 추출. 실제 저장은 ObjectStorageService가 담당.
 * </p>
 * UploadResult 내부 클래스는 그대로 유지 — AdminTermsService에서 계속 사용.
 */
@Slf4j
@Service
public class FileStorageService {
	
	// ── 허용 설정 ─────────────────────────────────────────────────────
	 
    /** 허용 확장자 (소문자). 현재 약관 PDF만 업로드 가능 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf");
 
    /** 허용 MIME 타입 */
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        "application/pdf"
    );
 
    /** 최대 파일 크기: 20MB (application.properties spring.servlet.multipart.max-file-size와 일치) */
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
 
    /** PDF 매직 바이트: %PDF- (25 50 44 46 2D) */
    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46, 0x2D};

	// ── 공개 메서드 ──────────────────────────────────────────────────
	 
    /**
     * MultipartFile에서 저장에 필요한 메타데이터를 추출.
     *
     * [보안 패치] 업로드 전 아래 순서로 검증:
     *   ① 파일 존재 및 크기 확인
     *   ② 확장자 화이트리스트 검증
     *   ③ Content-Type 헤더 검증 (위조 가능하나 1차 필터)
     *   ④ 매직 바이트 시그니처 검증 (실제 파일 타입 확인)
     *
     * @param file   업로드된 MultipartFile
     * @param subDir Object Storage 내 하위 경로 (예: "terms")
     * @return 파일 메타데이터
     * @throws BusinessException INVALID_INPUT — 검증 실패 시
     */
    public UploadResult extractMeta(MultipartFile file, String subDir) {
        // ① 파일 존재·크기 확인
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "업로드할 파일이 존재하지 않습니다.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                "파일 크기는 20MB를 초과할 수 없습니다. (현재: " + (file.getSize() / 1024 / 1024) + "MB)");
        }
 
        // ② 원본 파일명 정규화 (Path Traversal 방어)
        String originalFilename = sanitizeFilename(file.getOriginalFilename());
 
        // ③ 확장자 추출 및 화이트리스트 검증
        String extension = extractExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            log.warn("[FileStorage] 허용되지 않는 확장자 업로드 시도: filename={} extension={}",
                    originalFilename, extension);
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                "허용된 파일 형식이 아닙니다. 허용 확장자: " + ALLOWED_EXTENSIONS);
        }
 
        // ④ Content-Type 헤더 검증 (1차 필터 — 위조 가능하므로 단독 신뢰 금지)
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            log.warn("[FileStorage] 허용되지 않는 Content-Type: filename={} contentType={}",
                    originalFilename, contentType);
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                "허용된 파일 형식이 아닙니다. (Content-Type: " + contentType + ")");
        }
 
        // ⑤ 매직 바이트 시그니처 검증 (실제 파일 타입 확인 — 핵심 보안 검사)
        verifyMagicBytes(file, originalFilename);
 
        // ⑥ Object Storage 경로 생성 (UUID 기반 — 원본 파일명 미사용)
        String storedName  = UUID.randomUUID() + "." + extension;
        String objectName  = subDir + "/" + storedName;
 
        log.info("[FileStorage] 파일 검증 완료: originalName={} storedName={} size={}B",
                originalFilename, storedName, file.getSize());
 
        return UploadResult.builder()
                .originalName(originalFilename)
                .storedName(storedName)
                .objectName(objectName)
                .filePath("")                           // 업로드 완료 후 URL로 설정
                .fileExtension(extension)
                .fileSize(file.getSize())
                .mimeType(contentType)
                .build();
    }
    
	// ── private 헬퍼 ─────────────────────────────────────────────────

	/**
	 * 파일명 정규화. - null → "unnamed" - 경로 구분자(/ \) 제거 → Path Traversal 방어 - 선행/후행 공백
	 * 제거
	 */
	private String sanitizeFilename(String filename) {
		if (filename == null || filename.isBlank())
			return "unnamed";
		// 경로 구분자 및 null byte 제거
		return filename.trim().replaceAll("[/\\\\]", "").replaceAll("\0", "");
	}

	/**
	 * 확장자 추출. 점(.) 이 없으면 빈 문자열 반환.
	 */
	private String extractExtension(String filename) {
		int dotIdx = filename.lastIndexOf('.');
		if (dotIdx < 0 || dotIdx == filename.length() - 1)
			return "";
		return filename.substring(dotIdx + 1).toLowerCase();
	}

	/**
	 * 매직 바이트(파일 시그니처) 검증. 파일의 첫 N 바이트를 읽어 실제 파일 포맷을 확인. Content-Type 헤더는 클라이언트가 위조
	 * 가능하므로 이 검증이 핵심.
	 */
	private void verifyMagicBytes(MultipartFile file, String originalFilename) {
		try (InputStream is = file.getInputStream()) {
			byte[] header = new byte[PDF_MAGIC.length];
			int read = is.read(header);

			if (read < PDF_MAGIC.length || !startsWith(header, PDF_MAGIC)) {
				log.warn("[FileStorage] 매직 바이트 불일치 — PDF가 아닌 파일: filename={}", originalFilename);
				throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "파일 내용이 PDF 형식이 아닙니다. 실제 PDF 파일만 업로드 가능합니다.");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("[FileStorage] 파일 시그니처 읽기 실패: filename={}", originalFilename, e);
            throw new BusinessException(ErrorCode.INVALID_INPUT, "파일을 읽을 수 없습니다.");
        }
    }
 
    /**
     * 바이트 배열이 prefix로 시작하는지 확인.
     */
    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    /**
     * 파일 메타데이터 결과 DTO.
     * 기존 UploadResult 구조 유지 + objectName 필드 추가.
     * AdminTermsService에서 계속 이 타입을 사용.
     */
    /**
     * 파일 메타데이터 결과 DTO.
     */
    @Getter
    @Builder
    public static class UploadResult {
        private final String originalName;
        private final String storedName;
        private final String objectName;   // Object Storage 내 경로
        private final String filePath;     // Object Storage URL (업로드 후 채워짐)
        private final String fileExtension;
        private final Long   fileSize;
        private final String mimeType;
    }
}