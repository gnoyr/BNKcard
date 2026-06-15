-- ================================================================
-- USER_TRUSTED_IPS — 사용자 신뢰 IP 목록
-- Oracle 21c / SEQ + BEFORE INSERT TRG 패턴 (기존 프로젝트 패턴 동일)
--
-- [IP 암호화 설계]
-- ip_address     : AES-256-GCM 암호화 저장 (aesTypeHandler 적용)
--                  GCM은 매번 새 IV → 암호문 직접 비교 불가
-- ip_address_hash: SHA-256 결정론적 해시 → WHERE 조건 / UNIQUE 제약
--                  Watchlist.ci_value_hash 패턴과 동일
-- ================================================================

CREATE TABLE USER_TRUSTED_IPS (
    trust_id            NUMBER(15)      PRIMARY KEY,
    user_id             NUMBER(10)      NOT NULL,
    ip_address          VARCHAR2(200)   NOT NULL,
    ip_address_hash     VARCHAR2(64)    NOT NULL,
    nickname            VARCHAR2(50)    DEFAULT '내 기기',
    is_initial          CHAR(1)         DEFAULT 'N'
                        CHECK (is_initial IN ('Y','N')),
    status_code         VARCHAR2(20)    DEFAULT 'ACTIVE'
                        CHECK (status_code IN ('ACTIVE','DISABLED')),
    last_used_at        TIMESTAMP,
    registered_via      VARCHAR2(20)    DEFAULT 'SIGNUP'
                        CHECK (registered_via IN ('SIGNUP','EMAIL_VERIFY','CI_VERIFY','ADMIN')),
    created_at          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at          TIMESTAMP,
    deleted_yn          CHAR(1)         DEFAULT 'N' CHECK (deleted_yn IN ('Y','N')),
    deleted_at          TIMESTAMP,
    CONSTRAINT FK_TRUSTED_IPS_USER   FOREIGN KEY (user_id) REFERENCES USERS(user_id),
    CONSTRAINT UQ_TRUSTED_IPS_HASH   UNIQUE (user_id, ip_address_hash, deleted_yn)
);

COMMENT ON TABLE  USER_TRUSTED_IPS                 IS '사용자 신뢰 IP 목록 (IP 기반 2단계 인증)';
COMMENT ON COLUMN USER_TRUSTED_IPS.trust_id        IS 'PK — SEQ_TRUSTED_IPS 자동 채번';
COMMENT ON COLUMN USER_TRUSTED_IPS.ip_address      IS 'AES-256-GCM 암호화 저장';
COMMENT ON COLUMN USER_TRUSTED_IPS.ip_address_hash IS 'SHA-256(ip_address) hex 64자 — WHERE/UNIQUE 제약용';
COMMENT ON COLUMN USER_TRUSTED_IPS.is_initial      IS 'Y = 회원가입 최초 등록 IP (삭제 불가)';
COMMENT ON COLUMN USER_TRUSTED_IPS.status_code     IS 'ACTIVE / DISABLED';
COMMENT ON COLUMN USER_TRUSTED_IPS.registered_via  IS '등록 경로: SIGNUP / EMAIL_VERIFY / CI_VERIFY / ADMIN';
COMMENT ON COLUMN USER_TRUSTED_IPS.deleted_yn      IS '논리 삭제 여부';

-- 시퀀스
CREATE SEQUENCE SEQ_TRUSTED_IPS START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- BEFORE INSERT — PK 자동 채번
CREATE OR REPLACE TRIGGER TRG_TRUSTED_IPS_BI
    BEFORE INSERT ON USER_TRUSTED_IPS
    FOR EACH ROW
    WHEN (NEW.trust_id IS NULL)
BEGIN
    :NEW.trust_id := SEQ_TRUSTED_IPS.NEXTVAL;
END;
/

-- BEFORE UPDATE — updated_at 자동 갱신
CREATE OR REPLACE TRIGGER TRG_TRUSTED_IPS_BU
    BEFORE UPDATE ON USER_TRUSTED_IPS
    FOR EACH ROW
BEGIN
    :NEW.updated_at := SYSTIMESTAMP;
END;
/

-- 인덱스
CREATE INDEX IDX_TRUSTED_IPS_HASH ON USER_TRUSTED_IPS (user_id, ip_address_hash, status_code, deleted_yn);
CREATE INDEX IDX_TRUSTED_IPS_USER ON USER_TRUSTED_IPS (user_id, status_code, deleted_yn);
CREATE INDEX IDX_TRUSTED_IPS_INIT ON USER_TRUSTED_IPS (user_id, is_initial);
