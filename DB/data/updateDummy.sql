-- ================================================================
-- [BNK 부산은행] 기존 User 데이터 정정 + 마이페이지 테스트 더미데이터
-- 대상 DB : Oracle (busanbank schema)
-- 실행 전제: busanbank_insert_all.sql 이 이미 실행된 상태
--
-- ■ 수정 범위
--   [1] USERS          — 20건 전체 정정 (password_hash, job, income_level_code,
--                         login 시각, 검증 여부, WITHDRAWN/DORMANT 처리)
--   [2] LOGIN_HISTORIES — login_result_code 코드값 정정 (FAIL_PW → FAIL)
--   [3] CARD_APPLICATIONS — 마이페이지 신청현황 테스트용 INSERT
--   [4] USER_CARDS        — 발급카드 목록 테스트용 INSERT
--   [5] AUDIT_LOGS        — 마이페이지 이벤트 이력 보강
--
-- ■ 약관(TERMS*) 관련 테이블 : 변경 없음
-- ■ CARD_CATEGORIES / CARDS / CARD_BENEFITS 등 : 변경 없음
-- ================================================================
--
-- ■ user_id 매핑 (TRG_USERS_BI 자동 채번 순서)
--    1: 김민준(kim.minjun)     — ACTIVE  / 카드 2장 보유, 소비패턴 풍부
--    2: 이소연(lee.soyeon)     — ACTIVE  / 심사중 신청 보유
--    3: 박준호(park.junho)     — ACTIVE  / 신규(카드 없음)
--    4: 최은지(choi.eunji)     — ACTIVE  / 카드 1장, 신청중 1건
--    5: 정현우(jung.hyunwoo)   — ACTIVE  / 거절 이력 보유
--    6: 한지영(han.jiyoung)    — ACTIVE
--    7: 윤승호(yoon.seungho)   — ACTIVE
--    8: 임채연(lim.chaeyeon)   — ACTIVE  / 신규(카드 없음)
--    9: 신동현(shin.donghyun)  — ACTIVE  / 승인완료 카드 1장
--   10: 권민지(kwon.minji)     — ACTIVE
--   11: 오상훈(oh.sanghoon)    — ACTIVE
--   12: 서지은(seo.jieun)      — ACTIVE
--   13: 배민석(bae.minseok)    — ACTIVE  / 카드 1장 + 정지카드 1장
--   14: 장혜진(jang.hyejin)    — ACTIVE
--   15: 노지호(noh.jiho)       — ACTIVE
--   16: 문성원(moon.sungwon)   — ACTIVE
--   17: 양수진(yang.soojin)    — ACTIVE
--   18: 홍종우(hong.jongwoo)   — DORMANT (휴면, 1년+ 미접속)
--   19: 안예은(ahn.yeeun)      — ACTIVE
--   20: 송재현(song.jaehyun)   — WITHDRAWN (탈퇴, 개인정보 삭제)
--
-- ■ 공통 비밀번호: Test1234!
--   BCrypt hash: $2a$10$OFTcNmdF/m.mgp63QO.KD.4Y0acVIR.TQl14ntOE9GWqfR0Xok852
-- ================================================================


-- ================================================================
-- [1] USERS 정정 (20건)
-- ================================================================
-- 정정 항목:
--   password_hash    : Placeholder → 실제 BCrypt 해시 (Test1234!)
--   job              : EMPLOYEE→EMPLOYED, SELF_EMP→SELF_EMPLOYED,
--                      HOUSEWIFE→UNEMPLOYED, FREELANCER→OTHER
--   income_level_code: HIGH→LV4, MID_HIGH→LV3, MID→LV2, LOW_MID/LOW→LV1
--   last_login_at    : NULL → 현실적인 최근 접속 시각
--   last_password_changed_at : NULL → 가입 후 최초 설정 시각
--   is_email_verified / is_phone_verified : NULL → Y/N
--   push_enabled / marketing_agree : 다양하게 설정
--   [user_id=18] DORMANT: dormant_at, last_login_at 400일 전
--   [user_id=20] WITHDRAWN: deleted_yn='Y', withdrawn_at, PII NULL 처리
-- ================================================================

-- ── user_id = 1 | 김민준 | ACTIVE (마이페이지 메인 테스트 기준 계정) ────
UPDATE USERS SET
    password_hash             = '$2a$10$OFTcNmdF/m.mgp63QO.KD.4Y0acVIR.TQl14ntOE9GWqfR0Xok852',
    job                       = 'EMPLOYED',
    income_level_code         = 'LV3',
    last_login_at             = SYSTIMESTAMP - INTERVAL '2'   HOUR,
    last_password_changed_at  = SYSTIMESTAMP - INTERVAL '60'  DAY,
    is_email_verified         = 'Y',
    is_phone_verified         = 'Y',
    push_enabled              = 'Y',
    marketing_agree           = 'Y'
WHERE email = 'kim.minjun@email.com';

-- ── user_id = 2 | 이소연 | ACTIVE (카드 심사중 테스트) ──────────────────
UPDATE USERS SET
    password_hash             = '$2a$10$OFTcNmdF/m.mgp63QO.KD.4Y0acVIR.TQl14ntOE9GWqfR0Xok852',
    job                       = 'EMPLOYED',
    income_level_code         = 'LV2',
    last_login_at             = SYSTIMESTAMP - INTERVAL '1'   DAY,
    last_password_changed_at  = SYSTIMESTAMP - INTERVAL '90'  DAY,
    is_email_verified         = 'Y',
    is_phone_verified         = 'Y',
    push_enabled              = 'Y',
    marketing_agree           = 'N'
WHERE email = 'lee.soyeon@email.com';

