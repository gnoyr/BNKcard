package com.bnk.domain.admin.service;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.bnk.domain.terms.dto.request.TermsCreateRequest;
import com.bnk.domain.terms.mapper.TermsMapper;
import com.bnk.domain.terms.model.Terms;
import com.bnk.domain.terms.model.TermsFile;
import com.bnk.domain.terms.service.PdfConvertService;
import com.bnk.global.util.FileStorageService;
import com.bnk.global.util.FileStorageService.UploadResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminTermsService {
	
	private final TermsMapper termsMapper;
	private final PdfConvertService pdfConvertService; // 🔥 final 키워드 추가로 정상 주입 보장
	private final FileStorageService fileStorageService; // 🔥 주입 누락되었던 필드 추가 및 연동
	
	@Transactional
	public void registerTermsWithPdf(TermsCreateRequest request, MultipartFile pdfFile) throws IOException {

	    // 1. TERMS 테이블에 약관 기본 정보 저장
	    Terms terms = Terms.builder()
	            .termsMasterId(request.getTermsMasterId())
	            .version(request.getVersion())
	            .contentHtml(request.getContentHtml())
	            .requiredYn(request.getRequiredYn())
	            .effectiveFrom(request.getEffectiveFrom())
	            .build();
	    termsMapper.insertTerms(terms);  // MyBatis 실행 후 래퍼 객체 내부 keyProperty에 의해 terms_id가 자동 생성 할당됨

	    // 2. PDF 원본 저장 및 TermsFile 풍부한 도메인 스펙 바인딩
	    UploadResult pdfResult = fileStorageService.save(pdfFile, "terms");
	    
	    TermsFile pdfTermsFile = TermsFile.builder()
	            .termsId(terms.getTermsId())
	            .fileType("PDF")
	            .filePath(pdfResult.getFilePath())
	            .originalName(pdfResult.getOriginalName())
	            .storedName(pdfResult.getStoredName())
	            .fileExtension(pdfResult.getFileExtension())
	            .fileSize(pdfResult.getFileSize())
	            .mimeType(pdfResult.getMimeType())
	            .isPrimary("Y") // 원본 도큐먼트를 대표 플래그 처리
	            .build();
	    termsMapper.insertTermsFile(pdfTermsFile);

	    // 3. PDF → JPG 변환 엔진 가동 후 페이지별 이미지 객체 생성 및 순차 저장
	    List<String> imagePaths = pdfConvertService.convertPdfToImages(pdfFile);
	    for (String imagePath : imagePaths) {
	        // 파일 경로로부터 파일 단독 명칭만 파싱 처리
	        String storedImageName = imagePath.substring(imagePath.lastIndexOf("/") + 1);
	        String originalImageName = pdfResult.getOriginalName().replace(".pdf", ".jpg");

	        TermsFile imageTermsFile = TermsFile.builder()
	                .termsId(terms.getTermsId())
	                .fileType("IMAGE")
	                .filePath(imagePath)
	                .originalName(originalImageName)
	                .storedName(storedImageName)
	                .fileExtension("jpg")
	                .mimeType("image/jpeg")
	                .isPrimary("N")
	                .build();
	        termsMapper.insertTermsFile(imageTermsFile);
	    }
	}
}
