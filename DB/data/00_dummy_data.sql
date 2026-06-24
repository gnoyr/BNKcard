-- ================================================================
-- BNK 부산은행 금융 상품 플랫폼
-- 통합 더미데이터 INSERT
-- 대상 DDL: 01_ddl_common_admin.sql, 03_ddl_card_product.sql,
--           04_ddl_terms.sql, 06_ddl_file_search_ai_log.sql (최신화 버전)
--
-- ■ 포함 범위
--   01. ADMIN_ROLES
--   02. ADMIN_PERMISSIONS
--   03. ROLE_PERMISSIONS
--   04. ADMIN_USERS (10명)
--   05. ADMIN_USER_ROLES
--   06. CARD_CATEGORIES (23건)
--   07. CARDS (27건) ← brand_name, annual_fee_domestic 등 신규 컬럼 포함
--   08. CARD_BENEFITS
--   09. CARD_IMAGES
--   10. CARD_CONTENTS
--   11. CARD_ATTRIBUTE_DEFINITIONS
--   12. CARD_ATTRIBUTE_VALUES
--   13. CARD_TAGS
--   14. CARD_TAG_MAP
--   15. CARD_STATUS_HISTORIES
--   16. TERMS_GROUPS
--   17. TERMS_MASTERS
--   18. TERMS
--   19. CARD_TERMS
--   20. TERMS_PACKAGES / PACKAGE_TERMS
--   21. SEARCH_KEYWORDS
--   22. CARD_KEYWORDS
--   23. 시퀀스 재설정
--
-- ■ 제외 범위 (불필요)
--   USERS, USER_SESSIONS, LOGIN_HISTORIES, AUDIT_LOGS
--   APPROVAL_REQUESTS, APPROVAL_LINES
--   CREDIT_CARD_APPLICATIONS, CHECK_CARD_APPLICATIONS
--   USER_CARDS, USER_SPENDING_PATTERNS
--
-- ■ 실행 순서: 01~06 DDL 완료 후 본 파일 실행
-- ■ 재실행 안전: 모든 INSERT에 WHERE NOT EXISTS 패턴 적용
-- ================================================================
SET DEFINE OFF;
SET VERIFY OFF;


-- ================================================================
-- 01. ADMIN_ROLES (3건)
-- ================================================================
INSERT INTO ADMIN_ROLES (role_code, role_name, description, created_at, deleted_yn)
SELECT 'SUPER_ADMIN','최상위 관리자','모든 기능 접근 가능한 슈퍼 관리자',SYSTIMESTAMP,'N' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ADMIN_ROLES WHERE role_code='SUPER_ADMIN' AND deleted_yn='N');

INSERT INTO ADMIN_ROLES (role_code, role_name, description, created_at, deleted_yn)
SELECT 'MANAGER','중간 관리자','카드/약관 관리 및 결재 처리 가능',SYSTIMESTAMP,'N' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ADMIN_ROLES WHERE role_code='MANAGER' AND deleted_yn='N');

INSERT INTO ADMIN_ROLES (role_code, role_name, description, created_at, deleted_yn)
SELECT 'OPERATOR','하위 운영자','조회 및 기본 운영 업무만 가능',SYSTIMESTAMP,'N' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ADMIN_ROLES WHERE role_code='OPERATOR' AND deleted_yn='N');

COMMIT;


-- ================================================================
-- 02. ADMIN_PERMISSIONS (20건)
-- ================================================================
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) SELECT 'CARD_VIEW',             '카드 조회',         SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ADMIN_PERMISSIONS WHERE permission_code='CARD_VIEW');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) SELECT 'CARD_CREATE',           '카드 생성',         SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ADMIN_PERMISSIONS WHERE permission_code='CARD_CREATE');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) SELECT 'CARD_UPDATE',           '카드 수정',         SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ADMIN_PERMISSIONS WHERE permission_code='CARD_UPDATE');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) SELECT 'CARD_DELETE',           '카드 삭제',         SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ADMIN_PERMISSIONS WHERE permission_code='CARD_DELETE');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) SELECT 'CARD_PUBLISH',          '카드 게시 승인',    SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ADMIN_PERMISSIONS WHERE permission_code='CARD_PUBLISH');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) SELECT 'TERMS_VIEW',            '약관 조회',         SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ADMIN_PERMISSIONS WHERE permission_code='TERMS_VIEW');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) SELECT 'TERMS_CREATE',          '약관 생성',         SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ADMIN_PERMISSIONS WHERE permission_code='TERMS_CREATE');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) SELECT 'TERMS_UPDATE',          '약관 수정',         SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ADMIN_PERMISSIONS WHERE permission_code='TERMS_UPDATE');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) SELECT 'TERMS_PUBLISH',         '약관 게시 승인',    SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ADMIN_PERMISSIONS WHERE permission_code='TERMS_PUBLISH');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) SELECT 'USER_VIEW',             '회원 조회',         SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ADMIN_PERMISSIONS WHERE permission_code='USER_VIEW');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) SELECT 'USER_UPDATE',           '회원 정보 수정',    SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ADMIN_PERMISSIONS WHERE permission_code='USER_UPDATE');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) SELECT 'USER_SUSPEND',          '회원 정지 처리',    SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ADMIN_PERMISSIONS WHERE permission_code='USER_SUSPEND');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) SELECT 'ADMIN_MANAGE',          '관리자 계정 관리',  SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ADMIN_PERMISSIONS WHERE permission_code='ADMIN_MANAGE');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) SELECT 'ROLE_MANAGE',           '역할/권한 관리',    SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ADMIN_PERMISSIONS WHERE permission_code='ROLE_MANAGE');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) SELECT 'APPROVAL_REQUEST',      '결재 요청',         SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ADMIN_PERMISSIONS WHERE permission_code='APPROVAL_REQUEST');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) SELECT 'APPROVAL_PROCESS',      '결재 처리',         SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ADMIN_PERMISSIONS WHERE permission_code='APPROVAL_PROCESS');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) SELECT 'AUDIT_LOG_VIEW',        '감사 로그 조회',    SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ADMIN_PERMISSIONS WHERE permission_code='AUDIT_LOG_VIEW');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) SELECT 'SEARCH_KEYWORD_MANAGE', '검색 키워드 관리',  SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ADMIN_PERMISSIONS WHERE permission_code='SEARCH_KEYWORD_MANAGE');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) SELECT 'STATISTICS_VIEW',       '통계 조회',         SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ADMIN_PERMISSIONS WHERE permission_code='STATISTICS_VIEW');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn) SELECT 'SYSTEM_CONFIG',         '시스템 설정',       SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ADMIN_PERMISSIONS WHERE permission_code='SYSTEM_CONFIG');

COMMIT;


-- ================================================================
-- 03. ROLE_PERMISSIONS
-- ================================================================
-- SUPER_ADMIN(role_id=1): 전체 20개 권한
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 1, 1,  SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=1 AND permission_id=1);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 1, 2,  SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=1 AND permission_id=2);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 1, 3,  SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=1 AND permission_id=3);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 1, 4,  SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=1 AND permission_id=4);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 1, 5,  SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=1 AND permission_id=5);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 1, 6,  SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=1 AND permission_id=6);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 1, 7,  SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=1 AND permission_id=7);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 1, 8,  SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=1 AND permission_id=8);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 1, 9,  SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=1 AND permission_id=9);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 1, 10, SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=1 AND permission_id=10);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 1, 11, SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=1 AND permission_id=11);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 1, 12, SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=1 AND permission_id=12);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 1, 13, SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=1 AND permission_id=13);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 1, 14, SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=1 AND permission_id=14);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 1, 15, SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=1 AND permission_id=15);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 1, 16, SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=1 AND permission_id=16);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 1, 17, SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=1 AND permission_id=17);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 1, 18, SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=1 AND permission_id=18);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 1, 19, SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=1 AND permission_id=19);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 1, 20, SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=1 AND permission_id=20);
-- MANAGER(role_id=2): 13개
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 2, 1,  SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=2 AND permission_id=1);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 2, 2,  SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=2 AND permission_id=2);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 2, 3,  SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=2 AND permission_id=3);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 2, 5,  SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=2 AND permission_id=5);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 2, 6,  SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=2 AND permission_id=6);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 2, 7,  SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=2 AND permission_id=7);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 2, 8,  SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=2 AND permission_id=8);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 2, 9,  SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=2 AND permission_id=9);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 2, 10, SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=2 AND permission_id=10);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 2, 15, SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=2 AND permission_id=15);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 2, 16, SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=2 AND permission_id=16);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 2, 17, SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=2 AND permission_id=17);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 2, 19, SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=2 AND permission_id=19);
-- OPERATOR(role_id=3): 6개
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 3, 1,  SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=3 AND permission_id=1);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 3, 6,  SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=3 AND permission_id=6);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 3, 10, SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=3 AND permission_id=10);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 3, 17, SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=3 AND permission_id=17);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 3, 18, SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=3 AND permission_id=18);
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) SELECT 3, 19, SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE_PERMISSIONS WHERE role_id=3 AND permission_id=19);

COMMIT;


-- ================================================================
-- 04. ADMIN_USERS (10명)
-- 공통 password: Test1234!
-- BCrypt: $2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm
-- ================================================================
INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
SELECT 'super_admin','$2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm','김슈퍼','super@bnkfinance.co.kr','010-9001-0001','ACTIVE',SYSTIMESTAMP,'N' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ADMIN_USERS WHERE username='super_admin');

INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
SELECT 'card_manager1','$2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm','이카드','card1@bnkfinance.co.kr','010-9001-0002','ACTIVE',SYSTIMESTAMP,'N' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ADMIN_USERS WHERE username='card_manager1');

INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
SELECT 'card_manager2','$2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm','박상품','card2@bnkfinance.co.kr','010-9001-0003','ACTIVE',SYSTIMESTAMP,'N' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ADMIN_USERS WHERE username='card_manager2');

INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
SELECT 'card_manager3','$2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm','최운영','card3@bnkfinance.co.kr','010-9001-0004','ACTIVE',SYSTIMESTAMP,'N' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ADMIN_USERS WHERE username='card_manager3');

INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
SELECT 'terms_manager1','$2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm','정약관','terms1@bnkfinance.co.kr','010-9001-0005','ACTIVE',SYSTIMESTAMP,'N' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ADMIN_USERS WHERE username='terms_manager1');

INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
SELECT 'terms_manager2','$2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm','한법무','terms2@bnkfinance.co.kr','010-9001-0006','ACTIVE',SYSTIMESTAMP,'N' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ADMIN_USERS WHERE username='terms_manager2');

INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
SELECT 'viewer1','$2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm','윤조회','view1@bnkfinance.co.kr','010-9001-0007','ACTIVE',SYSTIMESTAMP,'N' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ADMIN_USERS WHERE username='viewer1');

INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
SELECT 'viewer2','$2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm','임뷰어','view2@bnkfinance.co.kr','010-9001-0008','ACTIVE',SYSTIMESTAMP,'N' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ADMIN_USERS WHERE username='viewer2');

INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
SELECT 'viewer3','$2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm','신열람','view3@bnkfinance.co.kr','010-9001-0009','ACTIVE',SYSTIMESTAMP,'N' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ADMIN_USERS WHERE username='viewer3');

INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
SELECT 'viewer4','$2a$12$TB8JjfqqjuPpyOaCM15nE.0fCY8ZWwjjywZ6rYshngwPefDxphRIm','권뷰어','view4@bnkfinance.co.kr','010-9001-0010','ACTIVE',SYSTIMESTAMP,'N' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ADMIN_USERS WHERE username='viewer4');

COMMIT;


-- ================================================================
-- 05. ADMIN_USER_ROLES
-- ================================================================
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn) SELECT 1,  1, SYSTIMESTAMP, 1,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ADMIN_USER_ROLES WHERE admin_id=1  AND role_id=1);
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn) SELECT 2,  2, SYSTIMESTAMP, 1,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ADMIN_USER_ROLES WHERE admin_id=2  AND role_id=2);
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn) SELECT 3,  2, SYSTIMESTAMP, 1,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ADMIN_USER_ROLES WHERE admin_id=3  AND role_id=2);
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn) SELECT 4,  2, SYSTIMESTAMP, 1,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ADMIN_USER_ROLES WHERE admin_id=4  AND role_id=2);
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn) SELECT 5,  3, SYSTIMESTAMP, 1,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ADMIN_USER_ROLES WHERE admin_id=5  AND role_id=3);
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn) SELECT 6,  3, SYSTIMESTAMP, 1,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ADMIN_USER_ROLES WHERE admin_id=6  AND role_id=3);
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn) SELECT 7,  3, SYSTIMESTAMP, 1,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ADMIN_USER_ROLES WHERE admin_id=7  AND role_id=3);
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn) SELECT 8,  3, SYSTIMESTAMP, 1,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ADMIN_USER_ROLES WHERE admin_id=8  AND role_id=3);
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn) SELECT 9,  3, SYSTIMESTAMP, 1,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ADMIN_USER_ROLES WHERE admin_id=9  AND role_id=3);
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn) SELECT 10, 3, SYSTIMESTAMP, 1,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ADMIN_USER_ROLES WHERE admin_id=10 AND role_id=3);

COMMIT;


-- ================================================================
-- 06. CARD_CATEGORIES (23건)
-- ★ 최신화: category_code, icon_code 컬럼 추가 (03_ddl 기준)
-- ================================================================
INSERT INTO CARD_CATEGORIES (category_code, category_name, category_icon, icon_code, display_order, use_yn, created_at)
SELECT 'TRAVEL',     '여행/항공',        'icon-travel',      'icon-travel',      1,  'Y', SYSTIMESTAMP FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARD_CATEGORIES WHERE category_name='여행/항공');
INSERT INTO CARD_CATEGORIES (category_code, category_name, category_icon, icon_code, display_order, use_yn, created_at)
SELECT 'DINING',     '식음료/카페',      'icon-dining',      'icon-dining',      2,  'Y', SYSTIMESTAMP FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARD_CATEGORIES WHERE category_name='식음료/카페');
INSERT INTO CARD_CATEGORIES (category_code, category_name, category_icon, icon_code, display_order, use_yn, created_at)
SELECT 'SHOPPING',   '쇼핑/백화점',      'icon-shopping',    'icon-shopping',    3,  'Y', SYSTIMESTAMP FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARD_CATEGORIES WHERE category_name='쇼핑/백화점');
INSERT INTO CARD_CATEGORIES (category_code, category_name, category_icon, icon_code, display_order, use_yn, created_at)
SELECT 'OIL',        '주유/충전',        'icon-oil',         'icon-oil',         4,  'Y', SYSTIMESTAMP FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARD_CATEGORIES WHERE category_name='주유/충전');
INSERT INTO CARD_CATEGORIES (category_code, category_name, category_icon, icon_code, display_order, use_yn, created_at)
SELECT 'TRANSPORT',  '교통/대중교통',    'icon-transport',   'icon-transport',   5,  'Y', SYSTIMESTAMP FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARD_CATEGORIES WHERE category_name='교통/대중교통');
INSERT INTO CARD_CATEGORIES (category_code, category_name, category_icon, icon_code, display_order, use_yn, created_at)
SELECT 'LEISURE',    '여가/문화',        'icon-leisure',     'icon-leisure',     6,  'Y', SYSTIMESTAMP FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARD_CATEGORIES WHERE category_name='여가/문화');
INSERT INTO CARD_CATEGORIES (category_code, category_name, category_icon, icon_code, display_order, use_yn, created_at)
SELECT 'MEDICAL',    '의료/약국',        'icon-medical',     'icon-medical',     7,  'Y', SYSTIMESTAMP FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARD_CATEGORIES WHERE category_name='의료/약국');
INSERT INTO CARD_CATEGORIES (category_code, category_name, category_icon, icon_code, display_order, use_yn, created_at)
SELECT 'TELECOM',    '통신/휴대폰',      'icon-telecom',     'icon-telecom',     8,  'Y', SYSTIMESTAMP FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARD_CATEGORIES WHERE category_name='통신/휴대폰');
INSERT INTO CARD_CATEGORIES (category_code, category_name, category_icon, icon_code, display_order, use_yn, created_at)
SELECT 'ONLINE',     '온라인/구독',      'icon-online',      'icon-online',      9,  'Y', SYSTIMESTAMP FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARD_CATEGORIES WHERE category_name='온라인/구독');
INSERT INTO CARD_CATEGORIES (category_code, category_name, category_icon, icon_code, display_order, use_yn, created_at)
SELECT 'DELIVERY',   '배달/음식',        'icon-delivery',    'icon-delivery',    10, 'Y', SYSTIMESTAMP FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARD_CATEGORIES WHERE category_name='배달/음식');
INSERT INTO CARD_CATEGORIES (category_code, category_name, category_icon, icon_code, display_order, use_yn, created_at)
SELECT 'EDUCATION',  '교육/학원',        'icon-education',   'icon-education',   11, 'Y', SYSTIMESTAMP FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARD_CATEGORIES WHERE category_name='교육/학원');
INSERT INTO CARD_CATEGORIES (category_code, category_name, category_icon, icon_code, display_order, use_yn, created_at)
SELECT 'LIVING',     '생활/관리비',      'icon-living',      'icon-living',      12, 'Y', SYSTIMESTAMP FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARD_CATEGORIES WHERE category_name='생활/관리비');
INSERT INTO CARD_CATEGORIES (category_code, category_name, category_icon, icon_code, display_order, use_yn, created_at)
SELECT 'INSURANCE',  '보험',             'icon-insurance',   'icon-insurance',   13, 'Y', SYSTIMESTAMP FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARD_CATEGORIES WHERE category_name='보험');
INSERT INTO CARD_CATEGORIES (category_code, category_name, category_icon, icon_code, display_order, use_yn, created_at)
SELECT 'CASHBACK',   '캐시백/포인트',    'icon-cashback',    'icon-cashback',    14, 'Y', SYSTIMESTAMP FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARD_CATEGORIES WHERE category_name='캐시백/포인트');
INSERT INTO CARD_CATEGORIES (category_code, category_name, category_icon, icon_code, display_order, use_yn, created_at)
SELECT 'SPORT',      '스포츠/레저',      'icon-sport',       'icon-sport',       15, 'Y', SYSTIMESTAMP FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARD_CATEGORIES WHERE category_name='스포츠/레저');
INSERT INTO CARD_CATEGORIES (category_code, category_name, category_icon, icon_code, display_order, use_yn, created_at)
SELECT 'CONVENIENCE','편의점',           'icon-convenience', 'icon-convenience', 16, 'Y', SYSTIMESTAMP FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARD_CATEGORIES WHERE category_name='편의점');
INSERT INTO CARD_CATEGORIES (category_code, category_name, category_icon, icon_code, display_order, use_yn, created_at)
SELECT 'TOLL',       '고속도로/하이패스','icon-toll',        'icon-toll',        17, 'Y', SYSTIMESTAMP FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARD_CATEGORIES WHERE category_name='고속도로/하이패스');
INSERT INTO CARD_CATEGORIES (category_code, category_name, category_icon, icon_code, display_order, use_yn, created_at)
SELECT 'PARKING',    '주차',             'icon-parking',     'icon-parking',     18, 'Y', SYSTIMESTAMP FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARD_CATEGORIES WHERE category_name='주차');
INSERT INTO CARD_CATEGORIES (category_code, category_name, category_icon, icon_code, display_order, use_yn, created_at)
SELECT 'BEAUTY',     '미용/세탁',        'icon-beauty',      'icon-beauty',      19, 'Y', SYSTIMESTAMP FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARD_CATEGORIES WHERE category_name='미용/세탁');
INSERT INTO CARD_CATEGORIES (category_code, category_name, category_icon, icon_code, display_order, use_yn, created_at)
SELECT 'MART',       '마트/대형마트',    'icon-mart',        'icon-mart',        20, 'Y', SYSTIMESTAMP FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARD_CATEGORIES WHERE category_name='마트/대형마트');
INSERT INTO CARD_CATEGORIES (category_code, category_name, category_icon, icon_code, display_order, use_yn, created_at)
SELECT 'LOCAL',      '지역화폐',         'icon-local',       'icon-local',       21, 'Y', SYSTIMESTAMP FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARD_CATEGORIES WHERE category_name='지역화폐');
INSERT INTO CARD_CATEGORIES (category_code, category_name, category_icon, icon_code, display_order, use_yn, created_at)
SELECT 'BUSINESS',   '사업지원',         'icon-business',    'icon-business',    22, 'Y', SYSTIMESTAMP FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARD_CATEGORIES WHERE category_name='사업지원');
INSERT INTO CARD_CATEGORIES (category_code, category_name, category_icon, icon_code, display_order, use_yn, created_at)
SELECT 'ETC',        '기타혜택',         'icon-etc',         'icon-etc',         23, 'Y', SYSTIMESTAMP FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARD_CATEGORIES WHERE category_name='기타혜택');

