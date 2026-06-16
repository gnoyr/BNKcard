-- ================================================================
-- BNK 부산은행 — 카드 신청 테이블 DDL
-- Oracle 21c
-- 작성일: 2026-06-15
--
-- [변경 사항]
--   1. CARD_APPLICATIONS 테이블 DROP (기존 테이블 대체)
--   2. CREDIT_CARD_APPLICATIONS 신규 생성 (신용카드 신청)
--   3. CHECK_CARD_APPLICATIONS  신규 생성 (체크카드 신청)
--   4. USER_CARDS FK 수정 (application_id → credit_app_id / check_app_id)
--   5. USER_TERMS_AGREEMENTS 컬럼 추가 (credit_app_id / check_app_id)
--
-- [수정 이력]
--   2026-06-15: screening_rejected_reason 제거 → rejection_reason으로 통합
--               approved_limit: PASS/MANUAL 케이스 모두 부산은행 서버에서 SET
--               bank_account_verified_yn 제거 → 계좌 선택 방식으로 변경
--               (payment_snapshot.linked_account_no로 선택 완료 판단)
-- ================================================================


-- ================================================================
-- [SECTION 1-A] DROP — 기존 busanbank_ddl.sql에서 생성된 것들
-- (CARD_APPLICATIONS 테이블 및 관련 객체 제거)
-- ================================================================

-- 트리거 DROP
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER TRG_CARD_APPLICATIONS_BI';              EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- USER_CARDS FK 제거 (CARD_APPLICATIONS 참조 — 테이블 DROP 전에 먼저 제거)
BEGIN EXECUTE IMMEDIATE 'ALTER TABLE USER_CARDS DROP CONSTRAINT FK_USER_CARDS_APPL'; EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- 테이블 DROP (CASCADE라 IDX_CARD_APPL_USER, IDX_CARD_APPL_CARD 인덱스도 같이 삭제됨)
BEGIN EXECUTE IMMEDIATE 'DROP TABLE CARD_APPLICATIONS CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- 시퀀스 DROP
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_CARD_APPLICATIONS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/


-- ================================================================
-- [SECTION 1-B] DROP — 이 파일에서 새로 생성하는 것들 (재실행 대비)
-- ================================================================

-- 트리거 DROP
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER TRG_CREDIT_CARD_APP_BI';     EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER TRG_CREDIT_CARD_APP_BU';     EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER TRG_CHECK_CARD_APP_BI';      EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER TRG_CHECK_CARD_APP_BU';      EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- USER_CARDS FK 제거 (신청 테이블 참조)
BEGIN EXECUTE IMMEDIATE 'ALTER TABLE USER_CARDS DROP CONSTRAINT FK_USER_CARDS_CREDIT_APP'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'ALTER TABLE USER_CARDS DROP CONSTRAINT FK_USER_CARDS_CHECK_APP';  EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- USER_TERMS_AGREEMENTS FK 제거
BEGIN EXECUTE IMMEDIATE 'ALTER TABLE USER_TERMS_AGREEMENTS DROP CONSTRAINT FK_UTA_CREDIT_APP'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'ALTER TABLE USER_TERMS_AGREEMENTS DROP CONSTRAINT FK_UTA_CHECK_APP';  EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- 테이블 DROP
BEGIN EXECUTE IMMEDIATE 'DROP TABLE CREDIT_CARD_APPLICATIONS CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE CHECK_CARD_APPLICATIONS  CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- 시퀀스 DROP
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_CREDIT_CARD_APPLICATIONS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_CHECK_CARD_APPLICATIONS';  EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- USER_CARDS 컬럼 DROP (재실행 대비)
BEGIN EXECUTE IMMEDIATE 'ALTER TABLE USER_CARDS DROP COLUMN credit_app_id'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'ALTER TABLE USER_CARDS DROP COLUMN check_app_id';  EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'ALTER TABLE USER_CARDS DROP COLUMN card_password_hash'; EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- USER_TERMS_AGREEMENTS 컬럼 DROP (재실행 대비)
BEGIN EXECUTE IMMEDIATE 'ALTER TABLE USER_TERMS_AGREEMENTS DROP COLUMN credit_app_id'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'ALTER TABLE USER_TERMS_AGREEMENTS DROP COLUMN check_app_id';  EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- ================================================================
-- [SECTION 2] CREATE
-- ================================================================

