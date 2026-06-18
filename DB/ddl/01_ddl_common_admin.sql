-- ================================================================
-- BNK 부산은행 금융 상품 플랫폼
-- [도메인 01] 공통 코드 · 관리자
-- Oracle 21c
-- 포함 테이블:
--   COMMON_CODE_GROUPS, COMMON_CODES
--   ADMIN_USERS, ADMIN_ROLES, ADMIN_PERMISSIONS
--   ROLE_PERMISSIONS, ADMIN_USER_ROLES
--
-- ■ 변경 이력
--   2026-06-17  더미데이터 정합화에 맞춰 COMMON_CODES 초기 데이터 반영
--               JOB_CODE: EMPLOYEE/SELF_EMP 등 구 코드 제거
--                         → EMPLOYED/SELF_EMPLOYED/STUDENT/UNEMPLOYED/OTHER
--               INCOME_LEVEL: HIGH/MID_HIGH 등 5단계 제거
--                             → LV1/LV2/LV3/LV4 4단계
--               ACTION_TYPE: USER_UPDATE/PASSWORD_CHANGE/STATUS_CHANGE/WITHDRAW 추가
--               TARGET_TYPE: CARD_APPLICATION/ADMIN/APPROVAL 추가
-- ================================================================


-- ================================================================
-- [SECTION 1] DROP (재실행 대비 — 의존 순서 역순)
-- ================================================================

BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER TRG_ADMIN_USER_ROLES_BI';    EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER TRG_ROLE_PERMISSIONS_BI';    EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER TRG_ADMIN_PERMISSIONS_BI';   EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER TRG_ADMIN_ROLES_BI';         EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER TRG_ADMIN_USERS_BI';         EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER TRG_COMMON_CODES_BI';        EXCEPTION WHEN OTHERS THEN NULL; END;
/

BEGIN EXECUTE IMMEDIATE 'DROP TABLE ADMIN_USER_ROLES    CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE ROLE_PERMISSIONS    CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE ADMIN_PERMISSIONS   CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE ADMIN_ROLES         CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE ADMIN_USERS         CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE COMMON_CODES        CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE COMMON_CODE_GROUPS  CASCADE CONSTRAINTS PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/

BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_ADMIN_USER_ROLES';   EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_ROLE_PERMISSIONS';   EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_ADMIN_PERMISSIONS';  EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_ADMIN_ROLES';        EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_ADMIN_USERS';        EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_COMMON_CODES';       EXCEPTION WHEN OTHERS THEN NULL; END;
/


-- ================================================================
-- [SECTION 2] CREATE
-- ================================================================

-- ── 시퀀스 ────────────────────────────────────────────────────────
CREATE SEQUENCE SEQ_COMMON_CODES      START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_ADMIN_USERS       START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_ADMIN_ROLES       START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_ADMIN_PERMISSIONS START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_ROLE_PERMISSIONS  START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_ADMIN_USER_ROLES  START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;


-- ── 테이블 ────────────────────────────────────────────────────────

-- 01. COMMON_CODE_GROUPS
CREATE TABLE COMMON_CODE_GROUPS (
    group_code      VARCHAR2(50)   PRIMARY KEY,
    group_name      VARCHAR2(100)  NOT NULL,
    description     VARCHAR2(1000),
    use_yn          CHAR(1)        DEFAULT 'Y' CHECK (use_yn IN ('Y','N')),
    created_at      TIMESTAMP      DEFAULT SYSTIMESTAMP,
    created_by      NUMBER(10),
    updated_at      TIMESTAMP,
    updated_by      NUMBER(10),
    deleted_yn      CHAR(1)        DEFAULT 'N' CHECK (deleted_yn IN ('Y','N'))
);
COMMENT ON TABLE COMMON_CODE_GROUPS IS '공통 코드 그룹';