-- ── user_id = 3 | 박준호 | ACTIVE (신규, 카드 없음) ─────────────────────
UPDATE USERS SET
    password_hash             = '$2a$10$OFTcNmdF/m.mgp63QO.KD.4Y0acVIR.TQl14ntOE9GWqfR0Xok852',
    job                       = 'STUDENT',
    income_level_code         = 'LV1',
    last_login_at             = SYSTIMESTAMP - INTERVAL '3'   DAY,
    last_password_changed_at  = SYSTIMESTAMP - INTERVAL '3'   DAY,
    is_email_verified         = 'Y',
    is_phone_verified         = 'N',
    push_enabled              = 'Y',
    marketing_agree           = 'N'
WHERE email = 'park.junho@email.com';

-- ── user_id = 4 | 최은지 | ACTIVE (카드 보유 + 신청중) ──────────────────
UPDATE USERS SET
    password_hash             = '$2a$10$OFTcNmdF/m.mgp63QO.KD.4Y0acVIR.TQl14ntOE9GWqfR0Xok852',
    job                       = 'EMPLOYED',
    income_level_code         = 'LV3',
    last_login_at             = SYSTIMESTAMP - INTERVAL '5'   HOUR,
    last_password_changed_at  = SYSTIMESTAMP - INTERVAL '45'  DAY,
    is_email_verified         = 'Y',
    is_phone_verified         = 'Y',
    push_enabled              = 'Y',
    marketing_agree           = 'Y'
WHERE email = 'choi.eunji@email.com';

-- ── user_id = 5 | 정현우 | ACTIVE (거절 이력 보유) ──────────────────────
UPDATE USERS SET
    password_hash             = '$2a$10$OFTcNmdF/m.mgp63QO.KD.4Y0acVIR.TQl14ntOE9GWqfR0Xok852',
    job                       = 'SELF_EMPLOYED',
    income_level_code         = 'LV4',
    last_login_at             = SYSTIMESTAMP - INTERVAL '12'  HOUR,
    last_password_changed_at  = SYSTIMESTAMP - INTERVAL '120' DAY(3),
    is_email_verified         = 'Y',
    is_phone_verified         = 'Y',
    push_enabled              = 'N',
    marketing_agree           = 'N'
WHERE email = 'jung.hyunwoo@email.com';

-- ── user_id = 6 | 한지영 | ACTIVE ────────────────────────────────────────
UPDATE USERS SET
    password_hash             = '$2a$10$OFTcNmdF/m.mgp63QO.KD.4Y0acVIR.TQl14ntOE9GWqfR0Xok852',
    job                       = 'EMPLOYED',
    income_level_code         = 'LV2',
    last_login_at             = SYSTIMESTAMP - INTERVAL '2'   DAY,
    last_password_changed_at  = SYSTIMESTAMP - INTERVAL '80'  DAY,
    is_email_verified         = 'Y',
    is_phone_verified         = 'Y',
    push_enabled              = 'Y',
    marketing_agree           = 'Y'
WHERE email = 'han.jiyoung@email.com';

-- ── user_id = 7 | 윤승호 | ACTIVE ────────────────────────────────────────
UPDATE USERS SET
    password_hash             = '$2a$10$OFTcNmdF/m.mgp63QO.KD.4Y0acVIR.TQl14ntOE9GWqfR0Xok852',
    job                       = 'OTHER',
    income_level_code         = 'LV3',
    last_login_at             = SYSTIMESTAMP - INTERVAL '6'   HOUR,
    last_password_changed_at  = SYSTIMESTAMP - INTERVAL '150' DAY(3),
    is_email_verified         = 'Y',
    is_phone_verified         = 'Y',
    push_enabled              = 'N',
    marketing_agree           = 'N'
WHERE email = 'yoon.seungho@email.com';

-- ── user_id = 8 | 임채연 | ACTIVE (신규, 카드 없음) ─────────────────────
UPDATE USERS SET
    password_hash             = '$2a$10$OFTcNmdF/m.mgp63QO.KD.4Y0acVIR.TQl14ntOE9GWqfR0Xok852',
    job                       = 'STUDENT',
    income_level_code         = 'LV1',
    last_login_at             = SYSTIMESTAMP - INTERVAL '4'   DAY,
    last_password_changed_at  = SYSTIMESTAMP - INTERVAL '4'   DAY,
    is_email_verified         = 'Y',
    is_phone_verified         = 'N',
    push_enabled              = 'Y',
    marketing_agree           = 'N'
WHERE email = 'lim.chaeyeon@email.com';

-- ── user_id = 9 | 신동현 | ACTIVE (승인완료 카드 보유) ──────────────────
UPDATE USERS SET
    password_hash             = '$2a$10$OFTcNmdF/m.mgp63QO.KD.4Y0acVIR.TQl14ntOE9GWqfR0Xok852',
    job                       = 'EMPLOYED',
    income_level_code         = 'LV4',
    last_login_at             = SYSTIMESTAMP - INTERVAL '1'   HOUR,
    last_password_changed_at  = SYSTIMESTAMP - INTERVAL '30'  DAY,
    is_email_verified         = 'Y',
    is_phone_verified         = 'Y',
    push_enabled              = 'Y',
    marketing_agree           = 'Y'
WHERE email = 'shin.donghyun@email.com';

-- ── user_id = 10 | 권민지 | ACTIVE ───────────────────────────────────────
UPDATE USERS SET
    password_hash             = '$2a$10$OFTcNmdF/m.mgp63QO.KD.4Y0acVIR.TQl14ntOE9GWqfR0Xok852',
    job                       = 'EMPLOYED',
    income_level_code         = 'LV2',
    last_login_at             = SYSTIMESTAMP - INTERVAL '8'   HOUR,
    last_password_changed_at  = SYSTIMESTAMP - INTERVAL '200' DAY(3),
    is_email_verified         = 'Y',
    is_phone_verified         = 'Y',
    push_enabled              = 'Y',
    marketing_agree           = 'N'
WHERE email = 'kwon.minji@email.com';

