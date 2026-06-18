-- ================================================================
-- BNK 부산은행 금융 상품 플랫폼
-- [도메인 05] 카드 신청 · 계좌
-- Oracle 21c
-- 포함 테이블:
--   ACCOUNTS, ACCOUNT_PASSWORDS, ACCOUNT_TERMS_AGREEMENTS
--   CREDIT_CARD_APPLICATIONS, CHECK_CARD_APPLICATIONS
--   USER_CARDS ALTER (card_password_hash, credit_app_id, check_app_id)
--   USER_TERMS_AGREEMENTS ALTER (credit_app_id, check_app_id)
--
-- [실행 선행 조건]
--   03_ddl_card_product.sql 실행 완료 (USERS, CARDS, USER_CARDS 존재)
--   04_ddl_terms.sql 실행 완료 (TERMS, USER_TERMS_AGREEMENTS 존재)
-- ================================================================


-- ================================================================
-- [SECTION 1] DROP (재실행 대비 — FK → 트리거 → 테이블 → 시퀀스 순)
-- ================================================================

-- 트리거
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER TRG_CHECK_CARD_APP_BU';      EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER TRG_CHECK_CARD_APP_BI';      EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER TRG_CREDIT_CARD_APP_BU';     EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER TRG_CREDIT_CARD_APP_BI';     EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER TRG_ACCOUNT_TERMS_BI';       EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER TRG_ACCOUNT_PASSWORDS_BI';   EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER TRG_ACCOUNTS_BI';            EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- USER_CARDS FK / 컬럼 DROP (재실행 대비)
BEGIN EXECUTE IMMEDIATE 'ALTER TABLE USER_CARDS DROP CONSTRAINT FK_USER_CARDS_CREDIT_APP'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'ALTER TABLE USER_CARDS DROP CONSTRAINT FK_USER_CARDS_CHECK_APP';  EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'ALTER TABLE USER_CARDS DROP COLUMN credit_app_id';                EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'ALTER TABLE USER_CARDS DROP COLUMN check_app_id';                 EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'ALTER TABLE USER_CARDS DROP COLUMN card_password_hash';           EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- USER_TERMS_AGREEMENTS FK / 컬럼 DROP (재실행 대비)
BEGIN EXECUTE IMMEDIATE 'ALTER TABLE USER_TERMS_AGREEMENTS DROP CONSTRAINT FK_UTA_CREDIT_APP'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'ALTER TABLE USER_TERMS_AGREEMENTS DROP CONSTRAINT FK_UTA_CHECK_APP';  EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'ALTER TABLE USER_TERMS_AGREEMENTS DROP COLUMN credit_app_id';         EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'ALTER TABLE USER_TERMS_AGREEMENTS DROP COLUMN check_app_id';          EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- 테이블 DROP
BEGIN EXECUTE IMMEDIATE 'DROP TABLE CHECK_CARD_APPLICATIONS   CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE CREDIT_CARD_APPLICATIONS  CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE ACCOUNT_TERMS_AGREEMENTS  CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE ACCOUNT_PASSWORDS         CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE ACCOUNTS                  CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- 시퀀스 DROP
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_CHECK_CARD_APPLICATIONS';   EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_CREDIT_CARD_APPLICATIONS';  EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_ACCOUNT_TERMS_AGREEMENTS';  EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_ACCOUNT_PASSWORDS';         EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_ACCOUNTS';                  EXCEPTION WHEN OTHERS THEN NULL; END;
/


-- ================================================================
-- [SECTION 2] CREATE
-- ================================================================

-- ── 시퀀스 ────────────────────────────────────────────────────────
CREATE SEQUENCE SEQ_ACCOUNTS                 START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_ACCOUNT_PASSWORDS        START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_ACCOUNT_TERMS_AGREEMENTS START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_CREDIT_CARD_APPLICATIONS START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_CHECK_CARD_APPLICATIONS  START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;


-- ── 테이블 ────────────────────────────────────────────────────────

