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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTermsService {

    private final TermsMapper termsMapper;
    private final PdfConvertService pdfConvertService;
    private final FileStorageService fileStorageService;

    /**
     * B-11 약관 신규 버전 등록 + PDF 변환 (RQ-B11, B-13)
     * TERMS INSERT(DRAFT) + TERMS_FILES INSERT(PDF 원본 + JPG 변환본)
     */
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
        termsMapper.insertTerms(terms); // keyProperty=termsId 자동 주입

        // 2. PDF 원본 저장 및 TERMS_FILES INSERT
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
                .isPrimary("Y")
                .build();
        termsMapper.insertTermsFile(pdfTermsFile);

        // 3. PDF → JPG 변환 후 페이지별 TERMS_FILES INSERT
        List<String> imagePaths = pdfConvertService.convertPdfToImages(pdfFile);
        for (String imagePath : imagePaths) {
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

        log.info("[약관등록] termsId={}, version={}", terms.getTermsId(), request.getVersion());
    }

    /**
     * B-12 약관 상태 변경 (RQ-B12)
     * TERMS.status UPDATE + TERMS_STATUS_HISTORY INSERT
     * PUBLISHED 전환 시 reconsent_required_yn='Y' → 알림 발송 (TERMS_NOTIFICATION_HISTORY INSERT)
     */
    @Transactional
    public void changeTermsStatus(Long termsId, TermsStatusRequest request, Long adminId) {

        // 현재 약관 조회
        Terms terms = termsMapper.findById(termsId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TERMS_NOT_FOUND));

        String previousStatus = terms.getStatus();
        String newStatus = request.getNewStatus();

        // 이미 같은 상태면 무시 (멱등성)
        if (previousStatus.equals(newStatus)) {
            return;
        }

        // TERMS.status UPDATE
        termsMapper.updateTermsStatus(termsId, newStatus, adminId);

        // TERMS_STATUS_HISTORY INSERT
        termsMapper.insertStatusHistory(
                termsId,
                previousStatus,
                newStatus,
                adminId,
                request.getChangedReason()
        );

        // PUBLISHED 전환 시 재동의 알림 처리
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