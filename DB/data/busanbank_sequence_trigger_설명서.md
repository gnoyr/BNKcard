# BNK 부산은행 금융 상품 플랫폼
## 시퀀스(SEQUENCE) · 트리거(TRIGGER) · 함수(FUNCTION) 설명서
> 작성일   : 2026-05-20
> DB       : Oracle 21c
> 참조 DDL : busanbank_ddl.sql

**[문서 구성]**
  1. 개요 및 채번 전략
  2. FUNCTION — FN_GEN_CARD_ID
  3. SEQUENCE 목록 및 상세 설명
     3-1. 일반 테이블 시퀀스 (45개)
     3-2. 카드 PK 전용 시퀀스 (4개)
  4. TRIGGER 목록 및 상세 설명
     4-1. BEFORE INSERT 자동 채번 트리거 (45개)
     4-2. 카드 PK 전용 BEFORE INSERT 트리거 (1개)
     4-3. BEFORE UPDATE 자동 갱신 트리거 (2개)
  5. 시퀀스 운영 가이드
     5-1. 더미 데이터 삽입 후 시퀀스 초기화 방법
     5-2. 카드 PK 시퀀스 관리 주의사항
  6. 트리거 운영 가이드
  7. 전체 목록 요약표


## 1. 개요 및 채번 전략


**■ 목적**
  Oracle DB 환경에서 AUTO_INCREMENT를 지원하지 않기 때문에
  SEQUENCE + TRIGGER 조합으로 PK 자동 채번을 구현한다.
  애플리케이션(Spring Boot + MyBatis)에서 별도 채번 로직 없이
  INSERT 만 수행하면 PK가 자동 부여된다.


**■ 전체 구성**
```
  ┌──────────────────────────────────────────────────────────┐
  │  일반 테이블 : SEQUENCE (45개) + BEFORE INSERT TRIGGER   │
  │  카드 상품   : SEQUENCE (4개)  + FUNCTION + TRIGGER (특수)│
  │  자동 갱신   : BEFORE UPDATE TRIGGER (2개)               │
  └──────────────────────────────────────────────────────────┘
```


**■ 채번 원칙**
  · WHEN (NEW.pk IS NULL) 조건 적용
    → 애플리케이션이 PK를 직접 명시한 경우 트리거 우회
    → 더미/마이그레이션 INSERT 시 명시적 PK 삽입 가능
    → 일반 INSERT(PK 미지정)만 시퀀스 자동 채번


**■ CARDS 테이블 특수 채번**
  · card_id = [카드 대분류(3자리)][카드사코드(2자리)][일련번호(3자리)]
  · 예) 신용카드 / BNK부산은행 / 1번째 = 10101001
  · FN_GEN_CARD_ID() 함수 → TRG_CARDS_BI 트리거에서 호출


## 2. FUNCTION — FN_GEN_CARD_ID


| 항목 | 내용 |
|------|------|
| 함수명 | `FN_GEN_CARD_ID` |
| 반환 타입 | `NUMBER` |
| 목적 | CARDS 테이블 card_id PK 구조화 자동 생성 |


**■ 입력 파라미터**
```
  ┌───────────────┬──────────────┬──────────────────────────────┐
  │ 파라미터명    │ 타입         │ 설명                         │
  ├───────────────┼──────────────┼──────────────────────────────┤
  │ p_card_type   │ VARCHAR2     │ CREDIT / CHECK / PREPAID /   │
  │               │              │ HYBRID                       │
  │ p_company_code│ NUMBER       │ 카드사 코드 (기본값=1)       │
  │               │              │ BNK부산은행 = 1              │
  └───────────────┴──────────────┴──────────────────────────────┘
```


**■ 카드 대분류 코드 매핑**
```
  ┌───────────┬──────────┬────────────────────────────────────┐
  │ card_type │ 대분류코드│ 사용 시퀀스                       │
  ├───────────┼──────────┼────────────────────────────────────┤
  │ CREDIT    │ 101      │ SEQ_CARD_SERIAL_CREDIT             │
  │ CHECK     │ 102      │ SEQ_CARD_SERIAL_CHECK              │
  │ PREPAID   │ 103      │ SEQ_CARD_SERIAL_PREPAID            │
  │ HYBRID    │ 104      │ SEQ_CARD_SERIAL_HYBRID             │
  └───────────┴──────────┴────────────────────────────────────┘
```


**■ 계산 공식**
  card_id = (대분류코드 × 100000) + (카드사코드 × 1000) + 일련번호

  예시:
    CREDIT / BNK(1) / 시퀀스 1번
      → 101 × 100000 + 1 × 1000 + 1 = 10101001

    CHECK / BNK(1) / 시퀀스 3번
      → 102 × 100000 + 1 × 1000 + 3 = 10201003

    PREPAID / BNK(1) / 시퀀스 1번
      → 103 × 100000 + 1 × 1000 + 1 = 10301001


**■ 예외 처리**
  지원하지 않는 card_type 입력 시:
  RAISE_APPLICATION_ERROR(-20001, 'FN_GEN_CARD_ID: 지원하지 않는 카드 유형 → ...')


**■ 호출 위치**
  TRG_CARDS_BI (BEFORE INSERT ON CARDS)
  → :NEW.card_id IS NULL 조건일 때만 호출


## 3. SEQUENCE 목록 및 상세 설명


**■ 공통 옵션**
  · START WITH 1       : 초기값 1
  · INCREMENT BY 1     : 1씩 증가
  · NOCACHE            : 캐시 미사용 (데이터 손실 방지)
  · NOCYCLE            : 최대값 도달 시 오류 발생 (재사용 금지)


### 3-1. 일반 테이블 시퀀스 (45개)


#### [공통 코드]
No.  시퀀스명                   대상 테이블           대상 컬럼
 1   SEQ_COMMON_CODES           COMMON_CODES          code_id
  · 공통 코드 상세 항목 PK 채번
  · COMMON_CODE_GROUPS.group_code는 VARCHAR2 PK이므로 시퀀스 불필요


