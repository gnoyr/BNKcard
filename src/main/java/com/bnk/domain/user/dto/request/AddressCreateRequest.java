package com.bnk.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** POST /api/users/me/addresses — 주소 등록 요청 */
@Getter
@NoArgsConstructor
public class AddressCreateRequest {

    @Size(max = 100, message = "별칭은 100자 이내로 입력해 주세요.")
    private String alias;

    @Size(max = 20)
    private String zipcode;

    @NotBlank(message = "주소를 입력해 주세요.")
    @Size(max = 300, message = "주소는 300자 이내로 입력해 주세요.")
    private String address;

    @Size(max = 300, message = "상세주소는 300자 이내로 입력해 주세요.")
    private String addressDetail;

    /** true 이면 기본 배송지로 지정 (등록된 주소가 하나도 없으면 자동 기본배송지) */
    private Boolean setDefault;
}
