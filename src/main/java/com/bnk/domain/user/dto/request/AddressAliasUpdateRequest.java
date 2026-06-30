package com.bnk.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** PATCH /api/users/me/addresses/{addressId} — 별칭 수정 */
@Getter
@NoArgsConstructor
public class AddressAliasUpdateRequest {

    @NotBlank(message = "별칭을 입력해 주세요.")
    @Size(max = 100, message = "별칭은 100자 이내로 입력해 주세요.")
    private String alias;
}