#### [관리자]
 2   SEQ_ADMIN_USERS            ADMIN_USERS           admin_id
  · 관리자 계정 PK 채번
  · USERS 테이블과 별개의 독립 시퀀스 (관리자/일반회원 완전 분리)

 3   SEQ_ADMIN_ROLES            ADMIN_ROLES           role_id
  · 관리자 역할 정의 PK 채번
  · 예) SUPER_ADMIN, CARD_MANAGER, REVIEWER

 4   SEQ_ADMIN_PERMISSIONS      ADMIN_PERMISSIONS     permission_id
  · 관리자 권한 항목 PK 채번
  · 권한 코드(CARD_READ, CARD_WRITE 등) 등록 시 사용

 5   SEQ_ROLE_PERMISSIONS       ROLE_PERMISSIONS      role_permission_id
  · 역할-권한 매핑 PK 채번
  · 초기 데이터 39건 (3개 역할 × 다수 권한 조합)

 6   SEQ_ADMIN_USER_ROLES       ADMIN_USER_ROLES      admin_user_role_id
  · 관리자-역할 매핑 PK 채번


#### [회원 / 인증]
 7   SEQ_USERS                  USERS                 user_id
  · 일반 회원 PK 채번
  · 가입 시 AuthService → INSERT INTO USERS → TRG_USERS_BI 자동 채번

 8   SEQ_USER_SESSIONS          USER_SESSIONS         session_id
  · JWT Refresh Token 세션 레코드 PK 채번
  · 로그인 성공 시마다 신규 세션 생성

 9   SEQ_LOGIN_HISTORIES        LOGIN_HISTORIES       history_id
  · 로그인 시도 이력 PK 채번
  · 성공/실패 모두 INSERT → 이력 누적

10   SEQ_AUDIT_LOGS             AUDIT_LOGS            audit_log_id
  · 감사 로그 PK 채번
  · 회원정보 변경, 비밀번호 변경, 관리자 열람 등 이벤트 기록


#### [결재]
11   SEQ_APPROVAL_REQUESTS      APPROVAL_REQUESTS     approval_id
  · 관리자 결재 요청 PK 채번
  · 카드 게시 요청, 약관 게시 요청 등

12   SEQ_APPROVAL_LINES         APPROVAL_LINES        approval_line_id
  · 결재 라인(승인자 목록) PK 채번


#### [카드 상품]
     ※ CARDS.card_id는 별도 카드 PK 전용 시퀀스 사용 (3-2 참조)

13   SEQ_CARD_CATEGORIES        CARD_CATEGORIES       category_id
  · 카드 혜택 카테고리 PK 채번
  · 교통, 쇼핑, 식음료 등 23개 초기 데이터

14   SEQ_CARD_BENEFITS          CARD_BENEFITS         benefit_id
  · 카드별 혜택 상세 PK 채번
  · 카드 1건당 복수 혜택 등록 가능

15   SEQ_CARD_IMAGES            CARD_IMAGES           image_id
  · 카드 이미지 PK 채번
  · FRONT/BACK/THUMBNAIL/DETAIL 유형별 복수 등록

16   SEQ_CARD_CONTENTS          CARD_CONTENTS         content_id
  · 카드 상세 HTML 콘텐츠 PK 채번
  · INTRO/GUIDE/NOTICE/FAQ/EVENT 유형

17   SEQ_CARD_APPLICATIONS      CARD_APPLICATIONS     application_id
  · 카드 신청 레코드 PK 채번
  · 회원의 카드 신청 건마다 1건 생성

18   SEQ_CARD_ATTRIBUTE_DEFINITIONS
                                CARD_ATTRIBUTE_DEFINITIONS  attribute_id
  · 카드 속성 정의(메타) PK 채번

19   SEQ_CARD_ATTRIBUTE_VALUES  CARD_ATTRIBUTE_VALUES card_attribute_value_id
  · 카드별 속성 값 PK 채번