COMMIT;


-- ================================================================
-- 07. CARDS (27건)
-- ★ 최신화: brand_name, annual_fee_domestic, annual_fee_overseas,
--   previous_month_spend, minimum_age, target_user,
--   summary_description, searchable_yn, visible_yn,
--   approval_required_yn 컬럼 포함 (03_ddl 기준)
-- ================================================================

-- [신용카드 18건]
INSERT INTO CARDS (card_id,card_code,card_type,company_code,company_name,card_name,brand_name,annual_fee_domestic,annual_fee_overseas,previous_month_spend,minimum_age,target_user,summary_description,card_summary,searchable_yn,visible_yn,approval_required_yn,card_status,publish_start_at,created_at,deleted_yn)
SELECT 10101001,'REX2_POINT','CREDIT','01','BNK부산은행','REX2_포인트형(개인)','LOCAL',200000,230000,300000,19,'개인(가족회원)','The Return of Royalty, REXⅡ카드','The Return of Royalty, REXⅡ카드','Y','Y','Y','PUBLISHED',SYSTIMESTAMP,SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARDS WHERE card_id=10101001);
INSERT INTO CARDS (card_id,card_code,card_type,company_code,company_name,card_name,brand_name,annual_fee_domestic,annual_fee_overseas,previous_month_spend,minimum_age,target_user,summary_description,card_summary,searchable_yn,visible_yn,approval_required_yn,card_status,publish_start_at,created_at,deleted_yn)
SELECT 10101002,'REX2_MILE','CREDIT','01','BNK부산은행','REX2_대한항공마일리지형(개인)','LOCAL',200000,230000,300000,19,'개인(가족회원)','The Return of Royalty, REXⅡ카드','The Return of Royalty, REXⅡ카드','Y','Y','Y','PUBLISHED',SYSTIMESTAMP,SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARDS WHERE card_id=10101002);
INSERT INTO CARDS (card_id,card_code,card_type,company_code,company_name,card_name,brand_name,annual_fee_domestic,annual_fee_overseas,previous_month_spend,minimum_age,target_user,summary_description,card_summary,searchable_yn,visible_yn,approval_required_yn,card_status,publish_start_at,created_at,deleted_yn)
SELECT 10101003,'CASHBACK_CARD','CREDIT','01','BNK부산은행','캐쉬백카드','LOCAL',10000,10000,200000,19,'개인','매달 결제일에 최대 0.7% 캐쉬백혜택','매달 결제일에 최대 0.7% 캐쉬백혜택','Y','Y','Y','PUBLISHED',SYSTIMESTAMP,SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARDS WHERE card_id=10101003);
INSERT INTO CARDS (card_id,card_code,card_type,company_code,company_name,card_name,brand_name,annual_fee_domestic,annual_fee_overseas,previous_month_spend,minimum_age,target_user,summary_description,card_summary,searchable_yn,visible_yn,approval_required_yn,card_status,publish_start_at,created_at,deleted_yn)
SELECT 10101004,'HIPASS_BIZ','CREDIT','01','BNK부산은행','후불 하이패스카드(기업)','LOCAL',10000,0,0,19,'법인(개인사업자 포함)','하이패스 전용 기업카드','하이패스 전용 기업카드','Y','Y','Y','PUBLISHED',SYSTIMESTAMP,SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARDS WHERE card_id=10101004);
INSERT INTO CARDS (card_id,card_code,card_type,company_code,company_name,card_name,brand_name,annual_fee_domestic,annual_fee_overseas,previous_month_spend,minimum_age,target_user,summary_description,card_summary,searchable_yn,visible_yn,approval_required_yn,card_status,publish_start_at,created_at,deleted_yn)
SELECT 10101005,'SOHO_BIZ','CREDIT','01','BNK부산은행','SOHO-BIZ카드','LOCAL',20000,20000,300000,19,'개인사업자/법인','소호사업자를 위한 비즈니스 특화카드','소호사업자를 위한 비즈니스 특화카드','Y','Y','Y','PUBLISHED',SYSTIMESTAMP,SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARDS WHERE card_id=10101005);
INSERT INTO CARDS (card_id,card_code,card_type,company_code,company_name,card_name,brand_name,annual_fee_domestic,annual_fee_overseas,previous_month_spend,minimum_age,target_user,summary_description,card_summary,searchable_yn,visible_yn,approval_required_yn,card_status,publish_start_at,created_at,deleted_yn)
SELECT 10101006,'PEOPLE_HAPPY','CREDIT','01','BNK부산은행','국민행복카드(신용)','LOCAL',0,0,0,19,'국민행복카드 신청 자격자','바우처사업 통합 사용 가능 국민행복카드','바우처사업 통합 사용 가능 국민행복카드','Y','Y','Y','PUBLISHED',SYSTIMESTAMP,SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARDS WHERE card_id=10101006);
INSERT INTO CARDS (card_id,card_code,card_type,company_code,company_name,card_name,brand_name,annual_fee_domestic,annual_fee_overseas,previous_month_spend,minimum_age,target_user,summary_description,card_summary,searchable_yn,visible_yn,approval_required_yn,card_status,publish_start_at,created_at,deleted_yn)
SELECT 10101007,'HIPASS_PERS','CREDIT','01','BNK부산은행','후불 하이패스카드(개인)','LOCAL',5000,0,0,19,'개인','하이패스 전용 개인 신용카드','하이패스 전용 개인 신용카드','Y','Y','Y','PUBLISHED',SYSTIMESTAMP,SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARDS WHERE card_id=10101007);
INSERT INTO CARDS (card_id,card_code,card_type,company_code,company_name,card_name,brand_name,annual_fee_domestic,annual_fee_overseas,previous_month_spend,minimum_age,target_user,summary_description,card_summary,searchable_yn,visible_yn,approval_required_yn,card_status,publish_start_at,created_at,deleted_yn)
SELECT 10101008,'ONEULE_CREDIT','CREDIT','01','BNK부산은행','오늘은e 신용카드','LOCAL',0,0,300000,19,'개인','각종 페이 및 생활 서비스 할인되는 오늘은e 신용카드','각종 페이 및 생활 서비스 할인','Y','Y','Y','PUBLISHED',SYSTIMESTAMP,SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARDS WHERE card_id=10101008);
INSERT INTO CARDS (card_id,card_code,card_type,company_code,company_name,card_name,brand_name,annual_fee_domestic,annual_fee_overseas,previous_month_spend,minimum_age,target_user,summary_description,card_summary,searchable_yn,visible_yn,approval_required_yn,card_status,publish_start_at,created_at,deleted_yn)
SELECT 10101009,'ZIPL_CREDIT','CREDIT','01','BNK부산은행','ZipL 신용카드','LOCAL',0,0,300000,19,'개인','주거 특화 신용카드 ZipL','주거 특화 신용카드 ZipL','Y','Y','Y','PUBLISHED',SYSTIMESTAMP,SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARDS WHERE card_id=10101009);
INSERT INTO CARDS (card_id,card_code,card_type,company_code,company_name,card_name,brand_name,annual_fee_domestic,annual_fee_overseas,previous_month_spend,minimum_age,target_user,summary_description,card_summary,searchable_yn,visible_yn,approval_required_yn,card_status,publish_start_at,created_at,deleted_yn)
SELECT 10101010,'DINGDING_CREDIT','CREDIT','01','BNK부산은행','딩딩 신용카드','LOCAL',0,0,300000,19,'개인','즐거움 가득, 혜택 가득~ DingDing 신용카드','즐거움 가득, 혜택 가득~ DingDing 신용카드','Y','Y','Y','PUBLISHED',SYSTIMESTAMP,SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARDS WHERE card_id=10101010);
INSERT INTO CARDS (card_id,card_code,card_type,company_code,company_name,card_name,brand_name,annual_fee_domestic,annual_fee_overseas,previous_month_spend,minimum_age,target_user,summary_description,card_summary,searchable_yn,visible_yn,approval_required_yn,card_status,publish_start_at,created_at,deleted_yn)
SELECT 10101011,'BNK_FRIENDS','CREDIT','01','BNK부산은행','BNK 프렌즈 신용카드','LOCAL',0,0,300000,19,'개인','BNK 프렌즈 신용카드','BNK 프렌즈 신용카드','Y','Y','Y','PUBLISHED',SYSTIMESTAMP,SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARDS WHERE card_id=10101011);
INSERT INTO CARDS (card_id,card_code,card_type,company_code,company_name,card_name,brand_name,annual_fee_domestic,annual_fee_overseas,previous_month_spend,minimum_age,target_user,summary_description,card_summary,searchable_yn,visible_yn,approval_required_yn,card_status,publish_start_at,created_at,deleted_yn)
SELECT 10101012,'SMART_LIFE','CREDIT','01','BNK부산은행','스마트라이프 신용카드','LOCAL',15000,15000,300000,19,'개인','생활 전반 10% 청구할인 스마트라이프 카드','생활 전반 10% 청구할인','Y','Y','Y','PUBLISHED',SYSTIMESTAMP,SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARDS WHERE card_id=10101012);
INSERT INTO CARDS (card_id,card_code,card_type,company_code,company_name,card_name,brand_name,annual_fee_domestic,annual_fee_overseas,previous_month_spend,minimum_age,target_user,summary_description,card_summary,searchable_yn,visible_yn,approval_required_yn,card_status,publish_start_at,created_at,deleted_yn)
SELECT 10101013,'POD_CARD','CREDIT','01','BNK부산은행','팟(pod)카드','LOCAL',0,0,300000,19,'개인','여행 및 숙박 특화 팟(pod)카드','여행 및 숙박 특화 팟(pod)카드','Y','Y','Y','PUBLISHED',SYSTIMESTAMP,SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARDS WHERE card_id=10101013);
INSERT INTO CARDS (card_id,card_code,card_type,company_code,company_name,card_name,brand_name,annual_fee_domestic,annual_fee_overseas,previous_month_spend,minimum_age,target_user,summary_description,card_summary,searchable_yn,visible_yn,approval_required_yn,card_status,publish_start_at,created_at,deleted_yn)
SELECT 10101014,'BNK_TRAVEL','CREDIT','01','BNK부산은행','BNK TRAVEL 신용카드','LOCAL',30000,30000,500000,19,'개인','해외여행 특화 트래블 신용카드','해외여행 특화 트래블 신용카드','Y','Y','Y','PUBLISHED',SYSTIMESTAMP,SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARDS WHERE card_id=10101014);
INSERT INTO CARDS (card_id,card_code,card_type,company_code,company_name,card_name,brand_name,annual_fee_domestic,annual_fee_overseas,previous_month_spend,minimum_age,target_user,summary_description,card_summary,searchable_yn,visible_yn,approval_required_yn,card_status,publish_start_at,created_at,deleted_yn)
SELECT 10101015,'DINGDING_COMBO','CREDIT','01','BNK부산은행','딩딩신용+체크','LOCAL',0,0,300000,19,'개인','딩딩신용+체크 통합 혜택카드','딩딩신용+체크 통합 혜택카드','Y','Y','Y','PUBLISHED',SYSTIMESTAMP,SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARDS WHERE card_id=10101015);
INSERT INTO CARDS (card_id,card_code,card_type,company_code,company_name,card_name,brand_name,annual_fee_domestic,annual_fee_overseas,previous_month_spend,minimum_age,target_user,summary_description,card_summary,searchable_yn,visible_yn,approval_required_yn,card_status,publish_start_at,created_at,deleted_yn)
SELECT 10101016,'FARMCO','CREDIT','01','BNK부산은행','팜코카드','LOCAL',0,0,0,19,'법인','법인 복지포인트 팜코카드','법인 복지포인트 팜코카드','Y','Y','Y','PUBLISHED',SYSTIMESTAMP,SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARDS WHERE card_id=10101016);
INSERT INTO CARDS (card_id,card_code,card_type,company_code,company_name,card_name,brand_name,annual_fee_domestic,annual_fee_overseas,previous_month_spend,minimum_age,target_user,summary_description,card_summary,searchable_yn,visible_yn,approval_required_yn,card_status,publish_start_at,created_at,deleted_yn)
SELECT 10101017,'AMEX_CARD','CREDIT','01','BNK부산은행','아맥스익스프레스(AMEX)','AMEX',100000,100000,500000,19,'개인 프리미엄','American Express 제휴 프리미엄 카드','American Express 제휴 프리미엄 카드','Y','Y','Y','PUBLISHED',SYSTIMESTAMP,SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARDS WHERE card_id=10101017);
INSERT INTO CARDS (card_id,card_code,card_type,company_code,company_name,card_name,brand_name,annual_fee_domestic,annual_fee_overseas,previous_month_spend,minimum_age,target_user,summary_description,card_summary,searchable_yn,visible_yn,approval_required_yn,card_status,publish_start_at,created_at,deleted_yn)
SELECT 10101018,'BNK_SIMPLE','CREDIT','01','BNK부산은행','BNK Simple카드','LOCAL',0,0,200000,19,'개인','심플한 혜택의 BNK Simple카드','심플한 혜택의 BNK Simple카드','Y','Y','Y','PUBLISHED',SYSTIMESTAMP,SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARDS WHERE card_id=10101018);