-- ACCOUNTS (부산은행 계좌)
CREATE TABLE ACCOUNTS (
    account_id      NUMBER(19)    PRIMARY KEY,
    user_id         NUMBER(10)    NOT NULL,
    account_number  VARCHAR2(20)  NOT NULL UNIQUE,
    account_type    VARCHAR2(20)  NOT NULL CHECK (account_type IN ('CHECKING','SAVINGS','DEPOSIT')),
    account_alias   VARCHAR2(50),
    account_status  VARCHAR2(20)  DEFAULT 'ACTIVE' CHECK (account_status IN ('ACTIVE','DORMANT','CLOSED')),
    balance         NUMBER(15,2)  DEFAULT 0,
    created_at      TIMESTAMP     DEFAULT SYSTIMESTAMP,
    CONSTRAINT FK_ACCOUNT_USER FOREIGN KEY (user_id) REFERENCES USERS(user_id)
);
COMMENT ON TABLE  ACCOUNTS               IS '부산은행 계좌';
COMMENT ON COLUMN ACCOUNTS.account_type  IS 'CHECKING(입출금) / SAVINGS(적금) / DEPOSIT(예금)';

-- ACCOUNT_PASSWORDS (계좌 비밀번호 — BCrypt 해시)
CREATE TABLE ACCOUNT_PASSWORDS (
    account_id     NUMBER(19)    PRIMARY KEY,
    password_hash  VARCHAR2(200) NOT NULL,
    fail_count     NUMBER(5)     DEFAULT 0,
    locked_yn      CHAR(1)       DEFAULT 'N' CHECK (locked_yn IN ('Y','N')),
    updated_at     TIMESTAMP     DEFAULT SYSTIMESTAMP,
    CONSTRAINT FK_ACCPW_ACCOUNT FOREIGN KEY (account_id) REFERENCES ACCOUNTS(account_id)
);
COMMENT ON TABLE  ACCOUNT_PASSWORDS           IS '계좌 비밀번호 (BCrypt 해시)';
COMMENT ON COLUMN ACCOUNT_PASSWORDS.fail_count IS '연속 실패 횟수. 5회 초과 시 locked_yn = Y';

-- ACCOUNT_TERMS_AGREEMENTS (계좌 개설 약관 동의)
CREATE TABLE ACCOUNT_TERMS_AGREEMENTS (
    agreement_id  NUMBER(19)  PRIMARY KEY,
    account_id    NUMBER(19)  NOT NULL,
    terms_id      NUMBER(10)  NOT NULL,
    agreed_at     TIMESTAMP   DEFAULT SYSTIMESTAMP,
    CONSTRAINT FK_ACCTERMS_ACCOUNT FOREIGN KEY (account_id) REFERENCES ACCOUNTS(account_id),
    CONSTRAINT FK_ACCTERMS_TERMS   FOREIGN KEY (terms_id)   REFERENCES TERMS(terms_id)
);
COMMENT ON TABLE ACCOUNT_TERMS_AGREEMENTS IS '계좌 개설 약관 동의 이력';