20   SEQ_CARD_TAGS              CARD_TAGS             tag_id
  · 카드 태그 PK 채번 (예: #여행, #무실적)

21   SEQ_CARD_TAG_MAP           CARD_TAG_MAP          card_tag_map_id
  · 카드-태그 매핑 PK 채번

22   SEQ_CARD_PROMOTIONS        CARD_PROMOTIONS       promotion_id
  · 카드 프로모션 PK 채번

23   SEQ_CARD_STATUS_HISTORIES  CARD_STATUS_HISTORIES history_id
  · 카드 상태 변경 이력 PK 채번
  · DRAFT → REVIEW → APPROVED → PUBLISHED 등 변경 시마다 INSERT

24   SEQ_CARD_VERSIONS          CARD_VERSIONS         version_id
  · 카드 버전 스냅샷 PK 채번
  · 결재 요청 시 snapshot_json과 함께 INSERT

25   SEQ_USER_CARDS             USER_CARDS            user_card_id
  · 실제 발급 카드 PK 채번
  · 심사 승인 후 발급 처리 시 INSERT

26   SEQ_MERCHANT_CATEGORY_MAP  MERCHANT_CATEGORY_MAP map_id
  · 가맹점명-카테고리 Rule-based 매핑 PK 채번


#### [약관]
27   SEQ_TERMS_GROUPS           TERMS_GROUPS          group_id
  · 약관 그룹(필수/선택/공지) PK 채번

28   SEQ_TERMS_PACKAGES         TERMS_PACKAGES        package_id
  · 약관 패키지(회원가입용, 카드신청용 등) PK 채번

29   SEQ_PACKAGE_TERMS          PACKAGE_TERMS         package_terms_id
  · 패키지-약관 연결 레코드 PK 채번

30   SEQ_TERMS_MASTERS          TERMS_MASTERS         terms_master_id
  · 약관 원본 정의 PK 채번 (버전 공통 메타)

31   SEQ_TERMS                  TERMS                 terms_id
  · 약관 버전 레코드 PK 채번
  · 동일 약관이라도 버전이 다르면 신규 레코드

32   SEQ_TERMS_STATUS_HISTORY   TERMS_STATUS_HISTORY  history_id
  · 약관 상태 변경 이력 PK 채번

33   SEQ_TERMS_EXPOSURE_RULES   TERMS_EXPOSURE_RULES  rule_id
  · 약관 노출 조건 규칙 PK 채번

34   SEQ_TERMS_FILES            TERMS_FILES           file_id
  · 약관 첨부파일(PDF 등) PK 채번

35   SEQ_CARD_TERMS             CARD_TERMS            card_terms_id
  · 카드-약관 연결 레코드 PK 채번
  · 초기 데이터 113건 (카드 27종 × 약관 다수)

36   SEQ_USER_TERMS_AGREEMENTS  USER_TERMS_AGREEMENTS agreement_id
  · 사용자 약관 동의 이력 PK 채번
  · 동의/철회/재동의 모두 신규 레코드

37   SEQ_CARD_NOTICE_SECTIONS   CARD_NOTICE_SECTIONS  notice_id
  · 카드 상품 안내/유의사항 섹션 PK 채번

38   SEQ_TERMS_CHANGE_DIFFS     TERMS_CHANGE_DIFFS    diff_id
  · 약관 변경 비교 레코드 PK 채번

39   SEQ_TERMS_NOTIFICATION_HISTORY
                                TERMS_NOTIFICATION_HISTORY  notification_id
  · 약관 재동의 알림 발송 이력 PK 채번


#### [파일 관리]
40   SEQ_FILE_UPLOADS           FILE_UPLOADS          file_id
  · 파일 업로드 레코드 PK 채번


#### [검색]
41   SEQ_SEARCH_KEYWORDS        SEARCH_KEYWORDS       keyword_id
  · 관리자 등록 추천 검색 키워드 PK 채번

42   SEQ_CARD_KEYWORDS          CARD_KEYWORDS         card_keyword_id
  · 카드-검색 키워드 매핑 PK 채번

43   SEQ_SEARCH_LOGS            SEARCH_LOGS           search_log_id
  · 사용자 검색 로그 PK 채번


#### [소비패턴 / AI]
44   SEQ_USER_SPENDING_PATTERNS USER_SPENDING_PATTERNS  pattern_id
  · 사용자 카테고리별 월 소비패턴 PK 채번
  · MERGE INTO 방식(UPSERT)이지만 신규 INSERT 시 채번

45   SEQ_AI_CHAT_LOGS           AI_CHAT_LOGS          chat_id
  · 챗봇 대화 로그 PK 채번
  · 비로그인 사용자도 포함 (user_id NULL 허용)


### 3-2. 카드 PK 전용 시퀀스 (4개)

※ CARDS 테이블 card_id의 6~8번째 자리(일련번호) 생성 전용
※ START WITH 1 / MAXVALUE 999 / NOCYCLE
   → 카드사 1곳 기준 유형별 최대 999종 상품 지원

No.  시퀀스명                  대응 card_type  PK 자리수
46   SEQ_CARD_SERIAL_CREDIT    CREDIT (신용)   10101001 ~
  · 신용카드 일련번호 채번
  · 현재 초기 데이터 18건 삽입 (10101001 ~ 10101018)
  · 다음 발급 가능 번호: 10101019

47   SEQ_CARD_SERIAL_CHECK     CHECK (체크)    10201001 ~
  · 체크카드 일련번호 채번
  · 현재 초기 데이터 8건 삽입 (10201001 ~ 10201008)
  · 다음 발급 가능 번호: 10201009

48   SEQ_CARD_SERIAL_PREPAID   PREPAID (선불)  10301001 ~
  · 선불카드 일련번호 채번
  · 현재 초기 데이터 1건 삽입 (10301001)
  · 다음 발급 가능 번호: 10301002

49   SEQ_CARD_SERIAL_HYBRID    HYBRID (하이브리드) 10401001 ~
  · 하이브리드 카드 일련번호 채번
  · 현재 초기 데이터 없음
  · 다음 발급 가능 번호: 10401001


**■ 카드사 코드 확장 시 참고**
  현재 BNK부산은행 = company_code '01'만 사용
  신규 카드사 추가 예시:
    타행 = 02 → CREDIT/타행/001 = 101 × 100000 + 2 × 1000 + 1 = 10102001
  단, 시퀀스는 유형별 공유이므로 카드사별로 일련번호가 이어짐
  → 카드사 구분이 필요할 경우 시퀀스를 카드사별로 추가 분리 권장


## 4. TRIGGER 목록 및 상세 설명


**■ 명명 규칙**
  TRG_{테이블명 축약}_BI   : BEFORE INSERT (자동 채번)
  TRG_{테이블명 축약}_BU   : BEFORE UPDATE (자동 갱신)


**■ BEFORE INSERT 공통 구조**
```sql
  CREATE OR REPLACE TRIGGER TRG_xxx_BI
```
  BEFORE INSERT ON {테이블명}
  FOR EACH ROW
  WHEN (NEW.{pk컬럼} IS NULL)       ← 핵심 조건
  BEGIN
      :NEW.{pk컬럼} := {시퀀스명}.NEXTVAL;
  END;

  → WHEN 조건 덕분에 명시적 PK 삽입 시 트리거 무시됨


### 4-1. BEFORE INSERT 자동 채번 트리거 (45개)


#### [공통 코드]
No.  트리거명                 대상 테이블           연결 시퀀스
 1   TRG_COMMON_CODES_BI      COMMON_CODES          SEQ_COMMON_CODES


#### [관리자]
 2   TRG_ADMIN_USERS_BI       ADMIN_USERS           SEQ_ADMIN_USERS
 3   TRG_ADMIN_ROLES_BI       ADMIN_ROLES           SEQ_ADMIN_ROLES
 4   TRG_ADMIN_PERMISSIONS_BI ADMIN_PERMISSIONS     SEQ_ADMIN_PERMISSIONS
 5   TRG_ROLE_PERMISSIONS_BI  ROLE_PERMISSIONS      SEQ_ROLE_PERMISSIONS
 6   TRG_ADMIN_USER_ROLES_BI  ADMIN_USER_ROLES      SEQ_ADMIN_USER_ROLES


#### [회원 / 인증]
 7   TRG_USERS_BI             USERS                 SEQ_USERS
  · 회원가입 INSERT 시 user_id 자동 채번
  · Spring Security @AuthUser 어노테이션으로 추출하는 PK 기준

 8   TRG_USER_SESSIONS_BI     USER_SESSIONS         SEQ_USER_SESSIONS
  · 로그인 성공 시 세션 레코드 자동 채번

 9   TRG_LOGIN_HISTORIES_BI   LOGIN_HISTORIES       SEQ_LOGIN_HISTORIES
  · 로그인 시도(성공/실패 모두) 이력 자동 채번

10   TRG_AUDIT_LOGS_BI        AUDIT_LOGS            SEQ_AUDIT_LOGS
  · 감사 이벤트 로그 자동 채번
  · AOP 또는 Service 레이어에서 직접 INSERT

11   TRG_APPROVAL_REQUESTS_BI APPROVAL_REQUESTS     SEQ_APPROVAL_REQUESTS
12   TRG_APPROVAL_LINES_BI    APPROVAL_LINES        SEQ_APPROVAL_LINES


#### [카드 상품 — 일반 자식 테이블]
13   TRG_CARD_CATEGORIES_BI   CARD_CATEGORIES       SEQ_CARD_CATEGORIES
14   TRG_CARD_BENEFITS_BI     CARD_BENEFITS         SEQ_CARD_BENEFITS
15   TRG_CARD_IMAGES_BI       CARD_IMAGES           SEQ_CARD_IMAGES
16   TRG_CARD_CONTENTS_BI     CARD_CONTENTS         SEQ_CARD_CONTENTS
17   TRG_CARD_APPLICATIONS_BI CARD_APPLICATIONS     SEQ_CARD_APPLICATIONS
18   TRG_CARD_ATTR_DEF_BI     CARD_ATTRIBUTE_DEFINITIONS
                                                    SEQ_CARD_ATTRIBUTE_DEFINITIONS
19   TRG_CARD_ATTR_VAL_BI     CARD_ATTRIBUTE_VALUES SEQ_CARD_ATTRIBUTE_VALUES
20   TRG_CARD_TAGS_BI         CARD_TAGS             SEQ_CARD_TAGS
21   TRG_CARD_TAG_MAP_BI      CARD_TAG_MAP          SEQ_CARD_TAG_MAP
22   TRG_CARD_PROMOTIONS_BI   CARD_PROMOTIONS       SEQ_CARD_PROMOTIONS
23   TRG_CARD_STATUS_HIST_BI  CARD_STATUS_HISTORIES SEQ_CARD_STATUS_HISTORIES
24   TRG_CARD_VERSIONS_BI     CARD_VERSIONS         SEQ_CARD_VERSIONS
25   TRG_USER_CARDS_BI        USER_CARDS            SEQ_USER_CARDS
26   TRG_MERCHANT_CATEGORY_BI MERCHANT_CATEGORY_MAP SEQ_MERCHANT_CATEGORY_MAP


#### [약관]
27   TRG_TERMS_GROUPS_BI         TERMS_GROUPS         SEQ_TERMS_GROUPS
28   TRG_TERMS_PACKAGES_BI       TERMS_PACKAGES       SEQ_TERMS_PACKAGES
29   TRG_PACKAGE_TERMS_BI        PACKAGE_TERMS        SEQ_PACKAGE_TERMS
30   TRG_TERMS_MASTERS_BI        TERMS_MASTERS        SEQ_TERMS_MASTERS
31   TRG_TERMS_BI                TERMS                SEQ_TERMS
32   TRG_TERMS_STATUS_HIST_BI    TERMS_STATUS_HISTORY SEQ_TERMS_STATUS_HISTORY
33   TRG_TERMS_EXPOSURE_RULES_BI TERMS_EXPOSURE_RULES SEQ_TERMS_EXPOSURE_RULES
34   TRG_TERMS_FILES_BI          TERMS_FILES          SEQ_TERMS_FILES
35   TRG_CARD_TERMS_BI           CARD_TERMS           SEQ_CARD_TERMS
36   TRG_USER_TERMS_AGMT_BI      USER_TERMS_AGREEMENTS
                                                     SEQ_USER_TERMS_AGREEMENTS
37   TRG_CARD_NOTICE_BI          CARD_NOTICE_SECTIONS SEQ_CARD_NOTICE_SECTIONS
38   TRG_TERMS_CHANGE_DIFFS_BI   TERMS_CHANGE_DIFFS   SEQ_TERMS_CHANGE_DIFFS
39   TRG_TERMS_NOTIF_HIST_BI     TERMS_NOTIFICATION_HISTORY
                                                     SEQ_TERMS_NOTIFICATION_HISTORY

#### [파일]
40   TRG_FILE_UPLOADS_BI         FILE_UPLOADS         SEQ_FILE_UPLOADS


#### [검색]
41   TRG_SEARCH_KEYWORDS_BI      SEARCH_KEYWORDS      SEQ_SEARCH_KEYWORDS
42   TRG_CARD_KEYWORDS_BI        CARD_KEYWORDS        SEQ_CARD_KEYWORDS
43   TRG_SEARCH_LOGS_BI          SEARCH_LOGS          SEQ_SEARCH_LOGS


#### [소비패턴 / AI]
44   TRG_USER_SPENDING_BI        USER_SPENDING_PATTERNS
                                                     SEQ_USER_SPENDING_PATTERNS
45   TRG_AI_CHAT_LOGS_BI         AI_CHAT_LOGS         SEQ_AI_CHAT_LOGS


### 4-2. 카드 PK 전용 BEFORE INSERT 트리거 (1개)


**■ TRG_CARDS_BI**
  대상 테이블 : CARDS
  타이밍      : BEFORE INSERT / FOR EACH ROW
  조건        : WHEN (NEW.card_id IS NULL)

  동작 흐름:
    1) INSERT INTO CARDS (...) VALUES (...) 실행
    2) card_id 값이 NULL인 경우만 트리거 발동
    3) FN_GEN_CARD_ID(:NEW.card_type, NVL(TO_NUMBER(:NEW.company_code), 1)) 호출
    4) 내부에서 card_type에 맞는 일련번호 시퀀스 NEXTVAL 획득
    5) [대분류코드 × 100000] + [카드사코드 × 1000] + [일련번호] 계산
    6) 결과값을 :NEW.card_id에 할당 후 INSERT 완료

  주의 사항:
  · card_type이 반드시 INSERT보다 먼저 설정되어야 함
  · company_code 기본값은 1 (BNK부산은행)
  · 잘못된 card_type 입력 시 ORA-20001 예외 발생 → 롤백
  · 더미 데이터 삽입 시 card_id 직접 명시 → 트리거 우회

  카드 등록 시 MyBatis 예시 (card_id 미지정):
