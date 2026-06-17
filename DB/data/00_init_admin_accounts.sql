-- ================================================================
-- BNK 부산은행 금융 상품 플랫폼
-- [초기 데이터] 관리자 계정 · 역할 · 권한
-- Oracle 21c
--
-- ※ 이 파일만 실행하면 시스템 운영에 필요한 관리자 계정이 셋업됩니다.
-- ※ 나머지 더미데이터(카드상품, 약관, 회원 등)는 제거되었습니다.
--    실제 데이터는 관리자 화면에서 직접 등록하거나 별도 마이그레이션 스크립트를 사용하세요.
--
-- [포함 내용]
--   1. ADMIN_ROLES    — 역할 3종 (SUPER_ADMIN, MANAGER, OPERATOR)
--   2. ADMIN_PERMISSIONS — 권한 20종
--   3. ROLE_PERMISSIONS  — 역할-권한 매핑 (39건)
--   4. ADMIN_USERS    — 관리자 계정 10명
--   5. ADMIN_USER_ROLES  — 관리자-역할 매핑 (10건)
--
-- [비밀번호]
--   모든 계정 초기 비밀번호: Admin1234!
--   BCrypt hash: $2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm
--   ※ 운영 배포 전 반드시 개별 비밀번호로 변경하세요.
-- ================================================================


-- ================================================================
-- [01] ADMIN_ROLES — 역할 정의
-- ================================================================
INSERT INTO ADMIN_ROLES (role_code, role_name, description, created_at, deleted_yn)
VALUES ('SUPER_ADMIN', '최상위 관리자', '모든 기능 접근 가능한 슈퍼 관리자', SYSTIMESTAMP, 'N');

INSERT INTO ADMIN_ROLES (role_code, role_name, description, created_at, deleted_yn)
VALUES ('MANAGER', '중간 관리자', '카드/약관 관리 및 결재 처리 가능', SYSTIMESTAMP, 'N');

INSERT INTO ADMIN_ROLES (role_code, role_name, description, created_at, deleted_yn)
VALUES ('OPERATOR', '하위 운영자', '조회 및 기본 운영 업무만 가능', SYSTIMESTAMP, 'N');

COMMIT;


