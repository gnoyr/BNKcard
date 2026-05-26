/**
 * mypage.js  |  BNK 마이페이지 통합 스크립트
 *
 * 구성:
 *   1. 공통 유틸  — API, Toast, Modal, V, Fmt, btnLoading,
 *                   initPwToggles, statusBadge, renderDonut, initMoneyInputs
 *   2. 페이지 감지 — DOM 요소 존재 여부로 현재 페이지 판별
 *   3. 페이지 로직 — 메인 / 수정 / 비밀번호 / 소비패턴
 *
 * 인증: HttpOnly 쿠키 (access_token / refresh_token)
 *       credentials:'include' 만 사용 — JS 쿠키 직접 읽기 없음
 *
 * [업데이트] initMain — mypage-main.js 기준으로 최신화
 *   사용 API:
 *     GET /api/users/me          → 프로필 정보
 *     GET /api/users/me/cards    → 보유카드 + 신청현황
 *     GET /api/users/me/spending → 소비패턴 도넛차트
 */

'use strict';

/* ================================================================
   §1. 공통 유틸
   ================================================================ */

/* ── API ── */
const API = (() => {
  const LOGIN = '/auth/login.html';

  async function req(method, url, body) {
    const opts = {
      method,
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
    };
    if (body) opts.body = JSON.stringify(body);

    let res = await fetch(url, opts);

    if (res.status === 401) {
      if (await _refresh()) {
        res = await fetch(url, opts);
      } else {
        window.location.href = LOGIN;
        throw new Error('인증이 만료되었습니다.');
      }
    }

    const json = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(json.message || `HTTP ${res.status}`);
    return json.data;
  }

  async function _refresh() {
    try {
      const res = await fetch('/api/auth/refresh', {
        method: 'POST', credentials: 'include',
      });
      if (res.ok) {
        sessionStorage.setItem('bnk_login_at', String(Date.now()));
        return true;
      }
      return false;
    } catch { return false; }
  }

  return {
    get   : url       => req('GET',    url),
    post  : (url, b)  => req('POST',   url, b),
    put   : (url, b)  => req('PUT',    url, b),
    patch : (url, b)  => req('PATCH',  url, b),
    del   : url       => req('DELETE', url),
  };
})();

/* ── Toast ── */
const Toast = (() => {
  function container() {
    let el = document.getElementById('toast-container');
    if (!el) {
      el = document.createElement('div');
      el.id = 'toast-container';
      document.body.appendChild(el);
    }
    return el;
  }
  function show(msg, cls, ms = 3000) {
    const c  = container();
    const el = document.createElement('div');
    el.className = cls ? `toast toast--${cls}` : 'toast';
    el.textContent = msg;
    c.appendChild(el);
    setTimeout(() => {
      el.style.animation = 'toastOut .3s ease forwards';
      el.addEventListener('animationend', () => el.remove(), { once: true });
    }, ms);
  }
  return {
    success : m => show(m, 'success'),
    error   : m => show(m, 'error'),
    warning : m => show(m, 'warning'),
    info    : m => show(m),
  };
})();

/* ── Modal ── */
const Modal = {
  _open(html) {
    const ov = document.createElement('div');
    ov.className = 'modal-overlay';
    ov.innerHTML = `<div class="modal" role="dialog" aria-modal="true">${html}</div>`;
    document.body.appendChild(ov);
    requestAnimationFrame(() => ov.classList.add('open'));
    return ov;
  },
  _close(ov) {
    ov.classList.remove('open');
    setTimeout(() => ov.remove(), 250);
  },
  confirm(title, body) {
    return new Promise(res => {
      const ov = this._open(`
        <div class="modal__title">${title}</div>
        <div class="modal__body">${body}</div>
        <div class="modal__actions">
          <button class="btn btn-outline" id="_mNo">취소</button>
          <button class="btn btn-primary" id="_mYes">확인</button>
        </div>`);
      ov.querySelector('#_mYes').onclick = () => { this._close(ov); res(true); };
      ov.querySelector('#_mNo').onclick  = () => { this._close(ov); res(false); };
      ov.addEventListener('click', e => { if (e.target === ov) { this._close(ov); res(false); } });
    });
  },
  alert(title, body) {
    return new Promise(res => {
      const ov = this._open(`
        <div class="modal__title">${title}</div>
        <div class="modal__body">${body}</div>
        <div class="modal__actions">
          <button class="btn btn-primary btn-block" id="_mOk">확인</button>
        </div>`);
      ov.querySelector('#_mOk').onclick = () => { this._close(ov); res(); };
    });
  },
};