```sql
    INSERT INTO CARDS (card_code, card_type, company_code, card_name, ...)
    VALUES (#{cardCode}, #{cardType}, #{companyCode}, #{cardName}, ...)
```
    → TRG_CARDS_BI 자동 실행 → card_id 자동 부여


### 4-3. BEFORE UPDATE 자동 갱신 트리거 (2개)


**■ TRG_CARDS_BU**
  대상 테이블 : CARDS
  타이밍      : BEFORE UPDATE / FOR EACH ROW
  조건        : (없음 — 모든 UPDATE에 발동)

  동작:
  · UPDATE 발생 시 :NEW.updated_at := SYSTIMESTAMP 자동 설정
  · 애플리케이션에서 updated_at 값을 별도로 넘길 필요 없음
  · 카드 정보 수정 API (PUT /api/admin/cards/{cardId}) 호출 시
      별도 SYSTIMESTAMP 지정 없이 자동 갱신됨


**■ TRG_USERS_BU**
  대상 테이블 : USERS
  타이밍      : BEFORE UPDATE / FOR EACH ROW
  조건        : (없음 — 모든 UPDATE에 발동)

  동작:
  · UPDATE 발생 시 :NEW.updated_at := SYSTIMESTAMP 자동 설정
  · 내 정보 수정 (PUT /api/users/me) 호출 시 자동 갱신
  · 비밀번호 변경, 상태 코드 변경 등 모든 UPDATE에 적용

  설계 의도:
  · USERS는 업데이트 빈도가 높고 updated_at 누락 리스크가 큼
  · 트리거로 강제하여 애플리케이션 버그로 인한 누락 방지


