package com.bnk.domain.terms.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF → JPG 변환 서비스.
 * <p>
 * 변경 전: 로컬 디스크에 JPG 파일로 저장 후 경로 반환.
 * 변경 후: 메모리(byte[]) 기반으로 변환, Object Storage 업로드용 byte[] 리스트 반환.
 *          → uploadDir 의존성 완전 제거.
 * </p>
 * AdminTermsService에서 호출:
 *   List&lt;byte[]&gt; imageBytesList = pdfConvertService.convertPdfToImageBytes(pdfFile);
 */
@Slf4j
@Service
public class PdfConvertService {

    // uploadDir 필드 제거 — Object Storage 방식에서는 로컬 경로 불필요

    /**
     * PDF → JPG 변환 (메모리 기반).
     * <p>
     * 기존 convertPdfToImages() 대체 메서드.
     * 로컬 파일 생성 없이 페이지별 JPG를 byte[]로 반환.
     * 반환된 byte[]를 ObjectStorageService.upload()로 바로 전달.
     * </p>
     *
     * @param pdfFile 업로드된 PDF MultipartFile
     * @return 페이지 순서대로 정렬된 JPG byte[] 목록 (1페이지 = index 0)
     */
    public List<byte[]> convertPdfToImageBytes(MultipartFile pdfFile) throws IOException {
        List<byte[]> result = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(pdfFile.getBytes())) {
            PDFRenderer renderer = new PDFRenderer(document);
            int totalPages = document.getNumberOfPages();

            log.info("[PDF변환] 변환 시작: 파일명={}, 총 페이지={}", 
                    pdfFile.getOriginalFilename(), totalPages);

            for (int page = 0; page < totalPages; page++) {
                // 150 DPI — 기존 설정 그대로 유지
                BufferedImage image = renderer.renderImageWithDPI(page, 150, ImageType.RGB);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "JPEG", baos);
                result.add(baos.toByteArray());

                log.debug("[PDF변환] 페이지 변환 완료: {}/{}", page + 1, totalPages);
            }
        }

        log.info("[PDF변환] 변환 완료: 총 {}페이지", result.size());
        return result;
    }
}