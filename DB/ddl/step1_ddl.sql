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


CREATE TABLE WATCHLIST (
    watchlist_id  NUMBER(19)    PRIMARY KEY,
    name          VARCHAR2(100) NOT NULL,
    birth_date    VARCHAR2(200),
    ci_value      VARCHAR2(500),
    reason        VARCHAR2(500),
    risk_level    VARCHAR2(20)  DEFAULT 'HIGH'
                  CHECK (risk_level IN ('HIGH','MEDIUM')),
    registered_at TIMESTAMP     DEFAULT SYSTIMESTAMP,
    registered_by NUMBER(10),
    deleted_yn    CHAR(1)       DEFAULT 'N' CHECK (deleted_yn IN ('Y','N'))
);
CREATE SEQUENCE SEQ_WATCHLIST START WITH 1 INCREMENT BY 1 NOCACHE;
 
CREATE OR REPLACE TRIGGER TRG_WATCHLIST_BI
    BEFORE INSERT ON WATCHLIST FOR EACH ROW
BEGIN
    IF :NEW.watchlist_id IS NULL THEN
        SELECT SEQ_WATCHLIST.NEXTVAL INTO :NEW.watchlist_id FROM DUAL;
    END IF;
END;
/
 
CREATE INDEX IDX_WATCHLIST_NAME ON WATCHLIST(name);

ALTER TABLE WATCHLIST ADD (
    ci_value_hash    VARCHAR2(64),
    birth_date_hash  VARCHAR2(64)
);

CREATE INDEX IDX_WATCHLIST_CI_HASH ON WATCHLIST(ci_value_hash);
CREATE INDEX IDX_WATCHLIST_NAME_BD ON WATCHLIST(name, birth_date_hash);
-- ※ AES 복호화는 Java에서만 가능하므로, 마이그레이션 엔드포인트 또는
--    EncryptionMigrationService 패턴으로 Java에서 일괄 처리 필요.
--    기존 Watchlist 데이터가 없다면 이 단계 생략.

COMMIT;



-- log table

-- ① 시퀀스
CREATE SEQUENCE SEQ_EVENT_LOGS START WITH 1 INCREMENT BY 1;

-- ② 부모 테이블
CREATE TABLE EVENT_LOGS (
    log_id        NUMBER PRIMARY KEY,
    event_type    VARCHAR2(50)  NOT NULL,
    event_status  VARCHAR2(10)  NOT NULL,  -- SUCCESS / FAILURE
    user_id       NUMBER,
    request_ip    VARCHAR2(50),
    duration_ms   NUMBER,
    created_at    TIMESTAMP DEFAULT SYSTIMESTAMP,
    CONSTRAINT fk_evlog_user FOREIGN KEY (user_id) REFERENCES USERS(user_id)
);

-- 트리거
CREATE OR REPLACE TRIGGER TRG_EVENT_LOGS_BI
BEFORE INSERT ON EVENT_LOGS FOR EACH ROW
WHEN (NEW.log_id IS NULL)
BEGIN
    :NEW.log_id := SEQ_EVENT_LOGS.NEXTVAL;
END;
/

-- ③ 카드 이벤트 자식 (PK = FK)
CREATE TABLE CARD_EVENT_LOGS (
    log_id        NUMBER PRIMARY KEY,
    card_id       VARCHAR2(20),
    action_detail VARCHAR2(100),
    result_code   VARCHAR2(30),
    error_message VARCHAR2(500),
    CONSTRAINT fk_card_evlog FOREIGN KEY (log_id) REFERENCES EVENT_LOGS(log_id)
);

-- ④ 약관 이벤트 자식
CREATE TABLE TERMS_EVENT_LOGS (
    log_id        NUMBER PRIMARY KEY,
    terms_id      NUMBER,
    action_detail VARCHAR2(100),
    error_message VARCHAR2(500),
    CONSTRAINT fk_terms_evlog FOREIGN KEY (log_id) REFERENCES EVENT_LOGS(log_id)
);

-- ⑤ 챗봇 이벤트 자식
CREATE TABLE CHAT_EVENT_LOGS (
    log_id           NUMBER PRIMARY KEY,
    session_id       VARCHAR2(100),
    query_text       VARCHAR2(1000),
    qdrant_hit_count NUMBER,
    top_score        NUMBER(5,4),
    used_fallback    CHAR(1) DEFAULT 'N',
    error_message    VARCHAR2(500),
    CONSTRAINT fk_chat_evlog FOREIGN KEY (log_id) REFERENCES EVENT_LOGS(log_id)
);