## 5. 시퀀스 운영 가이드


### 5-1. 더미 데이터 삽입 후 시퀀스 초기화 방법


**■ 문제 상황**
  busanbank_insert_all.sql 실행 시 명시적 PK 값으로 INSERT함
  → 트리거가 우회됨 (WHEN(NEW.pk IS NULL) 조건)
  → 시퀀스 현재값은 여전히 1에 머물러 있음
  → 이후 애플리케이션에서 시퀀스.NEXTVAL 사용 시
    이미 존재하는 PK와 충돌 → ORA-00001 UNIQUE CONSTRAINT 오류 발생


**■ 해결 방법 (Oracle ALTER SEQUENCE INCREMENT 방식)**
  Oracle에서 시퀀스를 특정 값으로 직접 변경하려면
  INCREMENT를 조정하여 한 번 NEXTVAL을 소진하는 방법 사용

```sql
  -- 예: TERMS_MASTERS에 125까지 명시적 삽입 → 다음 terms_master_id는 126
  ALTER SEQUENCE SEQ_TERMS_MASTERS INCREMENT BY 125;
  SELECT SEQ_TERMS_MASTERS.NEXTVAL FROM DUAL;   -- 이 시점에 126으로 점프
  ALTER SEQUENCE SEQ_TERMS_MASTERS INCREMENT BY 1;   -- 다시 1씩 증가로 복원
```


**■ 적용 대상 확인**
  busanbank_insert_all.sql 실행 방식을 반드시 먼저 확인:
  · PK 미지정 INSERT (일반 테이블) → 시퀀스 자동 소비 → 재설정 불필요
  · PK 명시적 INSERT (TERMS_MASTERS, TERMS) → 시퀀스 미소비 → 재설정 필요

  상세 현황은 상단 "초기 데이터 기준 시퀀스 현황 요약" 표 참조

  실행 스크립트는 busanbank_insert_all.sql 하단
  "SEQUENCE RESET" 섹션에 카드 시리얼 시퀀스 재설정 명령 포함


**■ 초기 데이터 기준 시퀀스 현황 요약**
  busanbank_insert_all.sql 실행 방식에 따라 두 가지 케이스로 구분됩니다.

  [케이스 A] 일반 테이블 — PK 미지정 INSERT (트리거가 시퀀스 소비)
    PK 컬럼이 INSERT 컬럼 목록에 없으므로 TRG_xxx_BI 발동 시 시퀀스 NEXTVAL 소비됨
    → insert_all.sql 실행 완료 후 시퀀스는 이미 올바른 다음 값을 가리킴
    → 별도 시퀀스 재설정 불필요

