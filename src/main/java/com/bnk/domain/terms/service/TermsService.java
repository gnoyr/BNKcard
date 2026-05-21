package com.bnk.domain.terms.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class TermsService {

    private final TermsMapper termsMapper;
    private final UserTermsAgreementMapper userTermsAgreementMapper;

    /**
     * F-16 약관 패키지 조회
     */
    @Transactional(readOnly = true)
    public TermsPackageResponse getTermsPackage(String packageType) {
        List<Terms> termsList = termsMapper.findByPackageType(packageType);

        if (termsList.isEmpty()) {
            throw new BusinessException(ErrorCode.TERMS_NOT_FOUND);
        }

        List<TermsPackageResponse.TermsItem> items = termsList.stream()
                .map(TermsPackageResponse::fromTerms)
                .collect(Collectors.toList());

        return TermsPackageResponse.builder()
                .packageType(packageType)
                .terms(items)
                .build();
    }

    /**
     * F-17 약관 동의 처리
     * required_yn='Y' 전체 동의 검증 → USER_TERMS_AGREEMENTS 배치 INSERT
     */
    @Transactional
    public List<Long> agreeTerms(@Valid TermsAgreementRequest request, Long userId) {
        // 동의 목록에서 Y로 동의한 termsId 집합
        Set<Long> agreedIds = request.getAgreedTerms().stream()
                .filter(item -> "Y".equals(item.getAgreedYn()))
                .map(TermsAgreementRequest.AgreedTermsItem::getTermsId)
                .collect(Collectors.toSet());

        // 요청한 termsId들 DB 조회 후 필수 약관 동의 검증
        List<Long> allTermsIds = request.getAgreedTerms().stream()
                .map(TermsAgreementRequest.AgreedTermsItem::getTermsId)
                .collect(Collectors.toList());

        boolean requiredNotAgreed = allTermsIds.stream()
                .map(id -> termsMapper.findById(id))
                .filter(opt -> opt.isPresent())
                .map(opt -> opt.get())
                .filter(t -> "Y".equals(t.getRequiredYn()))
                .anyMatch(t -> !agreedIds.contains(t.getTermsId()));

        if (requiredNotAgreed) {
            throw new BusinessException(ErrorCode.REQUIRED_TERMS_NOT_AGREED);
        }

        // USER_TERMS_AGREEMENTS 배치 INSERT
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

        userTermsAgreementMapper.insertAgreements(agreements);

        return agreements.stream()
                .map(UserTermsAgreement::getTermsId)
                .collect(Collectors.toList());
    }
    
    // TermsService.java에 추가
    public List<TermsFileResponse> getTermsFiles(Long termsId) {
    	// 약관 존재 여부 확인
        termsMapper.findById(termsId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TERMS_NOT_FOUND));

        List<TermsFile> files = termsMapper.findFilesByTermsId(termsId);
        
        return files.stream()
                .map(f -> TermsFileResponse.builder()
                        .fileId(f.getFileId())
                        .termsId(f.getTermsId())
                        .fileType(f.getFileType())
                        .filePath(f.getFilePath())
                        .originalName(f.getOriginalName())
                        .fileExtension(f.getFileExtension())
                        .fileSize(f.getFileSize())
                        .mimeType(f.getMimeType())
                        .isPrimary(f.getIsPrimary())
                        .build())
                .collect(Collectors.toList());
    }
}