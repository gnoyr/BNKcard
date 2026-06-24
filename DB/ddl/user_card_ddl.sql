-- ============================================================
-- USER_CARDS 테이블 DROP & CREATE
-- ============================================================

-- ── 기존 객체 제거 ───────────────────────────────────────────
DROP TABLE USER_CARDS CASCADE CONSTRAINTS PURGE;
DROP SEQUENCE seq_user_card_id;

-- ── 테이블 생성 ──────────────────────────────────────────────
CREATE TABLE USER_CARDS (
    -- ── 식별자 ──────────────────────────────────────────────
    user_card_id              NUMBER(19)                             NOT NULL,  -- PK
    user_id                   NUMBER(19)                             NOT NULL,  -- FK: USERS
    version_id                NUMBER(10)                             NOT NULL,  -- FK: 신청 시점 PUBLISHED 카드 버전
    credit_app_id             NUMBER(19)                             NULL,      -- FK: CREDIT_CARD_APPLICATIONS (체크카드 시 NULL)
    check_app_id              NUMBER(19)                             NULL,      -- FK: CHECK_CARD_APPLICATIONS  (신용카드 시 NULL)

    -- ── 카드번호 ─────────────────────────────────────────────
    encrypted_card_number     VARCHAR2(500 BYTE)                     NOT NULL,  -- AES 암호화된 16자리 카드번호 (원문 보호용)
    masked_card_number        VARCHAR2(30  BYTE)                     NOT NULL,  -- 화면 표시용 마스킹 번호 (예: 1234-56**-****-7890)

    -- ── 유효기간 / 상태 ──────────────────────────────────────
    issue_date                DATE                                   NOT NULL,  -- 발급일
    expire_date               DATE                                   NOT NULL,  -- 만료일
    card_status               VARCHAR2(30  BYTE) DEFAULT 'ACTIVE'   NOT NULL,  -- ACTIVE / LOST / STOPPED / EXPIRED / REISSUED
    usable_yn                 CHAR(1       BYTE) DEFAULT 'Y'         NOT NULL,  -- 사용 가능 여부 Y/N
    card_password_hash        VARCHAR2(200 BYTE)                     NULL,      -- 카드 비밀번호 해시 (BCrypt)

    -- ── 연결 계좌 ────────────────────────────────────────────
    linked_account_id         NUMBER(19)                             NULL,      -- FK: ACCOUNTS (체크카드 필수, 신용카드 선택)

    -- ── 한도 ─────────────────────────────────────────────────
    daily_limit_amount        NUMBER(15)     DEFAULT 1000000         NOT NULL,  -- 일 한도 (원, 발급 시 100만원 기본)
    monthly_limit_amount      NUMBER(15)                             NULL,      -- 월 한도 (원, 신용카드=승인한도 / 체크카드=NULL)

    -- ── payment_snapshot 항목 직접 컬럼화 ───────────────────
    card_brand                VARCHAR2(20  BYTE)                     NULL,      -- 카드 브랜드: VISA / MASTER / LOCAL / AMEX / UPI
    card_design_id            NUMBER(19)                             NULL,      -- FK: CARD_IMAGES (신청 시 선택한 디자인)
    payment_day               NUMBER(2)                              NULL,      -- 월 결제일 (1~31)
    combined_transit_yn       CHAR(1       BYTE) DEFAULT 'N'         NULL,      -- 교통카드 결합 여부 Y/N
    tx_alert_type             VARCHAR2(20  BYTE)                     NULL,      -- 거래 알림 방식: SMS / PUSH / NONE
    statement_method          VARCHAR2(20  BYTE)                     NULL,      -- 명세서 수령: EMAIL / APP / PAPER

    -- ── 해외 / 비접촉 설정 ───────────────────────────────────
    overseas_enabled_yn       CHAR(1       BYTE) DEFAULT 'Y'         NOT NULL,  -- 해외 사용 가능 여부 Y/N
    contactless_enabled_yn    CHAR(1       BYTE) DEFAULT 'Y'         NOT NULL,  -- 비접촉(NFC) 결제 가능 여부 Y/N

    -- ── 카드 별칭 ────────────────────────────────────────────
    card_nickname             VARCHAR2(100 BYTE)                     NULL,      -- 사용자 지정 카드 별칭

    -- ── 감사(Audit) ──────────────────────────────────────────
    issued_by                 NUMBER(19)                             NULL,      -- 발급 처리 관리자 ID
    issued_at                 TIMESTAMP(6)       DEFAULT SYSTIMESTAMP NOT NULL, -- 발급 처리 일시
    updated_at                TIMESTAMP(6)                           NULL,      -- 최종 수정 일시
    deleted_yn                CHAR(1       BYTE) DEFAULT 'N'         NOT NULL,  -- 논리 삭제 여부 Y/N
    deleted_at                TIMESTAMP(6)                           NULL,      -- 논리 삭제 일시

    -- ── 제약조건 ─────────────────────────────────────────────
    CONSTRAINT pk_user_cards          PRIMARY KEY (user_card_id),
    CONSTRAINT fk_uc_user             FOREIGN KEY (user_id)        REFERENCES USERS(user_id),
    CONSTRAINT fk_uc_credit_app       FOREIGN KEY (credit_app_id)  REFERENCES CREDIT_CARD_APPLICATIONS(credit_app_id),
    CONSTRAINT fk_uc_check_app        FOREIGN KEY (check_app_id)   REFERENCES CHECK_CARD_APPLICATIONS(check_app_id),
    CONSTRAINT ck_uc_app_id_xor       CHECK (
        (credit_app_id IS NOT NULL AND check_app_id IS NULL)
     OR (credit_app_id IS NULL     AND check_app_id IS NOT NULL)
    ),
    CONSTRAINT ck_uc_card_status      CHECK (card_status          IN ('ACTIVE','LOST','STOPPED','EXPIRED','REISSUED')),
    CONSTRAINT ck_uc_usable_yn        CHECK (usable_yn            IN ('Y','N')),
    CONSTRAINT ck_uc_overseas_yn      CHECK (overseas_enabled_yn  IN ('Y','N')),
    CONSTRAINT ck_uc_contactless_yn   CHECK (contactless_enabled_yn IN ('Y','N')),
    CONSTRAINT ck_uc_transit_yn       CHECK (combined_transit_yn  IN ('Y','N')),
    CONSTRAINT ck_uc_deleted_yn       CHECK (deleted_yn           IN ('Y','N')),
    CONSTRAINT ck_uc_tx_alert         CHECK (tx_alert_type        IN ('SMS','PUSH','NONE')),
    CONSTRAINT ck_uc_statement        CHECK (statement_method     IN ('EMAIL','APP','PAPER')),
    CONSTRAINT ck_uc_card_brand       CHECK (card_brand           IN ('VISA','MASTER','LOCAL','AMEX','UPI'))
);