```
  ┌────────────────────────────────────┬────────┬────────────────┐
  │ 시퀀스명                           │ INSERT │ 다음 NEXTVAL   │
  ├────────────────────────────────────┼────────┼────────────────┤
  │ SEQ_COMMON_CODES                   │     43 │             44 │
  │ SEQ_ADMIN_USERS                    │     10 │             11 │
  │ SEQ_ADMIN_ROLES                    │      3 │              4 │
  │ SEQ_ADMIN_PERMISSIONS              │     20 │             21 │
  │ SEQ_ROLE_PERMISSIONS               │     39 │             40 │
  │ SEQ_ADMIN_USER_ROLES               │     10 │             11 │
  │ SEQ_USERS                          │     20 │             21 │
  │ SEQ_USER_SESSIONS                  │      5 │              6 │
  │ SEQ_LOGIN_HISTORIES                │     15 │             16 │
  │ SEQ_AUDIT_LOGS                     │     10 │             11 │
  │ SEQ_APPROVAL_REQUESTS              │      3 │              4 │
  │ SEQ_APPROVAL_LINES                 │      5 │              6 │
  │ SEQ_CARD_CATEGORIES                │     23 │             24 │
  │ SEQ_CARD_BENEFITS                  │    129 │            130 │
  │ SEQ_CARD_IMAGES                    │      0 │              1 │
  │ SEQ_CARD_CONTENTS                  │     54 │             55 │
  │ SEQ_CARD_APPLICATIONS              │      0 │              1 │
  │ SEQ_CARD_ATTRIBUTE_DEFINITIONS     │     10 │             11 │
  │ SEQ_CARD_ATTRIBUTE_VALUES          │    260 │            261 │
  │ SEQ_CARD_TAGS                      │     25 │             26 │
  │ SEQ_CARD_TAG_MAP                   │    116 │            117 │
  │ SEQ_CARD_PROMOTIONS                │      0 │              1 │
  │ SEQ_CARD_STATUS_HISTORIES          │     27 │             28 │
  │ SEQ_CARD_VERSIONS                  │      0 │              1 │
  │ SEQ_USER_CARDS                     │      0 │              1 │
  │ SEQ_MERCHANT_CATEGORY_MAP          │      0 │              1 │
  │ SEQ_TERMS_GROUPS                   │      4 │              5 │
  │ SEQ_TERMS_PACKAGES                 │      2 │              3 │
  │ SEQ_PACKAGE_TERMS                  │      2 │              3 │
  │ SEQ_TERMS_STATUS_HISTORY           │     36 │             37 │
  │ SEQ_TERMS_EXPOSURE_RULES           │      0 │              1 │
  │ SEQ_TERMS_FILES                    │      0 │              1 │
  │ SEQ_CARD_TERMS                     │    113 │            114 │
  │ SEQ_USER_TERMS_AGREEMENTS          │      0 │              1 │
  │ SEQ_CARD_NOTICE_SECTIONS           │      0 │              1 │
  │ SEQ_TERMS_CHANGE_DIFFS             │      0 │              1 │
  │ SEQ_TERMS_NOTIFICATION_HISTORY     │      0 │              1 │
  │ SEQ_FILE_UPLOADS                   │      0 │              1 │
  │ SEQ_SEARCH_KEYWORDS                │     30 │             31 │
  │ SEQ_CARD_KEYWORDS                  │     62 │             63 │
  │ SEQ_SEARCH_LOGS                    │     15 │             16 │
  │ SEQ_USER_SPENDING_PATTERNS         │     45 │             46 │
  │ SEQ_AI_CHAT_LOGS                   │     10 │             11 │
  └────────────────────────────────────┴────────┴────────────────┘
```

  [케이스 B] TERMS 관련 — PK 명시적 INSERT (트리거 우회 → 시퀀스 미소비)
    TERMS_MASTERS, TERMS는 terms_master_id/terms_id를 INSERT 컬럼에 직접 포함
    → 트리거 우회(WHEN pk IS NULL 조건 미충족) → 시퀀스는 여전히 1 상태
    → 반드시 아래 명령으로 재설정 필요

```
  ┌────────────────────────────────────┬────────┬────────────────┐
  │ 시퀀스명                           │ MAX PK │ 재설정값(next) │
  ├────────────────────────────────────┼────────┼────────────────┤
  │ SEQ_TERMS_MASTERS                  │    125 │           126  │
  │ SEQ_TERMS                          │    125 │           126  │
  └────────────────────────────────────┴────────┴────────────────┘
```
  ※ DDL에서 두 시퀀스 모두 START WITH 126으로 이미 반영되어 있음
    (DROP → CREATE 재실행 시 자동 정합성 유지)


### 5-2. 카드 PK 시퀀스 관리 주의사항


**■ MAXVALUE=999 제한**
  SEQ_CARD_SERIAL_CREDIT/CHECK/PREPAID/HYBRID 모두
  MAXVALUE 999, NOCYCLE 설정
  → 유형별 카드 999종 초과 시 ORA-08004 시퀀스 최대값 초과 오류
  → 999종 초과가 예상될 경우 DDL에서 MAXVALUE 999999로 변경 필요


**■ NOCYCLE 의도**
  카드 PK는 기존 카드 ID와 충돌 가능성이 있으므로
  최대값 도달 시 오류 발생을 선택 (자동 재사용 방지)


**■ 시퀀스 복원 명령 (초기 데이터 기준)**
```sql
  -- SEQ_CARD_SERIAL_CREDIT: 18건 삽입 완료
  ALTER SEQUENCE SEQ_CARD_SERIAL_CREDIT  INCREMENT BY 18;
  SELECT SEQ_CARD_SERIAL_CREDIT.NEXTVAL  FROM DUAL;
  ALTER SEQUENCE SEQ_CARD_SERIAL_CREDIT  INCREMENT BY 1;
```

```sql
  -- SEQ_CARD_SERIAL_CHECK: 8건 삽입 완료
  ALTER SEQUENCE SEQ_CARD_SERIAL_CHECK   INCREMENT BY 8;
  SELECT SEQ_CARD_SERIAL_CHECK.NEXTVAL   FROM DUAL;
  ALTER SEQUENCE SEQ_CARD_SERIAL_CHECK   INCREMENT BY 1;
```

```sql
  -- SEQ_CARD_SERIAL_PREPAID: 1건 삽입 완료
  ALTER SEQUENCE SEQ_CARD_SERIAL_PREPAID INCREMENT BY 1;
  SELECT SEQ_CARD_SERIAL_PREPAID.NEXTVAL FROM DUAL;
  ALTER SEQUENCE SEQ_CARD_SERIAL_PREPAID INCREMENT BY 1;
```

```sql
  -- SEQ_CARD_SERIAL_HYBRID: 삽입 없음 (별도 초기화 불필요)
```


## 6. 트리거 운영 가이드


**■ 트리거 비활성화 / 재활성화**
```sql
  -- 특정 트리거 비활성화 (대용량 마이그레이션 등)
  ALTER TRIGGER TRG_CARDS_BI DISABLE;
```

