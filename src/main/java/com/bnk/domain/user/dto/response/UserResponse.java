package com.bnk.domain.user.dto.response;

import com.bnk.domain.user.model.User;
import com.bnk.global.util.MaskingUtil;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 사용자 정보 응답 DTO.
 *
 * [birthDate 타입 주의]
 *   User 모델의 birthDate는 step1_ddl.sql에서 DATE → VARCHAR2(200)로 마이그레이션 완료.
 *   따라서 String으로 받아 앞 4자리(년도)만 마스킹 처리.
 *   ex) "1990-07-15" → "1990-**-**"
 */
@Getter
@Builder
public class UserResponse {

	private Long userId;
	private String maskedName; // 홍*동
	private String maskedEmail; // ab***@domain.com
	private String maskedPhone; // 010-****-5678
	private String maskedBirthDate; // 1990-**-**

	private String job;
	private String incomeLevelCode;
	private Integer creditScore;
	private String statusCode;
	private String isEmailVerified;
	private String isPhoneVerified;
	private String pushEnabled;
	private String marketingAgree;
	private LocalDateTime lastLoginAt;
	private LocalDateTime createdAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .maskedName(MaskingUtil.maskName(user.getName()))
                .maskedEmail(MaskingUtil.maskEmail(user.getEmail()))
                .maskedPhone(MaskingUtil.maskPhone(user.getPhone()))
                .maskedBirthDate(maskBirthDate(user.getBirthDate()))
                .job(user.getJob())
                .incomeLevelCode(user.getIncomeLevelCode())
                .creditScore(user.getCreditScore())
                .statusCode(user.getStatusCode())
                .isEmailVerified(user.getIsEmailVerified())
                .isPhoneVerified(user.getIsPhoneVerified())
                .pushEnabled(user.getPushEnabled())
                .marketingAgree(user.getMarketingAgree())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * birthDate: "1990-07-15" 또는 "19900715" 형태 모두 처리.
     * 앞 4자리 년도만 남기고 나머지 마스킹.
     * "1990-07-15" → "1990-**-**"
     * "19900715"   → "1990-**-**"
     */
    private static String maskBirthDate(LocalDate birthDate) {
        if (birthDate == null) return null;         
        String str = birthDate.toString();         
        return str.substring(0, 4) + "-**-**";
    }
}