-- [체크카드 8건]
INSERT INTO CARDS (card_id,card_code,card_type,company_code,company_name,card_name,brand_name,annual_fee_domestic,annual_fee_overseas,previous_month_spend,minimum_age,target_user,summary_description,card_summary,searchable_yn,visible_yn,approval_required_yn,card_status,publish_start_at,created_at,deleted_yn)
SELECT 10201001,'BBANG_CHECK','CHECK','01','BNK부산은행','빵빵체크카드','LOCAL',0,0,0,18,'만 18세 이상 개인 본인 회원','혜택이 빵빵한 !! 빵빵체크카드 !!','혜택이 빵빵한 !! 빵빵체크카드 !!','Y','Y','Y','PUBLISHED',SYSTIMESTAMP,SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARDS WHERE card_id=10201001);
INSERT INTO CARDS (card_id,card_code,card_type,company_code,company_name,card_name,brand_name,annual_fee_domestic,annual_fee_overseas,previous_month_spend,minimum_age,target_user,summary_description,card_summary,searchable_yn,visible_yn,approval_required_yn,card_status,publish_start_at,created_at,deleted_yn)
SELECT 10201002,'GOV_HAPPY_CHECK','CHECK','01','BNK부산은행','국민행복체크카드','LOCAL',0,0,0,18,'개인','정부 바우처사업 통합 체크카드','정부 바우처사업 통합 체크카드','Y','Y','Y','PUBLISHED',SYSTIMESTAMP,SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARDS WHERE card_id=10201002);
INSERT INTO CARDS (card_id,card_code,card_type,company_code,company_name,card_name,brand_name,annual_fee_domestic,annual_fee_overseas,previous_month_spend,minimum_age,target_user,summary_description,card_summary,searchable_yn,visible_yn,approval_required_yn,card_status,publish_start_at,created_at,deleted_yn)
SELECT 10201003,'DONGBAEK_CHECK','CHECK','01','BNK부산은행','부산 동백전 체크카드','LOCAL',0,0,0,14,'후불교통: 만18세 이상 / 비교통: 만14세 이상','부산지역화폐 동백전 체크카드','부산지역화폐 동백전 체크카드','Y','Y','Y','PUBLISHED',SYSTIMESTAMP,SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARDS WHERE card_id=10201003);
INSERT INTO CARDS (card_id,card_code,card_type,company_code,company_name,card_name,brand_name,annual_fee_domestic,annual_fee_overseas,previous_month_spend,minimum_age,target_user,summary_description,card_summary,searchable_yn,visible_yn,approval_required_yn,card_status,publish_start_at,created_at,deleted_yn)
SELECT 10201004,'DINGDING_CHECK','CHECK','01','BNK부산은행','딩딩 체크카드','LOCAL',0,0,0,18,'개인','즐거움 가득, 혜택 가득~ DingDing 체크카드','즐거움 가득, 혜택 가득~ DingDing 체크카드','Y','Y','Y','PUBLISHED',SYSTIMESTAMP,SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARDS WHERE card_id=10201004);
INSERT INTO CARDS (card_id,card_code,card_type,company_code,company_name,card_name,brand_name,annual_fee_domestic,annual_fee_overseas,previous_month_spend,minimum_age,target_user,summary_description,card_summary,searchable_yn,visible_yn,approval_required_yn,card_status,publish_start_at,created_at,deleted_yn)
SELECT 10201005,'GREEN_CHECK','CHECK','01','BNK부산은행','어디로든 그린체크카드','LOCAL',0,0,0,18,'개인','친환경 업종 특화 체크카드','친환경 업종 특화 체크카드','Y','Y','Y','PUBLISHED',SYSTIMESTAMP,SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARDS WHERE card_id=10201005);
INSERT INTO CARDS (card_id,card_code,card_type,company_code,company_name,card_name,brand_name,annual_fee_domestic,annual_fee_overseas,previous_month_spend,minimum_age,target_user,summary_description,card_summary,searchable_yn,visible_yn,approval_required_yn,card_status,publish_start_at,created_at,deleted_yn)
SELECT 10201006,'UNTACT_CHECK','CHECK','01','BNK부산은행','2030 언택트 체크카드','LOCAL',0,0,0,18,'개인 / 외국인(웰컴글로벌)','비대면 특화 2030 언택트 체크카드','비대면 특화 2030 언택트 체크카드','Y','Y','Y','PUBLISHED',SYSTIMESTAMP,SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARDS WHERE card_id=10201006);
INSERT INTO CARDS (card_id,card_code,card_type,company_code,company_name,card_name,brand_name,annual_fee_domestic,annual_fee_overseas,previous_month_spend,minimum_age,target_user,summary_description,card_summary,searchable_yn,visible_yn,approval_required_yn,card_status,publish_start_at,created_at,deleted_yn)
SELECT 10201007,'ONEULE_CHECK','CHECK','01','BNK부산은행','오늘은e 체크카드','LOCAL',0,0,0,18,'개인','각종 페이 및 생활 서비스 할인 체크카드','각종 페이 및 생활 서비스 할인 체크카드','Y','Y','Y','PUBLISHED',SYSTIMESTAMP,SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARDS WHERE card_id=10201007);
INSERT INTO CARDS (card_id,card_code,card_type,company_code,company_name,card_name,brand_name,annual_fee_domestic,annual_fee_overseas,previous_month_spend,minimum_age,target_user,summary_description,card_summary,searchable_yn,visible_yn,approval_required_yn,card_status,publish_start_at,created_at,deleted_yn)
SELECT 10201008,'ZIPL_CHECK','CHECK','01','BNK부산은행','ZipL 체크카드','LOCAL',0,0,0,18,'개인','주거 특화 체크카드 ZipL','주거 특화 체크카드 ZipL','Y','Y','Y','PUBLISHED',SYSTIMESTAMP,SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARDS WHERE card_id=10201008);

-- [선불카드 1건]
INSERT INTO CARDS (card_id,card_code,card_type,company_code,company_name,card_name,brand_name,annual_fee_domestic,annual_fee_overseas,previous_month_spend,minimum_age,target_user,summary_description,card_summary,searchable_yn,visible_yn,approval_required_yn,card_status,publish_start_at,created_at,deleted_yn)
SELECT 10301001,'DONGBAEK_PREPAID','PREPAID','01','BNK부산은행','부산 동백전 선불카드','LOCAL',0,0,0,14,'만 14세 이상 부산 시민','부산시 공식 지역화폐 동백전 선불카드','부산시 공식 지역화폐 동백전 선불카드','Y','Y','N','PUBLISHED',SYSTIMESTAMP,SYSTIMESTAMP,'N' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CARDS WHERE card_id=10301001);

COMMIT;