```sql
  -- 특정 트리거 재활성화
  ALTER TRIGGER TRG_CARDS_BI ENABLE;
```

```sql
  -- 테이블 전체 트리거 비활성화
  ALTER TABLE CARDS DISABLE ALL TRIGGERS;
```

```sql
  -- 테이블 전체 트리거 재활성화
  ALTER TABLE CARDS ENABLE ALL TRIGGERS;
```


**■ 트리거 존재 여부 확인 쿼리**
```sql
  SELECT TRIGGER_NAME, TABLE_NAME, STATUS, TRIGGER_TYPE, TRIGGERING_EVENT
  FROM   USER_TRIGGERS
  WHERE  TABLE_NAME IN ('CARDS','USERS','USER_SESSIONS')
  ORDER BY TABLE_NAME, TRIGGER_NAME;
```


**■ 시퀀스 현재값 확인 쿼리**
```sql
  SELECT SEQUENCE_NAME, LAST_NUMBER, MIN_VALUE, MAX_VALUE,
```
         INCREMENT_BY, CYCLE_FLAG, CACHE_SIZE
  FROM   USER_SEQUENCES
  ORDER BY SEQUENCE_NAME;


**■ TRG_CARDS_BI 예외 발생 시 대응**
  ORA-20001: FN_GEN_CARD_ID: 지원하지 않는 카드 유형
  원인  : card_type 값이 CREDIT/CHECK/PREPAID/HYBRID 외 값
  조치  : INSERT 전 card_type 값 검증
         Java 레이어: CardType enum 사용 권장


**■ Spring Boot 연동 시 채번 흐름**

#### [일반 테이블 예시 — USERS]
  Controller → Service → UserMapper.insert(user)
  → MyBatis: INSERT INTO USERS (email, password_hash, ...) VALUES (...)
  → TRG_USERS_BI 발동: :NEW.user_id := SEQ_USERS.NEXTVAL
  → INSERT 완료 후 MyBatis useGeneratedKeys 또는
```sql
    SELECT SEQ_USERS.CURRVAL FROM DUAL로 채번값 회수
```


#### [카드 테이블 예시 — CARDS]
  Controller → Service → CardMapper.insertCard(card)
  → MyBatis: INSERT INTO CARDS (card_type, company_code, card_name, ...) VALUES (...)
  → TRG_CARDS_BI 발동: FN_GEN_CARD_ID('CREDIT', 1) 호출
  → SEQ_CARD_SERIAL_CREDIT.NEXTVAL → card_id 계산 → INSERT 완료

  MyBatis Mapper XML 예시:
```sql
    <insert id="insertCard" useGeneratedKeys="false">
        INSERT INTO CARDS
            (card_code, card_type, company_code, card_name, company_name, ...)
        VALUES
            (#{cardCode}, #{cardType}, #{companyCode}, #{cardName}, ...)
```
    </insert>
    <!-- card_id는 INSERT 후 별도 SELECT로 조회 또는 RETURNING 절 사용 -->

  채번 후 card_id 회수 방법:
