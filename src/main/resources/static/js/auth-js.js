/**
 * auth-js.js – BNK 부산은행 인증 프론트엔드
 *
 * API Base: /api/auth  (Spring Boot, same-origin)
 * 인증: HttpOnly 쿠키 (access_token / refresh_token) — fetch credentials:'include'
 */

'use strict';

/* ════════════════════════════════════════════
   공통 유틸
════════════════════════════════════════════ */

/** fetch 래퍼 — JSON 요청/응답, 에러 메시지 추출 */
async function api(method, url, body) {
  const opts = {
    method,
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
  };
  if (body) opts.body = JSON.stringify(body);

  const res  = await fetch(url, opts);
  const data = await res.json().catch(() => ({}));

  return { ok: res.ok, status: res.status, data };
}

/** 서버 에러 메시지 추출 */
function extractMessage(data) {
  return data?.message || data?.data?.message || '요청 처리 중 오류가 발생했습니다.';
}

function showError(el, msg) {
  el.textContent   = msg;
  el.style.display = 'block';
}

function hideError(el) {
  el.style.display = 'none';
}

function showView(activeId) {
  document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
  document.getElementById(activeId).classList.add('active');
}

function calcPwScore(pw) {
  let score = 0;
  if (pw.length >= 8)           score++;
  if (/[A-Z]/.test(pw))        score++;
  if (/[0-9]/.test(pw))        score++;
  if (/[^A-Za-z0-9]/.test(pw)) score++;
  return score;
}

function renderStrengthBar(score, barId, labelId) {
  const bar   = document.getElementById(barId);
  const label = document.getElementById(labelId);
  const pcts   = ['10%', '25%', '50%', '75%', '100%'];
  const colors = ['#C8102E', '#C8102E', '#FF7043', '#FFC107', '#2E7D32'];
  const texts  = ['매우 약함', '매우 약함', '약함', '보통', '강함'];
  bar.style.width      = pcts[score];
  bar.style.background = colors[score];
  label.textContent    = '비밀번호 강도: ' + texts[score];
}

function startTimer(seconds, timerId, onExpire) {
  const el = document.getElementById(timerId);
  let sec  = seconds;
  el.style.color = 'var(--red)';
  const id = setInterval(() => {
    sec--;
    const m = String(Math.floor(sec / 60)).padStart(2, '0');
    const s = String(sec % 60).padStart(2, '0');
    el.textContent = `${m}:${s}`;
    if (sec <= 0) {
      clearInterval(id);
      if (onExpire) onExpire(el);
    }
  }, 1000);
  return id;
}

function togglePw(inputId, btnId) {
  const el  = document.getElementById(inputId);
  const btn = btnId
    ? document.getElementById(btnId)
    : el.parentElement.querySelector('.pw-toggle');
  if (el.type === 'password') {
    el.type = 'text';
    btn.classList.add('is-visible');
  } else {
    el.type = 'password';
    btn.classList.remove('is-visible');
  }
}


/* ════════════════════════════════════════════
   로그인  (login.html)
   POST /api/auth/login  { email, password, deviceInfo }
   → HttpOnly 쿠키 발급
════════════════════════════════════════════ */

