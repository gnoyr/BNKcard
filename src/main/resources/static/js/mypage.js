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
    get:   url      => req('GET',   url),
    post:  (url, b) => req('POST',  url, b),
    put:   (url, b) => req('PUT',   url, b),
    patch: (url, b) => req('PATCH', url, b),
    del:   url      => req('DELETE', url),
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
    el.className   = cls ? `toast toast--${cls}` : 'toast';
    el.textContent = msg;
    c.appendChild(el);
    setTimeout(() => {
      el.style.animation = 'toastOut .3s ease forwards';
      el.addEventListener('animationend', () => el.remove(), { once: true });
    }, ms);
  }
  return {
    success: m => show(m, 'success'),
    error:   m => show(m, 'error'),
    warning: m => show(m, 'warning'),
    info:    m => show(m),
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
  required: (v, msg = '필수 입력 항목입니다.') => (v?.trim() ? '' : msg),
  phone:    v => (/^01[016789]\d{7,8}$/.test(v.replace(/-/g, '')) ? '' : '올바른 휴대폰 번호를 입력해주세요.'),
  password: v => {
    if (!v) return '비밀번호를 입력해주세요.';
    if (v.length < 8 || v.length > 50) return '8~50자로 입력해주세요.';
    if (!/(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])/.test(v))
      return '영문, 숫자, 특수문자를 모두 포함해야 합니다.';
    return '';
  },
  match: (a, b) => (a === b ? '' : '비밀번호가 일치하지 않습니다.'),
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
  money: n => `${Math.round(Number(n) || 0).toLocaleString('ko-KR')}원`,
  comma: n =>  Math.round(Number(n) || 0).toLocaleString('ko-KR'),
};

