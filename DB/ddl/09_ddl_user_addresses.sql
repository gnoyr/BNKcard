-- ================================================================
-- BNK 부산은행 금융 상품 플랫폼
-- [도메인 09] 사용자 주소록 (배송지 / 다중 주소)
-- Oracle 21c
-- 포함 테이블:
--   USER_ADDRESSES  (주소는 AES-256-GCM 암호화 저장, 별칭·기본배송지 플래그)
--
-- 신뢰기기(USER_TRUSTED_IPS) 패턴을 따른다:
--   - address / address_detail : AesTypeHandler 로 암복호화 (USERS.phone 과 동일)
--   - 별칭(alias) 으로 식별, is_default 로 기본 배송지 1건 지정
--   - 논리 삭제(deleted_yn)
-- ================================================================

BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER TRG_USER_ADDRESSES_BU'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER TRG_USER_ADDRESSES_BI'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE USER_ADDRESSES CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_USER_ADDRESSES'; EXCEPTION WHEN OTHERS THEN NULL; END;
/

CREATE SEQUENCE SEQ_USER_ADDRESSES START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE USER_ADDRESSES (
    address_id       NUMBER(19)    PRIMARY KEY,
    user_id          NUMBER(10)    NOT NULL,
    alias            VARCHAR2(100),                 -- 별칭 (예: 집, 회사)
    zipcode          VARCHAR2(20),                  -- 우편번호 (평문)
    address          VARCHAR2(500) NOT NULL,        -- AES-256-GCM 암호화 저장 (도로명/지번)
    address_detail   VARCHAR2(500),                 -- AES-256-GCM 암호화 저장 (상세주소)
    is_default       CHAR(1)       DEFAULT 'N' CHECK (is_default  IN ('Y','N')),
    status_code      VARCHAR2(20)  DEFAULT 'ACTIVE' CHECK (status_code IN ('ACTIVE','DISABLED')),
    created_at       TIMESTAMP     DEFAULT SYSTIMESTAMP,
    updated_at       TIMESTAMP,
    deleted_yn       CHAR(1)       DEFAULT 'N' CHECK (deleted_yn IN ('Y','N')),
    deleted_at       TIMESTAMP,
    CONSTRAINT FK_USER_ADDRESSES_USER FOREIGN KEY (user_id) REFERENCES USERS(user_id)
);
COMMENT ON TABLE  USER_ADDRESSES                IS '사용자 주소록 (배송지). 카드 발급 배송지 선택에 사용';
COMMENT ON COLUMN USER_ADDRESSES.address        IS 'AES-256-GCM 암호화 저장. 조회 시 Java(AesTypeHandler)에서 복호화';
COMMENT ON COLUMN USER_ADDRESSES.address_detail IS 'AES-256-GCM 암호화 저장. 상세주소';
COMMENT ON COLUMN USER_ADDRESSES.is_default     IS '기본 배송지 여부. 사용자당 최대 1건만 Y';

-- BEFORE INSERT — PK 자동 채번
CREATE OR REPLACE TRIGGER TRG_USER_ADDRESSES_BI
BEFORE INSERT ON USER_ADDRESSES FOR EACH ROW WHEN (NEW.address_id IS NULL)
BEGIN :NEW.address_id := SEQ_USER_ADDRESSES.NEXTVAL; END TRG_USER_ADDRESSES_BI;
/

-- BEFORE UPDATE — updated_at 자동 갱신
CREATE OR REPLACE TRIGGER TRG_USER_ADDRESSES_BU
BEFORE UPDATE ON USER_ADDRESSES FOR EACH ROW
BEGIN :NEW.updated_at := SYSTIMESTAMP; END TRG_USER_ADDRESSES_BU;
/

CREATE INDEX IDX_USER_ADDRESSES_USER    ON USER_ADDRESSES(user_id, status_code, deleted_yn);
CREATE INDEX IDX_USER_ADDRESSES_DEFAULT ON USER_ADDRESSES(user_id, is_default, deleted_yn);

COMMIT;
