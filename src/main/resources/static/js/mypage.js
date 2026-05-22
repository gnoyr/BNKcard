/**
 * mypage.js  |  부산은행 마이페이지 공통 스크립트
 * - API fetch 래퍼 (쿠키 기반 JWT 자동 포함)
 * - Toast / Modal / Validator / 차트 / 포맷 유틸
 */

/* ============================================================
   1. API fetch 래퍼
      - HttpOnly 쿠키 방식이면 credentials:'include' 만으로 동작
      - 401 → 로그인 페이지로 자동 이동
   ============================================================ */
const API = (() => {
  async function req(method, url, body) {
    const opts = {
      method,
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
    };
    if (body) opts.body = JSON.stringify(body);

    const res = await fetch(url, opts);

    if (res.status === 401) {
      window.location.href = '/login.html';
      throw new Error('인증이 필요합니다.');
    }

    const json = await res.json();
    if (!res.ok) throw new Error(json.message || `HTTP ${res.status}`);
    return json.data;   // ApiResponse.data 꺼냄
  }

  return {
    get:   url       => req('GET',   url),
    put:   (url, b)  => req('PUT',   url, b),
    patch: (url, b)  => req('PATCH', url, b),
    post:  (url, b)  => req('POST',  url, b),
  };
})();


/* ============================================================
   2. Toast
   ============================================================ */
const Toast = (() => {
  function ensureContainer() {
    let c = document.getElementById('toast-container');
    if (!c) { c = document.createElement('div'); c.id = 'toast-container'; document.body.appendChild(c); }
    return c;
  }
  function show(msg, type, ms = 3000) {
    const c = ensureContainer();
    const el = document.createElement('div');
    el.className = `toast${type ? ' toast-' + type : ''}`;
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


/* ============================================================
   3. Modal
   ============================================================ */
const Modal = {
  _build(html) {
    const overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.innerHTML = `<div class="modal">${html}</div>`;
    document.body.appendChild(overlay);
    requestAnimationFrame(() => overlay.classList.add('open'));
    return overlay;
  },
  _close(overlay) {
    overlay.classList.remove('open');
    setTimeout(() => overlay.remove(), 250);
  },
  confirm(title, body) {
    return new Promise(resolve => {
      const o = this._build(`
        <div class="modal__title">${title}</div>
        <div class="modal__body">${body}</div>
        <div class="modal__actions">
          <button class="btn btn-outline" id="_no">취소</button>
          <button class="btn btn-primary" id="_yes">확인</button>
        </div>`);
      o.querySelector('#_yes').onclick = () => { this._close(o); resolve(true); };
      o.querySelector('#_no').onclick  = () => { this._close(o); resolve(false); };
      o.addEventListener('click', e => { if (e.target === o) { this._close(o); resolve(false); } });
    });
  },
  alert(title, body) {
    return new Promise(resolve => {
      const o = this._build(`
        <div class="modal__title">${title}</div>
        <div class="modal__body">${body}</div>
        <div class="modal__actions">
          <button class="btn btn-primary btn-block" id="_ok">확인</button>
        </div>`);
      o.querySelector('#_ok').onclick = () => { this._close(o); resolve(); };
    });
  },
  open(overlayId) {
    document.getElementById(overlayId)?.classList.add('open');
  },
  close(overlayId) {
    document.getElementById(overlayId)?.classList.remove('open');
  },
};


/* ============================================================
   4. 유효성 검사 헬퍼
   ============================================================ */
const V = {
  required: (v, msg = '필수 입력 항목입니다.') => v?.trim() ? '' : msg,
  phone:    v => /^01[016789]\d{7,8}$/.test(v.replace(/-/g, '')) ? '' : '올바른 휴대폰 번호를 입력해주세요.',
  password: v => {
    if (!v) return '비밀번호를 입력해주세요.';
    if (v.length < 8 || v.length > 50) return '8~50자로 입력해주세요.';
    if (!/(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])/.test(v))
      return '영문·숫자·특수문자를 모두 포함해야 합니다.';
    return '';
  },
  match: (a, b) => a === b ? '' : '비밀번호가 일치하지 않습니다.',

  /** 필드에 에러 표시 / 제거 — fieldId 기반 */
  setErr(id, msg) {
    const f = document.getElementById(id);
    const e = document.getElementById(id + '-err');
    if (!f) return !msg;
    if (msg) { f.classList.add('err');  if (e) { e.textContent = msg; e.classList.add('show'); } }
    else      { f.classList.remove('err'); if (e) { e.textContent = ''; e.classList.remove('show'); } }
    return !msg;
  },
};


/* ============================================================
   5. 숫자 포맷
   ============================================================ */
const Fmt = {
  money:  n => `${Math.round(Number(n) || 0).toLocaleString('ko-KR')}원`,
  comma:  n => Math.round(Number(n) || 0).toLocaleString('ko-KR'),
};


/* ============================================================
   6. 버튼 로딩 상태
   ============================================================ */
function btnLoading(btn, on) {
  if (on) {
    btn._txt = btn.innerHTML;
    btn.classList.add('btn-loading');
    btn.disabled = true;
  } else {
    btn.classList.remove('btn-loading');
    btn.disabled = false;
    if (btn._txt) btn.innerHTML = btn._txt;
  }
}


/* ============================================================
   7. 비밀번호 눈 아이콘 초기화
   ============================================================ */
function initPwEyes() {
  const EYE = `<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>`;
  const HIDE = `<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>`;
  document.querySelectorAll('.pw-eye').forEach(btn => {
    btn.innerHTML = EYE;
    btn.addEventListener('click', () => {
      const inp = document.getElementById(btn.dataset.target);
      if (!inp) return;
      inp.type = inp.type === 'password' ? 'text' : 'password';
      btn.innerHTML = inp.type === 'password' ? EYE : HIDE;
    });
  });
}


/* ============================================================
   8. 신청 상태 → 배지 HTML
   ============================================================ */
function statusBadge(status) {
  const map = {
    REQUESTED: ['badge-requested', '접수 완료'],
    REVIEWING: ['badge-reviewing', '심사 중'],
    APPROVED:  ['badge-approved',  '승인'],
    REJECTED:  ['badge-rejected',  '반려'],
  };
  const [cls, label] = map[status] || ['badge-requested', status];
  return `<span class="badge ${cls}">${label}</span>`;
}


/* ============================================================
   9. 도넛 차트 렌더 (Chart.js 4.x)
   ============================================================ */
function renderDonut(canvasId, items, totalAmount) {
  const canvas = document.getElementById(canvasId);
  if (!canvas || !items?.length) return;

  const labels = items.map(i => i.categoryName);
  const values = items.map(i => Number(i.monthlyAmount));
  const colors = items.map(i => i.colorCode || fallbackColor(i.categoryId));

  if (window._donutChart) window._donutChart.destroy();
  window._donutChart = new Chart(canvas, {
    type: 'doughnut',
    data: {
      labels,
      datasets: [{
        data: values,
        backgroundColor: colors,
        borderWidth: 2,
        borderColor: '#fff',
        hoverOffset: 5,
      }],
    },
    options: {
      cutout: '68%',
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: ctx => ` ${ctx.label}: ${Fmt.money(ctx.raw)} (${items[ctx.dataIndex].percentage}%)`,
          },
        },
      },
    },
  });

  const amtEl = document.getElementById('chart-amount');
  if (amtEl) amtEl.textContent = Fmt.money(totalAmount);

  const legendEl = document.getElementById('chart-legend');
  if (legendEl) {
    legendEl.innerHTML = items.map((it, i) => `
      <div class="legend-item">
        <span class="legend-dot" style="background:${colors[i]}"></span>
        <span class="legend-name">${it.categoryName}</span>
        <span class="legend-pct">${it.percentage}%</span>
        <span class="legend-amt">${Fmt.money(it.monthlyAmount)}</span>
      </div>`).join('');
  }
}

