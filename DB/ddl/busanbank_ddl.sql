-- ================================================================
-- BNK 부산은행 금융 상품 플랫폼 — 전체 DDL
-- 생성일: 2025-05-14
-- DB    : Oracle 21c
-- 카드 PK 규칙:
--   자리 1-3 : 카드 대분류  (CREDIT=101 / CHECK=102 / PREPAID=103 / HYBRID=104)
--   자리 4-5 : 카드사 코드  (BNK부산은행=01)
--   자리 6-8 : 상품 일련번호 (001~999)
--   예) 10101001 = 신용카드 / BNK부산은행 / 상품001
--       10201001 = 체크카드 / BNK부산은행 / 상품001
--       10301001 = 선불카드 / BNK부산은행 / 상품001
-- ================================================================

-- ================================================================
-- 01. COMMON_CODE_GROUPS  (PK: VARCHAR2 — 시퀀스 불필요)
-- ================================================================
CREATE TABLE COMMON_CODE_GROUPS (
    group_code          VARCHAR2(50)   PRIMARY KEY,
    group_name          VARCHAR2(100)  NOT NULL,
    description         VARCHAR2(1000),
    use_yn              CHAR(1)        DEFAULT 'Y' CHECK (use_yn IN ('Y','N')),
    created_at          TIMESTAMP      DEFAULT SYSTIMESTAMP,
    created_by          NUMBER(10),
    updated_at          TIMESTAMP,
    updated_by          NUMBER(10),
    deleted_yn          CHAR(1)        DEFAULT 'N' CHECK (deleted_yn IN ('Y','N')),
    deleted_at          TIMESTAMP
);
COMMENT ON TABLE COMMON_CODE_GROUPS IS '공통 코드 그룹';

-- ================================================================
-- 02. COMMON_CODES
-- ================================================================
CREATE TABLE COMMON_CODES (
    code_id             NUMBER(10)     PRIMARY KEY,
    group_code          VARCHAR2(50)   NOT NULL,
    code                VARCHAR2(50)   NOT NULL,
    code_name           VARCHAR2(100)  NOT NULL,
    code_value          VARCHAR2(200),
    display_order       NUMBER(5),
    description         VARCHAR2(1000),
    use_yn              CHAR(1)        DEFAULT 'Y' CHECK (use_yn IN ('Y','N')),
    created_at          TIMESTAMP      DEFAULT SYSTIMESTAMP,
    created_by          NUMBER(10),
    updated_at          TIMESTAMP,
    updated_by          NUMBER(10),
    deleted_yn          CHAR(1)        DEFAULT 'N' CHECK (deleted_yn IN ('Y','N')),
    deleted_at          TIMESTAMP,
    CONSTRAINT FK_COMMON_CODES_GROUP
        FOREIGN KEY (group_code) REFERENCES COMMON_CODE_GROUPS(group_code)
);
COMMENT ON TABLE COMMON_CODES IS '공통 코드 상세';

-- ================================================================
-- 03. ADMIN_USERS
-- ================================================================
CREATE TABLE ADMIN_USERS (
    admin_id            NUMBER(10)     PRIMARY KEY,
    username            VARCHAR2(50)   NOT NULL UNIQUE,
    password_hash       VARCHAR2(255)  NOT NULL,
    name                VARCHAR2(50)   NOT NULL,
    email               VARCHAR2(100),
    phone               VARCHAR2(20),
    status_code         VARCHAR2(50)   DEFAULT 'ACTIVE',
    login_fail_count    NUMBER(5)      DEFAULT 0,
    locked_until        TIMESTAMP,
    last_login_at       TIMESTAMP,
    created_at          TIMESTAMP      DEFAULT SYSTIMESTAMP,
    created_by          NUMBER(10),
    updated_at          TIMESTAMP,
    updated_by          NUMBER(10),
    deleted_yn          CHAR(1)        DEFAULT 'N' CHECK (deleted_yn IN ('Y','N')),
    deleted_at          TIMESTAMP
);
COMMENT ON TABLE ADMIN_USERS IS '관리자 계정';

-- ================================================================
-- 04. ADMIN_ROLES
-- ================================================================
CREATE TABLE ADMIN_ROLES (
    role_id             NUMBER(10)     PRIMARY KEY,
    role_code           VARCHAR2(50)   NOT NULL UNIQUE,
    role_name           VARCHAR2(100)  NOT NULL,
    description         VARCHAR2(1000),
    created_at          TIMESTAMP      DEFAULT SYSTIMESTAMP,
    created_by          NUMBER(10),
    updated_at          TIMESTAMP,
    updated_by          NUMBER(10),
    deleted_yn          CHAR(1)        DEFAULT 'N' CHECK (deleted_yn IN ('Y','N')),
    deleted_at          TIMESTAMP
);
COMMENT ON TABLE ADMIN_ROLES IS '관리자 역할';

-- ================================================================
-- 05. ADMIN_PERMISSIONS
-- ================================================================
CREATE TABLE ADMIN_PERMISSIONS (
    permission_id       NUMBER(10)     PRIMARY KEY,
    permission_code     VARCHAR2(100)  NOT NULL UNIQUE,
    permission_name     VARCHAR2(200)  NOT NULL,
    description         VARCHAR2(1000),
    created_at          TIMESTAMP      DEFAULT SYSTIMESTAMP,
    created_by          NUMBER(10),
    updated_at          TIMESTAMP,
    updated_by          NUMBER(10),
    deleted_yn          CHAR(1)        DEFAULT 'N' CHECK (deleted_yn IN ('Y','N')),
    deleted_at          TIMESTAMP
);
COMMENT ON TABLE ADMIN_PERMISSIONS IS '관리자 권한';

-- ================================================================
-- 06. ROLE_PERMISSIONS
-- ================================================================
CREATE TABLE ROLE_PERMISSIONS (
    role_permission_id  NUMBER(10)     PRIMARY KEY,
    role_id             NUMBER(10)     NOT NULL,
    permission_id       NUMBER(10)     NOT NULL,
    created_at          TIMESTAMP      DEFAULT SYSTIMESTAMP,
    created_by          NUMBER(10),
    deleted_yn          CHAR(1)        DEFAULT 'N' CHECK (deleted_yn IN ('Y','N')),
    deleted_at          TIMESTAMP,
    CONSTRAINT FK_ROLE_PERM_ROLE   FOREIGN KEY (role_id)       REFERENCES ADMIN_ROLES(role_id),
    CONSTRAINT FK_ROLE_PERM_PERM   FOREIGN KEY (permission_id) REFERENCES ADMIN_PERMISSIONS(permission_id)
);
COMMENT ON TABLE ROLE_PERMISSIONS IS '역할별 권한 매핑';

-- ================================================================
-- 07. ADMIN_USER_ROLES
-- ================================================================
CREATE TABLE ADMIN_USER_ROLES (
    admin_user_role_id  NUMBER(10)     PRIMARY KEY,
    admin_id            NUMBER(10)     NOT NULL,
    role_id             NUMBER(10)     NOT NULL,
    assigned_at         TIMESTAMP      DEFAULT SYSTIMESTAMP,
    assigned_by         NUMBER(10),
    deleted_yn          CHAR(1)        DEFAULT 'N' CHECK (deleted_yn IN ('Y','N')),
    deleted_at          TIMESTAMP,
    CONSTRAINT FK_ADMIN_USER_ROLE_ADMIN  FOREIGN KEY (admin_id) REFERENCES ADMIN_USERS(admin_id),
    CONSTRAINT FK_ADMIN_USER_ROLE_ROLE   FOREIGN KEY (role_id)  REFERENCES ADMIN_ROLES(role_id)
);
COMMENT ON TABLE ADMIN_USER_ROLES IS '관리자 역할 매핑';

-- ================================================================
-- 08. USERS
-- ================================================================
CREATE TABLE USERS (
    user_id                     NUMBER(10)     PRIMARY KEY,
    email                       VARCHAR2(100)  NOT NULL UNIQUE,
    password_hash               VARCHAR2(255)  NOT NULL,
    name                        VARCHAR2(50)   NOT NULL,
    phone                       VARCHAR2(20),
    birth_date                  DATE,
    ci_value                    VARCHAR2(200),
    job                         VARCHAR2(50),
    income_level_code           VARCHAR2(50),
    credit_score                NUMBER(4)      DEFAULT 0,
    status_code                 VARCHAR2(50)   DEFAULT 'ACTIVE',
    login_fail_count            NUMBER(5)      DEFAULT 0,
    locked_until                TIMESTAMP,
    last_login_at               TIMESTAMP,
    last_password_changed_at    TIMESTAMP,
    is_email_verified           CHAR(1)        DEFAULT 'N' CHECK (is_email_verified  IN ('Y','N')),
    is_phone_verified           CHAR(1)        DEFAULT 'N' CHECK (is_phone_verified  IN ('Y','N')),
    push_enabled                CHAR(1)        DEFAULT 'Y' CHECK (push_enabled       IN ('Y','N')),
    marketing_agree             CHAR(1)        DEFAULT 'N' CHECK (marketing_agree    IN ('Y','N')),
    privacy_agree               CHAR(1)        DEFAULT 'Y' CHECK (privacy_agree      IN ('Y','N')),
    dormant_at                  TIMESTAMP,
    withdrawn_at                TIMESTAMP,
    created_at                  TIMESTAMP      DEFAULT SYSTIMESTAMP,
    created_by                  NUMBER(10),
    updated_at                  TIMESTAMP,
    updated_by                  NUMBER(10),
    deleted_yn                  CHAR(1)        DEFAULT 'N' CHECK (deleted_yn IN ('Y','N')),
    deleted_at                  TIMESTAMP
);
COMMENT ON TABLE USERS IS '일반 사용자 회원 정보';

-- ================================================================
-- 09. USER_SESSIONS
-- ================================================================
CREATE TABLE USER_SESSIONS (
    session_id          NUMBER(10)     PRIMARY KEY,
    user_id             NUMBER(10)     NOT NULL,
    refresh_token       VARCHAR2(1000) NOT NULL,
    device_info         VARCHAR2(500),
    ip_address          VARCHAR2(100),
    user_agent          VARCHAR2(1000),
    revoked_yn          CHAR(1)        DEFAULT 'N' CHECK (revoked_yn IN ('Y','N')),
    revoked_at          TIMESTAMP,
    expires_at          TIMESTAMP      NOT NULL,
    created_at          TIMESTAMP      DEFAULT SYSTIMESTAMP,
    CONSTRAINT FK_USER_SESSIONS_USER FOREIGN KEY (user_id) REFERENCES USERS(user_id)
);
COMMENT ON TABLE USER_SESSIONS IS '사용자 로그인 세션';