-- ----------------------------------------------------------------
-- 시퀀스
-- ----------------------------------------------------------------
CREATE SEQUENCE SEQ_CREDIT_CARD_APPLICATIONS
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE SEQUENCE SEQ_CHECK_CARD_APPLICATIONS
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;


-- ----------------------------------------------------------------
-- 1. CREDIT_CARD_APPLICATIONS (신용카드 신청)
-- ----------------------------------------------------------------
CREATE TABLE CREDIT_CARD_APPLICATIONS (
    -- [PK / 기본]
    credit_app_id               NUMBER(19)      PRIMARY KEY,
    user_id                     NUMBER(19)      NOT NULL,
    card_id                     NUMBER(19)      NOT NULL,
    application_status          VARCHAR2(30)    DEFAULT 'DRAFT' NOT NULL
        CHECK (application_status IN ('DRAFT','REQUESTED','REVIEWING','APPROVED','REJECTED','ISSUED')),

    -- [본인확인]
    -- id_type, id_issue_date: OCR로 추출한 값. 본인확인 API 호출 시 사용
    -- id_verified_yn: 심사서버에서 본인확인 결과 반환 후 업데이트. Y여야 다음 단계 진행 가능
    id_type                     VARCHAR2(20)
        CHECK (id_type IN ('RESIDENT','DRIVER')),
    id_verified_yn              CHAR(1)         DEFAULT 'N'
        CHECK (id_verified_yn IN ('Y','N')),

    -- [신청정보 - 컬럼으로 유지 (심사/필터링에 사용)]
    -- annual_income_band: 금융소비자보호법에 따라 수집하는 연소득 구간
    -- credit_score_band : 1차 필터링(600점 이하 거절)에 사용
    -- requested_limit   : 추정월소득 × 30% 초과 여부 판단에 사용
    annual_income_band          VARCHAR2(10)
        CHECK (annual_income_band IN ('LV1','LV2','LV3','LV4')),        -- LV1:600만이하 LV2:600~5000만 LV3:5000만~1억 LV4:1억초과
    credit_score_band           VARCHAR2(10)
        CHECK (credit_score_band IN ('HIGH','MID','LOW')),              -- HIGH:700초과 MID:500~700 LOW:500이하
    requested_limit             NUMBER(15),                             -- 신청 한도 (원)

    -- [계좌 - 연회비 자동이체]
    linked_account_id           NUMBER(19),                             -- 연회비 자동이체 계좌 ID

    -- [서류 - 신규고객만 해당, 기존고객은 NULL]
    -- 신청 버튼 클릭 시 부산은행 서버에서 기존고객 여부 체크
    -- 신규고객이면 서류 제출 팝업 → OCI 업로드 → object key 저장
    income_doc_key              VARCHAR2(1000),                         -- 소득확인서류 OCI object key
    asset_doc_key               VARCHAR2(1000),                         -- 재산확인서류 OCI object key (선택 제출)
    job_doc_key                 VARCHAR2(1000),                         -- 직업확인서류 OCI object key

    -- [심사 - 심사서버에서 업데이트]
    doc_verified_yn             CHAR(1)
        CHECK (doc_verified_yn IN ('Y','N')),                           -- 서류 육안 확인 완료 (심사서버 업데이트)
    screening_result            VARCHAR2(20)
        CHECK (screening_result IN ('PASS','REJECTED')),                -- 1차 필터링 결과 (심사서버 업데이트)

    -- [월소득 추정 및 한도 검증 - 부산은행 서버에서 처리]
    estimated_monthly_income    NUMBER(15),                             -- 결제내역 기반 추정 월소득 (부산은행 서버 계산)
    limit_check_result          VARCHAR2(20)
        CHECK (limit_check_result IN ('PASS','MANUAL_REQUIRED')),       -- PASS: 한도OK / MANUAL_REQUIRED: 한도초과 → 심사서버 재전달

    -- [최종 심사]
    -- approved_limit: PASS 케이스 → 부산은행 서버에서 requested_limit 그대로 SET
    --                 MANUAL 케이스 → 심사서버에서 조정한 한도 SET
    -- rejection_reason: 1차 거절(screening_result=REJECTED) 또는 최종 거절 사유 통합 저장
    approved_limit              NUMBER(15),                             -- 최종 승인 한도
    rejection_reason            VARCHAR2(1000),                         -- 거절 사유 (1차/최종 통합)
    reviewed_at                 TIMESTAMP,                              -- 심사서버 개입 시 심사 완료 일시
    reviewed_by                 NUMBER(19),                             -- 심사서버 개입 시 심사 담당자 ID

    -- [스냅샷 - JSON, 신청 시점 고정값]
    applicant_snapshot          CLOB,  -- { name, name_en, mobile_no, address, email, id_issue_date, income_type, health_insurance_type, has_real_estate, has_own_vehicle }
    payment_snapshot            CLOB,   -- { card_brand, payment_day, card_password_hash, combined_transit_yn, tx_alert_type, statement_method, overseas_dcc_block_yn }

    -- [공통]
    applied_at                  TIMESTAMP,                              -- 신청 완료 일시 (REQUESTED로 변경 시 업데이트)
    created_at                  TIMESTAMP   DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at                  TIMESTAMP,

    -- FK
    CONSTRAINT FK_CREDIT_APP_USER       FOREIGN KEY (user_id)           REFERENCES USERS(user_id),
    CONSTRAINT FK_CREDIT_APP_CARD       FOREIGN KEY (card_id)           REFERENCES CARDS(card_id),
    CONSTRAINT FK_CREDIT_APP_ACCOUNT    FOREIGN KEY (linked_account_id) REFERENCES ACCOUNTS(account_id)
);

