package com.bnk.domain.application.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bnk.domain.application.dto.request.ReviewResultRequest;
import com.bnk.domain.application.service.CheckCardApplicationService;
import com.bnk.domain.application.service.CreditCardApplicationService;
import com.bnk.global.util.InternalCallbackValidator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/callback")
@RequiredArgsConstructor
public class ReviewCallbackController {

	private final CreditCardApplicationService creditCardApplicationService;
	private final CheckCardApplicationService  checkCardApplicationService;
	private final InternalCallbackValidator    internalCallbackValidator;

    // 신용카드 추가 심사 결과 콜백 (X-Internal-Secret 필요)
    @PostMapping("/credit/review-result")
    public ResponseEntity<Void> creditReviewResult(
            HttpServletRequest httpRequest,
            @Valid @RequestBody ReviewResultRequest request) {
        internalCallbackValidator.validate(httpRequest);
        creditCardApplicationService.saveReviewResult(request);
        return ResponseEntity.ok().build();
    }

    // 체크카드 추가 심사 결과 콜백 (X-Internal-Secret 필요)
    @PostMapping("/check/review-result")
    public ResponseEntity<Void> checkReviewResult(
            HttpServletRequest httpRequest,
            @Valid @RequestBody ReviewResultRequest request) {
        internalCallbackValidator.validate(httpRequest);
        checkCardApplicationService.saveReviewResult(request);
        return ResponseEntity.ok().build();
    }

}
