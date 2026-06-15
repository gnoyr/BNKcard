package com.bnk.global.exception;

import com.bnk.global.util.audit.AuditLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 전역 예외 핸들러.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor  // auditLogger DI를 위해 추가
public class GlobalExceptionHandler {

    private final AuditLogger auditLogger;

    // ── 비즈니스 예외 ─────────────────────────────────────────────────
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        log.warn("BusinessException: code={}, message={}", ex.getErrorCode().getCode(), ex.getMessage());
        ErrorResponse body = ex.getDetail() != null
                ? ErrorResponse.of(ex.getErrorCode(), ex.getDetail())
                : ErrorResponse.of(ex.getErrorCode());
        return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(body);
    }

    // ── @Valid / @Validated 검증 실패 ──────────────────────────────────
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

        log.warn("Validation 실패: {}", fieldErrors.stream()
                .map(fe -> fe.getField() + "=" + fe.getValue() + " (" + fe.getReason() + ")")
                .collect(Collectors.joining(", ")));
        return ResponseEntity.badRequest().body(ErrorResponse.ofValidation(fieldErrors));
    }

    // ── JSON 파싱 실패 ────────────────────────────────────────────────
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("요청 바디 파싱 실패: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, "요청 본문을 읽을 수 없습니다."));
    }

    // ── 파라미터 타입 불일치 ───────────────────────────────────────────
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT,
                        String.format("파라미터 '%s' 값이 올바르지 않습니다.", ex.getName())));
    }

    // ── 필수 파라미터 누락 ────────────────────────────────────────────
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT,
                        String.format("필수 파라미터 '%s'가 없습니다.", ex.getParameterName())));
    }

    // ── 정적 리소스 없음 ──────────────────────────────────────────────
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResource(NoResourceFoundException ex) {
        log.debug("정적 리소스 없음: {}", ex.getMessage());
        return ResponseEntity.notFound().build();
    }

    // ── 파일 크기 초과 ────────────────────────────────────────────────
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("파일 크기 초과: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT,
                        "허용된 파일 크기를 초과했습니다. 최대 20MB까지 업로드 가능합니다."));
    }

    // ── 쿠키 누락 ────────────────────────────────────────────────────
    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ErrorResponse> handleMissingCookie(MissingRequestCookieException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT,
                        "쿠키가 누락되었습니다: " + ex.getCookieName()));
    }

    // ── 500 폴백 ─────────────────────────────────────────────────────
    /**
     * 예상치 못한 RuntimeException / Error 전체를 잡는 최후 핸들러.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        String path = request.getRequestURI();
        String ip   = request.getRemoteAddr();

        log.error("예상치 못한 오류 발생: method={} path={} ip={}",
                request.getMethod(), path, ip, ex);

        auditLogger.failure(
                AuditLogger.AUTH,
                AuditLogger.SYSTEM_ERROR,   // ← LOGIN → SYSTEM_ERROR
                null,
                ip,
                "500 예상치 못한 오류: " + request.getMethod() + " " + path
                        + " | " + ex.getClass().getSimpleName() + ": " + ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
    }
}