-- ================================================================
-- 10. LOGIN_HISTORIES
-- ================================================================
CREATE TABLE LOGIN_HISTORIES (
    history_id          NUMBER(10)     PRIMARY KEY,
    user_type_code      VARCHAR2(50),
    user_id             NUMBER(10),
    login_result_code   VARCHAR2(50),
    fail_reason         VARCHAR2(500),
    ip_address          VARCHAR2(100),
    device_info         VARCHAR2(500),
    user_agent          VARCHAR2(1000),
    login_at            TIMESTAMP      DEFAULT SYSTIMESTAMP
);
COMMENT ON TABLE LOGIN_HISTORIES IS '로그인 시도 이력';

-- ================================================================
-- 11. AUDIT_LOGS
-- ================================================================
CREATE TABLE AUDIT_LOGS (
    audit_log_id        NUMBER(10)     PRIMARY KEY,
    actor_type_code     VARCHAR2(50),
    actor_id            NUMBER(10),
    action_type_code    VARCHAR2(100),
    target_type_code    VARCHAR2(100),
    target_id           NUMBER(10),
    description         VARCHAR2(4000),
    ip_address          VARCHAR2(100),
    user_agent          VARCHAR2(1000),
    created_at          TIMESTAMP      DEFAULT SYSTIMESTAMP
);
COMMENT ON TABLE AUDIT_LOGS IS '감사 로그';

-- ================================================================
-- 12. APPROVAL_REQUESTS
-- ================================================================
CREATE TABLE APPROVAL_REQUESTS (
    approval_id             NUMBER(10)     PRIMARY KEY,
    request_type_code       VARCHAR2(50),
    requester_admin_id      NUMBER(10)     NOT NULL,
    target_id               NUMBER(10),
    status_code             VARCHAR2(50),
    request_comment         VARCHAR2(2000),
    requested_at            TIMESTAMP      DEFAULT SYSTIMESTAMP,
    completed_at            TIMESTAMP,
    CONSTRAINT FK_APPROVAL_REQUEST_ADMIN
        FOREIGN KEY (requester_admin_id) REFERENCES ADMIN_USERS(admin_id)
);
COMMENT ON TABLE APPROVAL_REQUESTS IS '관리자 결재 요청';

-- ================================================================
-- 13. APPROVAL_LINES
-- ================================================================
CREATE TABLE APPROVAL_LINES (
    approval_line_id    NUMBER(10)     PRIMARY KEY,
    approval_id         NUMBER(10)     NOT NULL,
    approver_admin_id   NUMBER(10)     NOT NULL,
    approval_order      NUMBER(3)      NOT NULL,
    status_code         VARCHAR2(50),
    comment_text        VARCHAR2(2000),
    approved_at         TIMESTAMP,
    CONSTRAINT FK_APPROVAL_LINE_APPROVAL  FOREIGN KEY (approval_id)        REFERENCES APPROVAL_REQUESTS(approval_id),
    CONSTRAINT FK_APPROVAL_LINE_ADMIN     FOREIGN KEY (approver_admin_id)  REFERENCES ADMIN_USERS(admin_id)
);
COMMENT ON TABLE APPROVAL_LINES IS '결재 승인 라인';

-- ================================================================
-- 14. CARD_CATEGORIES
-- ================================================================
CREATE TABLE CARD_CATEGORIES (
    category_id         NUMBER(19)     PRIMARY KEY,
    category_code       VARCHAR2(50)   NOT NULL UNIQUE,
    category_name       VARCHAR2(100)  NOT NULL,
    icon_code           VARCHAR2(100),
    display_order       NUMBER(5),
    use_yn              CHAR(1)        DEFAULT 'Y' CHECK (use_yn IN ('Y','N')),
    created_at          TIMESTAMP      DEFAULT SYSTIMESTAMP
);
COMMENT ON TABLE CARD_CATEGORIES IS '카드 혜택 카테고리';

-- ================================================================
-- 15. CARDS
--     PK card_id: 8자리 구조화 숫자
--       1~3자리  카드 대분류  (CREDIT=101 / CHECK=102 / PREPAID=103 / HYBRID=104)
--       4~5자리  카드사 코드  (BNK부산은행=01)
--       6~8자리  상품 일련번호 (001~999)
--     card_type: PREPAID 추가 (선불카드 지원)
-- ================================================================
CREATE TABLE CARDS (
    card_id                     NUMBER(10)     PRIMARY KEY,
    -- PK 구조: [type_code(3)][company_code(2)][serial(3)]
    -- 예) CREDIT/BNK/001 → 10101001

    card_code                   VARCHAR2(50)   NOT NULL UNIQUE,
    card_type                   VARCHAR2(20)   NOT NULL
                                    CHECK (card_type IN ('CREDIT','CHECK','PREPAID','HYBRID')),
    -- PREPAID 추가: 선불카드(부산 동백전 등)

    card_name                   VARCHAR2(200)  NOT NULL,
    company_name                VARCHAR2(100)  NOT NULL,
    company_code                VARCHAR2(10)   DEFAULT '01' NOT NULL,
    -- company_code: BNK부산은행=01 (카드 PK 4~5자리 기준)

    brand_name                  VARCHAR2(50)
                                    CHECK (brand_name IN ('VISA','MASTER','AMEX','UNIONPAY','LOCAL')),
    annual_fee_domestic         NUMBER(10)     DEFAULT 0 NOT NULL CHECK (annual_fee_domestic  >= 0),
    annual_fee_overseas         NUMBER(10)     DEFAULT 0 NOT NULL CHECK (annual_fee_overseas   >= 0),
    previous_month_spend        NUMBER(12)     DEFAULT 0 NOT NULL CHECK (previous_month_spend  >= 0),
    minimum_age                 NUMBER(3)                        CHECK (minimum_age            >= 0),
    maximum_age                 NUMBER(3)                        CHECK (maximum_age            >= minimum_age),
    credit_limit_min            NUMBER(15)                       CHECK (credit_limit_min       >= 0),
    credit_limit_max            NUMBER(15)                       CHECK (credit_limit_max       >= credit_limit_min),
    target_user                 VARCHAR2(300),
    summary_description         VARCHAR2(1000),
    searchable_yn               CHAR(1)        DEFAULT 'Y' CHECK (searchable_yn        IN ('Y','N')),
    visible_yn                  CHAR(1)        DEFAULT 'Y' CHECK (visible_yn            IN ('Y','N')),
    approval_required_yn        CHAR(1)        DEFAULT 'Y' CHECK (approval_required_yn  IN ('Y','N')),
    card_status                 VARCHAR2(30)   DEFAULT 'DRAFT'
                                    CHECK (card_status IN ('DRAFT','REVIEW','APPROVED','PUBLISHED','STOPPED','EXPIRED')),
    publish_start_at            TIMESTAMP,
    publish_end_at              TIMESTAMP,
    application_count           NUMBER(10)     DEFAULT 0 CHECK (application_count >= 0),
    created_by                  NUMBER(19),
    created_at                  TIMESTAMP      DEFAULT SYSTIMESTAMP,
    updated_by                  NUMBER(19),
    updated_at                  TIMESTAMP,
    deleted_yn                  CHAR(1)        DEFAULT 'N' CHECK (deleted_yn IN ('Y','N')),
    deleted_at                  TIMESTAMP,
    CONSTRAINT CHK_CARDS_PUBLISH_DATE
        CHECK (publish_end_at IS NULL OR publish_end_at >= publish_start_at)
);
COMMENT ON TABLE CARDS IS '카드 상품 기본 정보';
COMMENT ON COLUMN CARDS.card_id     IS '카드 PK: [대분류(3)][카드사코드(2)][일련번호(3)] — CREDIT/BNK/001=10101001';
COMMENT ON COLUMN CARDS.company_code IS '카드사 코드: BNK부산은행=01 (PK 4~5번째 자리)';
COMMENT ON COLUMN CARDS.card_type   IS 'CREDIT(신용) / CHECK(체크) / PREPAID(선불) / HYBRID(하이브리드)';

-- ================================================================
-- 16. CARD_BENEFITS
-- ================================================================
CREATE TABLE CARD_BENEFITS (
    benefit_id                  NUMBER(19)     PRIMARY KEY,
    card_id                     NUMBER(10)     NOT NULL,
    category_id                 NUMBER(19)     NOT NULL,
    benefit_title               VARCHAR2(200)  NOT NULL,
    benefit_type                VARCHAR2(30)   NOT NULL
                                    CHECK (benefit_type IN ('RATE_DISCOUNT','FIXED_DISCOUNT','POINT','CASHBACK','FREE')),
    discount_rate               NUMBER(5,4),
    discount_amount             NUMBER(12),
    point_rate                  NUMBER(5,4),
    cashback_rate               NUMBER(5,4),
    monthly_limit_amount        NUMBER(12),
    daily_limit_amount          NUMBER(12),
    minimum_payment_amount      NUMBER(12),
    benefit_condition           CLOB,
    display_text                VARCHAR2(300),
    display_order               NUMBER(5),
    visible_yn                  CHAR(1)        DEFAULT 'Y' CHECK (visible_yn IN ('Y','N')),
    created_at                  TIMESTAMP      DEFAULT SYSTIMESTAMP,
    updated_at                  TIMESTAMP,
    CONSTRAINT FK_CARD_BENEFITS_CARD      FOREIGN KEY (card_id)     REFERENCES CARDS(card_id)          ON DELETE CASCADE,
    CONSTRAINT FK_CARD_BENEFITS_CATEGORY  FOREIGN KEY (category_id) REFERENCES CARD_CATEGORIES(category_id)
);
COMMENT ON TABLE CARD_BENEFITS IS '카드 혜택 정보';

-- ================================================================
-- 17. CARD_IMAGES
-- ================================================================
CREATE TABLE CARD_IMAGES (
    image_id            NUMBER(19)     PRIMARY KEY,
    card_id             NUMBER(10)     NOT NULL,
    image_type          VARCHAR2(30)   NOT NULL
                            CHECK (image_type IN ('FRONT','BACK','THUMBNAIL','DETAIL')),
    image_url           VARCHAR2(1000) NOT NULL,
    original_name       VARCHAR2(300),
    stored_name         VARCHAR2(300),
    file_size           NUMBER(15),
    mime_type           VARCHAR2(100),
    image_width         NUMBER(5),
    image_height        NUMBER(5),
    sort_order          NUMBER(5)      DEFAULT 1,
    created_at          TIMESTAMP      DEFAULT SYSTIMESTAMP,
    CONSTRAINT FK_CARD_IMAGES_CARD FOREIGN KEY (card_id) REFERENCES CARDS(card_id) ON DELETE CASCADE
);
COMMENT ON TABLE CARD_IMAGES IS '카드 이미지';

