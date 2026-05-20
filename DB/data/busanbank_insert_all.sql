-- ================================================================
-- BNK 부산은행 금융 상품 플랫폼 — 전체 더미/초기 데이터 INSERT
-- Oracle SQL*Plus / SQL Developer 안전 실행 버전
-- ================================================================
-- ※ & 기호를 변수치환하지 않도록 DEFINE OFF 설정
SET DEFINE OFF;
-- ※ SQL*Plus 프롬프트 끄기
SET VERIFY OFF;

-- ================================================================


-- ==============================================================
-- 01. COMMON_CODE_GROUPS
-- ==============================================================
-- 1. COMMON_CODE_GROUPS
-- ==============================================================
INSERT INTO COMMON_CODE_GROUPS (group_code, group_name, description, use_yn, created_at, deleted_yn)
VALUES ('USER_STATUS', '사용자 상태', '사용자 계정 상태 코드', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODE_GROUPS (group_code, group_name, description, use_yn, created_at, deleted_yn)
VALUES ('ADMIN_STATUS', '관리자 상태', '관리자 계정 상태 코드', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODE_GROUPS (group_code, group_name, description, use_yn, created_at, deleted_yn)
VALUES ('LOGIN_RESULT', '로그인 결과', '로그인 시도 결과 코드', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODE_GROUPS (group_code, group_name, description, use_yn, created_at, deleted_yn)
VALUES ('USER_TYPE', '사용자 유형', '일반회원/관리자 구분', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODE_GROUPS (group_code, group_name, description, use_yn, created_at, deleted_yn)
VALUES ('ACTOR_TYPE', '액터 유형', '감사 로그 액터 유형', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODE_GROUPS (group_code, group_name, description, use_yn, created_at, deleted_yn)
VALUES ('ACTION_TYPE', '액션 유형', '감사 로그 액션 유형', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODE_GROUPS (group_code, group_name, description, use_yn, created_at, deleted_yn)
VALUES ('TARGET_TYPE', '대상 유형', '감사 로그 대상 유형', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODE_GROUPS (group_code, group_name, description, use_yn, created_at, deleted_yn)
VALUES ('APPROVAL_STATUS', '결재 상태', '결재 요청 상태 코드', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODE_GROUPS (group_code, group_name, description, use_yn, created_at, deleted_yn)
VALUES ('REQUEST_TYPE', '요청 유형', '결재 요청 유형', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODE_GROUPS (group_code, group_name, description, use_yn, created_at, deleted_yn)
VALUES ('JOB_CODE', '직업 코드', '사용자 직업 분류', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODE_GROUPS (group_code, group_name, description, use_yn, created_at, deleted_yn)
VALUES ('INCOME_LEVEL', '소득 수준', '사용자 소득 등급', 'Y', SYSTIMESTAMP, 'N');

COMMIT;

-- ==============================================================


-- ==============================================================
-- 02. COMMON_CODES
-- ==============================================================
-- 2. COMMON_CODES
-- ==============================================================
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('USER_STATUS', 'ACTIVE', '정상', 'active', 1, '정상 사용 가능 상태', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('USER_STATUS', 'DORMANT', '휴면', 'dormant', 2, '장기 미접속 휴면 상태', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('USER_STATUS', 'LOCKED', '잠금', 'locked', 3, '로그인 실패 초과로 잠금', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('USER_STATUS', 'WITHDRAWN', '탈퇴', 'withdrawn', 4, '회원 탈퇴 처리됨', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('USER_STATUS', 'SUSPENDED', '정지', 'suspended', 5, '관리자 정지 처리', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('ADMIN_STATUS', 'ACTIVE', '정상', 'active', 1, '정상 활성 관리자', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('ADMIN_STATUS', 'INACTIVE', '비활성', 'inactive', 2, '비활성 처리된 관리자', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('ADMIN_STATUS', 'LOCKED', '잠금', 'locked', 3, '잠금 처리된 관리자', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('LOGIN_RESULT', 'SUCCESS', '성공', 'success', 1, '로그인 성공', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('LOGIN_RESULT', 'FAIL_PW', '비밀번호 오류', 'fail_pw', 2, '비밀번호 불일치', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('LOGIN_RESULT', 'FAIL_LOCKED', '계정 잠금', 'fail_locked', 3, '계정 잠금 상태', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('LOGIN_RESULT', 'FAIL_NOUSER', '미존재 계정', 'fail_nouser', 4, '존재하지 않는 계정', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('USER_TYPE', 'USER', '일반회원', 'user', 1, '일반 사용자', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('USER_TYPE', 'ADMIN', '관리자', 'admin', 2, '관리자', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('ACTOR_TYPE', 'USER', '사용자', 'user', 1, '일반 사용자', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('ACTOR_TYPE', 'ADMIN', '관리자', 'admin', 2, '관리자', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('ACTOR_TYPE', 'SYSTEM', '시스템', 'system', 3, '시스템 자동', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('ACTION_TYPE', 'CREATE', '생성', 'create', 1, '데이터 생성', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('ACTION_TYPE', 'UPDATE', '수정', 'update', 2, '데이터 수정', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('ACTION_TYPE', 'DELETE', '삭제', 'delete', 3, '데이터 삭제', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('ACTION_TYPE', 'LOGIN', '로그인', 'login', 4, '로그인', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('ACTION_TYPE', 'LOGOUT', '로그아웃', 'logout', 5, '로그아웃', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('ACTION_TYPE', 'APPROVE', '승인', 'approve', 6, '결재 승인', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('TARGET_TYPE', 'USER', '회원', 'user', 1, '일반 회원', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('TARGET_TYPE', 'CARD', '카드', 'card', 2, '카드 상품', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('TARGET_TYPE', 'TERMS', '약관', 'terms', 3, '약관', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('APPROVAL_STATUS', 'PENDING', '대기', 'pending', 1, '승인 대기', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('APPROVAL_STATUS', 'APPROVED', '승인', 'approved', 2, '승인 완료', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('APPROVAL_STATUS', 'REJECTED', '반려', 'rejected', 3, '반려 처리', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('REQUEST_TYPE', 'CARD_PUBLISH', '카드 게시', 'card_publish', 1, '카드 상품 게시 요청', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('REQUEST_TYPE', 'TERMS_PUBLISH', '약관 게시', 'terms_pub', 2, '약관 게시 요청', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('REQUEST_TYPE', 'USER_SUSPEND', '회원 정지', 'user_suspend', 3, '회원 정지 요청', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('JOB_CODE', 'EMPLOYEE', '직장인', '직장인', 1, '일반 직장인', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('JOB_CODE', 'SELF_EMP', '자영업자', '자영업자', 2, '개인사업자', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('JOB_CODE', 'STUDENT', '학생', '학생', 3, '대학생 포함', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('JOB_CODE', 'HOUSEWIFE', '주부', '주부', 4, '전업주부', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('JOB_CODE', 'FREELANCER', '프리랜서', '프리랜서', 5, '프리랜서', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('JOB_CODE', 'ETC', '기타', '기타', 6, '기타 직업', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('INCOME_LEVEL', 'HIGH', '고소득', 'high', 1, '연 8천만원 이상', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('INCOME_LEVEL', 'MID_HIGH', '중상소득', 'mid_high', 2, '연 5천~8천만원', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('INCOME_LEVEL', 'MID', '중소득', 'mid', 3, '연 3천~5천만원', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('INCOME_LEVEL', 'LOW_MID', '중하소득', 'low_mid', 4, '연 2천~3천만원', 'Y', SYSTIMESTAMP, 'N');
INSERT INTO COMMON_CODES (group_code, code, code_name, code_value, display_order, description, use_yn, created_at, deleted_yn)
VALUES ('INCOME_LEVEL', 'LOW', '저소득', 'low', 5, '연 2천만원 미만', 'Y', SYSTIMESTAMP, 'N');

COMMIT;

-- ==============================================================


-- ==============================================================
-- 03. ADMIN_ROLES
-- ==============================================================
-- 3. ADMIN_ROLES (역할 3종)
-- ==============================================================
INSERT INTO ADMIN_ROLES (role_code, role_name, description, created_at, deleted_yn)
VALUES ('SUPER_ADMIN', '최상위 관리자', '모든 기능 접근 가능한 슈퍼 관리자', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_ROLES (role_code, role_name, description, created_at, deleted_yn)
VALUES ('MANAGER', '중간 관리자', '카드/약관 관리 및 결재 처리 가능', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_ROLES (role_code, role_name, description, created_at, deleted_yn)
VALUES ('OPERATOR', '하위 운영자', '조회 및 기본 운영 업무만 가능', SYSTIMESTAMP, 'N');

COMMIT;

-- ==============================================================


-- ==============================================================
-- 04. ADMIN_PERMISSIONS
-- ==============================================================
-- 4. ADMIN_PERMISSIONS (권한 정의)
-- ==============================================================
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn)
VALUES ('CARD_VIEW', '카드 조회', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn)
VALUES ('CARD_CREATE', '카드 생성', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn)
VALUES ('CARD_UPDATE', '카드 수정', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn)
VALUES ('CARD_DELETE', '카드 삭제', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn)
VALUES ('CARD_PUBLISH', '카드 게시 승인', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn)
VALUES ('TERMS_VIEW', '약관 조회', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn)
VALUES ('TERMS_CREATE', '약관 생성', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn)
VALUES ('TERMS_UPDATE', '약관 수정', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn)
VALUES ('TERMS_PUBLISH', '약관 게시 승인', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn)
VALUES ('USER_VIEW', '회원 조회', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn)
VALUES ('USER_UPDATE', '회원 정보 수정', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn)
VALUES ('USER_SUSPEND', '회원 정지 처리', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn)
VALUES ('ADMIN_MANAGE', '관리자 계정 관리', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn)
VALUES ('ROLE_MANAGE', '역할/권한 관리', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn)
VALUES ('APPROVAL_REQUEST', '결재 요청', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn)
VALUES ('APPROVAL_PROCESS', '결재 처리', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn)
VALUES ('AUDIT_LOG_VIEW', '감사 로그 조회', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn)
VALUES ('SEARCH_KEYWORD_MANAGE', '검색 키워드 관리', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn)
VALUES ('STATISTICS_VIEW', '통계 조회', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_PERMISSIONS (permission_code, permission_name, created_at, deleted_yn)
VALUES ('SYSTEM_CONFIG', '시스템 설정', SYSTIMESTAMP, 'N');

COMMIT;

-- ==============================================================


-- ==============================================================
-- 05. ADMIN_USERS
-- ==============================================================
INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
VALUES ('super_admin', '$2a$12$AdminPlaceholderHashXXXXXXXXXXXXXXXXXXXXXXXXXXXXX01', '김슈퍼', 'super@bnkfinance.co.kr', '010-9001-0001', 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
VALUES ('card_manager1', '$2a$12$AdminPlaceholderHashXXXXXXXXXXXXXXXXXXXXXXXXXXXXX02', '이카드', 'card1@bnkfinance.co.kr', '010-9001-0002', 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
VALUES ('card_manager2', '$2a$12$AdminPlaceholderHashXXXXXXXXXXXXXXXXXXXXXXXXXXXXX03', '박상품', 'card2@bnkfinance.co.kr', '010-9001-0003', 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
VALUES ('card_manager3', '$2a$12$AdminPlaceholderHashXXXXXXXXXXXXXXXXXXXXXXXXXXXXX04', '최운영', 'card3@bnkfinance.co.kr', '010-9001-0004', 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
VALUES ('terms_manager1', '$2a$12$AdminPlaceholderHashXXXXXXXXXXXXXXXXXXXXXXXXXXXXX05', '정약관', 'terms1@bnkfinance.co.kr', '010-9001-0005', 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
VALUES ('terms_manager2', '$2a$12$AdminPlaceholderHashXXXXXXXXXXXXXXXXXXXXXXXXXXXXX06', '한법무', 'terms2@bnkfinance.co.kr', '010-9001-0006', 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
VALUES ('viewer1', '$2a$12$AdminPlaceholderHashXXXXXXXXXXXXXXXXXXXXXXXXXXXXX07', '윤조회', 'view1@bnkfinance.co.kr', '010-9001-0007', 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
VALUES ('viewer2', '$2a$12$AdminPlaceholderHashXXXXXXXXXXXXXXXXXXXXXXXXXXXXX08', '임뷰어', 'view2@bnkfinance.co.kr', '010-9001-0008', 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
VALUES ('viewer3', '$2a$12$AdminPlaceholderHashXXXXXXXXXXXXXXXXXXXXXXXXXXXXX09', '신열람', 'view3@bnkfinance.co.kr', '010-9001-0009', 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO ADMIN_USERS (username, password_hash, name, email, phone, status_code, created_at, deleted_yn)
VALUES ('viewer4', '$2a$12$AdminPlaceholderHashXXXXXXXXXXXXXXXXXXXXXXXXXXXXX10', '권뷰어', 'view4@bnkfinance.co.kr', '010-9001-0010', 'ACTIVE', SYSTIMESTAMP, 'N');

COMMIT;

-- ==============================================================
-- ==============================================================
-- 06. ROLE_PERMISSIONS
-- ==============================================================
-- SUPER_ADMIN(role_id=1): 전체 20개 권한
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1, 1,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1, 2,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1, 3,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1, 4,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1, 5,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1, 6,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1, 7,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1, 8,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (1, 9,  SYSTIMESTAMP, 'N');
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
-- MANAGER(role_id=2): 카드/약관/회원/결재 관련 권한 (13개)
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2, 1,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2, 2,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2, 3,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2, 5,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2, 6,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2, 7,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2, 8,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2, 9,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2, 10, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2, 15, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2, 16, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2, 17, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (2, 19, SYSTIMESTAMP, 'N');
-- OPERATOR(role_id=3): 조회/검색키워드/통계 권한 (6개)
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (3, 1,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (3, 6,  SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (3, 10, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (3, 17, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (3, 18, SYSTIMESTAMP, 'N');
INSERT INTO ROLE_PERMISSIONS (role_id, permission_id, created_at, deleted_yn) VALUES (3, 19, SYSTIMESTAMP, 'N');

COMMIT;

-- 07. ADMIN_USER_ROLES
-- ==============================================================
-- 7. ADMIN_USER_ROLES
-- ==============================================================
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn)
VALUES (1, 1, SYSTIMESTAMP, 1, 'N');
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn)
VALUES (2, 2, SYSTIMESTAMP, 1, 'N');
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn)
VALUES (3, 2, SYSTIMESTAMP, 1, 'N');
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn)
VALUES (4, 2, SYSTIMESTAMP, 1, 'N');
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn)
VALUES (5, 3, SYSTIMESTAMP, 1, 'N');
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn)
VALUES (6, 3, SYSTIMESTAMP, 1, 'N');
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn)
VALUES (7, 3, SYSTIMESTAMP, 1, 'N');
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn)
VALUES (8, 3, SYSTIMESTAMP, 1, 'N');
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn)
VALUES (9, 3, SYSTIMESTAMP, 1, 'N');
INSERT INTO ADMIN_USER_ROLES (admin_id, role_id, assigned_at, assigned_by, deleted_yn)
VALUES (10, 3, SYSTIMESTAMP, 1, 'N');

COMMIT;

-- ==============================================================


-- ==============================================================
-- 08. USERS
-- ==============================================================
-- 8. USERS (20명 더미)
-- ==============================================================
INSERT INTO USERS (email, password_hash, name, phone, birth_date, ci_value, job, income_level_code, credit_score, status_code, created_at, deleted_yn)
VALUES ('kim.minjun@email.com', '$2a$12$PlaceholderHashForUserPassword1.ReplaceWithRealBCrypt', '김민준', '010-1001-0001', TO_DATE('1990-03-15','YYYY-MM-DD'), 'CI_HASH_001', 'EMPLOYEE', 'MID_HIGH', 720, 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO USERS (email, password_hash, name, phone, birth_date, ci_value, job, income_level_code, credit_score, status_code, created_at, deleted_yn)
VALUES ('lee.soyeon@email.com', '$2a$12$PlaceholderHashForUserPassword1.ReplaceWithRealBCrypt', '이소연', '010-1001-0002', TO_DATE('1988-07-22','YYYY-MM-DD'), 'CI_HASH_002', 'EMPLOYEE', 'MID', 680, 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO USERS (email, password_hash, name, phone, birth_date, ci_value, job, income_level_code, credit_score, status_code, created_at, deleted_yn)
VALUES ('park.junho@email.com', '$2a$12$PlaceholderHashForUserPassword1.ReplaceWithRealBCrypt', '박준호', '010-1001-0003', TO_DATE('1995-11-08','YYYY-MM-DD'), 'CI_HASH_003', 'STUDENT', 'LOW', 450, 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO USERS (email, password_hash, name, phone, birth_date, ci_value, job, income_level_code, credit_score, status_code, created_at, deleted_yn)
VALUES ('choi.eunji@email.com', '$2a$12$PlaceholderHashForUserPassword1.ReplaceWithRealBCrypt', '최은지', '010-1001-0004', TO_DATE('1992-05-30','YYYY-MM-DD'), 'CI_HASH_004', 'EMPLOYEE', 'MID_HIGH', 750, 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO USERS (email, password_hash, name, phone, birth_date, ci_value, job, income_level_code, credit_score, status_code, created_at, deleted_yn)
VALUES ('jung.hyunwoo@email.com', '$2a$12$PlaceholderHashForUserPassword1.ReplaceWithRealBCrypt', '정현우', '010-1001-0005', TO_DATE('1985-09-14','YYYY-MM-DD'), 'CI_HASH_005', 'SELF_EMP', 'HIGH', 810, 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO USERS (email, password_hash, name, phone, birth_date, ci_value, job, income_level_code, credit_score, status_code, created_at, deleted_yn)
VALUES ('han.jiyoung@email.com', '$2a$12$PlaceholderHashForUserPassword1.ReplaceWithRealBCrypt', '한지영', '010-1001-0006', TO_DATE('1993-02-28','YYYY-MM-DD'), 'CI_HASH_006', 'EMPLOYEE', 'MID', 660, 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO USERS (email, password_hash, name, phone, birth_date, ci_value, job, income_level_code, credit_score, status_code, created_at, deleted_yn)
VALUES ('yoon.seungho@email.com', '$2a$12$PlaceholderHashForUserPassword1.ReplaceWithRealBCrypt', '윤승호', '010-1001-0007', TO_DATE('1987-12-03','YYYY-MM-DD'), 'CI_HASH_007', 'FREELANCER', 'MID_HIGH', 700, 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO USERS (email, password_hash, name, phone, birth_date, ci_value, job, income_level_code, credit_score, status_code, created_at, deleted_yn)
VALUES ('lim.chaeyeon@email.com', '$2a$12$PlaceholderHashForUserPassword1.ReplaceWithRealBCrypt', '임채연', '010-1001-0008', TO_DATE('1998-06-17','YYYY-MM-DD'), 'CI_HASH_008', 'STUDENT', 'LOW', 400, 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO USERS (email, password_hash, name, phone, birth_date, ci_value, job, income_level_code, credit_score, status_code, created_at, deleted_yn)
VALUES ('shin.donghyun@email.com', '$2a$12$PlaceholderHashForUserPassword1.ReplaceWithRealBCrypt', '신동현', '010-1001-0009', TO_DATE('1982-04-25','YYYY-MM-DD'), 'CI_HASH_009', 'EMPLOYEE', 'HIGH', 830, 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO USERS (email, password_hash, name, phone, birth_date, ci_value, job, income_level_code, credit_score, status_code, created_at, deleted_yn)
VALUES ('kwon.minji@email.com', '$2a$12$PlaceholderHashForUserPassword1.ReplaceWithRealBCrypt', '권민지', '010-1001-0010', TO_DATE('1996-08-11','YYYY-MM-DD'), 'CI_HASH_010', 'EMPLOYEE', 'MID', 620, 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO USERS (email, password_hash, name, phone, birth_date, ci_value, job, income_level_code, credit_score, status_code, created_at, deleted_yn)
VALUES ('oh.sanghoon@email.com', '$2a$12$PlaceholderHashForUserPassword1.ReplaceWithRealBCrypt', '오상훈', '010-1001-0011', TO_DATE('1989-01-07','YYYY-MM-DD'), 'CI_HASH_011', 'SELF_EMP', 'MID_HIGH', 730, 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO USERS (email, password_hash, name, phone, birth_date, ci_value, job, income_level_code, credit_score, status_code, created_at, deleted_yn)
VALUES ('seo.jieun@email.com', '$2a$12$PlaceholderHashForUserPassword1.ReplaceWithRealBCrypt', '서지은', '010-1001-0012', TO_DATE('1994-10-19','YYYY-MM-DD'), 'CI_HASH_012', 'HOUSEWIFE', 'MID', 580, 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO USERS (email, password_hash, name, phone, birth_date, ci_value, job, income_level_code, credit_score, status_code, created_at, deleted_yn)
VALUES ('bae.minseok@email.com', '$2a$12$PlaceholderHashForUserPassword1.ReplaceWithRealBCrypt', '배민석', '010-1001-0013', TO_DATE('1991-07-04','YYYY-MM-DD'), 'CI_HASH_013', 'EMPLOYEE', 'MID_HIGH', 760, 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO USERS (email, password_hash, name, phone, birth_date, ci_value, job, income_level_code, credit_score, status_code, created_at, deleted_yn)
VALUES ('jang.hyejin@email.com', '$2a$12$PlaceholderHashForUserPassword1.ReplaceWithRealBCrypt', '장혜진', '010-1001-0014', TO_DATE('1986-03-22','YYYY-MM-DD'), 'CI_HASH_014', 'EMPLOYEE', 'HIGH', 800, 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO USERS (email, password_hash, name, phone, birth_date, ci_value, job, income_level_code, credit_score, status_code, created_at, deleted_yn)
VALUES ('noh.jiho@email.com', '$2a$12$PlaceholderHashForUserPassword1.ReplaceWithRealBCrypt', '노지호', '010-1001-0015', TO_DATE('1997-09-30','YYYY-MM-DD'), 'CI_HASH_015', 'STUDENT', 'LOW', 380, 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO USERS (email, password_hash, name, phone, birth_date, ci_value, job, income_level_code, credit_score, status_code, created_at, deleted_yn)
VALUES ('moon.sungwon@email.com', '$2a$12$PlaceholderHashForUserPassword1.ReplaceWithRealBCrypt', '문성원', '010-1001-0016', TO_DATE('1983-05-13','YYYY-MM-DD'), 'CI_HASH_016', 'EMPLOYEE', 'HIGH', 820, 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO USERS (email, password_hash, name, phone, birth_date, ci_value, job, income_level_code, credit_score, status_code, created_at, deleted_yn)
VALUES ('yang.soojin@email.com', '$2a$12$PlaceholderHashForUserPassword1.ReplaceWithRealBCrypt', '양수진', '010-1001-0017', TO_DATE('1999-12-25','YYYY-MM-DD'), 'CI_HASH_017', 'STUDENT', 'LOW_MID', 480, 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO USERS (email, password_hash, name, phone, birth_date, ci_value, job, income_level_code, credit_score, status_code, created_at, deleted_yn)
VALUES ('hong.jongwoo@email.com', '$2a$12$PlaceholderHashForUserPassword1.ReplaceWithRealBCrypt', '홍종우', '010-1001-0018', TO_DATE('1980-08-08','YYYY-MM-DD'), 'CI_HASH_018', 'SELF_EMP', 'HIGH', 790, 'DORMANT', SYSTIMESTAMP, 'N');
INSERT INTO USERS (email, password_hash, name, phone, birth_date, ci_value, job, income_level_code, credit_score, status_code, created_at, deleted_yn)
VALUES ('ahn.yeeun@email.com', '$2a$12$PlaceholderHashForUserPassword1.ReplaceWithRealBCrypt', '안예은', '010-1001-0019', TO_DATE('1993-04-16','YYYY-MM-DD'), 'CI_HASH_019', 'FREELANCER', 'MID', 640, 'ACTIVE', SYSTIMESTAMP, 'N');
INSERT INTO USERS (email, password_hash, name, phone, birth_date, ci_value, job, income_level_code, credit_score, status_code, created_at, deleted_yn)
VALUES ('song.jaehyun@email.com', '$2a$12$PlaceholderHashForUserPassword1.ReplaceWithRealBCrypt', '송재현', '010-1001-0020', TO_DATE('1988-11-20','YYYY-MM-DD'), 'CI_HASH_020', 'EMPLOYEE', 'MID_HIGH', 710, 'WITHDRAWN', SYSTIMESTAMP, 'N');

COMMIT;

-- ==============================================================


-- ==============================================================
-- 09. USER_SESSIONS
-- ==============================================================
-- 9. USER_SESSIONS (샘플 세션 5건)
-- ==============================================================
INSERT INTO USER_SESSIONS (user_id, refresh_token, device_info, ip_address, revoked_yn, expires_at, created_at)
VALUES (1, 'rt_token_abc001_kim_minjun', 'iPhone 15 Pro', '211.234.12.10', 'N', SYSTIMESTAMP + INTERVAL '30' DAY, SYSTIMESTAMP);
INSERT INTO USER_SESSIONS (user_id, refresh_token, device_info, ip_address, revoked_yn, expires_at, created_at)
VALUES (2, 'rt_token_abc002_lee_soyeon', 'Galaxy S24', '112.175.33.22', 'N', SYSTIMESTAMP + INTERVAL '30' DAY, SYSTIMESTAMP);
INSERT INTO USER_SESSIONS (user_id, refresh_token, device_info, ip_address, revoked_yn, expires_at, created_at)
VALUES (4, 'rt_token_abc003_choi_eunji', 'Chrome/Windows 11', '1.227.84.51', 'N', SYSTIMESTAMP + INTERVAL '30' DAY, SYSTIMESTAMP);
INSERT INTO USER_SESSIONS (user_id, refresh_token, device_info, ip_address, revoked_yn, expires_at, created_at)
VALUES (7, 'rt_token_abc004_yoon_seungho', 'Safari/macOS', '218.38.71.99', 'N', SYSTIMESTAMP + INTERVAL '30' DAY, SYSTIMESTAMP);
INSERT INTO USER_SESSIONS (user_id, refresh_token, device_info, ip_address, revoked_yn, expires_at, created_at)
VALUES (13, 'rt_token_abc005_bae_minseok', 'Edge/Windows 10', '175.209.25.14', 'N', SYSTIMESTAMP + INTERVAL '30' DAY, SYSTIMESTAMP);

COMMIT;

-- ==============================================================


-- ==============================================================
-- 10. LOGIN_HISTORIES
-- ==============================================================
-- 10. LOGIN_HISTORIES (샘플 이력 15건)
-- ==============================================================
INSERT INTO LOGIN_HISTORIES (user_type_code, user_id, login_result_code, fail_reason, ip_address, device_info, login_at)
VALUES ('USER', 1, 'SUCCESS', NULL, '211.234.12.10', 'iPhone 15 Pro', SYSTIMESTAMP - INTERVAL '15' HOUR);
INSERT INTO LOGIN_HISTORIES (user_type_code, user_id, login_result_code, fail_reason, ip_address, device_info, login_at)
VALUES ('USER', 2, 'SUCCESS', NULL, '112.175.33.22', 'Galaxy S24', SYSTIMESTAMP - INTERVAL '14' HOUR);
INSERT INTO LOGIN_HISTORIES (user_type_code, user_id, login_result_code, fail_reason, ip_address, device_info, login_at)
VALUES ('USER', 3, 'FAIL_PW', '비밀번호 오류', '59.8.45.111', 'Chrome/Win10', SYSTIMESTAMP - INTERVAL '13' HOUR);
INSERT INTO LOGIN_HISTORIES (user_type_code, user_id, login_result_code, fail_reason, ip_address, device_info, login_at)
VALUES ('USER', 3, 'FAIL_PW', '비밀번호 오류', '59.8.45.111', 'Chrome/Win10', SYSTIMESTAMP - INTERVAL '12' HOUR);
INSERT INTO LOGIN_HISTORIES (user_type_code, user_id, login_result_code, fail_reason, ip_address, device_info, login_at)
VALUES ('USER', 3, 'SUCCESS', NULL, '59.8.45.111', 'Chrome/Win10', SYSTIMESTAMP - INTERVAL '11' HOUR);
INSERT INTO LOGIN_HISTORIES (user_type_code, user_id, login_result_code, fail_reason, ip_address, device_info, login_at)
VALUES ('USER', 4, 'SUCCESS', NULL, '1.227.84.51', 'Chrome/Win11', SYSTIMESTAMP - INTERVAL '10' HOUR);
INSERT INTO LOGIN_HISTORIES (user_type_code, user_id, login_result_code, fail_reason, ip_address, device_info, login_at)
VALUES ('USER', 5, 'SUCCESS', NULL, '39.118.24.55', 'Safari/macOS', SYSTIMESTAMP - INTERVAL '9' HOUR);
INSERT INTO LOGIN_HISTORIES (user_type_code, user_id, login_result_code, fail_reason, ip_address, device_info, login_at)
VALUES ('ADMIN', 1, 'SUCCESS', NULL, '10.0.0.1', 'Chrome/Win11', SYSTIMESTAMP - INTERVAL '8' HOUR);
INSERT INTO LOGIN_HISTORIES (user_type_code, user_id, login_result_code, fail_reason, ip_address, device_info, login_at)
VALUES ('ADMIN', 2, 'SUCCESS', NULL, '10.0.0.2', 'Chrome/Win11', SYSTIMESTAMP - INTERVAL '7' HOUR);
INSERT INTO LOGIN_HISTORIES (user_type_code, user_id, login_result_code, fail_reason, ip_address, device_info, login_at)
VALUES ('ADMIN', 3, 'FAIL_PW', '비밀번호 오류', '10.0.0.3', 'Firefox/Win10', SYSTIMESTAMP - INTERVAL '6' HOUR);
INSERT INTO LOGIN_HISTORIES (user_type_code, user_id, login_result_code, fail_reason, ip_address, device_info, login_at)
VALUES ('USER', 6, 'SUCCESS', NULL, '118.37.9.200', 'Galaxy Tab S9', SYSTIMESTAMP - INTERVAL '5' HOUR);
INSERT INTO LOGIN_HISTORIES (user_type_code, user_id, login_result_code, fail_reason, ip_address, device_info, login_at)
VALUES ('USER', 7, 'SUCCESS', NULL, '218.38.71.99', 'Safari/macOS', SYSTIMESTAMP - INTERVAL '4' HOUR);
INSERT INTO LOGIN_HISTORIES (user_type_code, user_id, login_result_code, fail_reason, ip_address, device_info, login_at)
VALUES ('USER', 8, 'FAIL_PW', '비밀번호 오류', '222.100.5.88', 'Chrome/Android', SYSTIMESTAMP - INTERVAL '3' HOUR);
INSERT INTO LOGIN_HISTORIES (user_type_code, user_id, login_result_code, fail_reason, ip_address, device_info, login_at)
VALUES ('USER', 9, 'SUCCESS', NULL, '175.209.25.14', 'Edge/Win10', SYSTIMESTAMP - INTERVAL '2' HOUR);
INSERT INTO LOGIN_HISTORIES (user_type_code, user_id, login_result_code, fail_reason, ip_address, device_info, login_at)
VALUES ('USER', 10, 'SUCCESS', NULL, '121.66.32.7', 'Chrome/Win11', SYSTIMESTAMP - INTERVAL '1' HOUR);

COMMIT;

-- ==============================================================


-- ==============================================================
-- 11. AUDIT_LOGS
-- ==============================================================
-- 11. AUDIT_LOGS (샘플 감사 로그 10건)
-- ==============================================================
INSERT INTO AUDIT_LOGS (actor_type_code, actor_id, action_type_code, target_type_code, target_id, description, ip_address, created_at)
VALUES ('ADMIN', 1, 'CREATE', 'CARD', 1, '카드 상품 생성: REX2_포인트형(개인)', '10.0.0.1', SYSTIMESTAMP - INTERVAL '10' DAY);
INSERT INTO AUDIT_LOGS (actor_type_code, actor_id, action_type_code, target_type_code, target_id, description, ip_address, created_at)
VALUES ('ADMIN', 1, 'APPROVE', 'CARD', 1, '카드 상품 게시 승인: REX2_포인트형(개인)', '10.0.0.1', SYSTIMESTAMP - INTERVAL '9' DAY);
INSERT INTO AUDIT_LOGS (actor_type_code, actor_id, action_type_code, target_type_code, target_id, description, ip_address, created_at)
VALUES ('ADMIN', 2, 'CREATE', 'TERMS', 1, '약관 생성: 서비스 이용약관 v1.0', '10.0.0.1', SYSTIMESTAMP - INTERVAL '8' DAY);
INSERT INTO AUDIT_LOGS (actor_type_code, actor_id, action_type_code, target_type_code, target_id, description, ip_address, created_at)
VALUES ('ADMIN', 2, 'UPDATE', 'CARD', 5, '카드 정보 수정: 국민행복체크카드', '10.0.0.1', SYSTIMESTAMP - INTERVAL '7' DAY);
INSERT INTO AUDIT_LOGS (actor_type_code, actor_id, action_type_code, target_type_code, target_id, description, ip_address, created_at)
VALUES ('ADMIN', 3, 'CREATE', 'CARD', 10, '카드 상품 생성: 오늘은e 체크카드', '10.0.0.1', SYSTIMESTAMP - INTERVAL '6' DAY);
INSERT INTO AUDIT_LOGS (actor_type_code, actor_id, action_type_code, target_type_code, target_id, description, ip_address, created_at)
VALUES ('USER', 1, 'CREATE', 'USER', 1, '회원 가입: 김민준', '10.0.0.1', SYSTIMESTAMP - INTERVAL '5' DAY);
INSERT INTO AUDIT_LOGS (actor_type_code, actor_id, action_type_code, target_type_code, target_id, description, ip_address, created_at)
VALUES ('USER', 2, 'UPDATE', 'USER', 2, '회원 정보 수정: 이소연', '10.0.0.1', SYSTIMESTAMP - INTERVAL '4' DAY);
INSERT INTO AUDIT_LOGS (actor_type_code, actor_id, action_type_code, target_type_code, target_id, description, ip_address, created_at)
VALUES ('ADMIN', 4, 'UPDATE', 'TERMS', 2, '약관 상태 변경: DRAFT → REVIEW', '10.0.0.1', SYSTIMESTAMP - INTERVAL '3' DAY);
INSERT INTO AUDIT_LOGS (actor_type_code, actor_id, action_type_code, target_type_code, target_id, description, ip_address, created_at)
VALUES ('ADMIN', 1, 'APPROVE', 'TERMS', 2, '약관 게시 승인', '10.0.0.1', SYSTIMESTAMP - INTERVAL '2' DAY);
INSERT INTO AUDIT_LOGS (actor_type_code, actor_id, action_type_code, target_type_code, target_id, description, ip_address, created_at)
VALUES ('ADMIN', 2, 'CREATE', 'USER', NULL, '관리자에 의한 회원 정지 처리', '10.0.0.1', SYSTIMESTAMP - INTERVAL '1' DAY);

COMMIT;

-- ==============================================================


-- ==============================================================
-- 12. APPROVAL_REQUESTS
-- ==============================================================
-- 12. APPROVAL_REQUESTS + APPROVAL_LINES (샘플 결재 3건)
-- ==============================================================
INSERT INTO APPROVAL_REQUESTS (request_type_code, requester_admin_id, target_id, status_code, request_comment, requested_at, completed_at)
VALUES ('CARD_PUBLISH', 2, 1, 'APPROVED', 'REX2 카드 신규 게시 요청드립니다.', SYSTIMESTAMP - INTERVAL '3' DAY, SYSTIMESTAMP);
INSERT INTO APPROVAL_REQUESTS (request_type_code, requester_admin_id, target_id, status_code, request_comment, requested_at, completed_at)
VALUES ('TERMS_PUBLISH', 3, 2, 'APPROVED', '서비스 이용약관 v1.0 게시 요청드립니다.', SYSTIMESTAMP - INTERVAL '2' DAY, SYSTIMESTAMP);
INSERT INTO APPROVAL_REQUESTS (request_type_code, requester_admin_id, target_id, status_code, request_comment, requested_at, completed_at)
VALUES ('CARD_PUBLISH', 4, 15, 'PENDING', '오늘은e 신용카드 게시 요청입니다.', SYSTIMESTAMP - INTERVAL '1' DAY, NULL);

INSERT INTO APPROVAL_LINES (approval_id, approver_admin_id, approval_order, status_code, comment_text, approved_at)
VALUES (1, 2, 1, 'APPROVED', '검토 후 승인합니다.', SYSTIMESTAMP);
INSERT INTO APPROVAL_LINES (approval_id, approver_admin_id, approval_order, status_code, comment_text, approved_at)
VALUES (1, 1, 2, 'APPROVED', '최종 승인합니다.', SYSTIMESTAMP);
INSERT INTO APPROVAL_LINES (approval_id, approver_admin_id, approval_order, status_code, comment_text, approved_at)
VALUES (2, 2, 1, 'APPROVED', '약관 내용 확인 완료, 승인합니다.', SYSTIMESTAMP);
INSERT INTO APPROVAL_LINES (approval_id, approver_admin_id, approval_order, status_code, comment_text, approved_at)
VALUES (2, 1, 2, 'APPROVED', '최종 승인합니다.', SYSTIMESTAMP);
INSERT INTO APPROVAL_LINES (approval_id, approver_admin_id, approval_order, status_code, comment_text, approved_at)
VALUES (3, 3, 1, 'PENDING', NULL, NULL);

COMMIT;

-- ==============================================================


-- ==============================================================
-- 13. APPROVAL_LINES
-- ==============================================================


-- ==============================================================
-- 14. CARD_CATEGORIES (23건 — 변경 없음)
-- ==============================================================
-- 1. CARD_CATEGORIES (혜택 카테고리)
INSERT INTO CARD_CATEGORIES (category_code, category_name, icon_code, display_order, use_yn, created_at)
VALUES ('TRAVEL', '여행/항공', 'icon-travel', 1, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CATEGORIES (category_code, category_name, icon_code, display_order, use_yn, created_at)
VALUES ('DINING', '식음료/카페', 'icon-dining', 2, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CATEGORIES (category_code, category_name, icon_code, display_order, use_yn, created_at)
VALUES ('SHOPPING', '쇼핑/백화점', 'icon-shopping', 3, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CATEGORIES (category_code, category_name, icon_code, display_order, use_yn, created_at)
VALUES ('OIL', '주유/충전', 'icon-oil', 4, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CATEGORIES (category_code, category_name, icon_code, display_order, use_yn, created_at)
VALUES ('TRANSPORT', '교통/대중교통', 'icon-transport', 5, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CATEGORIES (category_code, category_name, icon_code, display_order, use_yn, created_at)
VALUES ('LEISURE', '여가/문화', 'icon-leisure', 6, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CATEGORIES (category_code, category_name, icon_code, display_order, use_yn, created_at)
VALUES ('MEDICAL', '의료/약국', 'icon-medical', 7, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CATEGORIES (category_code, category_name, icon_code, display_order, use_yn, created_at)
VALUES ('TELECOM', '통신/휴대폰', 'icon-telecom', 8, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CATEGORIES (category_code, category_name, icon_code, display_order, use_yn, created_at)
VALUES ('ONLINE', '온라인/구독', 'icon-online', 9, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CATEGORIES (category_code, category_name, icon_code, display_order, use_yn, created_at)
VALUES ('DELIVERY', '배달/음식', 'icon-delivery', 10, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CATEGORIES (category_code, category_name, icon_code, display_order, use_yn, created_at)
VALUES ('EDUCATION', '교육/학원', 'icon-education', 11, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CATEGORIES (category_code, category_name, icon_code, display_order, use_yn, created_at)
VALUES ('LIVING', '생활/관리비', 'icon-living', 12, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CATEGORIES (category_code, category_name, icon_code, display_order, use_yn, created_at)
VALUES ('INSURANCE', '보험', 'icon-insurance', 13, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CATEGORIES (category_code, category_name, icon_code, display_order, use_yn, created_at)
VALUES ('CASHBACK', '캐시백/포인트', 'icon-cashback', 14, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CATEGORIES (category_code, category_name, icon_code, display_order, use_yn, created_at)
VALUES ('SPORT', '스포츠/레저', 'icon-sport', 15, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CATEGORIES (category_code, category_name, icon_code, display_order, use_yn, created_at)
VALUES ('CONVENIENCE', '편의점', 'icon-convenience', 16, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CATEGORIES (category_code, category_name, icon_code, display_order, use_yn, created_at)
VALUES ('TOLL', '고속도로/하이패스', 'icon-toll', 17, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CATEGORIES (category_code, category_name, icon_code, display_order, use_yn, created_at)
VALUES ('PARKING', '주차', 'icon-parking', 18, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CATEGORIES (category_code, category_name, icon_code, display_order, use_yn, created_at)
VALUES ('BEAUTY', '미용/세탁', 'icon-beauty', 19, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CATEGORIES (category_code, category_name, icon_code, display_order, use_yn, created_at)
VALUES ('MART', '마트/대형마트', 'icon-mart', 20, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CATEGORIES (category_code, category_name, icon_code, display_order, use_yn, created_at)
VALUES ('LOCAL', '지역화폐', 'icon-local', 21, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CATEGORIES (category_code, category_name, icon_code, display_order, use_yn, created_at)
VALUES ('BUSINESS', '사업지원', 'icon-business', 22, 'Y', SYSTIMESTAMP);
INSERT INTO CARD_CATEGORIES (category_code, category_name, icon_code, display_order, use_yn, created_at)
VALUES ('ETC', '기타혜택', 'icon-etc', 23, 'Y', SYSTIMESTAMP);

COMMIT;




-- ==============================================================
-- 15. CARDS (27건 — card_id PK 재매핑, card_type PREPAID 적용)
-- ==============================================================
-- card_id 매핑표:
--  구 PK → 신 PK              카드 유형       카드명
-- ──────────────────────────────────────────────────────────────
--   1   → 10101001  [CREDIT  ]  REX2_포인트형(개인)
--   2   → 10101002  [CREDIT  ]  REX2_대한항공마일리지형(개인)
--   3   → 10201001  [CHECK   ]  빵빵체크카드
--   4   → 10101003  [CREDIT  ]  캐쉬백카드
--   5   → 10201002  [CHECK   ]  국민행복체크카드
--   6   → 10201003  [CHECK   ]  부산 동백전 체크카드
--   7   → 10201004  [CHECK   ]  딩딩 체크카드
--   8   → 10201005  [CHECK   ]  어디로든 그린체크카드
--   9   → 10201006  [CHECK   ]  2030 언택트 체크카드
--  10   → 10201007  [CHECK   ]  오늘은e 체크카드
--  11   → 10201008  [CHECK   ]  ZipL 체크카드
--  12   → 10101004  [CREDIT  ]  후불 하이패스카드(기업)
--  13   → 10101005  [CREDIT  ]  SOHO-BIZ카드
--  14   → 10101006  [CREDIT  ]  국민행복카드
--  15   → 10101007  [CREDIT  ]  후불 하이패스카드
--  16   → 10101008  [CREDIT  ]  오늘은e 신용카드
--  17   → 10101009  [CREDIT  ]  BNK 부자되세요 홈쇼핑카드
--  18   → 10101010  [CREDIT  ]  부산체육사랑카드
--  19   → 10101011  [CREDIT  ]  팟(pod) 카드
--  20   → 10101012  [CREDIT  ]  ZipL 신용카드
--  21   → 10101013  [CREDIT  ]  SK OIL&LPG카드
--  22   → 10101014  [CREDIT  ]  BNK 프렌즈 신용카드
--  23   → 10101015  [CREDIT  ]  딩딩 신용카드
--  24   → 10101016  [CREDIT  ]  팜코카드
--  25   → 10301001  [PREPAID ]  부산 동백전 선불카드 ★ PREPAID 변경
--  26   → 10101017  [CREDIT  ]  BNK SIMPLE AMEX BLUE BUSINESS 카드
--  27   → 10101018  [CREDIT  ]  BNK Simple카드

-- [01] REX2_포인트형(개인)
INSERT INTO CARDS (card_id, card_code, card_type, card_name, company_name, company_code, brand_name,
    annual_fee_domestic, annual_fee_overseas, previous_month_spend,
    minimum_age, target_user, summary_description,
    searchable_yn, visible_yn, approval_required_yn, card_status,
    publish_start_at, created_at, deleted_yn)
VALUES (10101001, 'REX2_POINT', 'CREDIT', 'REX2_포인트형(개인)', 'BNK부산은행', '01', 'LOCAL',
    200000, 230000, 300000,
    19, '개인(가족회원)', 'The Return of Royalty, REXⅡ카드',
    'Y', 'Y', 'Y', 'PUBLISHED',
    SYSTIMESTAMP, SYSTIMESTAMP, 'N');

-- [02] REX2_대한항공마일리지형(개인)
INSERT INTO CARDS (card_id, card_code, card_type, card_name, company_name, company_code, brand_name,
    annual_fee_domestic, annual_fee_overseas, previous_month_spend,
    minimum_age, target_user, summary_description,
    searchable_yn, visible_yn, approval_required_yn, card_status,
    publish_start_at, created_at, deleted_yn)
VALUES (10101002, 'REX2_MILE', 'CREDIT', 'REX2_대한항공마일리지형(개인)', 'BNK부산은행', '01', 'LOCAL',
    200000, 230000, 300000,
    19, '개인(가족회원)', 'The Return of Royalty, REXⅡ카드',
    'Y', 'Y', 'Y', 'PUBLISHED',
    SYSTIMESTAMP, SYSTIMESTAMP, 'N');

-- [03] 빵빵체크카드
INSERT INTO CARDS (card_id, card_code, card_type, card_name, company_name, company_code, brand_name,
    annual_fee_domestic, annual_fee_overseas, previous_month_spend,
    minimum_age, target_user, summary_description,
    searchable_yn, visible_yn, approval_required_yn, card_status,
    publish_start_at, created_at, deleted_yn)
VALUES (10201001, 'BBANG_CHECK', 'CHECK', '빵빵체크카드', 'BNK부산은행', '01', 'LOCAL',
    0, 0, 0,
    18, '만 18세 이상 개인 본인 회원', '혜택이 빵빵한 !! 빵빵체크카드 !!',
    'Y', 'Y', 'Y', 'PUBLISHED',
    SYSTIMESTAMP, SYSTIMESTAMP, 'N');

-- [04] 캐쉬백카드
INSERT INTO CARDS (card_id, card_code, card_type, card_name, company_name, company_code, brand_name,
    annual_fee_domestic, annual_fee_overseas, previous_month_spend,
    minimum_age, target_user, summary_description,
    searchable_yn, visible_yn, approval_required_yn, card_status,
    publish_start_at, created_at, deleted_yn)
VALUES (10101003, 'CASHBACK_CARD', 'CREDIT', '캐쉬백카드', 'BNK부산은행', '01', 'LOCAL',
    10000, 10000, 200000,
    19, '개인', '매달 결제일에 최대 0.7% 캐쉬백혜택으로 돌아온다! 생활편의 업종은 기존카드처럼 혜택받고, 기타 업종은 이용금액에 따라 일정률로 할인 받는 캐쉬백카드! 할인제외 대상 가맹점 및 업종을 제외하고 국내 모든 가맹점에서 카드이용시 상품에서 정한 할인율 만큼을 고객님 결제일에 마이너스(-) 방식으로 차감하여 청구, 할인 적용하는 방식의 카드 예) 2천만원 차량 ',
    'Y', 'Y', 'Y', 'PUBLISHED',
    SYSTIMESTAMP, SYSTIMESTAMP, 'N');

-- [05] 국민행복체크카드
INSERT INTO CARDS (card_id, card_code, card_type, card_name, company_name, company_code, brand_name,
    annual_fee_domestic, annual_fee_overseas, previous_month_spend,
    minimum_age, target_user, summary_description,
    searchable_yn, visible_yn, approval_required_yn, card_status,
    publish_start_at, created_at, deleted_yn)
VALUES (10201002, 'GOV_HAPPY_CHECK', 'CHECK', '국민행복체크카드', 'BNK부산은행', '01', 'LOCAL',
    0, 0, 0,
    18, '개인', '정부의 다양한 바우처사업을 통합하여 사용 가능한 카드',
    'Y', 'Y', 'Y', 'PUBLISHED',
    SYSTIMESTAMP, SYSTIMESTAMP, 'N');

-- [06] 부산 동백전 체크카드
INSERT INTO CARDS (card_id, card_code, card_type, card_name, company_name, company_code, brand_name,
    annual_fee_domestic, annual_fee_overseas, previous_month_spend,
    minimum_age, target_user, summary_description,
    searchable_yn, visible_yn, approval_required_yn, card_status,
    publish_start_at, created_at, deleted_yn)
VALUES (10201003, 'DONGBAEK_CHECK', 'CHECK', '부산 동백전 체크카드', 'BNK부산은행', '01', 'LOCAL',
    0, 0, 0,
    14, '후불교통 : 만18세 이상 비교통 : 만 14세 이상 브랜드 : 국내전용', '부산지역 경제활성화를 위한, 부산지역화폐 동백전! 지역자금의 역외유출 방지를 위해 부산시 내 가맹점 매출액에 대해 할인 혜택 제공 전통시장 매출액에 대한 추가 혜택 제공을 통해 지역 내 소상공인 매출 증대를 통한 지역경기 활성화에 기여',
    'Y', 'Y', 'Y', 'PUBLISHED',
    SYSTIMESTAMP, SYSTIMESTAMP, 'N');

-- [07] 딩딩 체크카드
INSERT INTO CARDS (card_id, card_code, card_type, card_name, company_name, company_code, brand_name,
    annual_fee_domestic, annual_fee_overseas, previous_month_spend,
    minimum_age, target_user, summary_description,
    searchable_yn, visible_yn, approval_required_yn, card_status,
    publish_start_at, created_at, deleted_yn)
VALUES (10201004, 'DINGDING_CHECK', 'CHECK', '딩딩 체크카드', 'BNK부산은행', '01', 'LOCAL',
    0, 0, 0,
    18, '개인', '즐거움 가득, 혜택 가득~ DingDing 체크카드~!',
    'Y', 'Y', 'Y', 'PUBLISHED',
    SYSTIMESTAMP, SYSTIMESTAMP, 'N');

-- [08] 어디로든 그린체크카드
INSERT INTO CARDS (card_id, card_code, card_type, card_name, company_name, company_code, brand_name,
    annual_fee_domestic, annual_fee_overseas, previous_month_spend,
    minimum_age, target_user, summary_description,
    searchable_yn, visible_yn, approval_required_yn, card_status,
    publish_start_at, created_at, deleted_yn)
VALUES (10201005, 'GREEN_CHECK', 'CHECK', '어디로든 그린체크카드', 'BNK부산은행', '01', 'LOCAL',
    0, 0, 0,
    18, '개인', '친환경 업종 특화 체크카드! 어디로든 그린체크카드!',
    'Y', 'Y', 'Y', 'PUBLISHED',
    SYSTIMESTAMP, SYSTIMESTAMP, 'N');

-- [09] 2030 언택트 체크카드
INSERT INTO CARDS (card_id, card_code, card_type, card_name, company_name, company_code, brand_name,
    annual_fee_domestic, annual_fee_overseas, previous_month_spend,
    minimum_age, target_user, summary_description,
    searchable_yn, visible_yn, approval_required_yn, card_status,
    publish_start_at, created_at, deleted_yn)
VALUES (10201006, 'UNTACT_CHECK', 'CHECK', '2030 언택트 체크카드', 'BNK부산은행', '01', 'LOCAL',
    0, 0, 0,
    18, '개인 카드디자인 블루 : 내국인 웰컴글로벌 : 외국인', '일상 속 언택트 서비스로 구성된 비대면 특화 카드! 2030 언택트 체크카드!',
    'Y', 'Y', 'Y', 'PUBLISHED',
    SYSTIMESTAMP, SYSTIMESTAMP, 'N');

-- [10] 오늘은e 체크카드
INSERT INTO CARDS (card_id, card_code, card_type, card_name, company_name, company_code, brand_name,
    annual_fee_domestic, annual_fee_overseas, previous_month_spend,
    minimum_age, target_user, summary_description,
    searchable_yn, visible_yn, approval_required_yn, card_status,
    publish_start_at, created_at, deleted_yn)
VALUES (10201007, 'ONEULE_CHECK', 'CHECK', '오늘은e 체크카드', 'BNK부산은행', '01', 'LOCAL',
    0, 0, 0,
    18, '', '각종 페이 및 생활 서비스 할인되는 오늘은e 체크카드!PAYCO, 삼성페이, 네이버페이, 카카오페이, 썸패스 결제 시 청구할인',
    'Y', 'Y', 'Y', 'PUBLISHED',
    SYSTIMESTAMP, SYSTIMESTAMP, 'N');

-- [11] ZipL 체크카드
INSERT INTO CARDS (card_id, card_code, card_type, card_name, company_name, company_code, brand_name,
    annual_fee_domestic, annual_fee_overseas, previous_month_spend,
    minimum_age, target_user, summary_description,
    searchable_yn, visible_yn, approval_required_yn, card_status,
    publish_start_at, created_at, deleted_yn)
VALUES (10201008, 'ZIPL_CHECK', 'CHECK', 'ZipL 체크카드', 'BNK부산은행', '01', 'LOCAL',
    0, 0, 0,
    18, '개인', '우리집에 플러스되는 체크카드! 생활요금 할인 체크카드',
    'Y', 'Y', 'Y', 'PUBLISHED',
    SYSTIMESTAMP, SYSTIMESTAMP, 'N');

-- [12] 후불 하이패스카드(기업)
INSERT INTO CARDS (card_id, card_code, card_type, card_name, company_name, company_code, brand_name,
    annual_fee_domestic, annual_fee_overseas, previous_month_spend,
    minimum_age, target_user, summary_description,
    searchable_yn, visible_yn, approval_required_yn, card_status,
    publish_start_at, created_at, deleted_yn)
VALUES (10101004, 'HIPASS_BIZ', 'CREDIT', '후불 하이패스카드(기업)', 'BNK부산은행', '01', 'LOCAL',
    10000, 0, 0,
    19, '개인사업자, 법인', '하이패스 차로 통과시 후불방식으로 이용하고 결제일에 결제하는 후불 하이패스카드! 전국고속도로, 민자도로 등에 설치된 톨게이트의 하이패스 車路 통과시 후불방식으로 이용하고 신용카드 결제일에 결제하는 카드',
    'Y', 'Y', 'Y', 'PUBLISHED',
    SYSTIMESTAMP, SYSTIMESTAMP, 'N');

-- [13] SOHO-BIZ카드
INSERT INTO CARDS (card_id, card_code, card_type, card_name, company_name, company_code, brand_name,
    annual_fee_domestic, annual_fee_overseas, previous_month_spend,
    minimum_age, target_user, summary_description,
    searchable_yn, visible_yn, approval_required_yn, card_status,
    publish_start_at, created_at, deleted_yn)
VALUES (10101005, 'SOHO_BIZ', 'CREDIT', 'SOHO-BIZ카드', 'BNK부산은행', '01', 'LOCAL',
    15000, 0, 200000,
    19, '기업회원(사용자지정,공용) 개인사업자, 법인', '당행 최초로 보증료(신용보증기금, 기술보증기금, 신용보증재단) 할인 서비스 탑재!',
    'Y', 'Y', 'Y', 'PUBLISHED',
    SYSTIMESTAMP, SYSTIMESTAMP, 'N');

-- [14] 국민행복카드
INSERT INTO CARDS (card_id, card_code, card_type, card_name, company_name, company_code, brand_name,
    annual_fee_domestic, annual_fee_overseas, previous_month_spend,
    minimum_age, target_user, summary_description,
    searchable_yn, visible_yn, approval_required_yn, card_status,
    publish_start_at, created_at, deleted_yn)
VALUES (10101006, 'GOV_HAPPY', 'CREDIT', '국민행복카드', 'BNK부산은행', '01', 'LOCAL',
    0, 0, 0,
    19, '개인', '정부의 다양한 바우처사업을 통합하여 사용 가능한 카드',
    'Y', 'Y', 'Y', 'PUBLISHED',
    SYSTIMESTAMP, SYSTIMESTAMP, 'N');

-- [15] 후불 하이패스카드
INSERT INTO CARDS (card_id, card_code, card_type, card_name, company_name, company_code, brand_name,
    annual_fee_domestic, annual_fee_overseas, previous_month_spend,
    minimum_age, target_user, summary_description,
    searchable_yn, visible_yn, approval_required_yn, card_status,
    publish_start_at, created_at, deleted_yn)
VALUES (10101007, 'HIPASS', 'CREDIT', '후불 하이패스카드', 'BNK부산은행', '01', 'LOCAL',
    8000, 0, 0,
    19, '개인 및 기업회원', '하이패스 차로 통과시 후불방식으로 이용하고 결제일에 결제하는 후불 하이패스카드! 전국고속도로, 민자도로 등에 설치된 톨게이트의 하이패스 車路 통과시 후불방식으로 이용하고 신용카드 결제일에 결제하는 카드',
    'Y', 'Y', 'Y', 'PUBLISHED',
    SYSTIMESTAMP, SYSTIMESTAMP, 'N');

-- [16] 오늘은e 신용카드
INSERT INTO CARDS (card_id, card_code, card_type, card_name, company_name, company_code, brand_name,
    annual_fee_domestic, annual_fee_overseas, previous_month_spend,
    minimum_age, target_user, summary_description,
    searchable_yn, visible_yn, approval_required_yn, card_status,
    publish_start_at, created_at, deleted_yn)
VALUES (10101008, 'ONEULE_CREDIT', 'CREDIT', '오늘은e 신용카드', 'BNK부산은행', '01', 'LOCAL',
    10000, 10000, 300000,
    19, '개인회원(가족카드 발급불가)', '각종 페이 및 생활 서비스 할인되는 오늘은e 신용카드! 간편결제할인 서비스 : PAYCO, 삼성페이, 네이버페이, 카카오페이, 썸패스 5% 청구할인 생활할인 서비스 : 학원업종/이동통신/커피/대중교통/영화관 할인 서비스 ※ 전월실적 및 서비스 세부조건은 상품안내장 및 홈페이지 참고',
    'Y', 'Y', 'Y', 'PUBLISHED',
    SYSTIMESTAMP, SYSTIMESTAMP, 'N');

-- [17] BNK 부자되세요 홈쇼핑카드
INSERT INTO CARDS (card_id, card_code, card_type, card_name, company_name, company_code, brand_name,
    annual_fee_domestic, annual_fee_overseas, previous_month_spend,
    minimum_age, target_user, summary_description,
    searchable_yn, visible_yn, approval_required_yn, card_status,
    publish_start_at, created_at, deleted_yn)
VALUES (10101009, 'BNK_HOMESHOPPING', 'CREDIT', 'BNK 부자되세요 홈쇼핑카드', 'BNK부산은행', '01', 'LOCAL',
    15000, 20000, 300000,
    19, '민법상 성년인 만 19세 이상 개인 회원(본인 및 가족회원)', '한 장의 카드로 폭 넓게 즐기는 쇼핑 특화 카드 한 장의 카드로 다양한 쇼핑을 할인받는 홈쇼핑카드! 현명하게 선택하고 당당하게 사용하는 우리는 모두 꽤 멋진 부자입니다.',
    'Y', 'Y', 'Y', 'PUBLISHED',
    SYSTIMESTAMP, SYSTIMESTAMP, 'N');

-- [18] 부산체육사랑카드
INSERT INTO CARDS (card_id, card_code, card_type, card_name, company_name, company_code, brand_name,
    annual_fee_domestic, annual_fee_overseas, previous_month_spend,
    minimum_age, target_user, summary_description,
    searchable_yn, visible_yn, approval_required_yn, card_status,
    publish_start_at, created_at, deleted_yn)
VALUES (10101010, 'BUSAN_SPORTS', 'CREDIT', '부산체육사랑카드', 'BNK부산은행', '01', 'LOCAL',
    15000, 20000, 300000,
    19, '개인회원(가족카드 제외)', 'Sports is Busan!! 부산시체육회 지정 체육시설 10%, 월 최대 1만5천원 할인! 스포츠, 의료, 학원 등 생활 곳곳에서 할인 챙기세요!',
    'Y', 'Y', 'Y', 'PUBLISHED',
    SYSTIMESTAMP, SYSTIMESTAMP, 'N');

-- [19] 팟(pod) 카드
INSERT INTO CARDS (card_id, card_code, card_type, card_name, company_name, company_code, brand_name,
    annual_fee_domestic, annual_fee_overseas, previous_month_spend,
    minimum_age, target_user, summary_description,
    searchable_yn, visible_yn, approval_required_yn, card_status,
    publish_start_at, created_at, deleted_yn)
VALUES (10101011, 'POD_CARD', 'CREDIT', '팟(pod) 카드', 'BNK부산은행', '01', 'LOCAL',
    15000, 20000, 300000,
    19, '개인회원 ※가족카드 발급 불가', '내맘속에 팟! 팟카드로 다양한 콘텐츠를 즐기세요!',
    'Y', 'Y', 'Y', 'PUBLISHED',
    SYSTIMESTAMP, SYSTIMESTAMP, 'N');

-- [20] ZipL 신용카드
INSERT INTO CARDS (card_id, card_code, card_type, card_name, company_name, company_code, brand_name,
    annual_fee_domestic, annual_fee_overseas, previous_month_spend,
    minimum_age, target_user, summary_description,
    searchable_yn, visible_yn, approval_required_yn, card_status,
    publish_start_at, created_at, deleted_yn)
VALUES (10101012, 'ZIPL_CREDIT', 'CREDIT', 'ZipL 신용카드', 'BNK부산은행', '01', 'LOCAL',
    15000, 20000, 300000,
    19, '개인회원 ※가족카드 발급 불가', '생활에 특별한 혜택, 더 나은 일상을 위한 신용카드',
    'Y', 'Y', 'Y', 'PUBLISHED',
    SYSTIMESTAMP, SYSTIMESTAMP, 'N');

-- [21] SK OIL&LPG카드
INSERT INTO CARDS (card_id, card_code, card_type, card_name, company_name, company_code, brand_name,
    annual_fee_domestic, annual_fee_overseas, previous_month_spend,
    minimum_age, target_user, summary_description,
    searchable_yn, visible_yn, approval_required_yn, card_status,
    publish_start_at, created_at, deleted_yn)
VALUES (10101013, 'SK_OIL', 'CREDIT', 'SK OIL&LPG카드', 'BNK부산은행', '01', 'LOCAL',
    10000, 10000, 300000,
    19, '개인회원(가족카드 발급불가)', '주유특화 할인 혜택과 생활 서비스 할인까지 가능한 SK OIL&LPG카드! SK 주유소/충전소 할인 서비스, 생활 할인 서비스',
    'Y', 'Y', 'Y', 'PUBLISHED',
    SYSTIMESTAMP, SYSTIMESTAMP, 'N');

-- [22] BNK 프렌즈 신용카드
INSERT INTO CARDS (card_id, card_code, card_type, card_name, company_name, company_code, brand_name,
    annual_fee_domestic, annual_fee_overseas, previous_month_spend,
    minimum_age, target_user, summary_description,
    searchable_yn, visible_yn, approval_required_yn, card_status,
    publish_start_at, created_at, deleted_yn)
VALUES (10101014, 'BNK_FRIENDS', 'CREDIT', 'BNK 프렌즈 신용카드', 'BNK부산은행', '01', 'LOCAL',
    15000, 20000, 300000,
    19, '개인', '간단 명료한 기본할인! 통큰 연간 캐시백! 생활필수 할인!',
    'Y', 'Y', 'Y', 'PUBLISHED',
    SYSTIMESTAMP, SYSTIMESTAMP, 'N');

-- [23] 딩딩 신용카드
INSERT INTO CARDS (card_id, card_code, card_type, card_name, company_name, company_code, brand_name,
    annual_fee_domestic, annual_fee_overseas, previous_month_spend,
    minimum_age, target_user, summary_description,
    searchable_yn, visible_yn, approval_required_yn, card_status,
    publish_start_at, created_at, deleted_yn)
VALUES (10101015, 'DINGDING_CREDIT', 'CREDIT', '딩딩 신용카드', 'BNK부산은행', '01', 'LOCAL',
    10000, 10000, 300000,
    19, '개인', '즐거움 가득, 혜택 가득~ DingDing 신용카드!',
    'Y', 'Y', 'Y', 'PUBLISHED',
    SYSTIMESTAMP, SYSTIMESTAMP, 'N');

-- [24] 팜코카드
INSERT INTO CARDS (card_id, card_code, card_type, card_name, company_name, company_code, brand_name,
    annual_fee_domestic, annual_fee_overseas, previous_month_spend,
    minimum_age, target_user, summary_description,
    searchable_yn, visible_yn, approval_required_yn, card_status,
    publish_start_at, created_at, deleted_yn)
VALUES (10101016, 'FARMCO', 'CREDIT', '팜코카드', 'BNK부산은행', '01', 'LOCAL',
    0, 0, 0,
    19, '약사 및 병원 개인 사업자, 법인사업자', '의약품 구입대금 결제전용 기업카드',
    'Y', 'Y', 'Y', 'PUBLISHED',
    SYSTIMESTAMP, SYSTIMESTAMP, 'N');

-- [25] 부산 동백전 선불카드
INSERT INTO CARDS (card_id, card_code, card_type, card_name, company_name, company_code, brand_name,
    annual_fee_domestic, annual_fee_overseas, previous_month_spend,
    minimum_age, target_user, summary_description,
    searchable_yn, visible_yn, approval_required_yn, card_status,
    publish_start_at, created_at, deleted_yn)
VALUES (10301001, 'DONGBAEK_PREPAID', 'PREPAID', '부산 동백전 선불카드', 'BNK부산은행', '01', 'LOCAL',
    0, 0, 0,
    14, '개인', '부산지역 경제활성화를 위한, 부산지역화폐 동백전! 지역자금의 역외유출 방지를 위해 부산시 내 사용가능하며 지역 내 소상공인 매출 증대를 통한 지역경기 활성화에 기여',
    'Y', 'Y', 'Y', 'PUBLISHED',
    SYSTIMESTAMP, SYSTIMESTAMP, 'N');

-- [26] BNK SIMPLE AMEX BLUE BUSINESS 카드
INSERT INTO CARDS (card_id, card_code, card_type, card_name, company_name, company_code, brand_name,
    annual_fee_domestic, annual_fee_overseas, previous_month_spend,
    minimum_age, target_user, summary_description,
    searchable_yn, visible_yn, approval_required_yn, card_status,
    publish_start_at, created_at, deleted_yn)
VALUES (10101017, 'BNK_AMEX_SIMPLE', 'CREDIT', 'BNK SIMPLE AMEX BLUE BUSINESS 카드', 'BNK부산은행', '01', 'AMEX',
    30000, 30000, 300000,
    19, '개인사업자/법인', '하나의 카드로 사업을 심플하게! BNK Simple 카드',
    'Y', 'Y', 'Y', 'PUBLISHED',
    SYSTIMESTAMP, SYSTIMESTAMP, 'N');

-- [27] BNK Simple카드
INSERT INTO CARDS (card_id, card_code, card_type, card_name, company_name, company_code, brand_name,
    annual_fee_domestic, annual_fee_overseas, previous_month_spend,
    minimum_age, target_user, summary_description,
    searchable_yn, visible_yn, approval_required_yn, card_status,
    publish_start_at, created_at, deleted_yn)
VALUES (10101018, 'BNK_SIMPLE', 'CREDIT', 'BNK Simple카드', 'BNK부산은행', '01', 'LOCAL',
    15000, 20000, 300000,
    19, '개인사업자, 법인', '포인트 적립의 Simple한 상품서비스에 지역사회 공헌하는 ESG 상품 하나의 카드로 사업을 심플하게..! BNK Simple 카드',
    'Y', 'Y', 'Y', 'PUBLISHED',
    SYSTIMESTAMP, SYSTIMESTAMP, 'N');

COMMIT;

-- ==============================================================

COMMIT;


-- ==============================================================
-- 16. CARD_BENEFITS (129건 — card_id 재매핑)
-- ==============================================================
-- 10201001. CARD_BENEFITS (혜택 상세)
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

COMMIT;


-- ==============================================================
-- 17. CARD_CONTENTS (54건 — card_id 재매핑)
-- ==============================================================
-- 10101003. CARD_CONTENTS (상품 소개 콘텐츠)
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

COMMIT;


-- ==============================================================
-- 18. CARD_ATTRIBUTE_DEFINITIONS (10건 — 변경 없음)
-- ==============================================================
-- 5. CARD_ATTRIBUTE_DEFINITIONS (속성 정의)
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

COMMIT;


-- ==============================================================
-- 19. CARD_ATTRIBUTE_VALUES (260건 — card_id 재매핑)
-- ==============================================================
-- 10201003. CARD_ATTRIBUTE_VALUES (카드별 속성 값)
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

COMMIT;


-- ==============================================================
-- 20. CARD_TAGS (25건 — 변경 없음)
-- ==============================================================
-- 7. CARD_TAGS (카드 태그)
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

COMMIT;


-- ==============================================================
-- 21. CARD_TAG_MAP (117건 — card_id 재매핑)
-- ==============================================================
-- 10201005. CARD_TAG_MAP (카드-태그 연결)
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

COMMIT;


-- ==============================================================
-- 22. CARD_STATUS_HISTORIES (27건 — card_id 재매핑)
-- ==============================================================
-- 10201006. CARD_STATUS_HISTORIES (초기 상태 이력: PUBLISHED)
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


-- ============================================================
-- 전체 INSERT 완료
-- ============================================================

COMMIT;


-- ==============================================================
-- 23. TERMS_GROUPS (4건)
-- ==============================================================
-- 1. TERMS_GROUPS
-- ==============================================================
INSERT INTO TERMS_GROUPS (group_name, group_type, display_order, created_at)
VALUES ('필수 동의 약관', 'REQUIRED', 1, SYSTIMESTAMP);

INSERT INTO TERMS_GROUPS (group_name, group_type, display_order, created_at)
VALUES ('선택 동의 약관', 'OPTIONAL', 2, SYSTIMESTAMP);

INSERT INTO TERMS_GROUPS (group_name, group_type, display_order, created_at)
VALUES ('카드 서비스 약관', 'REQUIRED', 3, SYSTIMESTAMP);

INSERT INTO TERMS_GROUPS (group_name, group_type, display_order, created_at)
VALUES ('카드 상품 안내', 'NOTICE', 4, SYSTIMESTAMP);

COMMIT;


-- ==============================================================


-- ==============================================================
-- 24. TERMS_MASTERS (36건)
-- ==============================================================
-- 2. TERMS_MASTERS
-- --------------------------------------------------------------
-- [공통 약관: terms_master_id 1~11]
--   1  회원약관 (회원가입 필수)
--   2  개인정보처리취급방침 (회원가입 필수)
--   3  신용카드 개인회원 표준약관
--   4  신용카드 개인회원 부속약관
--   5  개인신용평점 하락 가능성 설명 확인서
--   6  포인트이용약관
--   7  비씨체크(플러스)카드 개인회원약관
--   8  신용카드 기업회원 약관
--   9  신용카드 법인회원 연회비 부과 표준약관
--  10  국민행복카드 약관
--  11  선불카드 표준약관
--
-- [카드별 안내장: terms_master_id 101~127]
--  101  REX2 가이드북
--  102  2030 언택트 체크카드 안내장
--  103  동백전 선불카드 안내장
--  104  빵빵체크카드 안내장
--  105  SOHO-BIZ카드 안내장
--  106  오늘은e 신용카드 안내장
--  107  BNK 프렌즈 신용카드 안내장
--  108  부산동백전 체크카드 안내장
--  109  캐시백 리플렛
--  110  딩딩 신용카드 안내장
--  111  후불하이패스 안내장
--  112  딩딩체크카드 리플렛
--  113  부자되세요 홈쇼핑카드 리플렛
--  114  팜코카드 안내장
--  115  오늘은e 체크카드 안내장
--  116  SK OIL&LPG카드 안내장
--  117  팟(pod)카드 안내장
--  118  ZipL 신용카드 안내장
--  119  ZipL 체크카드 안내장
--  120  BNK Simple카드 안내장
--  121  부산체육사랑카드 안내장
--  122  어디로든 그린체크카드 안내장
--  123  아맥스익스프레스(AMEX) 안내장
--  124  국민행복카드 안내장 (신용)
--  125  국민행복체크카드 안내장
--  126  후불 하이패스카드(기업) 안내장
--  127  팜코카드 안내장 (기업용, 124와 동일 파일)
-- ==============================================================

-- ── 공통 약관 ──────────────────────────────────────────────────
INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (1, 'COMMON', '회원약관', 'BNK부산은행 금융상품 플랫폼 회원가입 기본 약관', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (2, 'PRIVACY', '개인정보처리취급방침', '서비스 이용 시 수집·이용하는 개인정보 처리 방침', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (3, 'CARD_SERVICE', '신용카드 개인회원 표준약관', '금융감독원 표준약관 기반 신용카드 개인회원 이용 약관', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (4, 'CARD_SERVICE', '신용카드 개인회원 부속약관', '신용카드 개인회원 표준약관에 대한 부속 약관 (2021.4.6 개정)', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (5, 'CARD_SERVICE', '개인신용평점 하락 가능성 등에 대한 설명 확인서', '신용카드 발급 시 개인신용평점 하락 가능성 안내 및 확인', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (6, 'COMMON', '포인트이용약관', 'BNK 포인트 적립·사용·소멸에 관한 이용 약관', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (7, 'CARD_SERVICE', '비씨체크(플러스)카드 개인회원 약관', 'BC카드 체크카드 개인회원 이용 약관', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (8, 'CARD_SERVICE', '신용카드 기업회원 약관', '기업·법인 대상 신용카드 이용 약관 전문', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (9, 'CARD_SERVICE', '신용카드 법인회원 연회비 부과 등에 관한 표준약관', '법인 신용카드 연회비 부과 기준 및 환불에 관한 표준약관', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (10, 'CARD_SERVICE', '국민행복카드 약관', '국민행복카드 발급 및 이용에 관한 약관 (개정본)', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (11, 'CARD_SERVICE', '선불카드 표준약관', '선불카드 발급·충전·사용에 관한 표준약관 (개정안)', SYSTIMESTAMP);

-- ── 카드별 안내장 ────────────────────────────────────────────────
INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (101, 'CARD_SERVICE', 'REX2 카드 가이드북', 'REX2 포인트형/마일리지형 공통 안내장 (2025.12.12)', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (102, 'CARD_SERVICE', '2030 언택트 체크카드 안내장', '2030 언택트 체크카드 상품 안내장 (2025.02.25)', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (103, 'CARD_SERVICE', '동백전 선불카드 안내장', '부산 동백전 선불카드 상품 안내장 (2024.02.19)', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (104, 'CARD_SERVICE', '빵빵체크카드 안내장', '빵빵체크카드 상품 안내장 (QR코드 버전)', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (105, 'CARD_SERVICE', 'SOHO-BIZ카드 안내장', 'SOHO-BIZ카드 상품 안내장 (2026.02.12)', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (106, 'CARD_SERVICE', '오늘은e 신용카드 안내장', '오늘은e 신용카드 상품 안내장 (2025.12.12)', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (107, 'CARD_SERVICE', 'BNK 프렌즈 신용카드 안내장', 'BNK 프렌즈 신용카드 상품 안내장 (2025.12.12)', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (108, 'CARD_SERVICE', '부산동백전 체크카드 안내장', '부산동백전 체크카드 상품 리플렛 (2025.12.12)', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (109, 'CARD_SERVICE', '캐시백카드 안내장', '캐시백카드 상품 리플렛 (2025.12.12)', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (110, 'CARD_SERVICE', '딩딩 신용카드 안내장', '딩딩 신용카드 상품 안내장 (2025.12.12)', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (111, 'CARD_SERVICE', '후불 하이패스카드 안내장', '후불 하이패스카드(개인/기업 공통) 상품 안내장 (2025.12.12)', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (112, 'CARD_SERVICE', '딩딩 체크카드 안내장', '딩딩 체크카드 상품 리플렛 (2025.12.12)', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (113, 'CARD_SERVICE', 'BNK 부자되세요 홈쇼핑카드 안내장', 'BNK 부자되세요 홈쇼핑카드 상품 리플렛 (2025.12.12)', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (114, 'CARD_SERVICE', '팜코카드 안내장', '팜코카드 상품 안내장 (2025.12.12)', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (115, 'CARD_SERVICE', '오늘은e 체크카드 안내장', '오늘은e 체크카드 상품 안내장 (2025.12.12)', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (116, 'CARD_SERVICE', 'SK OIL&LPG카드 안내장', 'SK OIL&LPG카드 상품 안내장 (2025.12.12)', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (117, 'CARD_SERVICE', '팟(pod)카드 안내장', '팟(pod)카드 상품 안내장 (2026.05.11)', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (118, 'CARD_SERVICE', 'ZipL 신용카드 안내장', 'ZipL 신용카드 상품 안내장 (2026.02.12)', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (119, 'CARD_SERVICE', 'ZipL 체크카드 안내장', 'ZipL 체크카드 상품 안내장 (2025.12.12)', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (120, 'CARD_SERVICE', 'BNK Simple카드 안내장', 'BNK Simple카드 상품 안내장 (2025.12.12)', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (121, 'CARD_SERVICE', '부산체육사랑카드 안내장', '부산체육사랑카드 상품 안내장 (2025.12.12)', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (122, 'CARD_SERVICE', '어디로든 그린체크카드 안내장', '어디로든 그린체크카드 상품 안내장', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (123, 'CARD_SERVICE', 'BNK SIMPLE AMEX BLUE BUSINESS카드 안내장', 'BNK SIMPLE AMEX BLUE BUSINESS카드 상품 안내장 (2025.12.12)', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (124, 'CARD_SERVICE', '국민행복카드(신용) 안내장', '국민행복카드 신용형 상품 안내 (약관 포함)', SYSTIMESTAMP);

INSERT INTO TERMS_MASTERS (terms_master_id, terms_type, title, description, created_at)
VALUES (125, 'CARD_SERVICE', '국민행복체크카드 안내장', '국민행복카드 체크형 상품 안내 (약관 포함)', SYSTIMESTAMP);

COMMIT;


-- ==============================================================


-- ==============================================================
-- 25. TERMS (36건 — v1.0 PUBLISHED)
-- ==============================================================
-- 공통 약관 11건
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (1, 1, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (2, 2, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (3, 3, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (4, 4, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (5, 5, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (6, 6, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (7, 7, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (8, 8, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (9, 9, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (10, 10, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (11, 11, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);

-- 카드별 안내장 25건
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (101, 101, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (102, 102, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (103, 103, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (104, 104, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (105, 105, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (106, 106, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (107, 107, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (108, 108, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (109, 109, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (110, 110, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (111, 111, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (112, 112, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (113, 113, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (114, 114, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (115, 115, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (116, 116, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (117, 117, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (118, 118, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (119, 119, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (120, 120, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (121, 121, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (122, 122, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (123, 123, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (124, 124, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);
INSERT INTO TERMS (terms_id, terms_master_id, version, status, effective_from, approved_by, approved_at, created_at)
VALUES (125, 125, 'v1.0', 'PUBLISHED', TO_DATE('2025-01-01','YYYY-MM-DD'), 1, SYSTIMESTAMP, SYSTIMESTAMP);

COMMIT;

-- ==============================================================
-- 26. TERMS_PACKAGES (2건)
-- ==============================================================
-- 4. TERMS_PACKAGES
-- ==============================================================
INSERT INTO TERMS_PACKAGES (package_name, package_type, description, created_at)
VALUES ('회원가입 약관 패키지', 'SIGNUP', '회원 가입 시 동의해야 하는 약관 묶음 (회원약관 + 개인정보처리방침)', SYSTIMESTAMP);

INSERT INTO TERMS_PACKAGES (package_name, package_type, description, created_at)
VALUES ('카드 신청 약관 패키지', 'CARD_APPLY', '카드 신청 시 카드별 약관은 CARD_TERMS 참조', SYSTIMESTAMP);

COMMIT;


-- ==============================================================


-- ==============================================================
-- 27. PACKAGE_TERMS (회원가입 패키지 연결)
-- ==============================================================
-- 5. PACKAGE_TERMS (회원가입 패키지)
-- ==============================================================
INSERT INTO PACKAGE_TERMS (package_id, terms_id, display_order, created_at)
VALUES (1, 1, 1, SYSTIMESTAMP);   -- 회원약관 (필수)

INSERT INTO PACKAGE_TERMS (package_id, terms_id, display_order, created_at)
VALUES (1, 2, 2, SYSTIMESTAMP);   -- 개인정보처리취급방침 (필수)

COMMIT;


-- ==============================================================


-- ==============================================================
-- 28. TERMS_STATUS_HISTORY (36건)
-- ==============================================================
-- 6. TERMS_STATUS_HISTORY (DRAFT → PUBLISHED)
-- ==============================================================
-- 공통 약관 11건
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (1,   'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (2,   'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (3,   'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (4,   'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (5,   'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (6,   'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (7,   'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (8,   'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (9,   'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (10,  'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (11,  'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
-- 카드별 안내장 25건
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (101, 'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (102, 'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (103, 'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (104, 'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (105, 'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (106, 'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (107, 'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (108, 'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (109, 'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (110, 'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (111, 'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (112, 'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (113, 'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (114, 'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (115, 'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (116, 'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (117, 'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (118, 'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (119, 'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (120, 'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (121, 'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (122, 'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (123, 'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (124, 'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);
INSERT INTO TERMS_STATUS_HISTORY (terms_id, previous_status, changed_status, changed_by, changed_reason, changed_at) VALUES (125, 'DRAFT', 'PUBLISHED', 1, '초기 시스템 오픈 배포', SYSTIMESTAMP);

COMMIT;


-- ==============================================================


-- ==============================================================
-- 29. CARD_TERMS (113건 — card_id 재매핑)
-- ==============================================================
-- 10201004. CARD_TERMS (카드-약관 매핑)
-- --------------------------------------------------------------
-- card_id 매핑:
--  10101001  REX10101002_포인트형(개인)
--  10101002  REX10101002_대한항공마일리지형(개인)
--  10201001  빵빵체크카드
--  10101003  캐쉬백카드
--  10201002  국민행복체크카드
--  10201003  부산 동백전 체크카드
--  10201004  딩딩 체크카드
--  10201005  어디로든 그린체크카드
--  10201006  2030 언택트 체크카드
-- 10201007  오늘은e 체크카드
-- 10201008  ZipL 체크카드
-- 10101004  후불 하이패스카드(기업)
-- 10101005  SOHO-BIZ카드
-- 10101006  국민행복카드(신용)
-- 10101007  후불 하이패스카드(개인)
-- 10101008  오늘은e 신용카드
-- 10101009  BNK 부자되세요 홈쇼핑카드
-- 10101010  부산체육사랑카드
-- 10101011  팟(pod) 카드
-- 10101012  ZipL 신용카드
-- 10101013  SK OIL&LPG카드
-- 10101014  BNK 프렌즈 신용카드
-- 10101015  딩딩 신용카드
-- 10101016  팜코카드
-- 10301001  부산 동백전 선불카드
-- 10101017  BNK SIMPLE AMEX BLUE BUSINESS 카드
-- 10101018  BNK Simple카드
--
-- group_id: 10101001=필수약관, 10201001=카드서비스약관, 10101003=카드안내장
-- ==============================================================

-- ──────────────────────────────────────────────
-- [01] REX10101002_포인트형(개인)  card_id=10101001
--  안내장(101) + 개인표준(10201001) + 개인부속(10101003) + 신용평점(10201002) + 포인트(10201003)
-- ──────────────────────────────────────────────
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
-- 완료
-- --------------------------------------------------------------
-- ■ 실행 순서 (FK 의존성 고려):
--   10101001. busanbank_dummy_data.sql   (공통코드, 관리자, 회원 등)
--   10101002. busanbank_cards_full_insert.sql   (카드 상품)
--   10201001. busanbank_terms_insert.sql  ← 이 파일
--
-- ■ content_html 채우기 (PDF/TXT 수신 후):
--   UPDATE TERMS
--   SET    content_html  = '[변환된 HTML]',
--          document_hash = '[SHA-256 해시]',
--          updated_at    = SYSTIMESTAMP
--   WHERE  terms_id = [10101001~10201008, 101~125];
--   COMMIT;
--
-- ■ 약관별 소스 파일 요약:
--   terms_id  10101001  ← 회원약관.txt
--   terms_id  10101002  ← 개인정보처리취급방침.txt
--   terms_id  10201001  ← 신용카드 개인회원 표준약관 개정(전문).pdf
--   terms_id  10101003  ← 신용카드 개인회원 부속약관 개정 전문(10101013.10101003.10201003).pdf
--   terms_id  10201002  ← 개인신용평점 하락 가능성 등에 대한 설명 확인서.pdf
--   terms_id  10201003  ← 포인트이용약관(10201003).pdf
--   terms_id  10201004  ← 비씨체크(플러스)카드개인회원약관.pdf
--   terms_id  10201005  ← 신용카드 기업회원 약관 (전문).pdf
--   terms_id  10201006  ← 신용카드_법인회원_연회비_부과_등에_관한_표준약관.pdf
--   terms_id 10201007  ← 10101001. (개정후)국민행복카드 약관.pdf
--   terms_id 10201008  ← 선불카드 표준약관 개정(안) 전문.pdf
--   terms_id 101 ← 84_10101001_렉스10101002 가이드북(100x210)_251212.pdf
--   terms_id 102 ← (02)2030 언택트 체크카드 안내장(2025.02.10301001).pdf
--   terms_id 103 ← (52)동백전선불카드안내장(2024.02.10101011).pdf
--   terms_id 104 ← (88)빵빵체크카드안내장(QR코드있는버전).pdf
--   terms_id 105 ← 01_SOHO-BIZ카드안내장(2026.02.10101004).pdf
--   terms_id 106 ← 07_오늘은e신용.pdf
--   terms_id 107 ← 09_BNK프렌즈신용카드_251212.pdf
--   terms_id 108 ← 10201008_부산동백전 체크카드리플렛_251212.pdf
--   terms_id 109 ← 10101008_캐시백리플렛_251212.pdf
--   terms_id 110 ← 10101013_딩딩신용카드_251212.pdf
--   terms_id 111 ← 10101014_후불하이패스_251212.pdf  (개인/기업 공통)
--   terms_id 112 ← 10101016_딩딩체크카드 리플렛_251212.pdf
--   terms_id 113 ← 10101017_부자되세요 홈쇼핑카드251212.pdf
--   terms_id 114 ← 47_팜코카드_251212.pdf
--   terms_id 115 ← 48_오늘은e체크_251212.pdf
--   terms_id 116 ← 49_SK OIL LPG카드_251212.pdf
--   terms_id 117 ← 49_팟카드 안내장(페이지분할)_확인용_260511.pdf
--   terms_id 118 ← 57_zipl신용카드_안내장(2026.02.10101004).pdf
--   terms_id 119 ← 57_zipl체크카드_251212.pdf
--   terms_id 120 ← 58_simple카드_251212.pdf
--   terms_id 121 ← 74_부산체육사랑카드 안내장__251212.pdf
--   terms_id 122 ← 77_ 어디로든 그린체크카드.pdf
--   terms_id 123 ← 82_아맥스익스프레스_251212.pdf
--   terms_id 124 ← 10101001. (개정후)국민행복카드 약관.pdf  (신용형 안내)
--   terms_id 125 ← 10101001. (개정후)국민행복카드 약관.pdf  (체크형 안내)
-- ==============================================================

COMMIT;


-- ==============================================================
-- ==============================================================
-- 30. SEARCH_KEYWORDS (30건)
-- ==============================================================
INSERT INTO SEARCH_KEYWORDS (keyword, use_yn, display_order, created_at, created_by, deleted_yn) VALUES ('주유',         'Y', 1,  SYSTIMESTAMP, 1, 'N');
INSERT INTO SEARCH_KEYWORDS (keyword, use_yn, display_order, created_at, created_by, deleted_yn) VALUES ('체크카드',     'Y', 2,  SYSTIMESTAMP, 1, 'N');
INSERT INTO SEARCH_KEYWORDS (keyword, use_yn, display_order, created_at, created_by, deleted_yn) VALUES ('신용카드',     'Y', 3,  SYSTIMESTAMP, 1, 'N');
INSERT INTO SEARCH_KEYWORDS (keyword, use_yn, display_order, created_at, created_by, deleted_yn) VALUES ('캐시백',       'Y', 4,  SYSTIMESTAMP, 1, 'N');
INSERT INTO SEARCH_KEYWORDS (keyword, use_yn, display_order, created_at, created_by, deleted_yn) VALUES ('포인트',       'Y', 5,  SYSTIMESTAMP, 1, 'N');
INSERT INTO SEARCH_KEYWORDS (keyword, use_yn, display_order, created_at, created_by, deleted_yn) VALUES ('여행',         'Y', 6,  SYSTIMESTAMP, 1, 'N');
INSERT INTO SEARCH_KEYWORDS (keyword, use_yn, display_order, created_at, created_by, deleted_yn) VALUES ('마일리지',     'Y', 7,  SYSTIMESTAMP, 1, 'N');
INSERT INTO SEARCH_KEYWORDS (keyword, use_yn, display_order, created_at, created_by, deleted_yn) VALUES ('연회비없음',   'Y', 8,  SYSTIMESTAMP, 1, 'N');
INSERT INTO SEARCH_KEYWORDS (keyword, use_yn, display_order, created_at, created_by, deleted_yn) VALUES ('쇼핑',         'Y', 9,  SYSTIMESTAMP, 1, 'N');
INSERT INTO SEARCH_KEYWORDS (keyword, use_yn, display_order, created_at, created_by, deleted_yn) VALUES ('교통',         'Y', 10, SYSTIMESTAMP, 1, 'N');
INSERT INTO SEARCH_KEYWORDS (keyword, use_yn, display_order, created_at, created_by, deleted_yn) VALUES ('대중교통',     'Y', 11, SYSTIMESTAMP, 1, 'N');
INSERT INTO SEARCH_KEYWORDS (keyword, use_yn, display_order, created_at, created_by, deleted_yn) VALUES ('커피',         'Y', 12, SYSTIMESTAMP, 1, 'N');
INSERT INTO SEARCH_KEYWORDS (keyword, use_yn, display_order, created_at, created_by, deleted_yn) VALUES ('배달',         'Y', 13, SYSTIMESTAMP, 1, 'N');
INSERT INTO SEARCH_KEYWORDS (keyword, use_yn, display_order, created_at, created_by, deleted_yn) VALUES ('동백전',       'Y', 14, SYSTIMESTAMP, 1, 'N');
INSERT INTO SEARCH_KEYWORDS (keyword, use_yn, display_order, created_at, created_by, deleted_yn) VALUES ('지역화폐',     'Y', 15, SYSTIMESTAMP, 1, 'N');
INSERT INTO SEARCH_KEYWORDS (keyword, use_yn, display_order, created_at, created_by, deleted_yn) VALUES ('의료',         'Y', 16, SYSTIMESTAMP, 1, 'N');
INSERT INTO SEARCH_KEYWORDS (keyword, use_yn, display_order, created_at, created_by, deleted_yn) VALUES ('영화',         'Y', 17, SYSTIMESTAMP, 1, 'N');
INSERT INTO SEARCH_KEYWORDS (keyword, use_yn, display_order, created_at, created_by, deleted_yn) VALUES ('통신할인',     'Y', 18, SYSTIMESTAMP, 1, 'N');
INSERT INTO SEARCH_KEYWORDS (keyword, use_yn, display_order, created_at, created_by, deleted_yn) VALUES ('편의점',       'Y', 19, SYSTIMESTAMP, 1, 'N');
INSERT INTO SEARCH_KEYWORDS (keyword, use_yn, display_order, created_at, created_by, deleted_yn) VALUES ('하이패스',     'Y', 20, SYSTIMESTAMP, 1, 'N');
INSERT INTO SEARCH_KEYWORDS (keyword, use_yn, display_order, created_at, created_by, deleted_yn) VALUES ('홈쇼핑',       'Y', 21, SYSTIMESTAMP, 1, 'N');
INSERT INTO SEARCH_KEYWORDS (keyword, use_yn, display_order, created_at, created_by, deleted_yn) VALUES ('스포츠',       'Y', 22, SYSTIMESTAMP, 1, 'N');
INSERT INTO SEARCH_KEYWORDS (keyword, use_yn, display_order, created_at, created_by, deleted_yn) VALUES ('기업카드',     'Y', 23, SYSTIMESTAMP, 1, 'N');
INSERT INTO SEARCH_KEYWORDS (keyword, use_yn, display_order, created_at, created_by, deleted_yn) VALUES ('학원',         'Y', 24, SYSTIMESTAMP, 1, 'N');
INSERT INTO SEARCH_KEYWORDS (keyword, use_yn, display_order, created_at, created_by, deleted_yn) VALUES ('마트',         'Y', 25, SYSTIMESTAMP, 1, 'N');
INSERT INTO SEARCH_KEYWORDS (keyword, use_yn, display_order, created_at, created_by, deleted_yn) VALUES ('선불카드',     'Y', 26, SYSTIMESTAMP, 1, 'N');
INSERT INTO SEARCH_KEYWORDS (keyword, use_yn, display_order, created_at, created_by, deleted_yn) VALUES ('보험',         'Y', 27, SYSTIMESTAMP, 1, 'N');
INSERT INTO SEARCH_KEYWORDS (keyword, use_yn, display_order, created_at, created_by, deleted_yn) VALUES ('친환경',       'Y', 28, SYSTIMESTAMP, 1, 'N');
INSERT INTO SEARCH_KEYWORDS (keyword, use_yn, display_order, created_at, created_by, deleted_yn) VALUES ('REX2',         'Y', 29, SYSTIMESTAMP, 1, 'N');
INSERT INTO SEARCH_KEYWORDS (keyword, use_yn, display_order, created_at, created_by, deleted_yn) VALUES ('소호',         'Y', 30, SYSTIMESTAMP, 1, 'N');

COMMIT;


-- ==============================================================
-- ==============================================================
-- 31. CARD_KEYWORDS (카드별 키워드 매핑)
-- ==============================================================
-- REX2 포인트/마일리지형 (10101001, 10101002) → 여행(6), 마일리지(7), 포인트(5)
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

COMMIT;



-- ==============================================================
-- ==============================================================
-- 32. SEARCH_LOGS (15건)
-- ==============================================================
INSERT INTO SEARCH_LOGS (user_id, keyword_raw, matched_keyword_id, result_count, search_at, ip_address) VALUES (1,  '주유카드',     1,  5,  SYSTIMESTAMP - INTERVAL '7' DAY, '211.234.12.10');
INSERT INTO SEARCH_LOGS (user_id, keyword_raw, matched_keyword_id, result_count, search_at, ip_address) VALUES (2,  '체크카드',     2,  8,  SYSTIMESTAMP - INTERVAL '6' DAY, '112.175.33.22');
INSERT INTO SEARCH_LOGS (user_id, keyword_raw, matched_keyword_id, result_count, search_at, ip_address) VALUES (3,  '여행카드',     6,  3,  SYSTIMESTAMP - INTERVAL '6' DAY, '59.8.45.111');
INSERT INTO SEARCH_LOGS (user_id, keyword_raw, matched_keyword_id, result_count, search_at, ip_address) VALUES (4,  '마일리지',     7,  2,  SYSTIMESTAMP - INTERVAL '5' DAY, '1.227.84.51');
INSERT INTO SEARCH_LOGS (user_id, keyword_raw, matched_keyword_id, result_count, search_at, ip_address) VALUES (5,  '동백전',       14, 2,  SYSTIMESTAMP - INTERVAL '5' DAY, '39.118.24.55');
INSERT INTO SEARCH_LOGS (user_id, keyword_raw, matched_keyword_id, result_count, search_at, ip_address) VALUES (1,  '캐시백',       4,  4,  SYSTIMESTAMP - INTERVAL '4' DAY, '211.234.12.10');
INSERT INTO SEARCH_LOGS (user_id, keyword_raw, matched_keyword_id, result_count, search_at, ip_address) VALUES (6,  '교통할인',     10, 3,  SYSTIMESTAMP - INTERVAL '4' DAY, '118.37.9.200');
INSERT INTO SEARCH_LOGS (user_id, keyword_raw, matched_keyword_id, result_count, search_at, ip_address) VALUES (7,  '포인트카드',   5,  6,  SYSTIMESTAMP - INTERVAL '3' DAY, '218.38.71.99');
INSERT INTO SEARCH_LOGS (user_id, keyword_raw, matched_keyword_id, result_count, search_at, ip_address) VALUES (NULL, '소호카드',    30, 2,  SYSTIMESTAMP - INTERVAL '3' DAY, '59.8.45.220');
INSERT INTO SEARCH_LOGS (user_id, keyword_raw, matched_keyword_id, result_count, search_at, ip_address) VALUES (8,  '선불카드',     26, 1,  SYSTIMESTAMP - INTERVAL '2' DAY, '222.100.5.88');
INSERT INTO SEARCH_LOGS (user_id, keyword_raw, matched_keyword_id, result_count, search_at, ip_address) VALUES (9,  '신용카드',     3,  18, SYSTIMESTAMP - INTERVAL '2' DAY, '175.209.25.14');
INSERT INTO SEARCH_LOGS (user_id, keyword_raw, matched_keyword_id, result_count, search_at, ip_address) VALUES (10, '하이패스',     20, 2,  SYSTIMESTAMP - INTERVAL '1' DAY, '121.66.32.7');
INSERT INTO SEARCH_LOGS (user_id, keyword_raw, matched_keyword_id, result_count, search_at, ip_address) VALUES (NULL, 'REX카드',    NULL, 0,  SYSTIMESTAMP - INTERVAL '1' DAY, '1.1.1.1');
INSERT INTO SEARCH_LOGS (user_id, keyword_raw, matched_keyword_id, result_count, search_at, ip_address) VALUES (3,  '편의점카드',   19, 1,  SYSTIMESTAMP - INTERVAL '1' DAY, '59.8.45.111');
INSERT INTO SEARCH_LOGS (user_id, keyword_raw, matched_keyword_id, result_count, search_at, ip_address) VALUES (4,  '홈쇼핑',       21, 1,  SYSTIMESTAMP,                   '1.227.84.51');

COMMIT;


-- ==============================================================
-- ==============================================================
-- 33. USER_SPENDING_PATTERNS (45건)
-- ==============================================================
-- user_id 1 (김민준)
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (1, 2,  150000, 'MANUAL', SYSDATE);
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (1, 4,  80000,  'MANUAL', SYSDATE);
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (1, 5,  50000,  'MANUAL', SYSDATE);
-- user_id 2 (이소연)
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (2, 3,  200000, 'MANUAL', SYSDATE);
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (2, 9,  30000,  'MANUAL', SYSDATE);
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (2, 2,  100000, 'MANUAL', SYSDATE);
-- user_id 3 (박준호)
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (3, 10, 80000,  'MANUAL', SYSDATE);
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (3, 9,  15000,  'MANUAL', SYSDATE);
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (3, 5,  30000,  'MANUAL', SYSDATE);
-- user_id 4 (최은지)
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (4, 3,  300000, 'MANUAL', SYSDATE);
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (4, 1,  500000, 'MANUAL', SYSDATE);
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (4, 2,  120000, 'MANUAL', SYSDATE);
-- user_id 5 (정현우)
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (5, 4,  200000, 'MANUAL', SYSDATE);
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (5, 2,  300000, 'MANUAL', SYSDATE);
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (5, 22, 150000, 'MANUAL', SYSDATE);
-- user_id 6 (한지영)
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (6, 7,  100000, 'MANUAL', SYSDATE);
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (6, 11, 200000, 'MANUAL', SYSDATE);
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (6, 2,  80000,  'MANUAL', SYSDATE);
-- user_id 7 (윤승호)
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (7, 9,  50000,  'MANUAL', SYSDATE);
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (7, 6,  200000, 'MANUAL', SYSDATE);
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (7, 2,  100000, 'MANUAL', SYSDATE);
-- user_id 8 (임채연)
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (8, 10, 60000,  'MANUAL', SYSDATE);
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (8, 16, 30000,  'MANUAL', SYSDATE);
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (8, 5,  20000,  'MANUAL', SYSDATE);
-- user_id 9 (신동현)
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (9, 4,  300000, 'MANUAL', SYSDATE);
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (9, 17, 100000, 'MANUAL', SYSDATE);
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (9, 2,  200000, 'MANUAL', SYSDATE);
-- user_id 10 (권민지)
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (10, 3,  150000, 'MANUAL', SYSDATE);
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (10, 5,  30000,  'MANUAL', SYSDATE);
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (10, 8,  50000,  'MANUAL', SYSDATE);
-- user_id 11 (오상훈)
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (11, 4,  250000, 'MANUAL', SYSDATE);
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (11, 22, 200000, 'MANUAL', SYSDATE);
-- user_id 12 (서지은)
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (12, 12, 200000, 'MANUAL', SYSDATE);
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (12, 7,  80000,  'MANUAL', SYSDATE);
-- user_id 13 (배민석)
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (13, 1,  400000, 'MANUAL', SYSDATE);
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (13, 4,  100000, 'MANUAL', SYSDATE);
-- user_id 14 (장혜진)
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (14, 1,  600000, 'MANUAL', SYSDATE);
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (14, 3,  400000, 'MANUAL', SYSDATE);
-- user_id 15 (노지호)
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (15, 10, 50000,  'MANUAL', SYSDATE);
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (15, 9,  30000,  'MANUAL', SYSDATE);
-- user_id 16 (문성원)
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (16, 4,  350000, 'MANUAL', SYSDATE);
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (16, 2,  250000, 'MANUAL', SYSDATE);
-- user_id 17 (양수진)
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (17, 11, 150000, 'MANUAL', SYSDATE);
-- user_id 19 (안예은)
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (19, 9,  100000, 'MANUAL', SYSDATE);
INSERT INTO USER_SPENDING_PATTERNS (user_id, category_id, monthly_amount, source, updated_at) VALUES (19, 2,  80000,  'MANUAL', SYSDATE);

COMMIT;


-- ==============================================================
-- ==============================================================
-- 34. AI_CHAT_LOGS (10건)
-- ==============================================================
INSERT INTO AI_CHAT_LOGS (user_id, session_id, user_input, ai_response, created_at)
VALUES (1, 'sess_guest_001', '주유 할인이 좋은 카드를 추천해줘', 'SK OIL&LPG카드와 딩딩 신용카드를 추천드립니다. SK OIL&LPG카드는 SK 주유소에서 리터당 최대 120원 할인을 제공합니다.', SYSDATE - 3);
INSERT INTO AI_CHAT_LOGS (user_id, session_id, user_input, ai_response, created_at)
VALUES (2, 'sess_guest_002', '연회비 없는 체크카드가 있나요?', '빵빵체크카드, 국민행복체크카드, 동백전 체크카드 등 연회비 0원 체크카드를 다양하게 제공하고 있습니다.', SYSDATE - 2);
INSERT INTO AI_CHAT_LOGS (user_id, session_id, user_input, ai_response, created_at)
VALUES (3, 'sess_guest_003', '대학생에게 맞는 카드가 뭔가요?', '빵빵체크카드를 추천드립니다. 만 18세 이상이면 발급 가능하며 커피, 쇼핑, 페이 할인 등 생활 혜택이 풍부합니다.', SYSDATE - 2);
INSERT INTO AI_CHAT_LOGS (user_id, session_id, user_input, ai_response, created_at)
VALUES (NULL, 'sess_anon_001', '동백전이 뭔가요?', '동백전은 부산시 지역화폐입니다. 부산 동백전 체크카드와 선불카드를 통해 부산 시내 가맹점에서 사용하면 캐시백 혜택을 받을 수 있습니다.', SYSDATE - 1);
INSERT INTO AI_CHAT_LOGS (user_id, session_id, user_input, ai_response, created_at)
VALUES (5, 'sess_guest_005', '사업자용 카드 추천', 'SOHO-BIZ카드와 BNK Simple카드를 추천드립니다. SOHO-BIZ카드는 보증료 할인 서비스가 포함된 당행 최초 사업자 특화 카드입니다.', SYSDATE - 1);
INSERT INTO AI_CHAT_LOGS (user_id, session_id, user_input, ai_response, created_at)
VALUES (4, 'sess_guest_006', '여행 마일리지 카드 알려줘', 'REX2 대한항공마일리지형 카드를 추천드립니다. 국내 가맹점 1,500원당 1마일리지가 적립되며 공항 라운지 무료 이용도 가능합니다.', SYSDATE);
INSERT INTO AI_CHAT_LOGS (user_id, session_id, user_input, ai_response, created_at)
VALUES (NULL, 'sess_anon_002', '카드 신청 방법이 궁금해요', '영업점 방문, 인터넷뱅킹, 모바일뱅킹을 통해 신청하실 수 있습니다. 카드별로 신청 채널이 다를 수 있으니 상품 상세 페이지를 확인해 주세요.', SYSDATE);
INSERT INTO AI_CHAT_LOGS (user_id, session_id, user_input, ai_response, created_at)
VALUES (7, 'sess_guest_007', '커피 할인되는 카드', '스타벅스 할인을 위해서는 REX2 포인트형, 오늘은e 신용카드, 오늘은e 체크카드를 추천드립니다. 최대 5% 청구할인이 가능합니다.', SYSDATE);
INSERT INTO AI_CHAT_LOGS (user_id, session_id, user_input, ai_response, created_at)
VALUES (9, 'sess_guest_008', '하이패스 카드 있어요?', '후불 하이패스카드(개인)와 후불 하이패스카드(기업) 두 종류가 있습니다. 출퇴근 시간대 통행료 최대 50% 할인, 경차 50% 할인 혜택이 있습니다.', SYSDATE);
INSERT INTO AI_CHAT_LOGS (user_id, session_id, user_input, ai_response, created_at)
VALUES (NULL, 'sess_anon_003', '쇼핑 많이 하는데 좋은 카드는?', 'BNK 부자되세요 홈쇼핑카드와 딩딩 체크카드를 추천드립니다. 홈쇼핑카드는 4대 홈쇼핑 6% 할인, 백화점 5% 할인 혜택을 제공합니다.', SYSDATE);

COMMIT;




-- ================================================================
-- SEQUENCE RESET
-- PK를 트리거로 자동 채번하는 방식이므로 일반 시퀀스는 재설정 불필요.
-- 카드 상품코드(card_id)는 CARDS INSERT에서 명시적으로 넣으므로
-- SEQ_CARD_SERIAL_* 시퀀스만 재설정합니다.
-- ================================================================

-- ── 카드 시리얼 시퀀스 재설정 ────────────────────────────────────
-- CREDIT 18건 삽입 완료 (10101001~10101018) → 다음 NEXTVAL=19
ALTER SEQUENCE SEQ_CARD_SERIAL_CREDIT  INCREMENT BY 18;
SELECT SEQ_CARD_SERIAL_CREDIT.NEXTVAL  FROM DUAL;
ALTER SEQUENCE SEQ_CARD_SERIAL_CREDIT  INCREMENT BY 1;

-- CHECK 8건 삽입 완료 (10201001~10201008) → 다음 NEXTVAL=9
ALTER SEQUENCE SEQ_CARD_SERIAL_CHECK   INCREMENT BY 8;
SELECT SEQ_CARD_SERIAL_CHECK.NEXTVAL   FROM DUAL;
ALTER SEQUENCE SEQ_CARD_SERIAL_CHECK   INCREMENT BY 1;

-- PREPAID 1건 삽입 완료 (10301001) → 다음 NEXTVAL=2
ALTER SEQUENCE SEQ_CARD_SERIAL_PREPAID INCREMENT BY 1;
SELECT SEQ_CARD_SERIAL_PREPAID.NEXTVAL FROM DUAL;
ALTER SEQUENCE SEQ_CARD_SERIAL_PREPAID INCREMENT BY 1;

-- HYBRID 삽입 없음 → 별도 초기화 불필요 (START WITH 1 유지)

COMMIT;