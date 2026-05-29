'use strict';

/* ════════════════════════════════════════════
   공통 유틸
════════════════════════════════════════════ */

const API = {
    async request(method, url, body) {
        const opts = {
            method,
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
        };
        if (body) opts.body = JSON.stringify(body);
        try {
            const res = await fetch(url, opts);
            const data = await res.json().catch(() => ({}));
            return { ok: res.ok, status: res.status, data };
        } catch {
            return { ok: false, status: 0, data: {} };
        }
    },
    get:   (url)       => API.request('GET',   url),
    post:  (url, body) => API.request('POST',  url, body),
    put:   (url, body) => API.request('PUT',   url, body),
    patch: (url, body) => API.request('PATCH', url, body),
};

/**
 * ✅ 인라인 에러 표시
 * auth.css의 .alert 는 display:none 이고 .show 규칙이 별도로 있으나
 * 안전하게 style.display 를 직접 세팅해 CSS 의존도 제거
 */
function showError(el, msg) {
    if (!el) return;
    el.textContent = msg;
    el.style.display = 'block';     // ✅ CSS 클래스 대신 직접 제어
    el.classList.add('show');       // (CSS .alert.show 있는 경우 대비 유지)
}

function hideError(el) {
    if (!el) return;
    el.textContent = '';
    el.style.display = '';          // ✅ 직접 제어
    el.classList.remove('show');
}

function showView(id) {
    document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
    document.getElementById(id)?.classList.add('active');
}

/**
 * GlobalExceptionHandler 응답 구조에서 사용자 메시지 추출
 * 우선순위: fieldErrors[0] > detail > message > data.message > fallback
 */
function extractMessage(data) {
    if (data?.fieldErrors?.length) {
        const fe = data.fieldErrors[0];
        return fe.field ? `${fe.field}: ${fe.message}` : fe.message;
    }
    return data?.detail
        || data?.message
        || data?.data?.message
        || '오류가 발생했습니다. 다시 시도해 주세요.';
}

/**
 * ✅ 토스트 알림
 * header.js가 window.showToast 를 노출하면 위임,
 * 그렇지 않으면 자체 구현으로 동작 (CSS는 auth.css에 추가됨)
 */
function authToast(msg, type = 'info') {
    // header.js 에서 window.showToast 노출 후 이 분기 사용
    if (typeof window.showToast === 'function') {
        window.showToast(msg, type);
        return;
    }

    // fallback: auth.css에 추가한 토스트 CSS 클래스 사용
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        document.body.appendChild(container);
    }

    const el = document.createElement('div');
    el.className = `toast toast--${type}`;
    el.textContent = msg;
    container.appendChild(el);

    requestAnimationFrame(() => el.classList.add('toast--show'));
    setTimeout(() => {
        el.classList.remove('toast--show');
        setTimeout(() => el.remove(), 300);
    }, 3500);
}

function togglePw(inputId, btnEl) {
    const el = document.getElementById(inputId);
    if (!el) return;
    const btn = btnEl instanceof Element ? btnEl : (btnEl ? document.getElementById(btnEl) : null);
    if (el.type === 'password') {
        el.type = 'text';
        if (btn) btn.classList.add('is-show');
    } else {
        el.type = 'password';
        if (btn) btn.classList.remove('is-show');
    }
}

