package com.bnk.domain.admin.service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.bnk.domain.terms.dto.request.TermsCreateRequest;
import com.bnk.domain.terms.dto.request.TermsStatusRequest;
import com.bnk.domain.terms.dto.response.TermsAdminResponse;
import com.bnk.domain.terms.dto.response.TermsMasterResponse;
import com.bnk.domain.terms.mapper.TermsMapper;
import com.bnk.domain.terms.model.Terms;
import com.bnk.domain.terms.model.TermsFile;
import com.bnk.domain.terms.model.TermsMaster;
import com.bnk.domain.terms.service.PdfConvertService;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.util.FileStorageService;
import com.bnk.global.util.FileStorageService.UploadResult;
import com.bnk.global.util.ObjectStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTermsService {

    private final TermsMapper          termsMapper;
    private final PdfConvertService    pdfConvertService;
    private final FileStorageService   fileStorageService;
    private final ObjectStorageService objectStorageService;

    /**
     * B-11 약관 신규 버전 등록 + PDF 변환 (RQ-B11, B-13).
     * Object Storage에 PDF/JPG 업로드 → Public URL DB 저장 (만료 없음).
     */
    @Transactional
    public void registerTermsWithPdf(TermsCreateRequest request, MultipartFile pdfFile) throws IOException {

        // ── 1. TERMS INSERT (status = DRAFT) ──────────────────────────────
        Terms terms = Terms.builder()
                .termsMasterId(request.getTermsMasterId())
                .version(request.getVersion())
                .contentHtml(request.getContentHtml())
                .requiredYn(request.getRequiredYn())
                .effectiveFrom(request.getEffectiveFrom())
                .build();
        termsMapper.insertTerms(terms);

        // ── 2. PDF 메타데이터 추출 (로컬 저장 없음) ───────────────────────
        UploadResult pdfMeta = fileStorageService.extractMeta(pdfFile, "terms");

        // ── 3. pdfFile.getBytes()를 한 번만 호출해서 재사용 ───────────────
        // 기존: upload()에서 1번, convertPdfToImageBytes() 내부에서 InputStream으로 1번
        // 수정: 여기서 1번만 읽고 upload에 넘긴 뒤, convertPdfToImageBytes는 InputStream 사용
        byte[] pdfBytes = pdfFile.getBytes();

        // ── 4. Object Storage에 PDF 원본 업로드 ───────────────────────────
        objectStorageService.upload(
                pdfMeta.getObjectName(),   // "terms/UUID.pdf"
                pdfBytes,
                pdfMeta.getMimeType()      // "application/pdf"
        );

        // ── 5. Public URL 생성 (만료 없음) ────────────────────────────────
        // 변경 전: createDownloadUrl() → 24시간 만료 PAR URL
        // 변경 후: getPublicUrl()     → 영구 Public URL (버킷 Public 설정 필요)
        String pdfUrl = objectStorageService.getPublicUrl(pdfMeta.getObjectName());

        // ── 6. TERMS_FILES INSERT (PDF) ───────────────────────────────────
        termsMapper.insertTermsFile(TermsFile.builder()
                .termsId(terms.getTermsId())
                .fileType("PDF")
                .filePath(pdfUrl)                       // ← 영구 Public URL
                .originalName(pdfMeta.getOriginalName())
                .storedName(pdfMeta.getStoredName())
                .fileExtension(pdfMeta.getFileExtension())
                .fileSize(pdfMeta.getFileSize())
                .mimeType(pdfMeta.getMimeType())
                .isPrimary("Y")
                .build());

        // ── 7. PDF → JPG 변환 (메모리 기반, InputStream 사용) ─────────────
        // PdfConvertService 내부에서 pdfFile.getInputStream()으로 읽으므로
        // 위의 pdfFile.getBytes()와 중복 로드 없이 동작
        List<byte[]> imageBytesList = pdfConvertService.convertPdfToImageBytes(pdfFile);

        // ── 8. 페이지별 JPG 업로드 + TERMS_FILES INSERT ───────────────────
        String baseName = pdfMeta.getStoredName()
                .substring(0, pdfMeta.getStoredName().lastIndexOf("."));
        // baseName = "UUID" (확장자 제거)

        for (int i = 0; i < imageBytesList.size(); i++) {
            String imageStoredName = baseName + "_page" + (i + 1) + ".jpg";
            String imageObjectName = "terms/" + imageStoredName;

            // JPG Object Storage 업로드
            objectStorageService.upload(
                    imageObjectName,
                    imageBytesList.get(i),
                    "image/jpeg"
            );

            // 변경 전: createDownloadUrl() → 24시간 만료 PAR URL
            // 변경 후: getPublicUrl()     → 영구 Public URL
            String imageUrl = objectStorageService.getPublicUrl(imageObjectName);

            // TERMS_FILES INSERT (IMAGE)
            termsMapper.insertTermsFile(TermsFile.builder()
                    .termsId(terms.getTermsId())
                    .fileType("IMAGE")
                    .filePath(imageUrl)                 // ← 영구 Public URL
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
     * B-12 약관 상태 변경
     * DRAFT → REVIEW → APPROVED → PUBLISHED
     * PUBLISHED 전환 시 reconsent_required_yn='Y' 이면 재동의 알림 발송
     */
    @Transactional
    public void changeTermsStatus(Long termsId, TermsStatusRequest request, Long adminId) {

        Terms terms = termsMapper.findById(termsId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TERMS_NOT_FOUND));

        String previousStatus = terms.getStatus();
        String newStatus      = request.getNewStatus();

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
    
    /** TERMS_MASTERS 목록 조회 */
    @Transactional(readOnly = true)
    public List<TermsMasterResponse> getTermsMasters() {
        List<TermsMaster> masters = termsMapper.findAllMasters();
        return masters.stream()
                .map(m -> TermsMasterResponse.builder()
                        .termsMasterId(m.getTermsMasterId())
                        .termsType(m.getTermsType())
                        .title(m.getTitle())
                        .description(m.getDescription())
                        .build())
                .collect(Collectors.toList());
    }

    /** 관리자 약관 목록 조회 (status 필터 가능) */
    @Transactional(readOnly = true)
    public List<TermsAdminResponse> getTermsList(String status) {
        List<Terms> termsList = termsMapper.findAllForAdmin(status);
        return termsList.stream()
                .map(t -> TermsAdminResponse.builder()
                        .termsId(t.getTermsId())
                        .termsMasterId(t.getTermsMasterId())
                        .title(t.getTitle())
                        .termsType(t.getTermsType())
                        .version(t.getVersion())
                        .status(t.getStatus())
                        .requiredYn(t.getRequiredYn())
                        .reconsentRequiredYn(t.getReconsentRequiredYn())
                        .effectiveFrom(t.getEffectiveFrom())
                        .createdAt(t.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /** 관리자 약관 상세 조회 */
    @Transactional(readOnly = true)
    public TermsAdminResponse getTermsDetail(Long termsId) {
        Terms terms = termsMapper.findById(termsId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TERMS_NOT_FOUND));

        List<com.bnk.domain.terms.model.TermsFile> files =
                termsMapper.findFilesByTermsId(termsId);

        List<com.bnk.domain.terms.dto.response.TermsFileResponse> fileResponses = files.stream()
                .map(f -> com.bnk.domain.terms.dto.response.TermsFileResponse.builder()
                        .fileId(f.getFileId())
                        .termsId(f.getTermsId())
                        .fileType(f.getFileType())
                        .filePath(f.getFilePath())
                        .originalName(f.getOriginalName())
                        .isPrimary(f.getIsPrimary())
                        .build())
                .collect(Collectors.toList());

        return TermsAdminResponse.builder()
                .termsId(terms.getTermsId())
                .termsMasterId(terms.getTermsMasterId())
                .title(terms.getTitle())
                .termsType(terms.getTermsType())
                .version(terms.getVersion())
                .status(terms.getStatus())
                .requiredYn(terms.getRequiredYn())
                .effectiveFrom(terms.getEffectiveFrom())
                .createdAt(terms.getCreatedAt())
                .files(fileResponses)
                .build();
    }
    
    @Transactional
    public void addFileToExistingTerms(Long termsId, MultipartFile pdfFile) throws IOException {

        // terms 존재 확인
        termsMapper.findById(termsId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TERMS_NOT_FOUND));

        UploadResult pdfMeta = fileStorageService.extractMeta(pdfFile, "terms");
        byte[] pdfBytes = pdfFile.getBytes();

        objectStorageService.upload(pdfMeta.getObjectName(), pdfBytes, pdfMeta.getMimeType());
        String pdfUrl = objectStorageService.getPublicUrl(pdfMeta.getObjectName());

        // PDF 파일 INSERT
        termsMapper.insertTermsFile(TermsFile.builder()
                .termsId(termsId)
                .fileType("PDF")
                .filePath(pdfUrl)
                .originalName(pdfMeta.getOriginalName())
                .storedName(pdfMeta.getStoredName())
                .fileExtension(pdfMeta.getFileExtension())
                .fileSize(pdfMeta.getFileSize())
                .mimeType(pdfMeta.getMimeType())
                .isPrimary("Y")
                .build());

        // JPG 변환 + 업로드
        List<byte[]> imageBytesList = pdfConvertService.convertPdfToImageBytes(pdfFile);
        String baseName = pdfMeta.getStoredName()
                .substring(0, pdfMeta.getStoredName().lastIndexOf("."));

        for (int i = 0; i < imageBytesList.size(); i++) {
            String imageStoredName = baseName + "_page" + (i + 1) + ".jpg";
            String imageObjectName = "terms/" + imageStoredName;
            objectStorageService.upload(imageObjectName, imageBytesList.get(i), "image/jpeg");
            String imageUrl = objectStorageService.getPublicUrl(imageObjectName);

            termsMapper.insertTermsFile(TermsFile.builder()
                    .termsId(termsId)
                    .fileType("IMAGE")
                    .filePath(imageUrl)
                    .originalName(pdfMeta.getOriginalName().replace(".pdf", ".jpg"))
                    .storedName(imageStoredName)
                    .fileExtension("jpg")
                    .mimeType("image/jpeg")
                    .isPrimary("N")
                    .build());
        }

        log.info("[파일추가] 완료: termsId={}, pages={}", termsId, imageBytesList.size());
    }

    
}