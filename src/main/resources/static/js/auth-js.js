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
    get: (url) => API.request('GET', url),
    post: (url, body) => API.request('POST', url, body),
    put: (url, body) => API.request('PUT', url, body),
    patch: (url, body) => API.request('PATCH', url, body),
};

function showError(el, msg) {
    if (!el) return;
    el.textContent = msg;
    el.classList.add('show');
}
function hideError(el) {
    if (!el) return;
    el.textContent = '';
    el.classList.remove('show');
}
function showView(id) {
    document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
    document.getElementById(id)?.classList.add('active');
}
function extractMessage(data) {
    return data?.message || data?.data?.message || '오류가 발생했습니다. 다시 시도해 주세요.';
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
    if (pw.length >= 8) score++;
    if (/[A-Za-z]/.test(pw)) score++;
    if (/\d/.test(pw)) score++;
    if (/[@$!%*#?&]/.test(pw)) score++;
    return score;
}

function renderStrengthBar(score, barId, labelId) {
    const bar = document.getElementById(barId);
    const label = document.getElementById(labelId);
    if (!bar) return;
    bar.className = 'strength-fill';
    if (score <= 1) { bar.classList.add('weak'); if (label) label.textContent = '보안 강도: 약함'; }
    else if (score <= 3) { bar.classList.add('medium'); if (label) label.textContent = '보안 강도: 보통'; }
    else { bar.classList.add('strong'); if (label) label.textContent = '보안 강도: 강함'; }
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

        const btnEye = document.getElementById('btn-pw-eye');
        btnEye?.addEventListener('click', () => togglePw('password', btnEye));
    },

    async _submit() {
        const email = document.getElementById('email')?.value.trim();
        const pw = document.getElementById('password')?.value ?? '';
        const err = document.getElementById('login-error');

        if (!email) { showError(err, '이메일을 입력해 주세요.'); return; }
        if (!pw) { showError(err, '비밀번호를 입력해 주세요.'); return; }
        hideError(err);

        const res = await API.post('/api/auth/login', {
            email,
            password: pw,
            deviceInfo: navigator.userAgent.substring(0, 100),
        });

        if (res.ok) {
            window.location.href = '/';
        } else {
            showError(err, extractMessage(res.data));
        }
    },
};


/* ════════════════════════════════════════════
   회원가입  (signup.html)

   Step1: GET  /api/terms/packages/SIGNUP → 약관 동적 로드
   Step2: POST /api/auth/send-verify-code { email }
          POST /api/auth/verify-email     { email, code }
          POST /api/auth/signup           {
            email, password, name, phone,
            birthDate, marketingAgree, agreedTermsIds,
            job, incomeLevelCode, creditScore
          }
════════════════════════════════════════════ */

const signup = (() => {
    let _agreedTermsIds = [];
    let _emailVerified = false;
    let _codeTimer = null;

    function _clearCodeTimer() {
        if (_codeTimer) { clearInterval(_codeTimer); _codeTimer = null; }
    }

    function _updateStepBar(step) {
        [1, 2, 3].forEach(n => {
            document.getElementById(`step-dot-${n}`)?.classList.toggle('active', n === step);
            document.getElementById(`step-dot-${n}`)?.classList.toggle('done', n < step);
        });
    }

    /* ──────────────────────────────────────────
       전체 동의 체크박스 상태 동기화
       - 정적 약관(data-static) + 동적 약관(data-id) 모두 검사
       - 모두 체크 시 #terms-all 자동 체크
       signup.html 인라인 <script>에서 이동
    ────────────────────────────────────────── */
    function syncAllCheck() {
        const allRequired = document.querySelectorAll('[data-required="Y"]');
        const allChecked = [...allRequired].every(cb => cb.checked);
        const allCb = document.getElementById('terms-all');
        if (allCb) allCb.checked = allChecked;
    }

    /* ──────────────────────────────────────────
       추가 약관 동적 로드 (DB/API → 선택 약관만)
       필수 약관(회원약관, 개인정보처리방침)은 signup.html에 정적 HTML로 포함
    ────────────────────────────────────────── */
    async function _loadTerms() {
        const loading = document.getElementById('terms-loading');
        const errEl = document.getElementById('terms-error');
        const extraWrap = document.getElementById('extra-terms-wrap');

        if (!extraWrap) return;

        const res = await API.get('/api/terms/packages/SIGNUP');
        loading?.classList.add('is-hidden');

        if (!res.ok) {
            errEl.style.display = 'block';
            errEl.textContent = '추가 약관을 불러오지 못했습니다. 필수 약관은 위에 표시된 내용으로 진행합니다.';
            return;
        }

        const terms = res.data?.data?.terms ?? [];

        /* 정적으로 이미 표시된 필수 약관은 중복 제외 → 선택 약관만 렌더링 */
        const extraTerms = terms.filter(t => (t.requiredYn ?? t.required_yn) === 'N');

        if (extraTerms.length === 0) {
            loading?.remove();
            return;
        }

        /* 선택 약관 섹션 타이틀 */
        const titleDiv = document.createElement('div');
        titleDiv.className = 'form-section-title';
        titleDiv.style.marginTop = '8px';
        titleDiv.textContent = '선택 약관';
        extraWrap.appendChild(titleDiv);

        extraTerms.forEach(t => {
            const div = document.createElement('div');
            div.className = 'terms-item';
            div.innerHTML = `
        <label class="terms-check-row">
          <input type="checkbox"
                 id="terms-${t.termsId}"
                 data-id="${t.termsId}"
                 data-required="${t.requiredYn ?? t.required_yn ?? 'N'}" />
          <span>${t.title}</span>
          <span class="tag opt">선택</span>
        </label>`;
            extraWrap.appendChild(div);

            /* 동적 체크박스도 전체동의 상태 갱신에 참여 */
            div.querySelector('input')?.addEventListener('change', syncAllCheck);
        });

        loading?.remove();
    }

    /* ──────────────────────────────────────────
       Step1 다음 버튼 — 약관 필수 검증
    ────────────────────────────────────────── */
    function _step1Next() {
        const errEl = document.getElementById('terms-error');

        /* 1) 정적 필수 약관 (회원약관, 개인정보처리방침) */
        const staticRequired = document.querySelectorAll('[data-static="true"][data-required="Y"]');
        const staticAllChecked = [...staticRequired].every(cb => cb.checked);

        if (!staticAllChecked) {
            errEl.style.display = 'block';
            errEl.textContent = '회원약관 및 개인정보처리취급방침에 모두 동의해 주세요.';
            return false;
        }

        /* 2) 동적 필수 약관 (API에서 requiredYn='Y'로 온 항목) */
        const dynamicRequired = document.querySelectorAll('#extra-terms-wrap input[data-required="Y"]');
        const dynamicAllChecked = [...dynamicRequired].every(cb => cb.checked);

        if (!dynamicAllChecked) {
            errEl.style.display = 'block';
            errEl.textContent = '필수 약관에 모두 동의해 주세요.';
            return false;
        }

        errEl.style.display = 'none';
        errEl.textContent = '';

        /* 3) 서버 전송용 agreedTermsIds = 체크된 동적 약관 ID만 */
        _agreedTermsIds = [...document.querySelectorAll('#extra-terms-wrap input[data-id]:checked')]
            .map(cb => Number(cb.dataset.id));

        return true;
    }

    return {
        init() {
            _loadTerms();

            /* ── 전체 동의 체크박스 연동
                   signup.html 인라인 <script>에서 이동
               ── */
            document.getElementById('terms-all')?.addEventListener('change', function() {
                const checked = this.checked;
                /* 정적 약관 (data-static="true") */
                document.querySelectorAll('[data-static="true"]').forEach(cb => { cb.checked = checked; });
                /* 동적 약관 (API 로드 후 생성) */
                document.querySelectorAll('#extra-terms-wrap input[data-id]').forEach(cb => { cb.checked = checked; });
            });

            /* 정적 체크박스 변경 시 전체동의 상태 갱신
               signup.html 인라인 <script>에서 이동 */
            document.querySelectorAll('[data-static="true"]').forEach(cb => {
                cb.addEventListener('change', syncAllCheck);
            });

            /* Step1 제출 */
            document.getElementById('termsForm')?.addEventListener('submit', (e) => {
                e.preventDefault();
                if (_step1Next()) { showView('view-step2'); _updateStepBar(2); }
            });

            /* Step2 버튼·이벤트 */
            const btnSendCode = document.getElementById('btnSendCode');
            const btnVerify = document.getElementById('btnVerifyCode');
            const btnPwToggle1 = document.getElementById('btnPwToggle1');
            const btnPwToggle2 = document.getElementById('btnPwToggle2');
            const btnStep2Back = document.getElementById('btnStep2Back');
            const pwInput = document.getElementById('password');
            const pwConfirm = document.getElementById('passwordConfirm');

            btnSendCode?.addEventListener('click', () => this._sendVerifyCode());
            btnVerify?.addEventListener('click', () => this._verifyEmail());
            btnPwToggle1?.addEventListener('click', () => togglePw('password', btnPwToggle1));
            btnPwToggle2?.addEventListener('click', () => togglePw('passwordConfirm', btnPwToggle2));

            btnStep2Back?.addEventListener('click', () => {
                _clearCodeTimer();
                showView('view-step1');
                _updateStepBar(1);
            });

            /* 비밀번호 강도 */
            pwInput?.addEventListener('input', () => {
                const wrap = document.getElementById('signup-pw-strength');
                const v = pwInput.value;
                if (!v) { wrap?.classList.add('is-hidden'); return; }
                wrap?.classList.remove('is-hidden');
                renderStrengthBar(calcPwScore(v), 'signup-strength-bar', 'signup-strength-label');
            });

            /* 비밀번호 확인 일치 */
            pwConfirm?.addEventListener('input', () => {
                const msg = document.getElementById('pw-match-msg');
                if (!msg) return;
                const c = pwConfirm.value;
                if (!c) { msg.textContent = ''; return; }
                msg.textContent = pwInput.value === c ? '비밀번호가 일치합니다.' : '비밀번호가 일치하지 않습니다.';
                msg.className = pwInput.value === c ? 'helper ok' : 'helper error';
            });

            /* 최종 제출 */
            document.getElementById('signupForm')?.addEventListener('submit', async (e) => {
                e.preventDefault();
                await this._submit();
            });
        },

        /** POST /api/auth/send-verify-code */
        async _sendVerifyCode() {
            const email = document.getElementById('email')?.value.trim();
            const errEl = document.getElementById('verify-error');
            if (!email) { showError(errEl, '이메일을 먼저 입력해 주세요.'); return; }
            hideError(errEl);

            const timerEl = document.getElementById('code-timer');
            const sentMsg = document.getElementById('code-sent-msg');

            const res = await API.post('/api/auth/send-verify-code', { email });
            if (res.ok) {
                sentMsg?.classList.remove('is-hidden');
                /* 5분 카운트다운 */
                _clearCodeTimer();
                let sec = 300;
                if (timerEl) timerEl.textContent = ` (${Math.floor(sec / 60)}:00)`;
                _codeTimer = setInterval(() => {
                    sec--;
                    if (timerEl) {
                        const m = String(Math.floor(sec / 60)).padStart(1, '0');
                        const s = String(sec % 60).padStart(2, '0');
                        timerEl.textContent = ` (${m}:${s})`;
                    }
                    if (sec <= 0) { _clearCodeTimer(); if (timerEl) timerEl.textContent = ' (만료)'; }
                }, 1000);
            } else {
                showError(errEl, extractMessage(res.data));
            }
        },

        /** POST /api/auth/verify-email */
        async _verifyEmail() {
            const email = document.getElementById('email')?.value.trim();
            const code = document.getElementById('verifyCode')?.value.trim();
            const errEl = document.getElementById('verify-error');
            const okEl = document.getElementById('verify-success');

            if (!email) { showError(errEl, '이메일을 입력해 주세요.'); return; }
            if (!code) { showError(errEl, '인증코드를 입력해 주세요.'); return; }
            hideError(errEl);

            const res = await API.post('/api/auth/verify-email', { email, code });
            if (res.ok) {
                _emailVerified = true;
                _clearCodeTimer();
                okEl?.classList.remove('is-hidden');
                document.getElementById('code-timer')?.parentElement?.classList.add('is-hidden');
            } else {
                showError(errEl, extractMessage(res.data));
            }
        },

        /** POST /api/auth/signup — job, incomeLevelCode, creditScore 포함 */
        async _submit() {
            const email = document.getElementById('email')?.value.trim();
            const pw = document.getElementById('password')?.value ?? '';
            const pwc = document.getElementById('passwordConfirm')?.value ?? '';
            const name = document.getElementById('name')?.value.trim();
            const phone = document.getElementById('phone')?.value.trim().replace(/-/g, '');
            const birth = document.getElementById('birthDate')?.value?.replace(/-/g, '') ?? '';
            const mkt = document.getElementById('marketingAgree')?.checked ? 'Y' : 'N';
            const err = document.getElementById('signup-error');

            /* ── 추가된 선택 필드 ── */
            const job = document.getElementById('job')?.value || null;
            const incomeLevelCode = document.getElementById('incomeLevelCode')?.value || null;
            const creditScoreStr = document.getElementById('creditScore')?.value?.trim();
            const creditScore = creditScoreStr ? Number(creditScoreStr) : null;

            /* ── 필수값 검증 ── */
            const checks = [
                [!email, '이메일을 입력해 주세요.'],
                [!_emailVerified, '이메일 인증을 완료해 주세요.'],
                [pw.length < 8, '비밀번호는 8자 이상 입력해 주세요.'],
                [pw !== pwc, '비밀번호가 일치하지 않습니다.'],
                [!name, '이름을 입력해 주세요.'],
                [!phone, '휴대전화 번호를 입력해 주세요.'],
                [creditScore !== null && (creditScore < 300 || creditScore > 1000),
                    '신용점수는 300~1000 사이로 입력해 주세요.'],
            ];
            for (const [cond, msg] of checks) {
                if (cond) { showError(err, msg); return; }
            }
            hideError(err);

            const res = await API.post('/api/auth/signup', {
                email,
                password: pw,
                name,
                phone,
                birthDate: birth || null,
                marketingAgree: mkt,
                agreedTermsIds: _agreedTermsIds,
                job,
                incomeLevelCode,
                creditScore,
            });

            if (res.ok) {
                _clearCodeTimer();
                const doneNameEl = document.getElementById('done-name');
                const doneEmailEl = document.getElementById('done-email-display');
                if (doneNameEl) doneNameEl.textContent = name;
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
   POST /api/auth/find-id { name, phone }
   → { maskedEmail }
════════════════════════════════════════════ */

const findId = {
    init() {
        document.getElementById('findIdForm')?.addEventListener('submit', async (e) => {
            e.preventDefault();
            await this._submit();
        });
    },

    async _submit() {
        const name = document.getElementById('name')?.value.trim();
        const phone = document.getElementById('phone')?.value.trim().replace(/-/g, '');
        const err = document.getElementById('find-error');

        if (!name) { showError(err, '이름을 입력해 주세요.'); return; }
        if (!phone) { showError(err, '휴대전화 번호를 입력해 주세요.'); return; }
        hideError(err);

        const res = await API.post('/api/auth/find-id', { name, phone });

        if (res.ok) {
            const maskedEmail = res.data?.data?.maskedEmail ?? res.data?.maskedEmail ?? '–';
            const emailEl = document.getElementById('result-email');
            if (emailEl) emailEl.textContent = maskedEmail;
            showView('view-result');
        } else {
            showError(err, extractMessage(res.data));
        }
    },
};


/* ════════════════════════════════════════════
   비밀번호 재설정  (reset-password.html)

   [링크 요청]
   POST /api/auth/find-password { email, name }
   → 이메일로 reset 링크 발송

   [실제 변경 — URL ?token= 감지 시 자동 표시]
   POST /api/auth/reset-password { token, newPassword, newPasswordConfirm }
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

        const newPw = document.getElementById('new-pw');
        const newPwConf = document.getElementById('new-pw-confirm');
        const btnEye1 = document.getElementById('btn-eye1');
        const btnEye2 = document.getElementById('btn-eye2');

        newPw?.addEventListener('input', () => this._checkStrength());
        newPwConf?.addEventListener('input', () => this._checkMatch());
        btnEye1?.addEventListener('click', () => togglePw('new-pw', btnEye1));
        btnEye2?.addEventListener('click', () => togglePw('new-pw-confirm', btnEye2));

        document.getElementById('resetPwForm')?.addEventListener('submit', async (e) => {
            e.preventDefault();
            await this._submit();
        });
    },

    async _requestLink() {
        const email = document.getElementById('rp-email')?.value.trim();
        const name = document.getElementById('rp-name')?.value.trim();
        const err = document.getElementById('rp-request-error');

        if (!email) { showError(err, '이메일을 입력해 주세요.'); return; }
        if (!name) { showError(err, '이름을 입력해 주세요.'); return; }
        hideError(err);

        const res = await API.post('/api/auth/find-password', { email, name });
        if (res.ok) { showView('view-sent'); }
        else { showError(err, extractMessage(res.data)); }
    },

    _checkStrength() {
        const pw = document.getElementById('new-pw')?.value ?? '';
        const wrap = document.getElementById('pw-strength');
        if (!pw) { wrap?.classList.add('is-hidden'); return; }
        wrap?.classList.remove('is-hidden');
        renderStrengthBar(calcPwScore(pw), 'strength-bar', 'strength-label');
    },

    _checkMatch() {
        const pw = document.getElementById('new-pw')?.value ?? '';
        const c = document.getElementById('new-pw-confirm')?.value ?? '';
        const msg = document.getElementById('match-msg');
        if (!msg) return;
        if (!c) { msg.textContent = ''; return; }
        msg.textContent = pw === c ? '비밀번호가 일치합니다.' : '비밀번호가 일치하지 않습니다.';
        msg.className = pw === c ? 'helper ok' : 'helper error';
    },

    async _submit() {
        const pw = document.getElementById('new-pw')?.value ?? '';
        const pwc = document.getElementById('new-pw-confirm')?.value ?? '';
        const err = document.getElementById('reset-error');

        if (pw.length < 8) { showError(err, '비밀번호는 8자 이상 입력해 주세요.'); return; }
        if (pw !== pwc) { showError(err, '비밀번호가 일치하지 않습니다.'); return; }
        hideError(err);

        const res = await API.post('/api/auth/reset-password', {
            token: this._token,
            newPassword: pw,
            newPasswordConfirm: pwc,
        });

        if (res.ok) {
            alert('비밀번호가 변경되었습니다.\n다시 로그인해 주세요.');
            window.location.href = 'login.html';
        } else {
            if (res.status === 400 || res.status === 401) { showView('view-token-error'); }
            else { showError(err, extractMessage(res.data)); }
        }
    },
};


/* ════════════════════════════════════════════
   페이지 초기화
════════════════════════════════════════════ */
document.addEventListener('DOMContentLoaded', () => {
    const page = document.body.dataset.page;

    if (page === 'login') login.init();
    if (page === 'signup') signup.init();
    if (page === 'find-id') findId.init();
    if (page === 'reset-password') resetPw.init();
});