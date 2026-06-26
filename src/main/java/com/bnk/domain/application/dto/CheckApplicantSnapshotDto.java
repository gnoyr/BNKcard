package com.bnk.domain.application.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class CheckApplicantSnapshotDto {
    private String name;
    private String nameEn;
    private String mobileNo;
    private String address;
    private String email;
    private String jobType;
    private String transactionPurpose;
    private String fundSource;
    private String birthDate; // 추가 — 한도 산정 나이 계산용 (yyyy-MM-dd)
}