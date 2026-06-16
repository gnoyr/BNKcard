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
}