/* ── Validator ── */
const V = {
  required : (v, msg = '필수 입력 항목입니다.') => (v?.trim() ? '' : msg),
  phone    : v => (/^01[016789]\d{7,8}$/.test(v.replace(/-/g, '')) ? '' : '올바른 휴대폰 번호를 입력해주세요.'),
  password : v => {
    if (!v) return '비밀번호를 입력해주세요.';
    if (v.length < 8 || v.length > 50) return '8~50자로 입력해주세요.';
    if (!/(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])/.test(v))
      return '영문, 숫자, 특수문자를 모두 포함해야 합니다.';
    return '';
  },
  match : (a, b) => (a === b ? '' : '비밀번호가 일치하지 않습니다.'),
  setErr(id, msg) {
    const f = document.getElementById(id);
    const e = document.getElementById(`${id}-err`);
    if (!f) return !msg;
    f.classList.toggle('err', !!msg);
    if (e) { e.textContent = msg || ''; e.classList.toggle('show', !!msg); }
    return !msg;
  },
};

/* ── 숫자 포맷 ── */
const Fmt = {
  money : n => `${Math.round(Number(n) || 0).toLocaleString('ko-KR')}원`,
  comma : n => Math.round(Number(n) || 0).toLocaleString('ko-KR'),
};

/* ── 버튼 로딩 ── */
function btnLoading(btn, on) {
  if (on) { btn._txt = btn.innerHTML; btn.classList.add('btn-loading'); btn.disabled = true; }
  else {
    btn.classList.remove('btn-loading'); btn.disabled = false;
    if (btn._txt !== undefined) btn.innerHTML = btn._txt;
  }
}

/* ── 비밀번호 토글 (텍스트 기반, SVG 없음) ── */
function initPwToggles() {
  document.querySelectorAll('.pw-toggle').forEach(btn => {
    btn.textContent = '표시';
    btn.addEventListener('click', () => {
      const inp = document.getElementById(btn.dataset.target);
      if (!inp) return;
      inp.type = inp.type === 'password' ? 'text' : 'password';
      btn.textContent = inp.type === 'password' ? '표시' : '숨기기';
    });
  });
}

/* ── 신청 상태 배지 ── */
function statusBadge(status) {
  const map = {
    REQUESTED : ['badge--requested', '접수 완료'],
    REVIEWING : ['badge--reviewing', '심사 중'],
    APPROVED  : ['badge--approved',  '승인'],
    REJECTED  : ['badge--rejected',  '반려'],
  };
  const [cls, label] = map[status] || ['badge--requested', status];
  return `<span class="badge ${cls}">${label}</span>`;
}

/* ── 도넛 차트 (Chart.js 4.x) ── */
function renderDonut(canvasId, items, totalAmount) {
  const canvas = document.getElementById(canvasId);
  if (!canvas || !items?.length) return;

  const labels = items.map(i => i.categoryName);
  const values = items.map(i => Number(i.monthlyAmount ?? i.ratio ?? 0));
  const colors = items.map(i => i.colorCode || chartColor(i.categoryId));

  if (window._donutChart) window._donutChart.destroy();
  window._donutChart = new Chart(canvas, {
    type : 'doughnut',
    data : {
      labels,
      datasets: [{
        data            : values,
        backgroundColor : colors,
        borderWidth     : 2,
        borderColor     : '#fff',
        hoverOffset     : 5,
      }],
    },
    options: {
      cutout               : '68%',
      responsive           : true,
      maintainAspectRatio  : false,
      plugins: {
        legend  : { display: false },
        tooltip : {
          callbacks: {
            label: ctx => {
              const it  = items[ctx.dataIndex];
              const pct = it.percentage ?? it.ratio ?? 0;
              return ` ${ctx.label}: ${Fmt.money(ctx.raw)} (${pct}%)`;
            },
          },
        },
      },
    },
  });

  const amtEl = document.getElementById('chart-amount');
  if (amtEl) amtEl.textContent = Fmt.money(totalAmount);

  const legEl = document.getElementById('chart-legend');
  if (legEl) {
    legEl.innerHTML = items.map((it, i) => {
      const pct = it.percentage ?? it.ratio ?? 0;
      return `<div class="legend-item">
        <span class="legend-dot" style="background:${colors[i]}"></span>
        <span class="legend-name">${it.categoryName}</span>
        <span class="legend-pct">${pct}%</span>
        <span class="legend-amt">${Fmt.money(it.monthlyAmount ?? 0)}</span>
      </div>`;
    }).join('');
  }
}

