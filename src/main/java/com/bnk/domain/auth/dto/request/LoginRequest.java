package com.bnk.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "이메일을 입력해주세요.")
    private String email;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;

    private String deviceInfo;  // USER_SESSIONS.device_info (선택)

    // ── 신뢰 기기 판정용 (클라이언트가 영구 보관하는 기기 식별자) ──────────────
    /** 클라이언트 기기 UUID. 서버는 SHA-256 해시로만 저장·비교한다. */
    @Size(max = 200)
    private String deviceId;

    /** 표시용 기기명 (예: iPhone 15 Pro, 갤럭시 S24). */
    @Size(max = 100)
    private String deviceName;

    /** 플랫폼 코드: IOS / ANDROID / WEB. */
    @Size(max = 20)
    private String platform;
}