COMMENT ON TABLE  CREDIT_CARD_APPLICATIONS                             IS '신용카드 신청';
COMMENT ON COLUMN CREDIT_CARD_APPLICATIONS.application_status          IS 'DRAFT:작성중 / REQUESTED:신청완료 / REVIEWING:심사중 / APPROVED:승인 / REJECTED:거절 / ISSUED:발급완료';
COMMENT ON COLUMN CREDIT_CARD_APPLICATIONS.id_type                     IS 'RESIDENT:주민등록증 / DRIVER:운전면허증';
COMMENT ON COLUMN CREDIT_CARD_APPLICATIONS.id_verified_yn              IS '본인확인 완료 여부. Y여야 다음 단계 진행 가능';
COMMENT ON COLUMN CREDIT_CARD_APPLICATIONS.annual_income_band          IS 'LV1:600만이하 / LV2:600만~5000만 / LV3:5000만~1억 / LV4:1억초과';
COMMENT ON COLUMN CREDIT_CARD_APPLICATIONS.credit_score_band           IS 'HIGH:700점초과 / MID:500~700점 / LOW:500점이하';
COMMENT ON COLUMN CREDIT_CARD_APPLICATIONS.linked_account_id           IS '연회비 자동이체 계좌 ID. ACCOUNTS(account_id) 참조';
COMMENT ON COLUMN CREDIT_CARD_APPLICATIONS.income_doc_key              IS '소득확인서류 OCI object key. 신규고객만 해당';
COMMENT ON COLUMN CREDIT_CARD_APPLICATIONS.asset_doc_key               IS '재산확인서류 OCI object key. 선택 제출';
COMMENT ON COLUMN CREDIT_CARD_APPLICATIONS.job_doc_key                 IS '직업확인서류 OCI object key. 신규고객만 해당';
COMMENT ON COLUMN CREDIT_CARD_APPLICATIONS.doc_verified_yn             IS '심사서버에서 서류 육안 확인 완료 후 업데이트';
COMMENT ON COLUMN CREDIT_CARD_APPLICATIONS.screening_result            IS '심사서버 1차 필터링 결과. PASS:통과 / REJECTED:신용점수600이하 또는 월가처분소득50만이하';
COMMENT ON COLUMN CREDIT_CARD_APPLICATIONS.estimated_monthly_income    IS '부산은행 서버가 보유 결제내역 기반으로 계산한 추정 월소득';
COMMENT ON COLUMN CREDIT_CARD_APPLICATIONS.limit_check_result          IS '부산은행 서버 한도검증 결과. PASS:신청한도≤추정월소득×30% / MANUAL_REQUIRED:초과하여 심사서버 재전달';
COMMENT ON COLUMN CREDIT_CARD_APPLICATIONS.approved_limit              IS '최종 승인 한도. PASS케이스=부산은행서버가 requested_limit 그대로 SET / MANUAL케이스=심사서버가 조정한 한도 SET';
COMMENT ON COLUMN CREDIT_CARD_APPLICATIONS.rejection_reason            IS '거절 사유 통합 컬럼. 1차 필터링 거절 및 최종 거절 모두 여기에 저장';
COMMENT ON COLUMN CREDIT_CARD_APPLICATIONS.reviewed_at                 IS '심사서버 개입 케이스(MANUAL_REQUIRED)의 심사 완료 일시';
COMMENT ON COLUMN CREDIT_CARD_APPLICATIONS.reviewed_by                 IS '심사서버 개입 케이스(MANUAL_REQUIRED)의 심사 담당자 ID';
COMMENT ON COLUMN CREDIT_CARD_APPLICATIONS.applicant_snapshot          IS '신청 시점 기본정보 JSON. name/name_en/mobile_no/address/email/id_issue_date/income_type/health_insurance_type/has_real_estate/has_own_vehicle';
COMMENT ON COLUMN CREDIT_CARD_APPLICATIONS.payment_snapshot            IS '신청정보 JSON. card_brand/payment_day/card_password_hash/combined_transit_yn/tx_alert_type/statement_method/overseas_dcc_block_yn';
COMMENT ON COLUMN CREDIT_CARD_APPLICATIONS.applied_at                  IS '신청 완료 일시. application_status가 REQUESTED로 변경될 때 업데이트';