-- ==============================================================
-- 3. CARD_BENEFITS  (구조 동일, 그대로 유지)
-- ==============================================================
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101001, 1, '호텔다이닝, 백화점상품권 등 바우처 제공(연 1회, 택1)', 'FREE',
    NULL, NULL, NULL, NULL,
    NULL, '호텔다이닝, 백화점상품권 등 바우처 제공(연 1회, 택1)', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101001, 14, '국내 일시불, 할부 금액에 대해 TOP포인트 적립 1%', 'POINT',
    NULL, NULL, 0.01, NULL,
    NULL, '국내 일시불, 할부 금액에 대해 TOP포인트 적립 1%', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101001, 1, '공항라운지 연3회 무료', 'FREE',
    NULL, NULL, NULL, NULL,
    NULL, '공항라운지 연3회 무료', 3, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101001, 1, '공항, 호텔 발렛파킹 각각 월2회/연5회 무료', 'FREE',
    NULL, NULL, NULL, NULL,
    NULL, '공항, 호텔 발렛파킹 각각 월2회/연5회 무료', 4, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101001, 3, '백화점(롯데, 신세계)/마트(이마트, 홈플러스) 할인 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, '백화점(롯데, 신세계)/마트(이마트, 홈플러스) 할인 10%', 5, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101001, 6, '카카오T 할인 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, '카카오T 할인 10%', 6, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101001, 2, '스타벅스 할인 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, '스타벅스 할인 10%', 7, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101001, 4, '주유소 리터당 할인 70원', 'FIXED_DISCOUNT',
    NULL, 70, NULL, NULL,
    NULL, '주유소 리터당 할인 70원', 8, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101002, 1, '호텔다이닝, 백화점상품권 등 바우처 제공(연 1회, 택1)', 'FREE',
    NULL, NULL, NULL, NULL,
    NULL, '호텔다이닝, 백화점상품권 등 바우처 제공(연 1회, 택1)', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101002, 1, '1,500원당 대한항공 1마일리지 적립(국내가맹점)', 'FIXED_DISCOUNT',
    NULL, 1500, NULL, NULL,
    NULL, '1,500원당 대한항공 1마일리지 적립(국내가맹점)', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101002, 1, '공항라운지 연3회 무료', 'FREE',
    NULL, NULL, NULL, NULL,
    NULL, '공항라운지 연3회 무료', 3, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101002, 1, '공항, 호텔 발렛파킹 각각 월2회/연5회 무료', 'FREE',
    NULL, NULL, NULL, NULL,
    NULL, '공항, 호텔 발렛파킹 각각 월2회/연5회 무료', 4, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101002, 3, '백화점(롯데, 신세계)/마트(이마트, 홈플러스) 할인 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, '백화점(롯데, 신세계)/마트(이마트, 홈플러스) 할인 10%', 5, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101002, 6, '카카오T 할인 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, '카카오T 할인 10%', 6, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101002, 2, '스타벅스 할인 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, '스타벅스 할인 10%', 7, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101002, 4, '주유소 리터당 할인 70원', 'FIXED_DISCOUNT',
    NULL, 70, NULL, NULL,
    NULL, '주유소 리터당 할인 70원', 8, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201001, 14, '국내 캐시백 최대 0.4%', 'CASHBACK',
    NULL, NULL, NULL, 0.004,
    NULL, '국내 캐시백 최대 0.4%', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201001, 14, '해외 캐시백 2%', 'CASHBACK',
    NULL, NULL, NULL, 0.02,
    NULL, '해외 캐시백 2%', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201001, 2, '커피(메가, 컴포즈, 텐퍼센트, 하삼동, 더벤티) 3%', 'RATE_DISCOUNT',
    0.03, NULL, NULL, NULL,
    NULL, '커피(메가, 컴포즈, 텐퍼센트, 하삼동, 더벤티) 3%', 3, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201001, 3, '쇼핑(다이소, 올리브영) 3%', 'RATE_DISCOUNT',
    0.03, NULL, NULL, NULL,
    NULL, '쇼핑(다이소, 올리브영) 3%', 4, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201001, 9, '페이(네이버, 카카오, 삼성페이) 3%', 'RATE_DISCOUNT',
    0.03, NULL, NULL, NULL,
    NULL, '페이(네이버, 카카오, 삼성페이) 3%', 5, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101003, 3, '백화점, 대형마트, 병/의원, 약국 2~3개월 무이자할부', 'FREE',
    NULL, NULL, NULL, NULL,
    NULL, '백화점, 대형마트, 병/의원, 약국 2~3개월 무이자할부', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101003, 4, 'SK, GS주유소 할인(LPG포함) 1.5%', 'RATE_DISCOUNT',
    0.0150, NULL, NULL, NULL,
    NULL, 'SK, GS주유소 할인(LPG포함) 1.5%', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101003, 6, '영화관 현장할인(1만원이상 결제시) 3,000원', 'FIXED_DISCOUNT',
    NULL, 3000, NULL, NULL,
    NULL, '영화관 현장할인(1만원이상 결제시) 3,000원', 3, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101003, 2, '파리바게트, 던킨도너츠, 베스킨라빈스 등 할인 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, '파리바게트, 던킨도너츠, 베스킨라빈스 등 할인 10%', 4, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101003, 2, '스타벅스, 커피빈, 엔제리너스 할인 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, '스타벅스, 커피빈, 엔제리너스 할인 10%', 5, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101003, 8, '휴대폰 요금(SKT, KT, LG) 자동이체 할인 최대 1,500원', 'FIXED_DISCOUNT',
    NULL, 1500, NULL, NULL,
    NULL, '휴대폰 요금(SKT, KT, LG) 자동이체 할인 최대 1,500원', 6, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101003, 6, '놀이공원 본인 무료입장 또는 자유이용권 할인 50%', 'FREE',
    NULL, NULL, NULL, NULL,
    NULL, '놀이공원 본인 무료입장 또는 자유이용권 할인 50%', 7, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201002, 7, '의료업종 청구할인 3%', 'RATE_DISCOUNT',
    0.03, NULL, NULL, NULL,
    NULL, '의료업종 청구할인 3%', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201002, 3, '온라인쇼핑몰 청구할인 3%', 'RATE_DISCOUNT',
    0.03, NULL, NULL, NULL,
    NULL, '온라인쇼핑몰 청구할인 3%', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201002, 2, '주요 커피전문점 청구할인 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, '주요 커피전문점 청구할인 10%', 3, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201002, 2, '주요 패밀리레스토랑 청구할인 5%', 'RATE_DISCOUNT',
    0.05, NULL, NULL, NULL,
    NULL, '주요 패밀리레스토랑 청구할인 5%', 4, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201002, 8, '이동통신요금 자동이체 청구할인 1,000원', 'FIXED_DISCOUNT',
    NULL, 1000, NULL, NULL,
    NULL, '이동통신요금 자동이체 청구할인 1,000원', 5, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201004, 16, 'GS25, CU, 세븐일레븐 할인 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, 'GS25, CU, 세븐일레븐 할인 10%', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201004, 2, '파리바게트, 뚜레쥬르, 던킨도너츠 할인 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, '파리바게트, 뚜레쥬르, 던킨도너츠 할인 10%', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201004, 2, '맥도날드, 롯데리아 할인 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, '맥도날드, 롯데리아 할인 10%', 3, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201004, 3, 'CJ올리브영, 다이소 할인 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, 'CJ올리브영, 다이소 할인 10%', 4, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201004, 3, 'G마켓, 옥션, 11번가 할인 5%', 'RATE_DISCOUNT',
    0.05, NULL, NULL, NULL,
    NULL, 'G마켓, 옥션, 11번가 할인 5%', 5, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201004, 6, '전국 영화관 예매 할인 4,000원', 'FIXED_DISCOUNT',
    NULL, 4000, NULL, NULL,
    NULL, '전국 영화관 예매 할인 4,000원', 6, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201004, 11, '교보, 영풍, YES24, 영광도서 할인 2,000원', 'FIXED_DISCOUNT',
    NULL, 2000, NULL, NULL,
    NULL, '교보, 영풍, YES24, 영광도서 할인 2,000원', 7, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201005, 4, '친환경 자동차 충전 적립 20%', 'POINT',
    NULL, NULL, 0.2000, NULL,
    NULL, '친환경 자동차 충전 적립 20%', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201005, 6, '공유 모빌리티 적립 5%', 'POINT',
    NULL, NULL, 0.05, NULL,
    NULL, '공유 모빌리티 적립 5%', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201005, 2, '커피 전문점 적립 5%', 'POINT',
    NULL, NULL, 0.05, NULL,
    NULL, '커피 전문점 적립 5%', 3, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201005, 6, '국내가맹점 적립 0.1%', 'POINT',
    NULL, NULL, 0.001, NULL,
    NULL, '국내가맹점 적립 0.1%', 4, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201006, 9, '유튜브 프리미엄, 멜론, 넷플릭스 청구할인(건당 8천원 이상 결제 시) 20%', 'RATE_DISCOUNT',
    0.2000, NULL, NULL, NULL,
    8000, '유튜브 프리미엄, 멜론, 넷플릭스 청구할인(건당 8천원 이상 결제 시) 20%', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201006, 2, '스타벅스(사이렌오더 포함) 할인(건당 1만원 이상 결제 시) 3%', 'RATE_DISCOUNT',
    0.03, NULL, NULL, NULL,
    10000, '스타벅스(사이렌오더 포함) 할인(건당 1만원 이상 결제 시) 3%', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201006, 2, '배달의민족, 요기요 할인(건당 2만원 이상 결제 시) 3%', 'RATE_DISCOUNT',
    0.03, NULL, NULL, NULL,
    20000, '배달의민족, 요기요 할인(건당 2만원 이상 결제 시) 3%', 3, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201006, 3, '쿠팡, 마켓컬리, SSG닷컴 할인(건당 2만원 이상 결제 시) 3%', 'RATE_DISCOUNT',
    0.03, NULL, NULL, NULL,
    20000, '쿠팡, 마켓컬리, SSG닷컴 할인(건당 2만원 이상 결제 시) 3%', 4, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201006, 6, '체크카드 발급 수수료 원', 'FREE',
    NULL, NULL, NULL, NULL,
    NULL, '체크카드 발급 수수료 원', 5, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201007, 2, '스타벅스 청구할인 3%', 'RATE_DISCOUNT',
    0.03, NULL, NULL, NULL,
    NULL, '스타벅스 청구할인 3%', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201007, 8, '휴대폰 요금 청구할인 3%', 'RATE_DISCOUNT',
    0.0300, NULL, NULL, NULL,
    NULL, '휴대폰 요금 청구할인 3%', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201007, 5, '버스, 지하철, 택시요금 청구할인 3%', 'RATE_DISCOUNT',
    0.03, NULL, NULL, NULL,
    NULL, '버스, 지하철, 택시요금 청구할인 3%', 3, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201007, 6, '롯데시네마 청구할인 3%', 'RATE_DISCOUNT',
    0.0300, NULL, NULL, NULL,
    NULL, '롯데시네마 청구할인 3%', 4, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201007, 11, '전국 모든 학원 업종 이용액 청구할인(건당 5만원 이상 결제 시) 3%', 'RATE_DISCOUNT',
    0.03, NULL, NULL, NULL,
    50000, '전국 모든 학원 업종 이용액 청구할인(건당 5만원 이상 결제 시) 3%', 5, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201007, 9, '간편결제 시 청구할인 3%', 'RATE_DISCOUNT',
    0.03, NULL, NULL, NULL,
    NULL, '간편결제 시 청구할인 3%', 6, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201008, 8, '아파트관리비, 보험업종, 도시가스, 통신요금 청구할인 영역통합 최대 8,000원', 'FIXED_DISCOUNT',
    NULL, 8000, NULL, NULL,
    NULL, '아파트관리비, 보험업종, 도시가스, 통신요금 청구할인 영역통합 최대 8,000원', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201008, 19, '이미용업종, 세탁업종 청구할인 영역통합 최대 8,000원', 'FIXED_DISCOUNT',
    NULL, 8000, NULL, NULL,
    NULL, '이미용업종, 세탁업종 청구할인 영역통합 최대 8,000원', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201008, 20, '이마트, 롯데마트, 홈플러스 청구할인 영역통합 최대 8,000원', 'FIXED_DISCOUNT',
    NULL, 8000, NULL, NULL,
    NULL, '이마트, 롯데마트, 홈플러스 청구할인 영역통합 최대 8,000원', 3, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10201008, 11, '학원업종 청구할인 영역통합 최대 8,000원', 'FIXED_DISCOUNT',
    NULL, 8000, NULL, NULL,
    NULL, '학원업종 청구할인 영역통합 최대 8,000원', 4, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101004, 5, '출퇴근 시간대 통행료 자동 할인(토/일/공휴일제외) 최대 50%', 'RATE_DISCOUNT',
    0.5000, NULL, NULL, NULL,
    NULL, '출퇴근 시간대 통행료 자동 할인(토/일/공휴일제외) 최대 50%', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101004, 6, '경차할인 50%', 'RATE_DISCOUNT',
    0.5000, NULL, NULL, NULL,
    NULL, '경차할인 50%', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101005, 14, '국내 이용금액에 대해 포인트 적립 0.2%', 'POINT',
    NULL, NULL, 0.002, NULL,
    NULL, '국내 이용금액에 대해 포인트 적립 0.2%', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101005, 13, '보증보험료 할인(신용보증기금, 기술보증기금, 신용보증재단) 5%', 'RATE_DISCOUNT',
    0.05, NULL, NULL, NULL,
    NULL, '보증보험료 할인(신용보증기금, 기술보증기금, 신용보증재단) 5%', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101005, 4, '주유(LPG포함) 할인 3%', 'RATE_DISCOUNT',
    0.0300, NULL, NULL, NULL,
    NULL, '주유(LPG포함) 할인 3%', 3, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101005, 22, '사업지원서비스(부가세신고/전자세금계산서/세무상담)', 'FREE',
    NULL, NULL, NULL, NULL,
    NULL, '사업지원서비스(부가세신고/전자세금계산서/세무상담)', 4, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101006, 7, '의료업종 청구할인 5%', 'RATE_DISCOUNT',
    0.05, NULL, NULL, NULL,
    NULL, '의료업종 청구할인 5%', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101006, 3, '온라인쇼핑몰 청구할인 5%', 'RATE_DISCOUNT',
    0.05, NULL, NULL, NULL,
    NULL, '온라인쇼핑몰 청구할인 5%', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101006, 2, '주요 커피전문점 청구할인 20%', 'RATE_DISCOUNT',
    0.2000, NULL, NULL, NULL,
    NULL, '주요 커피전문점 청구할인 20%', 3, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101006, 2, '주요 패밀리레스토랑 청구할인 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, '주요 패밀리레스토랑 청구할인 10%', 4, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101006, 8, '이동통신요금 자동이체 청구할인 1,000원', 'FIXED_DISCOUNT',
    NULL, 1000, NULL, NULL,
    NULL, '이동통신요금 자동이체 청구할인 1,000원', 5, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101006, 6, '딸기가 좋아 키즈파크 매장 내 이용 시 청구할인 5%', 'RATE_DISCOUNT',
    0.05, NULL, NULL, NULL,
    NULL, '딸기가 좋아 키즈파크 매장 내 이용 시 청구할인 5%', 6, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101007, 5, '출퇴근 시간대 통행료 자동 할인(토/일/공휴일제외) 최대 50%', 'RATE_DISCOUNT',
    0.5000, NULL, NULL, NULL,
    NULL, '출퇴근 시간대 통행료 자동 할인(토/일/공휴일제외) 최대 50%', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101007, 6, '경차할인 50%', 'RATE_DISCOUNT',
    0.5000, NULL, NULL, NULL,
    NULL, '경차할인 50%', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101008, 9, '간편결제 시 청구할인 5%', 'RATE_DISCOUNT',
    0.05, NULL, NULL, NULL,
    NULL, '간편결제 시 청구할인 5%', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101008, 5, '버스, 지하철, 택시요금 청구할인 5%', 'RATE_DISCOUNT',
    0.05, NULL, NULL, NULL,
    NULL, '버스, 지하철, 택시요금 청구할인 5%', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101008, 6, '롯데시네마 청구할인 5%', 'RATE_DISCOUNT',
    0.0500, NULL, NULL, NULL,
    NULL, '롯데시네마 청구할인 5%', 3, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101008, 2, '스타벅스 청구할인 5%', 'RATE_DISCOUNT',
    0.05, NULL, NULL, NULL,
    NULL, '스타벅스 청구할인 5%', 4, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101008, 8, '휴대폰 요금 청구할인 5%', 'RATE_DISCOUNT',
    0.0500, NULL, NULL, NULL,
    NULL, '휴대폰 요금 청구할인 5%', 5, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101008, 11, '전국 모든 학원 업종 이용액 청구할인 5%', 'RATE_DISCOUNT',
    0.05, NULL, NULL, NULL,
    NULL, '전국 모든 학원 업종 이용액 청구할인 5%', 6, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101009, 3, '6대 홈쇼핑 할인 6%', 'RATE_DISCOUNT',
    0.06, NULL, NULL, NULL,
    NULL, '6대 홈쇼핑 할인 6%', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101009, 3, '백화점, 온라인쇼핑몰, 소셜커머스 할인 5%', 'RATE_DISCOUNT',
    0.05, NULL, NULL, NULL,
    NULL, '백화점, 온라인쇼핑몰, 소셜커머스 할인 5%', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101010, 15, '체육회 지정 시설 할인(최대 1만5천원) 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, '체육회 지정 시설 할인(최대 1만5천원) 10%', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101010, 6, '골프/스포츠/레저 할인(최대 1만원) 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, '골프/스포츠/레저 할인(최대 1만원) 10%', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101010, 7, '병의원/약국 할인(최대 1만원) 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, '병의원/약국 할인(최대 1만원) 10%', 3, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101010, 11, '학원 할인(최대 5천원) 5%', 'RATE_DISCOUNT',
    0.05, NULL, NULL, NULL,
    NULL, '학원 할인(최대 5천원) 5%', 4, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101010, 6, '홍삼/인삼/건강식품 할인(최대 5천원) 5%', 'RATE_DISCOUNT',
    0.05, NULL, NULL, NULL,
    NULL, '홍삼/인삼/건강식품 할인(최대 5천원) 5%', 5, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101010, 2, '간편결제/CU/스타벅스 할인(최대 5천원) 5%', 'RATE_DISCOUNT',
    0.05, NULL, NULL, NULL,
    NULL, '간편결제/CU/스타벅스 할인(최대 5천원) 5%', 6, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101011, 2, 'Pay : 간편결제, 편의점, 커피, 사진 할인 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, 'Pay : 간편결제, 편의점, 커피, 사진 할인 10%', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101011, 6, 'Pay : 놀이공원 현장할인 50%', 'RATE_DISCOUNT',
    0.5000, NULL, NULL, NULL,
    NULL, 'Pay : 놀이공원 현장할인 50%', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101011, 9, 'Online : 디지털 구독 할인 30%', 'RATE_DISCOUNT',
    0.3000, NULL, NULL, NULL,
    NULL, 'Online : 디지털 구독 할인 30%', 3, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101011, 3, 'Online : 웹툰 할인 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, 'Online : 웹툰 할인 10%', 4, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101011, 3, 'Delivery : 온라인쇼핑 할인 15%', 'RATE_DISCOUNT',
    0.1500, NULL, NULL, NULL,
    NULL, 'Delivery : 온라인쇼핑 할인 15%', 5, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101011, 2, 'Delivery : 배달 앱 할인 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, 'Delivery : 배달 앱 할인 10%', 6, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101012, 12, '아파트 관리비 청구할인(자동납부 시에만 월 1회 가능) 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, '아파트 관리비 청구할인(자동납부 시에만 월 1회 가능) 10%', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101012, 12, '도시가스/전기요금 청구할인(자동납부 시에만 월 1회 가능) 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, '도시가스/전기요금 청구할인(자동납부 시에만 월 1회 가능) 10%', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101012, 8, '보험업종(손보/생보), 통신3사(SKT/KT/LG U+) 통신요금 청구할인(월 2회) 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, '보험업종(손보/생보), 통신3사(SKT/KT/LG U+) 통신요금 청구할인(월 2회) 10%', 3, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101012, 19, '이미용, 세탁업종 청구할인(현장결제 시에만 월 4회 가능) 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, '이미용, 세탁업종 청구할인(현장결제 시에만 월 4회 가능) 10%', 4, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101012, 20, '이마트, 롯데마트, 홈플러스, 트레이더스홀세일클럽 청구할인(현장결제 시에만 월 2회 가능) 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, '이마트, 롯데마트, 홈플러스, 트레이더스홀세일클럽 청구할인(현장결제 시에만 월 2회 가능) 10%', 5, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101012, 11, '학원업종, 학습지 청구할인(학원업종은 현장결제 시에만 가능) 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, '학원업종, 학습지 청구할인(학원업종은 현장결제 시에만 가능) 10%', 6, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101012, 7, '병의원/약국/동물병원 청구할인(월 2회) 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, '병의원/약국/동물병원 청구할인(월 2회) 10%', 7, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101012, 5, '후불교통 청구할인(최대 5천원 할인) 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, '후불교통 청구할인(최대 5천원 할인) 10%', 8, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101013, 4, 'SK 주유소/충전소 리터당 할인 최대 120원', 'FIXED_DISCOUNT',
    NULL, 120, NULL, NULL,
    NULL, 'SK 주유소/충전소 리터당 할인 최대 120원', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101013, 2, '스타벅스(사이렌오더 포함) 할인(월 2회) 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, '스타벅스(사이렌오더 포함) 할인(월 2회) 10%', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101013, 6, '스크린골프, CGV/롯데시네마/메가박스 할인(월 2회) 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, '스크린골프, CGV/롯데시네마/메가박스 할인(월 2회) 10%', 3, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101013, 16, 'GS, CU 편의점 현장결제 할인(월 2회) 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, 'GS, CU 편의점 현장결제 할인(월 2회) 10%', 4, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101013, 9, '넷플릭스, 멜론 할인(월 2회) 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, '넷플릭스, 멜론 할인(월 2회) 10%', 5, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101013, 18, '주차장 할인(월 4회) 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, '주차장 할인(월 4회) 10%', 6, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101014, 14, '국내가맹점 연간 사용금액에 따라 캐시백 최대 200,000원', 'CASHBACK',
    NULL, NULL, NULL, NULL,
    NULL, '국내가맹점 연간 사용금액에 따라 캐시백 최대 200,000원', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101014, 8, '휴대폰 요금(SKT, KT, LG) 자동이체 할인 5%', 'RATE_DISCOUNT',
    0.0500, NULL, NULL, NULL,
    NULL, '휴대폰 요금(SKT, KT, LG) 자동이체 할인 5%', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101014, 5, '대중교통(버스, 지하철 등 후불교통) 할인 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, '대중교통(버스, 지하철 등 후불교통) 할인 10%', 3, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101014, 6, '국내가맹점 1만원 이상 결제 시 기본할인(주말 할인율 2배) 0.3%', 'RATE_DISCOUNT',
    0.003, NULL, NULL, NULL,
    10000, '국내가맹점 1만원 이상 결제 시 기본할인(주말 할인율 2배) 0.3%', 4, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101015, 4, '주유소(LPG포함) 리터당 청구할인 60원', 'FIXED_DISCOUNT',
    NULL, 60, NULL, NULL,
    NULL, '주유소(LPG포함) 리터당 청구할인 60원', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101015, 2, '음식점업종 청구할인 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, '음식점업종 청구할인 10%', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101015, 2, '커피, 베이커리 청구할인 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, '커피, 베이커리 청구할인 10%', 3, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101015, 6, '영화관 청구할인 4,000원', 'FIXED_DISCOUNT',
    NULL, 4000, NULL, NULL,
    NULL, '영화관 청구할인 4,000원', 4, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101015, 6, '인터넷쇼핑 청구할인 5%', 'RATE_DISCOUNT',
    0.05, NULL, NULL, NULL,
    NULL, '인터넷쇼핑 청구할인 5%', 5, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101015, 8, '이동통신요금 자동이체 청구할인 5%', 'RATE_DISCOUNT',
    0.0500, NULL, NULL, NULL,
    NULL, '이동통신요금 자동이체 청구할인 5%', 6, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101015, 5, '대중교통 이용요금 청구할인 10%', 'RATE_DISCOUNT',
    0.1000, NULL, NULL, NULL,
    NULL, '대중교통 이용요금 청구할인 10%', 7, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101015, 6, '놀이공원 본인 무료입장 또는 자유이용권 현장할인 50%', 'RATE_DISCOUNT',
    0.5000, NULL, NULL, NULL,
    NULL, '놀이공원 본인 무료입장 또는 자유이용권 현장할인 50%', 8, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101016, 6, '무이자 2~3개월 할부 제공(개인사업자 및 소기업만 적용)', 'FREE',
    NULL, NULL, NULL, NULL,
    NULL, '무이자 2~3개월 할부 제공(개인사업자 및 소기업만 적용)', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101016, 14, '이용금액에 대해 포인트 또는 캐쉬백 선택 가능(개인사업자 및 소기업) 1%', 'CASHBACK',
    NULL, NULL, NULL, 0.01,
    NULL, '이용금액에 대해 포인트 또는 캐쉬백 선택 가능(개인사업자 및 소기업) 1%', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101016, 14, '이용금액에 대해 포인트 또는 캐쉬백 선택 가능(법인회원) 0.5%', 'CASHBACK',
    NULL, NULL, NULL, 0.005,
    NULL, '이용금액에 대해 포인트 또는 캐쉬백 선택 가능(법인회원) 0.5%', 3, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101016, 6, '연회비 영구 면제', 'FREE',
    NULL, NULL, NULL, NULL,
    NULL, '연회비 영구 면제', 4, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10301001, 14, '부산시 정책에 따른 캐시백 지급', 'CASHBACK',
    NULL, NULL, NULL, NULL,
    NULL, '부산시 정책에 따른 캐시백 지급', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101017, 14, '기본 포인트 적립 최대 0.5%', 'POINT',
    NULL, NULL, 0.005, NULL,
    NULL, '기본 포인트 적립 최대 0.5%', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101017, 4, '음식/주유 등 업종에 대해 추가 포인트 적립 최대(개인사업자 및 소기업만 적용) 0.2%', 'POINT',
    NULL, NULL, 0.002, NULL,
    NULL, '음식/주유 등 업종에 대해 추가 포인트 적립 최대(개인사업자 및 소기업만 적용) 0.2%', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101018, 14, '기본 포인트 적립 최대 0.5%', 'POINT',
    NULL, NULL, 0.005, NULL,
    NULL, '기본 포인트 적립 최대 0.5%', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101018, 4, '음식/주유 등 업종에 대해 추가 포인트 적립 최대(개인사업자 및 소기업만 적용) 0.2%', 'POINT',
    NULL, NULL, 0.002, NULL,
    NULL, '음식/주유 등 업종에 대해 추가 포인트 적립 최대(개인사업자 및 소기업만 적용) 0.2%', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_BENEFITS (card_id, category_id, benefit_title, benefit_type,
    discount_rate, discount_amount, point_rate, cashback_rate,
    minimum_payment_amount, display_text, display_order, visible_yn, created_at)
VALUES (10101018, 22, '사업지원서비스(부가세환급지원/전자세금계산서/온라인 세무상담/상권분석서비스)', 'FREE',
    NULL, NULL, NULL, NULL,
    NULL, '사업지원서비스(부가세환급지원/전자세금계산서/온라인 세무상담/상권분석서비스)', 3, 'Y', SYSTIMESTAMP);

COMMIT;

