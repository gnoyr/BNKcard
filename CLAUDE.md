# BNKcard 프로젝트

## 서버 구성
- BNKcard 서버: `http://192.168.50.30:8088` (Spring Boot, Oracle DB `bnkclone_medium`)
- MYDATAMOCK 서버: `http://192.168.50.30:8081` (심사/본인확인 서버)
- DB: Oracle Cloud (`Wallet_BNKCLONE_FIXED`), username=ADMIN
- `verification.server.url=http://192.168.50.30:8081` (application-local.properties)

## 주요 테이블 (bnkclone DB - ADMIN 스키마)
- `USERS` — 회원
- `ACCOUNTS` — 계좌 (체크카드 연결)
- `CREDIT_CARD_APPLICATIONS` — 신용카드 신청 (SEQ_CREDIT_CARD_APPLICATIONS)
- `CHECK_CARD_APPLICATIONS` — 체크카드 신청 (SEQ_CHECK_CARD_APPLICATIONS)
- `USER_CARDS` — 발급된 카드
- `CARD_TRANSACTIONS` — 카드 거래내역
- `CARD_TERMS` / `TERMS_MASTERS` — 약관
- `AUDIT_LOGS`, `EVENT_LOGS` — 로그

## 카드 신청 상태 흐름
```
DRAFT → REQUESTED → SCREENING_FAILED(재시도가능)
                  → REVIEWING → APPROVED → ISSUED
                              → REJECTED
                  → APPROVED → ISSUED
                  → REJECTED
```

### 신용카드 신청 API (`/api/applications/credit`)
| 순서 | 엔드포인트 | 설명 |
|------|-----------|------|
| 1 | POST /agree-terms | 약관동의, creditAppId 반환 |
| 2 | POST /verify-identity | 본인확인 (MYDATAMOCK CI 대조) |
| 3 | POST /applicant-info | 기본정보+직업/소득 저장 |
| 4 | GET /existing-customer | 기존고객 여부 확인 |
| 4-1 | POST /docs | 서류 업로드 (신규고객만) |
| 4-2 | POST /submit | 신청완료 → REQUESTED, 심사요청 자동발송 |
| 5 | POST /screening-result | 1차 심사 결과 수신 (심사서버 콜백) |
| 6 | POST /review-result | 추가심사 결과 수신 (REVIEWING 케이스) |

### 체크카드 신청 API (`/api/applications/check`)
| 순서 | 엔드포인트 | 설명 |
|------|-----------|------|
| 1 | POST /agree-terms | 약관동의, checkAppId 반환 |
| 2 | POST /verify-identity | 본인확인 |
| 3 | POST /applicant-info | 기본정보 저장 |
| 4 | POST /submit | 신청완료 → 심사요청 자동발송 |
| 5 | POST /screening-result | 심사결과 수신 → 바로 APPROVED/REJECTED |

## 신용 vs 체크카드 차이
| 항목 | 신용카드 | 체크카드 |
|------|---------|---------|
| 서류 업로드 | 신규고객 필요 | 없음 |
| 심사 단계 | 1차심사 + 추가심사(REVIEWING) 가능 | 1차심사로 바로 결정 |
| 한도 기준 | 신용점수+소득+연체율 복합 | 연결계좌 잔액 기반 |

## CI값 관련 주의
- BNKcard ↔ MYDATAMOCK 간 CI 생성 로직이 **반드시 동일**해야 함
- CI값 불일치 시 본인확인 실패 (`MYDATA_IDENTITY_MASTER`에서 매칭 안 됨)
- CI salt: `application-local.properties`의 `ci.salt` 값

## 콜백 인증
- 심사서버 → BNKcard 콜백 시 `X-Internal-Secret` 헤더 필요
- `internal.callback.secret` 값이 양쪽 서버에서 동일해야 함
