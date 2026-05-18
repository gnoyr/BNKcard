package com.bnk.domain.terms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.bnk.domain.terms.dto.request.TermsAgreementRequest;
import com.bnk.domain.terms.dto.response.TermsPackageResponse;

import jakarta.validation.Valid;

@Service
@Validated
public class TermsService {

	public List<Long> agreeTerms(@Valid TermsAgreementRequest request, Long userId) {
		// TODO Auto-generated method stub
		return null;
	}

	public TermsPackageResponse getTermsPackage(String packageType) {
		// TODO Auto-generated method stub
		return null;
	}

}