-- ==============================================================
-- 4. CARD_IMAGES
-- 변환: IMAGE_ID 제거(트리거 자동채번), 컬럼명 소문자 정렬
-- ==============================================================
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10101001, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000558/%EB%B6%80%EC%82%B0REX2_%EA%B0%9C%EC%9D%B8_%ED%8F%AC%EC%9D%B8%ED%8A%B8_metal.png', 1);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10101001, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000558/%EB%B6%80%EC%82%B0REX2_%EA%B0%9C%EC%9D%B8_%ED%8F%AC%EC%9D%B8%ED%8A%B8_pvc%20(1).png', 2);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10101002, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000558/%EB%B6%80%EC%82%B0REX2_%EA%B0%9C%EC%9D%B8_%ED%8F%AC%EC%9D%B8%ED%8A%B8_metal.png', 1);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10101002, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000558/%EB%B6%80%EC%82%B0REX2_%EA%B0%9C%EC%9D%B8_%ED%8F%AC%EC%9D%B8%ED%8A%B8_pvc%20(1).png', 2);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10201001, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000504/bbangbbang_check.png', 1);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10101003, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000209/%5B%EB%B6%80%EC%82%B0%EC%9D%80%ED%96%89%5D%20%EC%BA%90%EC%89%AC%EB%B0%B1%EC%B9%B4%EB%93%9C-01.png', 1);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10201002, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000128/people_happy_check.png', 1);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10201003, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000124/%EB%8F%99%EB%B0%B1%EC%A0%84%EC%B2%B4%ED%81%AC%EC%B9%B4%EB%93%9C.png', 1);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10201003, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000124/%EB%8F%99%EB%B0%B1%EC%A0%84%EC%B2%B4%ED%81%AC%EC%B9%B4%EB%93%9C2.png', 2);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10201003, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000124/%EB%8F%99%EB%B0%B1%EC%A0%84%EC%B2%B4%ED%81%AC%EC%B9%B4%EB%93%9C3.png', 3);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10201004, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000123/%EB%94%A9%EB%94%A9%EC%B2%B4%ED%81%AC%EC%B9%B4%EB%93%9C2.png', 1);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10201004, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000123/%EB%94%A9%EB%94%A9%EC%B2%B4%ED%81%AC%EC%B9%B4%EB%93%9C3.png', 2);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10201005, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000116/%EA%B7%B8%EB%A6%B0%EC%B2%B4%ED%81%AC_%EB%8C%80(%EC%84%B8%EB%A1%9C).png', 1);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10201006, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000115/%EC%98%A3%ED%83%9D%ED%8A%B8%EC%B2%B4%ED%81%AC%EC%B9%B4%EB%93%9C_%EB%8C%80(%EC%84%B8%EB%A1%9C).png', 1);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10201006, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000115/%5B%EB%B6%80%EC%82%B0%EC%9D%80%ED%96%89%5D%202030%20%EC%96%B8%ED%83%9D%ED%8A%B8%20%EC%B2%B4%ED%81%AC%EC%B9%B4%EB%93%9C-01.png', 2);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10201007, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000114/%EC%98%A4%EB%8A%98%EC%9D%80e%EC%B2%B4%ED%81%AC%EC%B9%B4%EB%93%9C_%EC%84%B8%EB%A1%9C.png', 1);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10201008, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000103/zipl_%EC%B2%B4%ED%81%AC%EC%B9%B4%EB%93%9C_%EB%8C%80(%EC%84%B8%EB%A1%9C).png', 1);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10101004, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000099/%ED%9B%84%EB%B6%88%EB%B6%80%EC%82%B0%ED%95%98%EC%9D%B4%ED%8C%A8%EC%8A%A4_l.png', 1);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10101005, %EB%B9%84%EC%9E%90).jpg', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000093/soho-biz(%EB%8C%80, 'FRONT', 1);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10101006, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000062/people_happy.png', 1);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10101007, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000058/hipass_88.png', 1);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10101008, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000034/[%EB%B6%80%EC%82%B0%EC%9D%80%ED%96%89]%20%EC%98%A4%EB%8A%98%EC%9D%98e%EC%B9%B4%EB%93%9C(%EC%8B%A0%EC%9A%A9-%EC%B2%B4%ED%81%AC)-05.png', 1);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10101008, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000034/[%EB%B6%80%EC%82%B0%EC%9D%80%ED%96%89]%20%EC%98%A4%EB%8A%98%EC%9D%98e%EC%B9%B4%EB%93%9C(%EC%8B%A0%EC%9A%A9-%EC%B2%B4%ED%81%AC)-03.png', 2);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10101009, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000031/4.IChomesVISA.png', 1);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10101010, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000025/%EB%B6%80%EC%82%B0%EC%B2%B4%EC%9C%A0%EC%82%AC%EB%9E%91%EC%B9%B4%EB%93%9C_%EB%8C%80_%EC%84%B8%EB%A1%9C.png', 1);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10101011, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000024/[%EB%B6%80%EC%82%B0%EC%9D%80%ED%96%89]%20%ED%8C%9F(pod)%EC%B9%B4%EB%93%9C-03.jpg', 1);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10101011, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000024/[%EB%B6%80%EC%82%B0%EC%9D%80%ED%96%89]%20%ED%8C%9F(pod)%EC%B9%B4%EB%93%9C-01.png', 2);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10101012, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000020/ZipL_%EC%8B%A0%EC%9A%A9%EC%B9%B4%EB%93%9C_%EB%8C%80(%EC%84%B8%EB%A1%9C).jpg', 1);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10101013, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000019/SK_OIL_%EC%84%B8%EB%A1%9C%EC%88%98%EC%A0%95.png', 1);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10101014, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000015/[%EB%B6%80%EC%82%B0%EC%9D%80%ED%96%89]%20BNK%20%ED%94%84%EB%A0%8C%EC%A6%88%20%EC%8B%A0%EC%9A%A9%EC%B9%B4%EB%93%9C-01.png', 1);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10101014, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000015/[%EB%B6%80%EC%82%B0%EC%9D%80%ED%96%89]%20BNK%20%ED%94%84%EB%A0%8C%EC%A6%88%20%EC%8B%A0%EC%9A%A9%EC%B9%B4%EB%93%9C-03.png', 2);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10101014, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000015/[%EB%B6%80%EC%82%B0%EC%9D%80%ED%96%89]%20BNK%20%ED%94%84%EB%A0%8C%EC%A6%88%20%EC%8B%A0%EC%9A%A9%EC%B9%B4%EB%93%9C-05.png', 3);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10101015, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000010/[%EB%B6%80%EC%82%B0%EC%9D%80%ED%96%89]%20%EB%94%A9%EB%94%A9%EC%B9%B4%EB%93%9C(%EC%B2%B4%ED%81%AC-%EC%8B%A0%EC%9A%A9)-01.png', 1);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10101015, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000010/[%EB%B6%80%EC%82%B0%EC%9D%80%ED%96%89]%20%EB%94%A9%EB%94%A9%EC%B9%B4%EB%93%9C(%EC%B2%B4%ED%81%AC-%EC%8B%A0%EC%9A%A9)-05.png', 2);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10101016, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000551/%ED%8C%9C%EC%BD%94%EC%B9%B4%EB%93%9C_%EB%8C%80.png', 1);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10301001, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000539/%EB%8F%99%EB%B0%B1%EC%A0%84%EC%84%A0%EB%B6%88%EC%B9%B4%EB%93%9C1.png', 1);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10301001, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000539/%EB%8F%99%EB%B0%B1%EC%A0%84%EC%84%A0%EB%B6%88%EC%B9%B4%EB%93%9C2.png', 2);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10101017, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000074/Amex_220x140.png', 1);
INSERT INTO CARD_IMAGES (card_id, image_type, image_url, sort_order)
VALUES (10101018, 'FRONT', 'https://www.busanbank.co.kr/SSPCTS/LTIV/CARD/0600000073/simple(3).png', 1);

COMMIT;

-- ==============================================================
-- 5. CARD_CONTENTS  (구조 동일)
-- ==============================================================
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101001, 'INTRO', 'REX2_포인트형(개인) 상품소개', '<p>The Return of Royalty, REXⅡ카드</p>', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101001, 'GUIDE', 'REX2_포인트형(개인) 발급안내', '<p><strong>발급대상:</strong> 개인(가족회원)</p><p><strong>신청방법:</strong> 영업점, 인터넷뱅킹, 모바일뱅킹</p>', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101002, 'INTRO', 'REX2_대한항공마일리지형(개인) 상품소개', '<p>The Return of Royalty, REXⅡ카드</p>', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101002, 'GUIDE', 'REX2_대한항공마일리지형(개인) 발급안내', '<p><strong>발급대상:</strong> 개인(가족회원)</p><p><strong>신청방법:</strong> 영업점, 인터넷뱅킹, 모바일뱅킹</p>', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10201001, 'INTRO', '빵빵체크카드 상품소개', '<p>혜택이 빵빵한 !! 빵빵체크카드 !!</p>', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10201001, 'GUIDE', '빵빵체크카드 발급안내', '<p><strong>발급대상:</strong> 개인(가족회원)</p><p><strong>신청방법:</strong> 영업점, 인터넷뱅킹, 모바일뱅킹</p>', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101003, 'INTRO', '캐쉬백카드 상품소개', '<p>매달 결제일에 최대 0.7% 캐쉬백혜택으로 돌아온다! 생활편의 업종은 기존카드처럼 혜택받고, 기타 업종은 이용금액에 따라 일정률로 할인 받는 캐쉬백카드! 할인제외 대상 가맹점 및 업종을 제외하고 국내 모든 가맹점에서 카드이용시 상품에서 정한 할인율 만큼을 고객님 결제일에 마이너스(-) 방식으로 차감하여 청구, 할인 적용하는 방식의 카드 예) 2천만원 차량 구입시 현행 할인율(0.7%)을 적용하여 결제일에는 14만원을 차감한 19,860,000원만 청구하는 방식</p>', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101003, 'GUIDE', '캐쉬백카드 발급안내', '<p><strong>발급대상:</strong> 개인</p><p><strong>신청방법:</strong> 영업점, 인터넷뱅킹, 모바일뱅킹</p>', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10201002, 'INTRO', '국민행복체크카드 상품소개', '<p>정부의 다양한 바우처사업을 통합하여 사용 가능한 카드</p>', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10201002, 'GUIDE', '국민행복체크카드 발급안내', '<p><strong>발급대상:</strong> 개인</p><p><strong>신청방법:</strong> </p>', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10201003, 'INTRO', '부산 동백전 체크카드 상품소개', '<p>부산지역 경제활성화를 위한, 부산지역화폐 동백전! 지역자금의 역외유출 방지를 위해 부산시 내 가맹점 매출액에 대해 할인 혜택 제공 전통시장 매출액에 대한 추가 혜택 제공을 통해 지역 내 소상공인 매출 증대를 통한 지역경기 활성화에 기여</p>', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10201003, 'GUIDE', '부산 동백전 체크카드 발급안내', '<p><strong>발급대상:</strong> 개인</p><p><strong>신청방법:</strong> 영업점, 인터넷뱅킹, 모바일뱅킹</p>', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10201004, 'INTRO', '딩딩 체크카드 상품소개', '<p>즐거움 가득, 혜택 가득~ DingDing 체크카드~!</p>', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10201004, 'GUIDE', '딩딩 체크카드 발급안내', '<p><strong>발급대상:</strong> 개인</p><p><strong>신청방법:</strong> </p>', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10201005, 'INTRO', '어디로든 그린체크카드 상품소개', '<p>친환경 업종 특화 체크카드! 어디로든 그린체크카드!</p>', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10201005, 'GUIDE', '어디로든 그린체크카드 발급안내', '<p><strong>발급대상:</strong> 개인</p><p><strong>신청방법:</strong> </p>', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10201006, 'INTRO', '2030 언택트 체크카드 상품소개', '<p>일상 속 언택트 서비스로 구성된 비대면 특화 카드! 2030 언택트 체크카드!</p>', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10201006, 'GUIDE', '2030 언택트 체크카드 발급안내', '<p><strong>발급대상:</strong> 개인 카드디자인 블루 : 내국인 웰컴글로벌 : 외국인</p><p><strong>신청방법:</strong> </p>', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10201007, 'INTRO', '오늘은e 체크카드 상품소개', '<p>각종 페이 및 생활 서비스 할인되는 오늘은e 체크카드!PAYCO, 삼성페이, 네이버페이, 카카오페이, 썸패스 결제 시 청구할인</p>', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10201007, 'GUIDE', '오늘은e 체크카드 발급안내', '<p><strong>발급대상:</strong> </p><p><strong>신청방법:</strong> </p>', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10201008, 'INTRO', 'ZipL 체크카드 상품소개', '<p>우리집에 플러스되는 체크카드! 생활요금 할인 체크카드</p>', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10201008, 'GUIDE', 'ZipL 체크카드 발급안내', '<p><strong>발급대상:</strong> 개인</p><p><strong>신청방법:</strong> </p>', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101004, 'INTRO', '후불 하이패스카드(기업) 상품소개', '<p>하이패스 차로 통과시 후불방식으로 이용하고 결제일에 결제하는 후불 하이패스카드! 전국고속도로, 민자도로 등에 설치된 톨게이트의 하이패스 車路 통과시 후불방식으로 이용하고 신용카드 결제일에 결제하는 카드</p>', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101004, 'GUIDE', '후불 하이패스카드(기업) 발급안내', '<p><strong>발급대상:</strong> 개인사업자, 법인</p><p><strong>신청방법:</strong> 영업점, 인터넷뱅킹, 기업모바일뱅킹</p>', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101005, 'INTRO', 'SOHO-BIZ카드 상품소개', '<p>당행 최초로 보증료(신용보증기금, 기술보증기금, 신용보증재단) 할인 서비스 탑재!</p>', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101005, 'GUIDE', 'SOHO-BIZ카드 발급안내', '<p><strong>발급대상:</strong> 기업회원(사용자지정,공용) 개인사업자, 법인</p><p><strong>신청방법:</strong> 영업점</p>', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101006, 'INTRO', '국민행복카드 상품소개', '<p>정부의 다양한 바우처사업을 통합하여 사용 가능한 카드</p>', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101006, 'GUIDE', '국민행복카드 발급안내', '<p><strong>발급대상:</strong> 개인</p><p><strong>신청방법:</strong> 영업점, 인터넷뱅킹, 모바일뱅킹</p>', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101007, 'INTRO', '후불 하이패스카드 상품소개', '<p>하이패스 차로 통과시 후불방식으로 이용하고 결제일에 결제하는 후불 하이패스카드! 전국고속도로, 민자도로 등에 설치된 톨게이트의 하이패스 車路 통과시 후불방식으로 이용하고 신용카드 결제일에 결제하는 카드</p>', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101007, 'GUIDE', '후불 하이패스카드 발급안내', '<p><strong>발급대상:</strong> 개인 및 기업회원</p><p><strong>신청방법:</strong> 개인 : 영업점, 모바일뱅킹 기업 : 영업점, 모바일뱅킹</p>', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101008, 'INTRO', '오늘은e 신용카드 상품소개', '<p>각종 페이 및 생활 서비스 할인되는 오늘은e 신용카드! 간편결제할인 서비스 : PAYCO, 삼성페이, 네이버페이, 카카오페이, 썸패스 5% 청구할인 생활할인 서비스 : 학원업종/이동통신/커피/대중교통/영화관 할인 서비스 ※ 전월실적 및 서비스 세부조건은 상품안내장 및 홈페이지 참고201002% 청구할인 생활할인 서비스 : 학원업종/이동통신/커피/대중교통/영화관 할인 서비스 ※ 전월실적 및 서비스 세부조건은 상품안내장 및 홈페이지 참고</p>', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101008, 'GUIDE', '오늘은e 신용카드 발급안내', '<p><strong>발급대상:</strong> 개인회원(가족카드 발급불가)</p><p><strong>신청방법:</strong> 영업점, 인터넷, 스마트폰</p>', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101009, 'INTRO', 'BNK 부자되세요 홈쇼핑카드 상품소개', '<p>한 장의 카드로 폭 넓게 즐기는 쇼핑 특화 카드 한 장의 카드로 다양한 쇼핑을 할인받는 홈쇼핑카드! 현명하게 선택하고 당당하게 사용하는 우리는 모두 꽤 멋진 부자입니다.</p>', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101009, 'GUIDE', 'BNK 부자되세요 홈쇼핑카드 발급안내', '<p><strong>발급대상:</strong> 민법상 성년인 만 19세 이상 개인 회원(본인 및 가족회원)</p><p><strong>신청방법:</strong> 영업점, 인터넷뱅킹, 모바일뱅킹</p>', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101010, 'INTRO', '부산체육사랑카드 상품소개', '<p>Sports is Busan!! 부산시체육회 지정 체육시설 10%, 월 최대 1만5천원 할인! 스포츠, 의료, 학원 등 생활 곳곳에서 할인 챙기세요! 의료, 학원 등 생활 곳곳에서 할인 챙기세요!</p>', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101010, 'GUIDE', '부산체육사랑카드 발급안내', '<p><strong>발급대상:</strong> 개인회원(가족카드 제외)</p><p><strong>신청방법:</strong> 영업점, 인터넷뱅킹, 모바일뱅킹</p>', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101011, 'INTRO', '팟(pod) 카드 상품소개', '<p>내맘속에 팟! 팟카드로 다양한 콘텐츠를 즐기세요!</p>', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101011, 'GUIDE', '팟(pod) 카드 발급안내', '<p><strong>발급대상:</strong> 개인회원 ※가족카드 발급 불가</p><p><strong>신청방법:</strong> 영업점, 인터넷뱅킹, 모바일뱅킹</p>', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101012, 'INTRO', 'ZipL 신용카드 상품소개', '<p>생활에 특별한 혜택, 더 나은 일상을 위한 신용카드</p>', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101012, 'GUIDE', 'ZipL 신용카드 발급안내', '<p><strong>발급대상:</strong> 개인회원 ※가족카드 발급 불가</p><p><strong>신청방법:</strong> 영업점, 인터넷뱅킹, 모바일뱅킹</p>', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101013, 'INTRO', 'SK OIL&LPG카드 상품소개', '<p>주유특화 할인 혜택과 생활 서비스 할인까지 가능한 SK OIL&LPG카드! SK 주유소/충전소 할인 서비스, 생활 할인 서비스</p>', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101013, 'GUIDE', 'SK OIL&LPG카드 발급안내', '<p><strong>발급대상:</strong> 개인회원(가족카드 발급불가)</p><p><strong>신청방법:</strong> 영업점, 인터넷, 스마트폰</p>', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101014, 'INTRO', 'BNK 프렌즈 신용카드 상품소개', '<p>간단 명료한 기본할인! 통큰 연간 캐시백! 생활필수 할인!</p>', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101014, 'GUIDE', 'BNK 프렌즈 신용카드 발급안내', '<p><strong>발급대상:</strong> 개인</p><p><strong>신청방법:</strong> 영업점, 인터넷뱅킹, 모바일뱅킹</p>', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101015, 'INTRO', '딩딩 신용카드 상품소개', '<p>즐거움 가득, 혜택 가득~ DingDing 신용카드!</p>', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101015, 'GUIDE', '딩딩 신용카드 발급안내', '<p><strong>발급대상:</strong> 개인</p><p><strong>신청방법:</strong> 영업점, 인터넷뱅킹, 모바일뱅킹</p>', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101016, 'INTRO', '팜코카드 상품소개', '<p>의약품 구입대금 결제전용 기업카드</p>', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101016, 'GUIDE', '팜코카드 발급안내', '<p><strong>발급대상:</strong> 약사 및 병원 개인 사업자, 법인사업자</p><p><strong>신청방법:</strong> 영업점</p>', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10301001, 'INTRO', '부산 동백전 선불카드 상품소개', '<p>부산지역 경제활성화를 위한, 부산지역화폐 동백전! 지역자금의 역외유출 방지를 위해 부산시 내 사용가능하며 지역 내 소상공인 매출 증대를 통한 지역경기 활성화에 기여</p>', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10301001, 'GUIDE', '부산 동백전 선불카드 발급안내', '<p><strong>발급대상:</strong> 개인</p><p><strong>신청방법:</strong> </p>', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101017, 'INTRO', 'BNK SIMPLE AMEX BLUE BUSINESS 카드 상품소개', '<p>하나의 카드로 사업을 심플하게! BNK Simple 카드</p>', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101017, 'GUIDE', 'BNK SIMPLE AMEX BLUE BUSINESS 카드 발급안내', '<p><strong>발급대상:</strong> 개인사업자/법인</p><p><strong>신청방법:</strong> 영업점 ※ 개인사업자는 기업모바일뱅킹에서 추가발급 가능</p>', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101018, 'INTRO', 'BNK Simple카드 상품소개', '<p>포인트 적립의 Simple한 상품서비스에 지역사회 공헌하는 ESG 상품 하나의 카드로 사업을 심플하게..! BNK Simple 카드</p>', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CONTENTS (card_id, content_type, title, content_html, display_order, visible_yn, created_at)
VALUES (10101018, 'GUIDE', 'BNK Simple카드 발급안내', '<p><strong>발급대상:</strong> 개인사업자, 법인</p><p><strong>신청방법:</strong> 영업점</p>', 2, 'Y', SYSTIMESTAMP);

