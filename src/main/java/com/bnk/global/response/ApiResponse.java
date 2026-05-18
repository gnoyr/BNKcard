package com.bnk.global.response;

import com.bnk.global.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String code;
    private final String message;
    private final T data;

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true).code("SUCCESS").message("요청이 성공했습니다.").data(data).build();
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true).code("SUCCESS").message(message).data(data).build();
    }

    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>builder()
                .success(true).code("CREATED").message("생성되었습니다.").data(data).build();
    }

    public static ApiResponse<Void> message(String message) {
        return ApiResponse.<Void>builder()
                .success(true).code("SUCCESS").message(message).build();
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return ApiResponse.<T>builder()
                .success(false).code(errorCode.getCode()).message(errorCode.getMessage()).build();
    }

    // ── ResponseEntity 헬퍼 ───────────────────────────────
    public static <T> ResponseEntity<ApiResponse<T>> toOk(T data) {
        return ResponseEntity.ok(ok(data));
    }

    public static <T> ResponseEntity<ApiResponse<T>> toCreated(T data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(created(data));
    }

    public static ResponseEntity<ApiResponse<Void>> toNoContent() {
        return ResponseEntity.noContent().build();
    }
}
