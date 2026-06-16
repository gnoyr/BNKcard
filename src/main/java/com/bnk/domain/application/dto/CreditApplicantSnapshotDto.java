package com.bnk.domain.application.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Getter @Setter @NoArgsConstructor
public class CreditApplicantSnapshotDto {
    private String name;
    private String nameEn;
    private String mobileNo;
    private String address;
    private String email;
    private String idIssueDate;  // 신분증 발급 일자

    private String incomeType;           // 직업 구분 (직장인/자영업자/전문직 등)
    private String healthInsuranceType;  // 건강보험 유형 (직장가입자/지역가입자)
    private String hasRealEstate;        // 부동산 보유 여부 Y/N
    private String hasOwnVehicle;        // 자차 보유 여부 Y/N
}