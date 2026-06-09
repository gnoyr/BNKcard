package com.bnk.domain.terms.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.bnk.domain.terms.dto.request.AgreedTermsItem;
import com.bnk.domain.terms.dto.request.TermsAgreementRequest;
import com.bnk.domain.terms.dto.response.TermsFileResponse;
import com.bnk.domain.terms.dto.response.TermsPackageResponse;
import com.bnk.domain.terms.mapper.TermsMapper;
import com.bnk.domain.terms.mapper.UserTermsAgreementMapper;
import com.bnk.domain.terms.model.Terms;
import com.bnk.domain.terms.model.TermsFile;
import com.bnk.domain.terms.model.UserTermsAgreement;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.log.annotation.Loggable;
import com.bnk.global.util.ObjectStorageService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class TermsService {

    private final TermsMapper              termsMapper;
    private final UserTermsAgreementMapper userTermsAgreementMapper;
    private final ObjectStorageService     objectStorageService; 

    private static final int BATCH_SIZE = 50;

    // ─────────────────────────────────────────────────────────────
    // F-16 | 약관 패키지 조회
    // ─────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public TermsPackageResponse getTermsPackage(String packageType) {
        List<Terms> termsList = termsMapper.findByPackageType(packageType);

        if (termsList.isEmpty()) {
            throw new BusinessException(ErrorCode.TERMS_NOT_FOUND);
        }

        List<TermsPackageResponse.TermsItem> items = termsList.stream()
                .map(TermsPackageResponse.TermsItem::from)
                .collect(Collectors.toList());

        return TermsPackageResponse.builder()
                .packageType(packageType)
                .terms(items)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // F-17 | 약관 동의 처리
    // ─────────────────────────────────────────────────────────────
    @Transactional
    @Loggable(eventType = "TERMS_AGREE", targetType = "TERMS", actionDetail = "약관동의")
    public List<Long> agreeTerms(@Valid TermsAgreementRequest request, Long userId) {

        Set<Long> agreedIds = request.getAgreedTerms().stream()
                .filter(item -> "Y".equals(item.getAgreedYn()))
                .map(AgreedTermsItem::getTermsId)
                .collect(Collectors.toSet());

        List<Long> requestedTermsIds = request.getAgreedTerms().stream()
                .map(AgreedTermsItem::getTermsId)
                .collect(Collectors.toList());

        for (Long termsId : requestedTermsIds) {
            Terms terms = termsMapper.findById(termsId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.TERMS_NOT_FOUND));

            if ("Y".equals(terms.getRequiredYn()) && !agreedIds.contains(termsId)) {
                log.warn("[약관동의] 필수 약관 미동의: userId={}, termsId={}", userId, termsId);
                throw new BusinessException(ErrorCode.REQUIRED_TERMS_NOT_AGREED);
            }
        }

        List<UserTermsAgreement> agreements = request.getAgreedTerms().stream()
                .map(item -> UserTermsAgreement.builder()
                        .userId(userId)
                        .termsId(item.getTermsId())
                        .agreedYn(item.getAgreedYn())
                        .agreementAction("Y".equals(item.getAgreedYn()) ? "AGREE" : "DISAGREE")
                        .agreementSource(request.getAgreementSource())
                        .agreementChannel(request.getAgreementChannel())
                        .agreedAt(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());

        List<Long> insertedIds = new ArrayList<>();
        for (int i = 0; i < agreements.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, agreements.size());
            List<UserTermsAgreement> batch = agreements.subList(i, end);
            userTermsAgreementMapper.insertAgreements(batch);
            log.debug("[약관동의] 배치 INSERT: userId={}, 건수={}", userId, batch.size());
        }

        insertedIds = agreements.stream()
                .map(UserTermsAgreement::getTermsId)
                .collect(Collectors.toList());

        return insertedIds;
    }

    // ─────────────────────────────────────────────────────────────
    // 약관 파일 URL 조회 (PDF 다운로드용)
    // ─────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<TermsFileResponse> getTermsFiles(Long termsId) {
        termsMapper.findById(termsId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TERMS_NOT_FOUND));

        List<TermsFile> files = termsMapper.findFilesByTermsId(termsId);

        return files.stream()
                .map(f -> TermsFileResponse.builder()
                        .fileId(f.getFileId())
                        .termsId(f.getTermsId())
                        .fileType(f.getFileType())
                        .filePath(objectStorageService.resolveUrl(f.getFilePath()))
                        .originalName(f.getOriginalName())
                        .fileExtension(f.getFileExtension())
                        .fileSize(f.getFileSize())
                        .mimeType(f.getMimeType())
                        .isPrimary(f.getIsPrimary())
                        .build())
                .collect(Collectors.toList());
    }
}