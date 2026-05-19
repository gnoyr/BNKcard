package com.bnk.domain.terms.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.bnk.domain.terms.dto.request.TermsAgreementRequest;
import com.bnk.domain.terms.dto.response.TermsPackageResponse;
import com.bnk.domain.terms.mapper.TermsMapper;
import com.bnk.domain.terms.model.Terms;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class TermsService {

    private final TermsMapper termsMapper;

    // ──────────────────────────────────────────────────────────────
    // 약관 패키지 조회 — 회원가입/카드신청 화면에서 비로그인 호출
    // ──────────────────────────────────────────────────────────────
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

    // ──────────────────────────────────────────────────────────────
    // 약관 동의 처리 — 로그인 후 호출 (TODO)
    // ──────────────────────────────────────────────────────────────
    public List<Long> agreeTerms(@Valid TermsAgreementRequest request, Long userId) {
        // TODO: 구현 예정
        return null;
    }
}