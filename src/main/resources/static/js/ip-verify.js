'use strict';
/* ================================================================
   ip-verify.js  |  IP 2단계 인증 페이지 스크립트
   의존: utils.js (BnkAPI, BnkToast, BnkDOM)
   ================================================================ */

/* ── 세션에서 챌린지 정보 복원 ── */
const CHALLENGE_TOKEN = sessionStorage.getItem('ip_challenge_token');
const USER_ID         = Number(sessionStorage.getItem('ip_challenge_userId'));
const NEXT            = sessionStorage.getItem('ip_challenge_next') || '/';

/* 세션 값 없으면 로그인 화면으로 */
if (!CHALLENGE_TOKEN || !USER_ID) {
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
    sessionStorage.removeItem('ip_challenge_token');
    sessionStorage.removeItem('ip_challenge_userId');
    sessionStorage.removeItem('ip_challenge_next');
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
        var res = await BnkAPI.post('/api/auth/ip-verify/email/send', {
            userId: USER_ID,
            challengeToken: CHALLENGE_TOKEN,
        });
        if (res.ok) {
            BnkToast.success('인증 코드가 발송되었습니다. (10분 유효)');
            startEmailTimer();
        } else {
            if (res.data && res.data.code === 'IP001') {
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
    var code     = (document.getElementById('emailCode').value || '').trim().toUpperCase();
    var nickname = (document.getElementById('emailNickname').value || '').trim() || null;

    if (!code) { showErr('email-error', '인증 코드를 입력해 주세요.'); return; }
    showErr('email-error', '');

    var btn = document.getElementById('btnEmailConfirm');
    BnkDOM.btnLoading(btn, true, '확인 중...');
    try {
        var res = await BnkAPI.post('/api/auth/ip-verify/email/confirm', {
            userId: USER_ID,
            challengeToken: CHALLENGE_TOKEN,
            code: code,
            nickname: nickname,
        });
        if (res.ok) {
            onLoginSuccess();
        } else {
            var errCode = res.data && res.data.code;
            if (errCode === 'IP001') {
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
   CI 인증
================================================================ */
async function confirmCi() {
    var residentFront = (document.getElementById('residentFront').value || '').trim();
    var genderCode    = (document.getElementById('genderCode').value || '').trim();
    var nickname      = (document.getElementById('ciNickname').value || '').trim() || null;

    if (!residentFront || residentFront.length !== 6 || !/^\d{6}$/.test(residentFront)) {
        showErr('ci-error', '주민번호 앞 6자리를 숫자로 정확히 입력해 주세요.'); return;
    }
    if (!genderCode || !/^[1-4]$/.test(genderCode)) {
        showErr('ci-error', '성별코드를 1~4 사이로 입력해 주세요.'); return;
    }
    showErr('ci-error', '');

    var btn = document.getElementById('btnCiConfirm');
    BnkDOM.btnLoading(btn, true, '확인 중...');
    try {
        var res = await BnkAPI.post('/api/auth/ip-verify/ci', {
            userId: USER_ID,
            challengeToken: CHALLENGE_TOKEN,
            residentFront: residentFront,
            genderCode: genderCode,
            nickname: nickname,
        });
        if (res.ok) {
            onLoginSuccess();
        } else {
            var errCode = res.data && res.data.code;
            if (errCode === 'IP001') {
                onSessionExpired();
            } else if (errCode === 'IP003') {
                showErr('ci-error', 'CI 인증 실패 횟수를 초과했습니다. 이메일 인증을 이용해 주세요.');
            } else {
                showErr('ci-error', (res.data && res.data.message) || '본인 정보가 일치하지 않습니다.');
            }
        }
    } catch (e) {
        showErr('ci-error', '인증 중 오류가 발생했습니다.');
    } finally {
        BnkDOM.btnLoading(btn, false);
    }
}

/* ── 이벤트 바인딩 — 스크립트 로드 시점에 바로 실행 ── */
function bindEvents() {
    var btnEmail = document.getElementById('btnEmail');
    var btnCi    = document.getElementById('btnCi');
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

/* HTML 파싱 완료 여부에 관계없이 안전하게 바인딩 */
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', bindEvents);
} else {
    bindEvents();
}