-- ── user_id = 11 | 오상훈 | ACTIVE ───────────────────────────────────────
UPDATE USERS SET
    password_hash             = '$2a$10$OFTcNmdF/m.mgp63QO.KD.4Y0acVIR.TQl14ntOE9GWqfR0Xok852',
    job                       = 'SELF_EMPLOYED',
    income_level_code         = 'LV3',
    last_login_at             = SYSTIMESTAMP - INTERVAL '3'   DAY,
    last_password_changed_at  = SYSTIMESTAMP - INTERVAL '100' DAY(3),
    is_email_verified         = 'Y',
    is_phone_verified         = 'Y',
    push_enabled              = 'Y',
    marketing_agree           = 'Y'
WHERE email = 'oh.sanghoon@email.com';

-- ── user_id = 12 | 서지은 | ACTIVE ───────────────────────────────────────
UPDATE USERS SET
    password_hash             = '$2a$10$OFTcNmdF/m.mgp63QO.KD.4Y0acVIR.TQl14ntOE9GWqfR0Xok852',
    job                       = 'UNEMPLOYED',
    income_level_code         = 'LV2',
    last_login_at             = SYSTIMESTAMP - INTERVAL '5'   DAY,
    last_password_changed_at  = SYSTIMESTAMP - INTERVAL '70'  DAY,
    is_email_verified         = 'Y',
    is_phone_verified         = 'Y',
    push_enabled              = 'N',
    marketing_agree           = 'N'
WHERE email = 'seo.jieun@email.com';

-- ── user_id = 13 | 배민석 | ACTIVE (카드 보유 + 정지카드) ───────────────
UPDATE USERS SET
    password_hash             = '$2a$10$OFTcNmdF/m.mgp63QO.KD.4Y0acVIR.TQl14ntOE9GWqfR0Xok852',
    job                       = 'EMPLOYED',
    income_level_code         = 'LV3',
    last_login_at             = SYSTIMESTAMP - INTERVAL '10'  HOUR,
    last_password_changed_at  = SYSTIMESTAMP - INTERVAL '55'  DAY,
    is_email_verified         = 'Y',
    is_phone_verified         = 'Y',
    push_enabled              = 'Y',
    marketing_agree           = 'Y'
WHERE email = 'bae.minseok@email.com';

-- ── user_id = 14 | 장혜진 | ACTIVE ───────────────────────────────────────
UPDATE USERS SET
    password_hash             = '$2a$10$OFTcNmdF/m.mgp63QO.KD.4Y0acVIR.TQl14ntOE9GWqfR0Xok852',
    job                       = 'EMPLOYED',
    income_level_code         = 'LV4',
    last_login_at             = SYSTIMESTAMP - INTERVAL '1'   DAY,
    last_password_changed_at  = SYSTIMESTAMP - INTERVAL '40'  DAY,
    is_email_verified         = 'Y',
    is_phone_verified         = 'Y',
    push_enabled              = 'Y',
    marketing_agree           = 'Y'
WHERE email = 'jang.hyejin@email.com';

-- ── user_id = 15 | 노지호 | ACTIVE ───────────────────────────────────────
UPDATE USERS SET
    password_hash             = '$2a$10$OFTcNmdF/m.mgp63QO.KD.4Y0acVIR.TQl14ntOE9GWqfR0Xok852',
    job                       = 'STUDENT',
    income_level_code         = 'LV1',
    last_login_at             = SYSTIMESTAMP - INTERVAL '7'   DAY,
    last_password_changed_at  = SYSTIMESTAMP - INTERVAL '7'   DAY,
    is_email_verified         = 'Y',
    is_phone_verified         = 'N',
    push_enabled              = 'Y',
    marketing_agree           = 'N'
WHERE email = 'noh.jiho@email.com';

-- ── user_id = 16 | 문성원 | ACTIVE ───────────────────────────────────────
UPDATE USERS SET
    password_hash             = '$2a$10$OFTcNmdF/m.mgp63QO.KD.4Y0acVIR.TQl14ntOE9GWqfR0Xok852',
    job                       = 'EMPLOYED',
    income_level_code         = 'LV4',
    last_login_at             = SYSTIMESTAMP - INTERVAL '6'   DAY,
    last_password_changed_at  = SYSTIMESTAMP - INTERVAL '300' DAY(3),
    is_email_verified         = 'Y',
    is_phone_verified         = 'Y',
    push_enabled              = 'Y',
    marketing_agree           = 'Y'
WHERE email = 'moon.sungwon@email.com';

-- ── user_id = 17 | 양수진 | ACTIVE ───────────────────────────────────────
UPDATE USERS SET
    password_hash             = '$2a$10$OFTcNmdF/m.mgp63QO.KD.4Y0acVIR.TQl14ntOE9GWqfR0Xok852',
    job                       = 'STUDENT',
    income_level_code         = 'LV1',
    last_login_at             = SYSTIMESTAMP - INTERVAL '2'   DAY,
    last_password_changed_at  = SYSTIMESTAMP - INTERVAL '2'   DAY,
    is_email_verified         = 'Y',
    is_phone_verified         = 'N',
    push_enabled              = 'Y',
    marketing_agree           = 'N'
WHERE email = 'yang.soojin@email.com';

-- ── user_id = 18 | 홍종우 | DORMANT (1년+ 미접속, dormant_at 추가) ──────
UPDATE USERS SET
    password_hash             = '$2a$10$OFTcNmdF/m.mgp63QO.KD.4Y0acVIR.TQl14ntOE9GWqfR0Xok852',
    job                       = 'SELF_EMPLOYED',
    income_level_code         = 'LV4',
    last_login_at             = SYSTIMESTAMP - INTERVAL '400' DAY(3),
    last_password_changed_at  = SYSTIMESTAMP - INTERVAL '400' DAY(3),
    dormant_at                = SYSTIMESTAMP - INTERVAL '35'  DAY,
    is_email_verified         = 'Y',
    is_phone_verified         = 'Y',
    push_enabled              = 'N',
    marketing_agree           = 'N'
WHERE email = 'hong.jongwoo@email.com';