COMMIT;

-- ==============================================================
-- 6. CARD_ATTRIBUTE_DEFINITIONS  (구조 동일)
-- ==============================================================
INSERT INTO CARD_ATTRIBUTE_DEFINITIONS (attribute_code, attribute_name, attribute_type, description, use_yn, created_at)
VALUES ('ISSUE_METHOD', '가입발급방법', 'TEXT', '영업점/인터넷/모바일 등 발급 채널', 'Y', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_DEFINITIONS (attribute_code, attribute_name, attribute_type, description, use_yn, created_at)
VALUES ('ISSUE_TARGET', '발급대상', 'TEXT', '발급 가능 대상 설명', 'Y', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_DEFINITIONS (attribute_code, attribute_name, attribute_type, description, use_yn, created_at)
VALUES ('APPROVAL_NO', '공시승인번호', 'TEXT', '금융감독원 공시승인번호', 'Y', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_DEFINITIONS (attribute_code, attribute_name, attribute_type, description, use_yn, created_at)
VALUES ('OVERSEAS_USE', '해외사용가능여부', 'BOOLEAN', '해외 가맹점 사용 가능 여부', 'Y', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_DEFINITIONS (attribute_code, attribute_name, attribute_type, description, use_yn, created_at)
VALUES ('FAMILY_CARD', '가족카드발급', 'BOOLEAN', '가족카드 발급 가능 여부', 'Y', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_DEFINITIONS (attribute_code, attribute_name, attribute_type, description, use_yn, created_at)
VALUES ('CONTACTLESS', '비접촉결제', 'BOOLEAN', 'NFC 비접촉 결제 지원 여부', 'Y', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_DEFINITIONS (attribute_code, attribute_name, attribute_type, description, use_yn, created_at)
VALUES ('INSTALLMENT', '할부지원', 'BOOLEAN', '국내 할부 결제 지원 여부', 'Y', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_DEFINITIONS (attribute_code, attribute_name, attribute_type, description, use_yn, created_at)
VALUES ('AUTO_PAY', '자동납부가능', 'BOOLEAN', '공과금 자동납부 지원 여부', 'Y', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_DEFINITIONS (attribute_code, attribute_name, attribute_type, description, use_yn, created_at)
VALUES ('TRAFFIC_CARD', '후불교통기능', 'BOOLEAN', '대중교통 후불 결제 지원 여부', 'Y', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_DEFINITIONS (attribute_code, attribute_name, attribute_type, description, use_yn, created_at)
VALUES ('POINT_TYPE', '포인트종류', 'TEXT', '적립 포인트 종류 (TOP포인트 등)', 'Y', SYSTIMESTAMP);

COMMIT;

-- ==============================================================
-- 7. CARD_ATTRIBUTE_VALUES  (구조 동일)
-- ==============================================================
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101001, 1, '영업점, 인터넷뱅킹, 모바일뱅킹', '영업점, 인터넷뱅킹, 모바일뱅킹', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101001, 2, '개인(가족회원)', '개인(가족회원)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101001, 3, '2025-0000 (심의일자: 2025.00.00)', '2025-0000 (심의일자: 2025.00.00)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101001, 4, 'N', '해외사용 불가', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101001, 5, 'Y', '가족카드 발급가능', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101001, 6, 'Y', '비접촉결제 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101001, 7, 'Y', '할부 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101001, 8, 'N', '자동납부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101001, 9, 'N', '후불교통 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101001, 10, 'TOP포인트', 'TOP포인트', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101002, 1, '영업점, 인터넷뱅킹, 모바일뱅킹', '영업점, 인터넷뱅킹, 모바일뱅킹', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101002, 2, '개인(가족회원)', '개인(가족회원)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101002, 3, '2025-0000 (심의일자: 2025.00.00)', '2025-0000 (심의일자: 2025.00.00)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101002, 4, 'N', '해외사용 불가', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101002, 5, 'Y', '가족카드 발급가능', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101002, 6, 'Y', '비접촉결제 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101002, 7, 'N', '할부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101002, 8, 'N', '자동납부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101002, 9, 'N', '후불교통 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101002, 10, '마일리지', '마일리지', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201001, 2, '개인(가족회원)', '개인(가족회원)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201001, 3, '2025-0000 (심의일자: 2025.00.00)', '2025-0000 (심의일자: 2025.00.00)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201001, 4, 'Y', '해외사용 가능', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201001, 5, 'Y', '가족카드 발급가능', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201001, 6, 'Y', '비접촉결제 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201001, 7, 'N', '할부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201001, 8, 'N', '자동납부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201001, 9, 'N', '후불교통 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201001, 10, '일반포인트', '일반포인트', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101003, 1, '영업점, 인터넷뱅킹, 모바일뱅킹', '영업점, 인터넷뱅킹, 모바일뱅킹', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101003, 2, '개인', '개인', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101003, 3, '2025-0000 (심의일자: 2025.00.00)', '2025-0000 (심의일자: 2025.00.00)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101003, 4, 'N', '해외사용 불가', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101003, 5, 'Y', '가족카드 발급가능', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101003, 6, 'Y', '비접촉결제 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101003, 7, 'Y', '할부 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101003, 8, 'Y', '자동납부 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101003, 9, 'N', '후불교통 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101003, 10, '일반포인트', '일반포인트', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201002, 2, '개인', '개인', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201002, 3, '2025-0000 (심의일자: 2025.00.00)', '2025-0000 (심의일자: 2025.00.00)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201002, 4, 'N', '해외사용 불가', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201002, 5, 'Y', '가족카드 발급가능', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201002, 6, 'Y', '비접촉결제 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201002, 7, 'N', '할부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201002, 8, 'Y', '자동납부 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201002, 9, 'N', '후불교통 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201002, 10, '일반포인트', '일반포인트', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201003, 2, '후불교통 : 만18세 이상 비교통 : 만 14세 이상 브랜드 : 국내전용', '후불교통 : 만18세 이상 비교통 : 만 14세 이상 브랜드 : 국내전용', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201003, 3, '2025-0000 (심의일자: 2025.00.00)', '2025-0000 (심의일자: 2025.00.00)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201003, 4, 'N', '해외사용 불가', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201003, 5, 'Y', '가족카드 발급가능', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201003, 6, 'Y', '비접촉결제 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201003, 7, 'N', '할부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201003, 8, 'N', '자동납부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201003, 9, 'Y', '후불교통 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201003, 10, '일반포인트', '일반포인트', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201004, 2, '개인', '개인', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201004, 3, '2025-0000 (심의일자: 2025.00.00)', '2025-0000 (심의일자: 2025.00.00)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201004, 4, 'N', '해외사용 불가', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201004, 5, 'Y', '가족카드 발급가능', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201004, 6, 'Y', '비접촉결제 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201004, 7, 'N', '할부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201004, 8, 'N', '자동납부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201004, 9, 'N', '후불교통 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201004, 10, '일반포인트', '일반포인트', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201005, 2, '개인', '개인', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201005, 3, '2025-0000 (심의일자: 2025.00.00)', '2025-0000 (심의일자: 2025.00.00)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201005, 4, 'N', '해외사용 불가', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201005, 5, 'Y', '가족카드 발급가능', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201005, 6, 'Y', '비접촉결제 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201005, 7, 'N', '할부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201005, 8, 'N', '자동납부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201005, 9, 'N', '후불교통 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201005, 10, '일반포인트', '일반포인트', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201006, 2, '개인 카드디자인 블루 : 내국인 웰컴글로벌 : 외국인', '개인 카드디자인 블루 : 내국인 웰컴글로벌 : 외국인', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201006, 3, '2025-0000 (심의일자: 2025.00.00)', '2025-0000 (심의일자: 2025.00.00)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201006, 4, 'N', '해외사용 불가', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201006, 5, 'Y', '가족카드 발급가능', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201006, 6, 'Y', '비접촉결제 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201006, 7, 'N', '할부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201006, 8, 'N', '자동납부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201006, 9, 'N', '후불교통 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201006, 10, '일반포인트', '일반포인트', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201007, 3, '2025-0000 (심의일자: 2025.00.00)', '2025-0000 (심의일자: 2025.00.00)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201007, 4, 'N', '해외사용 불가', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201007, 5, 'Y', '가족카드 발급가능', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201007, 6, 'Y', '비접촉결제 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201007, 7, 'N', '할부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201007, 8, 'N', '자동납부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201007, 9, 'Y', '후불교통 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201007, 10, '일반포인트', '일반포인트', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201008, 2, '개인', '개인', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201008, 3, '2025-0000 (심의일자: 2025.00.00)', '2025-0000 (심의일자: 2025.00.00)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201008, 4, 'N', '해외사용 불가', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201008, 5, 'Y', '가족카드 발급가능', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201008, 6, 'Y', '비접촉결제 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201008, 7, 'N', '할부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201008, 8, 'N', '자동납부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201008, 9, 'N', '후불교통 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10201008, 10, '일반포인트', '일반포인트', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101004, 1, '영업점, 인터넷뱅킹, 기업모바일뱅킹', '영업점, 인터넷뱅킹, 기업모바일뱅킹', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101004, 2, '개인사업자, 법인', '개인사업자, 법인', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101004, 3, '2025-0000 (심의일자: 2025.00.00)', '2025-0000 (심의일자: 2025.00.00)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101004, 4, 'N', '해외사용 불가', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101004, 5, 'Y', '가족카드 발급가능', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101004, 6, 'Y', '비접촉결제 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101004, 7, 'N', '할부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101004, 8, 'N', '자동납부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101004, 9, 'N', '후불교통 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101004, 10, '일반포인트', '일반포인트', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101005, 1, '영업점', '영업점', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101005, 2, '기업회원(사용자지정,공용) 개인사업자, 법인', '기업회원(사용자지정,공용) 개인사업자, 법인', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101005, 3, '2025-0000 (심의일자: 2025.00.00)', '2025-0000 (심의일자: 2025.00.00)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101005, 4, 'N', '해외사용 불가', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101005, 5, 'Y', '가족카드 발급가능', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101005, 6, 'Y', '비접촉결제 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101005, 7, 'N', '할부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101005, 8, 'N', '자동납부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101005, 9, 'N', '후불교통 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101005, 10, '일반포인트', '일반포인트', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101006, 1, '영업점, 인터넷뱅킹, 모바일뱅킹', '영업점, 인터넷뱅킹, 모바일뱅킹', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101006, 2, '개인', '개인', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101006, 3, '2025-0000 (심의일자: 2025.00.00)', '2025-0000 (심의일자: 2025.00.00)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101006, 4, 'N', '해외사용 불가', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101006, 5, 'Y', '가족카드 발급가능', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101006, 6, 'Y', '비접촉결제 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101006, 7, 'N', '할부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101006, 8, 'Y', '자동납부 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101006, 9, 'N', '후불교통 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101006, 10, '일반포인트', '일반포인트', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101007, 1, '개인 : 영업점, 모바일뱅킹 기업 : 영업점, 모바일뱅킹', '개인 : 영업점, 모바일뱅킹 기업 : 영업점, 모바일뱅킹', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101007, 2, '개인 및 기업회원', '개인 및 기업회원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101007, 3, '2025-0000 (심의일자: 2025.00.00)', '2025-0000 (심의일자: 2025.00.00)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101007, 4, 'N', '해외사용 불가', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101007, 5, 'Y', '가족카드 발급가능', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101007, 6, 'Y', '비접촉결제 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101007, 7, 'N', '할부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101007, 8, 'N', '자동납부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101007, 9, 'N', '후불교통 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101007, 10, '일반포인트', '일반포인트', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101008, 1, '영업점, 인터넷, 스마트폰', '영업점, 인터넷, 스마트폰', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101008, 2, '개인회원(가족카드 발급불가)', '개인회원(가족카드 발급불가)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101008, 3, '2025-0000 (심의일자: 2025.00.00)', '2025-0000 (심의일자: 2025.00.00)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101008, 4, 'N', '해외사용 불가', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101008, 5, 'Y', '가족카드 발급가능', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101008, 6, 'Y', '비접촉결제 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101008, 7, 'N', '할부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101008, 8, 'N', '자동납부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101008, 9, 'Y', '후불교통 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101008, 10, '일반포인트', '일반포인트', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101009, 1, '영업점, 인터넷뱅킹, 모바일뱅킹', '영업점, 인터넷뱅킹, 모바일뱅킹', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101009, 2, '민법상 성년인 만 19세 이상 개인 회원(본인 및 가족회원)', '민법상 성년인 만 19세 이상 개인 회원(본인 및 가족회원)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101009, 3, '2025-0000 (심의일자: 2025.00.00)', '2025-0000 (심의일자: 2025.00.00)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101009, 4, 'N', '해외사용 불가', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101009, 5, 'Y', '가족카드 발급가능', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101009, 6, 'Y', '비접촉결제 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101009, 7, 'N', '할부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101009, 8, 'N', '자동납부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101009, 9, 'N', '후불교통 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101009, 10, '일반포인트', '일반포인트', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101010, 1, '영업점, 인터넷뱅킹, 모바일뱅킹', '영업점, 인터넷뱅킹, 모바일뱅킹', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101010, 2, '개인회원(가족카드 제외)', '개인회원(가족카드 제외)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101010, 3, '2025-0000 (심의일자: 2025.00.00)', '2025-0000 (심의일자: 2025.00.00)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101010, 4, 'N', '해외사용 불가', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101010, 5, 'N', '가족카드 발급불가', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101010, 6, 'Y', '비접촉결제 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101010, 7, 'N', '할부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101010, 8, 'N', '자동납부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101010, 9, 'N', '후불교통 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101010, 10, '일반포인트', '일반포인트', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101011, 1, '영업점, 인터넷뱅킹, 모바일뱅킹', '영업점, 인터넷뱅킹, 모바일뱅킹', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101011, 2, '개인회원 ※가족카드 발급 불가', '개인회원 ※가족카드 발급 불가', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101011, 3, '2025-0000 (심의일자: 2025.00.00)', '2025-0000 (심의일자: 2025.00.00)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101011, 4, 'N', '해외사용 불가', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101011, 5, 'N', '가족카드 발급불가', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101011, 6, 'Y', '비접촉결제 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101011, 7, 'N', '할부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101011, 8, 'N', '자동납부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101011, 9, 'N', '후불교통 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101011, 10, '일반포인트', '일반포인트', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101012, 1, '영업점, 인터넷뱅킹, 모바일뱅킹', '영업점, 인터넷뱅킹, 모바일뱅킹', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101012, 2, '개인회원 ※가족카드 발급 불가', '개인회원 ※가족카드 발급 불가', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101012, 3, '2025-0000 (심의일자: 2025.00.00)', '2025-0000 (심의일자: 2025.00.00)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101012, 4, 'N', '해외사용 불가', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101012, 5, 'N', '가족카드 발급불가', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101012, 6, 'Y', '비접촉결제 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101012, 7, 'N', '할부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101012, 8, 'Y', '자동납부 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101012, 9, 'Y', '후불교통 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101012, 10, '일반포인트', '일반포인트', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101013, 1, '영업점, 인터넷, 스마트폰', '영업점, 인터넷, 스마트폰', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101013, 2, '개인회원(가족카드 발급불가)', '개인회원(가족카드 발급불가)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101013, 3, '2025-0000 (심의일자: 2025.00.00)', '2025-0000 (심의일자: 2025.00.00)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101013, 4, 'N', '해외사용 불가', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101013, 5, 'Y', '가족카드 발급가능', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101013, 6, 'Y', '비접촉결제 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101013, 7, 'N', '할부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101013, 8, 'N', '자동납부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101013, 9, 'N', '후불교통 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101013, 10, '일반포인트', '일반포인트', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101014, 1, '영업점, 인터넷뱅킹, 모바일뱅킹', '영업점, 인터넷뱅킹, 모바일뱅킹', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101014, 2, '개인', '개인', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101014, 3, '2025-0000 (심의일자: 2025.00.00)', '2025-0000 (심의일자: 2025.00.00)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101014, 4, 'N', '해외사용 불가', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101014, 5, 'Y', '가족카드 발급가능', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101014, 6, 'Y', '비접촉결제 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101014, 7, 'N', '할부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101014, 8, 'Y', '자동납부 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101014, 9, 'Y', '후불교통 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101014, 10, '일반포인트', '일반포인트', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101015, 1, '영업점, 인터넷뱅킹, 모바일뱅킹', '영업점, 인터넷뱅킹, 모바일뱅킹', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101015, 2, '개인', '개인', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101015, 3, '2025-0000 (심의일자: 2025.00.00)', '2025-0000 (심의일자: 2025.00.00)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101015, 4, 'N', '해외사용 불가', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101015, 5, 'Y', '가족카드 발급가능', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101015, 6, 'Y', '비접촉결제 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101015, 7, 'N', '할부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101015, 8, 'Y', '자동납부 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101015, 9, 'Y', '후불교통 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101015, 10, '일반포인트', '일반포인트', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101016, 1, '영업점', '영업점', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101016, 2, '약사 및 병원 개인 사업자, 법인사업자', '약사 및 병원 개인 사업자, 법인사업자', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101016, 3, '2025-0000 (심의일자: 2025.00.00)', '2025-0000 (심의일자: 2025.00.00)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101016, 4, 'N', '해외사용 불가', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101016, 5, 'Y', '가족카드 발급가능', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101016, 6, 'Y', '비접촉결제 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101016, 7, 'Y', '할부 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101016, 8, 'N', '자동납부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101016, 9, 'N', '후불교통 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101016, 10, '일반포인트', '일반포인트', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10301001, 2, '개인', '개인', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10301001, 3, '2025-0000 (심의일자: 2025.00.00)', '2025-0000 (심의일자: 2025.00.00)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10301001, 4, 'N', '해외사용 불가', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10301001, 5, 'Y', '가족카드 발급가능', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10301001, 6, 'Y', '비접촉결제 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10301001, 7, 'N', '할부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10301001, 8, 'N', '자동납부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10301001, 9, 'Y', '후불교통 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10301001, 10, '일반포인트', '일반포인트', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101017, 1, '영업점 ※ 개인사업자는 기업모바일뱅킹에서 추가발급 가능', '영업점 ※ 개인사업자는 기업모바일뱅킹에서 추가발급 가능', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101017, 2, '개인사업자/법인', '개인사업자/법인', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101017, 3, '2025-0000 (심의일자: 2025.00.00)', '2025-0000 (심의일자: 2025.00.00)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101017, 4, 'N', '해외사용 불가', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101017, 5, 'Y', '가족카드 발급가능', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101017, 6, 'Y', '비접촉결제 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101017, 7, 'N', '할부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101017, 8, 'N', '자동납부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101017, 9, 'N', '후불교통 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101017, 10, '일반포인트', '일반포인트', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101018, 1, '영업점', '영업점', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101018, 2, '개인사업자, 법인', '개인사업자, 법인', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101018, 3, '2025-0000 (심의일자: 2025.00.00)', '2025-0000 (심의일자: 2025.00.00)', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101018, 4, 'N', '해외사용 불가', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101018, 5, 'Y', '가족카드 발급가능', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101018, 6, 'Y', '비접촉결제 지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101018, 7, 'N', '할부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101018, 8, 'N', '자동납부 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101018, 9, 'N', '후불교통 미지원', SYSTIMESTAMP);
INSERT INTO CARD_ATTRIBUTE_VALUES (card_id, attribute_id, attribute_value, display_text, created_at)
VALUES (10101018, 10, '일반포인트', '일반포인트', SYSTIMESTAMP);

COMMIT;

-- ==============================================================
-- 8. CARD_TAGS  (구조 동일)
-- ==============================================================
INSERT INTO CARD_TAGS (tag_name, created_at)
VALUES ('체크카드', SYSTIMESTAMP);
INSERT INTO CARD_TAGS (tag_name, created_at)
VALUES ('신용카드', SYSTIMESTAMP);
INSERT INTO CARD_TAGS (tag_name, created_at)
VALUES ('기업카드', SYSTIMESTAMP);
INSERT INTO CARD_TAGS (tag_name, created_at)
VALUES ('선불카드', SYSTIMESTAMP);
INSERT INTO CARD_TAGS (tag_name, created_at)
VALUES ('주유특화', SYSTIMESTAMP);
INSERT INTO CARD_TAGS (tag_name, created_at)
VALUES ('쇼핑특화', SYSTIMESTAMP);
INSERT INTO CARD_TAGS (tag_name, created_at)
VALUES ('여행특화', SYSTIMESTAMP);
INSERT INTO CARD_TAGS (tag_name, created_at)
VALUES ('생활할인', SYSTIMESTAMP);
INSERT INTO CARD_TAGS (tag_name, created_at)
VALUES ('교통할인', SYSTIMESTAMP);
INSERT INTO CARD_TAGS (tag_name, created_at)
VALUES ('음식/카페', SYSTIMESTAMP);
INSERT INTO CARD_TAGS (tag_name, created_at)
VALUES ('연회비면제', SYSTIMESTAMP);
INSERT INTO CARD_TAGS (tag_name, created_at)
VALUES ('캐시백', SYSTIMESTAMP);
INSERT INTO CARD_TAGS (tag_name, created_at)
VALUES ('포인트적립', SYSTIMESTAMP);
INSERT INTO CARD_TAGS (tag_name, created_at)
VALUES ('마일리지', SYSTIMESTAMP);
INSERT INTO CARD_TAGS (tag_name, created_at)
VALUES ('지역화폐', SYSTIMESTAMP);
INSERT INTO CARD_TAGS (tag_name, created_at)
VALUES ('친환경', SYSTIMESTAMP);
INSERT INTO CARD_TAGS (tag_name, created_at)
VALUES ('언택트', SYSTIMESTAMP);
INSERT INTO CARD_TAGS (tag_name, created_at)
VALUES ('젊은층', SYSTIMESTAMP);
INSERT INTO CARD_TAGS (tag_name, created_at)
VALUES ('의료특화', SYSTIMESTAMP);
INSERT INTO CARD_TAGS (tag_name, created_at)
VALUES ('하이패스', SYSTIMESTAMP);
INSERT INTO CARD_TAGS (tag_name, created_at)
VALUES ('홈쇼핑', SYSTIMESTAMP);
INSERT INTO CARD_TAGS (tag_name, created_at)
VALUES ('스포츠', SYSTIMESTAMP);
INSERT INTO CARD_TAGS (tag_name, created_at)
VALUES ('소호/사업자', SYSTIMESTAMP);
INSERT INTO CARD_TAGS (tag_name, created_at)
VALUES ('2030세대', SYSTIMESTAMP);
INSERT INTO CARD_TAGS (tag_name, created_at)
VALUES ('콘텐츠', SYSTIMESTAMP);

COMMIT;

-- ==============================================================
-- 9. CARD_TAG_MAP  (구조 동일)
-- ==============================================================
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101001, 2, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101001, 12, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101001, 19, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101001, 10, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101001, 5, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101002, 2, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101002, 12, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101002, 19, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101002, 10, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101002, 14, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201001, 1, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201001, 15, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201001, 10, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201001, 24, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201001, 3, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101003, 2, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101003, 12, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101003, 10, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101003, 11, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201002, 1, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201002, 15, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201002, 8, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201002, 10, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201002, 24, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201002, 11, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201003, 1, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201003, 24, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201003, 7, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201004, 1, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201004, 24, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201005, 1, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201005, 10, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201005, 24, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201005, 16, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201006, 1, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201006, 10, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201006, 24, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201006, 17, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201006, 18, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201006, 25, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201007, 1, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201007, 9, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201007, 10, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201007, 24, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201008, 1, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201008, 8, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10201008, 24, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101004, 2, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101004, 13, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101004, 20, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101005, 2, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101005, 13, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101005, 12, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101005, 5, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101005, 23, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101006, 2, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101006, 15, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101006, 8, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101006, 10, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101006, 11, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101007, 2, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101007, 20, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101008, 2, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101008, 9, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101008, 10, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101009, 2, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101009, 15, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101009, 21, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101010, 2, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101010, 10, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101010, 11, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101010, 22, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101011, 2, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101011, 15, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101011, 10, SYSTIMESTAMP);
-- [중복제거] card_id=10101011, tag_id=15 (map_id=76 → map_id=74로 이미 존재)
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101012, 2, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101012, 8, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101012, 9, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101012, 11, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101013, 2, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101013, 12, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101013, 10, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101013, 17, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101013, 22, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101014, 2, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101014, 9, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101014, 3, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101015, 2, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101015, 12, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101015, 15, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101015, 8, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101015, 9, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101015, 10, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101016, 2, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101016, 13, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101016, 24, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101016, 3, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101016, 5, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101016, 23, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10301001, 1, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10301001, 4, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10301001, 24, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10301001, 3, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10301001, 7, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101017, 2, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101017, 13, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101017, 12, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101017, 10, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101017, 5, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101017, 23, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101018, 2, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101018, 13, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101018, 12, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101018, 10, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101018, 5, SYSTIMESTAMP);
INSERT INTO CARD_TAG_MAP (card_id, tag_id, created_at)
VALUES (10101018, 23, SYSTIMESTAMP);

COMMIT;

-- ==============================================================
-- 10. CARD_STATUS_HISTORIES  (구조 동일)
-- ==============================================================
INSERT INTO CARD_STATUS_HISTORIES (card_id, previous_status, changed_status, changed_by, changed_reason, changed_at)
VALUES (10101001, 'DRAFT', 'PUBLISHED', 1, '초기 데이터 마이그레이션', SYSTIMESTAMP);
INSERT INTO CARD_STATUS_HISTORIES (card_id, previous_status, changed_status, changed_by, changed_reason, changed_at)
VALUES (10101002, 'DRAFT', 'PUBLISHED', 1, '초기 데이터 마이그레이션', SYSTIMESTAMP);
INSERT INTO CARD_STATUS_HISTORIES (card_id, previous_status, changed_status, changed_by, changed_reason, changed_at)
VALUES (10201001, 'DRAFT', 'PUBLISHED', 1, '초기 데이터 마이그레이션', SYSTIMESTAMP);
INSERT INTO CARD_STATUS_HISTORIES (card_id, previous_status, changed_status, changed_by, changed_reason, changed_at)
VALUES (10101003, 'DRAFT', 'PUBLISHED', 1, '초기 데이터 마이그레이션', SYSTIMESTAMP);
INSERT INTO CARD_STATUS_HISTORIES (card_id, previous_status, changed_status, changed_by, changed_reason, changed_at)
VALUES (10201002, 'DRAFT', 'PUBLISHED', 1, '초기 데이터 마이그레이션', SYSTIMESTAMP);
INSERT INTO CARD_STATUS_HISTORIES (card_id, previous_status, changed_status, changed_by, changed_reason, changed_at)
VALUES (10201003, 'DRAFT', 'PUBLISHED', 1, '초기 데이터 마이그레이션', SYSTIMESTAMP);
INSERT INTO CARD_STATUS_HISTORIES (card_id, previous_status, changed_status, changed_by, changed_reason, changed_at)
VALUES (10201004, 'DRAFT', 'PUBLISHED', 1, '초기 데이터 마이그레이션', SYSTIMESTAMP);
INSERT INTO CARD_STATUS_HISTORIES (card_id, previous_status, changed_status, changed_by, changed_reason, changed_at)
VALUES (10201005, 'DRAFT', 'PUBLISHED', 1, '초기 데이터 마이그레이션', SYSTIMESTAMP);
INSERT INTO CARD_STATUS_HISTORIES (card_id, previous_status, changed_status, changed_by, changed_reason, changed_at)
VALUES (10201006, 'DRAFT', 'PUBLISHED', 1, '초기 데이터 마이그레이션', SYSTIMESTAMP);
INSERT INTO CARD_STATUS_HISTORIES (card_id, previous_status, changed_status, changed_by, changed_reason, changed_at)
VALUES (10201007, 'DRAFT', 'PUBLISHED', 1, '초기 데이터 마이그레이션', SYSTIMESTAMP);
INSERT INTO CARD_STATUS_HISTORIES (card_id, previous_status, changed_status, changed_by, changed_reason, changed_at)
VALUES (10201008, 'DRAFT', 'PUBLISHED', 1, '초기 데이터 마이그레이션', SYSTIMESTAMP);
INSERT INTO CARD_STATUS_HISTORIES (card_id, previous_status, changed_status, changed_by, changed_reason, changed_at)
VALUES (10101004, 'DRAFT', 'PUBLISHED', 1, '초기 데이터 마이그레이션', SYSTIMESTAMP);
INSERT INTO CARD_STATUS_HISTORIES (card_id, previous_status, changed_status, changed_by, changed_reason, changed_at)
VALUES (10101005, 'DRAFT', 'PUBLISHED', 1, '초기 데이터 마이그레이션', SYSTIMESTAMP);
INSERT INTO CARD_STATUS_HISTORIES (card_id, previous_status, changed_status, changed_by, changed_reason, changed_at)
VALUES (10101006, 'DRAFT', 'PUBLISHED', 1, '초기 데이터 마이그레이션', SYSTIMESTAMP);
INSERT INTO CARD_STATUS_HISTORIES (card_id, previous_status, changed_status, changed_by, changed_reason, changed_at)
VALUES (10101007, 'DRAFT', 'PUBLISHED', 1, '초기 데이터 마이그레이션', SYSTIMESTAMP);
INSERT INTO CARD_STATUS_HISTORIES (card_id, previous_status, changed_status, changed_by, changed_reason, changed_at)
VALUES (10101008, 'DRAFT', 'PUBLISHED', 1, '초기 데이터 마이그레이션', SYSTIMESTAMP);
INSERT INTO CARD_STATUS_HISTORIES (card_id, previous_status, changed_status, changed_by, changed_reason, changed_at)
VALUES (10101009, 'DRAFT', 'PUBLISHED', 1, '초기 데이터 마이그레이션', SYSTIMESTAMP);
INSERT INTO CARD_STATUS_HISTORIES (card_id, previous_status, changed_status, changed_by, changed_reason, changed_at)
VALUES (10101010, 'DRAFT', 'PUBLISHED', 1, '초기 데이터 마이그레이션', SYSTIMESTAMP);
INSERT INTO CARD_STATUS_HISTORIES (card_id, previous_status, changed_status, changed_by, changed_reason, changed_at)
VALUES (10101011, 'DRAFT', 'PUBLISHED', 1, '초기 데이터 마이그레이션', SYSTIMESTAMP);
INSERT INTO CARD_STATUS_HISTORIES (card_id, previous_status, changed_status, changed_by, changed_reason, changed_at)
VALUES (10101012, 'DRAFT', 'PUBLISHED', 1, '초기 데이터 마이그레이션', SYSTIMESTAMP);
INSERT INTO CARD_STATUS_HISTORIES (card_id, previous_status, changed_status, changed_by, changed_reason, changed_at)
VALUES (10101013, 'DRAFT', 'PUBLISHED', 1, '초기 데이터 마이그레이션', SYSTIMESTAMP);
INSERT INTO CARD_STATUS_HISTORIES (card_id, previous_status, changed_status, changed_by, changed_reason, changed_at)
VALUES (10101014, 'DRAFT', 'PUBLISHED', 1, '초기 데이터 마이그레이션', SYSTIMESTAMP);
INSERT INTO CARD_STATUS_HISTORIES (card_id, previous_status, changed_status, changed_by, changed_reason, changed_at)
VALUES (10101015, 'DRAFT', 'PUBLISHED', 1, '초기 데이터 마이그레이션', SYSTIMESTAMP);
INSERT INTO CARD_STATUS_HISTORIES (card_id, previous_status, changed_status, changed_by, changed_reason, changed_at)
VALUES (10101016, 'DRAFT', 'PUBLISHED', 1, '초기 데이터 마이그레이션', SYSTIMESTAMP);
INSERT INTO CARD_STATUS_HISTORIES (card_id, previous_status, changed_status, changed_by, changed_reason, changed_at)
VALUES (10301001, 'DRAFT', 'PUBLISHED', 1, '초기 데이터 마이그레이션', SYSTIMESTAMP);
INSERT INTO CARD_STATUS_HISTORIES (card_id, previous_status, changed_status, changed_by, changed_reason, changed_at)
VALUES (10101017, 'DRAFT', 'PUBLISHED', 1, '초기 데이터 마이그레이션', SYSTIMESTAMP);
INSERT INTO CARD_STATUS_HISTORIES (card_id, previous_status, changed_status, changed_by, changed_reason, changed_at)
VALUES (10101018, 'DRAFT', 'PUBLISHED', 1, '초기 데이터 마이그레이션', SYSTIMESTAMP);

COMMIT;

-- ==============================================================
-- 11. CARD_TERMS  (구조 동일)
-- ※ group_id → TERMS_GROUPS(group_id) FK, 해당 테이블 선 INSERT 필요
-- ==============================================================
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101001, 101, 3, 'Y', 1, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101001, 3, 1, 'Y', 2, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101001, 4, 1, 'Y', 3, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101001, 5, 1, 'Y', 4, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101001, 6, 1, 'Y', 5, SYSTIMESTAMP);

-- ──────────────────────────────────────────────
-- [02] REX10101002_대한항공마일리지형(개인)  card_id=10101002
--  안내장(101) + 개인표준(10201001) + 개인부속(10101003) + 신용평점(10201002) + 포인트(10201003)
-- ──────────────────────────────────────────────
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101002, 101, 3, 'Y', 1, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101002, 3, 1, 'Y', 2, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101002, 4, 1, 'Y', 3, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101002, 5, 1, 'Y', 4, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101002, 6, 1, 'Y', 5, SYSTIMESTAMP);

-- ──────────────────────────────────────────────
-- [03] 빵빵체크카드  card_id=10201001
--  안내장(104) + 비씨개인(10201004) + 포인트(10201003)
-- ──────────────────────────────────────────────
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10201001, 104, 3, 'Y', 1, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10201001, 7, 1, 'Y', 2, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10201001, 6, 1, 'Y', 3, SYSTIMESTAMP);

-- ──────────────────────────────────────────────
-- [04] 캐쉬백카드  card_id=10101003
--  안내장(109) + 개인표준(10201001) + 개인부속(10101003) + 신용평점(10201002) + 포인트(10201003)
-- ──────────────────────────────────────────────
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101003, 109, 3, 'Y', 1, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101003, 3, 1, 'Y', 2, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101003, 4, 1, 'Y', 3, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101003, 5, 1, 'Y', 4, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101003, 6, 1, 'Y', 5, SYSTIMESTAMP);

-- ──────────────────────────────────────────────
-- [05] 국민행복체크카드  card_id=10201002
--  안내장(125) + 국민행복카드약관(10201007) + 비씨개인(10201004) + 포인트(10201003)
-- ──────────────────────────────────────────────
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10201002, 125, 3, 'Y', 1, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10201002, 10, 1, 'Y', 2, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10201002, 7, 1, 'Y', 3, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10201002, 6, 1, 'Y', 4, SYSTIMESTAMP);

-- ──────────────────────────────────────────────
-- [06] 부산 동백전 체크카드  card_id=10201003
--  안내장(108) + 비씨개인(10201004) + 포인트(10201003)
-- ──────────────────────────────────────────────
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10201003, 108, 3, 'Y', 1, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10201003, 7, 1, 'Y', 2, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10201003, 6, 1, 'Y', 3, SYSTIMESTAMP);

-- ──────────────────────────────────────────────
-- [07] 딩딩 체크카드  card_id=10201004
--  안내장(112) + 비씨개인(10201004) + 포인트(10201003)
-- ──────────────────────────────────────────────
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10201004, 112, 3, 'Y', 1, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10201004, 7, 1, 'Y', 2, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10201004, 6, 1, 'Y', 3, SYSTIMESTAMP);

