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
 * [변경 이력]
 *   - §1 ApiError  : GlobalExceptionHandler 응답 구조를 전달하는 전용 에러 클래스 추가
 *   - §1 API.req() : detail > fieldErrors > message 우선순위로 throw ApiError
 *   - §1 API.req() : 403/500 상태코드 Toast로 즉시 표시 (caller까지 전파하지 않음)
 *   - §4 initEdit  : fieldErrors 수신 시 V.setErr()로 해당 필드에 직접 표시
 *   - §5 initPassword : fieldErrors 수신 시 V.setErr()로 해당 필드에 직접 표시
 *   - §6 initSpending : catch 블록 Toast.error() 유지 (개선된 메시지)
 *
 * 인증: HttpOnly 쿠키 (access_token / refresh_token)
 *       credentials:'include' 만 사용 — JS 쿠키 직접 읽기 없음
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

function renderDonut(canvasId, items, _totalAmount) {
    const canvas = document.getElementById(canvasId);
    if (!canvas || !window.Chart) return;
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
            const cards = await API.get(`/api/users/me/cards?type=${type}`);
            const list = Array.isArray(cards) ? cards : (cards?.items ?? []);
            if (!list.length) {
                cardSection.innerHTML = `<p class="empty-text">${type === 'owned' ? '보유 카드가 없습니다.' : '신청 중인 카드가 없습니다.'}</p>`;
                return;
            }
            cardSection.innerHTML = list.map(c => `
                <div class="card-item">
                    ${c.cardImageUrl ? `<img class="card-img" src="${c.cardImageUrl}" alt="${c.cardName}">` : ''}
                    <div class="card-info">
                        <div class="card-name">${c.cardName ?? '—'}</div>
                        ${type === 'applied'
                    ? `<span class="badge ${APP_STATUS_CLASS[c.applicationStatus] ?? ''}">${APP_STATUS_LABEL[c.applicationStatus] ?? c.applicationStatus}</span>`
                    : `<div class="card-date">발급일: ${fmtDate(c.issuedAt)}</div>`}
                    </div>
                </div>`).join('');
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
        const totalEl = document.getElementById('totalSpending');
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

    try {
        const user = await API.get('/api/users/me');
        document.getElementById('name').value = user.name ?? '';
        document.getElementById('currentPhone').textContent = user.phone ?? '미등록';
        document.getElementById('job').value = user.job ?? '';
        document.getElementById('incomeLevelCode').value = user.incomeLevelCode ?? '';
        document.getElementById('pushEnabled').checked = user.pushEnabled === 'Y' || user.pushEnabled === true;
        document.getElementById('marketingAgree').checked = user.marketingAgree === 'Y' || user.marketingAgree === true;
    } catch (err) {
        if (err.status !== 403 && err.status < 500) {
            Toast.error('정보를 불러오지 못했습니다.');
        }
    }

    const form = document.getElementById('editForm');
    const submitBtn = document.getElementById('submitBtn');
    const modal = document.getElementById('pwConfirmModal');
    const pwInput = document.getElementById('confirmPwInput');
    const pwErr = document.getElementById('confirmPwErr');
    const confirmBtn = document.getElementById('modalConfirmBtn');

    function collectBody(password) {
        const phoneVal = document.getElementById('phone').value.trim();
        return {
            name: document.getElementById('name').value.trim() || undefined,
            phone: phoneVal || undefined,
            job: document.getElementById('job').value.trim() || undefined,
            incomeLevelCode: document.getElementById('incomeLevelCode').value || undefined,
            pushEnabled: document.getElementById('pushEnabled').checked,
            marketingAgree: document.getElementById('marketingAgree').checked,
            currentPassword: password,
        };
    }

    async function doUpdate(body) {
        btnLoading(submitBtn, true);
        try {
            await API.put('/api/users/me', body);
            Toast.success('정보가 수정되었습니다.');
			setTimeout(() => { window.location.href = '/mypage'; }, 1000);
        } catch (err) {
            // ✅ fieldErrors → 각 필드에 직접 표시
            if (err instanceof ApiError && err.applyFieldErrors(V.setErr)) {
                // 필드 수준 오류는 이미 V.setErr로 표시됨
            } else if (err.message?.includes('비밀번호')) {
                if (pwErr) {
                    pwErr.textContent = err.message;
                    pwErr.classList.add('show');
                }
            } else if (err.status !== 403 && err.status < 500) {
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

        if (pwInput) pwInput.value = '';
        if (pwErr) { pwErr.textContent = ''; pwErr.classList.remove('show'); }
        modal?.classList.add('open');
        setTimeout(() => pwInput?.focus(), 150);
    });

    document.getElementById('modalCancelBtn')?.addEventListener('click', () => {
        modal?.classList.remove('open');
        if (pwInput) pwInput.value = '';
    });

    confirmBtn?.addEventListener('click', async () => {
        const pw = pwInput?.value ?? '';
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
    const totalEl = document.getElementById('totalAmount');

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
        container.innerHTML = allCategories.map(cat => {
            const existing = existingAmounts[cat.categoryId] ?? 0;
            const display = existing ? Number(existing).toLocaleString('ko-KR') : '';
            return `
                <div class="spending-row">
                    <label class="category-label">${cat.categoryName}</label>
                    <input class="form-input money-input"
                           type="text" inputmode="numeric"
                           data-money data-raw="${existing}"
                           data-category-id="${cat.categoryId}"
                           value="${display}"
                           placeholder="월 소비금액 (원)">
                </div>`;
        }).join('');
        initMoneyInputs();
        container.querySelectorAll('input[data-money]').forEach(inp => {
            inp.addEventListener('input', updateTotal);
        });
        updateTotal();
    }

    /* ④ 저장 */
    document.getElementById('spendingForm')?.addEventListener('submit', async e => {
        e.preventDefault();
        const submitBtn = document.getElementById('spendingSubmitBtn');
        btnLoading(submitBtn, true);
        try {
            const items = [...document.querySelectorAll('input[data-money]')].map(inp => ({
                categoryId: Number(inp.dataset.categoryId),
                monthlyAmount: Number(inp.dataset.raw ?? 0),
            }));
            await API.put('/api/users/me/spending', { items });
            Toast.success('소비 패턴이 저장되었습니다.');
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