-- ================================================================
-- 18. CARD_CONTENTS
-- ================================================================
CREATE TABLE CARD_CONTENTS (
    content_id          NUMBER(19)     PRIMARY KEY,
    card_id             NUMBER(10)     NOT NULL,
    content_type        VARCHAR2(50)   NOT NULL
                            CHECK (content_type IN ('INTRO','GUIDE','NOTICE','FAQ','EVENT')),
    title               VARCHAR2(300)  NOT NULL,
    content_html        CLOB,
    mobile_content_html CLOB,
    display_order       NUMBER(5),
    visible_yn          CHAR(1)        DEFAULT 'Y' CHECK (visible_yn IN ('Y','N')),
    created_by          NUMBER(19),
    created_at          TIMESTAMP      DEFAULT SYSTIMESTAMP,
    updated_at          TIMESTAMP,
    CONSTRAINT FK_CARD_CONTENTS_CARD FOREIGN KEY (card_id) REFERENCES CARDS(card_id) ON DELETE CASCADE
);
COMMENT ON TABLE CARD_CONTENTS IS '카드 상세 콘텐츠';

-- ================================================================
-- 19. CARD_APPLICATIONS
-- ================================================================
CREATE TABLE CARD_APPLICATIONS (
    application_id      NUMBER(19)     PRIMARY KEY,
    user_id             NUMBER(19)     NOT NULL,
    card_id             NUMBER(10)     NOT NULL,
    application_status  VARCHAR2(30)   DEFAULT 'REQUESTED'
                            CHECK (application_status IN ('REQUESTED','REVIEWING','APPROVED','REJECTED','ISSUED')),
    apply_channel       VARCHAR2(30)   CHECK (apply_channel IN ('WEB','MOBILE','ADMIN')),
    requested_limit     NUMBER(15),
    approved_limit      NUMBER(15),
    rejection_reason    VARCHAR2(1000),
    application_comment VARCHAR2(2000),
    applied_at          TIMESTAMP      DEFAULT SYSTIMESTAMP,
    reviewed_at         TIMESTAMP,
    reviewed_by         NUMBER(19),
    created_at          TIMESTAMP      DEFAULT SYSTIMESTAMP,
    updated_at          TIMESTAMP,
    CONSTRAINT FK_CARD_APPLICATIONS_USER  FOREIGN KEY (user_id)  REFERENCES USERS(user_id),
    CONSTRAINT FK_CARD_APPLICATIONS_CARD  FOREIGN KEY (card_id)  REFERENCES CARDS(card_id)
);
COMMENT ON TABLE CARD_APPLICATIONS IS '카드 신청 정보';

-- ================================================================
-- 20. CARD_ATTRIBUTE_DEFINITIONS
-- ================================================================
CREATE TABLE CARD_ATTRIBUTE_DEFINITIONS (
    attribute_id        NUMBER(19)     PRIMARY KEY,
    attribute_code      VARCHAR2(50)   NOT NULL UNIQUE,
    attribute_name      VARCHAR2(100)  NOT NULL,
    attribute_type      VARCHAR2(30)   NOT NULL
                            CHECK (attribute_type IN ('TEXT','NUMBER','BOOLEAN','JSON')),
    description         VARCHAR2(1000),
    use_yn              CHAR(1)        DEFAULT 'Y' CHECK (use_yn IN ('Y','N')),
    created_at          TIMESTAMP      DEFAULT SYSTIMESTAMP
);
COMMENT ON TABLE CARD_ATTRIBUTE_DEFINITIONS IS '카드 속성 정의';

-- ================================================================
-- 21. CARD_ATTRIBUTE_VALUES
-- ================================================================
CREATE TABLE CARD_ATTRIBUTE_VALUES (
    card_attribute_value_id  NUMBER(19)  PRIMARY KEY,
    card_id                  NUMBER(10)  NOT NULL,
    attribute_id             NUMBER(19)  NOT NULL,
    attribute_value          VARCHAR2(4000),
    display_text             VARCHAR2(500),
    created_at               TIMESTAMP   DEFAULT SYSTIMESTAMP,
    CONSTRAINT UK_CARD_ATTR_VALUE      UNIQUE(card_id, attribute_id),
    CONSTRAINT FK_CARD_ATTR_VALUE_CARD FOREIGN KEY (card_id)      REFERENCES CARDS(card_id) ON DELETE CASCADE,
    CONSTRAINT FK_CARD_ATTR_VALUE_ATTR FOREIGN KEY (attribute_id) REFERENCES CARD_ATTRIBUTE_DEFINITIONS(attribute_id)
);
COMMENT ON TABLE CARD_ATTRIBUTE_VALUES IS '카드 속성 값';

-- ================================================================
-- 22. CARD_TAGS
-- ================================================================
CREATE TABLE CARD_TAGS (
    tag_id      NUMBER(19)  PRIMARY KEY,
    tag_name    VARCHAR2(100) NOT NULL UNIQUE,
    created_at  TIMESTAMP   DEFAULT SYSTIMESTAMP
);
COMMENT ON TABLE CARD_TAGS IS '카드 태그';

-- ================================================================
-- 23. CARD_TAG_MAP
-- ================================================================
CREATE TABLE CARD_TAG_MAP (
    card_tag_map_id  NUMBER(19)  PRIMARY KEY,
    card_id          NUMBER(10)  NOT NULL,
    tag_id           NUMBER(19)  NOT NULL,
    created_at       TIMESTAMP   DEFAULT SYSTIMESTAMP,
    CONSTRAINT UK_CARD_TAG_MAP        UNIQUE(card_id, tag_id),
    CONSTRAINT FK_CARD_TAG_MAP_CARD   FOREIGN KEY (card_id) REFERENCES CARDS(card_id)     ON DELETE CASCADE,
    CONSTRAINT FK_CARD_TAG_MAP_TAG    FOREIGN KEY (tag_id)  REFERENCES CARD_TAGS(tag_id)
);
COMMENT ON TABLE CARD_TAG_MAP IS '카드 태그 연결';

-- ================================================================
-- 24. CARD_PROMOTIONS
-- ================================================================
CREATE TABLE CARD_PROMOTIONS (
    promotion_id        NUMBER(19)     PRIMARY KEY,
    card_id             NUMBER(10)     NOT NULL,
    promotion_title     VARCHAR2(300)  NOT NULL,
    promotion_summary   VARCHAR2(1000),
    promotion_content   CLOB,
    banner_image_url    VARCHAR2(1000),
    start_date          DATE           NOT NULL,
    end_date            DATE,
    promotion_status    VARCHAR2(30)   DEFAULT 'READY'
                            CHECK (promotion_status IN ('READY','ACTIVE','ENDED','STOPPED')),
    visible_yn          CHAR(1)        DEFAULT 'Y' CHECK (visible_yn IN ('Y','N')),
    created_by          NUMBER(19),
    created_at          TIMESTAMP      DEFAULT SYSTIMESTAMP,
    updated_at          TIMESTAMP,
    CONSTRAINT CHK_CARD_PROMOTION_DATE  CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT FK_CARD_PROMOTION_CARD   FOREIGN KEY (card_id) REFERENCES CARDS(card_id) ON DELETE CASCADE
);
COMMENT ON TABLE CARD_PROMOTIONS IS '카드 이벤트 및 프로모션';

-- ================================================================
-- 25. CARD_STATUS_HISTORIES
-- ================================================================
CREATE TABLE CARD_STATUS_HISTORIES (
    history_id          NUMBER(19)     PRIMARY KEY,
    card_id             NUMBER(10)     NOT NULL,
    previous_status     VARCHAR2(30),
    changed_status      VARCHAR2(30)   NOT NULL,
    changed_by          NUMBER(19),
    changed_reason      VARCHAR2(1000),
    changed_at          TIMESTAMP      DEFAULT SYSTIMESTAMP,
    CONSTRAINT FK_CARD_STATUS_HISTORY_CARD FOREIGN KEY (card_id) REFERENCES CARDS(card_id)
);
COMMENT ON TABLE CARD_STATUS_HISTORIES IS '카드 상태 변경 이력';

-- ================================================================
-- 26. CARD_VERSIONS
-- ================================================================
CREATE TABLE CARD_VERSIONS (
    version_id          NUMBER(19)     PRIMARY KEY,
    card_id             NUMBER(10)     NOT NULL,
    version_no          VARCHAR2(30)   NOT NULL,
    version_status      VARCHAR2(30)   DEFAULT 'DRAFT'
                            CHECK (version_status IN ('DRAFT','REVIEW','APPROVED','PUBLISHED','ARCHIVED')),
    snapshot_json       CLOB           NOT NULL,
    change_summary      VARCHAR2(2000),
    created_by          NUMBER(19),
    created_at          TIMESTAMP      DEFAULT SYSTIMESTAMP,
    approved_by         NUMBER(19),
    approved_at         TIMESTAMP,
    published_at        TIMESTAMP,
    CONSTRAINT FK_CARD_VERSIONS_CARD FOREIGN KEY (card_id) REFERENCES CARDS(card_id)
);
COMMENT ON TABLE CARD_VERSIONS IS '카드 버전 이력 (결재 스냅샷)';

-- ================================================================
-- 27. USER_CARDS
-- ================================================================
CREATE TABLE USER_CARDS (
    user_card_id            NUMBER(19)     PRIMARY KEY,
    user_id                 NUMBER(19)     NOT NULL,
    card_id                 NUMBER(10)     NOT NULL,
    application_id          NUMBER(19),
    masked_card_number      VARCHAR2(30)   NOT NULL,
    card_nickname           VARCHAR2(100),
    issue_date              DATE           NOT NULL,
    expire_date             DATE           NOT NULL,
    card_status             VARCHAR2(30)   DEFAULT 'ACTIVE'
                                CHECK (card_status IN ('ACTIVE','LOST','STOPPED','EXPIRED','REISSUED')),
    usable_yn               CHAR(1)        DEFAULT 'Y' CHECK (usable_yn            IN ('Y','N')),
    daily_limit_amount      NUMBER(15),
    monthly_limit_amount    NUMBER(15),
    overseas_enabled_yn     CHAR(1)        DEFAULT 'Y' CHECK (overseas_enabled_yn  IN ('Y','N')),
    contactless_enabled_yn  CHAR(1)        DEFAULT 'Y' CHECK (contactless_enabled_yn IN ('Y','N')),
    issued_by               NUMBER(19),
    issued_at               TIMESTAMP      DEFAULT SYSTIMESTAMP,
    updated_at              TIMESTAMP,
    deleted_yn              CHAR(1)        DEFAULT 'N' CHECK (deleted_yn IN ('Y','N')),
    deleted_at              TIMESTAMP,
    CONSTRAINT CHK_USER_CARD_DATE      CHECK (expire_date >= issue_date),
    CONSTRAINT FK_USER_CARDS_USER      FOREIGN KEY (user_id)        REFERENCES USERS(user_id),
    CONSTRAINT FK_USER_CARDS_CARD      FOREIGN KEY (card_id)        REFERENCES CARDS(card_id),
    CONSTRAINT FK_USER_CARDS_APPL      FOREIGN KEY (application_id) REFERENCES CARD_APPLICATIONS(application_id)
);
COMMENT ON TABLE USER_CARDS IS '실제 발급 카드';