-- CREDIT_CARD_APPLICATIONS (신용카드 신청)
CREATE TABLE CREDIT_CARD_APPLICATIONS (
    credit_app_id               NUMBER(19)     PRIMARY KEY,
    user_id                     NUMBER(10)     NOT NULL,
    card_id                     NUMBER(10)     NOT NULL,
    application_status          VARCHAR2(30)   DEFAULT 'DRAFT'
                                    CHECK (application_status IN ('DRAFT','REQUESTED','APPROVED','REJECTED','ISSUED')),
    apply_channel               VARCHAR2(30)   CHECK (apply_channel IN ('WEB','MOBILE','APP')),

    -- [본인확인]
    id_type                     VARCHAR2(30),                           -- RESIDENT / DRIVER
    id_verified_yn              CHAR(1)        DEFAULT 'N' CHECK (id_verified_yn IN ('Y','N')),

    -- [신용심사]
    requested_limit             NUMBER(15),
    approved_limit              NUMBER(15),
    rejection_reason            VARCHAR2(1000),
    reviewed_at                 TIMESTAMP,
    reviewed_by                 VARCHAR2(100),

    -- [스냅샷 — JSON]
    -- applicant_snapshot: name, name_en, mobile_no, address, email,
    --                     job_type, income, transaction_purpose, fund_source
    -- payment_snapshot: card_brand, linked_account_no, payment_day,
    --                   combined_transit_yn, tx_alert_type, statement_method,
    --                   overseas_dcc_block_yn
    applicant_snapshot          CLOB,
    payment_snapshot            CLOB,

    -- [공통]
    applied_at                  TIMESTAMP,
    created_at                  TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at                  TIMESTAMP,

    CONSTRAINT FK_CREDIT_APP_USER FOREIGN KEY (user_id) REFERENCES USERS(user_id),
    CONSTRAINT FK_CREDIT_APP_CARD FOREIGN KEY (card_id) REFERENCES CARDS(card_id)
);
COMMENT ON TABLE  CREDIT_CARD_APPLICATIONS                              IS '신용카드 신청';
COMMENT ON COLUMN CREDIT_CARD_APPLICATIONS.application_status          IS 'DRAFT:작성중 / REQUESTED:신청완료 / APPROVED:승인 / REJECTED:거절 / ISSUED:발급완료';
COMMENT ON COLUMN CREDIT_CARD_APPLICATIONS.id_type                     IS 'RESIDENT:주민등록증 / DRIVER:운전면허증';
COMMENT ON COLUMN CREDIT_CARD_APPLICATIONS.applicant_snapshot          IS '신청자 기본정보 JSON. name/name_en/mobile_no/address/email/job_type/income/transaction_purpose/fund_source';
COMMENT ON COLUMN CREDIT_CARD_APPLICATIONS.payment_snapshot            IS '결제정보 JSON. card_brand/linked_account_no/payment_day/combined_transit_yn/tx_alert_type/statement_method/overseas_dcc_block_yn';
COMMENT ON COLUMN CREDIT_CARD_APPLICATIONS.applied_at                  IS '신청 완료 일시. application_status가 REQUESTED로 변경될 때 업데이트';

-- CHECK_CARD_APPLICATIONS (체크카드 신청)
CREATE TABLE CHECK_CARD_APPLICATIONS (
    check_app_id                NUMBER(19)     PRIMARY KEY,
    user_id                     NUMBER(10)     NOT NULL,
    card_id                     NUMBER(10)     NOT NULL,
    application_status          VARCHAR2(30)   DEFAULT 'DRAFT'
                                    CHECK (application_status IN ('DRAFT','REQUESTED','APPROVED','REJECTED','ISSUED')),
    apply_channel               VARCHAR2(30)   CHECK (apply_channel IN ('WEB','MOBILE','APP')),

    -- [본인확인]
    id_type                     VARCHAR2(30),
    id_verified_yn              CHAR(1)        DEFAULT 'N' CHECK (id_verified_yn IN ('Y','N')),

    -- [연결 계좌 — 부산은행 계좌 필수]
    linked_account_id           NUMBER(19),

    -- [심사]
    rejection_reason            VARCHAR2(1000),
    reviewed_at                 TIMESTAMP,
    reviewed_by                 VARCHAR2(100),

    -- [스냅샷 — JSON]
    applicant_snapshot          CLOB,
    payment_snapshot            CLOB,

    -- [공통]
    applied_at                  TIMESTAMP,
    created_at                  TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at                  TIMESTAMP,

    CONSTRAINT FK_CHECK_APP_USER    FOREIGN KEY (user_id)           REFERENCES USERS(user_id),
    CONSTRAINT FK_CHECK_APP_CARD    FOREIGN KEY (card_id)           REFERENCES CARDS(card_id),
    CONSTRAINT FK_CHECK_APP_ACCOUNT FOREIGN KEY (linked_account_id) REFERENCES ACCOUNTS(account_id)
);
COMMENT ON TABLE  CHECK_CARD_APPLICATIONS                               IS '체크카드 신청';
COMMENT ON COLUMN CHECK_CARD_APPLICATIONS.application_status           IS 'DRAFT:작성중 / REQUESTED:신청완료 / APPROVED:승인 / REJECTED:거절 / ISSUED:발급완료';
COMMENT ON COLUMN CHECK_CARD_APPLICATIONS.linked_account_id            IS '선택한 부산은행 계좌 ID. ACCOUNTS(account_id) 참조';
COMMENT ON COLUMN CHECK_CARD_APPLICATIONS.applicant_snapshot           IS '신청자 기본정보 JSON';
COMMENT ON COLUMN CHECK_CARD_APPLICATIONS.payment_snapshot             IS '결제정보 JSON. linked_account_no 포함';
COMMENT ON COLUMN CHECK_CARD_APPLICATIONS.applied_at                   IS '신청 완료 일시. REQUESTED 상태 변경 시 업데이트';


