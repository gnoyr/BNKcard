package com.bnk.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// 공통
	INVALID_INPUT("C001", "입력값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
	INTERNAL_ERROR("C002", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
	NOT_FOUND("C003", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	TOO_MANY_REQUESTS("C004", "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.TOO_MANY_REQUESTS),

	// 인증/인가
	UNAUTHORIZED("A001", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
	FORBIDDEN("A002", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
	TOKEN_EXPIRED("A003", "토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),
	TOKEN_INVALID("A004", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
	REFRESH_TOKEN_INVALID("A005", "Refresh Token이 유효하지 않거나 만료되었습니다.", HttpStatus.UNAUTHORIZED),

	// 회원
	USER_NOT_FOUND("U001", "가입 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	DUPLICATE_EMAIL("U002", "이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT),
	INVALID_PASSWORD("U003", "비밀번호가 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
	ACCOUNT_LOCKED("U004", "계정이 잠겼습니다. 잠시 후 다시 시도하세요.", HttpStatus.LOCKED),
	ACCOUNT_SUSPENDED("U005", "정지된 계정입니다.", HttpStatus.FORBIDDEN),
	ACCOUNT_WITHDRAWN("U006", "탈퇴한 계정입니다.", HttpStatus.FORBIDDEN),
	EMAIL_NOT_VERIFIED("U007", "이메일 인증이 필요합니다.", HttpStatus.FORBIDDEN),
	VERIFY_TOKEN_INVALID("U008", "인증 토큰이 유효하지 않거나 만료되었습니다.", HttpStatus.BAD_REQUEST),
	PASSWORD_CONFIRM_MISMATCH("U009", "새 비밀번호와 확인 비밀번호가 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
	DUPLICATE_PHONE("U010", "이미 사용 중인 휴대폰 번호입니다.", HttpStatus.CONFLICT),
	PASSWORD_RECENTLY_USED("U011", "최근 사용한 비밀번호는 재사용할 수 없습니다.", HttpStatus.BAD_REQUEST),
	WATCHLIST_BLOCKED("U012", "서비스 이용이 제한된 계정입니다. 고객센터에 문의해 주세요.", HttpStatus.FORBIDDEN),

	// 카드
	CARD_NOT_FOUND("CD001", "카드 상품을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	CARD_NOT_PUBLISHED("CD002", "조회할 수 없는 카드 상품입니다.", HttpStatus.NOT_FOUND),
	CARD_COMPARE_LIMIT("CD003", "비교는 최대 3개까지 가능합니다.", HttpStatus.BAD_REQUEST),
	DUPLICATE_CARD_CODE("CD004", "이미 존재하는 카드 코드입니다.", HttpStatus.CONFLICT),

	// 결재
	APPROVAL_NOT_FOUND("AP001", "결재 요청을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	APPROVAL_ALREADY_DONE("AP002", "이미 처리된 결재 요청입니다.", HttpStatus.CONFLICT),
	REJECT_COMMENT_REQUIRED("AP003", "반려 사유를 입력해주세요.", HttpStatus.BAD_REQUEST),

	// 약관
	TERMS_NOT_FOUND("T001", "약관을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	REQUIRED_TERMS_NOT_AGREED("T002", "필수 약관에 모두 동의해야 합니다.", HttpStatus.BAD_REQUEST),
	TERMS_PACKAGE_NOT_FOUND("T003", "약관 패키지를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

	// 검색
	DUPLICATE_KEYWORD("S001", "이미 등록된 키워드입니다.", HttpStatus.CONFLICT),

	// CDD
	CDD_NOT_VERIFIED("CDD001", "고객 확인(CDD)이 완료되지 않았습니다. 신원 확인 후 이용해 주세요.", HttpStatus.FORBIDDEN),
	CDD_REJECTED("CDD002", "고객 확인(CDD)이 거부되었습니다. 고객센터에 문의해 주세요.", HttpStatus.FORBIDDEN),

	// 관리자
	ADMIN_NOT_FOUND("AD001", "관리자 계정을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	ADMIN_ACCOUNT_LOCKED("AD002", "관리자 계정이 잠겼습니다.", HttpStatus.LOCKED),
	
	//IP 인증
	IP_CHALLENGE_EXPIRED      ("IP001", "인증 세션이 만료되었습니다. 다시 로그인해 주세요.",   HttpStatus.BAD_REQUEST),
    CI_MISMATCH               ("IP002", "본인 정보가 일치하지 않습니다.",                    HttpStatus.BAD_REQUEST),
    CI_LOCKED                 ("IP003", "CI 인증 실패 횟수를 초과하였습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.TOO_MANY_REQUESTS),
    IP_INITIAL_DELETE_FORBIDDEN("IP004","최초 등록 기기는 삭제할 수 없습니다.",               HttpStatus.BAD_REQUEST),
    IP_ALREADY_TRUSTED        ("IP005", "이미 등록된 IP입니다.",                           HttpStatus.CONFLICT),
    IP_MAX_LIMIT_EXCEEDED     ("IP006", "신뢰 IP는 최대 10개까지 등록할 수 있습니다.",        HttpStatus.BAD_REQUEST);

	private final String code;
	private final String message;
	private final HttpStatus httpStatus;
}