-- ================================================================
-- 28. MERCHANT_CATEGORY_MAP
-- ================================================================
CREATE TABLE MERCHANT_CATEGORY_MAP (
    map_id          NUMBER(19)     PRIMARY KEY,
    keyword         VARCHAR2(100)  NOT NULL,
    category_id     NUMBER(19)     NOT NULL,
    priority        NUMBER(5)      DEFAULT 1,
    use_yn          CHAR(1)        DEFAULT 'Y' CHECK (use_yn IN ('Y','N')),
    created_at      TIMESTAMP      DEFAULT SYSTIMESTAMP,
    CONSTRAINT FK_MERCHANT_CATEGORY FOREIGN KEY (category_id) REFERENCES CARD_CATEGORIES(category_id)
);
COMMENT ON TABLE MERCHANT_CATEGORY_MAP IS '가맹점 자동 카테고리 분류';

-- ================================================================
-- 29. TERMS_GROUPS
-- ================================================================
CREATE TABLE TERMS_GROUPS (
    group_id        NUMBER(10)     PRIMARY KEY,
    group_name      VARCHAR2(100)  NOT NULL,
    group_type      VARCHAR2(30)   NOT NULL,
    display_order   NUMBER(5),
    created_at      TIMESTAMP      DEFAULT SYSTIMESTAMP
);
COMMENT ON TABLE TERMS_GROUPS IS '약관 그룹 관리';
COMMENT ON COLUMN TERMS_GROUPS.group_type IS 'REQUIRED / OPTIONAL / NOTICE';

-- ================================================================
-- 30. TERMS_PACKAGES
-- ================================================================
CREATE TABLE TERMS_PACKAGES (
    package_id      NUMBER(10)     PRIMARY KEY,
    package_name    VARCHAR2(200)  NOT NULL,
    package_type    VARCHAR2(50)   NOT NULL,
    description     VARCHAR2(1000),
    created_at      TIMESTAMP      DEFAULT SYSTIMESTAMP
);
COMMENT ON TABLE TERMS_PACKAGES IS '약관 패키지 관리';
COMMENT ON COLUMN TERMS_PACKAGES.package_type IS 'SIGNUP / CARD_APPLY / EVENT';

-- ================================================================
-- 31. TERMS_MASTERS
-- ================================================================
CREATE TABLE TERMS_MASTERS (
    terms_master_id  NUMBER(10)    PRIMARY KEY,
    terms_type       VARCHAR2(50)  NOT NULL,
    title            VARCHAR2(300) NOT NULL,
    description      VARCHAR2(1000),
    created_at       TIMESTAMP     DEFAULT SYSTIMESTAMP
);
COMMENT ON TABLE TERMS_MASTERS IS '약관 원본 정의';
COMMENT ON COLUMN TERMS_MASTERS.terms_type IS 'COMMON / PRIVACY / CARD_SERVICE';

-- ================================================================
-- 32. TERMS
-- ================================================================
CREATE TABLE TERMS (
    terms_id                NUMBER(10)     PRIMARY KEY,
    terms_master_id         NUMBER(10)     NOT NULL,
    version                 VARCHAR2(20)   NOT NULL,
    content_html            CLOB,
    required_yn             CHAR(1)        DEFAULT 'Y' CHECK (required_yn          IN ('Y','N')),
    reconsent_required_yn   CHAR(1)        DEFAULT 'N' CHECK (reconsent_required_yn IN ('Y','N')),
    status                  VARCHAR2(30)   DEFAULT 'DRAFT'
                                CHECK (status IN ('DRAFT','REVIEW','APPROVED','PUBLISHED','EXPIRED')),
    effective_from          DATE           NOT NULL,
    effective_to            DATE,
    document_hash           VARCHAR2(256),
    internal_note           VARCHAR2(2000),
    approved_by             NUMBER(10),
    approved_at             TIMESTAMP,
    created_at              TIMESTAMP      DEFAULT SYSTIMESTAMP,
    updated_at              TIMESTAMP,
    CONSTRAINT FK_TERMS_MASTER FOREIGN KEY (terms_master_id) REFERENCES TERMS_MASTERS(terms_master_id)
);
COMMENT ON TABLE TERMS IS '약관 버전 관리';
COMMENT ON COLUMN TERMS.status IS 'DRAFT → REVIEW → APPROVED → PUBLISHED → EXPIRED';

-- ================================================================
-- 33. PACKAGE_TERMS
-- ================================================================
CREATE TABLE PACKAGE_TERMS (
    package_terms_id  NUMBER(10)   PRIMARY KEY,
    package_id        NUMBER(10)   NOT NULL,
    terms_id          NUMBER(10)   NOT NULL,
    display_order     NUMBER(5),
    created_at        TIMESTAMP    DEFAULT SYSTIMESTAMP,
    CONSTRAINT FK_PACKAGE_TERMS_PACKAGE  FOREIGN KEY (package_id) REFERENCES TERMS_PACKAGES(package_id),
    CONSTRAINT FK_PACKAGE_TERMS_TERMS    FOREIGN KEY (terms_id)   REFERENCES TERMS(terms_id)
);
COMMENT ON TABLE PACKAGE_TERMS IS '패키지 구성 약관';

-- ================================================================
-- 34. TERMS_STATUS_HISTORY
-- ================================================================
CREATE TABLE TERMS_STATUS_HISTORY (
    history_id       NUMBER(10)    PRIMARY KEY,
    terms_id         NUMBER(10)    NOT NULL,
    previous_status  VARCHAR2(30),
    changed_status   VARCHAR2(30)  NOT NULL,
    changed_by       NUMBER(10),
    changed_reason   VARCHAR2(1000),
    changed_at       TIMESTAMP     DEFAULT SYSTIMESTAMP,
    CONSTRAINT FK_TERMS_STATUS_HISTORY FOREIGN KEY (terms_id) REFERENCES TERMS(terms_id)
);
COMMENT ON TABLE TERMS_STATUS_HISTORY IS '약관 상태 변경 이력';

-- ================================================================
-- 35. TERMS_EXPOSURE_RULES
-- ================================================================
CREATE TABLE TERMS_EXPOSURE_RULES (
    rule_id     NUMBER(10)     PRIMARY KEY,
    terms_id    NUMBER(10)     NOT NULL,
    rule_type   VARCHAR2(50)   NOT NULL,
    rule_value  VARCHAR2(1000) NOT NULL,
    created_at  TIMESTAMP      DEFAULT SYSTIMESTAMP,
    CONSTRAINT FK_EXPOSURE_TERMS FOREIGN KEY (terms_id) REFERENCES TERMS(terms_id)
);
COMMENT ON TABLE TERMS_EXPOSURE_RULES IS '약관 노출 정책';
COMMENT ON COLUMN TERMS_EXPOSURE_RULES.rule_type IS 'AGE_MIN / CARD_TYPE / CARD_BRAND';

-- ================================================================
-- 36. TERMS_FILES
-- ================================================================
CREATE TABLE TERMS_FILES (
    file_id         NUMBER(10)     PRIMARY KEY,
    terms_id        NUMBER(10)     NOT NULL,
    file_type       VARCHAR2(30)   NOT NULL,
    file_path       VARCHAR2(1000) NOT NULL,
    original_name   VARCHAR2(300)  NOT NULL,
    stored_name     VARCHAR2(300),
    file_extension  VARCHAR2(20),
    file_size       NUMBER(15),
    mime_type       VARCHAR2(100),
    is_primary      CHAR(1)        DEFAULT 'N' CHECK (is_primary    IN ('Y','N')),
    download_count  NUMBER(10)     DEFAULT 0,
    virus_scan_yn   CHAR(1)        DEFAULT 'N' CHECK (virus_scan_yn IN ('Y','N')),
    uploaded_at     TIMESTAMP      DEFAULT SYSTIMESTAMP,
    CONSTRAINT FK_TERMS_FILES_TERMS FOREIGN KEY (terms_id) REFERENCES TERMS(terms_id)
);
COMMENT ON TABLE TERMS_FILES IS '약관 첨부파일';
COMMENT ON COLUMN TERMS_FILES.file_type IS 'PDF / IMAGE / SUMMARY';

-- ================================================================
-- 37. CARD_TERMS
-- ================================================================
CREATE TABLE CARD_TERMS (
    card_terms_id           NUMBER(10)  PRIMARY KEY,
    card_id                 NUMBER(10)  NOT NULL,
    terms_id                NUMBER(10)  NOT NULL,
    group_id                NUMBER(10),
    required_yn             CHAR(1)     DEFAULT 'Y' CHECK (required_yn IN ('Y','N')),
    exposure_condition_json CLOB,
    display_order           NUMBER(5),
    created_at              TIMESTAMP   DEFAULT SYSTIMESTAMP,
    CONSTRAINT FK_CARD_TERMS_CARD   FOREIGN KEY (card_id)  REFERENCES CARDS(card_id),
    CONSTRAINT FK_CARD_TERMS_TERMS  FOREIGN KEY (terms_id) REFERENCES TERMS(terms_id),
    CONSTRAINT FK_CARD_TERMS_GROUP  FOREIGN KEY (group_id) REFERENCES TERMS_GROUPS(group_id)
);
COMMENT ON TABLE CARD_TERMS IS '카드별 약관 연결';

-- ================================================================
-- 38. USER_TERMS_AGREEMENTS
-- ================================================================
CREATE TABLE USER_TERMS_AGREEMENTS (
    agreement_id            NUMBER(10)     PRIMARY KEY,
    user_id                 NUMBER(10)     NOT NULL,
    terms_id                NUMBER(10)     NOT NULL,
    agreed_yn               CHAR(1)        DEFAULT 'Y' CHECK (agreed_yn IN ('Y','N')),
    agreement_action        VARCHAR2(30)   DEFAULT 'AGREE',
    agreed_version          VARCHAR2(20)   NOT NULL,
    agreement_channel       VARCHAR2(30),
    agreement_source        VARCHAR2(50),
    agreed_content_snapshot CLOB,
    agreed_at               TIMESTAMP      DEFAULT SYSTIMESTAMP,
    ip_address              VARCHAR2(100),
    user_agent              VARCHAR2(1000),
    CONSTRAINT FK_USER_TERMS_USER  FOREIGN KEY (user_id)  REFERENCES USERS(user_id),
    CONSTRAINT FK_USER_TERMS_TERMS FOREIGN KEY (terms_id) REFERENCES TERMS(terms_id)
);
COMMENT ON TABLE USER_TERMS_AGREEMENTS IS '사용자 약관 동의 이력';
COMMENT ON COLUMN USER_TERMS_AGREEMENTS.agreement_action   IS 'AGREE / WITHDRAW / REAGREE';
COMMENT ON COLUMN USER_TERMS_AGREEMENTS.agreement_channel  IS 'WEB / MOBILE / ADMIN / CALL_CENTER';
COMMENT ON COLUMN USER_TERMS_AGREEMENTS.agreement_source   IS 'SIGNUP / CARD_APPLY / EVENT';

