-- 계좌 테이블
CREATE TABLE ACCOUNTS (
    account_id      NUMBER PRIMARY KEY,
    user_id         NUMBER NOT NULL,
    account_number  VARCHAR2(20) NOT NULL UNIQUE,  -- 채번된 계좌번호
    account_type    VARCHAR2(20) NOT NULL,          -- CHECKING(입출금)/SAVINGS(적금)/DEPOSIT(예금)
    account_alias   VARCHAR2(50),                   -- 계좌 별명
    account_status  VARCHAR2(20) DEFAULT 'ACTIVE',  -- ACTIVE/DORMANT/CLOSED
    balance         NUMBER(15,2) DEFAULT 0,
    created_at      TIMESTAMP DEFAULT SYSTIMESTAMP,
    CONSTRAINT fk_account_user FOREIGN KEY (user_id) REFERENCES USERS(user_id)
);

-- 계좌 비밀번호 (별도 테이블 — AES 암호화)
CREATE TABLE ACCOUNT_PASSWORDS (
    account_id      NUMBER PRIMARY KEY,
    password_hash   VARCHAR2(200) NOT NULL,         -- BCrypt 해시
    fail_count      NUMBER DEFAULT 0,               -- 연속 실패 횟수
    locked_yn       CHAR(1) DEFAULT 'N',            -- 5회 실패 시 잠금
    updated_at      TIMESTAMP DEFAULT SYSTIMESTAMP,
    CONSTRAINT fk_accpw_account FOREIGN KEY (account_id) REFERENCES ACCOUNTS(account_id)
);

-- 계좌 약관 동의 이력
CREATE TABLE ACCOUNT_TERMS_AGREEMENTS (
    agreement_id    NUMBER PRIMARY KEY,
    account_id      NUMBER NOT NULL,
    terms_id        NUMBER NOT NULL,
    agreed_at       TIMESTAMP DEFAULT SYSTIMESTAMP,
    CONSTRAINT fk_accterms_account FOREIGN KEY (account_id) REFERENCES ACCOUNTS(account_id)
);

CREATE SEQUENCE SEQ_ACCOUNTS START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE SEQ_ACCOUNT_PASSWORDS START WITH 1 INCREMENT BY 1;


-- 1. TERMS_MASTERS 컬럼 추가
ALTER TABLE TERMS_MASTERS ADD required_yn CHAR(1) DEFAULT 'N' CHECK (required_yn IN ('Y', 'N'));
ALTER TABLE TERMS_MASTERS ADD display_order NUMBER(5) DEFAULT 1;

-- 2. EVENT_LOGS 계열 컬럼 크기 확대
ALTER TABLE EVENT_LOGS MODIFY event_type VARCHAR2(100);
ALTER TABLE CARD_EVENT_LOGS MODIFY result_code VARCHAR2(100);
ALTER TABLE CARD_EVENT_LOGS MODIFY action_detail VARCHAR2(200);
ALTER TABLE CARD_EVENT_LOGS MODIFY error_message VARCHAR2(1000);

-- 3. USER_ACTIVITY_LOG user_id 인덱스 재생성
DROP INDEX IDX_USER_ACT_USER;
ALTER TABLE USER_ACTIVITY_LOG MODIFY user_id NUMBER NULL;  -- 이미 NULL이라 오류나도 무시
CREATE INDEX IDX_USER_ACT_USER ON USER_ACTIVITY_LOG(user_id);

-- 4. ACCOUNTS 테이블 생성
CREATE SEQUENCE SEQ_ACCOUNTS START WITH 1 INCREMENT BY 1;

-- 5. ACCOUNT_PASSWORDS 테이블 생성
CREATE SEQUENCE SEQ_ACCOUNT_PASSWORDS START WITH 1 INCREMENT BY 1;

-- 6. TERMS_PACKAGES 데이터 추가
INSERT INTO TERMS_PACKAGES (package_id, package_name, package_type, description, created_at)
VALUES (SEQ_TERMS_PACKAGES.NEXTVAL, '계좌 개설 약관', 'ACCOUNT_OPEN', '계좌 개설 시 동의 약관 묶음', SYSTIMESTAMP);

INSERT INTO PACKAGE_TERMS (package_terms_id, package_id, terms_id, display_order, created_at)
VALUES (SEQ_PACKAGE_TERMS.NEXTVAL, 3, 150, 1, SYSTIMESTAMP);

INSERT INTO PACKAGE_TERMS (package_terms_id, package_id, terms_id, display_order, created_at)
VALUES (SEQ_PACKAGE_TERMS.NEXTVAL, 3, 152, 2, SYSTIMESTAMP);

COMMIT;