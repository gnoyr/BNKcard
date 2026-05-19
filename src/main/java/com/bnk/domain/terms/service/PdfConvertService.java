package com.bnk.domain.terms.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PdfConvertService {

    @Value("${file.upload-dir}") //수정해야함
    private String uploadDir;
	
    /**
     * PDF 파일 → JPG 이미지 변환 후 파일로 기록 보관
     * @return 저장 완료된 상대 경로 모음 컬렉션
     */
    public List<String> convertPdfToImages(MultipartFile pdfFile) throws IOException {

        List<String> savedPaths = new ArrayList<>();
        String baseName = UUID.randomUUID().toString();

        try (PDDocument document = Loader.loadPDF(pdfFile.getBytes())) {
            PDFRenderer renderer = new PDFRenderer(document);

            for (int page = 0; page < document.getNumberOfPages(); page++) {
                // 150 DPI 렌더링 세팅
                BufferedImage image = renderer.renderImageWithDPI(page, 150, ImageType.RGB);

                String fileName = baseName + "_page" + (page + 1) + ".jpg";
                String filePath = uploadDir + "/terms/" + fileName;

                File output = new File(filePath);
                output.getParentFile().mkdirs();
                ImageIO.write(image, "JPEG", output);

                savedPaths.add("/terms/" + fileName); // 웹 연동용 공통 가상 경로 주입
            }
        }
        return savedPaths;
    }
}