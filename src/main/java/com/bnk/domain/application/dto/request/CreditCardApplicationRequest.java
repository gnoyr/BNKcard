package com.bnk.domain.application.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

import com.bnk.domain.application.dto.CreditApplicantSnapshotDto;
import com.bnk.domain.application.dto.PaymentSnapshotDto;

@Getter @Setter @NoArgsConstructor
public class CreditCardApplicationRequest {

    private Long cardId;

    // STEP 1 - 약관 동의
    private List<AgreedTermsItem> agreedTerms;

    // STEP 2 - 본인확인
    private Long   creditAppId;    // STEP 1에서 생성된 ID, UPDATE 대상 식별용
    private String idType;         // RESIDENT / DRIVER
    private String idName;         // OCR 추출, API 호출 후 버림
    private String idResidentNo;   // OCR 추출 (주민번호 앞 6자리), API 호출 후 버림
    private String idAddress;      // OCR 추출, API 호출 후 버림
    private String idIssueDateOcr;      // OCR 추출 등록일자, API 호출 후 버림
    private String idIssueDateInput;    // 사용자 직접 기입 등록일자

    // STEP 3 - 기본정보 + 직업/소득
    private CreditApplicantSnapshotDto applicantSnapshot;   // { name, name_en, mobile_no, address, email, id_issue_date, income_type, health_insurance_type, has_real_estate, has_own_vehicle }
    private String                     annualIncomeBand;
    private String                     creditScoreBand;
    private Long                       linkedAccountId;     // 연회비 자동이체 계좌 ID

    // STEP 4 - 신청정보
    private PaymentSnapshotDto paymentSnapshot;
    private Long               requestedLimit;
    private String 			   cardPassword;  // 서비스에서 BCrypt 후 USER_CARDS에 저장

    // STEP 5 - 서류 (신규고객)
    private String incomeDocKey;
    private String assetDocKey;
    private String jobDocKey;

    @Getter @Setter @NoArgsConstructor
    public static class AgreedTermsItem {
        private Long   termsId;
        private String agreedYn;
        private String agreedVersion;
        private String agreedContentSnapshot;
    }
}