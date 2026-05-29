/**
 * mypage.js  |  BNK 마이페이지 통합 스크립트
 *
 * 구성:
 *   §1. 공통 유틸  — ApiError, API, Toast, V, Fmt, btnLoading, initPwToggles, renderDonut, initMoneyInputs
 *   §2. 페이지 감지 — DOM 요소 존재 여부로 현재 페이지 판별
 *   §3. 메인 대시보드 (index.html)
 *   §4. 내 정보 수정 (edit.html)
 *   §5. 비밀번호 변경 (password.html)
 *   §6. 소비 패턴 관리 (spending.html)
 *
 * 인증: HttpOnly 쿠키 (access_token / refresh_token)
 */

'use strict';

/* ================================================================
   §1. 공통 유틸
   ================================================================ */

/* ── ApiError — GlobalExceptionHandler 응답 구조를 그대로 전달 ── */
class ApiError extends Error {
    /**
     * @param {object} json  GlobalExceptionHandler ErrorResponse
     *   { code, message, detail, fieldErrors: [{field, value, message}] }
     * @param {number} status HTTP 상태코드
     */
    constructor(json = {}, status = 0) {
        // 사용자에게 표시할 메시지 우선순위: detail > fieldErrors[0] > message > fallback
        const display =
            json.detail
            ?? (json.fieldErrors?.[0]
                ? (json.fieldErrors[0].field
                    ? `${json.fieldErrors[0].field}: ${json.fieldErrors[0].message}`
                    : json.fieldErrors[0].message)
                : null)
            ?? json.message
            ?? '오류가 발생했습니다.';

        super(display);
        this.name = 'ApiError';
        this.code = json.code ?? null;
        this.status = status;
        this.detail = json.detail ?? null;
        this.fieldErrors = json.fieldErrors ?? [];      // [{ field, value, message }]
        this.serverMessage = json.message ?? null;
    }

    /** V.setErr()를 통해 각 fieldError를 해당 DOM 요소에 표시한다. */
    applyFieldErrors(setErrFn) {
        if (!this.fieldErrors.length) return false;
        this.fieldErrors.forEach(fe => {
            if (fe.field) setErrFn(fe.field, fe.message);
        });
        return true;
    }
}

/* ── API ── */
const API = (() => {
    const LOGIN = '/login';

    async function req(method, url, body) {
        const opts = {
            method,
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
        };
        if (body) opts.body = JSON.stringify(body);

        let res;
        try {
            res = await fetch(url, opts);
        } catch {
            // 네트워크 단절
            Toast.error('서버에 연결할 수 없습니다. 네트워크를 확인해 주세요.');
            throw new ApiError({}, 0);
        }

        // 401 — refresh 시도 후 재요청
        if (res.status === 401) {
            if (await _refresh()) {
                try { res = await fetch(url, opts); }
                catch { Toast.error('서버에 연결할 수 없습니다.'); throw new ApiError({}, 0); }
            } else {
                window.location.href = LOGIN;
                throw new ApiError({ message: '인증이 만료되었습니다.' }, 401);
            }
        }

        const json = await res.json().catch(() => ({}));

        // 403 — 권한 없음: Toast 표시 후 throw
        if (res.status === 403) {
            const msg = json.message ?? '접근 권한이 없습니다.';
            Toast.error(msg);
            throw new ApiError(json, 403);
        }

        // 5xx — 서버 오류: Toast 표시 후 throw
        if (res.status >= 500) {
            Toast.error('서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.');
            throw new ApiError(json, res.status);
        }

        // 기타 4xx — caller가 처리하도록 ApiError throw
        if (!res.ok) {
            throw new ApiError(json, res.status);
        }

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
        get: url => req('GET', url),
        post: (url, b) => req('POST', url, b),
        put: (url, b) => req('PUT', url, b),
        patch: (url, b) => req('PATCH', url, b),
        del: url => req('DELETE', url),
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
        const c = container();
        const el = document.createElement('div');
        el.className = cls ? `toast toast--${cls}` : 'toast';
        el.textContent = msg;
        c.appendChild(el);
        requestAnimationFrame(() => el.classList.add('show'));
        setTimeout(() => {
            el.classList.remove('show');
            setTimeout(() => el.remove(), 300);
        }, ms);
    }
    return {
        success: msg => show(msg, 'success'),
        error: msg => show(msg, 'error'),
        warning: msg => show(msg, 'warning'),
        info: msg => show(msg, 'info'),
    };
})();