-- 02. COMMON_CODES
CREATE TABLE COMMON_CODES (
    code_id         NUMBER(10)     PRIMARY KEY,
    group_code      VARCHAR2(50)   NOT NULL,
    code            VARCHAR2(50)   NOT NULL,
    code_name       VARCHAR2(100)  NOT NULL,
    code_value      VARCHAR2(200),
    display_order   NUMBER(5),
    description     VARCHAR2(1000),
    use_yn          CHAR(1)        DEFAULT 'Y' CHECK (use_yn IN ('Y','N')),
    created_at      TIMESTAMP      DEFAULT SYSTIMESTAMP,
    created_by      NUMBER(10),
    updated_at      TIMESTAMP,
    updated_by      NUMBER(10),
    deleted_yn      CHAR(1)        DEFAULT 'N' CHECK (deleted_yn IN ('Y','N')),
    deleted_at      TIMESTAMP,
    CONSTRAINT FK_COMMON_CODES_GROUP FOREIGN KEY (group_code) REFERENCES COMMON_CODE_GROUPS(group_code)
);
COMMENT ON TABLE COMMON_CODES IS '공통 코드 상세';

-- 03. ADMIN_USERS
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

-- 04. ADMIN_ROLES
CREATE TABLE ADMIN_ROLES (
    role_id         NUMBER(10)     PRIMARY KEY,
    role_code       VARCHAR2(50)   NOT NULL UNIQUE,
    role_name       VARCHAR2(100)  NOT NULL,
    description     VARCHAR2(1000),
    created_at      TIMESTAMP      DEFAULT SYSTIMESTAMP,
    created_by      NUMBER(10),
    updated_at      TIMESTAMP,
    updated_by      NUMBER(10),
    deleted_yn      CHAR(1)        DEFAULT 'N' CHECK (deleted_yn IN ('Y','N')),
    deleted_at      TIMESTAMP
);
COMMENT ON TABLE ADMIN_ROLES IS '관리자 역할';

-- 05. ADMIN_PERMISSIONS
CREATE TABLE ADMIN_PERMISSIONS (
    permission_id   NUMBER(10)     PRIMARY KEY,
    permission_code VARCHAR2(100)  NOT NULL UNIQUE,
    permission_name VARCHAR2(200)  NOT NULL,
    description     VARCHAR2(1000),
    created_at      TIMESTAMP      DEFAULT SYSTIMESTAMP,
    deleted_yn      CHAR(1)        DEFAULT 'N' CHECK (deleted_yn IN ('Y','N'))
);
COMMENT ON TABLE ADMIN_PERMISSIONS IS '관리자 권한 항목';

-- 06. ROLE_PERMISSIONS
CREATE TABLE ROLE_PERMISSIONS (
    role_permission_id  NUMBER(10)  PRIMARY KEY,
    role_id             NUMBER(10)  NOT NULL,
    permission_id       NUMBER(10)  NOT NULL,
    created_at          TIMESTAMP   DEFAULT SYSTIMESTAMP,
    deleted_yn          CHAR(1)     DEFAULT 'N' CHECK (deleted_yn IN ('Y','N')),
    CONSTRAINT FK_ROLE_PERM_ROLE  FOREIGN KEY (role_id)       REFERENCES ADMIN_ROLES(role_id),
    CONSTRAINT FK_ROLE_PERM_PERM  FOREIGN KEY (permission_id) REFERENCES ADMIN_PERMISSIONS(permission_id)
);
COMMENT ON TABLE ROLE_PERMISSIONS IS '역할-권한 매핑';

