package com.bnk.domain.user.dto.response;

import com.bnk.domain.user.model.UserAddress;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * GET /api/users/me/addresses 응답 DTO.
 * 본인 소유 주소이므로 마스킹 없이 평문(복호화 결과)을 그대로 내려준다.
 */
@Getter
@Builder
public class AddressResponse {

    private Long          addressId;
    private String        alias;
    private String        zipcode;
    private String        address;
    private String        addressDetail;

    /** 기본 배송지 여부. Jackson "is" 접두사 제거를 막기 위해 명시. */
    @JsonProperty("isDefault")
    private boolean       isDefault;

    private LocalDateTime createdAt;

    public static AddressResponse from(UserAddress m) {
        return AddressResponse.builder()
                .addressId(m.getAddressId())
                .alias(m.getAlias())
                .zipcode(m.getZipcode())
                .address(m.getAddress())
                .addressDetail(m.getAddressDetail())
                .isDefault("Y".equals(m.getIsDefault()))
                .createdAt(m.getCreatedAt())
                .build();
    }
}
