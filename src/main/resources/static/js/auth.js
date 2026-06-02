/**
 * auth.js  |  BNK 부산은행 인증 페이지 (로그인 · 회원가입 · 아이디찾기 · 비밀번호재설정)
 * ─────────────────────────────────────────────────────────────────────────────
 * 의존: utils.js (BnkAPI, BnkError, BnkToast, BnkDOM)
 * 로드: header.js → utils.js → auth.js
 * ─────────────────────────────────────────────────────────────────────────────
 */
'use strict';

/* ────────────────────────────────────────────────────────────
   로컬 단축 별칭 (utils.js 전역 객체 → 짧게 참조)
──────────────────────────────────────────────────────────── */
const API = BnkAPI;
const showError = BnkDOM.showError.bind(BnkDOM);
const hideError = BnkDOM.hideError.bind(BnkDOM);
const authToast = BnkToast;          // authToast.success / .error / .warning / .info

/* ────────────────────────────────────────────────────────────
   공통 유틸
──────────────────────────────────────────────────────────── */
function showView(id) {
    document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
    document.getElementById(id)?.classList.add('active');
}

function extractMessage(data) {
    return BnkError.extract(data);
}

function togglePw(inputId, btnEl) {
    const el = document.getElementById(inputId);
    const btn = btnEl instanceof Element ? btnEl : document.getElementById(btnEl);
    if (!el) return;
    if (el.type === 'password') {
        el.type = 'text';
        btn?.classList.add('is-show');
    } else {
        el.type = 'password';
        btn?.classList.remove('is-show');
    }
}