-- ================================================================
-- 39. CARD_NOTICE_SECTIONS
-- ================================================================
CREATE TABLE CARD_NOTICE_SECTIONS (
    notice_id       NUMBER(10)     PRIMARY KEY,
    card_id         NUMBER(10)     NOT NULL,
    section_title   VARCHAR2(200)  NOT NULL,
    content_html    CLOB,
    display_order   NUMBER(5),
    created_at      TIMESTAMP      DEFAULT SYSTIMESTAMP,
    CONSTRAINT FK_NOTICE_CARD FOREIGN KEY (card_id) REFERENCES CARDS(card_id)
);
COMMENT ON TABLE CARD_NOTICE_SECTIONS IS '카드 상품 안내/유의사항';

-- ================================================================
-- 40. TERMS_CHANGE_DIFFS
-- ================================================================
CREATE TABLE TERMS_CHANGE_DIFFS (
    diff_id         NUMBER(10)  PRIMARY KEY,
    old_terms_id    NUMBER(10)  NOT NULL,
    new_terms_id    NUMBER(10)  NOT NULL,
    summary_html    CLOB        NOT NULL,
    created_at      TIMESTAMP   DEFAULT SYSTIMESTAMP,
    CONSTRAINT FK_DIFF_OLD_TERMS FOREIGN KEY (old_terms_id) REFERENCES TERMS(terms_id),
    CONSTRAINT FK_DIFF_NEW_TERMS FOREIGN KEY (new_terms_id) REFERENCES TERMS(terms_id)
);
COMMENT ON TABLE TERMS_CHANGE_DIFFS IS '약관 변경 비교';

-- ================================================================
-- 41. TERMS_NOTIFICATION_HISTORY
-- ================================================================
CREATE TABLE TERMS_NOTIFICATION_HISTORY (
    notification_id       NUMBER(10)     PRIMARY KEY,
    user_id               NUMBER(10)     NOT NULL,
    terms_id              NUMBER(10)     NOT NULL,
    notification_type     VARCHAR2(30)   NOT NULL,
    notification_title    VARCHAR2(300),
    notification_message  VARCHAR2(2000),
    sent_yn               CHAR(1)        DEFAULT 'N' CHECK (sent_yn IN ('Y','N')),
    sent_at               TIMESTAMP,
    read_yn               CHAR(1)        DEFAULT 'N' CHECK (read_yn IN ('Y','N')),
    read_at               TIMESTAMP,
    created_at            TIMESTAMP      DEFAULT SYSTIMESTAMP,
    CONSTRAINT FK_NOTIFICATION_USER  FOREIGN KEY (user_id)  REFERENCES USERS(user_id),
    CONSTRAINT FK_NOTIFICATION_TERMS FOREIGN KEY (terms_id) REFERENCES TERMS(terms_id)
);
COMMENT ON TABLE TERMS_NOTIFICATION_HISTORY IS '약관 알림 발송 이력';
COMMENT ON COLUMN TERMS_NOTIFICATION_HISTORY.notification_type IS 'EMAIL / SMS / PUSH / INAPP';

-- ================================================================
-- 42. FILE_UPLOADS
-- ================================================================
CREATE TABLE FILE_UPLOADS (
    file_id                 NUMBER(10)     PRIMARY KEY,
    original_filename       VARCHAR2(500)  NOT NULL,
    stored_filename         VARCHAR2(500)  NOT NULL,
    stored_path             VARCHAR2(1000) NOT NULL,
    file_url                VARCHAR2(1000),
    file_type_code          VARCHAR2(50)   NOT NULL,
    mime_type               VARCHAR2(100),
    file_size               NUMBER(15),
    converted_file_id       NUMBER(10),
    convert_status_code     VARCHAR2(50),
    ref_type_code           VARCHAR2(50),
    ref_id                  NUMBER(10),
    created_at              TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    created_by              NUMBER(10),
    deleted_yn              CHAR(1)        DEFAULT 'N' NOT NULL CHECK (deleted_yn IN ('Y','N')),
    deleted_at              TIMESTAMP,
    CONSTRAINT FK_FU_CONVERTED_FILE  FOREIGN KEY (converted_file_id) REFERENCES FILE_UPLOADS(file_id),
    CONSTRAINT FK_FU_CREATED_BY      FOREIGN KEY (created_by)        REFERENCES ADMIN_USERS(admin_id)
);
COMMENT ON TABLE FILE_UPLOADS IS '파일 업로드 관리';

-- ================================================================
-- 43. SEARCH_KEYWORDS
-- ================================================================
CREATE TABLE SEARCH_KEYWORDS (
    keyword_id      NUMBER(10)     PRIMARY KEY,
    keyword         VARCHAR2(100)  NOT NULL UNIQUE,
    category_id     NUMBER(10),
    use_yn          CHAR(1)        DEFAULT 'Y' NOT NULL CHECK (use_yn    IN ('Y','N')),
    display_order   NUMBER(5),
    created_at      TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    created_by      NUMBER(10),
    updated_at      TIMESTAMP,
    updated_by      NUMBER(10),
    deleted_yn      CHAR(1)        DEFAULT 'N' NOT NULL CHECK (deleted_yn IN ('Y','N')),
    deleted_at      TIMESTAMP,
    CONSTRAINT FK_SK_CATEGORY    FOREIGN KEY (category_id) REFERENCES CARD_CATEGORIES(category_id),
    CONSTRAINT FK_SK_CREATED_BY  FOREIGN KEY (created_by)  REFERENCES ADMIN_USERS(admin_id),
    CONSTRAINT FK_SK_UPDATED_BY  FOREIGN KEY (updated_by)  REFERENCES ADMIN_USERS(admin_id)
);
COMMENT ON TABLE SEARCH_KEYWORDS IS '관리자 등록 검색 키워드';

-- ================================================================
-- 44. CARD_KEYWORDS
-- ================================================================
CREATE TABLE CARD_KEYWORDS (
    card_keyword_id  NUMBER(10)   PRIMARY KEY,
    card_id          NUMBER(10)   NOT NULL,
    keyword_id       NUMBER(10)   NOT NULL,
    created_at       TIMESTAMP    DEFAULT SYSTIMESTAMP NOT NULL,
    created_by       NUMBER(10),
    deleted_yn       CHAR(1)      DEFAULT 'N' NOT NULL CHECK (deleted_yn IN ('Y','N')),
    deleted_at       TIMESTAMP,
    CONSTRAINT FK_CK_CARD        FOREIGN KEY (card_id)    REFERENCES CARDS(card_id),
    CONSTRAINT FK_CK_KEYWORD     FOREIGN KEY (keyword_id) REFERENCES SEARCH_KEYWORDS(keyword_id),
    CONSTRAINT FK_CK_CREATED_BY  FOREIGN KEY (created_by) REFERENCES ADMIN_USERS(admin_id),
    CONSTRAINT UK_CK_CARD_KEYWORD UNIQUE (card_id, keyword_id)
);
COMMENT ON TABLE CARD_KEYWORDS IS '카드 ↔ 검색 키워드 매핑';

-- ================================================================
-- 45. SEARCH_LOGS
-- ================================================================
CREATE TABLE SEARCH_LOGS (
    search_log_id       NUMBER(10)     PRIMARY KEY,
    user_id             NUMBER(10),
    keyword_raw         VARCHAR2(200)  NOT NULL,
    matched_keyword_id  NUMBER(10),
    result_count        NUMBER(10),
    search_at           TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    ip_address          VARCHAR2(100),
    device_info         VARCHAR2(500),
    CONSTRAINT FK_SL_USER            FOREIGN KEY (user_id)           REFERENCES USERS(user_id),
    CONSTRAINT FK_SL_MATCHED_KEYWORD FOREIGN KEY (matched_keyword_id) REFERENCES SEARCH_KEYWORDS(keyword_id)
);
COMMENT ON TABLE SEARCH_LOGS IS '고객 검색 로그';

-- ================================================================
-- 46. USER_SPENDING_PATTERNS
-- ================================================================
CREATE TABLE USER_SPENDING_PATTERNS (
    pattern_id      NUMBER(10)     PRIMARY KEY,
    user_id         NUMBER(10)     NOT NULL,
    category_id     NUMBER(10)     NOT NULL,
    monthly_amount  NUMBER(10)     NOT NULL CHECK (monthly_amount >= 0),
    source          VARCHAR2(10)   NOT NULL CHECK (source IN ('MANUAL','AUTO')),
    updated_at      DATE           DEFAULT SYSDATE NOT NULL,
    CONSTRAINT FK_USP_USER    FOREIGN KEY (user_id)    REFERENCES USERS(user_id),
    CONSTRAINT FK_USP_CATEGO  FOREIGN KEY (category_id) REFERENCES CARD_CATEGORIES(category_id)
);
COMMENT ON TABLE USER_SPENDING_PATTERNS IS '사용자 월별 카테고리 소비패턴';
COMMENT ON COLUMN USER_SPENDING_PATTERNS.source IS 'MANUAL=직접입력 / AUTO=결제기반 자동분류';

-- ================================================================
-- 47. AI_CHAT_LOGS
-- ================================================================
CREATE TABLE AI_CHAT_LOGS (
    chat_id     NUMBER(10)     PRIMARY KEY,
    user_id     NUMBER(10)     NULL,
    session_id  VARCHAR2(100)  NOT NULL,
    user_input  CLOB           NOT NULL,
    ai_response CLOB           NOT NULL,
    created_at  DATE           DEFAULT SYSDATE NOT NULL,
    CONSTRAINT FK_CHAT_USER FOREIGN KEY (user_id) REFERENCES USERS(user_id)
);
COMMENT ON TABLE AI_CHAT_LOGS IS '챗봇 대화 로그 (비로그인 허용)';


