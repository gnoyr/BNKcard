package com.bnk.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 전역 예외 처리.
 * jakarta.validation (@Valid) — spring-boot-starter-validation 의존성 필요.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 비즈니스 예외 ──────────────────────────────────────
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        log.warn("BusinessException: code={}, message={}", ex.getErrorCode().getCode(), ex.getMessage());
        ErrorResponse body = ex.getDetail() != null
                ? ErrorResponse.of(ex.getErrorCode(), ex.getDetail())
                : ErrorResponse.of(ex.getErrorCode());
        return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(body);
    }

    // ── @Valid / @Validated 검증 실패 ─────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> ErrorResponse.FieldError.of(
                        e.getField(),
                        e.getRejectedValue() == null ? "" : e.getRejectedValue().toString(),
                        e.getDefaultMessage()
                ))
                .collect(Collectors.toList());

        log.warn("Validation 실패: {}", fieldErrors);
        return ResponseEntity.badRequest().body(ErrorResponse.ofValidation(fieldErrors));
    }

    // ── JSON 파싱 실패 ────────────────────────────────────
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("요청 바디 파싱 실패: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, "요청 본문을 읽을 수 없습니다."));
    }

    // ── 파라미터 타입 불일치 ───────────────────────────────
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT,
                        String.format("파라미터 '%s' 값이 올바르지 않습니다.", ex.getName())));
    }

    // ── 필수 파라미터 누락 ─────────────────────────────────
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT,
                        String.format("필수 파라미터 '%s'가 없습니다.", ex.getParameterName())));
    }

    // ── 정적 리소스 없음 (favicon.ico 등) — 404, ERROR 로그 제외 ──
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResource(NoResourceFoundException ex) {
        log.debug("정적 리소스 없음: {}", ex.getMessage());
        return ResponseEntity.notFound().build();
    }

    // ── 500 폴백 ──────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("예상치 못한 오류 발생", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
    }
}