const login = {
  togglePw() {
    togglePw('password', null);
  },

  async submit() {
    const email = document.getElementById('email').value.trim();
    const pw    = document.getElementById('password').value;
    const err   = document.getElementById('login-error');

    if (!email || !pw) {
      showError(err, '아이디와 비밀번호를 모두 입력해 주세요.');
      return;
    }
    hideError(err);

    const res = await api('POST', '/api/auth/login', {
      email,
      password: pw,
      deviceInfo: navigator.userAgent.substring(0, 100),
    });

    if (res.ok) {
      window.location.href = '/';   // 로그인 성공 → 메인으로
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
  let _codeTimer      = null;

  function _updateStepBar(step) {
    for (let i = 1; i <= 3; i++) {
      const dot = document.getElementById(`step-dot-${i}`);
      const num = document.getElementById(`step-num-${i}`);
      dot.classList.remove('active', 'done');
      if (i < step)       { dot.classList.add('done');   num.textContent = '✓'; }
      else if (i === step) { dot.classList.add('active'); num.textContent = i;   }
      else                 {                              num.textContent = i;   }
    }
  }

  /** GET /api/terms/packages/SIGNUP — 약관 목록 동적 렌더링 */
  async function _loadTerms() {
    const loading = document.getElementById('terms-loading');
    const errEl   = document.getElementById('terms-error');
    const list    = document.getElementById('terms-list');

    const res = await api('GET', '/api/terms/packages/SIGNUP');

    loading.classList.add('is-hidden');

    if (!res.ok) {
      showError(errEl, '약관을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.');
      return;
    }

    const terms = res.data?.data?.terms ?? [];
    if (terms.length === 0) {
      showError(errEl, '약관 정보가 없습니다. 관리자에게 문의해 주세요.');
      return;
    }

    terms.forEach(t => {
      const item = document.createElement('div');
      item.className = 'form-group';
      item.innerHTML =
        '<div class="term-box" id="term-content-' + t.termsId + '">' +
          (t.content || t.title || '약관 내용') +
        '</div>' +
        '<div class="term-check">' +
          '<input type="checkbox" id="term-' + t.termsId + '"' +
          ' data-id="' + t.termsId + '" data-required="' + t.requiredYn + '"' +
          ' onchange="signup.clearTermError()" />' +
          '<label for="term-' + t.termsId + '">' +
            (t.requiredYn === 'Y' ? '[필수] ' : '[선택] ') + t.title +
          '</label>' +
        '</div>';
      list.appendChild(item);
    });
  }

  return {
    init() {
      if (document.getElementById('terms-list')) _loadTerms();
      // reset-password: token 파라미터 감지는 resetPw.init() 에서 처리
    },

    goToStep(step) {
      showView(`view-step${step}`);
      _updateStepBar(step);
    },

    clearTermError() {
      hideError(document.getElementById('term-error'));
    },

    /** Step1 → Step2: 필수 약관 전체 동의 확인 후 이동 */
    step1Next() {
      const required = document.querySelectorAll('#terms-list input[data-required="Y"]');
      const allChecked = [...required].every(c => c.checked);
      if (!allChecked) {
        document.getElementById('term-error').style.display = 'block';
        return;
      }
      _agreedTermsIds = [...document.querySelectorAll('#terms-list input:checked')]
        .map(c => Number(c.dataset.id));
      this.goToStep(2);
    },

    /** 이메일 입력 변경 시 인증 상태 초기화 */
    onEmailInput() {
      _emailVerified = false;
      document.getElementById('code-section').classList.add('is-hidden');
      const msg = document.getElementById('email-msg');
      msg.textContent = '이메일로 6자리 인증코드가 발송됩니다.';
      msg.className   = 'helper';
      clearInterval(_codeTimer);
    },

    /** POST /api/auth/send-verify-code */
    async sendVerifyCode() {
      const email = document.getElementById('email').value.trim();
      const msg   = document.getElementById('email-msg');
      const err   = document.getElementById('signup-error');

      if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        msg.textContent = '올바른 이메일 형식을 입력해 주세요.';
        msg.className   = 'helper error';
        return;
      }
      hideError(err);

      const res = await api('POST', '/api/auth/send-verify-code', { email });

      if (res.ok) {
        msg.textContent = '인증코드가 발송되었습니다.';
        msg.className   = 'helper ok';
        document.getElementById('code-section').classList.remove('is-hidden');
        document.getElementById('send-code-btn').textContent = '재발송';
        clearInterval(_codeTimer);
        _codeTimer = startTimer(600, 'su-timer', el => {
          el.textContent = '만료됨';
          el.style.color = 'var(--gray-400)';
        });
      } else {
        msg.textContent = extractMessage(res.data);
        msg.className   = 'helper error';
      }
    },

    /** POST /api/auth/verify-email */
    async verifyEmail() {
      const email = document.getElementById('email').value.trim();
      const code  = document.getElementById('su-code').value.trim();
      const err   = document.getElementById('signup-error');

      if (!code) { showError(err, '인증코드를 입력해 주세요.'); return; }

      const res = await api('POST', '/api/auth/verify-email', { email, code });

      if (res.ok) {
        _emailVerified = true;
        clearInterval(_codeTimer);
        document.getElementById('su-timer').textContent = '인증 완료';
        document.getElementById('su-timer').style.color = 'var(--green)';
        hideError(err);
      } else {
        showError(err, extractMessage(res.data));
      }
    },

    checkPwStrength() {
      const pw   = document.getElementById('pw').value;
      const wrap = document.getElementById('pw-strength');
      if (!pw) { wrap.classList.add('is-hidden'); return; }
      wrap.classList.remove('is-hidden');
      renderStrengthBar(calcPwScore(pw), 'strength-bar', 'strength-label');
    },

    checkPwMatch() {
      const pw  = document.getElementById('pw').value;
      const c   = document.getElementById('pw-confirm').value;
      const msg = document.getElementById('pw-match-msg');
      if (!c) { msg.textContent = ''; return; }
      msg.textContent = pw === c ? '비밀번호가 일치합니다.' : '비밀번호가 일치하지 않습니다.';
      msg.className   = pw === c ? 'helper ok' : 'helper error';
    },

    /**
     * Step2 → Step3
     * POST /api/auth/signup
     * { email, password, name, phone, birthDate, marketingAgree, agreedTermsIds }
     */
    async step2Next() {
      const err   = document.getElementById('signup-error');
      const name  = document.getElementById('name').value.trim();
      const email = document.getElementById('email').value.trim();
      const pw    = document.getElementById('pw').value;
      const pwc   = document.getElementById('pw-confirm').value;
      const phone = document.getElementById('phone').value.trim().replace(/-/g, '');
      const birth = document.getElementById('birth').value.replace(/-/g, '');
      const mkt   = document.getElementById('su-mkt').checked ? 'Y' : 'N';

      const checks = [
        [!name,          '이름을 입력해 주세요.'],
        [!email,         '이메일을 입력해 주세요.'],
        [!_emailVerified,'이메일 인증을 완료해 주세요.'],
        [pw.length < 8,  '비밀번호는 8자 이상 입력해 주세요.'],
        [pw !== pwc,     '비밀번호가 일치하지 않습니다.'],
        [!phone,         '휴대전화 번호를 입력해 주세요.'],
      ];
      for (const [cond, msg] of checks) {
        if (cond) { showError(err, msg); return; }
      }
      hideError(err);

      const res = await api('POST', '/api/auth/signup', {
        email,
        password: pw,
        name,
        phone,
        birthDate:     birth || null,
        marketingAgree: mkt,
        agreedTermsIds: _agreedTermsIds,
      });

      if (res.ok) {
        document.getElementById('done-name').textContent         = name;
        document.getElementById('done-name-display').textContent = name;
        document.getElementById('done-email-display').textContent = email;
        this.goToStep(3);
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
  /** POST /api/auth/find-id → maskedEmail 수신 후 결과 뷰로 전환 */
  async submit() {
    const name  = document.getElementById('name').value.trim();
    const phone = document.getElementById('phone').value.trim().replace(/-/g, '');
    const err   = document.getElementById('find-error');

    if (!name)  { showError(err, '이름을 입력해 주세요.'); return; }
    if (!phone) { showError(err, '휴대전화 번호를 입력해 주세요.'); return; }
    hideError(err);

    const res = await api('POST', '/api/auth/find-id', { name, phone });

    if (res.ok) {
      const maskedEmail = res.data?.data?.maskedEmail ?? res.data?.maskedEmail ?? '–';
      document.getElementById('result-email').textContent = maskedEmail;
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
   → 이메일로 reset 링크 발송 (링크: /reset-password.html?token=xxx)

   [실제 변경 — URL ?token= 감지 시 자동 표시]
   POST /api/auth/reset-password { token, newPassword, newPasswordConfirm }
════════════════════════════════════════════ */

const resetPw = {
  /** 페이지 진입 시 URL ?token= 파라미터 감지 → view-new-pw 자동 표시 */
  init() {
    const token = new URLSearchParams(window.location.search).get('token');
    if (token) {
      this._token = token;
      showView('view-new-pw');
    }
  },

  _token: null,

  /** POST /api/auth/find-password — 재설정 링크 발송 */
  async requestLink() {
    const email = document.getElementById('rp-email').value.trim();
    const name  = document.getElementById('rp-name').value.trim();
    const err   = document.getElementById('rp-request-error');

    if (!email) { showError(err, '이메일을 입력해 주세요.'); return; }
    if (!name)  { showError(err, '이름을 입력해 주세요.'); return; }
    hideError(err);

    const res = await api('POST', '/api/auth/find-password', { email, name });

    if (res.ok) {
      showView('view-sent');
    } else {
      showError(err, extractMessage(res.data));
    }
  },

  checkStrength() {
    const pw   = document.getElementById('new-pw').value;
    const wrap = document.getElementById('pw-strength');
    if (!pw) { wrap.classList.add('is-hidden'); return; }
    wrap.classList.remove('is-hidden');
    renderStrengthBar(calcPwScore(pw), 'strength-bar', 'strength-label');
  },

  checkMatch() {
    const pw  = document.getElementById('new-pw').value;
    const c   = document.getElementById('new-pw-confirm').value;
    const msg = document.getElementById('match-msg');
    if (!c) { msg.textContent = ''; return; }
    msg.textContent = pw === c ? '비밀번호가 일치합니다.' : '비밀번호가 일치하지 않습니다.';
    msg.className   = pw === c ? 'helper ok' : 'helper error';
  },

  /**
   * POST /api/auth/reset-password
   * { token, newPassword, newPasswordConfirm }
   */
  async submit() {
    const pw  = document.getElementById('new-pw').value;
    const pwc = document.getElementById('new-pw-confirm').value;
    const err = document.getElementById('reset-error');

    if (pw.length < 8) { showError(err, '비밀번호는 8자 이상 입력해 주세요.'); return; }
    if (pw !== pwc)    { showError(err, '비밀번호가 일치하지 않습니다.'); return; }
    hideError(err);

    const res = await api('POST', '/api/auth/reset-password', {
      token:              this._token,
      newPassword:        pw,
      newPasswordConfirm: pwc,
    });

    if (res.ok) {
      alert('비밀번호가 변경되었습니다.\n다시 로그인해 주세요.');
      window.location.href = 'login.html';
    } else {
      // 토큰 만료 등 → view-token-error
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
════════════════════════════════════════════ */
document.addEventListener('DOMContentLoaded', () => {
  const page = document.body.dataset.page;
  if (page === 'signup')         signup.init();
  if (page === 'reset-password') resetPw.init();
});
