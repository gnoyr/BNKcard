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

            if (res.status >= 500) {
                authToast('서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.', 'error');
            }

            return { ok: res.ok, status: res.status, data };
        } catch {
            authToast('서버에 연결할 수 없습니다. 네트워크를 확인해 주세요.', 'error');
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
    el.style.display = 'block';
    el.classList.add('show');
}

function hideError(el) {
    if (!el) return;
    el.textContent = '';
    el.style.display = '';
    el.classList.remove('show');
}

function showView(id) {
    document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
    document.getElementById(id)?.classList.add('active');
}

/**
 * GlobalExceptionHandler 응답 구조에서 사용자 메시지 추출
 * 우선순위: fieldErrors[0] > detail > message > data.message > fallback
 * fe.message 우선, fe.reason fallback (ErrorResponse.FieldError 필드명 통일)
 */
function extractMessage(data) {
    const fieldErrors = data?.fieldErrors ?? data?.errors ?? [];
    if (fieldErrors.length) {
        return fieldErrors
            .map(fe => {
                const msg = fe.message ?? fe.reason ?? '';
                return fe.field ? `[${fe.field}] ${msg}` : msg;
            })
            .join('\n');
    }
    return data?.detail
        || data?.message
        || data?.data?.message
        || '오류가 발생했습니다. 다시 시도해 주세요.';
}

function handleApiError(res, errEl, handlers = {}) {
    if (res.ok) return false;

    // 네트워크 단절 — API.request에서 이미 Toast 처리
    if (res.status === 0) return true;

    // 5xx — API.request에서 이미 Toast 처리
    if (res.status >= 500) return true;

    // 커스텀 핸들러 우선
    if (handlers[res.status]) {
        showError(errEl, handlers[res.status]);
        return true;
    }

    // 상태코드별 기본 메시지
    const defaultMessages = {
        400: extractMessage(res.data),
        403: extractMessage(res.data) || '접근 권한이 없습니다.',
        404: extractMessage(res.data) || '요청한 정보를 찾을 수 없습니다.',
        409: extractMessage(res.data) || '이미 사용 중인 정보입니다.',
        423: '계정이 잠겼습니다. 잠시 후 다시 시도해 주세요.',
        429: '요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.',
    };

    const msg = defaultMessages[res.status] ?? extractMessage(res.data);
    showError(errEl, msg);
    return true;
}