-- 07. ADMIN_USER_ROLES
CREATE TABLE ADMIN_USER_ROLES (
    admin_user_role_id  NUMBER(10)  PRIMARY KEY,
    admin_id            NUMBER(10)  NOT NULL,
    role_id             NUMBER(10)  NOT NULL,
    assigned_at         TIMESTAMP   DEFAULT SYSTIMESTAMP,
    assigned_by         NUMBER(10),
    deleted_yn          CHAR(1)     DEFAULT 'N' CHECK (deleted_yn IN ('Y','N')),
    CONSTRAINT FK_ADMIN_USER_ROLE_ADMIN FOREIGN KEY (admin_id) REFERENCES ADMIN_USERS(admin_id),
    CONSTRAINT FK_ADMIN_USER_ROLE_ROLE  FOREIGN KEY (role_id)  REFERENCES ADMIN_ROLES(role_id)
);
COMMENT ON TABLE ADMIN_USER_ROLES IS '관리자-역할 매핑';


-- ── 트리거 (BEFORE INSERT — PK 자동 채번) ─────────────────────────
CREATE OR REPLACE TRIGGER TRG_COMMON_CODES_BI
BEFORE INSERT ON COMMON_CODES FOR EACH ROW WHEN (NEW.code_id IS NULL)
BEGIN :NEW.code_id := SEQ_COMMON_CODES.NEXTVAL; END TRG_COMMON_CODES_BI;
/

CREATE OR REPLACE TRIGGER TRG_ADMIN_USERS_BI
BEFORE INSERT ON ADMIN_USERS FOR EACH ROW WHEN (NEW.admin_id IS NULL)
BEGIN :NEW.admin_id := SEQ_ADMIN_USERS.NEXTVAL; END TRG_ADMIN_USERS_BI;
/

CREATE OR REPLACE TRIGGER TRG_ADMIN_ROLES_BI
BEFORE INSERT ON ADMIN_ROLES FOR EACH ROW WHEN (NEW.role_id IS NULL)
BEGIN :NEW.role_id := SEQ_ADMIN_ROLES.NEXTVAL; END TRG_ADMIN_ROLES_BI;
/

CREATE OR REPLACE TRIGGER TRG_ADMIN_PERMISSIONS_BI
BEFORE INSERT ON ADMIN_PERMISSIONS FOR EACH ROW WHEN (NEW.permission_id IS NULL)
BEGIN :NEW.permission_id := SEQ_ADMIN_PERMISSIONS.NEXTVAL; END TRG_ADMIN_PERMISSIONS_BI;
/

CREATE OR REPLACE TRIGGER TRG_ROLE_PERMISSIONS_BI
BEFORE INSERT ON ROLE_PERMISSIONS FOR EACH ROW WHEN (NEW.role_permission_id IS NULL)
BEGIN :NEW.role_permission_id := SEQ_ROLE_PERMISSIONS.NEXTVAL; END TRG_ROLE_PERMISSIONS_BI;
/

CREATE OR REPLACE TRIGGER TRG_ADMIN_USER_ROLES_BI
BEFORE INSERT ON ADMIN_USER_ROLES FOR EACH ROW WHEN (NEW.admin_user_role_id IS NULL)
BEGIN :NEW.admin_user_role_id := SEQ_ADMIN_USER_ROLES.NEXTVAL; END TRG_ADMIN_USER_ROLES_BI;
/


-- ── 인덱스 ────────────────────────────────────────────────────────
CREATE INDEX IDX_COMMON_CODES_GROUP    ON COMMON_CODES(group_code);
CREATE INDEX IDX_ADMIN_USERS_STATUS    ON ADMIN_USERS(status_code);
CREATE INDEX IDX_ROLE_PERM_ROLE        ON ROLE_PERMISSIONS(role_id);
CREATE INDEX IDX_ADMIN_USER_ROLE_ADMIN ON ADMIN_USER_ROLES(admin_id);


COMMIT;


-- ================================================================
-- [SECTION 3] 초기 데이터 INSERT
-- ================================================================