```sql
    <selectKey keyProperty="cardId" resultType="long" order="AFTER">
        SELECT SEQ_CARD_SERIAL_CREDIT.CURRVAL * 0
```
             + FN_GEN_CARD_ID(#{cardType}, #{companyCode})
        FROM DUAL
    </selectKey>
```sql
    -- 또는 INSERT 후 SELECT card_id FROM CARDS WHERE card_code = #{cardCode}
```


## 7. 전체 목록 요약표


#### [FUNCTION]
함수명                  반환     목적
FN_GEN_CARD_ID          NUMBER   카드 PK 8자리 구조화 생성


#### [SEQUENCE 전체 49개]
No.  시퀀스명                          START  MAX    CYCLE
 1   SEQ_COMMON_CODES                  1      무제한  N
 2   SEQ_ADMIN_USERS                   1      무제한  N
 3   SEQ_ADMIN_ROLES                   1      무제한  N
 4   SEQ_ADMIN_PERMISSIONS             1      무제한  N
 5   SEQ_ROLE_PERMISSIONS              1      무제한  N
 6   SEQ_ADMIN_USER_ROLES              1      무제한  N
 7   SEQ_USERS                         1      무제한  N
 8   SEQ_USER_SESSIONS                 1      무제한  N
 9   SEQ_LOGIN_HISTORIES               1      무제한  N
10   SEQ_AUDIT_LOGS                    1      무제한  N
11   SEQ_APPROVAL_REQUESTS             1      무제한  N
12   SEQ_APPROVAL_LINES                1      무제한  N
13   SEQ_CARD_CATEGORIES               1      무제한  N
14   SEQ_CARD_BENEFITS                 1      무제한  N
15   SEQ_CARD_IMAGES                   1      무제한  N
16   SEQ_CARD_CONTENTS                 1      무제한  N
17   SEQ_CARD_APPLICATIONS             1      무제한  N
18   SEQ_CARD_ATTRIBUTE_DEFINITIONS    1      무제한  N
19   SEQ_CARD_ATTRIBUTE_VALUES         1      무제한  N
20   SEQ_CARD_TAGS                     1      무제한  N
21   SEQ_CARD_TAG_MAP                  1      무제한  N
22   SEQ_CARD_PROMOTIONS               1      무제한  N
23   SEQ_CARD_STATUS_HISTORIES         1      무제한  N
24   SEQ_CARD_VERSIONS                 1      무제한  N
25   SEQ_USER_CARDS                    1      무제한  N
26   SEQ_MERCHANT_CATEGORY_MAP         1      무제한  N
27   SEQ_TERMS_GROUPS                  1      무제한  N
28   SEQ_TERMS_PACKAGES                1      무제한  N
29   SEQ_PACKAGE_TERMS                 1      무제한  N
30   SEQ_TERMS_MASTERS               126      무제한  N
31   SEQ_TERMS                        126      무제한  N
32   SEQ_TERMS_STATUS_HISTORY          1      무제한  N
33   SEQ_TERMS_EXPOSURE_RULES          1      무제한  N
34   SEQ_TERMS_FILES                   1      무제한  N
35   SEQ_CARD_TERMS                    1      무제한  N
36   SEQ_USER_TERMS_AGREEMENTS         1      무제한  N
37   SEQ_CARD_NOTICE_SECTIONS          1      무제한  N
38   SEQ_TERMS_CHANGE_DIFFS            1      무제한  N
39   SEQ_TERMS_NOTIFICATION_HISTORY    1      무제한  N
40   SEQ_FILE_UPLOADS                  1      무제한  N
41   SEQ_SEARCH_KEYWORDS               1      무제한  N
42   SEQ_CARD_KEYWORDS                 1      무제한  N
43   SEQ_SEARCH_LOGS                   1      무제한  N
44   SEQ_USER_SPENDING_PATTERNS        1      무제한  N
45   SEQ_AI_CHAT_LOGS                  1      무제한  N
46   SEQ_CARD_SERIAL_CREDIT            1      999     N  ★ 카드PK용
47   SEQ_CARD_SERIAL_CHECK             1      999     N  ★ 카드PK용
48   SEQ_CARD_SERIAL_PREPAID           1      999     N  ★ 카드PK용
49   SEQ_CARD_SERIAL_HYBRID            1      999     N  ★ 카드PK용


#### [TRIGGER 전체 48개]
No.  트리거명                      타입  대상 테이블             비고
 1   TRG_COMMON_CODES_BI           BI    COMMON_CODES
 2   TRG_ADMIN_USERS_BI            BI    ADMIN_USERS
 3   TRG_ADMIN_ROLES_BI            BI    ADMIN_ROLES
 4   TRG_ADMIN_PERMISSIONS_BI      BI    ADMIN_PERMISSIONS
 5   TRG_ROLE_PERMISSIONS_BI       BI    ROLE_PERMISSIONS
 6   TRG_ADMIN_USER_ROLES_BI       BI    ADMIN_USER_ROLES
 7   TRG_USERS_BI                  BI    USERS
 8   TRG_USER_SESSIONS_BI          BI    USER_SESSIONS
 9   TRG_LOGIN_HISTORIES_BI        BI    LOGIN_HISTORIES
10   TRG_AUDIT_LOGS_BI             BI    AUDIT_LOGS
11   TRG_APPROVAL_REQUESTS_BI      BI    APPROVAL_REQUESTS
12   TRG_APPROVAL_LINES_BI         BI    APPROVAL_LINES
13   TRG_CARD_CATEGORIES_BI        BI    CARD_CATEGORIES
14   TRG_CARD_BENEFITS_BI          BI    CARD_BENEFITS
15   TRG_CARD_IMAGES_BI            BI    CARD_IMAGES
16   TRG_CARD_CONTENTS_BI          BI    CARD_CONTENTS
17   TRG_CARD_APPLICATIONS_BI      BI    CARD_APPLICATIONS
18   TRG_CARD_ATTR_DEF_BI          BI    CARD_ATTRIBUTE_DEFINITIONS
19   TRG_CARD_ATTR_VAL_BI          BI    CARD_ATTRIBUTE_VALUES
20   TRG_CARD_TAGS_BI              BI    CARD_TAGS
21   TRG_CARD_TAG_MAP_BI           BI    CARD_TAG_MAP
22   TRG_CARD_PROMOTIONS_BI        BI    CARD_PROMOTIONS
23   TRG_CARD_STATUS_HIST_BI       BI    CARD_STATUS_HISTORIES
24   TRG_CARD_VERSIONS_BI          BI    CARD_VERSIONS
25   TRG_USER_CARDS_BI             BI    USER_CARDS
26   TRG_MERCHANT_CATEGORY_BI      BI    MERCHANT_CATEGORY_MAP
27   TRG_TERMS_GROUPS_BI           BI    TERMS_GROUPS
28   TRG_TERMS_PACKAGES_BI         BI    TERMS_PACKAGES
29   TRG_PACKAGE_TERMS_BI          BI    PACKAGE_TERMS
30   TRG_TERMS_MASTERS_BI          BI    TERMS_MASTERS
31   TRG_TERMS_BI                  BI    TERMS
32   TRG_TERMS_STATUS_HIST_BI      BI    TERMS_STATUS_HISTORY
33   TRG_TERMS_EXPOSURE_RULES_BI   BI    TERMS_EXPOSURE_RULES
34   TRG_TERMS_FILES_BI            BI    TERMS_FILES
35   TRG_CARD_TERMS_BI             BI    CARD_TERMS
36   TRG_USER_TERMS_AGMT_BI        BI    USER_TERMS_AGREEMENTS
37   TRG_CARD_NOTICE_BI            BI    CARD_NOTICE_SECTIONS
38   TRG_TERMS_CHANGE_DIFFS_BI     BI    TERMS_CHANGE_DIFFS
39   TRG_TERMS_NOTIF_HIST_BI       BI    TERMS_NOTIFICATION_HISTORY
40   TRG_FILE_UPLOADS_BI           BI    FILE_UPLOADS
41   TRG_SEARCH_KEYWORDS_BI        BI    SEARCH_KEYWORDS
42   TRG_CARD_KEYWORDS_BI          BI    CARD_KEYWORDS
43   TRG_SEARCH_LOGS_BI            BI    SEARCH_LOGS
44   TRG_USER_SPENDING_BI          BI    USER_SPENDING_PATTERNS
45   TRG_AI_CHAT_LOGS_BI           BI    AI_CHAT_LOGS
46   TRG_CARDS_BI                  BI    CARDS          ★ FN_GEN_CARD_ID 호출
47   TRG_CARDS_BU                  BU    CARDS          updated_at 자동 갱신
48   TRG_USERS_BU                  BU    USERS          updated_at 자동 갱신
BI = BEFORE INSERT / BU = BEFORE UPDATE
★ = 일반 시퀀스가 아닌 FN_GEN_CARD_ID() 함수 호출

END OF DOCUMENT