-- ── USER_CARDS ALTER (카드 신청 참조 컬럼 추가) ────────────────────
ALTER TABLE USER_CARDS ADD (
    card_password_hash  VARCHAR2(200),
    credit_app_id       NUMBER(19),
    check_app_id        NUMBER(19)
);
COMMENT ON COLUMN USER_CARDS.card_password_hash IS '카드 결제 비밀번호 BCrypt 해시값. 발급 시 서비스에서 BCrypt 후 저장';
COMMENT ON COLUMN USER_CARDS.credit_app_id      IS '신용카드 발급 시 참조. 체크카드 발급 시 NULL';
COMMENT ON COLUMN USER_CARDS.check_app_id       IS '체크카드 발급 시 참조. 신용카드 발급 시 NULL';

ALTER TABLE USER_CARDS ADD CONSTRAINT FK_USER_CARDS_CREDIT_APP
    FOREIGN KEY (credit_app_id) REFERENCES CREDIT_CARD_APPLICATIONS(credit_app_id);
ALTER TABLE USER_CARDS ADD CONSTRAINT FK_USER_CARDS_CHECK_APP
    FOREIGN KEY (check_app_id)  REFERENCES CHECK_CARD_APPLICATIONS(check_app_id);


-- ── USER_TERMS_AGREEMENTS ALTER (카드 신청 연결) ──────────────────
ALTER TABLE USER_TERMS_AGREEMENTS ADD (
    credit_app_id  NUMBER(19),
    check_app_id   NUMBER(19)
);
COMMENT ON COLUMN USER_TERMS_AGREEMENTS.credit_app_id IS '신용카드 신청 시 동의한 약관 연결. 타 소스는 NULL';
COMMENT ON COLUMN USER_TERMS_AGREEMENTS.check_app_id  IS '체크카드 신청 시 동의한 약관 연결. 타 소스는 NULL';

ALTER TABLE USER_TERMS_AGREEMENTS ADD CONSTRAINT FK_UTA_CREDIT_APP
    FOREIGN KEY (credit_app_id) REFERENCES CREDIT_CARD_APPLICATIONS(credit_app_id);
ALTER TABLE USER_TERMS_AGREEMENTS ADD CONSTRAINT FK_UTA_CHECK_APP
    FOREIGN KEY (check_app_id)  REFERENCES CHECK_CARD_APPLICATIONS(check_app_id);


-- ── 트리거 ────────────────────────────────────────────────────────
CREATE OR REPLACE TRIGGER TRG_ACCOUNTS_BI
BEFORE INSERT ON ACCOUNTS FOR EACH ROW WHEN (NEW.account_id IS NULL)
BEGIN :NEW.account_id := SEQ_ACCOUNTS.NEXTVAL; END TRG_ACCOUNTS_BI;
/

CREATE OR REPLACE TRIGGER TRG_ACCOUNT_PASSWORDS_BI
BEFORE INSERT ON ACCOUNT_PASSWORDS FOR EACH ROW
BEGIN
    IF :NEW.account_id IS NULL THEN
        SELECT SEQ_ACCOUNT_PASSWORDS.NEXTVAL INTO :NEW.account_id FROM DUAL;
    END IF;
END TRG_ACCOUNT_PASSWORDS_BI;
/

CREATE OR REPLACE TRIGGER TRG_ACCOUNT_TERMS_BI
BEFORE INSERT ON ACCOUNT_TERMS_AGREEMENTS FOR EACH ROW WHEN (NEW.agreement_id IS NULL)
BEGIN :NEW.agreement_id := SEQ_ACCOUNT_TERMS_AGREEMENTS.NEXTVAL; END TRG_ACCOUNT_TERMS_BI;
/