function calcPwScore(pw) {
    let s = 0;
    if (pw.length >= 8) s++;
    if (/[A-Za-z]/.test(pw)) s++;
    if (/\d/.test(pw)) s++;
    if (/[@$!%*#?&]/.test(pw)) s++;
    return s;
}

/**
 * 비밀번호 규칙 체크리스트 실시간 갱신
 * @param {string} pw        현재 입력값
 * @param {string} rulesId   ul 요소 id
 * @returns {boolean}        4개 규칙 모두 충족 시 true
 */
function renderPwRules(pw, rulesId = 'signup-pw-rules') {
    const RULES = {
        length: { test: v => v.length >= 8 && v.length <= 50, label: '8자 이상 50자 이하' },
        letter: { test: v => /[A-Za-z]/.test(v), label: '영문 포함' },
        number: { test: v => /\d/.test(v), label: '숫자 포함' },
        special: { test: v => /[@$!%*#?&]/.test(v), label: '특수문자 포함 (@$!%*#?&)' },
    };
    let allPass = true;
    document.querySelectorAll(`#${rulesId} li`).forEach(li => {
        const rule = RULES[li.dataset.rule];
        if (!rule) return;
        const pass = rule.test(pw);
        if (!pass) allPass = false;
        li.classList.toggle('pass', pass);
        li.textContent = (pass ? '✓ ' : '✗ ') + rule.label;
    });
    return allPass;
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
        const btnEye = document.getElementById('btnPwToggle');
        btnEye?.addEventListener('click', () => togglePw('password', btnEye));
    },

    async _submit() {
        const email = document.getElementById('email')?.value.trim();
        const pw = document.getElementById('password')?.value ?? '';
        const err = document.getElementById('login-error');
        const btn = document.querySelector('#loginForm [type="submit"]');

        if (!email) { showError(err, '이메일을 입력해 주세요.'); return; }
        if (!pw) { showError(err, '비밀번호를 입력해 주세요.'); return; }
        hideError(err);
        BnkDOM.btnLoading(btn, true, '로그인 중...');

        const res = await API.post('/api/auth/login', {
            email,
            password: pw,
            deviceInfo: navigator.userAgent.substring(0, 100),
        });

        BnkDOM.btnLoading(btn, false);

        if (res.ok) {
            sessionStorage.setItem('bnk_login_at', String(Date.now()));
            try {
                const me = await API.get('/api/users/me');
                const name = me.data?.data?.name ?? me.data?.data?.email ?? '회원';
                sessionStorage.setItem('bnk_user_name', name);
            } catch {
                sessionStorage.setItem('bnk_user_name', '회원');
            }
            const next = new URLSearchParams(location.search).get('next');
            const safeNext = next?.startsWith('/') && !next.startsWith('//') ? next : '/';
            location.href = safeNext;
            return;
        }

        // ── 에러 분기 ──
        if (res.status === 0 || res.status >= 500) return;

        const custom = {
            401: extractMessage(res.data) || '이메일 또는 비밀번호가 올바르지 않습니다.',
            403: extractMessage(res.data) || '계정이 정지 또는 탈퇴 처리되었습니다.',
            423: '로그인 실패 횟수 초과로 계정이 잠겼습니다. 잠시 후 다시 시도해 주세요.',
            429: '요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.',
        };
        BnkError.handle(res, err, custom);
    },
};

/* ════════════════════════════════════════════
   회원가입  (signup.html)
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

    function syncAllCheck() {
        const form = document.getElementById('termsForm');
        const allCbs = form?.querySelectorAll('input[type="checkbox"][data-id], input[type="checkbox"][data-required]') ?? [];
        const allChecked = [...allCbs].length > 0 && [...allCbs].every(cb => cb.checked);
        const allCb = document.getElementById('terms-all');
        if (allCb) allCb.checked = allChecked;
    }

    async function _loadTerms() {
        const loading = document.getElementById('terms-loading');
        const errEl = document.getElementById('terms-error');
        const extraWrap = document.getElementById('extra-terms-wrap');
        if (!extraWrap) return;

        const res = await API.get('/api/terms/packages/SIGNUP');
        loading?.classList.add('is-hidden');

        if (!res.ok) {
            if (errEl) {
                errEl.style.display = 'block';
                errEl.textContent = '추가 약관을 불러오지 못했습니다. 필수 약관은 위에 표시된 내용으로 진행합니다.';
            }
            return;
        }

        const terms = res.data?.data?.terms ?? [];
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

            // 정적 필수 약관 체크박스 → 전체동의 상태 역동기화
            document.querySelectorAll('#termsForm input[type="checkbox"][data-required="Y"]')
                .forEach(cb => cb.addEventListener('change', syncAllCheck));

            // 전체 동의
            document.getElementById('terms-all')?.addEventListener('change', (e) => {
                const form = document.getElementById('termsForm');
                form?.querySelectorAll('input[type="checkbox"][data-id], input[type="checkbox"][data-required]')
                    .forEach(cb => { cb.checked = e.target.checked; });
            });

            // STEP1 → STEP2
            document.getElementById('btnStep1Next')?.addEventListener('click', (e) => {
                e.preventDefault();
                const requiredCbs = document.querySelectorAll('[data-required="Y"]');
                if (![...requiredCbs].every(cb => cb.checked)) {
                    authToast.error('필수 약관에 모두 동의해 주세요.');
                    return;
                }
                _agreedTermsIds = [...document.querySelectorAll('[data-id]:checked')]
                    .map(cb => Number(cb.dataset.id))
                    .filter(id => id > 0);

                if (_agreedTermsIds.length === 0) {
                    authToast.error('약관 정보를 불러올 수 없습니다. 페이지를 새로고침 해주세요.');
                    return;
                }
                showView('view-step2');
                _updateStepBar(2);
            });

            document.getElementById('btnSendCode')?.addEventListener('click', () => this._sendVerifyCode());
            document.getElementById('btnVerifyCode')?.addEventListener('click', () => this._verifyCode());

            const pwInput = document.getElementById('password');
            const pwConfirm = document.getElementById('passwordConfirm');

            document.getElementById('btnPwToggle1')?.addEventListener('click', (e) => togglePw('password', e.currentTarget));
            document.getElementById('btnPwToggle2')?.addEventListener('click', (e) => togglePw('passwordConfirm', e.currentTarget));

            // ── 비밀번호 확인: 붙여넣기 차단
            document.getElementById('passwordConfirm')?.addEventListener('paste', (e) => {
                e.preventDefault();
                authToast.warning('비밀번호 확인란에는 붙여넣기를 사용할 수 없습니다.');
            });

            document.getElementById('btnStep2Back')?.addEventListener('click', () => {
                _clearCodeTimer();
                showView('view-step1');
                _updateStepBar(1);
            });

            pwInput?.addEventListener('input', () => {
                const v = pwInput.value;
                const wrap = document.getElementById('signup-pw-strength');
                if (!v) {
                    wrap?.classList.add('is-hidden');
                    renderPwRules('');   // 전체 미충족 리셋
                    return;
                }
                // 강도 바 갱신
                wrap?.classList.remove('is-hidden');
                renderStrengthBar(calcPwScore(v), 'signup-strength-bar', 'signup-strength-label');
                // 규칙 체크리스트 갱신
                renderPwRules(v);
            });

            pwConfirm?.addEventListener('input', () => {
                const msg = document.getElementById('pw-match-msg');
                if (!msg) return;
                const c = pwConfirm.value;
                if (!c) { msg.textContent = ''; return; }
                msg.textContent = pwInput.value === c ? '비밀번호가 일치합니다.' : '비밀번호가 일치하지 않습니다.';
                msg.className = pwInput.value === c ? 'helper ok' : 'helper error';
            });

            document.getElementById('signupForm')?.addEventListener('submit', async (e) => {
                e.preventDefault();
                await this._submit();
            });

            // ── 생년월일: 14세 이상 제한 (max = 오늘 기준 -14년)
            const birthInput = document.getElementById('birthDate');
            if (birthInput) {
                const today = new Date();
                const maxDate = new Date(today.getFullYear() - 14, today.getMonth(), today.getDate());
                birthInput.max = maxDate.toISOString().split('T')[0];
            }
        },

        async _sendVerifyCode() {
            const email = document.getElementById('email')?.value.trim();
            const errEl = document.getElementById('verify-error');

            if (!email) { showError(errEl, '이메일을 먼저 입력해 주세요.'); return; }
            if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
                showError(errEl, '올바른 이메일 형식이 아닙니다. (예: example@naver.com)');
                return;
            }
            hideError(errEl);

            const btn = document.getElementById('btnSendCode');
            BnkDOM.btnLoading(btn, true, '발송 중...');
            const res = await API.post('/api/auth/send-verify-code', { email });
            BnkDOM.btnLoading(btn, false);

            if (res.ok) {
                authToast.success('인증코드가 발송되었습니다. 이메일을 확인해 주세요.');
                let remaining = 180;
                _clearCodeTimer();
                const timerEl = document.getElementById('code-timer');
                document.getElementById('code-sent-msg')?.classList.remove('is-hidden');
                if (timerEl) {
                    _codeTimer = setInterval(() => {
                        remaining--;
                        const m = String(Math.floor(remaining / 60)).padStart(2, '0');
                        const s = String(remaining % 60).padStart(2, '0');
                        timerEl.textContent = `${m}:${s}`;
                        if (remaining <= 0) { _clearCodeTimer(); timerEl.textContent = '만료됨'; }
                    }, 1000);
                }
                return;
            }

            if (res.status === 0 || res.status >= 500) return;
            BnkError.handle(res, errEl, {
                409: '이미 가입된 이메일입니다. 로그인 페이지에서 로그인해 주세요.',
                429: '인증코드 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.',
            });
        },

        async _verifyCode() {
            const email = document.getElementById('email')?.value.trim();
            const code = document.getElementById('verifyCode')?.value.trim();
            const errEl = document.getElementById('verify-error');
            const success = document.getElementById('verify-success');

            if (!code) { showError(errEl, '인증코드를 입력해 주세요.'); return; }
            if (!/^\d{6}$/.test(code)) { showError(errEl, '인증코드는 6자리 숫자입니다.'); return; }
            hideError(errEl);

            const btn = document.getElementById('btnVerifyCode');
            BnkDOM.btnLoading(btn, true, '확인 중...');
            const res = await API.post('/api/auth/verify-email', { email, code });
            BnkDOM.btnLoading(btn, false);

            if (res.ok) {
                _emailVerified = true;
                _clearCodeTimer();
                hideError(errEl);
                success?.classList.remove('is-hidden');
                return;
            }

            if (res.status === 0 || res.status >= 500) return;
            BnkError.handle(res, errEl, {
                400: '인증코드가 올바르지 않거나 만료되었습니다. 코드를 다시 발송해 주세요.',
                429: '인증 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요.',
            });
        },

        async _submit() {
            const email = document.getElementById('email')?.value.trim() ?? '';
            const pw = document.getElementById('password')?.value ?? '';
            const pwc = document.getElementById('passwordConfirm')?.value ?? '';
            const name = document.getElementById('name')?.value.trim() ?? '';
            const rawPhone = document.getElementById('phone')?.value ?? '';
            const phone = rawPhone.trim().replace(/-/g, '');
            const birthRaw = document.getElementById('birthDate')?.value ?? '';
            const birth = birthRaw ? birthRaw.replace(/-/g, '') : null; // "2000-01-01" → "20000101"
            const mkt = document.getElementById('marketingAgree')?.checked ?? false;
            const job = document.getElementById('job')?.value ?? '';
            const incomeLevel = document.getElementById('incomeLevelCode')?.value ?? '';
            const csStr = document.getElementById('creditScore')?.value;
            const creditScore = csStr ? Number(csStr) : null;
            const err = document.getElementById('signup-error');
            const btn = document.getElementById('btnSignup') ?? document.querySelector('#signupForm [type="submit"]');

            // submit 시점 재수집 (STEP1 건너뜀 방어)
            const currentAgreed = [...document.querySelectorAll('[data-id]:checked')]
                .map(cb => Number(cb.dataset.id)).filter(id => id > 0);
            if (currentAgreed.length > 0) _agreedTermsIds = currentAgreed;

            // 클라이언트 사전 검증
            // 이전 인라인 에러 초기화
            ['email', 'verifyCode', 'password', 'passwordConfirm', 'name', 'phone', 'birthDate', 'creditScore'].forEach(f => {
                const el = document.getElementById(`${f}-err`);
                if (el) hideError(el);
            });

            const checks = [
                [!email, '이메일을 입력해 주세요.', 'email'],
                [!_emailVerified, '이메일 인증을 완료해 주세요.', 'verifyCode'],
                [pw.length < 8, '비밀번호는 8자 이상 입력해 주세요.', 'password'],
                [pw !== (document.getElementById('passwordConfirm')?.value ?? ''), '비밀번호가 일치하지 않습니다.', 'passwordConfirm'],
                [!name, '이름을 입력해 주세요.', 'name'],
                [!phone, '휴대전화 번호를 입력해 주세요.', 'phone'],
                [phone && !/^01[0-9]{8,9}$/.test(phone), '올바른 휴대폰 번호 형식이 아닙니다. (예: 01012345678)', 'phone'],
                [!birthRaw, '생년월일을 입력해 주세요.', 'birthDate'],
                [creditScore !== null && (creditScore < 300 || creditScore > 900), '신용점수는 300~900 사이로 입력해 주세요.', 'creditScore'],
                [_agreedTermsIds.length === 0, '약관 동의 정보가 없습니다. 이전 단계로 돌아가 약관에 동의해 주세요.', null],
            ];
            for (const [cond, msg, fieldId] of checks) {
                if (cond) {
                    if (fieldId) {
                        const inlineErr = document.getElementById(`${fieldId}-err`);
                        if (inlineErr) showError(inlineErr, msg);
                    } else {
                        showError(err, msg);
                    }
                    if (fieldId) {
                        requestAnimationFrame(() => {
                            const el = document.getElementById(fieldId);
                            el?.focus();
                            el?.scrollIntoView({ behavior: 'smooth', block: 'center' });
                        });
                    }
                    return;
                }
            }
            hideError(err);
            // 이전 400 fieldErrors 잔여 메시지 초기화
            ['email', 'verifyCode', 'password', 'passwordConfirm', 'name', 'phone', 'birthDate', 'creditScore'].forEach(f => {
                const el = document.getElementById(`${f}-err`);
                if (el) hideError(el);
            });

            BnkDOM.btnLoading(btn, true, '가입 중...');
            const res = await API.post('/api/auth/signup', {
                email,
                password: pw,
                name,
                phone,
                birthDate: birth,
                marketingAgree: mkt,
                agreedTermsIds: _agreedTermsIds,
                job: job || undefined,
                incomeLevelCode: incomeLevel || undefined,
                creditScore,
            });
            BnkDOM.btnLoading(btn, false);

            if (res.ok) {
                _clearCodeTimer();
                const doneNameEl = document.getElementById('done-name');
                const doneEmailEl = document.getElementById('done-email-display');
                if (doneNameEl) doneNameEl.textContent = name;
                if (doneEmailEl) doneEmailEl.textContent = email;
                showView('view-step3');
                _updateStepBar(3);
                return;
            }

            if (res.status === 0 || res.status >= 500) {
                showError(err, res.status === 0
                    ? '네트워크 오류가 발생했습니다. 인터넷 연결을 확인해 주세요.'
                    : '서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.');
                return;
            }
            if (res.status === 403) {
                // 이메일 인증 만료
                _emailVerified = false;
                document.getElementById('verify-success')?.classList.add('is-hidden');
                showError(err, '이메일 인증이 만료되었습니다. 이메일 인증을 다시 진행해 주세요.');
                return;
            }
            if (res.status === 400) {
                // fieldErrors → 해당 필드에 직접 에러 표시 + focus
                const fieldMap = {
                    email: 'email',
                    password: 'password',
                    name: 'name',
                    phone: 'phone',
                    birthDate: 'birthDate',
                    creditScore: 'creditScore',
                };
                const fields = res.data?.fieldErrors ?? res.data?.errors ?? [];
                let firstInput = null;
                if (Array.isArray(fields) && fields.length) {
                    fields.forEach(fe => {
                        const inputId = fieldMap[fe.field];
                        const inputEl = inputId ? document.getElementById(inputId) : null;
                        if (inputEl) {
                            // 해당 필드 바로 아래 에러 표시
                            let errEl = document.getElementById(`${inputId}-err`);
                            if (!errEl) {
                                errEl = document.createElement('div');
                                errEl.id = `${inputId}-err`;
                                errEl.className = 'alert alert-error';
                                inputEl.closest('.form-group')?.appendChild(errEl);
                            }
                            errEl.textContent = fe.message ?? fe.reason ?? '';
                            errEl.style.display = 'block';
                            if (!firstInput) firstInput = inputEl;
                        }
                    });
                    // DOM 변경이 모두 끝난 뒤 포커스 — 중간 appendChild가 포커스를 빼앗는 문제 방지
                    if (firstInput) requestAnimationFrame(() => firstInput.focus());
                    else showError(err, BnkError.extract(res.data));
                } else {
                    showError(err, BnkError.extract(res.data));
                }
                return;
            }
            BnkError.handle(res, err, {
                409: res.data?.code === 'U010'
                    ? '이미 가입된 휴대폰 번호입니다.'
                    : '이미 가입된 이메일입니다. 로그인 페이지에서 로그인해 주세요.',
                429: '요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.',
            });
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
        const name = document.getElementById('name')?.value.trim();
        const phone = document.getElementById('phone')?.value.trim().replace(/-/g, '');
        const err = document.getElementById('find-error');
        const btn = document.querySelector('#findIdForm [type="submit"]');

        if (!name) { showError(err, '이름을 입력해 주세요.'); return; }
        if (!phone) { showError(err, '휴대전화 번호를 입력해 주세요.'); return; }
        hideError(err);

        BnkDOM.btnLoading(btn, true, '조회 중...');
        const res = await API.post('/api/auth/find-id', { name, phone });
        BnkDOM.btnLoading(btn, false);

        if (res.ok) {
            const maskedEmail = res.data?.data?.maskedEmail ?? res.data?.maskedEmail ?? '–';
            const emailEl = document.getElementById('result-email');
            if (emailEl) emailEl.textContent = maskedEmail;
            showView('view-result');
            return;
        }

        if (res.status === 0 || res.status >= 500) return;
        BnkError.handle(res, err, {
            404: '이름 또는 이메일이 일치하지 않습니다. 입력 정보를 다시 확인해 주세요.',
            429: '요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.',
        });
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

        const newPw = document.getElementById('new-pw');
        const newPwConf = document.getElementById('new-pw-confirm');

        newPw?.addEventListener('input', () => this._checkStrength());
        newPwConf?.addEventListener('input', () => this._checkMatch());

        document.getElementById('btn-eye1')?.addEventListener('click', (e) => togglePw('new-pw', e.currentTarget));
        document.getElementById('btn-eye2')?.addEventListener('click', (e) => togglePw('new-pw-confirm', e.currentTarget));

        document.getElementById('resetPwForm')?.addEventListener('submit', async (e) => {
            e.preventDefault();
            await this._submit();
        });
    },

    async _requestLink() {
        const email = document.getElementById('rp-email')?.value.trim();
        const name = document.getElementById('rp-name')?.value.trim();
        const err = document.getElementById('rp-request-error');
        const btn = document.querySelector('#findPwForm [type="submit"]');

        if (!email) { showError(err, '이메일을 입력해 주세요.'); return; }
        if (!name) { showError(err, '이름을 입력해 주세요.'); return; }
        hideError(err);

        BnkDOM.btnLoading(btn, true, '발송 중...');
        const res = await API.post('/api/auth/find-password', { email, name });
        BnkDOM.btnLoading(btn, false);

        if (res.ok) {
            showView('view-sent');
            authToast.success(`${email}로 재설정 링크를 발송했습니다. 메일함을 확인해 주세요.`);
            return;
        }
        if (res.status === 0 || res.status >= 500) return;
        BnkError.handle(res, err, {
            404: '이름 또는 이메일이 일치하지 않습니다. 입력 정보를 다시 확인해 주세요.',
            429: '요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.',
        });
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
        const btn = document.querySelector('#resetPwForm [type="submit"]');

        if (pw.length < 8) { showError(err, '비밀번호는 8자 이상 입력해 주세요.'); return; }
        if (pw !== pwc) { showError(err, '비밀번호가 일치하지 않습니다.'); return; }
        hideError(err);

        BnkDOM.btnLoading(btn, true, '변경 중...');
        const res = await API.post('/api/auth/reset-password', {
            token: this._token,
            newPassword: pw,
            newPasswordConfirm: pwc,
        });
        BnkDOM.btnLoading(btn, false);

        if (res.ok) {
            authToast.success('비밀번호가 변경되었습니다. 로그인 페이지로 이동합니다.');
            setTimeout(() => { location.href = '/login'; }, 2000);
            return;
        }
        if (res.status === 0 || res.status >= 500) return;
        if (res.status === 400 || res.status === 401) {
            showView('view-token-error');
            return;
        }
        BnkError.handle(res, err);
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