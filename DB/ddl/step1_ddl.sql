-- ================================================================
-- STEP 1. DDL — 먼저 실행 (서버 켜진 상태에서도 가능)
-- ================================================================

-- phone 컬럼 크기 확장 (평문 20자 → 암호화 후 약 150자)
ALTER TABLE USERS MODIFY (phone VARCHAR2(300));

-- ci_value 컬럼 크기 확장
ALTER TABLE USERS MODIFY (ci_value VARCHAR2(500));

-- 보안 컬럼 추가
ALTER TABLE USERS ADD (
    password_version NUMBER(3)    DEFAULT 1,
    mfa_enabled      CHAR(1)      DEFAULT 'N' CHECK (mfa_enabled IN ('Y','N')),
    mfa_secret       VARCHAR2(300),
    cdd_status_code  VARCHAR2(20) DEFAULT 'PENDING'
                     CHECK (cdd_status_code IN ('PENDING','VERIFIED','REJECTED','ENHANCED'))
);

-- 비밀번호 이력 테이블
CREATE TABLE USER_PASSWORD_HISTORIES (
    history_id    NUMBER(19)    PRIMARY KEY,
    user_id       NUMBER(10)    NOT NULL,
    password_hash VARCHAR2(255) NOT NULL,
    created_at    TIMESTAMP     DEFAULT SYSTIMESTAMP,
    CONSTRAINT FK_PWD_HIST_USER FOREIGN KEY (user_id) REFERENCES USERS(user_id)
);
CREATE SEQUENCE SEQ_USER_PWD_HIST START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE INDEX IDX_PWD_HIST_USER ON USER_PASSWORD_HISTORIES(user_id, created_at DESC);

CREATE OR REPLACE TRIGGER TRG_USER_PWD_HIST_BI
    BEFORE INSERT ON USER_PASSWORD_HISTORIES FOR EACH ROW
BEGIN
    IF :NEW.history_id IS NULL THEN
        SELECT SEQ_USER_PWD_HIST.NEXTVAL INTO :NEW.history_id FROM DUAL;
    END IF;
END;
/

-- CDD 테이블
CREATE TABLE USER_CDD_CHECKS (
    cdd_id               NUMBER(19)   PRIMARY KEY,
    user_id              NUMBER(10)   NOT NULL,
    cdd_level            VARCHAR2(20) DEFAULT 'SIMPLE'
                         CHECK (cdd_level IN ('SIMPLE','NORMAL','ENHANCED')),
    identity_verified_at TIMESTAMP,
    transaction_purpose  VARCHAR2(200),
    pep_yn               CHAR(1)      DEFAULT 'N' CHECK (pep_yn IN ('Y','N')),
    check_result_code    VARCHAR2(50),
    checked_at           TIMESTAMP,
    created_at           TIMESTAMP    DEFAULT SYSTIMESTAMP,
    CONSTRAINT FK_CDD_USER FOREIGN KEY (user_id) REFERENCES USERS(user_id)
);
CREATE SEQUENCE SEQ_USER_CDD START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER TRG_USER_CDD_BI
    BEFORE INSERT ON USER_CDD_CHECKS FOR EACH ROW
BEGIN
    IF :NEW.cdd_id IS NULL THEN
        SELECT SEQ_USER_CDD.NEXTVAL INTO :NEW.cdd_id FROM DUAL;
    END IF;
END;
/

COMMIT;

-- 확인 쿼리
--SELECT column_name, data_length FROM user_tab_columns
-- WHERE table_name = 'USERS' AND column_name IN ('PHONE','CI_VALUE');
-- phone: 300, ci_value: 500 이어야 함


-- 1. 임시 컬럼 추가
ALTER TABLE USERS ADD (birth_date_temp VARCHAR2(200));

-- 2. 기존 DATE 값을 문자열로 임시 컬럼에 복사
UPDATE USERS
SET    birth_date_temp = TO_CHAR(birth_date, 'YYYY-MM-DD')
WHERE  birth_date IS NOT NULL;

COMMIT;

-- 3. 기존 birth_date 컬럼 값 전체 NULL로 비우기
UPDATE USERS SET birth_date = NULL;
COMMIT;

-- 4. 이제 비어있으므로 타입 변경 가능
ALTER TABLE USERS MODIFY (birth_date VARCHAR2(200));

-- 5. 임시 컬럼 값을 다시 birth_date로 복사
UPDATE USERS
SET    birth_date = birth_date_temp
WHERE  birth_date_temp IS NOT NULL;

COMMIT;

-- 6. 임시 컬럼 삭제
ALTER TABLE USERS DROP COLUMN birth_date_temp;

COMMIT;