-- ──────────────────────────────────────────────
-- [08] 어디로든 그린체크카드  card_id=10201005
--  안내장(122) + 비씨개인(10201004) + 포인트(10201003)
-- ──────────────────────────────────────────────
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10201005, 122, 3, 'Y', 1, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10201005, 7, 1, 'Y', 2, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10201005, 6, 1, 'Y', 3, SYSTIMESTAMP);

-- ──────────────────────────────────────────────
-- [09] 2030 언택트 체크카드  card_id=10201006
--  안내장(102) + 비씨개인(10201004) + 포인트(10201003)
-- ──────────────────────────────────────────────
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10201006, 102, 3, 'Y', 1, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10201006, 7, 1, 'Y', 2, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10201006, 6, 1, 'Y', 3, SYSTIMESTAMP);

-- ──────────────────────────────────────────────
-- [10201007] 오늘은e 체크카드  card_id=10201007
--  안내장(115) + 비씨개인(10201004) + 포인트(10201003)
-- ──────────────────────────────────────────────
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10201007, 115, 3, 'Y', 1, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10201007, 7, 1, 'Y', 2, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10201007, 6, 1, 'Y', 3, SYSTIMESTAMP);

-- ──────────────────────────────────────────────
-- [10201008] ZipL 체크카드  card_id=10201008
--  안내장(119) + 비씨개인(10201004) + 포인트(10201003)
-- ──────────────────────────────────────────────
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10201008, 119, 3, 'Y', 1, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10201008, 7, 1, 'Y', 2, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10201008, 6, 1, 'Y', 3, SYSTIMESTAMP);