/* ── 차트 팔레트 (categoryId 기반) ── */
function chartColor(categoryId) {
  const p = [
    '#C8102E', '#E8374F', '#F28B82', '#FF9800',
    '#4CAF50', '#2196F3', '#9C27B0', '#00BCD4',
    '#FF5722', '#795548',
  ];
  return p[(Number(categoryId) - 1) % p.length];
}

/* ── 금액 입력 콤마 포맷 ── */
function initMoneyInputs(selector) {
  document.querySelectorAll(selector).forEach(inp => {
    inp.addEventListener('input', () => {
      const raw  = inp.value.replace(/\D/g, '');
      inp.value  = raw ? Number(raw).toLocaleString('ko-KR') : '';
      inp.dataset.raw = raw || '0';
    });
  });
}


/* ================================================================
   §2. 페이지 감지 + 초기화 진입
   ================================================================ */

document.addEventListener('DOMContentLoaded', () => {
  initPwToggles();

  if (document.getElementById('donutChart'))   initMain();
  if (document.getElementById('editForm'))     initEdit();
  if (document.getElementById('pwForm'))       initPassword();
  if (document.getElementById('spendingForm')) initSpending();
});


/* ================================================================
   §3. 메인 대시보드 (index.html)
   ── mypage-main.js 기준 최신화 ──
   ================================================================ */

/* ── 애플리케이션 상태 한글 라벨 ── */
const APP_STATUS_LABEL = {
  REQUESTED : '신청 접수',
  REVIEWING : '심사 중',
  APPROVED  : '승인 완료',
  REJECTED  : '신청 거절',
  ISSUED    : '발급 완료',
};

/* ── 배지 클래스 매핑 ── */
const APP_STATUS_CLASS = {
  REQUESTED : 'badge--requested',
  REVIEWING : 'badge--reviewing',
  APPROVED  : 'badge--approved',
  REJECTED  : 'badge--rejected',
  ISSUED    : 'badge--issued',
};

/* ── 날짜 포맷 ── */
function fmtDate(str) {
  if (!str) return '—';
  return str.slice(0, 10);
}

/* ── 금액 포맷 (만원 단위 축약 포함) ── */
function fmtMoney(n) {
  if (n == null) return '—';
  const num = Number(n);
  if (num >= 10000) return (num / 10000).toFixed(num % 10000 === 0 ? 0 : 1) + '만원';
  return num.toLocaleString('ko-KR') + '원';
}

/* ── 이름 첫 글자 추출 ── */
function nameInitial(name) {
  if (!name) return '?';
  return name.charAt(0);
}

/* ── 직업 한글 ── */
const JOB_LABEL = {
  EMPLOYED      : '직장인',
  SELF_EMPLOYED : '자영업자',
  STUDENT       : '학생',
  UNEMPLOYED    : '무직',
  OTHER         : '기타',
};

/* ── 소득등급 한글 ── */
const INCOME_LABEL = {
  LV1 : 'LV1 (3천만 미만)',
  LV2 : 'LV2 (3천~5천만)',
  LV3 : 'LV3 (5천만~1억)',
  LV4 : 'LV4 (1억 이상)',
};

