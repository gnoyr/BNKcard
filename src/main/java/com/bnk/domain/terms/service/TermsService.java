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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class TermsService {

    private final TermsMapper termsMapper;
    private final UserTermsAgreementMapper userTermsAgreementMapper;

    /**
     * 약관 1건당 INSERT INTO ... VALUES(...) 구문이 약 400바이트이므로
     * 100건 × 400B = 40KB — 안전 범위.
     * 일반적인 약관 동의는 5~20건이므로 실질적으로 단일 배치로 처리됨.
     */
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
    public List<Long> agreeTerms(@Valid TermsAgreementRequest request, Long userId) {

		//클라이언트가 Y로 동의한 termsId 집합
		Set<Long> agreedIds = request.getAgreedTerms().stream().filter(item -> "Y".equals(item.getAgreedYn()))
				.map(AgreedTermsItem::getTermsId).collect(Collectors.toSet());

		// 검증 방식:
		// 1) 요청에 포함된 termsId를 DB에서 조회해 required_yn 확인
		// 2) 추가로 packageType 기반으로 패키지 전체 필수 약관과 비교하면 더 강하지만,
		// 현재 구조(TermsAgreementRequest에 packageType 없음)에서는 요청 내 termsId
		// 기준으로 DB 재조회하여 검증하는 방식 적용.
		List<Long> requestedTermsIds = request.getAgreedTerms().stream().map(AgreedTermsItem::getTermsId)
				.collect(Collectors.toList());

		for (Long termsId : requestedTermsIds) {
			Terms terms = termsMapper.findById(termsId)
					.orElseThrow(() -> new BusinessException(ErrorCode.TERMS_NOT_FOUND));

			if ("Y".equals(terms.getRequiredYn()) && !agreedIds.contains(termsId)) {
				log.warn("[약관동의] 필수 약관 미동의: userId={}, termsId={}", userId, termsId);
				throw new BusinessException(ErrorCode.REQUIRED_TERMS_NOT_AGREED);
			}
		}

        // UserTermsAgreement 목록 구성
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

		// Oracle INSERT ALL SQL 길이 제한 방어 — 배치 분할 INSERT
		// MyBatis <foreach> + Oracle INSERT ALL 은 단일 SQL로 실행되므로
		// 건수가 많으면 ORA-01795 (최대 표현식 초과) 발생 가능.
		// BATCH_SIZE(50건) 단위로 분할하여 순차 INSERT.
		// 일반 약관 동의(5~20건)는 항상 단일 배치로 처리되므로 성능 영향 없음.
		List<Long> insertedIds = new ArrayList<>();
		for (int i = 0; i < agreements.size(); i += BATCH_SIZE) {
			int end = Math.min(i + BATCH_SIZE, agreements.size());
			List<UserTermsAgreement> batch = agreements.subList(i, end);
			userTermsAgreementMapper.insertAgreements(batch);
			log.debug("[약관동의] 배치 INSERT: userId={}, 건수={}", userId, batch.size());
		}

        // 처리된 termsId 목록 반환 (inserted agreements 기준)
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