-- ── user_id = 19 | 안예은 | ACTIVE ───────────────────────────────────────
UPDATE USERS SET
    password_hash             = '$2a$10$OFTcNmdF/m.mgp63QO.KD.4Y0acVIR.TQl14ntOE9GWqfR0Xok852',
    job                       = 'OTHER',
    income_level_code         = 'LV2',
    last_login_at             = SYSTIMESTAMP - INTERVAL '3'   HOUR,
    last_password_changed_at  = SYSTIMESTAMP - INTERVAL '110' DAY(3),
    is_email_verified         = 'Y',
    is_phone_verified         = 'Y',
    push_enabled              = 'Y',
    marketing_agree           = 'Y'
WHERE email = 'ahn.yeeun@email.com';

-- ── user_id = 20 | 송재현 | WITHDRAWN (탈퇴, 개인정보 삭제) ─────────────
-- phone / birth_date / ci_value / job / income_level_code 를 NULL 처리
-- deleted_yn='Y', withdrawn_at, deleted_at 설정
UPDATE USERS SET
    password_hash             = '$2a$10$OFTcNmdF/m.mgp63QO.KD.4Y0acVIR.TQl14ntOE9GWqfR0Xok852',
    phone                     = NULL,
    birth_date                = NULL,
    ci_value                  = NULL,
    job                       = NULL,
    income_level_code         = NULL,
    credit_score              = 0,
    last_login_at             = SYSTIMESTAMP - INTERVAL '15'  DAY,
    last_password_changed_at  = SYSTIMESTAMP - INTERVAL '200' DAY(3),
    withdrawn_at              = SYSTIMESTAMP - INTERVAL '10'  DAY,
    deleted_yn                = 'Y',
    deleted_at                = SYSTIMESTAMP - INTERVAL '10'  DAY,
    is_email_verified         = 'N',
    is_phone_verified         = 'N',
    push_enabled              = 'N',
    marketing_agree           = 'N'
WHERE email = 'song.jaehyun@email.com';



-- ================================================================
-- [2] LOGIN_HISTORIES 정정
--     'FAIL_PW' → 'FAIL' (AuthService 실제 코드값과 일치)
-- ================================================================
UPDATE LOGIN_HISTORIES
SET    login_result_code = 'FAIL'
WHERE  login_result_code = 'FAIL_PW';



-- ================================================================
-- [3] CARD_APPLICATIONS + [4] USER_CARDS (마이페이지 테스트용)
--
-- 신청 시나리오:
--   app_id=1  user_id=1  10101001 ISSUED    → USER_CARDS 연결 (활성 신용카드)
--   app_id=2  user_id=1  10201001 ISSUED    → USER_CARDS 연결 (활성 체크카드)
--   app_id=3  user_id=4  10101002 ISSUED    → USER_CARDS 연결 (최은지 카드)
--   app_id=4  user_id=13 10101001 ISSUED    → USER_CARDS 연결 (배민석 활성)
--   app_id=5  user_id=13 10201002 ISSUED    → USER_CARDS 연결 (배민석 정지)
--   app_id=6  user_id=2  10101002 REVIEWING → 심사중 (카드 미발급)
--   app_id=7  user_id=4  10201001 REQUESTED → 신규신청 (카드 미발급)
--   app_id=8  user_id=9  10101001 APPROVED  → USER_CARDS 연결 (신동현 승인완료)
--   app_id=9  user_id=5  10101003 REJECTED  → 거절 (한도초과)
--
-- ※ card_id 사용 목록 (busanbank_insert_all.sql 기준)
--    10101001 : REX2_포인트형(개인)        [CREDIT]
--    10101002 : REX2_대한항공마일리지형    [CREDIT]
--    10101003 : 캐쉬백카드                 [CREDIT]
--    10201001 : 빵빵체크카드               [CHECK]
--    10201002 : 국민행복체크카드           [CHECK]
-- ================================================================

-- CARD_APPLICATIONS (MERGE INTO — 재실행 시 UPDATE, 최초 실행 시 INSERT)
MERGE INTO CARD_APPLICATIONS tgt
USING (SELECT 1 AS application_id FROM DUAL) src ON (tgt.application_id = src.application_id)
WHEN MATCHED THEN UPDATE SET
    user_id=1, card_id=10101001, application_status='ISSUED', apply_channel='MOBILE',
    requested_limit=3000000, approved_limit=3000000,
    applied_at=SYSTIMESTAMP-INTERVAL '365' DAY(3), reviewed_at=SYSTIMESTAMP-INTERVAL '363' DAY(3),
    updated_at=SYSTIMESTAMP-INTERVAL '363' DAY(3)
WHEN NOT MATCHED THEN INSERT (application_id,user_id,card_id,application_status,apply_channel,requested_limit,approved_limit,applied_at,reviewed_at,created_at,updated_at)
VALUES (1,1,10101001,'ISSUED','MOBILE',3000000,3000000,
    SYSTIMESTAMP-INTERVAL '365' DAY(3),SYSTIMESTAMP-INTERVAL '363' DAY(3),
    SYSTIMESTAMP-INTERVAL '365' DAY(3),SYSTIMESTAMP-INTERVAL '363' DAY(3));

MERGE INTO CARD_APPLICATIONS tgt
USING (SELECT 2 AS application_id FROM DUAL) src ON (tgt.application_id = src.application_id)
WHEN MATCHED THEN UPDATE SET
    user_id=1, card_id=10201001, application_status='ISSUED', apply_channel='WEB',
    requested_limit=0, approved_limit=0,
    applied_at=SYSTIMESTAMP-INTERVAL '180' DAY(3), reviewed_at=SYSTIMESTAMP-INTERVAL '178' DAY(3),
    updated_at=SYSTIMESTAMP-INTERVAL '178' DAY(3)
