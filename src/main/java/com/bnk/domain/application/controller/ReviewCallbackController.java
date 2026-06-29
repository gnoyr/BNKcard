package com.bnk.domain.application.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bnk.domain.application.dto.request.ReviewResultRequest;
import com.bnk.domain.application.dto.request.ScreeningResultRequest;
import com.bnk.domain.application.service.CheckCardApplicationService;
import com.bnk.domain.application.service.CreditCardApplicationService;

import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/callback")
@RequiredArgsConstructor
public class ReviewCallbackController {

	private final CreditCardApplicationService creditCardApplicationService;
	
    // 신용카드 추가 심사 결과 콜백
    @PostMapping("/credit/review-result")
    public ResponseEntity<Void> creditReviewResult(
            @RequestBody ReviewResultRequest request) {
        creditCardApplicationService.saveReviewResult(request);
        return ResponseEntity.ok().build();
    }

}