-- ----------------------------------------------------------------
-- 2. CHECK_CARD_APPLICATIONS (체크카드 신청)
-- ----------------------------------------------------------------
CREATE TABLE CHECK_CARD_APPLICATIONS (
    -- [PK / 기본]
    check_app_id                NUMBER(19)      PRIMARY KEY,
    user_id                     NUMBER(19)      NOT NULL,
    card_id                     NUMBER(19)      NOT NULL,
    application_status          VARCHAR2(30)    DEFAULT 'DRAFT' NOT NULL
        CHECK (application_status IN ('DRAFT','REQUESTED','APPROVED','REJECTED','ISSUED')),

    -- [실명확인]
    -- 체크카드는 신분증 실명확인 필요
    -- 계좌는 보유 계좌 목록 옵션박스로 선택 → linked_account_id로 참조
    id_type                     VARCHAR2(20)
        CHECK (id_type IN ('RESIDENT','DRIVER')),
    id_verified_yn              CHAR(1)         DEFAULT 'N'
        CHECK (id_verified_yn IN ('Y','N')),                            -- 본인확인 완료 Y/N
    linked_account_id           NUMBER(19),                             -- 선택한 부산은행 계좌 ID. BANK_ACCOUNTS 생성 후 FK 추가 예정

    -- [심사]
    rejection_reason            VARCHAR2(1000),                         -- 거절 사유
    reviewed_at                 TIMESTAMP,                              -- 심사 완료 일시
    reviewed_by                 NUMBER(19),                             -- 심사 담당자 ID

    -- [스냅샷 - JSON]
    -- applicant_snapshot: 기본정보 + 직업구분 + 거래목적 + 자금원천
    -- payment_snapshot  : 카드 신청정보 (linked_account_no 포함)
    applicant_snapshot          CLOB,   -- { name, name_en, mobile_no, address, email, job_type, transaction_purpose, fund_source }
    payment_snapshot            CLOB,   -- { card_brand, payment_day, card_password_hash, combined_transit_yn, tx_alert_type, statement_method, overseas_dcc_block_yn }

    -- [공통]
    applied_at                  TIMESTAMP,                              -- 신청 완료 일시
    created_at                  TIMESTAMP   DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at                  TIMESTAMP,

    -- FK
    CONSTRAINT FK_CHECK_APP_USER        FOREIGN KEY (user_id)           REFERENCES USERS(user_id),
    CONSTRAINT FK_CHECK_APP_CARD        FOREIGN KEY (card_id)           REFERENCES CARDS(card_id),
    CONSTRAINT FK_CHECK_APP_ACCOUNT     FOREIGN KEY (linked_account_id) REFERENCES ACCOUNTS(account_id)
);

