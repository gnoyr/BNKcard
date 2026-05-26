'use strict';

/* ════════════════════════════════════════════
   공통 유틸
════════════════════════════════════════════ */

/**
 * [개선] 네이밍 통일: api() → API (mypage.js 의 API 객체와 동일 컨벤션)
 * credentials:'include' 고정 — HttpOnly 쿠키 자동 전송
 */
const API = {
  async request(method, url, body) {
    const opts = {
      method,
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
    };
    if (body) opts.body = JSON.stringify(body);

    try {
      const res  = await fetch(url, opts);
      const data = await res.json().catch(() => ({}));
      return { ok: res.ok, status: res.status, data };
    } catch {
      return { ok: false, status: 0, data: {} };
    }
  },
  get:   (url)       => API.request('GET',    url),
  post:  (url, body) => API.request('POST',   url, body),
  put:   (url, body) => API.request('PUT',    url, body),
  patch: (url, body) => API.request('PATCH',  url, body),
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

/** 비밀번호 표시/숨김 토글 */
function togglePw(inputId, btnEl) {
  const el  = document.getElementById(inputId);
  if (!el) return;
  const btn = btnEl instanceof Element
    ? btnEl
    : (btnEl ? document.getElementById(btnEl) : el.parentElement.querySelector('.pw-toggle'));
  if (el.type === 'password') {
    el.type = 'text';
    btn?.classList.add('is-visible');
  } else {
    el.type = 'password';
    btn?.classList.remove('is-visible');
  }
}

/** 비밀번호 강도 점수 계산 (0~5) */
function calcPwScore(pw) {
  let score = 0;
  if (pw.length >= 8)           score++;
  if (pw.length >= 12)          score++;
  if (/[A-Z]/.test(pw))         score++;
  if (/[0-9]/.test(pw))         score++;
  if (/[@$!%*#?&]/.test(pw))    score++;
  return score;
}

/** 강도 바 렌더링 */
function renderStrengthBar(score, barId, labelId) {
  const fill  = document.getElementById(barId);
  const label = document.getElementById(labelId);
  if (!fill || !label) return;
  fill.className = 'strength-fill';
  if      (score <= 1) { fill.classList.add('fill-weak');   label.textContent = '보안 강도: 약함'; }
  else if (score <= 3) { fill.classList.add('fill-medium'); label.textContent = '보안 강도: 보통'; }
  else                 { fill.classList.add('fill-strong'); label.textContent = '보안 강도: 강함'; }
}


/* ════════════════════════════════════════════
   로그인  (login.html)
   POST /api/auth/login  { email, password, deviceInfo }
   → HttpOnly 쿠키 발급
════════════════════════════════════════════ */

const login = {
  init() {
    const form      = document.getElementById('loginForm');
    const btnToggle = document.getElementById('btnPwToggle');

    // [개선] onclick 제거 → addEventListener
    form?.addEventListener('submit', async (e) => {
      e.preventDefault();
      await this._submit();
    });

    btnToggle?.addEventListener('click', () => {
      togglePw('password', btnToggle);
    });
  },

  async _submit() {
    const email = document.getElementById('email').value.trim();
    const pw    = document.getElementById('password').value;
    const err   = document.getElementById('login-error');

    if (!email || !pw) {
      showError(err, '아이디와 비밀번호를 모두 입력해 주세요.');
      return;
    }
    hideError(err);

    const res = await API.post('/api/auth/login', {
      email,
      password:   pw,
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
          POST /api/auth/signup           { email, password, name, phone,
                                            birthDate, marketingAgree, agreedTermsIds }
════════════════════════════════════════════ */

const signup = (() => {
  let _agreedTermsIds = [];
  let _emailVerified  = false;
  let _codeTimer      = null;  // [개선] 타이머 참조 보관 → clearInterval 보장

  function _updateStepBar(step) {
    for (let i = 1; i <= 3; i++) {
      const dot = document.getElementById(`step-dot-${i}`);
      const num = document.getElementById(`step-num-${i}`);
      if (!dot || !num) continue;
      dot.classList.remove('active', 'done');
      if (i < step)        { dot.classList.add('done');   num.textContent = '✓'; }
      else if (i === step) { dot.classList.add('active'); num.textContent = i;   }
      else                 {                              num.textContent = i;   }
    }
  }

  /** [개선] step 전환 시 타이머 정리 */
  function _clearCodeTimer() {
    if (_codeTimer) {
      clearInterval(_codeTimer);
      _codeTimer = null;
    }
  }

  /** GET /api/terms/packages/SIGNUP — 약관 목록 동적 렌더링 */
  async function _loadTerms() {
    const loading = document.getElementById('terms-loading');
    const errEl   = document.getElementById('terms-error');
    const list    = document.getElementById('terms-list');
    if (!list) return;

    const res = await API.get('/api/terms/packages/SIGNUP');

    loading?.classList.add('is-hidden');

    if (!res.ok) {
      showError(errEl, '약관을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.');
      return;
    }

    const terms = res.data?.data?.terms ?? [];
    if (terms.length === 0) {
      showError(errEl, '약관 정보가 없습니다. 관리자에게 문의해 주세요.');
      return;
    }

    // 전체 동의 체크박스
    const allDiv = document.createElement('div');
    allDiv.className = 'terms-item terms-item--all';
    allDiv.innerHTML = `
      <label class="terms-check-row terms-check-row--all">
        <input type="checkbox" id="terms-all" />
        <span>전체 동의</span>
      </label>`;
    list.appendChild(allDiv);

    terms.forEach(t => {
      const div = document.createElement('div');
      div.className = 'terms-item';
      div.innerHTML = `
        <label class="terms-check-row">
          <input type="checkbox" id="terms-${t.termsId}"
            data-id="${t.termsId}" data-required="${t.requiredYn ?? t.required_yn ?? 'N'}" />
          <span>${t.title}</span>
          <span class="tag ${(t.requiredYn ?? t.required_yn) === 'Y' ? '' : 'opt'}">
            ${(t.requiredYn ?? t.required_yn) === 'Y' ? '필수' : '선택'}
          </span>
        </label>`;
      list.appendChild(div);
    });

    // 전체 동의 토글
    document.getElementById('terms-all')?.addEventListener('change', (e) => {
      list.querySelectorAll('input[data-id]').forEach(cb => {
        cb.checked = e.target.checked;
      });
    });
  }

  /** Step1: 필수 약관 체크 → Step2 진입 */
  function _step1Next() {
    const errEl = document.getElementById('terms-error');
    const required = document.querySelectorAll('#terms-list input[data-required="Y"]');
    const allChecked = [...required].every(cb => cb.checked);

    if (!allChecked) {
      showError(errEl, '필수 약관에 모두 동의해 주세요.');
      return false;
    }
    hideError(errEl);

    _agreedTermsIds = [...document.querySelectorAll('#terms-list input[data-id]:checked')]
      .map(cb => Number(cb.dataset.id));

    return true;
  }

  return {
    init() {
      _loadTerms();

      // ── Step1 폼 ──────────────────────────────────────────
      // [개선] onclick 제거 → addEventListener
      document.getElementById('termsForm')?.addEventListener('submit', (e) => {
        e.preventDefault();
        if (_step1Next()) {
          showView('view-step2');
          _updateStepBar(2);
        }
      });

      // ── Step2 폼 ──────────────────────────────────────────
      const btnSendCode  = document.getElementById('btnSendCode');
      const btnVerify    = document.getElementById('btnVerifyCode');
      const btnPwToggle1 = document.getElementById('btnPwToggle1');
      const btnPwToggle2 = document.getElementById('btnPwToggle2');
      const btnStep2Back = document.getElementById('btnStep2Back');
      const pwInput      = document.getElementById('password');
      const pwConfirm    = document.getElementById('passwordConfirm');

      // [개선] onclick 제거 → addEventListener
      btnSendCode?.addEventListener('click', () => this._sendVerifyCode());
      btnVerify?.addEventListener('click',   () => this._verifyEmail());
      btnPwToggle1?.addEventListener('click', () => togglePw('password',        btnPwToggle1));
      btnPwToggle2?.addEventListener('click', () => togglePw('passwordConfirm', btnPwToggle2));

      // [개선] Step2 이전 버튼 — clearInterval 포함
      btnStep2Back?.addEventListener('click', () => {
        _clearCodeTimer();           // [개선] 타이머 정리
        showView('view-step1');
        _updateStepBar(1);
      });

      // 비밀번호 강도 실시간 표시
      pwInput?.addEventListener('input', () => {
        const wrap = document.getElementById('signup-pw-strength');
        const v = pwInput.value;
        if (!v) { wrap?.classList.add('is-hidden'); return; }
        wrap?.classList.remove('is-hidden');
        renderStrengthBar(calcPwScore(v), 'signup-strength-bar', 'signup-strength-label');
      });

      // 비밀번호 확인 일치 표시
      pwConfirm?.addEventListener('input', () => {
        const msg = document.getElementById('pw-match-msg');
        if (!msg) return;
        const c = pwConfirm.value;
        if (!c) { msg.textContent = ''; return; }
        msg.textContent = pwInput.value === c ? '비밀번호가 일치합니다.' : '비밀번호가 일치하지 않습니다.';
        msg.className   = pwInput.value === c ? 'helper ok' : 'helper error';
      });

      // 회원가입 최종 제출
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

      const res = await API.post('/api/auth/send-verify-code', { email });
      if (!res.ok) { showError(errEl, extractMessage(res.data)); return; }

      // 타이머 시작 — [개선] 기존 타이머 먼저 정리 후 재시작
      _clearCodeTimer();
      document.getElementById('code-sent-msg')?.classList.remove('is-hidden');

      let remaining = 10 * 60; // 10분
      const timerEl = document.getElementById('code-timer');

      function tick() {
        if (!timerEl) return;
        const m = String(Math.floor(remaining / 60)).padStart(2, '0');
        const s = String(remaining % 60).padStart(2, '0');
        timerEl.textContent = `(${m}:${s})`;
        if (remaining-- <= 0) {
          _clearCodeTimer();
          timerEl.textContent = '(만료됨)';
        }
      }
      tick();
      _codeTimer = setInterval(tick, 1000);
    },

    /** POST /api/auth/verify-email */
    async _verifyEmail() {
      const email   = document.getElementById('email')?.value.trim();
      const code    = document.getElementById('verifyCode')?.value.trim();
      const errEl   = document.getElementById('verify-error');
      const success = document.getElementById('verify-success');

      if (!code) { showError(errEl, '인증코드를 입력해 주세요.'); return; }
      hideError(errEl);

      const res = await API.post('/api/auth/verify-email', { email, code });
      if (res.ok) {
        _emailVerified = true;
        _clearCodeTimer();  // [개선] 인증 성공 시 타이머 정리
        success?.classList.remove('is-hidden');
        document.getElementById('code-sent-msg')?.classList.add('is-hidden');
      } else {
        showError(errEl, extractMessage(res.data));
      }
    },

    /** POST /api/auth/signup */
    async _submit() {
      const email = document.getElementById('email')?.value.trim();
      const pw    = document.getElementById('password')?.value;
      const pwc   = document.getElementById('passwordConfirm')?.value;
      const name  = document.getElementById('name')?.value.trim();
      const phone = document.getElementById('phone')?.value.trim().replace(/-/g, '');
      const birth = document.getElementById('birthDate')?.value.replace(/-/g, '') || null;
      const mkt   = document.getElementById('marketingAgree')?.checked ? 'Y' : 'N';
      const err   = document.getElementById('signup-error');

      const checks = [
        [!email,          '이메일을 입력해 주세요.'],
        [!_emailVerified, '이메일 인증을 완료해 주세요.'],
        [pw.length < 8,   '비밀번호는 8자 이상 입력해 주세요.'],
        [pw !== pwc,      '비밀번호가 일치하지 않습니다.'],
        [!name,           '이름을 입력해 주세요.'],
        [!phone,          '휴대전화 번호를 입력해 주세요.'],
      ];
      for (const [cond, msg] of checks) {
        if (cond) { showError(err, msg); return; }
      }
      hideError(err);

      const res = await API.post('/api/auth/signup', {
        email,
        password:       pw,
        name,
        phone,
        birthDate:      birth,
        marketingAgree: mkt,
        agreedTermsIds: _agreedTermsIds,
      });

      if (res.ok) {
        _clearCodeTimer();  // [개선] 가입 완료 시 타이머 정리
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
   POST /api/auth/find-id { name, phone }
   → { maskedEmail }
════════════════════════════════════════════ */

const findId = {
  init() {
    // [개선] onclick 제거 → addEventListener
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
    } else {
      showError(err, extractMessage(res.data));
    }
  },
};


/* ════════════════════════════════════════════
   비밀번호 재설정  (reset-password.html)

   [링크 요청]
   POST /api/auth/find-password { email, name }
   → 이메일로 reset 링크 발송 (링크: /auth/reset-password.html?token=xxx)

   [실제 변경 — URL ?token= 감지 시 자동 표시]
   POST /api/auth/reset-password { token, newPassword, newPasswordConfirm }
════════════════════════════════════════════ */

const resetPw = {
  _token: null,

  init() {
    // URL ?token= 파라미터 감지 → view-new-pw 자동 표시
    const token = new URLSearchParams(window.location.search).get('token');
    if (token) {
      this._token = token;
      showView('view-new-pw');
    }

    // ── 링크 요청 폼 ──────────────────────────────────────
    // [개선] onclick 제거 → addEventListener
    document.getElementById('findPwForm')?.addEventListener('submit', async (e) => {
      e.preventDefault();
      await this._requestLink();
    });

    // ── 새 비밀번호 설정 폼 ───────────────────────────────
    const newPw     = document.getElementById('new-pw');
    const newPwConf = document.getElementById('new-pw-confirm');
    const btnEye1   = document.getElementById('btn-eye1');
    const btnEye2   = document.getElementById('btn-eye2');

    // [개선] oninput 인라인 이벤트 제거 → addEventListener
    newPw?.addEventListener('input', () => this._checkStrength());
    newPwConf?.addEventListener('input', () => this._checkMatch());

    // [개선] onclick 제거 → addEventListener
    btnEye1?.addEventListener('click', () => togglePw('new-pw',         btnEye1));
    btnEye2?.addEventListener('click', () => togglePw('new-pw-confirm', btnEye2));

    document.getElementById('resetPwForm')?.addEventListener('submit', async (e) => {
      e.preventDefault();
      await this._submit();
    });
  },

  /** POST /api/auth/find-password — 재설정 링크 발송 */
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

  /** POST /api/auth/reset-password */
  async _submit() {
    const pw  = document.getElementById('new-pw')?.value ?? '';
    const pwc = document.getElementById('new-pw-confirm')?.value ?? '';
    const err = document.getElementById('reset-error');

    if (pw.length < 8) { showError(err, '비밀번호는 8자 이상 입력해 주세요.'); return; }
    if (pw !== pwc)    { showError(err, '비밀번호가 일치하지 않습니다.'); return; }
    hideError(err);

    const res = await API.post('/api/auth/reset-password', {
      token:              this._token,
      newPassword:        pw,
      newPasswordConfirm: pwc,
    });

    if (res.ok) {
      alert('비밀번호가 변경되었습니다.\n다시 로그인해 주세요.');
      window.location.href = 'login.html';
    } else {
      if (res.status === 400 || res.status === 401) {
        showView('view-token-error');
      } else {
        showError(err, extractMessage(res.data));
      }
    }
  },
};


/* ════════════════════════════════════════════
   페이지 초기화
   [개선] 각 객체의 init() 으로 모든 이벤트 바인딩 처리
════════════════════════════════════════════ */
document.addEventListener('DOMContentLoaded', () => {
  const page = document.body.dataset.page;

  if (page === 'login')          login.init();
  if (page === 'signup')         signup.init();
  if (page === 'find-id')        findId.init();
  if (page === 'reset-password') resetPw.init();
});
