package com.bnk.domain.terms.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TermsFileResponse {

    private Long   fileId;
    private Long   termsId;
    private String fileType;       // PDF / IMAGE
    private String filePath;       // OCI Public URL
    private String originalName;   // 원본 파일명
    private String fileExtension;  // pdf / jpg
    private Long   fileSize;
    private String mimeType;
    private String isPrimary;      // Y / N
}