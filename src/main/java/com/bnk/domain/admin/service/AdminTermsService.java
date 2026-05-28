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
    private final ApprovalMapper       approvalMapper;   // ← 추가

    // ══════════════════════════════════════════════════════════════════
    // 약관 신규 등록 + 결재 신청 (수정 후)
    // ══════════════════════════════════════════════════════════════════
    @Transactional
    public java.util.Map<String, Long> registerTermsWithApproval(
            TermsCreateRequest request,
            MultipartFile pdfFile,
            Long adminId) throws IOException {

        // ── 1. TERMS INSERT (status = DRAFT) ──────────────────────────
        Terms terms = Terms.builder()
                .termsMasterId(request.getTermsMasterId())
                .version(request.getVersion())
                .contentHtml(request.getContentHtml())
                .requiredYn(request.getRequiredYn())
                .reconsentRequiredYn(request.getReconsentRequiredYn())
                .effectiveFrom(request.getEffectiveFrom())
                .build();
        termsMapper.insertTerms(terms);   // termsId 채번 완료

        log.info("[약관등록] TERMS INSERT 완료: termsId={}, status=DRAFT", terms.getTermsId());

        // ── 2. PDF/JPG 파일 업로드 ────────────────────────────────────
        uploadTermsFiles(terms.getTermsId(), pdfFile);

        // ── 3. TERMS_STATUS_HISTORY INSERT (DRAFT 최초 이력) ──────────
        termsMapper.insertStatusHistory(
                terms.getTermsId(), null, "DRAFT", adminId, "약관 신규 등록");

        // ── 4. APPROVAL_REQUESTS INSERT ───────────────────────────────
        ApprovalRequest approval = ApprovalRequest.builder()
                .requestTypeCode("TERMS_PUBLISH")
                .requesterAdminId(adminId)
                .targetId(terms.getTermsId())      // ← terms_id를 직접 target_id로
                .requestComment(request.getChangeSummary())
                .build();
        approvalMapper.insertApprovalRequest(approval);

        log.info("[약관등록] APPROVAL_REQUESTS INSERT 완료: approvalId={}", approval.getApprovalId());

        // ── 5. APPROVAL_LINES INSERT ──────────────────────────────────
        // 결재자는 1번 관리자(superAdmin) 고정 — 실 운영에서는 결재라인 설정 기반으로 동적 지정
        ApprovalLine line = ApprovalLine.builder()
                .approvalId(approval.getApprovalId())
                .approverAdminId(1L)
                .approvalOrder(1)
                .statusCode("PENDING")
                .build();
        approvalMapper.insertApprovalLine(line);

        log.info("[약관등록] APPROVAL_LINES INSERT 완료: approvalLineId={}", line.getApprovalLineId());

        java.util.Map<String, Long> result = new java.util.HashMap<>();
        result.put("termsId",    terms.getTermsId());
        result.put("approvalId", approval.getApprovalId());
        return result;
    }

    // ── PDF + JPG 파일 업로드 공통 메서드 ─────────────────────────────
    private void uploadTermsFiles(Long termsId, MultipartFile pdfFile) throws IOException {
        UploadResult pdfMeta = fileStorageService.extractMeta(pdfFile, "terms");
        byte[] pdfBytes = pdfFile.getBytes();

        objectStorageService.upload(pdfMeta.getObjectName(), pdfBytes, pdfMeta.getMimeType());
        String pdfUrl = objectStorageService.getPublicUrl(pdfMeta.getObjectName());

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
        log.info("[약관등록] 파일 업로드 완료: termsId={}, pages={}", termsId, imageBytesList.size());
    }

    // ══════════════════════════════════════════════════════════════════
    // 기존 약관에 파일만 추가 (파일 업로드 탭)
    // ══════════════════════════════════════════════════════════════════
    @Transactional
    public void addFileToExistingTerms(Long termsId, MultipartFile pdfFile) throws IOException {
        termsMapper.findById(termsId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TERMS_NOT_FOUND));
        uploadTermsFiles(termsId, pdfFile);
    }

    // ══════════════════════════════════════════════════════════════════
    // 약관 상태 직접 변경 (관리자 수동 — 긴급 처리용으로만 사용)
    // 정상 흐름은 Approval 승인을 통해 처리됨
    // ══════════════════════════════════════════════════════════════════
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
            // 같은 terms_master_id의 기존 PUBLISHED → SUPERSEDED 처리
            termsMapper.supersedePreviousPublished(terms.getTermsMasterId(), termsId);
            log.info("[약관상태변경] 기존 PUBLISHED → EXPIRED 처리: termsMasterId={}",
                    terms.getTermsMasterId());

            if ("Y".equals(terms.getReconsentRequiredYn())) {
                List<Long> userIds = termsMapper.findUserIdsForReconsent(termsId);
                userIds.forEach(uid -> termsMapper.insertNotificationHistory(termsId, uid, "EMAIL"));
                log.info("[약관상태변경] 재동의 알림 {}명 발송: termsId={}", userIds.size(), termsId);
            }
        }
        log.info("[약관상태변경] termsId={}, {} → {}", termsId, previousStatus, newStatus);
    }

    // ══════════════════════════════════════════════════════════════════
    // 조회 메서드들 (변경 없음)
    // ══════════════════════════════════════════════════════════════════
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

        // TermsAdminResponse.files 타입 = List<TermsFileResponse>
        List<TermsFileResponse> fileResponses = files.stream()
                .map(f -> TermsFileResponse.builder()
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
                .contentHtml(terms.getContentHtml())
                .status(terms.getStatus())
                .requiredYn(terms.getRequiredYn())
                .reconsentRequiredYn(terms.getReconsentRequiredYn())
                .effectiveFrom(terms.getEffectiveFrom())
                .effectiveTo(terms.getEffectiveTo())
                .documentHash(terms.getDocumentHash())
                .internalNote(terms.getInternalNote())
                .approvedBy(terms.getApprovedBy())
                .approvedAt(terms.getApprovedAt())
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
                        .description(m.getDescription())
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
        if (latest == null) return "v1.0";
        // v1.0 → v2.0, v2.3 → v3.0 형태로 메이저 버전만 올림
        try {
            String numeric = latest.replaceAll("[^0-9.]", "");
            int major = Integer.parseInt(numeric.split("\\.")[0]);
            return "v" + (major + 1) + ".0";
        } catch (Exception e) {
            return latest + "_new";
        }
    }
    
}