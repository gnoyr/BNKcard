package com.bnk.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 주소 변경 → CI(연계정보) 갱신 요청 (본인인증 모달 결과).
 *
 * CI는 SHA-256(이름 + 주민번호앞6+성별코드 + 주소 + salt)로 재계산된다.
 * 주민번호 뒷자리(성별코드 제외)는 수집·저장하지 않는다.
 */
@Getter
@Setter
@NoArgsConstructor
public class UserCiUpdateRequest {

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    /** 주민등록번호 앞 6자리 (YYMMDD) */
    @Pattern(regexp = "\\d{6}", message = "주민등록번호 앞 6자리를 입력해 주세요.")
    private String residentFront;

    /** 주민등록번호 뒷자리 첫 번째(성별코드) */
    @Pattern(regexp = "[1-4789]", message = "성별코드가 올바르지 않습니다.")
    private String genderCode;

    @NotBlank(message = "주소는 필수입니다.")
    private String address;

    /** 상세 주소 (선택) — CI 입력에는 포함하지 않는다(가입 시와 동일). */
    private String addressDetail;
}