WHEN NOT MATCHED THEN INSERT (application_id,user_id,card_id,application_status,apply_channel,requested_limit,approved_limit,applied_at,reviewed_at,created_at,updated_at)
VALUES (2,1,10201001,'ISSUED','WEB',0,0,
    SYSTIMESTAMP-INTERVAL '180' DAY(3),SYSTIMESTAMP-INTERVAL '178' DAY(3),
    SYSTIMESTAMP-INTERVAL '180' DAY(3),SYSTIMESTAMP-INTERVAL '178' DAY(3));

MERGE INTO CARD_APPLICATIONS tgt
USING (SELECT 3 AS application_id FROM DUAL) src ON (tgt.application_id = src.application_id)
WHEN MATCHED THEN UPDATE SET
    user_id=4, card_id=10101002, application_status='ISSUED', apply_channel='WEB',
    requested_limit=2000000, approved_limit=2000000,
    applied_at=SYSTIMESTAMP-INTERVAL '200' DAY(3), reviewed_at=SYSTIMESTAMP-INTERVAL '198' DAY(3),
    updated_at=SYSTIMESTAMP-INTERVAL '198' DAY(3)
WHEN NOT MATCHED THEN INSERT (application_id,user_id,card_id,application_status,apply_channel,requested_limit,approved_limit,applied_at,reviewed_at,created_at,updated_at)
VALUES (3,4,10101002,'ISSUED','WEB',2000000,2000000,
    SYSTIMESTAMP-INTERVAL '200' DAY(3),SYSTIMESTAMP-INTERVAL '198' DAY(3),
    SYSTIMESTAMP-INTERVAL '200' DAY(3),SYSTIMESTAMP-INTERVAL '198' DAY(3));

MERGE INTO CARD_APPLICATIONS tgt
USING (SELECT 4 AS application_id FROM DUAL) src ON (tgt.application_id = src.application_id)
WHEN MATCHED THEN UPDATE SET
    user_id=13, card_id=10101001, application_status='ISSUED', apply_channel='MOBILE',
    requested_limit=5000000, approved_limit=5000000,
    applied_at=SYSTIMESTAMP-INTERVAL '300' DAY(3), reviewed_at=SYSTIMESTAMP-INTERVAL '298' DAY(3),
    updated_at=SYSTIMESTAMP-INTERVAL '298' DAY(3)
WHEN NOT MATCHED THEN INSERT (application_id,user_id,card_id,application_status,apply_channel,requested_limit,approved_limit,applied_at,reviewed_at,created_at,updated_at)
VALUES (4,13,10101001,'ISSUED','MOBILE',5000000,5000000,
    SYSTIMESTAMP-INTERVAL '300' DAY(3),SYSTIMESTAMP-INTERVAL '298' DAY(3),
    SYSTIMESTAMP-INTERVAL '300' DAY(3),SYSTIMESTAMP-INTERVAL '298' DAY(3));

MERGE INTO CARD_APPLICATIONS tgt
USING (SELECT 5 AS application_id FROM DUAL) src ON (tgt.application_id = src.application_id)
WHEN MATCHED THEN UPDATE SET
    user_id=13, card_id=10201002, application_status='ISSUED', apply_channel='WEB',
    requested_limit=0, approved_limit=0,
    applied_at=SYSTIMESTAMP-INTERVAL '150' DAY(3), reviewed_at=SYSTIMESTAMP-INTERVAL '148' DAY(3),
    updated_at=SYSTIMESTAMP-INTERVAL '148' DAY(3)
WHEN NOT MATCHED THEN INSERT (application_id,user_id,card_id,application_status,apply_channel,requested_limit,approved_limit,applied_at,reviewed_at,created_at,updated_at)
VALUES (5,13,10201002,'ISSUED','WEB',0,0,
    SYSTIMESTAMP-INTERVAL '150' DAY(3),SYSTIMESTAMP-INTERVAL '148' DAY(3),
    SYSTIMESTAMP-INTERVAL '150' DAY(3),SYSTIMESTAMP-INTERVAL '148' DAY(3));

-- 진행중: REVIEWING (이소연)
MERGE INTO CARD_APPLICATIONS tgt
USING (SELECT 6 AS application_id FROM DUAL) src ON (tgt.application_id = src.application_id)
WHEN MATCHED THEN UPDATE SET
    user_id=2, card_id=10101002, application_status='REVIEWING', apply_channel='WEB',
    requested_limit=4000000, application_comment='소득 증빙 서류 첨부 완료',
    applied_at=SYSTIMESTAMP-INTERVAL '5' DAY, updated_at=SYSTIMESTAMP-INTERVAL '3' DAY
WHEN NOT MATCHED THEN INSERT (application_id,user_id,card_id,application_status,apply_channel,requested_limit,application_comment,applied_at,created_at,updated_at)
VALUES (6,2,10101002,'REVIEWING','WEB',4000000,'소득 증빙 서류 첨부 완료',
    SYSTIMESTAMP-INTERVAL '5' DAY,SYSTIMESTAMP-INTERVAL '5' DAY,SYSTIMESTAMP-INTERVAL '3' DAY);

-- 진행중: REQUESTED (최은지)
MERGE INTO CARD_APPLICATIONS tgt
USING (SELECT 7 AS application_id FROM DUAL) src ON (tgt.application_id = src.application_id)
WHEN MATCHED THEN UPDATE SET
    user_id=4, card_id=10201001, application_status='REQUESTED', apply_channel='MOBILE',
    requested_limit=0, application_comment='체크카드 추가 발급 요청',
    applied_at=SYSTIMESTAMP-INTERVAL '1' DAY, updated_at=SYSTIMESTAMP-INTERVAL '1' DAY
WHEN NOT MATCHED THEN INSERT (application_id,user_id,card_id,application_status,apply_channel,requested_limit,application_comment,applied_at,created_at,updated_at)
VALUES (7,4,10201001,'REQUESTED','MOBILE',0,'체크카드 추가 발급 요청',
    SYSTIMESTAMP-INTERVAL '1' DAY,SYSTIMESTAMP-INTERVAL '1' DAY,SYSTIMESTAMP-INTERVAL '1' DAY);

