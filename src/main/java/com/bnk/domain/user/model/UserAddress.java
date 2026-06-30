package com.bnk.domain.user.model;

import lombok.*;
import java.time.LocalDateTime;

/**
 * USER_ADDRESSES 테이블 매핑 모델 (MyBatis)
 *
 * [암호화 컬럼]
 * address       : AES-256-GCM 암호화 저장
 *                 UserAddressMapper.xml resultMap → typeHandler=aesTypeHandler → 조회 시 자동 복호화
 * addressDetail : AES-256-GCM 암호화 저장 (상세주소)
 *
 * USERS.phone / USER_TRUSTED_IPS.ip_address 와 동일한 암복호화 패턴.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAddress {

    private Long          addressId;
    private Long          userId;
    private String        alias;          // 별칭 (집, 회사 등)
    private String        zipcode;        // 우편번호 (평문)
    private String        address;        // AES 복호화 후 평문 (도로명/지번)
    private String        addressDetail;  // AES 복호화 후 평문 (상세주소)
    private String        isDefault;      // Y / N
    private String        statusCode;     // ACTIVE / DISABLED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String        deletedYn;
    private LocalDateTime deletedAt;

    public boolean isDefaultAddress() {
        return "Y".equals(this.isDefault);
    }
}
