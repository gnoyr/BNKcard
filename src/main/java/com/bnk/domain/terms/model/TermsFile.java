package com.bnk.domain.terms.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TermsFile {
    /**
     * 파일 ID (PK)
     */
    private Long fileId;

    /**
     * 약관 ID (FK: TERMS.terms_id)
     */
    private Long termsId;

    /**
     * 파일 유형 (PDF / IMAGE / SUMMARY)
     */
    private String fileType;

    /**
     * 파일 저장 경로
     */
    private String filePath;

    /**
     * 원본 파일명
     */
    private String originalName;

    /**
     * 서버 저장 파일명
     */
    private String storedName;

    /**
     * 파일 확장자
     */
    private String fileExtension;

    /**
     * 파일 크기 (bytes)
     */
    private Long fileSize;

    /**
     * MIME 타입
     */
    private String mimeType;

    /**
     * 대표 파일 여부 (Y/N)
     */
    @Builder.Default
    private String isPrimary = "N";

    /**
     * 다운로드 횟수
     */
    @Builder.Default
    private Integer downloadCount = 0;

    /**
     * 바이러스 검사 여부 (Y/N)
     */
    @Builder.Default
    private String virusScanYn = "N";

    /**
     * 업로드 일시
     */
    private LocalDateTime uploadedAt;
}