-- 진행중: APPROVED (신동현)
MERGE INTO CARD_APPLICATIONS tgt
USING (SELECT 8 AS application_id FROM DUAL) src ON (tgt.application_id = src.application_id)
WHEN MATCHED THEN UPDATE SET
    user_id=9, card_id=10101001, application_status='APPROVED', apply_channel='WEB',
    requested_limit=5000000, approved_limit=4000000,
    applied_at=SYSTIMESTAMP-INTERVAL '20' DAY, reviewed_at=SYSTIMESTAMP-INTERVAL '18' DAY,
    updated_at=SYSTIMESTAMP-INTERVAL '18' DAY
WHEN NOT MATCHED THEN INSERT (application_id,user_id,card_id,application_status,apply_channel,requested_limit,approved_limit,applied_at,reviewed_at,created_at,updated_at)
VALUES (8,9,10101001,'APPROVED','WEB',5000000,4000000,
    SYSTIMESTAMP-INTERVAL '20' DAY,SYSTIMESTAMP-INTERVAL '18' DAY,
    SYSTIMESTAMP-INTERVAL '20' DAY,SYSTIMESTAMP-INTERVAL '18' DAY);

-- 거절: REJECTED (정현우)
MERGE INTO CARD_APPLICATIONS tgt
USING (SELECT 9 AS application_id FROM DUAL) src ON (tgt.application_id = src.application_id)
WHEN MATCHED THEN UPDATE SET
    user_id=5, card_id=10101003, application_status='REJECTED', apply_channel='WEB',
    requested_limit=30000000, rejection_reason='신용점수 기준 충족하나 기존 부채 비율 초과로 한도 설정 불가',
    applied_at=SYSTIMESTAMP-INTERVAL '30' DAY, reviewed_at=SYSTIMESTAMP-INTERVAL '28' DAY,
    updated_at=SYSTIMESTAMP-INTERVAL '28' DAY
WHEN NOT MATCHED THEN INSERT (application_id,user_id,card_id,application_status,apply_channel,requested_limit,rejection_reason,applied_at,reviewed_at,created_at,updated_at)
VALUES (9,5,10101003,'REJECTED','WEB',30000000,'신용점수 기준 충족하나 기존 부채 비율 초과로 한도 설정 불가',
    SYSTIMESTAMP-INTERVAL '30' DAY,SYSTIMESTAMP-INTERVAL '28' DAY,
    SYSTIMESTAMP-INTERVAL '30' DAY,SYSTIMESTAMP-INTERVAL '28' DAY);


-- USER_CARDS (MERGE INTO — 재실행 시 UPDATE, 최초 실행 시 INSERT)

-- user_id=1 (김민준) — REX2 포인트형 신용카드 (활성)
MERGE INTO USER_CARDS tgt
USING (SELECT 1 AS user_card_id FROM DUAL) src ON (tgt.user_card_id = src.user_card_id)
WHEN MATCHED THEN UPDATE SET
    user_id=1, card_id=10101001, application_id=1,
    masked_card_number='4501-****-****-2234', card_nickname='REX2 포인트',
    issue_date=DATE '2022-06-01', expire_date=DATE '2027-05-31',
    card_status='ACTIVE', usable_yn='Y',
    daily_limit_amount=1000000, monthly_limit_amount=3000000,
    overseas_enabled_yn='Y', contactless_enabled_yn='Y', deleted_yn='N'
WHEN NOT MATCHED THEN INSERT (user_card_id,user_id,card_id,application_id,masked_card_number,card_nickname,issue_date,expire_date,card_status,usable_yn,daily_limit_amount,monthly_limit_amount,overseas_enabled_yn,contactless_enabled_yn,issued_at,deleted_yn)
VALUES (1,1,10101001,1,'4501-****-****-2234','REX2 포인트',DATE '2022-06-01',DATE '2027-05-31','ACTIVE','Y',1000000,3000000,'Y','Y',TIMESTAMP '2022-06-01 10:00:00','N');

-- user_id=1 (김민준) — 빵빵체크카드 (활성)
MERGE INTO USER_CARDS tgt
USING (SELECT 2 AS user_card_id FROM DUAL) src ON (tgt.user_card_id = src.user_card_id)
WHEN MATCHED THEN UPDATE SET
    user_id=1, card_id=10201001, application_id=2,
    masked_card_number='5401-****-****-8812', card_nickname='빵빵체크',
    issue_date=DATE '2023-02-01', expire_date=DATE '2028-01-31',
    card_status='ACTIVE', usable_yn='Y',
    daily_limit_amount=500000, monthly_limit_amount=1500000,
    overseas_enabled_yn='N', contactless_enabled_yn='Y', deleted_yn='N'
WHEN NOT MATCHED THEN INSERT (user_card_id,user_id,card_id,application_id,masked_card_number,card_nickname,issue_date,expire_date,card_status,usable_yn,daily_limit_amount,monthly_limit_amount,overseas_enabled_yn,contactless_enabled_yn,issued_at,deleted_yn)
VALUES (2,1,10201001,2,'5401-****-****-8812','빵빵체크',DATE '2023-02-01',DATE '2028-01-31','ACTIVE','Y',500000,1500000,'N','Y',TIMESTAMP '2023-02-01 09:30:00','N');

-- user_id=4 (최은지) — REX2 대한항공 신용카드 (활성)
MERGE INTO USER_CARDS tgt
USING (SELECT 3 AS user_card_id FROM DUAL) src ON (tgt.user_card_id = src.user_card_id)
WHEN MATCHED THEN UPDATE SET
    user_id=4, card_id=10101002, application_id=3,
    masked_card_number='4578-****-****-6601', card_nickname='마일리지카드',
    issue_date=DATE '2022-11-01', expire_date=DATE '2027-10-31',
    card_status='ACTIVE', usable_yn='Y',
    daily_limit_amount=500000, monthly_limit_amount=2000000,
    overseas_enabled_yn='Y', contactless_enabled_yn='Y', deleted_yn='N'
