package com.bnk.domain.admin.service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.bnk.domain.admin.mapper.ApprovalMapper;
import com.bnk.domain.admin.model.ApprovalLine;
import com.bnk.domain.admin.model.ApprovalRequest;
import com.bnk.domain.terms.dto.request.TermsCreateRequest;
import com.bnk.domain.terms.dto.request.TermsMasterCreateRequest;
import com.bnk.domain.terms.dto.request.TermsStatusRequest;
import com.bnk.domain.terms.dto.response.TermsAdminResponse;
import com.bnk.domain.terms.dto.response.TermsFileResponse;
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
    private final ApprovalMapper       approvalMapper;

    // ════════════════════════════════════════════════════════════
    // 약관 신규 등록 + 결재 신청
    // ════════════════════════════════════════════════════════════
    @Transactional
    public java.util.Map<String, Long> registerTermsWithApproval(
            TermsCreateRequest request,
            MultipartFile pdfFile,
            Long adminId) throws IOException {

        Terms terms = Terms.builder()
                .termsMasterId(request.getTermsMasterId())
                .version(request.getVersion())
                .contentHtml(request.getContentHtml())
                .requiredYn(request.getRequiredYn())
                .reconsentRequiredYn(request.getReconsentRequiredYn())
                .effectiveFrom(request.getEffectiveFrom())
                .build();
        termsMapper.insertTerms(terms);

        log.info("[약관등록] TERMS INSERT 완료: termsId={}, status=DRAFT", terms.getTermsId());

        uploadTermsFiles(terms.getTermsId(), pdfFile);

        termsMapper.insertStatusHistory(
                terms.getTermsId(), null, "DRAFT", adminId, "약관 신규 등록");

        ApprovalRequest approval = ApprovalRequest.builder()
                .requestTypeCode("TERMS_PUBLISH")
                .requesterAdminId(adminId)
                .targetId(terms.getTermsId())
                .requestComment(request.getChangeSummary())
                .build();
        approvalMapper.insertApprovalRequest(approval);

        ApprovalLine line = ApprovalLine.builder()
                .approvalId(approval.getApprovalId())
                .approverAdminId(1L)
                .approvalOrder(1)
                .statusCode("PENDING")
                .build();
        approvalMapper.insertApprovalLine(line);

        java.util.Map<String, Long> result = new java.util.HashMap<>();
        result.put("termsId",    terms.getTermsId());
        result.put("approvalId", approval.getApprovalId());
        return result;
    }

    // ════════════════════════════════════════════════════════════
    // PDF + JPG 파일 업로드 공통 메서드
    // ════════════════════════════════════════════════════════════
    private void uploadTermsFiles(Long termsId, MultipartFile pdfFile) throws IOException {
        UploadResult pdfMeta   = fileStorageService.extractMeta(pdfFile, "terms");
        byte[]       pdfBytes  = pdfFile.getBytes();

        List<byte[]> imageBytesList = pdfConvertService.convertPdfToImageBytes(pdfFile);
        String baseName = pdfMeta.getStoredName()
                .substring(0, pdfMeta.getStoredName().lastIndexOf("."));

        // ── PDF 업로드 — DB에는 objectName만 저장 ──────────────────────
        objectStorageService.upload(pdfMeta.getObjectName(), pdfBytes, pdfMeta.getMimeType());

        termsMapper.insertTermsFile(TermsFile.builder()
                .termsId(termsId)
                .fileType("PDF")
                .filePath(pdfMeta.getObjectName())   // ← PAR URL 아닌 objectName 저장
                .originalName(pdfMeta.getOriginalName())
                .storedName(pdfMeta.getStoredName())
                .fileExtension(pdfMeta.getFileExtension())
                .fileSize(pdfMeta.getFileSize())
                .mimeType(pdfMeta.getMimeType())
                .isPrimary("Y")
                .build());

        // ── 이미지 업로드 — DB에는 objectName만 저장 ───────────────────
        for (int i = 0; i < imageBytesList.size(); i++) {
            String imageStoredName = baseName + "_page" + (i + 1) + ".jpg";
            String imageObjectName = "terms/" + imageStoredName;

            objectStorageService.upload(imageObjectName, imageBytesList.get(i), "image/jpeg");

            termsMapper.insertTermsFile(TermsFile.builder()
                    .termsId(termsId)
                    .fileType("IMAGE")
                    .filePath(imageObjectName)       // ← PAR URL 아닌 objectName 저장
                    .originalName(pdfMeta.getOriginalName().replace(".pdf", ".jpg"))
                    .storedName(imageStoredName)
                    .fileExtension("jpg")
                    .mimeType("image/jpeg")
                    .isPrimary("N")
                    .build());
        }

        log.info("[약관등록] 파일 업로드 완료: termsId={}, pages={}", termsId, imageBytesList.size());
    }

    // ════════════════════════════════════════════════════════════
    // 기존 약관에 파일만 추가
    // ════════════════════════════════════════════════════════════
    @Transactional
    public void addFileToExistingTerms(Long termsId, MultipartFile pdfFile) throws IOException {
        termsMapper.findById(termsId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TERMS_NOT_FOUND));
        uploadTermsFiles(termsId, pdfFile);
    }

    // ════════════════════════════════════════════════════════════
    // 약관 상태 변경
    // ════════════════════════════════════════════════════════════
    @Transactional
    public void changeTermsStatus(Long termsId, TermsStatusRequest request, Long adminId) {
        Terms terms = termsMapper.findById(termsId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TERMS_NOT_FOUND));

        String previousStatus = terms.getStatus();
        String newStatus      = request.getNewStatus();
        if (previousStatus.equals(newStatus)) return;

        termsMapper.updateTermsStatus(termsId, newStatus, adminId);
        termsMapper.insertStatusHistory(termsId, previousStatus, newStatus,
                adminId, request.getChangedReason());

        if ("PUBLISHED".equals(newStatus)) {
            termsMapper.supersedePreviousPublished(terms.getTermsMasterId(), termsId);
            log.info("[약관상태변경] 기존 PUBLISHED → SUPERSEDED 처리: termsMasterId={}",
                    terms.getTermsMasterId());

            if ("Y".equals(terms.getReconsentRequiredYn())) {
                List<Long> userIds = termsMapper.findUserIdsForReconsent(termsId);
                userIds.forEach(uid -> termsMapper.insertNotificationHistory(termsId, uid, "EMAIL"));
                log.info("[약관상태변경] 재동의 알림 {}명 발송: termsId={}", userIds.size(), termsId);
            }
        }
        log.info("[약관상태변경] termsId={}, {} → {}", termsId, previousStatus, newStatus);
    }

    // ════════════════════════════════════════════════════════════
    // 조회 메서드
    // ════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public List<TermsAdminResponse> getTermsList(String status) {
        return termsMapper.findAllForAdmin(status).stream()
                .map(t -> TermsAdminResponse.builder()
                        .termsId(t.getTermsId())
                        .termsMasterId(t.getTermsMasterId())
                        .title(t.getTitle())
                        .version(t.getVersion())
                        .status(t.getStatus())
                        .requiredYn(t.getRequiredYn())
                        .reconsentRequiredYn(t.getReconsentRequiredYn())
                        .effectiveFrom(t.getEffectiveFrom())
                        .createdAt(t.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TermsAdminResponse getTermsDetail(Long termsId) {
        Terms terms = termsMapper.findByIdWithMaster(termsId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TERMS_NOT_FOUND));

        List<TermsFile> files = termsMapper.findFilesByTermsId(termsId);

        List<TermsFileResponse> fileResponses = files.stream()
                .map(f -> TermsFileResponse.builder()
                        .fileId(f.getFileId())
                        .termsId(f.getTermsId())
                        .fileType(f.getFileType())
                        // [수정] objectName → 요청 시점 PAR URL로 변환
                        .filePath(objectStorageService.resolveUrl(f.getFilePath()))
                        .originalName(f.getOriginalName())
                        .isPrimary(f.getIsPrimary())
                        .build())
                .collect(Collectors.toList());

        return TermsAdminResponse.builder()
                .termsId(terms.getTermsId())
                .termsMasterId(terms.getTermsMasterId())
                .title(terms.getTitle())
                .version(terms.getVersion())
                .status(terms.getStatus())
                .requiredYn(terms.getRequiredYn())
                .reconsentRequiredYn(terms.getReconsentRequiredYn())
                .effectiveFrom(terms.getEffectiveFrom())
                .createdAt(terms.getCreatedAt())
                .files(fileResponses)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TermsMasterResponse> getTermsMasters() {
        return termsMapper.findAllMasters().stream()
                .map(m -> TermsMasterResponse.builder()
                        .termsMasterId(m.getTermsMasterId())
                        .termsType(m.getTermsType())
                        .title(m.getTitle())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void createTermsMaster(TermsMasterCreateRequest request, Long adminId) {
        TermsMaster master = TermsMaster.builder()
                .termsType(request.getTermsType())
                .title(request.getTitle())
                .description(request.getDescription())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 99)
                .requiredYn(request.getRequiredYn() != null ? request.getRequiredYn() : "N")
                .build();
        termsMapper.insertTermsMaster(master);
        log.info("[마스터등록] termsMasterId={}, title={}", master.getTermsMasterId(), master.getTitle());
    }
    
    @Transactional(readOnly = true)
    public String suggestNextVersion(Long termsMasterId) {
        String latest = termsMapper.findLatestVersionByMasterId(termsMasterId);
        if (latest == null) return "1.0";
        try {
            String[] parts = latest.split("\\.");
            int minor = Integer.parseInt(parts[parts.length - 1]) + 1;
            parts[parts.length - 1] = String.valueOf(minor);
            return String.join(".", parts);
        } catch (Exception e) {
            return latest + ".1";
        }
    }
}