-- ================================================================
-- SEQUENCES
-- ================================================================
-- ─── 일반 테이블 시퀀스 (START WITH 1) ───
-- COMMON_CODES.code_id
CREATE SEQUENCE SEQ_COMMON_CODES
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- ADMIN_USERS.admin_id
CREATE SEQUENCE SEQ_ADMIN_USERS
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- ADMIN_ROLES.role_id
CREATE SEQUENCE SEQ_ADMIN_ROLES
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- ADMIN_PERMISSIONS.permission_id
CREATE SEQUENCE SEQ_ADMIN_PERMISSIONS
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- ROLE_PERMISSIONS.role_permission_id
CREATE SEQUENCE SEQ_ROLE_PERMISSIONS
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- ADMIN_USER_ROLES.admin_user_role_id
CREATE SEQUENCE SEQ_ADMIN_USER_ROLES
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- USERS.user_id
CREATE SEQUENCE SEQ_USERS
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- USER_SESSIONS.session_id
CREATE SEQUENCE SEQ_USER_SESSIONS
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- LOGIN_HISTORIES.history_id
CREATE SEQUENCE SEQ_LOGIN_HISTORIES
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- AUDIT_LOGS.audit_log_id
CREATE SEQUENCE SEQ_AUDIT_LOGS
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- APPROVAL_REQUESTS.approval_id
CREATE SEQUENCE SEQ_APPROVAL_REQUESTS
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- APPROVAL_LINES.approval_line_id
CREATE SEQUENCE SEQ_APPROVAL_LINES
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- CARD_CATEGORIES.category_id
CREATE SEQUENCE SEQ_CARD_CATEGORIES
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- CARD_BENEFITS.benefit_id
CREATE SEQUENCE SEQ_CARD_BENEFITS
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- CARD_IMAGES.image_id
CREATE SEQUENCE SEQ_CARD_IMAGES
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- CARD_CONTENTS.content_id
CREATE SEQUENCE SEQ_CARD_CONTENTS
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- CARD_APPLICATIONS.application_id
CREATE SEQUENCE SEQ_CARD_APPLICATIONS
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- CARD_ATTRIBUTE_DEFINITIONS.attribute_id
CREATE SEQUENCE SEQ_CARD_ATTRIBUTE_DEFINITIONS
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- CARD_ATTRIBUTE_VALUES.card_attribute_value_id
CREATE SEQUENCE SEQ_CARD_ATTRIBUTE_VALUES
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- CARD_TAGS.tag_id
CREATE SEQUENCE SEQ_CARD_TAGS
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- CARD_TAG_MAP.card_tag_map_id
CREATE SEQUENCE SEQ_CARD_TAG_MAP
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- CARD_PROMOTIONS.promotion_id
CREATE SEQUENCE SEQ_CARD_PROMOTIONS
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- CARD_STATUS_HISTORIES.history_id
CREATE SEQUENCE SEQ_CARD_STATUS_HISTORIES
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- CARD_VERSIONS.version_id
CREATE SEQUENCE SEQ_CARD_VERSIONS
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- USER_CARDS.user_card_id
CREATE SEQUENCE SEQ_USER_CARDS
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- MERCHANT_CATEGORY_MAP.map_id
CREATE SEQUENCE SEQ_MERCHANT_CATEGORY_MAP
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- TERMS_GROUPS.group_id
CREATE SEQUENCE SEQ_TERMS_GROUPS
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- TERMS_PACKAGES.package_id
CREATE SEQUENCE SEQ_TERMS_PACKAGES
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- PACKAGE_TERMS.package_terms_id
CREATE SEQUENCE SEQ_PACKAGE_TERMS
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- TERMS_MASTERS.terms_master_id
CREATE SEQUENCE SEQ_TERMS_MASTERS
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- TERMS.terms_id
CREATE SEQUENCE SEQ_TERMS
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- TERMS_STATUS_HISTORY.history_id
CREATE SEQUENCE SEQ_TERMS_STATUS_HISTORY
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- TERMS_EXPOSURE_RULES.rule_id
CREATE SEQUENCE SEQ_TERMS_EXPOSURE_RULES
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- TERMS_FILES.file_id
CREATE SEQUENCE SEQ_TERMS_FILES
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- CARD_TERMS.card_terms_id
CREATE SEQUENCE SEQ_CARD_TERMS
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- USER_TERMS_AGREEMENTS.agreement_id
CREATE SEQUENCE SEQ_USER_TERMS_AGREEMENTS
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- CARD_NOTICE_SECTIONS.notice_id
CREATE SEQUENCE SEQ_CARD_NOTICE_SECTIONS
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- TERMS_CHANGE_DIFFS.diff_id
CREATE SEQUENCE SEQ_TERMS_CHANGE_DIFFS
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- TERMS_NOTIFICATION_HISTORY.notification_id
CREATE SEQUENCE SEQ_TERMS_NOTIFICATION_HISTORY
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- FILE_UPLOADS.file_id
CREATE SEQUENCE SEQ_FILE_UPLOADS
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- SEARCH_KEYWORDS.keyword_id
CREATE SEQUENCE SEQ_SEARCH_KEYWORDS
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- CARD_KEYWORDS.card_keyword_id
CREATE SEQUENCE SEQ_CARD_KEYWORDS
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- SEARCH_LOGS.search_log_id
CREATE SEQUENCE SEQ_SEARCH_LOGS
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- USER_SPENDING_PATTERNS.pattern_id
CREATE SEQUENCE SEQ_USER_SPENDING_PATTERNS
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- AI_CHAT_LOGS.chat_id
CREATE SEQUENCE SEQ_AI_CHAT_LOGS
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- ─── CARDS PK 전용 시퀀스 ───────────────
-- card_id = [type_prefix(3)][company_code(2)][serial(3)]
-- 각 카드 유형별 일련번호 독립 관리
-- CREDIT  (101)  일련번호 — 10101001~
CREATE SEQUENCE SEQ_CARD_SERIAL_CREDIT
    START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 999 NOCACHE NOCYCLE;

-- CHECK   (102)  일련번호 — 10201001~
CREATE SEQUENCE SEQ_CARD_SERIAL_CHECK
    START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 999 NOCACHE NOCYCLE;

-- PREPAID (103)  일련번호 — 10301001~
CREATE SEQUENCE SEQ_CARD_SERIAL_PREPAID
    START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 999 NOCACHE NOCYCLE;

-- HYBRID  (104)  일련번호 — 10401001~
CREATE SEQUENCE SEQ_CARD_SERIAL_HYBRID
    START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 999 NOCACHE NOCYCLE;


-- ================================================================
-- FUNCTION: FN_GEN_CARD_ID
--   카드 PK 자동 생성
--   입력: card_type VARCHAR2, company_code NUMBER(기본 1=BNK)
--   출력: 8자리 구조화 card_id NUMBER
-- ================================================================
CREATE OR REPLACE FUNCTION FN_GEN_CARD_ID(
    p_card_type    IN VARCHAR2,
    p_company_code IN NUMBER DEFAULT 1
) RETURN NUMBER
AS
    v_type_prefix  NUMBER(3);
    v_serial       NUMBER(3);
BEGIN
    CASE UPPER(p_card_type)
        WHEN 'CREDIT'  THEN
            v_type_prefix := 101;
            SELECT SEQ_CARD_SERIAL_CREDIT.NEXTVAL  INTO v_serial FROM DUAL;
        WHEN 'CHECK'   THEN
            v_type_prefix := 102;
            SELECT SEQ_CARD_SERIAL_CHECK.NEXTVAL   INTO v_serial FROM DUAL;
        WHEN 'PREPAID' THEN
            v_type_prefix := 103;
            SELECT SEQ_CARD_SERIAL_PREPAID.NEXTVAL INTO v_serial FROM DUAL;
        WHEN 'HYBRID'  THEN
            v_type_prefix := 104;
            SELECT SEQ_CARD_SERIAL_HYBRID.NEXTVAL  INTO v_serial FROM DUAL;
        ELSE
            RAISE_APPLICATION_ERROR(-20001,
                'FN_GEN_CARD_ID: 지원하지 않는 카드 유형 → ' || p_card_type);
    END CASE;

    -- [type_prefix(3)] * 100000 + [company_code(2)] * 1000 + [serial(3)]
    -- 예) 101 * 100000 + 1 * 1000 + 1 = 10101001
    RETURN v_type_prefix * 100000 + p_company_code * 1000 + v_serial;
END FN_GEN_CARD_ID;
/


-- ================================================================
-- TRIGGERS — BEFORE INSERT 자동 PK 채번
-- 조건: NEW.pk IS NULL 일 때만 시퀀스 사용
--       → INSERT 시 명시적 값도 허용 (더미 데이터 삽입용)
-- ================================================================
CREATE OR REPLACE TRIGGER TRG_COMMON_CODES_BI
BEFORE INSERT ON COMMON_CODES
FOR EACH ROW
WHEN (NEW.code_id IS NULL)
BEGIN
    :NEW.code_id := SEQ_COMMON_CODES.NEXTVAL;
END TRG_COMMON_CODES_BI;
/

CREATE OR REPLACE TRIGGER TRG_ADMIN_USERS_BI
BEFORE INSERT ON ADMIN_USERS
FOR EACH ROW
WHEN (NEW.admin_id IS NULL)
BEGIN
    :NEW.admin_id := SEQ_ADMIN_USERS.NEXTVAL;
END TRG_ADMIN_USERS_BI;
/

CREATE OR REPLACE TRIGGER TRG_ADMIN_ROLES_BI
BEFORE INSERT ON ADMIN_ROLES
FOR EACH ROW
WHEN (NEW.role_id IS NULL)
BEGIN
    :NEW.role_id := SEQ_ADMIN_ROLES.NEXTVAL;
END TRG_ADMIN_ROLES_BI;
/

CREATE OR REPLACE TRIGGER TRG_ADMIN_PERMISSIONS_BI
BEFORE INSERT ON ADMIN_PERMISSIONS
FOR EACH ROW
WHEN (NEW.permission_id IS NULL)
BEGIN
    :NEW.permission_id := SEQ_ADMIN_PERMISSIONS.NEXTVAL;
END TRG_ADMIN_PERMISSIONS_BI;
/

CREATE OR REPLACE TRIGGER TRG_ROLE_PERMISSIONS_BI
BEFORE INSERT ON ROLE_PERMISSIONS
FOR EACH ROW
WHEN (NEW.role_permission_id IS NULL)
BEGIN
    :NEW.role_permission_id := SEQ_ROLE_PERMISSIONS.NEXTVAL;
END TRG_ROLE_PERMISSIONS_BI;
/

CREATE OR REPLACE TRIGGER TRG_ADMIN_USER_ROLES_BI
BEFORE INSERT ON ADMIN_USER_ROLES
FOR EACH ROW
WHEN (NEW.admin_user_role_id IS NULL)
BEGIN
    :NEW.admin_user_role_id := SEQ_ADMIN_USER_ROLES.NEXTVAL;
END TRG_ADMIN_USER_ROLES_BI;
/