-- ──────────────────────────────────────────────
-- [10101004] 후불 하이패스카드(기업)  card_id=10101004
--  안내장(111) + 기업회원(10201005) + 법인연회비(10201006) + 포인트(10201003)
-- ──────────────────────────────────────────────
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101004, 111, 3, 'Y', 1, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101004, 8, 1, 'Y', 2, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101004, 9, 1, 'Y', 3, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101004, 6, 1, 'Y', 4, SYSTIMESTAMP);

-- ──────────────────────────────────────────────
-- [10101005] SOHO-BIZ카드  card_id=10101005
--  안내장(105) + 기업회원(10201005) + 법인연회비(10201006) + 포인트(10201003)
-- ──────────────────────────────────────────────
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101005, 105, 3, 'Y', 1, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101005, 8, 1, 'Y', 2, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101005, 9, 1, 'Y', 3, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101005, 6, 1, 'Y', 4, SYSTIMESTAMP);

-- ──────────────────────────────────────────────
-- [10101006] 국민행복카드(신용)  card_id=10101006
--  안내장(124) + 국민행복약관(10201007) + 개인표준(10201001) + 개인부속(10101003) + 신용평점(10201002) + 포인트(10201003)
-- ──────────────────────────────────────────────
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101006, 124, 3, 'Y', 1, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101006, 10, 1, 'Y', 2, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101006, 3, 1, 'Y', 3, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101006, 4, 1, 'Y', 4, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101006, 5, 1, 'Y', 5, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101006, 6, 1, 'Y', 6, SYSTIMESTAMP);

-- ──────────────────────────────────────────────
-- [10101007] 후불 하이패스카드(개인)  card_id=10101007
--  안내장(111, 기업과 동일파일) + 개인표준(10201001) + 개인부속(10101003) + 신용평점(10201002) + 포인트(10201003)
-- ──────────────────────────────────────────────
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101007, 111, 3, 'Y', 1, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101007, 3, 1, 'Y', 2, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101007, 4, 1, 'Y', 3, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101007, 5, 1, 'Y', 4, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101007, 6, 1, 'Y', 5, SYSTIMESTAMP);

-- ──────────────────────────────────────────────
-- [10101008] 오늘은e 신용카드  card_id=10101008
--  안내장(106) + 개인표준(10201001) + 개인부속(10101003) + 신용평점(10201002) + 포인트(10201003)
-- ──────────────────────────────────────────────
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101008, 106, 3, 'Y', 1, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101008, 3, 1, 'Y', 2, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101008, 4, 1, 'Y', 3, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101008, 5, 1, 'Y', 4, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101008, 6, 1, 'Y', 5, SYSTIMESTAMP);

-- ──────────────────────────────────────────────
-- [10101009] BNK 부자되세요 홈쇼핑카드  card_id=10101009
--  안내장(113) + 개인표준(10201001) + 개인부속(10101003) + 신용평점(10201002) + 포인트(10201003)
-- ──────────────────────────────────────────────
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101009, 113, 3, 'Y', 1, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101009, 3, 1, 'Y', 2, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101009, 4, 1, 'Y', 3, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101009, 5, 1, 'Y', 4, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101009, 6, 1, 'Y', 5, SYSTIMESTAMP);

-- ──────────────────────────────────────────────
-- [10101010] 부산체육사랑카드  card_id=10101010
--  안내장(121) + 개인표준(10201001) + 개인부속(10101003) + 신용평점(10201002) + 포인트(10201003)
-- ──────────────────────────────────────────────
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101010, 121, 3, 'Y', 1, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101010, 3, 1, 'Y', 2, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101010, 4, 1, 'Y', 3, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101010, 5, 1, 'Y', 4, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101010, 6, 1, 'Y', 5, SYSTIMESTAMP);

-- ──────────────────────────────────────────────
-- [10101011] 팟(pod) 카드  card_id=10101011
--  안내장(117) + 개인표준(10201001) + 개인부속(10101003) + 신용평점(10201002) + 포인트(10201003)
-- ──────────────────────────────────────────────
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101011, 117, 3, 'Y', 1, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101011, 3, 1, 'Y', 2, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101011, 4, 1, 'Y', 3, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101011, 5, 1, 'Y', 4, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101011, 6, 1, 'Y', 5, SYSTIMESTAMP);

-- ──────────────────────────────────────────────
-- [10101012] ZipL 신용카드  card_id=10101012
--  안내장(118) + 개인표준(10201001) + 개인부속(10101003) + 신용평점(10201002) + 포인트(10201003)
-- ──────────────────────────────────────────────
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101012, 118, 3, 'Y', 1, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101012, 3, 1, 'Y', 2, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101012, 4, 1, 'Y', 3, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101012, 5, 1, 'Y', 4, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101012, 6, 1, 'Y', 5, SYSTIMESTAMP);

-- ──────────────────────────────────────────────
-- [10101013] SK OIL&LPG카드  card_id=10101013
--  안내장(116) + 개인표준(10201001) + 개인부속(10101003) + 신용평점(10201002) + 포인트(10201003)
-- ──────────────────────────────────────────────
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101013, 116, 3, 'Y', 1, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101013, 3, 1, 'Y', 2, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101013, 4, 1, 'Y', 3, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101013, 5, 1, 'Y', 4, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101013, 6, 1, 'Y', 5, SYSTIMESTAMP);

-- ──────────────────────────────────────────────
-- [10101014] BNK 프렌즈 신용카드  card_id=10101014
--  안내장(107) + 개인표준(10201001) + 개인부속(10101003) + 신용평점(10201002) + 포인트(10201003)
-- ──────────────────────────────────────────────
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101014, 107, 3, 'Y', 1, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101014, 3, 1, 'Y', 2, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101014, 4, 1, 'Y', 3, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101014, 5, 1, 'Y', 4, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101014, 6, 1, 'Y', 5, SYSTIMESTAMP);

-- ──────────────────────────────────────────────
-- [10101015] 딩딩 신용카드  card_id=10101015
--  안내장(110) + 개인표준(10201001) + 개인부속(10101003) + 신용평점(10201002) + 포인트(10201003)
-- ──────────────────────────────────────────────
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101015, 110, 3, 'Y', 1, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101015, 3, 1, 'Y', 2, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101015, 4, 1, 'Y', 3, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101015, 5, 1, 'Y', 4, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101015, 6, 1, 'Y', 5, SYSTIMESTAMP);

-- ──────────────────────────────────────────────
-- [10101016] 팜코카드  card_id=10101016
--  안내장(114) + 기업회원(10201005) + 법인연회비(10201006) + 포인트(10201003)
-- ──────────────────────────────────────────────
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101016, 114, 3, 'Y', 1, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101016, 8, 1, 'Y', 2, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101016, 9, 1, 'Y', 3, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101016, 6, 1, 'Y', 4, SYSTIMESTAMP);

-- ──────────────────────────────────────────────
-- [10301001] 부산 동백전 선불카드  card_id=10301001
--  안내장(103) + 선불카드표준약관(10201008)
-- ──────────────────────────────────────────────
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10301001, 103, 3, 'Y', 1, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10301001, 11, 1, 'Y', 2, SYSTIMESTAMP);

-- ──────────────────────────────────────────────
-- [10101017] BNK SIMPLE AMEX BLUE BUSINESS 카드  card_id=10101017
--  안내장(123) + 기업회원(10201005) + 법인연회비(10201006) + 포인트(10201003)
-- ──────────────────────────────────────────────
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101017, 123, 3, 'Y', 1, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101017, 8, 1, 'Y', 2, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101017, 9, 1, 'Y', 3, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101017, 6, 1, 'Y', 4, SYSTIMESTAMP);

-- ──────────────────────────────────────────────
-- [10101018] BNK Simple카드  card_id=10101018
--  안내장(120) + 기업회원(10201005) + 법인연회비(10201006) + 포인트(10201003)
-- ──────────────────────────────────────────────
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101018, 120, 3, 'Y', 1, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101018, 8, 1, 'Y', 2, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101018, 9, 1, 'Y', 3, SYSTIMESTAMP);
INSERT INTO CARD_TERMS (card_id, terms_id, group_id, required_yn, display_order, created_at) VALUES (10101018, 6, 1, 'Y', 4, SYSTIMESTAMP);

COMMIT;

-- ==============================================================
-- 12. CARD_KEYWORDS  (구조 동일)
-- ==============================================================
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101001, 6,  SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101001, 5,  SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101002, 6,  SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101002, 7,  SYSTIMESTAMP, 1, 'N');
-- 빵빵체크카드 (10201001) → 체크카드(2), 캐시백(4)
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10201001, 2,  SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10201001, 4,  SYSTIMESTAMP, 1, 'N');
-- 캐쉬백카드 (10101003) → 신용카드(3), 캐시백(4)
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101003, 3,  SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101003, 4,  SYSTIMESTAMP, 1, 'N');
-- 국민행복체크카드 (10201002) → 체크카드(2), 연회비없음(8)
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10201002, 2,  SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10201002, 8,  SYSTIMESTAMP, 1, 'N');
-- 부산 동백전 체크카드 (10201003) → 동백전(14), 지역화폐(15)
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10201003, 14, SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10201003, 15, SYSTIMESTAMP, 1, 'N');
-- 딩딩 체크카드 (10201004) → 체크카드(2), 쇼핑(9)
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10201004, 2,  SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10201004, 9,  SYSTIMESTAMP, 1, 'N');
-- 어디로든 그린체크카드 (10201005) → 체크카드(2), 친환경(28)
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10201005, 2,  SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10201005, 28, SYSTIMESTAMP, 1, 'N');
-- 2030 언택트 체크카드 (10201006) → 체크카드(2), 배달(13)
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10201006, 2,  SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10201006, 13, SYSTIMESTAMP, 1, 'N');
-- 오늘은e 체크카드 (10201007) → 체크카드(2), 교통(10)
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10201007, 2,  SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10201007, 10, SYSTIMESTAMP, 1, 'N');
-- ZipL 체크카드 (10201008) → 체크카드(2), 보험(27)
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10201008, 2,  SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10201008, 27, SYSTIMESTAMP, 1, 'N');
-- 후불 하이패스카드(기업) (10101004) → 하이패스(20), 기업카드(23)
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101004, 20, SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101004, 23, SYSTIMESTAMP, 1, 'N');
-- SOHO-BIZ카드 (10101005) → 기업카드(23), 소호(30)
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101005, 23, SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101005, 30, SYSTIMESTAMP, 1, 'N');
-- 국민행복카드 신용 (10101006) → 신용카드(3), 연회비없음(8)
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101006, 3,  SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101006, 8,  SYSTIMESTAMP, 1, 'N');
-- 후불 하이패스카드 개인 (10101007) → 하이패스(20), 신용카드(3)
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101007, 20, SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101007, 3,  SYSTIMESTAMP, 1, 'N');
-- 오늘은e 신용카드 (10101008) → 신용카드(3), 교통(10), 커피(12)
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101008, 3,  SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101008, 10, SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101008, 12, SYSTIMESTAMP, 1, 'N');
-- BNK 홈쇼핑카드 (10101009) → 신용카드(3), 쇼핑(9), 홈쇼핑(21)
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101009, 3,  SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101009, 9,  SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101009, 21, SYSTIMESTAMP, 1, 'N');
-- 부산체육사랑카드 (10101010) → 신용카드(3), 스포츠(22), 의료(16)
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101010, 3,  SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101010, 22, SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101010, 16, SYSTIMESTAMP, 1, 'N');
-- 팟(pod) 카드 (10101011) → 신용카드(3), 편의점(19)
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101011, 3,  SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101011, 19, SYSTIMESTAMP, 1, 'N');
-- ZipL 신용카드 (10101012) → 신용카드(3), 보험(27)
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101012, 3,  SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101012, 27, SYSTIMESTAMP, 1, 'N');
-- SK OIL&LPG카드 (10101013) → 신용카드(3), 주유(1)
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101013, 3,  SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101013, 1,  SYSTIMESTAMP, 1, 'N');
-- BNK 프렌즈 신용카드 (10101014) → 신용카드(3), 캐시백(4), 대중교통(11)
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101014, 3,  SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101014, 4,  SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101014, 11, SYSTIMESTAMP, 1, 'N');
-- 딩딩 신용카드 (10101015) → 신용카드(3), 주유(1), 커피(12), 영화(17)
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101015, 3,  SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101015, 1,  SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101015, 12, SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101015, 17, SYSTIMESTAMP, 1, 'N');
-- 팜코카드 (10101016) → 기업카드(23), 연회비없음(8)
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101016, 23, SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101016, 8,  SYSTIMESTAMP, 1, 'N');
-- 부산 동백전 선불카드 (10301001) → 선불카드(26), 동백전(14), 지역화폐(15)
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10301001, 26, SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10301001, 14, SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10301001, 15, SYSTIMESTAMP, 1, 'N');
-- BNK SIMPLE AMEX (10101017) → 기업카드(23), 포인트(5)
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101017, 23, SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101017, 5,  SYSTIMESTAMP, 1, 'N');
-- BNK Simple카드 (10101018) → 기업카드(23), 포인트(5), 소호(30)
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101018, 23, SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101018, 5,  SYSTIMESTAMP, 1, 'N');
INSERT INTO CARD_KEYWORDS (card_id, keyword_id, created_at, created_by, deleted_yn) VALUES (10101018, 30, SYSTIMESTAMP, 1, 'N');


-- ================================================================
-- 23. 시퀀스 재설정 (카드 시리얼)
-- CREDIT 18건 (10101001~10101018) → 다음 NEXTVAL=19
-- CHECK  8건  (10201001~10201008) → 다음 NEXTVAL=9
-- PREPAID 1건 (10301001)          → 다음 NEXTVAL=2
-- ================================================================
DECLARE v_curr NUMBER;
BEGIN
    SELECT SEQ_CARD_SERIAL_CREDIT.NEXTVAL INTO v_curr FROM DUAL;
    IF v_curr < 19 THEN
        EXECUTE IMMEDIATE 'ALTER SEQUENCE SEQ_CARD_SERIAL_CREDIT INCREMENT BY ' || (19 - v_curr);
        SELECT SEQ_CARD_SERIAL_CREDIT.NEXTVAL INTO v_curr FROM DUAL;
        EXECUTE IMMEDIATE 'ALTER SEQUENCE SEQ_CARD_SERIAL_CREDIT INCREMENT BY 1';
    END IF;

    SELECT SEQ_CARD_SERIAL_CHECK.NEXTVAL INTO v_curr FROM DUAL;
    IF v_curr < 9 THEN
        EXECUTE IMMEDIATE 'ALTER SEQUENCE SEQ_CARD_SERIAL_CHECK INCREMENT BY ' || (9 - v_curr);
        SELECT SEQ_CARD_SERIAL_CHECK.NEXTVAL INTO v_curr FROM DUAL;
        EXECUTE IMMEDIATE 'ALTER SEQUENCE SEQ_CARD_SERIAL_CHECK INCREMENT BY 1';
    END IF;

    SELECT SEQ_CARD_SERIAL_PREPAID.NEXTVAL INTO v_curr FROM DUAL;
    IF v_curr < 2 THEN
        EXECUTE IMMEDIATE 'ALTER SEQUENCE SEQ_CARD_SERIAL_PREPAID INCREMENT BY 1';
        SELECT SEQ_CARD_SERIAL_PREPAID.NEXTVAL INTO v_curr FROM DUAL;
        EXECUTE IMMEDIATE 'ALTER SEQUENCE SEQ_CARD_SERIAL_PREPAID INCREMENT BY 1';
    END IF;
END;
/

COMMIT;