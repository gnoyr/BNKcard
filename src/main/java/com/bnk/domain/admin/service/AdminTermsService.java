package com.bnk.domain.admin.service;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.bnk.domain.terms.dto.request.TermsCreateRequest;
import com.bnk.domain.terms.dto.request.TermsStatusRequest;
import com.bnk.domain.terms.mapper.TermsMapper;
import com.bnk.domain.terms.model.Terms;
import com.bnk.domain.terms.model.TermsFile;
import com.bnk.domain.terms.service.PdfConvertService;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.util.FileStorageService;
import com.bnk.global.util.FileStorageService.UploadResult;
import com.bnk.global.util.ObjectStorageService;  // ← 추가

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTermsService {

    private final TermsMapper termsMapper;
    private final PdfConvertService pdfConvertService;
    private final FileStorageService fileStorageService;
    private final ObjectStorageService objectStorageService;  // ← 추가

    /**
     * B-11 약관 신규 버전 등록 + PDF 변환 (RQ-B11, B-13).
     * <p>
     * 변경 전: 로컬 디스크에 PDF/JPG 저장 → 로컬 경로 DB 저장.
     * 변경 후: Object Storage에 PDF/JPG 업로드 → Object Storage URL DB 저장.
     * </p>
     * Controller, Mapper, TermsFile 모델은 변경 없음.
     */
    @Transactional
    public void registerTermsWithPdf(TermsCreateRequest request, MultipartFile pdfFile) throws IOException {

        // ── 1. TERMS 기본 정보 INSERT (변경 없음) ─────────────────────────
        Terms terms = Terms.builder()
                .termsMasterId(request.getTermsMasterId())
                .version(request.getVersion())
                .contentHtml(request.getContentHtml())
                .requiredYn(request.getRequiredYn())
                .effectiveFrom(request.getEffectiveFrom())
                .build();
        termsMapper.insertTerms(terms);

        // ── 2. PDF 메타데이터 추출 (로컬 저장 X) ──────────────────────────
        // 기존: fileStorageService.save(pdfFile, "terms")  ← 로컬 저장
        // 변경: fileStorageService.extractMeta(pdfFile, "terms")  ← 메타만 추출
        UploadResult pdfMeta = fileStorageService.extractMeta(pdfFile, "terms");

     // ── 3. Object Storage에 PDF 원본 업로드 ───────────────────────────
        objectStorageService.upload(
        		pdfMeta.getObjectName(),
        		pdfFile.getBytes(), 
        		pdfMeta.getMimeType());
        // PAR URL을 filePath에 저장 (외부 접근 가능)
        String pdfUrl = objectStorageService.createDownloadUrl(pdfMeta.getObjectName());

        // ── 4. TERMS_FILES INSERT (PDF) — filePath에 Object Storage URL 저장 ─
        // 기존과 동일한 TermsFile 빌더 사용, filePath만 로컬경로 → OCI URL로 변경
        termsMapper.insertTermsFile(TermsFile.builder()
                .termsId(terms.getTermsId())
                .fileType("PDF")
                .filePath(pdfUrl)                      // ← Object Storage URL
                .originalName(pdfMeta.getOriginalName())
                .storedName(pdfMeta.getStoredName())
                .fileExtension(pdfMeta.getFileExtension())
                .fileSize(pdfMeta.getFileSize())
                .mimeType(pdfMeta.getMimeType())
                .isPrimary("Y")
                .build());

        // ── 5. PDF → JPG 변환 (메모리 기반) ──────────────────────────────
        // 기존: pdfConvertService.convertPdfToImages()  ← 로컬 파일로 저장
        // 변경: pdfConvertService.convertPdfToImageBytes()  ← byte[] 반환
        List<byte[]> imageBytesList = pdfConvertService.convertPdfToImageBytes(pdfFile);

        // ── 6. 페이지별 JPG Object Storage 업로드 + TERMS_FILES INSERT ────
        // baseName = PDF UUID 부분만 추출 ("UUID") → JPG는 "UUID_page1.jpg" 형식
        String baseName = pdfMeta.getStoredName()
                .substring(0, pdfMeta.getStoredName().lastIndexOf("."));

        for (int i = 0; i < imageBytesList.size(); i++) {
            String imageStoredName = baseName + "_page" + (i + 1) + ".jpg";
            String imageObjectName = "terms/" + imageStoredName;  // "terms/UUID_page1.jpg"

            objectStorageService.upload(
            		imageObjectName, 
            		imageBytesList.get(i), 
            		"image/jpeg");
            
            String imageUrl = objectStorageService.createDownloadUrl(imageObjectName);

            // 기존과 동일한 TermsFile 빌더 사용
            termsMapper.insertTermsFile(TermsFile.builder()
                    .termsId(terms.getTermsId())
                    .fileType("IMAGE")
                    .filePath(imageUrl)                // ← Object Storage URL
                    .originalName(pdfMeta.getOriginalName().replace(".pdf", ".jpg"))
                    .storedName(imageStoredName)
                    .fileExtension("jpg")
                    .mimeType("image/jpeg")
                    .isPrimary("N")
                    .build());
        }

        log.info("[약관등록] 완료: termsId={}, version={}, pdfUrl={}, pages={}",
                terms.getTermsId(), request.getVersion(), pdfUrl, imageBytesList.size());
    }

    /**
     * B-12 약관 상태 변경 — 변경 없음
     */
    @Transactional
    public void changeTermsStatus(Long termsId, TermsStatusRequest request, Long adminId) {

        Terms terms = termsMapper.findById(termsId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TERMS_NOT_FOUND));

        String previousStatus = terms.getStatus();
        String newStatus = request.getNewStatus();

        if (previousStatus.equals(newStatus)) {
            return;
        }

        termsMapper.updateTermsStatus(termsId, newStatus, adminId);
        termsMapper.insertStatusHistory(termsId, previousStatus, newStatus,
                adminId, request.getChangedReason());

        if ("PUBLISHED".equals(newStatus) && "Y".equals(terms.getReconsentRequiredYn())) {
            List<Long> userIds = termsMapper.findUserIdsForReconsent(termsId);
            for (Long userId : userIds) {
                termsMapper.insertNotificationHistory(termsId, userId, "EMAIL");
            }
            log.info("[약관상태변경] PUBLISHED → 재동의 알림 발송 대상: {}명, termsId={}",
                    userIds.size(), termsId);
        }

        log.info("[약관상태변경] termsId={}, {} → {}, adminId={}",
                termsId, previousStatus, newStatus, adminId);
    }
}