-- ================================================================
-- [02] ADMIN_PERMISSIONS — 권한 정의 (20종)
-- ================================================================
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('CARD_VIEW',            '카드 조회',        SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('CARD_CREATE',          '카드 생성',        SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('CARD_UPDATE',          '카드 수정',        SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('CARD_DELETE',          '카드 삭제',        SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('CARD_PUBLISH',         '카드 게시 승인',   SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('TERMS_VIEW',           '약관 조회',        SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('TERMS_CREATE',         '약관 생성',        SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('TERMS_UPDATE',         '약관 수정',        SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('TERMS_PUBLISH',        '약관 게시 승인',   SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('USER_VIEW',            '회원 조회',        SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('USER_UPDATE',          '회원 정보 수정',   SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('USER_SUSPEND',         '회원 정지 처리',   SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('ADMIN_MANAGE',         '관리자 계정 관리', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('ROLE_MANAGE',          '역할/권한 관리',   SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('APPROVAL_REQUEST',     '결재 요청',        SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('APPROVAL_PROCESS',     '결재 처리',        SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('AUDIT_LOG_VIEW',       '감사 로그 조회',   SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('SEARCH_KEYWORD_MANAGE','검색 키워드 관리', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('STATISTICS_VIEW',      '통계 조회',        SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) VALUES ('SYSTEM_CONFIG',        '시스템 설정',      SYSTIMESTAMP, 'N');

COMMIT;


-- ================================================================
-- [03] ROLE_PERMISSIONS — 역할-권한 매핑
-- ================================================================
-- SUPER_ADMIN (role_id=1): 전체 20개 권한
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1,  1, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1,  2, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1,  3, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1,  4, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1,  5, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1,  6, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1,  7, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1,  8, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1,  9, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1, 10, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1, 11, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1, 12, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1, 13, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1, 14, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1, 15, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1, 16, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1, 17, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1, 18, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1, 19, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1, 20, SYSTIMESTAMP, 'N');

-- MANAGER (role_id=2): 카드/약관/회원/결재 관련 13개 권한
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2,  1, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2,  2, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2,  3, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2,  5, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2,  6, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2,  7, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2,  8, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2,  9, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2, 10, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2, 15, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2, 16, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2, 17, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2, 19, SYSTIMESTAMP, 'N');

-- OPERATOR (role_id=3): 조회/검색키워드/통계 6개 권한
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (3,  1, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (3,  6, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (3, 10, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (3, 17, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (3, 18, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (3, 19, SYSTIMESTAMP, 'N');

COMMIT;


-- ================================================================
-- [04] ADMIN_USERS — 관리자 계정 10명
-- [비밀번호] 초기값: Admin1234!
--   BCrypt: $2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm
-- ================================================================
INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
VALUES ('super_admin',   '$2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm', '김슈퍼', 'super@bnkfinance.co.kr',   '010-9001-0001', 'ACTIVE', SYSTIMESTAMP, 'N');

INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
VALUES ('card_manager1', '$2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm', '이카드', 'card1@bnkfinance.co.kr',   '010-9001-0002', 'ACTIVE', SYSTIMESTAMP, 'N');

INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
VALUES ('card_manager2', '$2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm', '박상품', 'card2@bnkfinance.co.kr',   '010-9001-0003', 'ACTIVE', SYSTIMESTAMP, 'N');

INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
VALUES ('card_manager3', '$2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm', '최운영', 'card3@bnkfinance.co.kr',   '010-9001-0004', 'ACTIVE', SYSTIMESTAMP, 'N');

INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
VALUES ('terms_manager1','$2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm', '정약관', 'terms1@bnkfinance.co.kr',  '010-9001-0005', 'ACTIVE', SYSTIMESTAMP, 'N');

INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
VALUES ('terms_manager2','$2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm', '한법무', 'terms2@bnkfinance.co.kr',  '010-9001-0006', 'ACTIVE', SYSTIMESTAMP, 'N');

INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
VALUES ('viewer1',       '$2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm', '윤조회', 'view1@bnkfinance.co.kr',   '010-9001-0007', 'ACTIVE', SYSTIMESTAMP, 'N');

INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
VALUES ('viewer2',       '$2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm', '임뷰어', 'view2@bnkfinance.co.kr',   '010-9001-0008', 'ACTIVE', SYSTIMESTAMP, 'N');

INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
VALUES ('viewer3',       '$2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm', '신열람', 'view3@bnkfinance.co.kr',   '010-9001-0009', 'ACTIVE', SYSTIMESTAMP, 'N');

INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
VALUES ('viewer4',       '$2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm', '권뷰어', 'view4@bnkfinance.co.kr',   '010-9001-0010', 'ACTIVE', SYSTIMESTAMP, 'N');

COMMIT;


-- ================================================================
-- [05] ADMIN_USER_ROLES — 관리자-역할 매핑
-- ================================================================
-- super_admin(1)   → SUPER_ADMIN(1)
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn) VALUES (1,  1, SYSTIMESTAMP, 1, 'N');
-- card_manager1(2) → MANAGER(2)
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn) VALUES (2,  2, SYSTIMESTAMP, 1, 'N');
-- card_manager2(3) → MANAGER(2)
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn) VALUES (3,  2, SYSTIMESTAMP, 1, 'N');
-- card_manager3(4) → MANAGER(2)
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn) VALUES (4,  2, SYSTIMESTAMP, 1, 'N');
-- terms_manager1(5) → MANAGER(2)
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn) VALUES (5,  2, SYSTIMESTAMP, 1, 'N');
-- terms_manager2(6) → MANAGER(2)
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn) VALUES (6,  2, SYSTIMESTAMP, 1, 'N');
-- viewer1(7)       → OPERATOR(3)
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn) VALUES (7,  3, SYSTIMESTAMP, 1, 'N');
-- viewer2(8)       → OPERATOR(3)
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn) VALUES (8,  3, SYSTIMESTAMP, 1, 'N');
-- viewer3(9)       → OPERATOR(3)
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn) VALUES (9,  3, SYSTIMESTAMP, 1, 'N');
-- viewer4(10)      → OPERATOR(3)
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn) VALUES (10, 3, SYSTIMESTAMP, 1, 'N');

COMMIT;


-- ================================================================
-- [완료] 확인 쿼리
-- ================================================================
/*
-- 관리자 계정 + 역할 확인
SELECT au.admin_id, au.username, au.name, ar.role_code, au.status_code
FROM   ADMIN_USERS au
JOIN   ADMIN_USER_ROLES aur ON au.admin_id  = aur.admin_id  AND aur.deleted_yn = 'N'
JOIN   ADMIN_ROLES ar       ON aur.role_id  = ar.role_id    AND ar.deleted_yn  = 'N'
WHERE  au.deleted_yn = 'N'
ORDER  BY au.admin_id;

-- 역할별 권한 수 확인
SELECT ar.role_code, COUNT(*) AS perm_count
FROM   ADMIN_ROLES ar
JOIN   ROLE_PERMISSIONS rp ON ar.role_id = rp.role_id AND rp.deleted_yn = 'N'
WHERE  ar.deleted_yn = 'N'
GROUP  BY ar.role_code
ORDER  BY ar.role_code;
*/