function fallbackColor(categoryId) {
  const p = ['#C8102E','#E8374F','#F28B82','#FF9800','#4CAF50',
             '#2196F3','#9C27B0','#00BCD4','#FF5722','#795548'];
  return p[(Number(categoryId) - 1) % p.length];
}


/* ============================================================
   10. 금액 입력 콤마 자동 포맷
   ============================================================ */
function initMoneyInputs(selector) {
  document.querySelectorAll(selector).forEach(inp => {
    inp.addEventListener('input', () => {
      const raw = inp.value.replace(/\D/g, '');
      inp.value = raw ? Number(raw).toLocaleString('ko-KR') : '';
      inp.dataset.raw = raw || '0';
    });
  });
}


/* ============================================================
   11. 재인증 안내 배너 삽입
   ============================================================ */
function showPhoneBanner(containerId) {
  const el = document.getElementById(containerId);
  if (!el) return;
  el.innerHTML = `
    <div class="alert alert-warning" style="margin-top:8px;">
      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor"
           stroke-width="2" style="flex-shrink:0;margin-top:1px">
        <circle cx="12" cy="12" r="10"/>
        <line x1="12" y1="8" x2="12" y2="12"/>
        <line x1="12" y1="16" x2="12.01" y2="16"/>
      </svg>
      <span>휴대폰 번호가 변경되었습니다. 변경된 번호로 본인 재인증이 필요합니다.</span>
    </div>`;
}


/* 전역 노출 */
window.MP = {
  API, Toast, Modal, V, Fmt,
  btnLoading, initPwEyes, statusBadge, renderDonut,
  initMoneyInputs, showPhoneBanner, fallbackColor,
};