COMMENT ON TABLE  CHECK_CARD_APPLICATIONS                               IS '체크카드 신청';
COMMENT ON COLUMN CHECK_CARD_APPLICATIONS.application_status           IS 'DRAFT:작성중 / REQUESTED:신청완료 / APPROVED:승인 / REJECTED:거절 / ISSUED:발급완료';
COMMENT ON COLUMN CHECK_CARD_APPLICATIONS.id_type                      IS 'RESIDENT:주민등록증 / DRIVER:운전면허증';
COMMENT ON COLUMN CHECK_CARD_APPLICATIONS.id_verified_yn               IS '본인확인 완료 여부. Y여야 다음 단계 진행 가능';
COMMENT ON COLUMN CHECK_CARD_APPLICATIONS.linked_account_id            IS '선택한 부산은행 계좌 ID. ACCOUNTS(account_id) 참조';
COMMENT ON COLUMN CHECK_CARD_APPLICATIONS.applicant_snapshot           IS '신청자 기본정보 + 거래정보 JSON. name/name_en/mobile_no/address/email/id_issue_date/job_type/transaction_purpose/fund_source';
COMMENT ON COLUMN CHECK_CARD_APPLICATIONS.payment_snapshot             IS '신청정보 JSON. card_brand/payment_day/card_password_hash/combined_transit_yn/tx_alert_type/statement_method/overseas_dcc_block_yn';
COMMENT ON COLUMN CHECK_CARD_APPLICATIONS.applied_at                   IS '신청 완료 일시. application_status가 REQUESTED로 변경될 때 업데이트';


-- ----------------------------------------------------------------
-- 3. USER_CARDS FK 수정
-- (기존 application_id → credit_app_id / check_app_id 로 분리)
-- ----------------------------------------------------------------
ALTER TABLE USER_CARDS ADD (
    card_password_hash  VARCHAR2(200),
    credit_app_id       NUMBER(19),
    check_app_id        NUMBER(19)
);

COMMENT ON COLUMN USER_CARDS.card_password_hash IS '카드 결제 비밀번호 해시값. 발급 시 payment_snapshot에서 옮겨 저장. 비밀번호 변경 시 업데이트';
COMMENT ON COLUMN USER_CARDS.credit_app_id      IS '신용카드 발급 시 참조. 체크카드 발급 시 NULL';
COMMENT ON COLUMN USER_CARDS.check_app_id       IS '체크카드 발급 시 참조. 신용카드 발급 시 NULL';

