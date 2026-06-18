package com.bnk.domain.application.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

import com.bnk.domain.application.dto.CheckApplicantSnapshotDto;
import com.bnk.domain.application.dto.PaymentSnapshotDto;

@Getter @Setter @NoArgsConstructor
public class CheckCardApplicationRequest {

    private Long cardId;

    // STEP 1 - 약관 동의
    private List<AgreedTermsItem> agreedTerms;

    // STEP 2 - 본인확인
    private Long   checkAppId;	   // STEP 1에서 생성된 ID, UPDATE 대상 식별용
    private String idType;         // RESIDENT / DRIVER, API 호출 후 버림
    private String idName;         // OCR 추출, API 호출 후 버림
    private String idResidentNo;   // OCR 추출 (주민번호 앞 6자리), API 호출 후 버림
    private String idAddress;      // OCR 추출, API 호출 후 버림
    private String idIssueDateOcr;      // OCR 추출 등록일자, API 호출 후 버림
    private String idIssueDateInput;    // 사용자 직접 기입 등록일자

    // STEP 3 - 계좌 선택
    private Long linkedAccountId;

    // STEP 4 - 기본정보 + 신청정보
    private CheckApplicantSnapshotDto applicantSnapshot;
    private PaymentSnapshotDto        paymentSnapshot;
    private String 					  cardPassword;  // 서비스에서 BCrypt 후 USER_CARDS에 저장

    @Getter @Setter @NoArgsConstructor
    public static class AgreedTermsItem {
        private Long   termsId;
        private String agreedYn;
    }
}