WHEN NOT MATCHED THEN INSERT (user_card_id,user_id,card_id,application_id,masked_card_number,card_nickname,issue_date,expire_date,card_status,usable_yn,daily_limit_amount,monthly_limit_amount,overseas_enabled_yn,contactless_enabled_yn,issued_at,deleted_yn)
VALUES (3,4,10101002,3,'4578-****-****-6601','마일리지카드',DATE '2022-11-01',DATE '2027-10-31','ACTIVE','Y',500000,2000000,'Y','Y',TIMESTAMP '2022-11-01 14:00:00','N');

-- user_id=13 (배민석) — REX2 포인트형 신용카드 (활성)
MERGE INTO USER_CARDS tgt
USING (SELECT 4 AS user_card_id FROM DUAL) src ON (tgt.user_card_id = src.user_card_id)
WHEN MATCHED THEN UPDATE SET
    user_id=13, card_id=10101001, application_id=4,
    masked_card_number='4521-****-****-3390', card_nickname='REX2(메인)',
    issue_date=DATE '2021-07-01', expire_date=DATE '2026-06-30',
    card_status='ACTIVE', usable_yn='Y',
    daily_limit_amount=2000000, monthly_limit_amount=5000000,
    overseas_enabled_yn='Y', contactless_enabled_yn='Y', deleted_yn='N'
WHEN NOT MATCHED THEN INSERT (user_card_id,user_id,card_id,application_id,masked_card_number,card_nickname,issue_date,expire_date,card_status,usable_yn,daily_limit_amount,monthly_limit_amount,overseas_enabled_yn,contactless_enabled_yn,issued_at,deleted_yn)
VALUES (4,13,10101001,4,'4521-****-****-3390','REX2(메인)',DATE '2021-07-01',DATE '2026-06-30','ACTIVE','Y',2000000,5000000,'Y','Y',TIMESTAMP '2021-07-01 11:00:00','N');

-- user_id=13 (배민석) — 국민행복체크카드 (분실정지)
MERGE INTO USER_CARDS tgt
USING (SELECT 5 AS user_card_id FROM DUAL) src ON (tgt.user_card_id = src.user_card_id)
WHEN MATCHED THEN UPDATE SET
    user_id=13, card_id=10201002, application_id=5,
    masked_card_number='5502-****-****-7741', card_nickname='행복체크(정지)',
    issue_date=DATE '2022-03-01', expire_date=DATE '2027-02-28',
    card_status='LOST', usable_yn='N', deleted_yn='N'
WHEN NOT MATCHED THEN INSERT (user_card_id,user_id,card_id,application_id,masked_card_number,card_nickname,issue_date,expire_date,card_status,usable_yn,issued_at,deleted_yn)
VALUES (5,13,10201002,5,'5502-****-****-7741','행복체크(정지)',DATE '2022-03-01',DATE '2027-02-28','LOST','N',TIMESTAMP '2022-03-01 09:00:00','N');

-- user_id=9 (신동현) — REX2 포인트형 신용카드 (APPROVED 후 발급)
MERGE INTO USER_CARDS tgt
USING (SELECT 6 AS user_card_id FROM DUAL) src ON (tgt.user_card_id = src.user_card_id)
WHEN MATCHED THEN UPDATE SET
    user_id=9, card_id=10101001, application_id=8,
    masked_card_number='4599-****-****-0023', card_nickname=NULL,
    issue_date=DATE '2025-03-01', expire_date=DATE '2030-02-28',
    card_status='ACTIVE', usable_yn='Y',
    daily_limit_amount=1000000, monthly_limit_amount=4000000,
    overseas_enabled_yn='Y', contactless_enabled_yn='Y', deleted_yn='N'
WHEN NOT MATCHED THEN INSERT (user_card_id,user_id,card_id,application_id,masked_card_number,card_nickname,issue_date,expire_date,card_status,usable_yn,daily_limit_amount,monthly_limit_amount,overseas_enabled_yn,contactless_enabled_yn,issued_at,deleted_yn)
VALUES (6,9,10101001,8,'4599-****-****-0023',NULL,DATE '2025-03-01',DATE '2030-02-28','ACTIVE','Y',1000000,4000000,'Y','Y',TIMESTAMP '2025-03-01 15:00:00','N');



-- ================================================================
-- [5] AUDIT_LOGS 보강 (마이페이지 이벤트 — F-25, F-26 기준)
--     TRG_AUDIT_LOGS_BI 자동채번 → audit_log_id 생략
-- ================================================================

-- 김민준(1): 내 정보 수정 이력 (F-25, 비밀번호 검증 후 job 변경)
INSERT INTO AUDIT_LOGS (actor_type_code, actor_id, action_type_code, target_type_code, target_id, description, ip_address, created_at)
VALUES ('USER', 1, 'USER_UPDATE', 'USER', 1, '직업=EMPLOYED, 소득구간=LV3, 푸시알림=Y (비밀번호 검증 완료)', '211.234.12.10', SYSTIMESTAMP - INTERVAL '10' DAY);

-- 김민준(1): 비밀번호 변경 이력 (F-26, revokeAllSessions 포함)
INSERT INTO AUDIT_LOGS (actor_type_code, actor_id, action_type_code, target_type_code, target_id, description, ip_address, created_at)
VALUES ('USER', 1, 'PASSWORD_CHANGE', 'USER', 1, '비밀번호 변경 완료 (전체 세션 무효화)', '211.234.12.10', SYSTIMESTAMP - INTERVAL '60' DAY);

-- 이소연(2): 내 정보 수정 이력 (phone 번호 변경 포함)
INSERT INTO AUDIT_LOGS (actor_type_code, actor_id, action_type_code, target_type_code, target_id, description, ip_address, created_at)
VALUES ('USER', 2, 'USER_UPDATE', 'USER', 2, '휴대폰 번호 변경 (비밀번호 검증 완료)', '112.175.33.22', SYSTIMESTAMP - INTERVAL '20' DAY);