/* ── 차트 팔레트 (index 직접 참조용) ── */
const CHART_COLORS = [
  '#C8102E', '#E8374F', '#F28B82', '#FF9800',
  '#4CAF50', '#2196F3', '#9C27B0', '#00BCD4',
  '#FF5722', '#795548',
];

async function initMain() {

  /* ──────────────────────────────────────────
     [1] 내 정보 — GET /api/users/me
  ────────────────────────────────────────── */
  try {
    const user = await API.get('/api/users/me');

    /* 프로필 이니셜 */
    const initialEl = document.getElementById('profileInitial');
    if (initialEl) initialEl.textContent = nameInitial(user.name);

    /* 프로필 이름 */
    const nameEl = document.getElementById('profileName');
    if (nameEl) nameEl.textContent = (user.name ?? '사용자') + ' 님';

    /* 마지막 로그인 */
    const metaEl = document.getElementById('profileMeta');
    if (metaEl) {
      const loginStr = user.lastLoginAt
        ? new Date(user.lastLoginAt).toLocaleString('ko-KR', { dateStyle: 'short', timeStyle: 'short' })
        : '—';
      metaEl.textContent = '마지막 로그인 ' + loginStr;
    }

    /* 신용점수 */
    const scoreEl = document.getElementById('profileScore');
    if (scoreEl) scoreEl.textContent = user.creditScore ?? '—';

    /* 내 정보 목록 */
    const infoList = document.getElementById('infoList');
    if (infoList) {
      const rows = [
        { label: '이메일',      value: user.maskedEmail },
        { label: '휴대폰',      value: user.maskedPhone },
        { label: '생년월일',    value: user.birthDate },
        { label: '직업',        value: JOB_LABEL[user.job] ?? user.job },
        { label: '소득 등급',   value: INCOME_LABEL[user.incomeLevelCode] ?? user.incomeLevelCode },
        { label: '이메일 인증', value: user.isEmailVerified },
        { label: '푸시 알림',   value: user.pushEnabled },
        { label: '마케팅 동의', value: user.marketingAgree },
      ];

      infoList.innerHTML = rows.map(({ label, value }) => {
        const isBool = value === 'Y' || value === 'N' || typeof value === 'boolean';
        const on     = value === 'Y' || value === true;

        let display;
        if (isBool) {
          display = `<span class="dot ${on ? 'dot-on' : 'dot-off'}"></span> ${on ? '동의 / 수신' : '미동의 / 미수신'}`;
        } else {
          display = value ?? '미입력';
        }

        return `<li>
          <span class="info-label">${label}</span>
          <span class="info-value">${display}</span>
        </li>`;
      }).join('');
    }

  } catch (err) {
    Toast.error('프로필 정보를 불러오지 못했습니다.');
  }


  /* ──────────────────────────────────────────
     [2] 보유 카드 + 신청 현황 — GET /api/users/me/cards
  ────────────────────────────────────────── */
  const cardSection = document.getElementById('cardSection');

  try {
    const data = await API.get('/api/users/me/cards');

    const owned = (data.ownedCards   ?? []).map(c => ({
      type   : 'owned',
      name   : c.cardName,
      sub    : '발급일 ' + fmtDate(c.issuedAt),
      status : 'ISSUED',
    }));

    const applied = (data.applications ?? []).map(a => ({
      type   : 'applied',
      name   : a.cardName,
      sub    : '신청일 ' + fmtDate(a.appliedAt),
      status : a.applicationStatus,
    }));

    const all = [...owned, ...applied];

    if (all.length === 0) {
      cardSection.innerHTML = `
        <div class="empty-state">
          보유 카드 및 신청 내역이 없습니다.
        </div>`;
    } else {
      cardSection.innerHTML = all.map(item => {
        const chipClass  = item.type === 'owned' ? 'card-chip--owned' : 'card-chip--applied';
        const chipText   = item.type === 'owned' ? '보유' : '신청';
        const badgeClass = APP_STATUS_CLASS[item.status] ?? 'badge--requested';
        const badgeLabel = APP_STATUS_LABEL[item.status] ?? item.status;

        return `<div class="card-item">
          <span class="card-chip ${chipClass}">${chipText}</span>
          <div class="card-info">
            <div class="card-info__name">${item.name ?? '—'}</div>
            <div class="card-info__sub">${item.sub}</div>
          </div>
          <span class="badge ${badgeClass}">${badgeLabel}</span>
        </div>`;
      }).join('');
    }

  } catch {
    cardSection.innerHTML = `<div class="empty-state">카드 정보를 불러오지 못했습니다.</div>`;
  }


  /* ──────────────────────────────────────────
     [3] 소비 패턴 — GET /api/users/me/spending
  ────────────────────────────────────────── */
  const spendingSection = document.getElementById('spendingSection');

  try {
    const data   = await API.get('/api/users/me/spending');
    const items  = Array.isArray(data) ? data : (data.items ?? []);
    const active = items.filter(i => Number(i.monthlyAmount ?? 0) > 0);

    if (active.length === 0) {
      spendingSection.innerHTML = `
        <div class="empty-state">
          등록된 소비 패턴이 없습니다.<br>
          <a href="/mypage/spending.html">등록하러 가기</a>
        </div>`;
      return;
    }

    const total = active.reduce((s, i) => s + Number(i.monthlyAmount), 0);

    /* 캔버스 래퍼 유지, 차트만 다시 그림 */
    spendingSection.innerHTML = `
      <div class="chart-wrap">
        <div class="chart-canvas-box">
          <canvas id="donutChart" width="170" height="170"></canvas>
          <div class="chart-center">
            <div class="chart-center__amount" id="chart-amount">${fmtMoney(total)}</div>
            <div class="chart-center__label">총 지출</div>
          </div>
        </div>
        <div class="legend" id="chart-legend"></div>
      </div>`;

    /* Chart.js 도넛 */
    const ctx    = document.getElementById('donutChart').getContext('2d');
    const colors = active.map((_, i) => CHART_COLORS[i % CHART_COLORS.length]);

    new Chart(ctx, {
      type : 'doughnut',
      data : {
        labels   : active.map(i => i.categoryName),
        datasets : [{
          data            : active.map(i => Number(i.monthlyAmount)),
          backgroundColor : colors,
          borderWidth     : 0,
        }],
      },
      options: {
        cutout  : '72%',
        plugins : {
          legend  : { display: false },
          tooltip : {
            callbacks: {
              label: ctx => {
                const pct = active[ctx.dataIndex]?.percentage
                  ?? active[ctx.dataIndex]?.ratio
                  ?? Math.round(ctx.parsed / total * 100);
                return ` ${ctx.label}: ${fmtMoney(ctx.parsed)} (${pct}%)`;
              },
            },
          },
        },
      },
    });

    /* 범례 */
    const legendEl = document.getElementById('chart-legend');
    if (legendEl) {
      legendEl.innerHTML = active.map((item, i) => {
        const pct = item.percentage ?? item.ratio ?? Math.round(Number(item.monthlyAmount) / total * 100);
        return `<div class="legend-item">
          <span class="legend-dot" style="background:${colors[i]}"></span>
          <span class="legend-name">${item.categoryName}</span>
          <span class="legend-pct">${pct}%</span>
          <span class="legend-amt">${fmtMoney(item.monthlyAmount)}</span>
        </div>`;
      }).join('');
    }

  } catch {
    spendingSection.innerHTML = `<div class="empty-state">소비 패턴을 불러오지 못했습니다.</div>`;
  }
}


