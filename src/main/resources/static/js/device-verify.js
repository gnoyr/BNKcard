'use strict';
/* ================================================================
   device-verify.js  |  새 기기 인증 페이지 스크립트
   의존: utils.js (BnkAPI, BnkToast, BnkDOM)
   challengeToken 은 서버가 발급한 불투명 토큰. userId 는 서버가 도출한다.
   ================================================================ */

/* ── 세션에서 챌린지 정보 복원 ── */
const CHALLENGE_TOKEN = sessionStorage.getItem('device_challenge_token');
const NEXT            = sessionStorage.getItem('device_challenge_next') || '/';

/* 세션 값 없으면 로그인 화면으로 */
if (!CHALLENGE_TOKEN) {
    location.href = '/login';
}

/* ── 뷰 전환 ── */
function showView(id) {
    ['view-select', 'view-email', 'view-ci'].forEach(function(v) {
        var el = document.getElementById(v);
        if (el) el.style.display = (v === id) ? '' : 'none';
    });
}

/* ── 에러 표시 / 숨김 ── */
function showErr(id, msg) {
    var el = document.getElementById(id);
    if (!el) return;
    el.textContent = msg;
    el.style.display = msg ? '' : 'none';
}

/* ── 로그인 완료 처리 ── */
function onLoginSuccess() {
    sessionStorage.removeItem('device_challenge_token');
    sessionStorage.removeItem('device_challenge_next');
    sessionStorage.setItem('bnk_login_at', String(Date.now()));
    location.href = NEXT;
}

/* ── 세션 만료 처리 ── */
function onSessionExpired() {
    BnkToast.error('인증 세션이 만료되었습니다. 다시 로그인해 주세요.');
    setTimeout(function() { location.href = '/login'; }, 2000);
}

/* ================================================================
   이메일 인증
================================================================ */
var _emailTimer = null;

async function sendEmailCode() {
    var btn = document.getElementById('btnSendCode');
    BnkDOM.btnLoading(btn, true, '발송 중...');
    showErr('email-error', '');
    try {
        var res = await BnkAPI.post('/api/auth/device-verify/email/send', {
            challengeToken: CHALLENGE_TOKEN,
        });
        if (res.ok) {
            BnkToast.success('인증 코드가 발송되었습니다. (10분 유효)');
            startEmailTimer();
        } else {
            if (res.data && res.data.code === 'DEV001') {
                onSessionExpired();
            } else {
                showErr('email-error', (res.data && res.data.message) || '발송에 실패했습니다.');
            }
        }
    } catch (e) {
        showErr('email-error', '발송 중 오류가 발생했습니다.');
    } finally {
        BnkDOM.btnLoading(btn, false);
    }
}

function startEmailTimer() {
    clearInterval(_emailTimer);
    var remaining = 600;
    var timerEl = document.getElementById('email-timer');
    if (timerEl) timerEl.style.display = '';

    _emailTimer = setInterval(function() {
        remaining--;
        var m = String(Math.floor(remaining / 60)).padStart(2, '0');
        var s = String(remaining % 60).padStart(2, '0');
        if (timerEl) timerEl.textContent = '남은 시간 ' + m + ':' + s;
        if (remaining <= 0) {
            clearInterval(_emailTimer);
            if (timerEl) timerEl.textContent = '코드가 만료되었습니다. 다시 발송해 주세요.';
        }
    }, 1000);
}

async function confirmEmailCode() {
    var code       = (document.getElementById('emailCode').value || '').trim().toUpperCase();
    var deviceName = (document.getElementById('emailDeviceName').value || '').trim() || null;

    if (!code) { showErr('email-error', '인증 코드를 입력해 주세요.'); return; }
    showErr('email-error', '');

    var btn = document.getElementById('btnEmailConfirm');
    BnkDOM.btnLoading(btn, true, '확인 중...');
    try {
        var res = await BnkAPI.post('/api/auth/device-verify/email/confirm', {
            challengeToken: CHALLENGE_TOKEN,
            code: code,
            deviceName: deviceName,
        });
        if (res.ok) {
            onLoginSuccess();
        } else {
            var errCode = res.data && res.data.code;
            if (errCode === 'DEV001') {
                onSessionExpired();
            } else {
                showErr('email-error', (res.data && res.data.message) || '인증 코드가 올바르지 않습니다.');
            }
        }
    } catch (e) {
        showErr('email-error', '인증 중 오류가 발생했습니다.');
    } finally {
        BnkDOM.btnLoading(btn, false);
    }
}