-- ── 시퀀스 ───────────────────────────────────────────────────
CREATE SEQUENCE seq_user_card_id
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;
    
-- ── 트리거 (BI: PK 자동 채번) ────────────────────────────────
CREATE OR REPLACE TRIGGER TRG_USER_CARDS_BI
BEFORE INSERT ON USER_CARDS FOR EACH ROW WHEN (NEW.user_card_id IS NULL)
BEGIN
    :NEW.user_card_id := SEQ_USER_CARD_ID.NEXTVAL;
END TRG_USER_CARDS_BI;
/

-- ── 트리거 (BU: updated_at 자동 갱신) ───────────────────────
CREATE OR REPLACE TRIGGER TRG_USER_CARDS_BU
BEFORE UPDATE ON USER_CARDS FOR EACH ROW
BEGIN
    :NEW.updated_at := SYSTIMESTAMP;
END TRG_USER_CARDS_BU;
/

-- ── 인덱스 ───────────────────────────────────────────────────
CREATE INDEX idx_uc_user_id        ON USER_CARDS(user_id);
CREATE INDEX idx_uc_credit_app_id  ON USER_CARDS(credit_app_id);
CREATE INDEX idx_uc_check_app_id   ON USER_CARDS(check_app_id);
CREATE INDEX idx_uc_linked_account ON USER_CARDS(linked_account_id);
CREATE INDEX idx_uc_card_status    ON USER_CARDS(card_status);

