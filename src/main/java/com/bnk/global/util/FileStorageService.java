package com.bnk.global.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.Builder;
import lombok.Getter;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}") //수정해야함
    private String uploadDir;

    /**
     * 파일을 실제 물리 디렉토리에 저장하고, TermsFile에 바인딩할 상세 메타데이터 정보를 반환합니다.
     */
    public UploadResult save(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 존재하지 않습니다.");
        }

        // 1. 디렉토리 풀 경로 설정 및 자동 생성
        String targetDirPath = uploadDir + File.separator + subDir;
        File targetDir = new File(targetDirPath);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        // 2. 오리지널 명칭 및 확장자 추출
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")); // 예: .pdf
        }
        
        // 3. 중복 방지용 UUID 파일 이름 생성
        String storedName = UUID.randomUUID().toString() + extension;

        // 4. 서버 저장 처리
        File destinationFile = new File(targetDir, storedName);
        try {
            file.transferTo(destinationFile);
        } catch (IOException e) {
            throw new RuntimeException("물리 파일 저장 실패: " + originalFilename, e);
        }

        // 5. 파일 컨텐츠를 분석하여 MIME 타입 획득
        String mimeType = "application/octet-stream";
        try {
            String detectedMime = Files.probeContentType(destinationFile.toPath());
            if (detectedMime != null) {
                mimeType = detectedMime;
            }
        } catch (IOException e) {
            // MIME 인식 실패 시 기본 스트림 형식 유지
        }

        // DB 기록용 웹 상대 경로 리턴
        String filePath = "/" + subDir + "/" + storedName;

        return UploadResult.builder()
                .originalName(originalFilename)
                .storedName(storedName)
                .filePath(filePath)
                .fileExtension(extension.replace(".", "")) // 온점 제거한 순수 확장자 (예: pdf)
                .fileSize(file.getSize())
                .mimeType(mimeType)
                .build();
    }

    /**
     * 파일 업로드 처리 결과를 모아서 반환하는 내부 DTO 컴포넌트
     */
    @Getter
    @Builder
    public static class UploadResult {
        private final String originalName;
        private final String storedName;
        private final String filePath;
        private final String fileExtension;
        private final Long fileSize;
        private final String mimeType;
    }
}