/* ================================================================
   본인정보 인증 (이름 + 생년월일 + 전화번호)
================================================================ */

/**
 * 생년월일 입력값을 YYYY-MM-DD 형식으로 정규화.
 * "19900101" → "1990-01-01", "1990-01-01" → 그대로
 */
function normalizeBirthDate(raw) {
    var digits = raw.replace(/[^0-9]/g, '');
    if (digits.length === 8) {
        return digits.slice(0, 4) + '-' + digits.slice(4, 6) + '-' + digits.slice(6, 8);
    }
    return raw.trim();
}

async function confirmCi() {
    var name       = (document.getElementById('ciName').value || '').trim();
    var birthRaw   = (document.getElementById('ciBirthDate').value || '').trim();
    var phone      = (document.getElementById('ciPhone').value || '').trim();
    var deviceName = (document.getElementById('ciDeviceName').value || '').trim() || null;

    // 입력 검증
    if (!name) {
        showErr('ci-error', '이름을 입력해 주세요.'); return;
    }
    var birthDate = normalizeBirthDate(birthRaw);
    if (!/^\d{4}-\d{2}-\d{2}$/.test(birthDate)) {
        showErr('ci-error', '생년월일을 올바르게 입력해 주세요. (예: 1990-01-01)'); return;
    }
    var phoneDigits = phone.replace(/[^0-9]/g, '');
    if (phoneDigits.length < 10 || phoneDigits.length > 11) {
        showErr('ci-error', '전화번호를 올바르게 입력해 주세요.'); return;
    }
    showErr('ci-error', '');

    var btn = document.getElementById('btnCiConfirm');
    BnkDOM.btnLoading(btn, true, '확인 중...');
    try {
        // 백엔드는 residentFront(주민번호 앞 6자리=YYMMDD)를 기대한다.
        // birthDate "1992-03-15" → "920315" 로 변환.
        var residentFront = birthDate.replace(/-/g, '').slice(2); // YYYYMMDD → YYMMDD
        var res = await BnkAPI.post('/api/auth/device-verify/ci', {
            challengeToken: CHALLENGE_TOKEN,
            name: name,
            residentFront: residentFront,
            phone: phone,
            deviceName: deviceName,
        });
        if (res.ok) {
            onLoginSuccess();
        } else {
            var errCode = res.data && res.data.code;
            if (errCode === 'DEV001') {
                onSessionExpired();
            } else if (errCode === 'DEV003') {
                showErr('ci-error', '인증 실패 횟수를 초과했습니다. 이메일 인증을 이용해 주세요.');
            } else {
                showErr('ci-error', (res.data && res.data.message) || '입력하신 정보가 일치하지 않습니다.');
            }
        }
    } catch (e) {
        showErr('ci-error', '인증 중 오류가 발생했습니다.');
    } finally {
        BnkDOM.btnLoading(btn, false);
    }
}

/* ── 이벤트 바인딩 ── */
function bindEvents() {
    var btnEmail     = document.getElementById('btnEmail');
    var btnCi        = document.getElementById('btnCi');
    var btnBackEmail = document.getElementById('btnBackFromEmail');
    var btnBackCi    = document.getElementById('btnBackFromCi');
    var btnSend      = document.getElementById('btnSendCode');
    var btnEmailOk   = document.getElementById('btnEmailConfirm');
    var btnCiOk      = document.getElementById('btnCiConfirm');
    var emailCodeEl  = document.getElementById('emailCode');

    if (btnEmail)     btnEmail.addEventListener('click', function() { showView('view-email'); });
    if (btnCi)        btnCi.addEventListener('click', function() { showView('view-ci'); });
    if (btnBackEmail) btnBackEmail.addEventListener('click', function() { showView('view-select'); });
    if (btnBackCi)    btnBackCi.addEventListener('click', function() { showView('view-select'); });
    if (btnSend)      btnSend.addEventListener('click', sendEmailCode);
    if (btnEmailOk)   btnEmailOk.addEventListener('click', confirmEmailCode);
    if (btnCiOk)      btnCiOk.addEventListener('click', confirmCi);
    if (emailCodeEl)  emailCodeEl.addEventListener('keydown', function(e) {
        if (e.key === 'Enter') confirmEmailCode();
    });
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', bindEvents);
} else {
    bindEvents();
}