/* ================================================================
   §4. 내 정보 수정 (edit.html)
   ================================================================ */

async function initEdit() {

  try {
    const user = await API.get('/api/users/me');
    document.getElementById('name').value                 = user.name ?? '';
    document.getElementById('currentPhone').textContent   = user.maskedPhone ?? '미등록';
    document.getElementById('job').value                  = user.job ?? '';
    document.getElementById('incomeLevelCode').value      = user.incomeLevelCode ?? '';
    document.getElementById('pushEnabled').checked        = user.pushEnabled === 'Y' || user.pushEnabled === true;
    document.getElementById('marketingAgree').checked     = user.marketingAgree === 'Y' || user.marketingAgree === true;
  } catch {
    Toast.error('정보를 불러오지 못했습니다.');
  }

  const form       = document.getElementById('editForm');
  const submitBtn  = document.getElementById('submitBtn');
  const modal      = document.getElementById('pwConfirmModal');
  const pwInput    = document.getElementById('confirmPwInput');
  const pwErr      = document.getElementById('confirmPwErr');
  const confirmBtn = document.getElementById('modalConfirmBtn');

  function collectBody(password) {
    const phoneVal = document.getElementById('phone').value.trim();
    return {
      name           : document.getElementById('name').value.trim() || undefined,
      phone          : phoneVal || undefined,
      job            : document.getElementById('job').value.trim() || undefined,
      incomeLevelCode: document.getElementById('incomeLevelCode').value || undefined,
      pushEnabled    : document.getElementById('pushEnabled').checked,
      marketingAgree : document.getElementById('marketingAgree').checked,
      currentPassword: password,
    };
  }

  async function doUpdate(body) {
    btnLoading(submitBtn, true);
    try {
      await API.put('/api/users/me', body);
      Toast.success('정보가 수정되었습니다.');
      setTimeout(() => { window.location.href = '/mypage/index.html'; }, 1000);
    } catch (err) {
      if (err.message?.includes('비밀번호')) {
        pwErr.textContent = err.message;
        pwErr.classList.add('show');
      } else {
        Toast.error(err.message || '수정 중 오류가 발생했습니다.');
      }
    } finally {
      btnLoading(submitBtn, false);
    }
  }

  form.addEventListener('submit', async e => {
    e.preventDefault();

    const phoneVal = document.getElementById('phone').value.trim();
    if (phoneVal && !V.setErr('phone', V.phone(phoneVal))) return;

    pwInput.value = '';
    pwErr.textContent = '';
    pwErr.classList.remove('show');
    modal.classList.add('open');
    setTimeout(() => pwInput.focus(), 150);
  });

  document.getElementById('modalCancelBtn').addEventListener('click', () => {
    modal.classList.remove('open');
    pwInput.value = '';
  });

  confirmBtn.addEventListener('click', async () => {
    const pw = pwInput.value;
    if (!pw) {
      pwErr.textContent = '비밀번호를 입력해주세요.';
      pwErr.classList.add('show');
      return;
    }
    pwErr.classList.remove('show');
    modal.classList.remove('open');
    await doUpdate(collectBody(pw));
  });

  pwInput.addEventListener('keydown', e => { if (e.key === 'Enter') confirmBtn.click(); });
  document.getElementById('phone')?.addEventListener('input', () => V.setErr('phone', ''));
}