CREATE OR REPLACE TRIGGER TRG_USERS_BI
BEFORE INSERT ON USERS
FOR EACH ROW
WHEN (NEW.user_id IS NULL)
BEGIN
    :NEW.user_id := SEQ_USERS.NEXTVAL;
END TRG_USERS_BI;
/

CREATE OR REPLACE TRIGGER TRG_USER_SESSIONS_BI
BEFORE INSERT ON USER_SESSIONS
FOR EACH ROW
WHEN (NEW.session_id IS NULL)
BEGIN
    :NEW.session_id := SEQ_USER_SESSIONS.NEXTVAL;
END TRG_USER_SESSIONS_BI;
/

CREATE OR REPLACE TRIGGER TRG_LOGIN_HISTORIES_BI
BEFORE INSERT ON LOGIN_HISTORIES
FOR EACH ROW
WHEN (NEW.history_id IS NULL)
BEGIN
    :NEW.history_id := SEQ_LOGIN_HISTORIES.NEXTVAL;
END TRG_LOGIN_HISTORIES_BI;
/

CREATE OR REPLACE TRIGGER TRG_AUDIT_LOGS_BI
BEFORE INSERT ON AUDIT_LOGS
FOR EACH ROW
WHEN (NEW.audit_log_id IS NULL)
BEGIN
    :NEW.audit_log_id := SEQ_AUDIT_LOGS.NEXTVAL;
END TRG_AUDIT_LOGS_BI;
/

CREATE OR REPLACE TRIGGER TRG_APPROVAL_REQUESTS_BI
BEFORE INSERT ON APPROVAL_REQUESTS
FOR EACH ROW
WHEN (NEW.approval_id IS NULL)
BEGIN
    :NEW.approval_id := SEQ_APPROVAL_REQUESTS.NEXTVAL;
END TRG_APPROVAL_REQUESTS_BI;
/

CREATE OR REPLACE TRIGGER TRG_APPROVAL_LINES_BI
BEFORE INSERT ON APPROVAL_LINES
FOR EACH ROW
WHEN (NEW.approval_line_id IS NULL)
BEGIN
    :NEW.approval_line_id := SEQ_APPROVAL_LINES.NEXTVAL;
END TRG_APPROVAL_LINES_BI;
/

CREATE OR REPLACE TRIGGER TRG_CARD_CATEGORIES_BI
BEFORE INSERT ON CARD_CATEGORIES
FOR EACH ROW
WHEN (NEW.category_id IS NULL)
BEGIN
    :NEW.category_id := SEQ_CARD_CATEGORIES.NEXTVAL;
END TRG_CARD_CATEGORIES_BI;
/

CREATE OR REPLACE TRIGGER TRG_CARD_BENEFITS_BI
BEFORE INSERT ON CARD_BENEFITS
FOR EACH ROW
WHEN (NEW.benefit_id IS NULL)
BEGIN
    :NEW.benefit_id := SEQ_CARD_BENEFITS.NEXTVAL;
END TRG_CARD_BENEFITS_BI;
/

CREATE OR REPLACE TRIGGER TRG_CARD_IMAGES_BI
BEFORE INSERT ON CARD_IMAGES
FOR EACH ROW
WHEN (NEW.image_id IS NULL)
BEGIN
    :NEW.image_id := SEQ_CARD_IMAGES.NEXTVAL;
END TRG_CARD_IMAGES_BI;
/

CREATE OR REPLACE TRIGGER TRG_CARD_CONTENTS_BI
BEFORE INSERT ON CARD_CONTENTS
FOR EACH ROW
WHEN (NEW.content_id IS NULL)
BEGIN
    :NEW.content_id := SEQ_CARD_CONTENTS.NEXTVAL;
END TRG_CARD_CONTENTS_BI;
/

CREATE OR REPLACE TRIGGER TRG_CARD_APPLICATIONS_BI
BEFORE INSERT ON CARD_APPLICATIONS
FOR EACH ROW
WHEN (NEW.application_id IS NULL)
BEGIN
    :NEW.application_id := SEQ_CARD_APPLICATIONS.NEXTVAL;
END TRG_CARD_APPLICATIONS_BI;
/

CREATE OR REPLACE TRIGGER TRG_CARD_ATTR_DEF_BI
BEFORE INSERT ON CARD_ATTRIBUTE_DEFINITIONS
FOR EACH ROW
WHEN (NEW.attribute_id IS NULL)
BEGIN
    :NEW.attribute_id := SEQ_CARD_ATTRIBUTE_DEFINITIONS.NEXTVAL;
END TRG_CARD_ATTR_DEF_BI;
/

CREATE OR REPLACE TRIGGER TRG_CARD_ATTR_VAL_BI
BEFORE INSERT ON CARD_ATTRIBUTE_VALUES
FOR EACH ROW
WHEN (NEW.card_attribute_value_id IS NULL)
BEGIN
    :NEW.card_attribute_value_id := SEQ_CARD_ATTRIBUTE_VALUES.NEXTVAL;
END TRG_CARD_ATTR_VAL_BI;
/

CREATE OR REPLACE TRIGGER TRG_CARD_TAGS_BI
BEFORE INSERT ON CARD_TAGS
FOR EACH ROW
WHEN (NEW.tag_id IS NULL)
BEGIN
    :NEW.tag_id := SEQ_CARD_TAGS.NEXTVAL;
END TRG_CARD_TAGS_BI;
/

CREATE OR REPLACE TRIGGER TRG_CARD_TAG_MAP_BI
BEFORE INSERT ON CARD_TAG_MAP
FOR EACH ROW
WHEN (NEW.card_tag_map_id IS NULL)
BEGIN
    :NEW.card_tag_map_id := SEQ_CARD_TAG_MAP.NEXTVAL;
END TRG_CARD_TAG_MAP_BI;
/

CREATE OR REPLACE TRIGGER TRG_CARD_PROMOTIONS_BI
BEFORE INSERT ON CARD_PROMOTIONS
FOR EACH ROW
WHEN (NEW.promotion_id IS NULL)
BEGIN
    :NEW.promotion_id := SEQ_CARD_PROMOTIONS.NEXTVAL;
END TRG_CARD_PROMOTIONS_BI;
/

CREATE OR REPLACE TRIGGER TRG_CARD_STATUS_HIST_BI
BEFORE INSERT ON CARD_STATUS_HISTORIES
FOR EACH ROW
WHEN (NEW.history_id IS NULL)
BEGIN
    :NEW.history_id := SEQ_CARD_STATUS_HISTORIES.NEXTVAL;
END TRG_CARD_STATUS_HIST_BI;
/

CREATE OR REPLACE TRIGGER TRG_CARD_VERSIONS_BI
BEFORE INSERT ON CARD_VERSIONS
FOR EACH ROW
WHEN (NEW.version_id IS NULL)
BEGIN
    :NEW.version_id := SEQ_CARD_VERSIONS.NEXTVAL;
END TRG_CARD_VERSIONS_BI;
/

CREATE OR REPLACE TRIGGER TRG_USER_CARDS_BI
BEFORE INSERT ON USER_CARDS
FOR EACH ROW
WHEN (NEW.user_card_id IS NULL)
BEGIN
    :NEW.user_card_id := SEQ_USER_CARDS.NEXTVAL;
END TRG_USER_CARDS_BI;
/

CREATE OR REPLACE TRIGGER TRG_MERCHANT_CATEGORY_BI
BEFORE INSERT ON MERCHANT_CATEGORY_MAP
FOR EACH ROW
WHEN (NEW.map_id IS NULL)
BEGIN
    :NEW.map_id := SEQ_MERCHANT_CATEGORY_MAP.NEXTVAL;
END TRG_MERCHANT_CATEGORY_BI;
/

CREATE OR REPLACE TRIGGER TRG_TERMS_GROUPS_BI
BEFORE INSERT ON TERMS_GROUPS
FOR EACH ROW
WHEN (NEW.group_id IS NULL)
BEGIN
    :NEW.group_id := SEQ_TERMS_GROUPS.NEXTVAL;
END TRG_TERMS_GROUPS_BI;
/

CREATE OR REPLACE TRIGGER TRG_TERMS_PACKAGES_BI
BEFORE INSERT ON TERMS_PACKAGES
FOR EACH ROW
WHEN (NEW.package_id IS NULL)
BEGIN
    :NEW.package_id := SEQ_TERMS_PACKAGES.NEXTVAL;
END TRG_TERMS_PACKAGES_BI;
/

CREATE OR REPLACE TRIGGER TRG_PACKAGE_TERMS_BI
BEFORE INSERT ON PACKAGE_TERMS
FOR EACH ROW
WHEN (NEW.package_terms_id IS NULL)
BEGIN
    :NEW.package_terms_id := SEQ_PACKAGE_TERMS.NEXTVAL;
END TRG_PACKAGE_TERMS_BI;
/

CREATE OR REPLACE TRIGGER TRG_TERMS_MASTERS_BI
BEFORE INSERT ON TERMS_MASTERS
FOR EACH ROW
WHEN (NEW.terms_master_id IS NULL)
BEGIN
    :NEW.terms_master_id := SEQ_TERMS_MASTERS.NEXTVAL;
END TRG_TERMS_MASTERS_BI;
/

CREATE OR REPLACE TRIGGER TRG_TERMS_BI
BEFORE INSERT ON TERMS
FOR EACH ROW
WHEN (NEW.terms_id IS NULL)
BEGIN
    :NEW.terms_id := SEQ_TERMS.NEXTVAL;
END TRG_TERMS_BI;
/

CREATE OR REPLACE TRIGGER TRG_TERMS_STATUS_HIST_BI
BEFORE INSERT ON TERMS_STATUS_HISTORY
FOR EACH ROW
WHEN (NEW.history_id IS NULL)
BEGIN
    :NEW.history_id := SEQ_TERMS_STATUS_HISTORY.NEXTVAL;
END TRG_TERMS_STATUS_HIST_BI;
/

CREATE OR REPLACE TRIGGER TRG_TERMS_EXPOSURE_RULES_BI
BEFORE INSERT ON TERMS_EXPOSURE_RULES
FOR EACH ROW
WHEN (NEW.rule_id IS NULL)
BEGIN
    :NEW.rule_id := SEQ_TERMS_EXPOSURE_RULES.NEXTVAL;
END TRG_TERMS_EXPOSURE_RULES_BI;
/

CREATE OR REPLACE TRIGGER TRG_TERMS_FILES_BI
BEFORE INSERT ON TERMS_FILES
FOR EACH ROW
WHEN (NEW.file_id IS NULL)
BEGIN
    :NEW.file_id := SEQ_TERMS_FILES.NEXTVAL;
END TRG_TERMS_FILES_BI;
/