ALTER TABLE USER_CARDS ADD CONSTRAINT FK_USER_CARDS_CREDIT_APP
    FOREIGN KEY (credit_app_id) REFERENCES CREDIT_CARD_APPLICATIONS(credit_app_id);

ALTER TABLE USER_CARDS ADD CONSTRAINT FK_USER_CARDS_CHECK_APP
    FOREIGN KEY (check_app_id)  REFERENCES CHECK_CARD_APPLICATIONS(check_app_id);
-- ----------------------------------------------------------------
-- 4. USER_TERMS_AGREEMENTS 컬럼 추가
-- (카드 신청 건과 약관 동의 연결)
-- ----------------------------------------------------------------
ALTER TABLE USER_TERMS_AGREEMENTS ADD (
    credit_app_id   NUMBER(19),
    check_app_id    NUMBER(19)
);

ALTER TABLE USER_TERMS_AGREEMENTS ADD CONSTRAINT FK_UTA_CREDIT_APP
    FOREIGN KEY (credit_app_id) REFERENCES CREDIT_CARD_APPLICATIONS(credit_app_id);

ALTER TABLE USER_TERMS_AGREEMENTS ADD CONSTRAINT FK_UTA_CHECK_APP
    FOREIGN KEY (check_app_id)  REFERENCES CHECK_CARD_APPLICATIONS(check_app_id);

COMMENT ON COLUMN USER_TERMS_AGREEMENTS.credit_app_id  IS '신용카드 신청 시 동의한 약관 연결. SIGNUP 등 다른 소스는 NULL';
COMMENT ON COLUMN USER_TERMS_AGREEMENTS.check_app_id   IS '체크카드 신청 시 동의한 약관 연결. SIGNUP 등 다른 소스는 NULL';


-- ----------------------------------------------------------------
-- 5. BEFORE INSERT 트리거 (PK 자동 채번)
-- ----------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_CREDIT_CARD_APP_BI
BEFORE INSERT ON CREDIT_CARD_APPLICATIONS
FOR EACH ROW
WHEN (NEW.credit_app_id IS NULL)
BEGIN
    :NEW.credit_app_id := SEQ_CREDIT_CARD_APPLICATIONS.NEXTVAL;
END TRG_CREDIT_CARD_APP_BI;
/

CREATE OR REPLACE TRIGGER TRG_CHECK_CARD_APP_BI
BEFORE INSERT ON CHECK_CARD_APPLICATIONS
FOR EACH ROW
WHEN (NEW.check_app_id IS NULL)
BEGIN
    :NEW.check_app_id := SEQ_CHECK_CARD_APPLICATIONS.NEXTVAL;
END TRG_CHECK_CARD_APP_BI;
/


-- ----------------------------------------------------------------
-- 6. BEFORE UPDATE 트리거 (updated_at 자동 갱신)
-- ----------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_CREDIT_CARD_APP_BU
BEFORE UPDATE ON CREDIT_CARD_APPLICATIONS
FOR EACH ROW
BEGIN
    :NEW.updated_at := SYSTIMESTAMP;
END TRG_CREDIT_CARD_APP_BU;
/

CREATE OR REPLACE TRIGGER TRG_CHECK_CARD_APP_BU
BEFORE UPDATE ON CHECK_CARD_APPLICATIONS
FOR EACH ROW
BEGIN
    :NEW.updated_at := SYSTIMESTAMP;
END TRG_CHECK_CARD_APP_BU;
/


-- ----------------------------------------------------------------
-- 7. 인덱스
-- ----------------------------------------------------------------
CREATE INDEX IDX_CREDIT_APP_USER        ON CREDIT_CARD_APPLICATIONS(user_id);
CREATE INDEX IDX_CREDIT_APP_CARD        ON CREDIT_CARD_APPLICATIONS(card_id);
CREATE INDEX IDX_CREDIT_APP_STATUS      ON CREDIT_CARD_APPLICATIONS(application_status);
CREATE INDEX IDX_CREDIT_APP_CREATED     ON CREDIT_CARD_APPLICATIONS(created_at DESC);