/* ================================================================
   §5. 비밀번호 변경 (password.html)
   ================================================================ */

function initPassword() {
  const form      = document.getElementById('pwForm');
  const submitBtn = document.getElementById('submitBtn');
  const newPw     = document.getElementById('newPw');
  const confirmPw = document.getElementById('confirmPw');

  const rules = {
    length  : { test: v => v.length >= 8 && v.length <= 50 },
    letter  : { test: v => /[A-Za-z]/.test(v) },
    number  : { test: v => /\d/.test(v) },
    special : { test: v => /[@$!%*#?&]/.test(v) },
  };
  document.querySelectorAll('#pwRules li').forEach(li => {
    rules[li.dataset.rule].el = li;
  });

  newPw.addEventListener('input', () => {
    const v = newPw.value;
    document.getElementById('strengthWrap').hidden = !v;

    let score = 0;
    Object.values(rules).forEach(r => {
      const pass = r.test(v);
      if (pass) score++;
      if (r.el) {
        r.el.classList.toggle('pass', pass);
        r.el.textContent = (pass ? '✓ ' : '✗ ') + r.el.textContent.replace(/^[✓✗]\s/, '');
      }
    });

    const fill  = document.getElementById('strengthFill');
    const label = document.getElementById('strengthLabel');
    fill.className = 'strength-fill';
    if (score <= 1)      { fill.classList.add('fill-weak');   label.textContent = '보안 강도: 약함'; }
    else if (score <= 3) { fill.classList.add('fill-medium'); label.textContent = '보안 강도: 보통'; }
    else                 { fill.classList.add('fill-strong'); label.textContent = '보안 강도: 강함'; }

    V.setErr('newPw', '');
  });

  confirmPw.addEventListener('input', () => {
    if (confirmPw.value) V.setErr('confirmPw', V.match(newPw.value, confirmPw.value));
  });

  form.addEventListener('submit', async e => {
    e.preventDefault();
    let ok = true;
    if (!V.setErr('currentPw', V.required(document.getElementById('currentPw').value, '현재 비밀번호를 입력해주세요.'))) ok = false;
    if (!V.setErr('newPw', V.password(newPw.value))) ok = false;
    if (ok && !V.setErr('confirmPw', V.match(newPw.value, confirmPw.value))) ok = false;
    if (!ok) return;

    btnLoading(submitBtn, true);
    try {
      await API.patch('/api/users/me/password', {
        currentPassword    : document.getElementById('currentPw').value,
        newPassword        : newPw.value,
        newPasswordConfirm : confirmPw.value,
      });
      Toast.success('비밀번호가 변경되었습니다.');
      setTimeout(() => { document.getElementById('doneModal').classList.add('open'); }, 500);
    } catch (err) {
      if (err.message?.includes('비밀번호')) V.setErr('currentPw', err.message);
      else Toast.error(err.message || '변경 중 오류가 발생했습니다.');
    } finally {
      btnLoading(submitBtn, false);
    }
  });

  document.getElementById('doneOk')
    ?.addEventListener('click', () => { window.location.href = '/mypage/index.html'; });
}


/* ================================================================
   §6. 소비 패턴 관리 (spending.html)
   ================================================================ */

async function initSpending() {
  const container = document.getElementById('rowContainer');
  const totalEl   = document.getElementById('totalAmount');

  let items = [];
  try {
    const data = await API.get('/api/users/me/spending');
    items = Array.isArray(data) ? data : (data.items ?? []);
  } catch {
    Toast.error('데이터를 불러오지 못했습니다.');
  }

  if (items.length === 0) {
    container.innerHTML = '<div class="empty-state">등록된 카테고리가 없습니다.</div>';
  } else {
    container.innerHTML = items.map(item => {
      const color   = chartColor(item.categoryId);
      const amt     = Number(item.monthlyAmount || 0);
      const display = amt > 0 ? amt.toLocaleString('ko-KR') : '';
      return `
        <div class="spending-row" data-category-id="${item.categoryId}">
          <span class="spending-dot" style="background:${color}"></span>
          <span class="spending-label">${item.categoryName}</span>
          <input class="form-control spending-input money-input"
                 type="text" inputmode="numeric"
                 placeholder="0" value="${display}"
                 data-raw="${amt}"
                 aria-label="${item.categoryName} 월 지출액"/>
          <span class="spending-unit">원</span>
        </div>`;
    }).join('');

    initMoneyInputs('.money-input');
    recalcTotal();
    container.addEventListener('input', recalcTotal);
  }

  function recalcTotal() {
    const sum = [...document.querySelectorAll('.money-input')]
      .reduce((acc, inp) => acc + Number(inp.dataset.raw || 0), 0);
    totalEl.textContent = Fmt.money(sum);
  }

  document.getElementById('spendingForm').addEventListener('submit', async e => {
    e.preventDefault();
    const btn = document.getElementById('submitBtn');

    const patterns = [...document.querySelectorAll('#rowContainer .spending-row')]
      .map(row => ({
        categoryId    : Number(row.dataset.categoryId),
        monthlyAmount : Number(row.querySelector('.money-input')?.dataset.raw || 0),
      }));

    if (!patterns.length) { Toast.warning('저장할 항목이 없습니다.'); return; }

    btnLoading(btn, true);
    try {
      await API.put('/api/users/me/spending', { patterns });
      Toast.success('소비 패턴이 저장되었습니다.');
      setTimeout(() => { window.location.href = '/mypage/index.html'; }, 1100);
    } catch (err) {
      Toast.error(err.message || '저장 중 오류가 발생했습니다.');
    } finally {
      btnLoading(btn, false);
    }
  });
}