-- 신동현(9): 카드 신청 승인 후 발급 처리
INSERT INTO AUDIT_LOGS (actor_type_code, actor_id, action_type_code, target_type_code, target_id, description, ip_address, created_at)
VALUES ('USER', 9, 'CREATE', 'CARD_APPLICATION', 8, '카드 신청: REX2_포인트형(개인) 한도 4,000,000원 승인', '175.209.25.14', SYSTIMESTAMP - INTERVAL '18' DAY);

-- 관리자: 홍종우(18) 휴면 처리 기록
INSERT INTO AUDIT_LOGS (actor_type_code, actor_id, action_type_code, target_type_code, target_id, description, ip_address, created_at)
VALUES ('ADMIN', 1, 'STATUS_CHANGE', 'USER', 18, '장기 미접속(400일) 휴면 전환 처리', '10.0.0.1', SYSTIMESTAMP - INTERVAL '35' DAY);

-- 관리자: 송재현(20) 탈퇴 처리 기록
INSERT INTO AUDIT_LOGS (actor_type_code, actor_id, action_type_code, target_type_code, target_id, description, ip_address, created_at)
VALUES ('ADMIN', 2, 'WITHDRAW', 'USER', 20, '회원 탈퇴 처리 및 개인정보 삭제', '10.0.0.1', SYSTIMESTAMP - INTERVAL '10' DAY);



-- ================================================================
-- [SEQUENCE RESET] 재실행 안전 방식 — 현재값 기준으로 목표값까지 점프
-- CARD_APPLICATIONS: 삽입 최대 id=9  → 다음 NEXTVAL=10
-- USER_CARDS        : 삽입 최대 id=6  → 다음 NEXTVAL=7
-- ================================================================
DECLARE
    v_curr  NUMBER;
    v_diff  NUMBER;
    v_target NUMBER;
BEGIN
    -- SEQ_CARD_APPLICATIONS → 목표: 현재 NEXTVAL 이후 10이 되도록
    v_target := 10;
    SELECT SEQ_CARD_APPLICATIONS.NEXTVAL INTO v_curr FROM DUAL;
    v_diff := v_target - v_curr - 1;
    IF v_diff > 0 THEN
        EXECUTE IMMEDIATE 'ALTER SEQUENCE SEQ_CARD_APPLICATIONS INCREMENT BY ' || v_diff;
        SELECT SEQ_CARD_APPLICATIONS.NEXTVAL INTO v_curr FROM DUAL;
        EXECUTE IMMEDIATE 'ALTER SEQUENCE SEQ_CARD_APPLICATIONS INCREMENT BY 1';
    ELSIF v_diff < 0 THEN
        NULL; -- 이미 목표 초과 → 그대로 유지 (실데이터 충돌 없음)
    END IF;

    -- SEQ_USER_CARDS → 목표: 현재 NEXTVAL 이후 7이 되도록
    v_target := 7;
    SELECT SEQ_USER_CARDS.NEXTVAL INTO v_curr FROM DUAL;
    v_diff := v_target - v_curr - 1;
    IF v_diff > 0 THEN
        EXECUTE IMMEDIATE 'ALTER SEQUENCE SEQ_USER_CARDS INCREMENT BY ' || v_diff;
        SELECT SEQ_USER_CARDS.NEXTVAL INTO v_curr FROM DUAL;
        EXECUTE IMMEDIATE 'ALTER SEQUENCE SEQ_USER_CARDS INCREMENT BY 1';
    ELSIF v_diff < 0 THEN
        NULL;
    END IF;
END;
/



-- ================================================================
-- 검증 쿼리
-- ================================================================

-- ▶ USERS 정정 확인
SELECT user_id, name, status_code, job, income_level_code,
       credit_score, push_enabled, marketing_agree,
       is_email_verified, is_phone_verified,
       deleted_yn,
       TO_CHAR(last_login_at,            'YYYY-MM-DD HH24:MI') last_login,
       TO_CHAR(dormant_at,               'YYYY-MM-DD')         dormant_at,
       TO_CHAR(withdrawn_at,             'YYYY-MM-DD')         withdrawn_at
FROM   USERS
ORDER  BY user_id;

-- ▶ 마이페이지 카드현황 (GET /api/users/me/cards) — 대표 계정
SELECT u.user_id, u.name,
       uc.user_card_id, uc.masked_card_number, uc.card_status,
       c.card_name,
       TO_CHAR(uc.issue_date,  'YYYY-MM-DD') issue_date,
       TO_CHAR(uc.expire_date, 'YYYY-MM-DD') expire_date
FROM   USER_CARDS uc
JOIN   USERS u ON u.user_id = uc.user_id
JOIN   CARDS c ON c.card_id = uc.card_id
WHERE  uc.deleted_yn = 'N'
ORDER  BY u.user_id, uc.issued_at DESC;

-- ▶ 마이페이지 신청현황 (GET /api/users/me/cards)
SELECT u.user_id, u.name, ca.application_id,
       ca.application_status, c.card_name,
       ca.requested_limit, ca.approved_limit,
       SUBSTR(ca.rejection_reason, 1, 25) rej_reason,
       TO_CHAR(ca.applied_at, 'YYYY-MM-DD') applied_at
FROM   CARD_APPLICATIONS ca
JOIN   USERS u ON u.user_id = ca.user_id
JOIN   CARDS c ON c.card_id = ca.card_id
ORDER  BY ca.user_id, ca.applied_at DESC;

-- ▶ 소비패턴 도넛차트 (GET /api/users/me/spending) — 김민준
SELECT cc.category_name, usp.monthly_amount,
       ROUND(usp.monthly_amount * 100.0 
           / SUM(usp.monthly_amount) OVER (), 1) AS ratio_pct
FROM   USER_SPENDING_PATTERNS usp
JOIN   CARD_CATEGORIES cc ON cc.category_id = usp.category_id
WHERE  usp.user_id = 1
ORDER  BY usp.monthly_amount DESC;