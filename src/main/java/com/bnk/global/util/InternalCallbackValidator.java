package com.bnk.global.util;

import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InternalCallbackValidator {

    @Value("${internal.callback.secret}")
    private String callbackSecret;

    public void validate(HttpServletRequest request) {
        String header = request.getHeader("X-Internal-Secret");
        if (header == null || !callbackSecret.equals(header)) {
            log.warn("[InternalCallback] 비인가 콜백 요청 차단: ip={}", request.getRemoteAddr());
            throw new BusinessException(ErrorCode.CALLBACK_AUTH_FAILED);
        }
    }
}