CREATE INDEX IDX_CHECK_APP_USER         ON CHECK_CARD_APPLICATIONS(user_id);
CREATE INDEX IDX_CHECK_APP_CARD         ON CHECK_CARD_APPLICATIONS(card_id);
CREATE INDEX IDX_CHECK_APP_STATUS       ON CHECK_CARD_APPLICATIONS(application_status);
CREATE INDEX IDX_CHECK_APP_CREATED      ON CHECK_CARD_APPLICATIONS(created_at DESC);

CREATE INDEX IDX_UTA_CREDIT_APP         ON USER_TERMS_AGREEMENTS(credit_app_id);
CREATE INDEX IDX_UTA_CHECK_APP          ON USER_TERMS_AGREEMENTS(check_app_id);


-- ----------------------------------------------------------------
-- 8. DRAFT 배치 삭제 참고 쿼리
-- (7일 이상 DRAFT 상태인 미완료 신청 건 정리)
-- ----------------------------------------------------------------
/*
DELETE FROM USER_TERMS_AGREEMENTS
WHERE credit_app_id IN (
    SELECT credit_app_id FROM CREDIT_CARD_APPLICATIONS
    WHERE  application_status = 'DRAFT'
    AND    created_at < SYSTIMESTAMP - INTERVAL '7' DAY
);
DELETE FROM CREDIT_CARD_APPLICATIONS
WHERE  application_status = 'DRAFT'
AND    created_at < SYSTIMESTAMP - INTERVAL '7' DAY;

DELETE FROM USER_TERMS_AGREEMENTS
WHERE check_app_id IN (
    SELECT check_app_id FROM CHECK_CARD_APPLICATIONS
    WHERE  application_status = 'DRAFT'
    AND    created_at < SYSTIMESTAMP - INTERVAL '7' DAY
);
DELETE FROM CHECK_CARD_APPLICATIONS
WHERE  application_status = 'DRAFT'
AND    created_at < SYSTIMESTAMP - INTERVAL '7' DAY;

COMMIT;
*/

COMMIT;



SELECT table_name
FROM user_tables
WHERE table_name IN (
    'CREDIT_CARD_APPLICATIONS',
    'CHECK_CARD_APPLICATIONS'
);

-- 컬럼 확인 (screening_rejected_reason 없어야 함, bank_account_verified_yn 없어야 함)
SELECT column_name FROM user_tab_columns
WHERE table_name = 'CREDIT_CARD_APPLICATIONS'
ORDER BY column_id;

SELECT column_name FROM user_tab_columns
WHERE table_name = 'CHECK_CARD_APPLICATIONS'
ORDER BY column_id;

----------------------------

SELECT sequence_name
FROM user_sequences
WHERE sequence_name IN (
    'SEQ_CREDIT_CARD_APPLICATIONS',
    'SEQ_CHECK_CARD_APPLICATIONS'
);

SELECT trigger_name, status
FROM user_triggers
WHERE trigger_name IN (
    'TRG_CREDIT_CARD_APP_BI',
    'TRG_CREDIT_CARD_APP_BU',
    'TRG_CHECK_CARD_APP_BI',
    'TRG_CHECK_CARD_APP_BU'
);

SELECT column_name
FROM user_tab_columns
WHERE table_name = 'USER_CARDS'
AND column_name IN ('CREDIT_APP_ID', 'CHECK_APP_ID', 'APPLICATION_ID');

SELECT column_name
FROM user_tab_columns
WHERE table_name = 'USER_TERMS_AGREEMENTS'
AND column_name IN ('CREDIT_APP_ID', 'CHECK_APP_ID');