function authToast(msg, type = 'info') {
    if (typeof window.showToast === 'function') {
        window.showToast(msg, type);
        return;
    }

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
        const btnEye = document.getElementById('btnPwToggle');
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
            sessionStorage.setItem('bnk_login_at', String(Date.now()));

            try {
                const me = await API.get('/api/users/me');

                if (me.ok) {
                    const name =
                        me.data?.data?.name ||
                        me.data?.data?.userName ||
                        me.data?.data?.email ||
                        '회원';

                    sessionStorage.setItem('bnk_user_name', name);
                } else {
                    sessionStorage.setItem('bnk_user_name', '회원');
                }
            } catch (e) {
                sessionStorage.setItem('bnk_user_name', '회원');
            }

            const next = new URLSearchParams(location.search).get('next');
            const safeNext = next && next.startsWith('/') && !next.startsWith('//') ? next : '/';

            window.location.href = safeNext;
        } else if (res.status === 0) {
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
        const allRequired = document.querySelectorAll('[data-required="Y"]');
        const allChecked = [...allRequired].every(cb => cb.checked);
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

            const allCb = document.getElementById('terms-all');
            allCb?.addEventListener('change', () => {
                document.querySelectorAll('[data-required], [data-id]').forEach(cb => {
                    cb.checked = allCb.checked;
                });
            });

            document.getElementById('btnStep1Next')?.addEventListener('click', (e) => {
                e.preventDefault();

                const requiredCbs = document.querySelectorAll('[data-required="Y"]');
                const allAgreed = [...requiredCbs].every(cb => cb.checked);
                if (!allAgreed) {
                    authToast('필수 약관에 모두 동의해 주세요.', 'error');
                    return;
                }

                // filter(id > 0)으로 NaN/0 방어
                _agreedTermsIds = [...document.querySelectorAll('[data-id]:checked')]
                    .map(cb => Number(cb.dataset.id))
                    .filter(id => id > 0);

                //  약관 ID 수집 실패 가드 (필수 약관이 체크됐지만 ID 파싱 실패한 경우)
                if (_agreedTermsIds.length === 0) {
                    authToast('약관 정보를 불러올 수 없습니다. 페이지를 새로고침 해주세요.', 'error');
                    return;
                }

                showView('view-step2');
                _updateStepBar(2);
            });

            document.getElementById('btnSendCode')?.addEventListener('click', () => this._sendVerifyCode());
            document.getElementById('btnVerifyCode')?.addEventListener('click', () => this._verifyCode());

            const pwInput = document.getElementById('password');
            const pwConfirm = document.getElementById('passwordConfirm');
            const btnPwToggle1 = document.getElementById('btnPwToggle1');
            const btnPwToggle2 = document.getElementById('btnPwToggle2');
            const btnStep2Back = document.getElementById('btnStep2Back');

            btnPwToggle1?.addEventListener('click', () => togglePw('password', btnPwToggle1));
            btnPwToggle2?.addEventListener('click', () => togglePw('passwordConfirm', btnPwToggle2));

            btnStep2Back?.addEventListener('click', () => {
                _clearCodeTimer();
                showView('view-step1');
                _updateStepBar(1);
            });

            pwInput?.addEventListener('input', () => {
                const wrap = document.getElementById('signup-pw-strength');
                const v = pwInput.value;
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
                msg.className = pwInput.value === c ? 'helper ok' : 'helper error';
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

            // 이메일 형식 사전 검증 (서버 왕복 없이 즉시 차단)
            if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
                showError(errEl, '올바른 이메일 형식이 아닙니다. (예: example@naver.com)');
                return;
            }

            hideError(errEl);

            const res = await API.post('/api/auth/send-verify-code', { email });

            if (res.ok) {
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
                return;
            }

            // 상태코드별 명시적 분기
            if (res.status === 0 || res.status >= 500) return; // API.request에서 처리됨
            if (res.status === 409) {
                showError(errEl, '이미 가입된 이메일입니다. 로그인 페이지에서 로그인해 주세요.');
                return;
            }
            if (res.status === 429) {
                showError(errEl, '인증코드 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.');
                return;
            }
            showError(errEl, extractMessage(res.data));
        },

        async _verifyCode() {
            const email = document.getElementById('email')?.value.trim();
            const code = document.getElementById('verifyCode')?.value.trim();
            const errEl = document.getElementById('verify-error');
            const success = document.getElementById('verify-success');

            if (!code) { showError(errEl, '인증코드를 입력해 주세요.'); return; }

            // 6자리 숫자 형식 사전 검증
            if (!/^\d{6}$/.test(code)) {
                showError(errEl, '인증코드는 6자리 숫자입니다.');
                return;
            }

            hideError(errEl);

            const res = await API.post('/api/auth/verify-email', { email, code });

            if (res.ok) {
                _emailVerified = true;
                _clearCodeTimer();
                hideError(errEl);
                success?.classList.remove('is-hidden');
                return;
            }

            // 상태코드별 명시적 분기
            if (res.status === 0 || res.status >= 500) return;
            if (res.status === 400) {
                // U008: 인증 토큰 만료/불일치
                showError(errEl, '인증코드가 올바르지 않거나 만료되었습니다. 코드를 다시 발송해 주세요.');
                return;
            }
            if (res.status === 429) {
                showError(errEl, '인증 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요.');
                return;
            }
            showError(errEl, extractMessage(res.data));
        },

        async _submit() {
            const email = document.getElementById('email')?.value.trim();
            const pw = document.getElementById('password')?.value ?? '';
            const pwc = document.getElementById('passwordConfirm')?.value ?? '';
            const name = document.getElementById('name')?.value.trim();
            const phone = document.getElementById('phone')?.value.trim().replace(/-/g, '');
            // ✅ 수정: "yyyy-MM-dd" → "yyyyMMdd" 변환 (HTML date input 포맷 대응)
            const birthRaw = document.getElementById('birthDate')?.value;
            const birth = birthRaw ? birthRaw.replace(/-/g, '') : null;
            const mkt = document.getElementById('marketingAgree')?.checked ?? false;
            const job = document.getElementById('job')?.value ?? '';
            const incomeLevel = document.getElementById('incomeLevelCode')?.value ?? '';
            const csStr = document.getElementById('creditScore')?.value;
            const creditScore = csStr ? Number(csStr) : null;
            const err = document.getElementById('signup-error');

            // submit 시점 재수집 — STEP1 건너뜀 방어
            const currentAgreed = [...document.querySelectorAll('[data-id]:checked')]
                .map(cb => Number(cb.dataset.id))
                .filter(id => id > 0);
            if (currentAgreed.length > 0) _agreedTermsIds = currentAgreed;

            // ─── 클라이언트 사전 검증 ─────────────────────────────────────
            const checks = [
                [!email, '이메일을 입력해 주세요.'],
                [!_emailVerified, '이메일 인증을 완료해 주세요.'],
                [pw.length < 8, '비밀번호는 8자 이상 입력해 주세요.'],
                [pw !== pwc, '비밀번호가 일치하지 않습니다.'],
                [!name, '이름을 입력해 주세요.'],
                [!phone, '휴대전화 번호를 입력해 주세요.'],
                // 전화번호 형식 검증 (숫자 10~11자리)
                [phone && !/^01[0-9]{8,9}$/.test(phone), '올바른 휴대폰 번호 형식이 아닙니다. (예: 01012345678)'],
                [creditScore !== null && (creditScore < 300 || creditScore > 900), '신용점수는 300~900 사이로 입력해 주세요.'],
                // agreedTermsIds 빈 배열 가드
                [_agreedTermsIds.length === 0, '약관 동의 정보가 없습니다. 이전 단계로 돌아가 약관에 동의해 주세요.'],
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
                birthDate: birth,
                marketingAgree: mkt,
                agreedTermsIds: _agreedTermsIds,
                job: job || undefined,
                incomeLevelCode: incomeLevel || undefined,
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
                return;
            }

            // 상태코드별 명시적 분기
            if (res.status === 0 || res.status >= 500) return; // API.request에서 처리됨

            if (res.status === 400) {
                // Bean Validation 실패 or 필수 약관 미동의(T002)
                showError(err, extractMessage(res.data));
                return;
            }
            if (res.status === 403) {
                // U007: 이메일 인증 미완료 — 인증 상태 초기화 후 안내
                _emailVerified = false;
                document.getElementById('verify-success')?.classList.add('is-hidden');
                showError(err, '이메일 인증이 만료되었습니다. 이메일 인증을 다시 진행해 주세요.');
                return;
            }
            if (res.status === 409) {
                // U002: 이메일 중복 / U010: 전화번호 중복
                const code = res.data?.code;
                if (code === 'U010') {
                    showError(err, '이미 가입된 휴대폰 번호입니다. 다른 번호를 사용해 주세요.');
                } else {
                    showError(err, '이미 가입된 이메일입니다. 로그인 페이지에서 로그인해 주세요.');
                }
                return;
            }
            if (res.status === 429) {
                showError(err, '요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.');
                return;
            }

            // 그 외 — 서버 메시지 그대로 표시
            showError(err, extractMessage(res.data));
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

        if (!name) { showError(err, '이름을 입력해 주세요.'); return; }
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
        if (res.ok) {
            showView('view-sent');
        } else if (res.status === 0) {
            authToast('서버에 연결할 수 없습니다. 네트워크를 확인해 주세요.', 'error');
        } else {
            showError(err, extractMessage(res.data));
        }
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

    if (page === 'login') login.init();
    if (page === 'signup') signup.init();
    if (page === 'find-id') findId.init();
    if (page === 'reset-password') resetPw.init();
});