CREATE OR REPLACE TRIGGER TRG_CREDIT_CARD_APP_BI
BEFORE INSERT ON CREDIT_CARD_APPLICATIONS FOR EACH ROW WHEN (NEW.credit_app_id IS NULL)
BEGIN :NEW.credit_app_id := SEQ_CREDIT_CARD_APPLICATIONS.NEXTVAL; END TRG_CREDIT_CARD_APP_BI;
/

CREATE OR REPLACE TRIGGER TRG_CREDIT_CARD_APP_BU
BEFORE UPDATE ON CREDIT_CARD_APPLICATIONS FOR EACH ROW
BEGIN :NEW.updated_at := SYSTIMESTAMP; END TRG_CREDIT_CARD_APP_BU;
/

CREATE OR REPLACE TRIGGER TRG_CHECK_CARD_APP_BI
BEFORE INSERT ON CHECK_CARD_APPLICATIONS FOR EACH ROW WHEN (NEW.check_app_id IS NULL)
BEGIN :NEW.check_app_id := SEQ_CHECK_CARD_APPLICATIONS.NEXTVAL; END TRG_CHECK_CARD_APP_BI;
/

CREATE OR REPLACE TRIGGER TRG_CHECK_CARD_APP_BU
BEFORE UPDATE ON CHECK_CARD_APPLICATIONS FOR EACH ROW
BEGIN :NEW.updated_at := SYSTIMESTAMP; END TRG_CHECK_CARD_APP_BU;
/


-- ── 인덱스 ────────────────────────────────────────────────────────
CREATE INDEX IDX_ACCOUNTS_USER           ON ACCOUNTS(user_id);
CREATE INDEX IDX_ACCOUNTS_STATUS         ON ACCOUNTS(account_status);
CREATE INDEX IDX_CREDIT_APP_USER         ON CREDIT_CARD_APPLICATIONS(user_id);
CREATE INDEX IDX_CREDIT_APP_CARD         ON CREDIT_CARD_APPLICATIONS(card_id);
CREATE INDEX IDX_CREDIT_APP_STATUS       ON CREDIT_CARD_APPLICATIONS(application_status);
CREATE INDEX IDX_CHECK_APP_USER          ON CHECK_CARD_APPLICATIONS(user_id);
CREATE INDEX IDX_CHECK_APP_CARD          ON CHECK_CARD_APPLICATIONS(card_id);
CREATE INDEX IDX_CHECK_APP_STATUS        ON CHECK_CARD_APPLICATIONS(application_status);
CREATE INDEX IDX_USER_CARDS_CREDIT_APP   ON USER_CARDS(credit_app_id);
CREATE INDEX IDX_USER_CARDS_CHECK_APP    ON USER_CARDS(check_app_id);


-- ── DRAFT 정리 참고 쿼리 (배치 사용) ─────────────────────────────
/*
-- 7일 이상 DRAFT 상태 신용카드 신청 건 정리
DELETE FROM USER_TERMS_AGREEMENTS
WHERE credit_app_id IN (
    SELECT credit_app_id FROM CREDIT_CARD_APPLICATIONS
    WHERE application_status = 'DRAFT'
    AND   created_at < SYSTIMESTAMP - INTERVAL '7' DAY
);
DELETE FROM CREDIT_CARD_APPLICATIONS
WHERE application_status = 'DRAFT'
AND   created_at < SYSTIMESTAMP - INTERVAL '7' DAY;

-- 7일 이상 DRAFT 상태 체크카드 신청 건 정리
DELETE FROM USER_TERMS_AGREEMENTS
WHERE check_app_id IN (
    SELECT check_app_id FROM CHECK_CARD_APPLICATIONS
    WHERE application_status = 'DRAFT'
    AND   created_at < SYSTIMESTAMP - INTERVAL '7' DAY
);
DELETE FROM CHECK_CARD_APPLICATIONS
WHERE application_status = 'DRAFT'
AND   created_at < SYSTIMESTAMP - INTERVAL '7' DAY;

COMMIT;
*/


COMMIT;
