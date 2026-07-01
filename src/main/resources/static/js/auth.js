/**
 * auth.js  |  BNK 부산은행 인증 페이지 (로그인 · 회원가입 · 아이디찾기 · 비밀번호재설정)
 * ─────────────────────────────────────────────────────────────────────────────
 * 의존: utils.js (BnkAPI, BnkError, BnkToast, BnkDOM)
 * 로드: utils.js → header.js → auth.js
 *
 * ─────────────────────────────────────────────────────────────────────────────
 */
'use strict';

(() => {

    /* ────────────────────────────────────────────────────────────
       로컬 단축 별칭 (utils.js 전역 객체 → IIFE 스코프 내 참조)
    ──────────────────────────────────────────────────────────── */
    const API = BnkAPI;
    const showError = BnkDOM.showError.bind(BnkDOM);
    const hideError = BnkDOM.hideError.bind(BnkDOM);
    const authToast = BnkToast;

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
			// 자동완성 강제 초기화
			setTimeout(() => {
			    document.getElementById('email').value    = '';
			    document.getElementById('password').value = '';
			}, 50);
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

            const dev = (window.BnkDevice ? BnkDevice.context() : { id: null, name: null, platform: 'WEB' });
            const res = await API.post('/api/auth/login', {
                email,
                password: pw,
                deviceInfo: navigator.userAgent.substring(0, 100),
                deviceId: dev.id,
                deviceName: dev.name,
                platform: dev.platform,
            });

            BnkDOM.btnLoading(btn, false);

			if (res.ok) {
			    const payload = res.data?.data ?? res.data;
			    if (payload?.requireDeviceVerify) {
			        // challengeToken 은 불투명 토큰. userId 는 서버가 토큰에서 도출한다.
			        sessionStorage.setItem('device_challenge_token', payload.challengeToken);
			        const next = new URLSearchParams(location.search).get('next');
			        sessionStorage.setItem('device_challenge_next', (next?.startsWith('/') && !next.startsWith('//')) ? next : '/');
			        location.href = '/auth/device-verify';
			        return;
			    }

			    sessionStorage.setItem('bnk_login_at', String(Date.now()));
			    const next = new URLSearchParams(location.search).get('next');
			    location.href = (next?.startsWith('/') && !next.startsWith('//')) ? next : '/';
			    return;
			}

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
        let _ivDone = false;
        let _emailVerified = false;
        let _codeTimer = null;

        function _clearCodeTimer() {
            if (_codeTimer) { clearInterval(_codeTimer); _codeTimer = null; }
        }

        /** 코드 인증 성공 처리 */
        function _markEmailVerified() {
            if (_emailVerified) return;
            _emailVerified = true;
            _clearCodeTimer();
            authToast.success('이메일 인증이 완료되었습니다.');
            document.getElementById('verifyCode')?.setAttribute('disabled', '');
            document.getElementById('btnVerifyCode')?.setAttribute('disabled', '');
            document.getElementById('verify-success')?.classList.remove('is-hidden');
        }

        function _updateStepBar(step) {
            [1, 2, 3].forEach(n => {
                document.getElementById(`step-dot-${n}`)?.classList.toggle('active', n === step);
                document.getElementById(`step-dot-${n}`)?.classList.toggle('done', n < step);
            });
        }

        function syncAllCheck() {
            const form = document.getElementById('termsForm');
            // data-required="false"인 선택 약관은 전체동의 판단에서 제외
            const allCbs = [...(form?.querySelectorAll('input[type="checkbox"][data-id], input[type="checkbox"][data-required]') ?? [])]
                .filter(cb => cb.dataset.required !== 'false');
            const allChecked = allCbs.length > 0 && allCbs.every(cb => cb.checked);
            const allCb = document.getElementById('terms-all');
            if (allCb) allCb.checked = allChecked;
        }

        return {
            init() {
                const allCb = document.getElementById('terms-all');

                // 전체동의 → 개별 체크박스 일괄 토글
                allCb?.addEventListener('change', () => {
                    const form = document.getElementById('termsForm');
                    const allCbs = [...(form?.querySelectorAll('input[type="checkbox"][data-id], input[type="checkbox"][data-required]') ?? [])]
                        .filter(cb => cb.dataset.required !== 'false');
                    allCbs.forEach(cb => { cb.checked = allCb.checked; });
                });

                document.getElementById('termsForm')?.addEventListener('change', (e) => {
                    // 전체동의 체크박스 자체 변경은 제외 (무한 루프 방지)
                    if (e.target.matches('input[type="checkbox"]') && e.target !== allCb) {
                        syncAllCheck();
                    }
                });

                document.getElementById('btnStep1Next')?.addEventListener('click', () => {
                    const requiredCbs = document.querySelectorAll(
                        'input[type="checkbox"][data-required="Y"], input[type="checkbox"][data-required="true"]'
                    );
                    if (![...requiredCbs].every(cb => cb.checked)) {
                        authToast.error('필수 약관에 모두 동의해 주세요.');
                        return;
                    }
                    _agreedTermsIds = [...document.querySelectorAll('[data-id]:checked')]
                        .map(cb => Number(cb.dataset.id))
                        .filter(id => id > 0);

                    // 필수 약관 ID 존재 여부로 체크 (선택 약관 미체크는 허용)
                    const requiredCheckedIds = [...document.querySelectorAll(
                        'input[type="checkbox"][data-required="Y"]:checked, input[type="checkbox"][data-required="true"]:checked'
                    )].map(cb => Number(cb.dataset.id)).filter(id => id > 0);

                    if (requiredCheckedIds.length === 0) {
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

                // 브라우저 자동완성 차단: readonly → focus 시 제거
                [pwInput, pwConfirm].forEach(el => {
                    el?.addEventListener('focus', () => el.removeAttribute('readonly'));
                });

                document.getElementById('btnPwToggle1')?.addEventListener('click', (e) => togglePw('password', e.currentTarget));
                document.getElementById('btnPwToggle2')?.addEventListener('click', (e) => togglePw('passwordConfirm', e.currentTarget));

                document.getElementById('passwordConfirm')?.addEventListener('paste', (e) => {
                    e.preventDefault();
                    authToast.warning('비밀번호 확인란에는 붙여넣기를 사용할 수 없습니다.');
                });

                document.getElementById('btnStep2Back')?.addEventListener('click', () => {
                    _clearCodeTimer();
                    showView('view-step1');
                    _updateStepBar(1);
                });

                //본인 인증 버튼
                document.getElementById('btnIdentityVerify')?.addEventListener('click', () => {
                    IdentityVerify.open({
                        onSuccess: (result) => {
                            // 이름, 생년월일 자동 입력
                            const nameEl = document.getElementById('name');
                            const bdEl = document.getElementById('birthDate');
                            if (nameEl) nameEl.value = result.name || '';
                            if (bdEl) bdEl.value = result.birthDate || '';

                            // 숨겨진 필드 저장
                            const set = (id, val) => {
                                const el = document.getElementById(id);
                                if (el) el.value = val || '';
                            };
                            set('iv-resident-front', result.residentFront);
							set('iv-gender-code',    result.genderCode);
                            set('iv-address', result.address);
                            set('iv-address-detail', result.addressDetail);
                            set('iv-zip-code', result.zipCode);

                            // 완료 UI 업데이트 — 클래스로만 처리 (인라인 CSS 없음)
                            const msg = document.getElementById('iv-done-msg');
                            if (msg) {
                                msg.textContent = '✓ 본인인증이 완료되었습니다.';
                                msg.classList.add('iv-complete-msg');  // display: block 트리거
                                msg.style.display = 'block';           // CSS display:none 덮어쓰기용
                            }

                            const btn = document.getElementById('btnIdentityVerify');
                            if (btn) {
                                btn.textContent = '✓ 본인인증 완료';
                                btn.classList.add('is-done');
                            }

                            _ivDone = true;
                        }
                    });
                });

                pwInput?.addEventListener('input', () => {
                    const v = pwInput.value;
                    const wrap = document.getElementById('signup-pw-strength');
                    if (!v) { wrap?.classList.add('is-hidden'); renderPwRules(''); return; }
                    wrap?.classList.remove('is-hidden');
                    renderStrengthBar(calcPwScore(v), 'signup-strength-bar', 'signup-strength-label');
                    renderPwRules(v);
                    hideError(document.getElementById('signup-error'));
                });

                pwConfirm?.addEventListener('input', () => {
                    const msg = document.getElementById('pw-match-msg');
                    if (!msg) return;
                    const c = pwConfirm.value;
                    if (!c) { msg.textContent = ''; return; }
                    msg.textContent = pwInput.value === c ? '비밀번호가 일치합니다.' : '비밀번호가 일치하지 않습니다.';
                    msg.className = pwInput.value === c ? 'helper ok' : 'helper error';
                    hideError(document.getElementById('signup-error'));
                });

                // 입력 시 에러 메시지 자동 클리어 — 필드별 + 공통
                const errClearMap = {
                    'name': ['name-err', 'signup-error'],
                    'phone': ['phone-err', 'signup-error'],
                    'email': ['email-err', 'signup-error'],
                    'verifyCode': ['verifyCode-err'],
                    'creditScore': ['creditScore-err'],
                    'birthDate': ['birthDate-err'],
                };
                Object.entries(errClearMap).forEach(([fieldId, errIds]) => {
                    document.getElementById(fieldId)?.addEventListener('input', () => {
                        errIds.forEach(id => hideError(document.getElementById(id)));
                    });
                });

                document.getElementById('btnStep2Submit')?.addEventListener('click', () => this._submitSignup());
            },

            async _sendVerifyCode() {
                const email = document.getElementById('email')?.value.trim();
                const err = document.getElementById('email-err');
                if (!email) { showError(err, '이메일을 입력해 주세요.'); return; }
                hideError(err);

                const res = await API.post('/api/auth/send-verify-code', { email });
                if (res.ok) {
                    authToast.success('인증 코드가 발송되었습니다. 메일의 코드를 입력해 주세요.');
                    document.getElementById('code-sent-msg')?.classList.remove('is-hidden');
                    this._startCodeTimer();
                } else {
                    BnkError.handle(res, err, {
                        409: '이미 가입된 이메일입니다. 로그인 페이지에서 로그인해 주세요.',
                        429: '요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.',
                    });
                }
            },

            _startCodeTimer() {
                _clearCodeTimer();
                let remaining = 600;   // ← 180 → 600
                const timerEl = document.getElementById('code-timer');
                function tick() {
                    if (!timerEl) { _clearCodeTimer(); return; }
                    const m = Math.floor(remaining / 60);
                    const s = remaining % 60;
                    timerEl.textContent = `${m}:${String(s).padStart(2, '0')}`;
                    if (remaining-- <= 0) {
                        _clearCodeTimer();
                        timerEl.textContent = '만료됨';
                        // _emailVerified 건드리지 않음 — 서버 코드는 아직 유효
                        document.getElementById('code-sent-msg')
                            ?.querySelector('.timer-text')
                            ?.closest('.helper')
                            ?.classList.add('is-hidden');
                    }
                }
                tick();
                _codeTimer = setInterval(tick, 1000);
            },

            async _verifyCode() {
                const email = document.getElementById('email')?.value.trim();
                const code = document.getElementById('verifyCode')?.value.trim();
                const err = document.getElementById('verifyCode-err');
                if (!code) { showError(err, '인증 코드를 입력해 주세요.'); return; }

                const res = await API.post('/api/auth/verify-email', { email, code });
                if (res.ok) {
                    _markEmailVerified();
                } else {
                    BnkError.handle(res, err, {
                        400: '인증 코드가 올바르지 않습니다.',
                        410: '인증 코드가 만료되었습니다. 재발송해 주세요.',
                    });
                }
            },

            async _submitSignup() {
                if (!_emailVerified) { authToast.error('이메일 인증을 완료해 주세요.'); return; }
                if (!_ivDone) { authToast.error('본인인증을 완료해 주세요.'); return; }
                const marketingAgree = document.getElementById('chk-marketing')?.checked ?? false;
                const email = document.getElementById('email')?.value.trim();  // ① signupEmail → email
                const name = document.getElementById('name')?.value.trim();
                const phone = document.getElementById('phone')?.value.trim().replace(/-/g, '');
                const password = document.getElementById('password')?.value ?? '';
                const pwConfirm = document.getElementById('passwordConfirm')?.value ?? '';
                const birthDate = document.getElementById('birthDate')?.value ?? '';
                const err = document.getElementById('signup-error');  // ② step2-err → signup-error
				const residentFront = document.getElementById('iv-resident-front')?.value || '';
				const genderCode    = document.getElementById('iv-gender-code')?.value || '';
				const address       = document.getElementById('iv-address')?.value || '';

                if (!name) { showError(err, '이름을 입력해 주세요.'); return; }
                if (!phone) { showError(err, '휴대전화 번호를 입력해 주세요.'); return; }
                if (password !== pwConfirm) { showError(err, '비밀번호가 일치하지 않습니다.'); return; }
                if (!renderPwRules(password)) { showError(err, '비밀번호 조건을 확인해 주세요.'); return; }
                hideError(err);

                const btn = document.getElementById('btnStep2Submit');
                BnkDOM.btnLoading(btn, true, '가입 중...');

				const res = await API.post('/api/auth/signup', {
				    email, name, phone, password,
				    passwordConfirm: pwConfirm,
				    birthDate,
				    marketingAgree,
				    agreedTermsIds: _agreedTermsIds,
				    residentFront,
				    genderCode,
				    address,
				});

                BnkDOM.btnLoading(btn, false);

                if (res.ok) {
                    // ③ 완료 화면 이름·이메일 채우기 추가
                    document.getElementById('done-name').textContent = name;
                    document.getElementById('done-email-display').textContent = email;
                    showView('view-step3');
                    _updateStepBar(3);
                    return;
                }
                if (res.status === 0 || res.status >= 500) return;
                BnkError.handle(res, err, {
                    403: '서비스 이용이 제한된 계정입니다. 고객센터에 문의해 주세요.',
                    409: res.data?.message?.includes('phone')
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
                404: '이름 또는 휴대전화 번호가 일치하지 않습니다. 입력 정보를 다시 확인해 주세요.',
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
            if (!/^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,50}$/.test(pw)) {
                showError(err, '비밀번호는 영문, 숫자, 특수문자(@$!%*#?&)를 포함한 8~50자여야 합니다.');
                return;
            }
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
                setTimeout(() => { location.href = '/auth/login'; }, 2000);
                return;
            }
            if (res.status === 0 || res.status >= 500) return;
            if (res.status === 400 || res.status === 401) { showView('view-token-error'); return; }
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

})();