-- ── COMMON_CODE_GROUPS ───────────────────────────────────────────
INSERT INTO COMMON_CODE_GROUPS (group_code, group_name, description, use_yn, created_at, deleted_yn)
VALUES ('USER_STATUS',    '사용자 상태',   '사용자 계정 상태 코드',     'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODE_GROUPS (group_code, group_name, description, use_yn, created_at, deleted_yn)
VALUES ('ADMIN_STATUS',   '관리자 상태',   '관리자 계정 상태 코드',     'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODE_GROUPS (group_code, group_name, description, use_yn, created_at, deleted_yn)
VALUES ('LOGIN_RESULT',   '로그인 결과',   '로그인 시도 결과 코드',     'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODE_GROUPS (group_code, group_name, description, use_yn, created_at, deleted_yn)
VALUES ('USER_TYPE',      '사용자 유형',   '일반회원/관리자 구분',      'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODE_GROUPS (group_code, group_name, description, use_yn, created_at, deleted_yn)
VALUES ('ACTOR_TYPE',     '액터 유형',     '감사 로그 액터 유형',       'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODE_GROUPS (group_code, group_name, description, use_yn, created_at, deleted_yn)
VALUES ('ACTION_TYPE',    '액션 유형',     '감사 로그 액션 유형',       'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODE_GROUPS (group_code, group_name, description, use_yn, created_at, deleted_yn)
VALUES ('TARGET_TYPE',    '대상 유형',     '감사 로그 대상 유형',       'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODE_GROUPS (group_code, group_name, description, use_yn, created_at, deleted_yn)
VALUES ('APPROVAL_STATUS','결재 상태',     '결재 요청 상태 코드',       'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODE_GROUPS (group_code, group_name, description, use_yn, created_at, deleted_yn)
VALUES ('REQUEST_TYPE',   '요청 유형',     '결재 요청 유형',            'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODE_GROUPS (group_code, group_name, description, use_yn, created_at, deleted_yn)
-- ★ JOB_CODE: 애플리케이션(signup.html, edit.html, USERS.job) 기준으로 통일
VALUES ('JOB_CODE',       '직업 코드',     '사용자 직업 분류',          'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODE_GROUPS (group_code, group_name, description, use_yn, created_at, deleted_yn)
-- ★ INCOME_LEVEL: 애플리케이션(signup.html, edit.html) 기준 LV1~LV4 4단계로 통일
VALUES ('INCOME_LEVEL',   '소득 수준',     '사용자 소득 등급',          'Y', SYSTIMESTAMP, 'N');

COMMIT;

-- ── COMMON_CODES ─────────────────────────────────────────────────

-- USER_STATUS
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('USER_STATUS', 'ACTIVE',    '정상', 'active',    1, '정상 사용 가능 상태',          'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('USER_STATUS', 'DORMANT',   '휴면', 'dormant',   2, '장기 미접속 휴면 상태',        'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('USER_STATUS', 'LOCKED',    '잠금', 'locked',    3, '로그인 실패 초과로 잠금',      'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('USER_STATUS', 'WITHDRAWN', '탈퇴', 'withdrawn', 4, '회원 탈퇴 처리됨',             'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('USER_STATUS', 'SUSPENDED', '정지', 'suspended', 5, '관리자 정지 처리',             'Y', SYSTIMESTAMP, 'N');

-- ADMIN_STATUS
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('ADMIN_STATUS', 'ACTIVE',   '정상',   'active',   1, '정상 활성 관리자',            'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('ADMIN_STATUS', 'INACTIVE', '비활성', 'inactive', 2, '비활성 처리된 관리자',        'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('ADMIN_STATUS', 'LOCKED',   '잠금',   'locked',   3, '잠금 처리된 관리자',          'Y', SYSTIMESTAMP, 'N');

-- LOGIN_RESULT
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('LOGIN_RESULT', 'SUCCESS',     '성공',       'success',     1, '로그인 성공',              'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('LOGIN_RESULT', 'FAIL_PW',     '비밀번호 오류', 'fail_pw',  2, '비밀번호 불일치',          'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('LOGIN_RESULT', 'FAIL_LOCKED', '계정 잠금',  'fail_locked', 3, '계정 잠금 상태',           'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('LOGIN_RESULT', 'FAIL_NOUSER', '미존재 계정','fail_nouser', 4, '존재하지 않는 계정',       'Y', SYSTIMESTAMP, 'N');

-- USER_TYPE
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('USER_TYPE', 'USER',  '일반회원', 'user',  1, '일반 사용자', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('USER_TYPE', 'ADMIN', '관리자',   'admin', 2, '관리자',      'Y', SYSTIMESTAMP, 'N');

-- ACTOR_TYPE
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('ACTOR_TYPE', 'USER',   '사용자', 'user',   1, '일반 사용자',    'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('ACTOR_TYPE', 'ADMIN',  '관리자', 'admin',  2, '관리자',         'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('ACTOR_TYPE', 'SYSTEM', '시스템', 'system', 3, '시스템 자동',    'Y', SYSTIMESTAMP, 'N');

-- ACTION_TYPE  ★ USER_UPDATE/PASSWORD_CHANGE/STATUS_CHANGE/WITHDRAW 추가
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('ACTION_TYPE', 'CREATE',          '생성',           'create',          1,  '데이터 생성',                        'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('ACTION_TYPE', 'UPDATE',          '수정',           'update',          2,  '데이터 수정',                        'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('ACTION_TYPE', 'DELETE',          '삭제',           'delete',          3,  '데이터 삭제',                        'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('ACTION_TYPE', 'LOGIN',           '로그인',         'login',           4,  '로그인',                             'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('ACTION_TYPE', 'LOGOUT',          '로그아웃',       'logout',          5,  '로그아웃',                           'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('ACTION_TYPE', 'APPROVE',         '승인',           'approve',         6,  '결재 승인',                          'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('ACTION_TYPE', 'READ',            '조회',           'read',            7,  '데이터 조회',                        'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('ACTION_TYPE', 'REJECT',          '반려',           'reject',          8,  '결재 반려',                          'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('ACTION_TYPE', 'USER_UPDATE',     '회원정보수정',   'user_update',     9,  '마이페이지 회원정보 수정',           'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('ACTION_TYPE', 'PASSWORD_CHANGE', '비밀번호변경',   'password_change', 10, '비밀번호 변경 (세션 무효화 포함)',   'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('ACTION_TYPE', 'STATUS_CHANGE',   '상태변경',       'status_change',   11, '계정 상태 변경 (관리자)',            'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('ACTION_TYPE', 'WITHDRAW',        '탈퇴처리',       'withdraw',        12, '회원 탈퇴 처리 및 개인정보 삭제',   'Y', SYSTIMESTAMP, 'N');

-- TARGET_TYPE  ★ CARD_APPLICATION/ADMIN/APPROVAL 추가
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('TARGET_TYPE', 'USER',             '회원',       'user',             1, '일반 회원',       'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('TARGET_TYPE', 'CARD',             '카드',       'card',             2, '카드 상품',       'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('TARGET_TYPE', 'TERMS',            '약관',       'terms',            3, '약관',            'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('TARGET_TYPE', 'CARD_APPLICATION', '카드신청',   'card_application', 4, '카드 신청 처리', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('TARGET_TYPE', 'ADMIN',            '관리자',     'admin',            5, '관리자 계정',     'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('TARGET_TYPE', 'APPROVAL',         '결재',       'approval',         6, '결재 처리',       'Y', SYSTIMESTAMP, 'N');

-- APPROVAL_STATUS
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('APPROVAL_STATUS', 'PENDING',  '대기', 'pending',  1, '승인 대기', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('APPROVAL_STATUS', 'APPROVED', '승인', 'approved', 2, '승인 완료', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('APPROVAL_STATUS', 'REJECTED', '반려', 'rejected', 3, '반려 처리', 'Y', SYSTIMESTAMP, 'N');

-- REQUEST_TYPE
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('REQUEST_TYPE', 'CARD_PUBLISH',  '카드 게시', 'card_publish',  1, '카드 상품 게시 요청', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('REQUEST_TYPE', 'TERMS_PUBLISH', '약관 게시', 'terms_pub',     2, '약관 게시 요청',      'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('REQUEST_TYPE', 'USER_SUSPEND',  '회원 정지', 'user_suspend',  3, '회원 정지 요청',      'Y', SYSTIMESTAMP, 'N');

-- JOB_CODE  ★ 구 코드(EMPLOYEE/SELF_EMP/HOUSEWIFE/FREELANCER/ETC) 제거
--             → 애플리케이션 실제 사용 코드로 통일
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('JOB_CODE', 'EMPLOYED',      '직장인',   'employed',      1, '일반 직장인 (급여소득자)',       'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('JOB_CODE', 'SELF_EMPLOYED', '자영업자', 'self_employed', 2, '개인사업자 (자영업)',            'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('JOB_CODE', 'STUDENT',       '학생',     'student',       3, '대학생 포함 학업 중인 자',      'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('JOB_CODE', 'UNEMPLOYED',    '무직',     'unemployed',    4, '무직·전업주부 등 소득 없음',    'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('JOB_CODE', 'OTHER',         '기타',     'other',         5, '기타 직업 (프리랜서 등)',       'Y', SYSTIMESTAMP, 'N');

-- INCOME_LEVEL  ★ 구 코드(HIGH/MID_HIGH/MID/LOW_MID/LOW) 제거
--                → 애플리케이션 실제 사용 코드 LV1~LV4로 통일
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('INCOME_LEVEL', 'LV1', 'LV1 (3천만 미만)', 'lv1', 1, '연 3천만원 미만',     'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('INCOME_LEVEL', 'LV2', 'LV2 (3천~5천만)', 'lv2',  2, '연 3천~5천만원',     'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('INCOME_LEVEL', 'LV3', 'LV3 (5천만~1억)', 'lv3',  3, '연 5천만~1억원',     'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('INCOME_LEVEL', 'LV4', 'LV4 (1억 이상)',  'lv4',  4, '연 1억원 이상',      'Y', SYSTIMESTAMP, 'N');

COMMIT;


-- ── ADMIN_ROLES ───────────────────────────────────────────────────
INSERT INTO ADMIN_ROLES (role_code, role_name, description, created_at, deleted_yn)
VALUES ('SUPER_ADMIN', '최상위 관리자', '모든 기능 접근 가능한 슈퍼 관리자',         SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_ROLES (role_code, role_name, description, created_at, deleted_yn)
VALUES ('MANAGER',     '중간 관리자',   '카드/약관 관리 및 결재 처리 가능',          SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_ROLES (role_code, role_name, description, created_at, deleted_yn)
VALUES ('OPERATOR',    '하위 운영자',   '조회 및 기본 운영 업무만 가능',             SYSTIMESTAMP, 'N');

COMMIT;

-- ── ADMIN_PERMISSIONS ─────────────────────────────────────────────
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('CARD_VIEW',             '카드 조회',           SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('CARD_CREATE',           '카드 생성',           SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('CARD_UPDATE',           '카드 수정',           SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('CARD_DELETE',           '카드 삭제',           SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('CARD_PUBLISH',          '카드 게시 승인',      SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('TERMS_VIEW',            '약관 조회',           SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('TERMS_CREATE',          '약관 생성',           SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('TERMS_UPDATE',          '약관 수정',           SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('TERMS_PUBLISH',         '약관 게시 승인',      SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('USER_VIEW',             '회원 조회',           SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('USER_UPDATE',           '회원 정보 수정',      SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('USER_SUSPEND',          '회원 정지 처리',      SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('ADMIN_MANAGE',          '관리자 계정 관리',    SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('ROLE_MANAGE',           '역할/권한 관리',      SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('APPROVAL_REQUEST',      '결재 요청',           SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('APPROVAL_PROCESS',      '결재 처리',           SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('AUDIT_LOG_VIEW',        '감사 로그 조회',      SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('SEARCH_KEYWORD_MANAGE', '검색 키워드 관리',    SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('STATISTICS_VIEW',       '통계 조회',           SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('SYSTEM_CONFIG',         '시스템 설정',         SYSTIMESTAMP, 'N');

COMMIT;

-- ── ROLE_PERMISSIONS ──────────────────────────────────────────────
-- SUPER_ADMIN(role_id=1): 전체 20개 권한
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1,  1,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1,  2,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1,  3,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1,  4,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1,  5,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1,  6,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1,  7,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1,  8,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1,  9,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1,  10, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1,  11, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1,  12, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1,  13, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1,  14, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1,  15, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1,  16, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1,  17, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1,  18, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1,  19, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1,  20, SYSTIMESTAMP, 'N');
-- MANAGER(role_id=2): 카드/약관/회원/결재 관련 권한 (13개)
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2,  1,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2,  2,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2,  3,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2,  5,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2,  6,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2,  7,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2,  8,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2,  9,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2,  10, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2,  15, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2,  16, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2,  17, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2,  19, SYSTIMESTAMP, 'N');
-- OPERATOR(role_id=3): 조회/검색키워드/통계 (6개)
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (3,  1,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (3,  6,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (3,  10, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (3,  17, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (3,  18, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (3,  19, SYSTIMESTAMP, 'N');

COMMIT;

-- ── ADMIN_USERS (10명) ────────────────────────────────────────────
-- 공통 password: Test1234! / BCrypt: $2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm
INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
VALUES ('super_admin',    '$2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm', '김슈퍼', 'super@bnkfinance.co.kr',  '010-9001-0001', 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
VALUES ('card_manager1',  '$2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm', '이카드', 'card1@bnkfinance.co.kr',  '010-9001-0002', 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
VALUES ('card_manager2',  '$2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm', '박상품', 'card2@bnkfinance.co.kr',  '010-9001-0003', 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
VALUES ('card_manager3',  '$2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm', '최운영', 'card3@bnkfinance.co.kr',  '010-9001-0004', 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
VALUES ('terms_manager1', '$2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm', '정약관', 'terms1@bnkfinance.co.kr', '010-9001-0005', 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
VALUES ('terms_manager2', '$2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm', '한법무', 'terms2@bnkfinance.co.kr', '010-9001-0006', 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
VALUES ('viewer1',        '$2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm', '윤조회', 'view1@bnkfinance.co.kr',  '010-9001-0007', 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
VALUES ('viewer2',        '$2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm', '임뷰어', 'view2@bnkfinance.co.kr',  '010-9001-0008', 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
VALUES ('viewer3',        '$2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm', '신열람', 'view3@bnkfinance.co.kr',  '010-9001-0009', 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
VALUES ('viewer4',        '$2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm', '권뷰어', 'view4@bnkfinance.co.kr',  '010-9001-0010', 'ACTIVE', SYSTIMESTAMP, 'N');

COMMIT;

-- ── ADMIN_USER_ROLES ──────────────────────────────────────────────
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn) VALUES (1,  1, SYSTIMESTAMP, 1, 'N');
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn) VALUES (2,  2, SYSTIMESTAMP, 1, 'N');
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn) VALUES (3,  2, SYSTIMESTAMP, 1, 'N');
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn) VALUES (4,  2, SYSTIMESTAMP, 1, 'N');
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn) VALUES (5,  3, SYSTIMESTAMP, 1, 'N');
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn) VALUES (6,  3, SYSTIMESTAMP, 1, 'N');
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn) VALUES (7,  3, SYSTIMESTAMP, 1, 'N');
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn) VALUES (8,  3, SYSTIMESTAMP, 1, 'N');
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn) VALUES (9,  3, SYSTIMESTAMP, 1, 'N');
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn) VALUES (10, 3, SYSTIMESTAMP, 1, 'N');

COMMIT;