/* ── Validator ── */
const V = (() => {
    function setErr(id, msg) {
        const el = document.getElementById(id + '-err') ?? document.getElementById(id + 'Err');
        if (!el) return !msg;
        el.textContent = msg || '';
        el.classList.toggle('show', !!msg);
        return !msg;
    }
    return {
        setErr,
        required: (v, msg = '필수 입력 항목입니다.') => v?.trim() ? '' : msg,
        phone: v => /^01[0-9]{8,9}$/.test(v.replace(/-/g, '')) ? '' : '올바른 휴대폰 번호를 입력해주세요.',
        password: v => {
            if (!v || v.length < 8) return '8자 이상 입력해주세요.';
            if (!/[A-Za-z]/.test(v)) return '영문을 포함해주세요.';
            if (!/\d/.test(v)) return '숫자를 포함해주세요.';
            if (!/[@$!%*#?&]/.test(v)) return '특수문자를 포함해주세요.';
            return '';
        },
        match: (a, b) => a === b ? '' : '비밀번호가 일치하지 않습니다.',
    };
})();

/* ── Fmt ── */
const Fmt = {
    money: n => {
        if (n == null || n === '') return '—';
        return Number(n).toLocaleString('ko-KR') + '원';
    },
};

/* ── 버튼 로딩 상태 ── */
function btnLoading(btn, on) {
    if (!btn) return;
    btn.disabled = on;
    btn.dataset.origText = btn.dataset.origText || btn.textContent;
    btn.textContent = on ? '처리 중…' : btn.dataset.origText;
}

/* ── 비밀번호 표시/숨김 토글 ── */
function initPwToggles() {
    document.querySelectorAll('.pw-toggle[data-target]').forEach(btn => {
        btn.addEventListener('click', () => {
            const inp = document.getElementById(btn.dataset.target);
            if (!inp) return;
            const isText = inp.type === 'text';
            inp.type = isText ? 'password' : 'text';
            btn.textContent = isText ? '표시' : '숨김';
        });
    });
}

/* ── 도넛 차트 (Chart.js) ── */
const CHART_COLORS = [
    '#C8102E', '#E8593C', '#F2A623', '#639922', '#1D9E75',
    '#378ADD', '#534AB7', '#D4537E', '#888780',
];

function renderDonut(canvasId, items, totalAmount) {
    const canvas = document.getElementById(canvasId);
    const legendEl = document.getElementById('chart-legend');

    // ── 범례 렌더링 (items 유무와 관계없이 스켈레톤 반드시 교체) ──
    if (legendEl) {
        if (!items || !items.length) {
            legendEl.innerHTML = '<p class="empty-text" style="font-size:12px;padding:8px 0;">소비 패턴 데이터가 없습니다.</p>';
        } else {
            const total = totalAmount || items.reduce((s, i) => s + Number(i.monthlyAmount ?? 0), 0);
            const colors = items.map((_, i) => CHART_COLORS[i % CHART_COLORS.length]);
            legendEl.innerHTML = items.map((item, i) => {
                const amt  = Number(item.monthlyAmount ?? 0);
                const pct  = total > 0 ? Math.round(amt / total * 100) : 0;
                return `
                <div class="legend-item">
                    <span class="legend-dot" style="background:${colors[i]};"></span>
                    <span class="legend-name">${item.categoryName ?? '—'}</span>
                    <span class="legend-pct">${pct}%</span>
                    <span class="legend-amt">${fmtMoney(amt)}</span>
                </div>`;
            }).join('');
        }
    }

    // ── 차트 렌더링 ──
    if (!canvas || !window.Chart) return;
    if (!items || !items.length) return;

    const colors = items.map((_, i) => CHART_COLORS[i % CHART_COLORS.length]);
    new Chart(canvas.getContext('2d'), {
        type: 'doughnut',
        data: {
            labels: items.map(i => i.categoryName),
            datasets: [{ data: items.map(i => Number(i.monthlyAmount ?? 0)), backgroundColor: colors, borderWidth: 2 }],
        },
        options: {
            cutout: '68%',
            plugins: { legend: { display: false }, tooltip: { enabled: true } },
        },
    });
}

/* ── 금액 입력 포매팅 ── */
function initMoneyInputs() {
    document.querySelectorAll('input[data-money]').forEach(inp => {
        inp.addEventListener('input', () => {
            const raw = inp.value.replace(/[^0-9]/g, '');
            inp.value = raw ? Number(raw).toLocaleString('ko-KR') : '';
            inp.dataset.raw = raw || '0';
        });
    });
}

/* ── 날짜 포맷 ── */
function fmtDate(str) { return str ? str.slice(0, 10) : '—'; }

/* ── 금액 포맷 (만원 단위) ── */
function fmtMoney(n) {
    if (n == null) return '—';
    const num = Number(n);
    if (num >= 10000) return (num / 10000).toFixed(num % 10000 === 0 ? 0 : 1) + '만원';
    return num.toLocaleString('ko-KR') + '원';
}

/* ── 이름 이니셜 ── */
function nameInitial(name) { return name ? name.charAt(0) : '?'; }

/* ── 한글 라벨 맵 ── */
const JOB_LABEL = {
    EMPLOYED: '직장인', SELF_EMPLOYED: '자영업자',
    STUDENT: '학생', UNEMPLOYED: '무직', OTHER: '기타',
};
const INCOME_LABEL = {
    LV1: 'LV1 (3천만 미만)', LV2: 'LV2 (3천~5천만)',
    LV3: 'LV3 (5천만~1억)', LV4: 'LV4 (1억 이상)',
};
const APP_STATUS_LABEL = {
    REQUESTED: '신청 접수', REVIEWING: '심사 중',
    APPROVED: '승인 완료', REJECTED: '신청 거절', ISSUED: '발급 완료',
};
const APP_STATUS_CLASS = {
    REQUESTED: 'badge--requested', REVIEWING: 'badge--reviewing',
    APPROVED: 'badge--approved', REJECTED: 'badge--rejected', ISSUED: 'badge--issued',
};
// 신청현황 이미지 없을 때 왼쪽 상태 칩 배경/텍스트 색상 맵
const APP_STATUS_CHIP = {
    REQUESTED: { bg: '#FEF3C7', color: '#B45309', short: '접수' },
    REVIEWING: { bg: '#DBEAFE', color: '#1D4ED8', short: '심사' },
    APPROVED:  { bg: '#DCFCE7', color: '#15803D', short: '승인' },
    REJECTED:  { bg: '#FEE2E2', color: '#DC2626', short: '거절' },
    ISSUED:    { bg: 'var(--red-pale)', color: 'var(--red)', short: '발급' },
};


/* ================================================================
   §2. 페이지 감지 + 초기화 진입
   ================================================================ */

document.addEventListener('DOMContentLoaded', () => {
    initPwToggles();
    if (document.getElementById('donutChart')) initMain();
    if (document.getElementById('editForm')) initEdit();
    if (document.getElementById('pwForm')) initPassword();
    if (document.getElementById('spendingForm')) initSpending();
});


/* ================================================================
   §3. 메인 대시보드 (index.html)
   ================================================================ */

async function initMain() {

    /* [1] 내 정보 — GET /api/users/me */
    try {
        const user = await API.get('/api/users/me');

        const initialEl = document.getElementById('profileInitial');
        if (initialEl) initialEl.textContent = nameInitial(user.name);

        const nameEl = document.getElementById('profileName');
        if (nameEl) nameEl.textContent = (user.name ?? '사용자') + ' 님';

        const metaEl = document.getElementById('profileMeta');
        if (metaEl) {
            const loginStr = user.lastLoginAt
                ? new Date(user.lastLoginAt).toLocaleString('ko-KR', { dateStyle: 'short', timeStyle: 'short' })
                : '—';
            metaEl.textContent = '마지막 로그인 ' + loginStr;
        }

        const scoreEl = document.getElementById('profileScore');
        if (scoreEl) scoreEl.textContent = user.creditScore ?? '—';

        const infoList = document.getElementById('infoList');
        if (infoList) {
            infoList.innerHTML = `
                <li><span class="info-label">이메일</span><span class="info-value">${user.email ?? '—'}</span></li>
                <li><span class="info-label">휴대전화</span><span class="info-value">${user.phone ?? '—'}</span></li>
                <li><span class="info-label">직업</span><span class="info-value">${JOB_LABEL[user.job] ?? '—'}</span></li>
                <li><span class="info-label">소득수준</span><span class="info-value">${INCOME_LABEL[user.incomeLevelCode] ?? '—'}</span></li>
            `;
        }
    } catch (err) {
        // ApiError: 403/500은 이미 Toast 처리됨. 나머지 오류만 표시.
        if (err.status !== 403 && err.status < 500) {
            Toast.error(err.message || '내 정보를 불러오지 못했습니다.');
        }
    }

    /* [2] 카드 탭 — GET /api/users/me/cards */
    const ownedTab = document.getElementById('tab-owned');
    const appliedTab = document.getElementById('tab-applied');
    const cardSection = document.getElementById('cardSection');

    async function loadCards(type = 'owned') {
        if (!cardSection) return;
        cardSection.innerHTML = '<p class="loading-text">불러오는 중...</p>';
        try {
			const cardStatus = await API.get('/api/users/me/cards');
			const list = type === 'owned'
			    ? (cardStatus?.ownedCards ?? [])
			    : (cardStatus?.applications ?? []);
            if (!list.length) {
                cardSection.innerHTML = `<p class="empty-text">${type === 'owned' ? '보유 카드가 없습니다.' : '신청 중인 카드가 없습니다.'}</p>`;
                return;
            }
            cardSection.innerHTML = list.map(c => {
                // ── 왼쪽 이미지/상태 영역 ──────────────────────────────
                let imgHtml;
                if (c.cardImageUrl) {
                    // 이미지 있음: 공통
                    imgHtml = `<div class="card-item__img-wrap">
                           <img class="card-item__img" src="${c.cardImageUrl}" alt="${c.cardName}">
                       </div>`;
                } else if (type === 'applied') {
                    // 신청현황 + 이미지 없음: 상태 색상 칩으로 표시
                    const chip = APP_STATUS_CHIP[c.applicationStatus] ?? { bg: 'var(--gray-100)', color: 'var(--gray-500)', short: '?' };
                    imgHtml = `<div class="card-item__img-wrap card-item__status-chip"
                                    style="background:${chip.bg}; border-color:${chip.bg};">
                                   <span class="card-item__status-chip-text" style="color:${chip.color};">${chip.short}</span>
                               </div>`;
                } else {
                    // 보유카드 + 이미지 없음: 카드명 이니셜 플레이스홀더
                    imgHtml = `<div class="card-item__img-wrap">
                           <div class="card-item__img-placeholder">${(c.cardName ?? 'CARD').charAt(0)}</div>
                       </div>`;
                }

                // ── 오른쪽 텍스트 영역 ────────────────────────────────
                const subHtml = type === 'applied'
                    ? `<div class="card-info__sub">
                           <span class="badge ${APP_STATUS_CLASS[c.applicationStatus] ?? ''}">${APP_STATUS_LABEL[c.applicationStatus] ?? c.applicationStatus}</span>
                           <span class="card-info__date">신청일: ${fmtDate(c.appliedAt)}</span>
                       </div>`
                    : `<div class="card-info__sub">발급일: ${fmtDate(c.issuedAt)}</div>`;

                return `
                <div class="card-item">
                    ${imgHtml}
                    <div class="card-info">
                        <div class="card-info__name">${c.cardName ?? '—'}</div>
                        ${subHtml}
                    </div>
                </div>`;
            }).join('');
        } catch (err) {
            if (err.status !== 403 && err.status < 500) {
                cardSection.innerHTML = `<p class="error-text">${err.message}</p>`;
            }
        }
    }

    ownedTab?.addEventListener('click', () => {
        ownedTab.classList.add('active');
        appliedTab?.classList.remove('active');
        loadCards('owned');
    });
    appliedTab?.addEventListener('click', () => {
        appliedTab.classList.add('active');
        ownedTab?.classList.remove('active');
        loadCards('applied');
    });
    loadCards('owned');

    /* [3] 소비 패턴 도넛 — GET /api/users/me/spending */
    try {
        const spending = await API.get('/api/users/me/spending');
        const items = Array.isArray(spending) ? spending : (spending?.items ?? []);
        const total = items.reduce((s, i) => s + Number(i.monthlyAmount ?? 0), 0);
        const totalEl = document.getElementById('chart-amount');  // index.html 실제 ID (Bug #3 수정)
        if (totalEl) totalEl.textContent = fmtMoney(total);
        renderDonut('donutChart', items, total);
    } catch (err) {
        if (err.status !== 403 && err.status < 500) {
            Toast.warning('소비 패턴 데이터를 불러오지 못했습니다.');
        }
    }
}


/* ================================================================
   §4. 내 정보 수정 (edit.html)
   ================================================================ */

async function initEdit() {

    // 초기 로드값 기억 (변경감지용)
    let _original = {};
    let _user = null;

    try {
        _user = await API.get('/api/users/me');

        document.getElementById('name').value            = _user.name            ?? '';
        document.getElementById('currentPhone').textContent = _user.phone        ?? '미등록';
        document.getElementById('job').value             = _user.job             ?? '';
        document.getElementById('incomeLevelCode').value = _user.incomeLevelCode ?? '';

        const pushEl = document.getElementById('pushEnabled');
        if (pushEl) pushEl.checked = _user.pushEnabled === 'Y' || _user.pushEnabled === true;
        const mktEl = document.getElementById('marketingAgree');
        if (mktEl) mktEl.checked = _user.marketingAgree === 'Y' || _user.marketingAgree === true;

        const creditScoreEl = document.getElementById('creditScore');
        if (creditScoreEl) creditScoreEl.value = _user.creditScore ?? '';

        // ✅ 초기값 스냅샷 저장 (변경감지 기준)
        _original = {
            name:            _user.name            ?? '',
            phone:           '',                          // 새 번호 입력란은 항상 빈 상태로 시작
            job:             _user.job             ?? '',
            incomeLevelCode: _user.incomeLevelCode ?? '',
            pushEnabled:     pushEl?.checked ?? false,
            marketingAgree:  mktEl?.checked  ?? false,
        };

    } catch (err) {
        if (err.status !== 403 && err.status < 500) {
            Toast.error('정보를 불러오지 못했습니다.');
        }
    }

    const form      = document.getElementById('editForm');
    const submitBtn = document.getElementById('submitBtn');
    const modal     = document.getElementById('pwConfirmModal');
    const pwInput   = document.getElementById('confirmPwInput');
    const pwErr     = document.getElementById('confirmPwErr');
    const confirmBtn = document.getElementById('modalConfirmBtn');

    // pushEnabled/marketingAgree 타입 정확히 처리
    function collectBody(password) {
        const phoneVal = document.getElementById('phone').value.trim();
        const pushEl   = document.getElementById('pushEnabled');
        const mktEl    = document.getElementById('marketingAgree');

        return {
            name:            document.getElementById('name').value.trim() || undefined,
            phone:           phoneVal || undefined,
            job:             document.getElementById('job').value || undefined,
            incomeLevelCode: document.getElementById('incomeLevelCode').value || undefined,
            // ✅ checkbox는 checked 값(boolean)을 그대로 전송 (서버 Boolean 타입과 일치)
            pushEnabled:     pushEl  ? pushEl.checked  : undefined,
            marketingAgree:  mktEl   ? mktEl.checked   : undefined,
            currentPassword: password,
        };
    }

    // 변경된 필드가 하나도 없으면 모달을 열지 않고 안내
    function hasChanges() {
        const pushEl = document.getElementById('pushEnabled');
        const mktEl  = document.getElementById('marketingAgree');
        return (
            (document.getElementById('name').value.trim()            !== _original.name)           ||
            (document.getElementById('phone').value.trim()           !== _original.phone)          ||
            (document.getElementById('job').value                    !== _original.job)            ||
            (document.getElementById('incomeLevelCode').value        !== _original.incomeLevelCode)||
            ((pushEl?.checked ?? false)                              !== _original.pushEnabled)    ||
            ((mktEl?.checked  ?? false)                              !== _original.marketingAgree)
        );
    }

    async function doUpdate(body) {
        btnLoading(submitBtn, true);
        try {
            await API.put('/api/users/me', body);
            Toast.success('정보가 수정되었습니다.');
            setTimeout(() => { window.location.href = '/mypage'; }, 1000);
        } catch (err) {
            if (err instanceof ApiError && err.applyFieldErrors(V.setErr)) {
                // 필드 수준 오류는 V.setErr로 표시됨
            } else if (err.code === 'U003' || err.message?.includes('비밀번호')) {
                // 비밀번호 틀렸을 때 모달을 다시 열어서 에러 표시
                if (pwInput) pwInput.value = '';
                if (pwErr) {
                    pwErr.textContent = '비밀번호가 올바르지 않습니다. 다시 입력해주세요.';
                    pwErr.classList.add('show');
                }
                modal?.classList.add('open');
                setTimeout(() => pwInput?.focus(), 150);
            } else if (err.status !== 403 && err.status < 500) {
                Toast.error(err.message || '수정 중 오류가 발생했습니다.');
            }
        } finally {
            btnLoading(submitBtn, false);
        }
    }

    form.addEventListener('submit', async e => {
        e.preventDefault();

        // 변경사항 없으면 안내 토스트만 표시
        if (!hasChanges()) {
            Toast.warning('변경된 내용이 없습니다.');
            return;
        }

        // 전화번호 형식 검증
        const phoneVal = document.getElementById('phone').value.trim();
        if (phoneVal && !V.setErr('phone', V.phone(phoneVal))) return;

        // 비밀번호 확인 모달 오픈
        if (pwInput) pwInput.value = '';
        if (pwErr)   { pwErr.textContent = ''; pwErr.classList.remove('show'); }
        modal?.classList.add('open');
        setTimeout(() => pwInput?.focus(), 150);
    });

    document.getElementById('modalCancelBtn')?.addEventListener('click', () => {
        modal?.classList.remove('open');
        if (pwInput) pwInput.value = '';
    });

    confirmBtn?.addEventListener('click', async () => {
        const pw = pwInput?.value?.trim() ?? '';
        if (!pw) {
            if (pwErr) { pwErr.textContent = '비밀번호를 입력해주세요.'; pwErr.classList.add('show'); }
            return;
        }
        pwErr?.classList.remove('show');
        modal?.classList.remove('open');
        await doUpdate(collectBody(pw));
    });

    pwInput?.addEventListener('keydown', e => { if (e.key === 'Enter') confirmBtn?.click(); });
    document.getElementById('phone')?.addEventListener('input', () => V.setErr('phone', ''));
}


/* ================================================================
   §5. 비밀번호 변경 (password.html)
   ================================================================ */

function initPassword() {
    const form = document.getElementById('pwForm');
    const submitBtn = document.getElementById('submitBtn');
    const newPw = document.getElementById('newPw');
    const confirmPw = document.getElementById('confirmPw');

    const rules = {
        length: { test: v => v.length >= 8 && v.length <= 50 },
        letter: { test: v => /[A-Za-z]/.test(v) },
        number: { test: v => /\d/.test(v) },
        special: { test: v => /[@$!%*#?&]/.test(v) },
    };
    document.querySelectorAll('#pwRules li').forEach(li => {
        rules[li.dataset.rule].el = li;
    });

    newPw.addEventListener('input', () => {
        const v = newPw.value;
        const strengthWrap = document.getElementById('strengthWrap');
        if (strengthWrap) strengthWrap.hidden = !v;

        let score = 0;
        Object.values(rules).forEach(r => {
            const pass = r.test(v);
            if (pass) score++;
            if (r.el) {
                r.el.classList.toggle('pass', pass);
                const text = r.el.textContent.replace(/^[✓✗]\s/, '');
                r.el.textContent = (pass ? '✓ ' : '✗ ') + text;
            }
        });

        const fill = document.getElementById('strengthFill');
        const label = document.getElementById('strengthLabel');
        if (fill) {
            fill.className = 'strength-fill';
            if (score <= 1) { fill.classList.add('fill-weak'); if (label) label.textContent = '보안 강도: 약함'; }
            else if (score <= 3) { fill.classList.add('fill-medium'); if (label) label.textContent = '보안 강도: 보통'; }
            else { fill.classList.add('fill-strong'); if (label) label.textContent = '보안 강도: 강함'; }
        }
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
                currentPassword: document.getElementById('currentPw').value,
                newPassword: newPw.value,
                newPasswordConfirm: confirmPw.value,
            });
            Toast.success('비밀번호가 변경되었습니다.');
            setTimeout(() => {
                const doneModal = document.getElementById('doneModal');
                if (doneModal) doneModal.classList.add('open');
            }, 500);
        } catch (err) {
            // ✅ fieldErrors → 각 필드에 직접 표시
            if (err instanceof ApiError && err.applyFieldErrors(V.setErr)) {
                // 이미 처리됨
            } else if (err.message?.includes('비밀번호') || err.message?.includes('password')) {
                V.setErr('currentPw', err.message);
            } else if (err.status !== 403 && err.status < 500) {
                Toast.error(err.message || '변경 중 오류가 발생했습니다.');
            }
        } finally {
            btnLoading(submitBtn, false);
        }
    });

    document.getElementById('doneOk')?.addEventListener('click', () => {
        window.location.href = '/mypage';
    });
}


/* ================================================================
   §6. 소비 패턴 관리 (spending.html)
   ──  전체 카테고리(GET /api/cards/categories) 로드 후
       기존 패턴(GET /api/users/me/spending) 금액 merge
   ================================================================ */

async function initSpending() {
    const container = document.getElementById('rowContainer');
    const totalEl = document.getElementById('totalAmount');  // spending.html 실제 ID (Bug #4 수정)

    /* ① 전체 카테고리 로드 */
    let allCategories = [];
    try {
        const cats = await API.get('/api/cards/categories');
        allCategories = Array.isArray(cats) ? cats : [];
    } catch (err) {
        if (err.status !== 403 && err.status < 500) {
            Toast.error('카테고리를 불러오지 못했습니다.');
        }
    }

    /* ② 기존 소비패턴 로드 → categoryId → monthlyAmount 맵 */
    const existingAmounts = {};
    try {
        const data = await API.get('/api/users/me/spending');
        const items = Array.isArray(data) ? data : (data?.items ?? []);
        items.forEach(i => { existingAmounts[i.categoryId] = i.monthlyAmount ?? 0; });
    } catch (err) {
        if (err.status !== 403 && err.status < 500) {
            Toast.warning('기존 소비 패턴을 불러오지 못했습니다. 새로 입력해주세요.');
        }
    }

    /* ③ 행 렌더링 */
    function updateTotal() {
        if (!totalEl) return;
        const total = [...document.querySelectorAll('input[data-money]')]
            .reduce((s, inp) => s + Number(inp.dataset.raw ?? 0), 0);
        totalEl.textContent = fmtMoney(total);
    }

    if (container) {
        container.innerHTML = allCategories.map((cat, idx) => {
            const existing = existingAmounts[cat.categoryId] ?? 0;
            const display = existing ? Number(existing).toLocaleString('ko-KR') : '';
            const color = CHART_COLORS[idx % CHART_COLORS.length];
            return `
                <div class="spending-row">
                    <span class="spending-dot" style="background:${color};"></span>
                    <label class="spending-label">${cat.categoryName}</label>
                    <input class="spending-input"
                           type="text" inputmode="numeric"
                           data-money data-raw="${existing}"
                           data-category-id="${cat.categoryId}"
                           value="${display}"
                           placeholder="0">
                </div>`;
        }).join('');
        initMoneyInputs();
        container.querySelectorAll('input[data-money]').forEach(inp => {
            inp.addEventListener('input', updateTotal);
        });
        updateTotal();
    }

    /* ④ 저장 — 변경된 항목만 필터링해서 전송 */
    document.getElementById('spendingForm')?.addEventListener('submit', async e => {
        e.preventDefault();
        const submitBtn = document.getElementById('submitBtn');

        // 현재 입력값 중 기존값과 달라진 것만 추출
        const changedItems = [...document.querySelectorAll('input[data-money]')]
            .filter(inp => {
                const current  = Number(inp.dataset.raw ?? 0);
                const original = existingAmounts[inp.dataset.categoryId] ?? 0;
                return current !== original;
            })
            .map(inp => ({
                categoryId:    Number(inp.dataset.categoryId),
                monthlyAmount: Number(inp.dataset.raw ?? 0),
            }));

        // 변경된 항목이 없으면 요청 자체를 막음
        if (changedItems.length === 0) {
            Toast.warning('변경된 내용이 없습니다.');
            return;
        }

        btnLoading(submitBtn, true);
        try {
            // 변경된 카테고리만 서버에 UPSERT (MERGE INTO 구조이므로 부분 전송 OK)
            await API.put('/api/users/me/spending', { patterns: changedItems });
            // 로컬 기준값 갱신 (다음 저장 시 비교 기준으로 사용)
            changedItems.forEach(item => {
                existingAmounts[item.categoryId] = item.monthlyAmount;
            });
            Toast.success(`${changedItems.length}개 항목이 저장되었습니다.`);
        } catch (err) {
            if (err instanceof ApiError && err.applyFieldErrors(V.setErr)) {
                // 필드 오류 처리됨
            } else if (err.status !== 403 && err.status < 500) {
                Toast.error(err.message || '저장 중 오류가 발생했습니다.');
            }
        } finally {
            btnLoading(submitBtn, false);
        }
    });
}