-- ── 컬럼 코멘트 ─────────────────────────────────────────────
COMMENT ON TABLE  USER_CARDS                          IS '실제 발급된 카드 정보';
COMMENT ON COLUMN USER_CARDS.user_card_id             IS '발급 카드 고유 식별자 (PK)';
COMMENT ON COLUMN USER_CARDS.user_id                  IS '카드 소유 사용자 ID (FK: USERS)';
COMMENT ON COLUMN USER_CARDS.version_id               IS '신청 시점 PUBLISHED 카드 버전 (FK)';
COMMENT ON COLUMN USER_CARDS.credit_app_id            IS '신용카드 신청 ID (FK). 체크카드 발급 시 NULL';
COMMENT ON COLUMN USER_CARDS.check_app_id             IS '체크카드 신청 ID (FK). 신용카드 발급 시 NULL';
COMMENT ON COLUMN USER_CARDS.encrypted_card_number    IS 'AES 암호화된 실제 16자리 카드번호';
COMMENT ON COLUMN USER_CARDS.masked_card_number       IS '화면 표시용 마스킹 카드번호 (예: 1234-56**-****-7890)';
COMMENT ON COLUMN USER_CARDS.issue_date               IS '발급일';
COMMENT ON COLUMN USER_CARDS.expire_date              IS '만료일';
COMMENT ON COLUMN USER_CARDS.card_status              IS '카드 상태: ACTIVE / LOST / STOPPED / EXPIRED / REISSUED';
COMMENT ON COLUMN USER_CARDS.usable_yn                IS '사용 가능 여부 Y/N';
COMMENT ON COLUMN USER_CARDS.card_password_hash       IS '카드 비밀번호 BCrypt 해시';
COMMENT ON COLUMN USER_CARDS.linked_account_id        IS '결제 연결 계좌 ID (체크카드 필수, 신용카드 선택)';
COMMENT ON COLUMN USER_CARDS.daily_limit_amount       IS '일 한도(원). 발급 시 100만원 기본, 이후 변경 가능';
COMMENT ON COLUMN USER_CARDS.monthly_limit_amount     IS '월 한도(원). 신용카드=승인한도로 세팅, 체크카드=NULL';
COMMENT ON COLUMN USER_CARDS.card_brand               IS '카드 브랜드: VISA / MASTER / LOCAL / AMEX / UPI';
COMMENT ON COLUMN USER_CARDS.card_design_id           IS '신청 시 선택한 카드 디자인 ID (FK: CARD_IMAGES)';
COMMENT ON COLUMN USER_CARDS.payment_day              IS '월 결제일 (1~31)';
COMMENT ON COLUMN USER_CARDS.combined_transit_yn      IS '교통카드 결합 여부 Y/N';
COMMENT ON COLUMN USER_CARDS.tx_alert_type            IS '거래 알림 방식: SMS / PUSH / NONE';
COMMENT ON COLUMN USER_CARDS.statement_method         IS '명세서 수령 방법: EMAIL / APP / PAPER';
COMMENT ON COLUMN USER_CARDS.overseas_enabled_yn      IS '해외 사용 가능 여부 Y/N (발급 후 변경 가능)';
COMMENT ON COLUMN USER_CARDS.contactless_enabled_yn   IS '비접촉(NFC) 결제 가능 여부 Y/N (발급 후 변경 가능)';
COMMENT ON COLUMN USER_CARDS.card_nickname            IS '사용자 지정 카드 별칭';
COMMENT ON COLUMN USER_CARDS.issued_by                IS '발급 처리 관리자 ID';
COMMENT ON COLUMN USER_CARDS.issued_at                IS '발급 처리 일시 (DEFAULT SYSTIMESTAMP)';
COMMENT ON COLUMN USER_CARDS.updated_at               IS '최종 수정 일시';
COMMENT ON COLUMN USER_CARDS.deleted_yn               IS '논리 삭제 여부 Y/N';
COMMENT ON COLUMN USER_CARDS.deleted_at               IS '논리 삭제 일시';

commit;