CREATE OR REPLACE TRIGGER TRG_CARD_TERMS_BI
BEFORE INSERT ON CARD_TERMS
FOR EACH ROW
WHEN (NEW.card_terms_id IS NULL)
BEGIN
    :NEW.card_terms_id := SEQ_CARD_TERMS.NEXTVAL;
END TRG_CARD_TERMS_BI;
/

CREATE OR REPLACE TRIGGER TRG_USER_TERMS_AGMT_BI
BEFORE INSERT ON USER_TERMS_AGREEMENTS
FOR EACH ROW
WHEN (NEW.agreement_id IS NULL)
BEGIN
    :NEW.agreement_id := SEQ_USER_TERMS_AGREEMENTS.NEXTVAL;
END TRG_USER_TERMS_AGMT_BI;
/

CREATE OR REPLACE TRIGGER TRG_CARD_NOTICE_BI
BEFORE INSERT ON CARD_NOTICE_SECTIONS
FOR EACH ROW
WHEN (NEW.notice_id IS NULL)
BEGIN
    :NEW.notice_id := SEQ_CARD_NOTICE_SECTIONS.NEXTVAL;
END TRG_CARD_NOTICE_BI;
/

CREATE OR REPLACE TRIGGER TRG_TERMS_CHANGE_DIFFS_BI
BEFORE INSERT ON TERMS_CHANGE_DIFFS
FOR EACH ROW
WHEN (NEW.diff_id IS NULL)
BEGIN
    :NEW.diff_id := SEQ_TERMS_CHANGE_DIFFS.NEXTVAL;
END TRG_TERMS_CHANGE_DIFFS_BI;
/

CREATE OR REPLACE TRIGGER TRG_TERMS_NOTIF_HIST_BI
BEFORE INSERT ON TERMS_NOTIFICATION_HISTORY
FOR EACH ROW
WHEN (NEW.notification_id IS NULL)
BEGIN
    :NEW.notification_id := SEQ_TERMS_NOTIFICATION_HISTORY.NEXTVAL;
END TRG_TERMS_NOTIF_HIST_BI;
/

CREATE OR REPLACE TRIGGER TRG_FILE_UPLOADS_BI
BEFORE INSERT ON FILE_UPLOADS
FOR EACH ROW
WHEN (NEW.file_id IS NULL)
BEGIN
    :NEW.file_id := SEQ_FILE_UPLOADS.NEXTVAL;
END TRG_FILE_UPLOADS_BI;
/

CREATE OR REPLACE TRIGGER TRG_SEARCH_KEYWORDS_BI
BEFORE INSERT ON SEARCH_KEYWORDS
FOR EACH ROW
WHEN (NEW.keyword_id IS NULL)
BEGIN
    :NEW.keyword_id := SEQ_SEARCH_KEYWORDS.NEXTVAL;
END TRG_SEARCH_KEYWORDS_BI;
/

CREATE OR REPLACE TRIGGER TRG_CARD_KEYWORDS_BI
BEFORE INSERT ON CARD_KEYWORDS
FOR EACH ROW
WHEN (NEW.card_keyword_id IS NULL)
BEGIN
    :NEW.card_keyword_id := SEQ_CARD_KEYWORDS.NEXTVAL;
END TRG_CARD_KEYWORDS_BI;
/

CREATE OR REPLACE TRIGGER TRG_SEARCH_LOGS_BI
BEFORE INSERT ON SEARCH_LOGS
FOR EACH ROW
WHEN (NEW.search_log_id IS NULL)
BEGIN
    :NEW.search_log_id := SEQ_SEARCH_LOGS.NEXTVAL;
END TRG_SEARCH_LOGS_BI;
/

CREATE OR REPLACE TRIGGER TRG_USER_SPENDING_BI
BEFORE INSERT ON USER_SPENDING_PATTERNS
FOR EACH ROW
WHEN (NEW.pattern_id IS NULL)
BEGIN
    :NEW.pattern_id := SEQ_USER_SPENDING_PATTERNS.NEXTVAL;
END TRG_USER_SPENDING_BI;
/

CREATE OR REPLACE TRIGGER TRG_AI_CHAT_LOGS_BI
BEFORE INSERT ON AI_CHAT_LOGS
FOR EACH ROW
WHEN (NEW.chat_id IS NULL)
BEGIN
    :NEW.chat_id := SEQ_AI_CHAT_LOGS.NEXTVAL;
END TRG_AI_CHAT_LOGS_BI;
/

-- ─── CARDS PK 전용 트리거 ───────────────────────────────
-- card_id IS NULL 일 때 FN_GEN_CARD_ID() 호출
-- card_id 명시 시 트리거 우회 (더미 INSERT용)
CREATE OR REPLACE TRIGGER TRG_CARDS_BI
BEFORE INSERT ON CARDS
FOR EACH ROW
WHEN (NEW.card_id IS NULL)
BEGIN
    -- FN_GEN_CARD_ID: card_type + company_code(=CARDS.company_code or 1)
    :NEW.card_id := FN_GEN_CARD_ID(
        :NEW.card_type,
        NVL(TO_NUMBER(:NEW.company_code), 1)
    );
END TRG_CARDS_BI;
/

-- ─── CARDS updated_at 자동 갱신 트리거 ─────────────────
CREATE OR REPLACE TRIGGER TRG_CARDS_BU
BEFORE UPDATE ON CARDS
FOR EACH ROW
BEGIN
    :NEW.updated_at := SYSTIMESTAMP;
END TRG_CARDS_BU;
/

-- ─── USERS updated_at 자동 갱신 트리거 ─────────────────
CREATE OR REPLACE TRIGGER TRG_USERS_BU
BEFORE UPDATE ON USERS
FOR EACH ROW
BEGIN
    :NEW.updated_at := SYSTIMESTAMP;
END TRG_USERS_BU;
/


-- ================================================================
-- INDEXES
-- ================================================================
CREATE INDEX IDX_USERS_EMAIL ON USERS(email);
CREATE INDEX IDX_USERS_STATUS ON USERS(status_code);
CREATE INDEX IDX_USERS_PHONE ON USERS(phone);
CREATE INDEX IDX_USERS_NAME ON USERS(name);
CREATE INDEX IDX_ADMIN_USERS_USERNAME ON ADMIN_USERS(username);
CREATE INDEX IDX_USER_SESSIONS_USER ON USER_SESSIONS(user_id);
CREATE INDEX IDX_USER_SESSIONS_TOKEN ON USER_SESSIONS(refresh_token);
CREATE INDEX IDX_LOGIN_HIST_USER ON LOGIN_HISTORIES(user_id);
CREATE INDEX IDX_LOGIN_HIST_DATE ON LOGIN_HISTORIES(login_at);
CREATE INDEX IDX_AUDIT_LOGS_ACTOR ON AUDIT_LOGS(actor_id);
CREATE INDEX IDX_AUDIT_LOGS_TARGET ON AUDIT_LOGS(target_id);
CREATE INDEX IDX_AUDIT_LOGS_ACTION ON AUDIT_LOGS(action_type_code);
CREATE INDEX IDX_APPROVAL_REQ_STATUS ON APPROVAL_REQUESTS(status_code);
CREATE INDEX IDX_APPROVAL_LINES_APPR ON APPROVAL_LINES(approval_id);
CREATE INDEX IDX_CARDS_STATUS ON CARDS(card_status);
CREATE INDEX IDX_CARDS_NAME ON CARDS(card_name);
CREATE INDEX IDX_CARDS_TYPE ON CARDS(card_type);
CREATE INDEX IDX_CARDS_COMPANY ON CARDS(company_name);
CREATE INDEX IDX_CARDS_PUBLISH ON CARDS(publish_start_at);
CREATE INDEX IDX_CARD_BENEFITS_CARD ON CARD_BENEFITS(card_id);
CREATE INDEX IDX_CARD_IMAGES_CARD ON CARD_IMAGES(card_id);
CREATE INDEX IDX_CARD_CONTENTS_CARD ON CARD_CONTENTS(card_id);
CREATE INDEX IDX_CARD_APPL_USER ON CARD_APPLICATIONS(user_id);
CREATE INDEX IDX_CARD_APPL_CARD ON CARD_APPLICATIONS(card_id);
CREATE INDEX IDX_CARD_PROMOS_CARD ON CARD_PROMOTIONS(card_id);
CREATE INDEX IDX_CARD_STATUS_HIST_CARD ON CARD_STATUS_HISTORIES(card_id);
CREATE INDEX IDX_CARD_VERSIONS_CARD ON CARD_VERSIONS(card_id);
CREATE INDEX IDX_USER_CARDS_USER ON USER_CARDS(user_id);
CREATE INDEX IDX_USER_CARDS_CARD ON USER_CARDS(card_id);
CREATE INDEX IDX_MERCH_CAT_KEYWORD ON MERCHANT_CATEGORY_MAP(keyword);
CREATE INDEX IDX_CARD_TAG_MAP_CARD ON CARD_TAG_MAP(card_id);
CREATE INDEX IDX_CARD_TAG_MAP_TAG ON CARD_TAG_MAP(tag_id);
CREATE INDEX IDX_TERMS_STATUS ON TERMS(status);
CREATE INDEX IDX_TERMS_MASTER ON TERMS(terms_master_id);
CREATE INDEX IDX_USER_TERMS_USER ON USER_TERMS_AGREEMENTS(user_id);
CREATE INDEX IDX_USER_TERMS_TERMS ON USER_TERMS_AGREEMENTS(terms_id);
CREATE INDEX IDX_CARD_TERMS_CARD ON CARD_TERMS(card_id);
CREATE INDEX IDX_TERMS_STATUS_HIST ON TERMS_STATUS_HISTORY(terms_id);
CREATE INDEX IDX_SEARCH_KW_USE ON SEARCH_KEYWORDS(use_yn);
CREATE INDEX IDX_SEARCH_KW_ORDER ON SEARCH_KEYWORDS(display_order);
CREATE INDEX IDX_CARD_KW_CARD ON CARD_KEYWORDS(card_id);
CREATE INDEX IDX_CARD_KW_KW ON CARD_KEYWORDS(keyword_id);
CREATE INDEX IDX_SEARCH_LOGS_DATE ON SEARCH_LOGS(search_at);
CREATE INDEX IDX_SEARCH_LOGS_USER ON SEARCH_LOGS(user_id);
CREATE INDEX IDX_SPEND_USER ON USER_SPENDING_PATTERNS(user_id);
CREATE INDEX IDX_SPEND_CAT ON USER_SPENDING_PATTERNS(category_id);
CREATE INDEX IDX_AI_CHAT_SESS ON AI_CHAT_LOGS(session_id);
CREATE INDEX IDX_AI_CHAT_USER ON AI_CHAT_LOGS(user_id);
CREATE INDEX IDX_FILE_UPLOADS_REF ON FILE_UPLOADS(ref_id);