/* ── 버튼 로딩 ── */
function btnLoading(btn, on) {
  if (on) { btn._txt = btn.innerHTML; btn.classList.add('btn-loading');    btn.disabled = true; }
  else    { btn.classList.remove('btn-loading'); btn.disabled = false;
            if (btn._txt !== undefined) btn.innerHTML = btn._txt; }
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
    REQUESTED: ['badge--requested', '접수 완료'],
    REVIEWING: ['badge--reviewing', '심사 중'],
    APPROVED:  ['badge--approved',  '승인'],
    REJECTED:  ['badge--rejected',  '반려'],
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
    type: 'doughnut',
    data: { labels, datasets: [{ data: values, backgroundColor: colors, borderWidth: 2, borderColor: '#fff', hoverOffset: 5 }] },
    options: {
      cutout: '68%',
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
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

function chartColor(categoryId) {
  const p = ['#C8102E','#E8374F','#F28B82','#FF9800','#4CAF50',
             '#2196F3','#9C27B0','#00BCD4','#FF5722','#795548'];
  return p[(Number(categoryId) - 1) % p.length];
}

/* ── 금액 입력 콤마 포맷 ── */
function initMoneyInputs(selector) {
  document.querySelectorAll(selector).forEach(inp => {
    inp.addEventListener('input', () => {
      const raw = inp.value.replace(/\D/g, '');
      inp.value       = raw ? Number(raw).toLocaleString('ko-KR') : '';
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
  if (document.getElementById('editForm'))      initEdit();
  if (document.getElementById('pwForm'))        initPassword();
  if (document.getElementById('spendingForm'))  initSpending();
});

/* ================================================================
   §3. 메인 대시보드 (index.html)
   ================================================================ */

async function initMain() {
  /* ── 내 정보 ── */
  try {
    const user = await API.get('/api/users/me');

    document.getElementById('profileName').textContent  = user.name + ' 님';
    document.getElementById('profileScore').textContent = user.creditScore ?? '—';
    document.getElementById('profileMeta').textContent  =
      '마지막 로그인 '
      + (user.lastLoginAt
          ? new Date(user.lastLoginAt).toLocaleString('ko-KR', { dateStyle: 'short', timeStyle: 'short' })
          : '—');

    const rows = [
      ['이메일',     user.maskedEmail],
      ['휴대폰',     user.maskedPhone],
      ['생년월일',   user.birthDate       ?? '미입력'],
      ['직업',       user.job             ?? '미입력'],
      ['소득 등급',  user.incomeLevelCode ?? '미입력'],
      ['푸시 알림',  user.pushEnabled],
      ['마케팅 동의',user.marketingAgree],
    ];
    document.getElementById('infoList').innerHTML = rows.map(([label, val]) => {
      const on    = val === true || val === 'Y';
      const isBool = typeof val === 'boolean' || val === 'Y' || val === 'N';
      const disp   = isBool
        ? `<span class="dot ${on ? 'dot-on' : 'dot-off'}"></span> ${on ? '수신/동의' : '미수신/미동의'}`
        : (val ?? '—');
      return `<li><span class="info-label">${label}</span>
                  <span class="info-value">${disp}</span></li>`;
    }).join('');
  } catch (err) {
    Toast.error(err.message);
  }

  /* ── 카드 현황 ── */
  try {
    const data = await API.get('/api/users/me/cards');
    const section = document.getElementById('cardSection');
    const all = [
      ...(data.ownedCards   ?? []).map(c => ({ name: c.cardName, sub: '발급일: ' + c.issuedAt,  badge: '<span class="badge badge--owned">보유 중</span>' })),
      ...(data.applications ?? []).map(a => ({ name: a.cardName, sub: '신청일: ' + a.appliedAt, badge: statusBadge(a.applicationStatus) })),
    ];
    section.innerHTML = all.length === 0
      ? '<div class="empty-state">보유 카드 및 신청 내역이 없습니다.</div>'
      : all.map(it => `
          <div class="card-item">
            <span class="card-thumb">카드</span>
            <div class="card-info">
              <div class="card-info__name">${it.name}</div>
              <div class="card-info__sub">${it.sub}</div>
            </div>
            ${it.badge}
          </div>`).join('');
  } catch {
    document.getElementById('cardSection').innerHTML =
      '<div class="empty-state">카드 정보를 불러오지 못했습니다.</div>';
  }

  /* ── 소비 패턴 ── */
  try {
    const data  = await API.get('/api/users/me/spending');
    const items = Array.isArray(data) ? data : (data.items ?? []);
    const hasData = items.some(i => Number(i.monthlyAmount ?? 0) > 0);

    if (!hasData) {
      document.getElementById('spendingSection').innerHTML =
        '<div class="empty-state">등록된 소비 패턴이 없습니다. '
        + '<a href="/mypage/spending.html">등록하기</a></div>';
    } else {
      const total = items.reduce((s, i) => s + Number(i.monthlyAmount ?? 0), 0);
      renderDonut('donutChart', items, total);
    }
  } catch {
    document.getElementById('spendingSection').innerHTML =
      '<div class="empty-state">소비 패턴을 불러오지 못했습니다.</div>';
  }
}

/* ================================================================
   §4. 내 정보 수정 (edit.html)
   ================================================================ */

async function initEdit() {
  /* 기존 정보 로드 */
  try {
    const user = await API.get('/api/users/me');
    document.getElementById('name').value              = user.name ?? '';
    document.getElementById('currentPhone').textContent = user.maskedPhone ?? '미등록';
    document.getElementById('job').value               = user.job ?? '';
    document.getElementById('incomeLevelCode').value   = user.incomeLevelCode ?? '';
    document.getElementById('pushEnabled').checked     = user.pushEnabled === 'Y' || user.pushEnabled === true;
    document.getElementById('marketingAgree').checked  = user.marketingAgree === 'Y' || user.marketingAgree === true;
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
      name:            document.getElementById('name').value.trim()            || undefined,
      phone:           phoneVal                                                 || undefined,
      job:             document.getElementById('job').value.trim()             || undefined,
      incomeLevelCode: document.getElementById('incomeLevelCode').value        || undefined,
      pushEnabled:     document.getElementById('pushEnabled').checked,
      marketingAgree:  document.getElementById('marketingAgree').checked,
      currentPassword: password || undefined,
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

    if (phoneVal) {
      pwInput.value = '';
      pwErr.textContent = '';
      pwErr.classList.remove('show');
      modal.classList.add('open');
      setTimeout(() => pwInput.focus(), 150);
    } else {
      await doUpdate(collectBody(null));
    }
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
    length:  { test: v => v.length >= 8 && v.length <= 50 },
    letter:  { test: v => /[A-Za-z]/.test(v) },
    number:  { test: v => /\d/.test(v) },
    special: { test: v => /[@$!%*#?&]/.test(v) },
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
    if      (score <= 1) { fill.classList.add('fill-weak');   label.textContent = '보안 강도: 약함'; }
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
    if (!V.setErr('newPw',     V.password(newPw.value))) ok = false;
    if (ok && !V.setErr('confirmPw', V.match(newPw.value, confirmPw.value))) ok = false;
    if (!ok) return;

    btnLoading(submitBtn, true);
    try {
      await API.patch('/api/users/me/password', {
        currentPassword:    document.getElementById('currentPw').value,
        newPassword:        newPw.value,
        newPasswordConfirm: confirmPw.value,
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
        categoryId:    Number(row.dataset.categoryId),
        monthlyAmount: Number(row.querySelector('.money-input')?.dataset.raw || 0),
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