function calcPwScore(pw) {
    let score = 0;
    if (pw.length >= 8)        score++;
    if (/[A-Za-z]/.test(pw))   score++;
    if (/\d/.test(pw))         score++;
    if (/[@$!%*#?&]/.test(pw)) score++;
    return score;
}

function renderStrengthBar(score, barId, labelId) {
    const bar   = document.getElementById(barId);
    const label = document.getElementById(labelId);
    if (!bar) return;
    bar.className = 'strength-fill';
    if (score <= 1)      { bar.classList.add('weak');   if (label) label.textContent = '보안 강도: 약함'; }
    else if (score <= 3) { bar.classList.add('medium'); if (label) label.textContent = '보안 강도: 보통'; }
    else                 { bar.classList.add('strong'); if (label) label.textContent = '보안 강도: 강함'; }
}


/* ════════════════════════════════════════════
   로그인  (login.html)
════════════════════════════════════════════ */

const login = {
    init() {
        document.getElementById('loginForm')?.addEventListener('submit', async (e) => {
            e.preventDefault();
            await this._submit();
        });
        const btnEye = document.getElementById('btnPwToggle');
        btnEye?.addEventListener('click', () => togglePw('password', btnEye));
    },

    async _submit() {
        const email = document.getElementById('email')?.value.trim();
        const pw    = document.getElementById('password')?.value ?? '';
        const err   = document.getElementById('login-error');

        if (!email) { showError(err, '이메일을 입력해 주세요.'); return; }
        if (!pw)    { showError(err, '비밀번호를 입력해 주세요.'); return; }
        hideError(err);

        const res = await API.post('/api/auth/login', {
            email,
            password:   pw,
            deviceInfo: navigator.userAgent.substring(0, 100),
        });

		if (res.ok) {
		    const next = new URLSearchParams(location.search).get('next');
		    const safeNext = next && next.startsWith('/') && !next.startsWith('//') ? next : '/';
		    window.location.href = safeNext;
		} else if (res.status === 0) {
            // 네트워크 단절 — 토스트로 표시 (인라인 에러보다 위치상 적절)
            authToast('서버에 연결할 수 없습니다. 네트워크를 확인해 주세요.', 'error');
        } else {
            showError(err, extractMessage(res.data));
        }
    },
};


/* ════════════════════════════════════════════
   회원가입  (signup.html)
════════════════════════════════════════════ */

const signup = (() => {
    let _agreedTermsIds = [];
    let _emailVerified  = false;
    let _codeTimer      = null;

    function _clearCodeTimer() {
        if (_codeTimer) { clearInterval(_codeTimer); _codeTimer = null; }
    }

    function _updateStepBar(step) {
        [1, 2, 3].forEach(n => {
            document.getElementById(`step-dot-${n}`)?.classList.toggle('active', n === step);
            document.getElementById(`step-dot-${n}`)?.classList.toggle('done',   n < step);
        });
    }

    function syncAllCheck() {
        const allRequired = document.querySelectorAll('[data-required="Y"]');
        const allChecked  = [...allRequired].every(cb => cb.checked);
        const allCb       = document.getElementById('terms-all');
        if (allCb) allCb.checked = allChecked;
    }

    async function _loadTerms() {
        const loading   = document.getElementById('terms-loading');
        const errEl     = document.getElementById('terms-error');
        const extraWrap = document.getElementById('extra-terms-wrap');
        if (!extraWrap) return;

        const res = await API.get('/api/terms/packages/SIGNUP');
        loading?.classList.add('is-hidden');

        if (!res.ok) {
            if (errEl) {
                errEl.style.display = 'block';
                errEl.textContent   = '추가 약관을 불러오지 못했습니다. 필수 약관은 위에 표시된 내용으로 진행합니다.';
            }
            return;
        }

        const terms      = res.data?.data?.terms ?? [];
        const extraTerms = terms.filter(t => (t.requiredYn ?? t.required) !== 'Y');

        extraTerms.forEach(t => {
            const li = document.createElement('li');
            li.className = 'terms-item';
            li.innerHTML = `
                <label class="terms-check">
                    <input type="checkbox" data-id="${t.termsId}" data-required="N" />
                    <span class="terms-label">[선택] ${t.title}</span>
                </label>`;
            extraWrap.appendChild(li);
            li.querySelector('input').addEventListener('change', syncAllCheck);
        });
    }

    return {
        init() {
            _loadTerms();

            const allCb = document.getElementById('terms-all');
            allCb?.addEventListener('change', () => {
                document.querySelectorAll('[data-required], [data-id]').forEach(cb => {
                    cb.checked = allCb.checked;
                });
            });

            document.getElementById('btnStep1Next')?.addEventListener('click', () => {
                const requiredCbs = document.querySelectorAll('[data-required="Y"]');
                const allAgreed   = [...requiredCbs].every(cb => cb.checked);
                if (!allAgreed) {
                    authToast('필수 약관에 모두 동의해 주세요.', 'error');
                    return;
                }
                _agreedTermsIds = [...document.querySelectorAll('[data-id]:checked')]
                    .map(cb => Number(cb.dataset.id));
                showView('view-step2');
                _updateStepBar(2);
            });

            document.getElementById('btnSendCode')?.addEventListener('click',   () => this._sendVerifyCode());
            document.getElementById('btnVerifyCode')?.addEventListener('click', () => this._verifyCode());

            const pwInput      = document.getElementById('password');
            const pwConfirm    = document.getElementById('passwordConfirm');
            const btnPwToggle1 = document.getElementById('btnPwToggle1');
            const btnPwToggle2 = document.getElementById('btnPwToggle2');
            const btnStep2Back = document.getElementById('btnStep2Back');

            btnPwToggle1?.addEventListener('click', () => togglePw('password',        btnPwToggle1));
            btnPwToggle2?.addEventListener('click', () => togglePw('passwordConfirm', btnPwToggle2));

            btnStep2Back?.addEventListener('click', () => {
                _clearCodeTimer();
                showView('view-step1');
                _updateStepBar(1);
            });

            pwInput?.addEventListener('input', () => {
                const wrap = document.getElementById('signup-pw-strength');
                const v    = pwInput.value;
                if (!v) { wrap?.classList.add('is-hidden'); return; }
                wrap?.classList.remove('is-hidden');
                renderStrengthBar(calcPwScore(v), 'signup-strength-bar', 'signup-strength-label');
            });

            pwConfirm?.addEventListener('input', () => {
                const msg = document.getElementById('pw-match-msg');
                if (!msg) return;
                const c = pwConfirm.value;
                if (!c) { msg.textContent = ''; return; }
                msg.textContent = pwInput.value === c ? '비밀번호가 일치합니다.' : '비밀번호가 일치하지 않습니다.';
                msg.className   = pwInput.value === c ? 'helper ok' : 'helper error';
            });

            document.getElementById('signupForm')?.addEventListener('submit', async (e) => {
                e.preventDefault();
                await this._submit();
            });
        },

        async _sendVerifyCode() {
            const email = document.getElementById('email')?.value.trim();
            const errEl = document.getElementById('verify-error');
            if (!email) { showError(errEl, '이메일을 먼저 입력해 주세요.'); return; }

            const res = await API.post('/api/auth/send-verify-code', { email });
            if (res.ok) {
                hideError(errEl);
                authToast('인증코드가 발송되었습니다. 이메일을 확인해 주세요.', 'success');
                let remaining = 180;
                _clearCodeTimer();
                const timerEl = document.getElementById('code-timer');
                if (timerEl) {
                    _codeTimer = setInterval(() => {
                        remaining--;
                        const m = String(Math.floor(remaining / 60)).padStart(2, '0');
                        const s = String(remaining % 60).padStart(2, '0');
                        timerEl.textContent = `${m}:${s}`;
                        if (remaining <= 0) { _clearCodeTimer(); timerEl.textContent = '만료됨'; }
                    }, 1000);
                }
            } else {
                showError(errEl, extractMessage(res.data));
            }
        },

        async _verifyCode() {
            const email   = document.getElementById('email')?.value.trim();
            const code    = document.getElementById('verifyCode')?.value.trim();
            const errEl   = document.getElementById('verify-error');
            const success = document.getElementById('verify-success');
            if (!code) { showError(errEl, '인증코드를 입력해 주세요.'); return; }

            const res = await API.post('/api/auth/verify-email', { email, code });
            if (res.ok) {
                _emailVerified = true;
                _clearCodeTimer();
                hideError(errEl);
                success?.classList.remove('is-hidden');
            } else {
                showError(errEl, extractMessage(res.data));
            }
        },

        async _submit() {
            const email       = document.getElementById('email')?.value.trim();
            const pw          = document.getElementById('password')?.value ?? '';
            const pwc         = document.getElementById('passwordConfirm')?.value ?? '';
            const name        = document.getElementById('name')?.value.trim();
            const phone       = document.getElementById('phone')?.value.trim().replace(/-/g, '');
            const birth       = document.getElementById('birthDate')?.value;
            const mkt         = document.getElementById('marketingAgree')?.checked ?? false;
            const job         = document.getElementById('job')?.value ?? '';
            const incomeLevel = document.getElementById('incomeLevelCode')?.value ?? '';
            const csStr       = document.getElementById('creditScore')?.value;
            const creditScore = csStr ? Number(csStr) : null;
            const err         = document.getElementById('signup-error');

            const checks = [
                [!email,                                                              '이메일을 입력해 주세요.'],
                [!_emailVerified,                                                     '이메일 인증을 완료해 주세요.'],
                [pw.length < 8,                                                       '비밀번호는 8자 이상 입력해 주세요.'],
                [pw !== pwc,                                                          '비밀번호가 일치하지 않습니다.'],
                [!name,                                                               '이름을 입력해 주세요.'],
                [!phone,                                                              '휴대전화 번호를 입력해 주세요.'],
                [creditScore !== null && (creditScore < 300 || creditScore > 1000),   '신용점수는 300~1000 사이로 입력해 주세요.'],
            ];
            for (const [cond, msg] of checks) {
                if (cond) { showError(err, msg); return; }
            }
            hideError(err);

            const res = await API.post('/api/auth/signup', {
                email,
                password:        pw,
                name,
                phone,
                birthDate:       birth || null,
                marketingAgree:  mkt,
                agreedTermsIds:  _agreedTermsIds,
                job,
                incomeLevelCode: incomeLevel,
                creditScore,
            });

            if (res.ok) {
                _clearCodeTimer();
                const doneNameEl  = document.getElementById('done-name');
                const doneEmailEl = document.getElementById('done-email-display');
                if (doneNameEl)  doneNameEl.textContent  = name;
                if (doneEmailEl) doneEmailEl.textContent = email;
                showView('view-step3');
                _updateStepBar(3);
            } else {
                showError(err, extractMessage(res.data));
            }
        },
    };
})();


/* ════════════════════════════════════════════
   아이디 찾기  (find-id.html)
════════════════════════════════════════════ */

const findId = {
    init() {
        document.getElementById('findIdForm')?.addEventListener('submit', async (e) => {
            e.preventDefault();
            await this._submit();
        });
    },

    async _submit() {
        const name  = document.getElementById('name')?.value.trim();
        const phone = document.getElementById('phone')?.value.trim().replace(/-/g, '');
        const err   = document.getElementById('find-error');

        if (!name)  { showError(err, '이름을 입력해 주세요.'); return; }
        if (!phone) { showError(err, '휴대전화 번호를 입력해 주세요.'); return; }
        hideError(err);

        const res = await API.post('/api/auth/find-id', { name, phone });

        if (res.ok) {
            const maskedEmail = res.data?.data?.maskedEmail ?? res.data?.maskedEmail ?? '–';
            const emailEl = document.getElementById('result-email');
            if (emailEl) emailEl.textContent = maskedEmail;
            showView('view-result');
        } else if (res.status === 0) {
            authToast('서버에 연결할 수 없습니다. 네트워크를 확인해 주세요.', 'error');
        } else {
            showError(err, extractMessage(res.data));
        }
    },
};


/* ════════════════════════════════════════════
   비밀번호 재설정  (reset-password.html)
════════════════════════════════════════════ */

const resetPw = {
    _token: null,

    init() {
        const token = new URLSearchParams(window.location.search).get('token');
        if (token) { this._token = token; showView('view-new-pw'); }

        document.getElementById('findPwForm')?.addEventListener('submit', async (e) => {
            e.preventDefault();
            await this._requestLink();
        });

        const newPw     = document.getElementById('new-pw');
        const newPwConf = document.getElementById('new-pw-confirm');
        const btnEye1   = document.getElementById('btn-eye1');
        const btnEye2   = document.getElementById('btn-eye2');

        newPw?.addEventListener('input',     () => this._checkStrength());
        newPwConf?.addEventListener('input', () => this._checkMatch());
        btnEye1?.addEventListener('click',   () => togglePw('new-pw',         btnEye1));
        btnEye2?.addEventListener('click',   () => togglePw('new-pw-confirm', btnEye2));

        document.getElementById('resetPwForm')?.addEventListener('submit', async (e) => {
            e.preventDefault();
            await this._submit();
        });
    },

    async _requestLink() {
        const email = document.getElementById('rp-email')?.value.trim();
        const name  = document.getElementById('rp-name')?.value.trim();
        const err   = document.getElementById('rp-request-error');

        if (!email) { showError(err, '이메일을 입력해 주세요.'); return; }
        if (!name)  { showError(err, '이름을 입력해 주세요.'); return; }
        hideError(err);

        const res = await API.post('/api/auth/find-password', { email, name });
        if (res.ok) {
            showView('view-sent');
        } else if (res.status === 0) {
            authToast('서버에 연결할 수 없습니다. 네트워크를 확인해 주세요.', 'error');
        } else {
            showError(err, extractMessage(res.data));
        }
    },

    _checkStrength() {
        const pw   = document.getElementById('new-pw')?.value ?? '';
        const wrap = document.getElementById('pw-strength');
        if (!pw) { wrap?.classList.add('is-hidden'); return; }
        wrap?.classList.remove('is-hidden');
        renderStrengthBar(calcPwScore(pw), 'strength-bar', 'strength-label');
    },

    _checkMatch() {
        const pw  = document.getElementById('new-pw')?.value ?? '';
        const c   = document.getElementById('new-pw-confirm')?.value ?? '';
        const msg = document.getElementById('match-msg');
        if (!msg) return;
        if (!c) { msg.textContent = ''; return; }
        msg.textContent = pw === c ? '비밀번호가 일치합니다.' : '비밀번호가 일치하지 않습니다.';
        msg.className   = pw === c ? 'helper ok' : 'helper error';
    },

    async _submit() {
        const pw  = document.getElementById('new-pw')?.value ?? '';
        const pwc = document.getElementById('new-pw-confirm')?.value ?? '';
        const err = document.getElementById('reset-error');

        if (pw.length < 8) { showError(err, '비밀번호는 8자 이상 입력해 주세요.'); return; }
        if (pw !== pwc)     { showError(err, '비밀번호가 일치하지 않습니다.'); return; }
        hideError(err);

        const res = await API.post('/api/auth/reset-password', {
            token:              this._token,
            newPassword:        pw,
            newPasswordConfirm: pwc,
        });

        if (res.ok) {
            // ✅ alert() 제거 → 토스트 표시 후 2초 뒤 자동 이동
            authToast('비밀번호가 변경되었습니다. 로그인 페이지로 이동합니다.', 'success');
            setTimeout(() => { window.location.href = '/login'; }, 2000);
        } else if (res.status === 400 || res.status === 401) {
            showView('view-token-error');
        } else if (res.status === 0) {
            authToast('서버에 연결할 수 없습니다. 네트워크를 확인해 주세요.', 'error');
        } else {
            showError(err, extractMessage(res.data));
        }
    },
};


/* ════════════════════════════════════════════
   페이지 초기화
════════════════════════════════════════════ */
document.addEventListener('DOMContentLoaded', () => {
    const page = document.body.dataset.page;

    if (page === 'login')          login.init();
    if (page === 'signup')         signup.init();
    if (page === 'find-id')        findId.init();
    if (page === 'reset-password') resetPw.init();
});