package com.bnk.domain.user.model;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.bnk.domain.user.dto.request.UserUpdateRequest;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
	private Long userId;
	private String email;
	private String passwordHash; // 응답 DTO에서 반드시 제외
	private String name;
	private String phone;
	private LocalDate birthDate;
	private String ciValue; // 응답 DTO에서 반드시 제외
	private String job;
	private String incomeLevelCode;
	private Integer creditScore;
	private String statusCode; // ACTIVE / SUSPENDED / DORMANT / WITHDRAWN
	private Integer loginFailCount;
	private LocalDateTime lockedUntil;
	private LocalDateTime lastLoginAt;
	private LocalDateTime lastPasswordChangedAt;
	private String isEmailVerified; // Y / N
	private String isPhoneVerified; // Y / N
	private String pushEnabled; // Y / N
	private String marketingAgree; // Y / N
	private String privacyAgree; // Y / N
	private LocalDateTime dormantAt;
	private LocalDateTime withdrawnAt;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private String deletedYn;
	private String cddStatusCode; // PENDING / VERIFIED / ENHANCED / REJECTED
	
	public User applyUpdate(UserUpdateRequest request) {
	    return User.builder()
	            .userId(this.userId)
	            .email(this.email)
	            .passwordHash(this.passwordHash)
	            .name(request.getName() != null ? request.getName() : this.name)
	            .phone(request.getPhone() != null ? request.getPhone() : this.phone)
	            .birthDate(this.birthDate)
	            .ciValue(this.ciValue)
	            .job(request.getJob() != null ? request.getJob() : this.job)
	            .incomeLevelCode(request.getIncomeLevelCode() != null ? request.getIncomeLevelCode() : this.incomeLevelCode)
	            .statusCode(this.statusCode)
	            .loginFailCount(this.loginFailCount)
	            .lockedUntil(this.lockedUntil)
	            .lastLoginAt(this.lastLoginAt)
	            .lastPasswordChangedAt(this.lastPasswordChangedAt)
	            .isEmailVerified(this.isEmailVerified)
	            .isPhoneVerified(this.isPhoneVerified)
	            // ★ Boolean → "Y"/"N" 변환
	            .pushEnabled(request.getPushEnabled() != null
	                    ? (request.getPushEnabled() ? "Y" : "N")
	                    : this.pushEnabled)
	            .marketingAgree(request.getMarketingAgree() != null
	                    ? (request.getMarketingAgree() ? "Y" : "N")
	                    : this.marketingAgree)
	            .privacyAgree(this.privacyAgree)
	            .dormantAt(this.dormantAt)
	            .withdrawnAt(this.withdrawnAt)
	            .createdAt(this.createdAt)
	            .updatedAt(this.updatedAt)
	            .deletedYn(this.deletedYn)
	            .cddStatusCode(this.cddStatusCode)
	            .build();
	}
}
