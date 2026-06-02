/**
 * mypage.js  |  BNK 마이페이지 통합 스크립트
 *
 * 구성:
 *   §1. 공통 유틸  — ApiError, API, Toast, V, Fmt, btnLoading, initPwToggles, renderDonut, initMoneyInputs
 *   §2. 페이지 감지 — body[data-page] 속성으로 현재 페이지 판별  [FIX-1]
 *   §3. 메인 대시보드 (index.html)
 *   §4. 내 정보 수정 (edit.html)
 *   §5. 비밀번호 변경 (password.html)
 *   §6. 소비 패턴 관리 (spending.html)
 *
 * 인증: HttpOnly 쿠키 (access_token / refresh_token)
 *
 * ─────────────────────────────────────────────────────────────────
 */

'use strict';

/* ================================================================
   §1. 공통 유틸
   ================================================================ */

/* ── ApiError ── */
class ApiError extends Error {
    constructor(json = {}, status = 0) {
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
        this.fieldErrors = json.fieldErrors ?? [];
        this.serverMessage = json.message ?? null;
    }

    applyFieldErrors(setErrFn) {
        if (!this.fieldErrors.length) return false;
        this.fieldErrors.forEach(fe => { if (fe.field) setErrFn(fe.field, fe.message); });
        return true;
    }
}

/* ── API 래퍼
    401 시 즉시 리다이렉트 제거.
    header.js 가 refresh → 재확인 → 리다이렉트를 담당한다.
    여기서는 ApiError(401) 를 throw 하기만 하고,
    호출부에서 필요 시 처리한다.
    BnkAPI 단일 래핑 — 이중 래핑 제거
── */
const API = (() => {
    async function req(method, url, body) {
        let res;
        try {
            res = await BnkAPI[method](url, body);
        } catch {
            throw new ApiError({}, 0);
        }

        if (!res.ok) {
            // 401: header.js 가 이미 refresh 처리했거나 처리 중
            // 여기서는 에러만 throw — window.location 조작하지 않음
            const errJson = res.data ?? {};
            throw new ApiError(errJson, res.status);
        }

        return res.data?.data ?? res.data;
    }

    return {
        get: url => req('get', url),
        post: (url, b) => req('post', url, b),
        put: (url, b) => req('put', url, b),
        patch: (url, b) => req('patch', url, b),
        del: url => req('del', url),
    };
})();

/* ── Toast — BnkToast 위임 ── */
const Toast = BnkToast;

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
function btnLoading(btn, on) { BnkDOM.btnLoading(btn, on, '처리 중…'); }

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

/* ── 금액 입력 포맷 ── */
function initMoneyInputs() {
    document.querySelectorAll('.money-input').forEach(inp => {
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
const APP_STATUS_CHIP = {
    REQUESTED: { bg: '#FEF3C7', color: '#B45309', short: '접수' },
    REVIEWING: { bg: '#DBEAFE', color: '#1D4ED8', short: '심사' },
    APPROVED: { bg: '#DCFCE7', color: '#15803D', short: '승인' },
    REJECTED: { bg: '#FEE2E2', color: '#DC2626', short: '거절' },
    ISSUED: { bg: 'var(--red-pale)', color: 'var(--red)', short: '발급' },
};

/* ── 도넛 차트 (Chart.js) ── */
const CHART_COLORS = [
    '#C8102E', '#E8593C', '#F2A623', '#639922', '#1D9E75',
    '#378ADD', '#534AB7', '#D4537E', '#888780',
];

function renderDonut(canvasId, items, _totalAmount) {
    const canvas = document.getElementById(canvasId);
    const legendEl = document.getElementById('chart-legend');

    if (legendEl) {
        if (!items || !items.length) {
            legendEl.innerHTML = '<p class="empty-text" style="font-size:12px;padding:8px 0;">소비 패턴 데이터가 없습니다.</p>';
        } else {
            legendEl.innerHTML = items.map((item, i) => `
                <div class="legend-item">
                  <span class="legend-dot" style="background:${CHART_COLORS[i % CHART_COLORS.length]}"></span>
                  <span class="legend-label">${item.categoryName ?? '기타'}</span>
                  <span class="legend-amount">${fmtMoney(item.monthlyAmount)}</span>
                </div>`).join('');
        }
    }

    if (!canvas) return;

    // [FIX-4] Chart.js CDN 로드 실패 방어
    if (typeof Chart === 'undefined') {
        canvas.parentElement?.insertAdjacentHTML(
            'afterend',
            '<p style="font-size:12px;color:#888;text-align:center;">차트 라이브러리를 불러올 수 없습니다.</p>'
        );
        return;
    }

    if (!items || !items.length) {
        const ctx = canvas.getContext('2d');
        new Chart(ctx, {
            type: 'doughnut',
            data: { datasets: [{ data: [1], backgroundColor: ['#e4e4e7'], borderWidth: 0 }] },
            options: { cutout: '72%', plugins: { legend: { display: false }, tooltip: { enabled: false } } },
        });
        return;
    }

    const ctx = canvas.getContext('2d');
    new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: items.map(i => i.categoryName ?? '기타'),
            datasets: [{
                data: items.map(i => Number(i.monthlyAmount ?? 0)),
                backgroundColor: items.map((_, i) => CHART_COLORS[i % CHART_COLORS.length]),
                borderWidth: 2,
                borderColor: '#fff',
            }],
        },
        options: {
            cutout: '72%',
            plugins: { legend: { display: false }, tooltip: { enabled: true } },
        },
    });
}

