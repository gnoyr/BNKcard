-- ================================================================
-- 감사 로그 추가 DDL
-- Oracle 21c / 프로젝트 SEQ + BEFORE INSERT TRG 패턴 동일 적용
--
-- 기존 테이블 (수정 없음)
--   LOGIN_HISTORIES  : 로그인 이력 (AuthService에서 사용 중)
--   AUDIT_LOGS       : 보안 이벤트 (UserMapper, AdminUserMapper에서 사용 중)
--
-- 신규 추가
--   USER_ACTIVITY_LOG  : 사용자 행위 이력
--   ADMIN_ACTIVITY_LOG : 관리자 행위 이력
-- ================================================================


-- ================================================================
-- 1. USER_ACTIVITY_LOG — 사용자 행위 이력
-- ================================================================
CREATE TABLE USER_ACTIVITY_LOG (
    activity_id  NUMBER(15)    PRIMARY KEY,
    user_id      NUMBER(10)    NOT NULL,
    action       VARCHAR2(60)  NOT NULL,
    result       CHAR(1)       NOT NULL CHECK (result IN ('S', 'F')),
    target_id    VARCHAR2(100),
    detail       VARCHAR2(2000),
    client_ip    VARCHAR2(45),
    request_uri  VARCHAR2(500),
    occurred_at  TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL
);

COMMENT ON TABLE  USER_ACTIVITY_LOG            IS '사용자 행위 이력';
COMMENT ON COLUMN USER_ACTIVITY_LOG.action     IS 'CARD_APPLY_APPLY / TERMS_AGREE / AUTH_PASSWORD_CHANGE 등 category_action 형태';
COMMENT ON COLUMN USER_ACTIVITY_LOG.result     IS 'S=SUCCESS, F=FAILURE';
COMMENT ON COLUMN USER_ACTIVITY_LOG.target_id  IS '대상 리소스 ID (카드ID, 약관ID 등 범용 문자열)';

CREATE SEQUENCE SEQ_USER_ACTIVITY_LOG
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE OR REPLACE TRIGGER TRG_USER_ACTIVITY_LOG_BI
    BEFORE INSERT ON USER_ACTIVITY_LOG
    FOR EACH ROW
    WHEN (NEW.activity_id IS NULL)
BEGIN
    :NEW.activity_id := SEQ_USER_ACTIVITY_LOG.NEXTVAL;
END;
/

-- 무결성 보장 — DELETE / UPDATE 차단
CREATE OR REPLACE TRIGGER TRG_USER_ACTIVITY_LOG_NO_DEL
    BEFORE DELETE ON USER_ACTIVITY_LOG FOR EACH ROW
BEGIN
    RAISE_APPLICATION_ERROR(-20110, '[USER_ACTIVITY_LOG] 행위 로그는 삭제할 수 없습니다.');
END;
/
CREATE OR REPLACE TRIGGER TRG_USER_ACTIVITY_LOG_NO_UPD
    BEFORE UPDATE ON USER_ACTIVITY_LOG FOR EACH ROW
BEGIN
    RAISE_APPLICATION_ERROR(-20111, '[USER_ACTIVITY_LOG] 행위 로그는 수정할 수 없습니다.');
END;
/

CREATE INDEX IDX_USER_ACT_USER ON USER_ACTIVITY_LOG (user_id, occurred_at DESC);
CREATE INDEX IDX_USER_ACT_ACT  ON USER_ACTIVITY_LOG (action,  occurred_at DESC);
CREATE INDEX IDX_USER_ACT_RES  ON USER_ACTIVITY_LOG (result,  occurred_at DESC);


-- ================================================================
-- 2. ADMIN_ACTIVITY_LOG — 관리자 행위 이력
-- ================================================================
CREATE TABLE ADMIN_ACTIVITY_LOG (
    activity_id  NUMBER(15)    PRIMARY KEY,
    admin_id     NUMBER(10)    NOT NULL,
    action       VARCHAR2(60)  NOT NULL,
    result       CHAR(1)       NOT NULL CHECK (result IN ('S', 'F')),
    target_id    VARCHAR2(100),
    detail       VARCHAR2(2000),
    client_ip    VARCHAR2(45),
    request_uri  VARCHAR2(500),
    occurred_at  TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL
);

COMMENT ON TABLE  ADMIN_ACTIVITY_LOG            IS '관리자 행위 이력';
COMMENT ON COLUMN ADMIN_ACTIVITY_LOG.action     IS 'CARD_CREATE / CARD_APPROVAL_APPROVE / ADMIN_USER_STATUS_CHANGE 등';
COMMENT ON COLUMN ADMIN_ACTIVITY_LOG.result     IS 'S=SUCCESS, F=FAILURE';
COMMENT ON COLUMN ADMIN_ACTIVITY_LOG.target_id  IS '대상 리소스 ID (카드ID, 신청ID, 유저ID 등 범용 문자열)';

CREATE SEQUENCE SEQ_ADMIN_ACTIVITY_LOG
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE OR REPLACE TRIGGER TRG_ADMIN_ACTIVITY_LOG_BI
    BEFORE INSERT ON ADMIN_ACTIVITY_LOG
    FOR EACH ROW
    WHEN (NEW.activity_id IS NULL)
BEGIN
    :NEW.activity_id := SEQ_ADMIN_ACTIVITY_LOG.NEXTVAL;
END;
/

-- 무결성 보장 — DELETE / UPDATE 차단
CREATE OR REPLACE TRIGGER TRG_ADMIN_ACTIVITY_LOG_NO_DEL
    BEFORE DELETE ON ADMIN_ACTIVITY_LOG FOR EACH ROW
BEGIN
    RAISE_APPLICATION_ERROR(-20120, '[ADMIN_ACTIVITY_LOG] 행위 로그는 삭제할 수 없습니다.');
END;
/
CREATE OR REPLACE TRIGGER TRG_ADMIN_ACTIVITY_LOG_NO_UPD
    BEFORE UPDATE ON ADMIN_ACTIVITY_LOG FOR EACH ROW
BEGIN
    RAISE_APPLICATION_ERROR(-20121, '[ADMIN_ACTIVITY_LOG] 행위 로그는 수정할 수 없습니다.');
END;
/

CREATE INDEX IDX_ADMIN_ACT_ADMIN ON ADMIN_ACTIVITY_LOG (admin_id, occurred_at DESC);
CREATE INDEX IDX_ADMIN_ACT_ACT   ON ADMIN_ACTIVITY_LOG (action,   occurred_at DESC);
CREATE INDEX IDX_ADMIN_ACT_RES   ON ADMIN_ACTIVITY_LOG (result,   occurred_at DESC);


-- ================================================================
-- 3. 기존 AUDIT_LOGS 무결성 트리거 추가
-- ================================================================
CREATE OR REPLACE TRIGGER TRG_AUDIT_LOGS_NO_DEL
    BEFORE DELETE ON AUDIT_LOGS FOR EACH ROW
BEGIN
    RAISE_APPLICATION_ERROR(-20100, '[AUDIT_LOGS] 감사 로그는 삭제할 수 없습니다.');
END;
/
CREATE OR REPLACE TRIGGER TRG_AUDIT_LOGS_NO_UPD
    BEFORE UPDATE ON AUDIT_LOGS FOR EACH ROW
BEGIN
    RAISE_APPLICATION_ERROR(-20101, '[AUDIT_LOGS] 감사 로그는 수정할 수 없습니다.');
END;
/
