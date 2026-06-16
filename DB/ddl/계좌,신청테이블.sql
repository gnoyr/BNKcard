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