/* ================================================================
   §2. 페이지 감지 + 초기화 진입
   body[data-page] 속성으로 판별
   (하위 호환: data-page 없으면 기존 DOM id 방식으로 폴백)
   ================================================================ */

document.addEventListener('DOMContentLoaded', () => {
    initPwToggles();
    initMoneyInputs();

    const page = document.body.dataset.page;

    if (page) {
        // data-page 명시 방식 (권장)
        if (page === 'mypage-main') initMain();
        if (page === 'mypage-edit') initEdit();
        if (page === 'mypage-password') initPassword();
        if (page === 'mypage-spending') initSpending();
    } else {
        // 폴백: 기존 DOM id 방식 (마이그레이션 기간 호환)
        if (document.getElementById('donutChart')) initMain();
        if (document.getElementById('editForm')) initEdit();
        if (document.getElementById('pwForm')) initPassword();
        if (document.getElementById('spendingForm')) initSpending();
    }
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
                ? '최근 로그인: ' + fmtDate(user.lastLoginAt)
                : '';
            const jobStr = JOB_LABEL[user.job] ?? '';
            metaEl.textContent = [jobStr, loginStr].filter(Boolean).join(' · ');
        }

        const scoreEl = document.getElementById('profileScore');
        if (scoreEl) scoreEl.textContent = user.creditScore != null ? String(user.creditScore) : '—';

        const infoList = document.getElementById('infoList');
        if (infoList) {
            infoList.innerHTML = [
                { label: '이메일', value: user.email },
                { label: '연락처', value: user.phone ?? '미등록' },
                { label: '직업', value: JOB_LABEL[user.job] ?? '—' },
                { label: '소득구간', value: INCOME_LABEL[user.incomeLevelCode] ?? '—' },
            ].map(({ label, value }) =>
                `<li class="info-item"><span class="info-item__label">${label}</span><span class="info-item__value">${value ?? '—'}</span></li>`
            ).join('');
        }
    } catch (err) {
        if (err.status !== 403 && err.status < 500) {
            Toast.error('내 정보를 불러오지 못했습니다.');
        }
    }

    /* [2] 보유 카드 / 신청 현황 탭 */
    const ownedTab = document.getElementById('tab-owned');
    const appliedTab = document.getElementById('tab-applied');
    const cardSection = document.getElementById('cardSection');

    async function loadCards(type) {
        if (!cardSection) return;
        cardSection.innerHTML = `
            <div class="skeleton-item"><span class="skeleton skeleton-chip"></span><span class="skeleton skeleton-text"></span></div>
            <div class="skeleton-item"><span class="skeleton skeleton-chip"></span><span class="skeleton skeleton-text skeleton-text--short"></span></div>`;
        try {
            const endpoint = type === 'owned' ? '/api/users/me/cards' : '/api/users/me/applications';
            const data = await API.get(endpoint);
            const list = Array.isArray(data) ? data : (data?.cards ?? data?.applications ?? []);

            if (!list.length) {
                cardSection.innerHTML = `<p class="empty-text">${type === 'owned' ? '보유한 카드가 없습니다.' : '신청 내역이 없습니다.'}</p>`;
                return;
            }

            cardSection.innerHTML = list.map(c => {
                let imgHtml;
                if (c.cardImageUrl) {
                    imgHtml = `<div class="card-item__img-wrap"><img src="${c.cardImageUrl}" alt="${c.cardName}" loading="lazy"></div>`;
                } else if (type === 'applied') {
                    const chip = APP_STATUS_CHIP[c.applicationStatus] ?? { bg: 'var(--gray-100)', color: 'var(--gray-500)', short: '?' };
                    imgHtml = `<div class="card-item__img-wrap card-item__status-chip"
                                    style="background:${chip.bg}; border-color:${chip.bg};">
                                   <span class="card-item__status-chip-text" style="color:${chip.color};">${chip.short}</span>
                               </div>`;
                } else {
                    imgHtml = `<div class="card-item__img-wrap">
                                   <div class="card-item__img-placeholder">${(c.cardName ?? 'CARD').charAt(0)}</div>
                               </div>`;
                }
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
        const totalEl = document.getElementById('chart-amount');
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
    let _original = {};
    let _user = null;

    try {
        _user = await API.get('/api/users/me');

        document.getElementById('name').value = _user.name ?? '';
        document.getElementById('currentPhone').textContent = _user.phone ?? '미등록';
        document.getElementById('job').value = _user.job ?? '';
        document.getElementById('incomeLevelCode').value = _user.incomeLevelCode ?? '';

        const pushEl = document.getElementById('pushEnabled');
        if (pushEl) pushEl.checked = _user.pushEnabled === 'Y' || _user.pushEnabled === true;
        const mktEl = document.getElementById('marketingAgree');
        if (mktEl) mktEl.checked = _user.marketingAgree === 'Y' || _user.marketingAgree === true;

        const creditScoreEl = document.getElementById('creditScore');
        if (creditScoreEl) creditScoreEl.value = _user.creditScore ?? '';

        _original = {
            name: _user.name ?? '',
            phone: '',
            job: _user.job ?? '',
            incomeLevelCode: _user.incomeLevelCode ?? '',
            pushEnabled: pushEl?.checked ?? false,
            marketingAgree: mktEl?.checked ?? false,
            creditScore: _user.creditScore ?? '',
        };
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
        const pushEl = document.getElementById('pushEnabled');
        const mktEl = document.getElementById('marketingAgree');
        return {
            name: document.getElementById('name').value.trim() || undefined,
            phone: phoneVal || undefined,
            job: document.getElementById('job').value || undefined,
            incomeLevelCode: document.getElementById('incomeLevelCode').value || undefined,
            creditScore: document.getElementById('creditScore')?.value || undefined,
            pushEnabled: pushEl ? pushEl.checked : undefined,
            marketingAgree: mktEl ? mktEl.checked : undefined,
            currentPassword: password,
        };
    }

    function hasChanges() {
        const pushEl = document.getElementById('pushEnabled');
        const mktEl = document.getElementById('marketingAgree');
        return (
            document.getElementById('name').value.trim() !== _original.name ||
            document.getElementById('phone').value.trim() !== _original.phone ||
            document.getElementById('job').value !== _original.job ||
            document.getElementById('incomeLevelCode').value !== _original.incomeLevelCode ||
            (document.getElementById('creditScore')?.value ?? '') !== _original.creditScore ||
            (pushEl?.checked ?? false) !== _original.pushEnabled ||
            (mktEl?.checked ?? false) !== _original.marketingAgree
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
                // 필드 수준 오류 표시됨
            } else if (err.code === 'U003' || err.message?.includes('비밀번호')) {
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

    form?.addEventListener('submit', async e => {
        e.preventDefault();
        if (!hasChanges()) { Toast.warning('변경된 내용이 없습니다.'); return; }
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

async function initPassword() {
    const form = document.getElementById('pwForm');
    const submitBtn = document.getElementById('submitBtn');
    const newPw = document.getElementById('newPw');
    const confirmPw = document.getElementById('confirmPw');

    form?.addEventListener('submit', async e => {
        e.preventDefault();
        let ok = true;
        if (!V.setErr('currentPw', V.required(document.getElementById('currentPw')?.value))) ok = false;
        if (!V.setErr('newPw', V.password(newPw?.value ?? ''))) ok = false;
        if (ok && document.getElementById('currentPw').value === newPw.value) {
            V.setErr('newPw', '현재 비밀번호와 동일한 비밀번호는 사용할 수 없습니다.');
            ok = false;
        }
        if (ok && !V.setErr('confirmPw', V.match(newPw?.value ?? '', confirmPw?.value ?? ''))) ok = false;
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
            if (err instanceof ApiError && err.applyFieldErrors(V.setErr)) {
                // 처리됨
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
        if (err.status !== 403 && err.status < 500) Toast.error('카테고리를 불러오지 못했습니다.');
    }

    /* ② 기존 소비패턴 로드 */
    const existingAmounts = {};
    try {
        const data = await API.get('/api/users/me/spending');
        const items = Array.isArray(data) ? data : (data?.items ?? []);
        items.forEach(i => { existingAmounts[i.categoryId] = i.monthlyAmount ?? 0; });
    } catch (err) {
        if (err.status !== 403 && err.status < 500) Toast.warning('기존 소비 패턴을 불러오지 못했습니다. 새로 입력해주세요.');
    }

    /* ③ 카테고리 행 렌더링 */
    if (!container) return;
    if (!allCategories.length) {
        container.innerHTML = '<p class="empty-text">카테고리 데이터를 불러오지 못했습니다.</p>';
        return;
    }

    const COLORS = CHART_COLORS;
    container.innerHTML = allCategories.map((cat, i) => {
        const existing = existingAmounts[cat.id] ?? 0;
        const formatted = existing ? Number(existing).toLocaleString('ko-KR') : '';
        return `
        <div class="spending-row">
          <span class="spending-dot" style="background:${COLORS[i % COLORS.length]}"></span>
          <span class="spending-label">${cat.name}</span>
          <input class="spending-input money-input form-input"
                 type="text"
                 data-id="${cat.id}"
                 data-raw="${existing}"
                 value="${formatted}"
                 placeholder="0">
        </div>`;
    }).join('');

    initMoneyInputs();

    /* ④ 합계 실시간 업데이트 */
    function updateTotal() {
        const inputs = container.querySelectorAll('.spending-input');
        const sum = [...inputs].reduce((s, inp) => s + Number(inp.dataset.raw ?? 0), 0);
        if (totalEl) totalEl.textContent = Number(sum).toLocaleString('ko-KR') + '원';
    }
    container.addEventListener('input', updateTotal);
    updateTotal();

    /* ⑤ 저장 */
    document.getElementById('spendingForm')?.addEventListener('submit', async e => {
        e.preventDefault();
        const inputs = container.querySelectorAll('.spending-input');
        const body = [...inputs]
            .map(inp => ({ categoryId: Number(inp.dataset.id), monthlyAmount: Number(inp.dataset.raw ?? 0) }))
            .filter(item => item.monthlyAmount > 0);

        const submitBtn = document.getElementById('submitBtn');
        btnLoading(submitBtn, true);
        try {
            await API.put('/api/users/me/spending', { items: body });
            Toast.success('소비 패턴이 저장되었습니다.');
            setTimeout(() => { window.location.href = '/mypage'; }, 1000);
        } catch (err) {
            if (err.status !== 403 && err.status < 500) Toast.error(err.message || '저장 중 오류가 발생했습니다.');
        } finally {
            btnLoading(submitBtn, false);
        }
    });
}
