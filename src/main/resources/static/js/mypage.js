/**
 * mypage.js  |  BNK 마이페이지 통합 스크립트
 *
 * 의존: utils.js (BnkAPI, BnkToast, BnkDOM)
 * 로드 순서: utils.js → header.js → mypage.js
 *
 * 구성:
 *   §1. 로컬 별칭 + 마이페이지 전용 유틸
 *   §2. 페이지 감지 + 초기화 진입  (body[data-page])
 *   §3. 메인 대시보드      (mypage-main)
 *   §4. 내 정보 수정       (mypage-edit)
 *   §5. 비밀번호 변경      (mypage-password)
 *   §6. 소비 패턴 관리     (mypage-spending)
 *   §7. 신용점수 조회      (mypage-credit-score)
 *   §8. 신뢰 기기(IP) 관리 (mypage-trusted-ips)
 */

'use strict';

/* ================================================================
   §1. 로컬 별칭 + 마이페이지 전용 유틸
   ================================================================
   ※ API 통신·Toast·버튼 로딩은 utils.js(BnkAPI·BnkToast·BnkDOM)에
     전적으로 위임한다. 이 파일에서 직접 구현하지 않는다.
   ================================================================ */

/* ── utils.js 전역 객체 단축 별칭 ── */
const Toast = BnkToast;
const btnLoading = (btn, on) => BnkDOM.btnLoading(btn, on, '처리 중…');

/* ── API 래퍼
   BnkAPI는 { ok, status, data } 를 반환한다.
   mypage 전체에서 throw/catch 패턴을 통일하기 위해
   ok=false 면 ApiError 를 throw 하도록 얇게 한 번만 감싼다.
── */
class ApiError extends Error {
    constructor(data = {}, status = 0) {
        const msg =
            data.detail
            ?? data.fieldErrors?.[0]?.message
            ?? data.message
            ?? '오류가 발생했습니다.';
        super(msg);
        this.name = 'ApiError';
        this.status = status;
        this.code = data.code ?? null;
        this.fieldErrors = data.fieldErrors ?? [];
    }

    /** V.setErr 에 필드별 에러 메시지를 분산 적용 */
    applyFieldErrors(setErrFn) {
        if (!this.fieldErrors.length) return false;
        this.fieldErrors.forEach(fe => { if (fe.field) setErrFn(fe.field, fe.message); });
        return true;
    }
}

const API = (() => {
    async function req(method, url, body) {
        const res = await BnkAPI[method](url, body);
        if (!res.ok) throw new ApiError(res.data ?? {}, res.status);
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

/* ── Validator ── */
const V = (() => {
    function setErr(id, msg) {
        const el = document.getElementById(`${id}-err`) ?? document.getElementById(`${id}Err`);
        if (!el) return !msg;
        el.textContent = msg || '';
        el.classList.toggle('show', !!msg);
        return !msg;
    }
    return {
        setErr,
        required: (v, msg = '필수 입력 항목입니다.') => v?.trim() ? '' : msg,
        phone: v => /^01[0-9]{8,9}$/.test(v.replace(/-/g, ''))
            ? '' : '올바른 휴대폰 번호를 입력해주세요.',
        password: v => {
            if (!v || v.length < 8) return '8자 이상 입력해주세요.';
            if (v.length > 50) return '50자 이하로 입력해주세요.';
            if (!/[A-Za-z]/.test(v)) return '영문을 포함해주세요.';
            if (!/\d/.test(v)) return '숫자를 포함해주세요.';
            if (!/[@$!%*#?&]/.test(v)) return '특수문자(@$!%*#?&)를 포함해주세요.';
            return '';
        },
        match: (a, b) => a === b ? '' : '비밀번호가 일치하지 않습니다.',
    };
})();

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

/* ── 금액 입력 포맷 (천 단위 콤마) ── */
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
const fmtDate = str => str ? str.slice(0, 10) : '—';

/* ── 금액 포맷 (만원 단위 축약) ── */
function fmtMoney(n) {
    if (n == null) return '—';
    const num = Number(n);
    if (num >= 10_000) return (num / 10_000).toFixed(num % 10_000 === 0 ? 0 : 1) + '만원';
    return num.toLocaleString('ko-KR') + '원';
}

/* ── 이름 이니셜 ── */
const nameInitial = name => name ? name.charAt(0) : '?';

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
    REQUESTED: { short: '접수' },
    REVIEWING: { short: '심사' },
    APPROVED: { short: '승인' },
    REJECTED: { short: '거절' },
    ISSUED: { short: '발급' },
};

/* ── 도넛 차트 ── */
const CHART_COLORS = [
    '#C8102E', '#E8593C', '#F2A623', '#639922', '#1D9E75',
    '#378ADD', '#534AB7', '#D4537E', '#888780',
];

function renderDonut(canvasId, items, _total) {
    const canvas = document.getElementById(canvasId);
    const legendEl = document.getElementById('chart-legend');

    if (legendEl) {
        legendEl.innerHTML = items?.length
            ? items.map((item, i) => `
                <div class="legend-item">
                  <span class="legend-dot legend-dot--${i % CHART_COLORS.length}"></span>
                  <span class="legend-label">${item.categoryName ?? '기타'}</span>
                  <span class="legend-amount">${fmtMoney(item.monthlyAmount)}</span>
                </div>`).join('')
            : '<p class="chart-empty-msg">소비 패턴 데이터가 없습니다.</p>';
    }

    if (!canvas) return;

    if (typeof Chart === 'undefined') {
        canvas.parentElement?.insertAdjacentHTML(
            'afterend',
            '<p class="chart-error-msg">차트 라이브러리를 불러올 수 없습니다.</p>',
        );
        return;
    }

    const ctx = canvas.getContext('2d');

    if (!items?.length) {
        new Chart(ctx, {
            type: 'doughnut',
            data: { datasets: [{ data: [1], backgroundColor: ['#e4e4e7'], borderWidth: 0 }] },
            options: { cutout: '72%', plugins: { legend: { display: false }, tooltip: { enabled: false } } },
        });
        return;
    }

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
        options: { cutout: '72%', plugins: { legend: { display: false }, tooltip: { enabled: true } } },
    });
}

/* ================================================================
   §2. 페이지 감지 + 초기화 진입
   body[data-page] 속성으로 판별 — data-page 없으면 아무 것도 하지 않는다.
   (이전의 DOM id 폴백은 모든 페이지에 data-page 가 붙었으므로 제거)
   ================================================================ */
document.addEventListener('DOMContentLoaded', () => {
    initPwToggles();
    initMoneyInputs();

    const PAGE_MAP = {
        'mypage-main': initMain,
        'mypage-edit': initEdit,
        'mypage-password': initPassword,
        'mypage-spending': initSpending,
        'mypage-credit-score': initCreditScore,
        'mypage-trusted-ips': initTrustedIps,
    };
    PAGE_MAP[document.body.dataset.page]?.();
});

/* ================================================================
   §3. 메인 대시보드 (mypage-main)
   ================================================================ */
async function initMain() {

    /* [1] 내 정보 — GET /api/users/me */
    try {
        const user = await API.get('/api/users/me');

        const $set = (id, val) => {
            const el = document.getElementById(id);
            if (el) el.textContent = val;
        };

        $set('profileInitial', nameInitial(user.name));
        $set('profileName', (user.name ?? '사용자') + ' 님');
        $set('profileMeta', [
            JOB_LABEL[user.job] ?? '',
            user.lastLoginAt ? '최근 로그인: ' + fmtDate(user.lastLoginAt) : ''
        ].filter(Boolean).join(' · '));

        const infoList = document.getElementById('infoList');
        if (infoList) {
            infoList.innerHTML = [
                { label: '이메일', value: user.email },
                { label: '연락처', value: user.phone ?? '미등록' },
                { label: '직업', value: JOB_LABEL[user.job] ?? '—' },
                { label: '소득구간', value: INCOME_LABEL[user.incomeLevelCode] ?? '—' },
            ].map(({ label, value }) =>
                `<li class="info-item">
			       <span class="info-label">${label}</span>
			       <span class="info-value">${value ?? '—'}</span>
			     </li>`
            ).join('');
        }
    } catch (err) {
        if (err.status === 403) {
            Toast.error('접근 권한이 없습니다. 다시 로그인해 주세요.');
            setTimeout(() => location.replace('/auth/login'), 1500);
        } else if (err.status > 0 && err.status < 500) {
            Toast.error('내 정보를 불러오지 못했습니다.');
        }
    }
    /* [2] 보유 카드 / 신청 현황 탭 */
    const ownedTab = document.getElementById('tab-owned');
    const appliedTab = document.getElementById('tab-applied');
    const cardSection = document.getElementById('cardSection');

    const SKELETON = `
        <div class="skeleton-item"><span class="skeleton skeleton-chip"></span><span class="skeleton skeleton-text"></span></div>
        <div class="skeleton-item"><span class="skeleton skeleton-chip"></span><span class="skeleton skeleton-text skeleton-text--short"></span></div>`;

    /* GET /api/users/me/cards 단일 호출 → { ownedCards, applications } 캐시 */
    let _cardCache = null;

    async function fetchCardStatus() {
        if (_cardCache) return _cardCache;
        _cardCache = await API.get('/api/users/me/cards');
        return _cardCache;
    }

    /* 카드 아이템 HTML 생성 */
    function renderCardItem(c, type) {
        let imgHtml;
        if (c.cardImageUrl) {
            imgHtml = `<div class="card-item__img-wrap">
                           <img src="${c.cardImageUrl}" alt="${c.cardName ?? ''}" loading="lazy">
                       </div>`;
        } else if (type === 'applied') {
            const status = (c.applicationStatus ?? 'unknown').toLowerCase();
            imgHtml = `<div class="card-item__img-wrap card-item__status-chip card-item__status-chip--${status}">
                           <span class="card-item__status-chip-text">${APP_STATUS_CHIP[c.applicationStatus]?.short ?? '?'}</span>
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
    }

    async function renderTab(type) {
        if (!cardSection) return;
        cardSection.innerHTML = SKELETON;
        try {
            const status = await fetchCardStatus();
            /* ownedCards → owned 탭 / applications → applied 탭 */
            const list = type === 'owned'
                ? (status?.ownedCards ?? [])
                : (status?.applications ?? []);

            cardSection.innerHTML = list.length
                ? list.map(c => renderCardItem(c, type)).join('')
                : `<p class="empty-text">${type === 'owned' ? '보유한 카드가 없습니다.' : '신청 내역이 없습니다.'}</p>`;
        } catch (err) {
            if (err.status > 0 && err.status !== 403 && err.status < 500)
                cardSection.innerHTML = `<p class="error-text">${err.message}</p>`;
        }
    }

    /* 탭 전환 */
    function switchTab(activeTab, inactiveTab, type) {
        activeTab?.classList.add('active');
        inactiveTab?.classList.remove('active');
        renderTab(type);
    }
    ownedTab?.addEventListener('click', () => switchTab(ownedTab, appliedTab, 'owned'));
    appliedTab?.addEventListener('click', () => switchTab(appliedTab, ownedTab, 'applied'));
    renderTab('owned');

    /* [3] 소비 패턴 도넛 — GET /api/users/me/spending */
    try {
        const spending = await API.get('/api/users/me/spending');
        const items = Array.isArray(spending) ? spending : (spending?.items ?? []);
        const total = items.reduce((s, i) => s + Number(i.monthlyAmount ?? 0), 0);
        const totalEl = document.getElementById('chart-amount');
        if (totalEl) totalEl.textContent = fmtMoney(total);
        renderDonut('donutChart', items, total);
    } catch (err) {
        if (err.status > 0 && err.status !== 403 && err.status < 500) Toast.warning('소비 패턴 데이터를 불러오지 못했습니다.');
    }
}

/* ================================================================
   §4. 내 정보 수정 (mypage-edit)
   ================================================================ */
async function initEdit() {
    const form = document.getElementById('editForm');
    const submitBtn = document.getElementById('submitBtn');
    const modal = document.getElementById('pwConfirmModal');
    const pwInput = document.getElementById('confirmPwInput');
    const pwErr = document.getElementById('confirmPwErr');
    const confirmBtn = document.getElementById('modalConfirmBtn');

    let _original = {};
    let _phoneChanged = false; // phone input에 실제로 값을 입력했는지 추적

    /* ① 사용자 정보 로드 */
    try {
        const user = await API.get('/api/users/me');

        document.getElementById('name').value = user.name ?? '';
        // 마스킹된 번호는 currentPhone(표시용)에만 노출, phone input은 빈 상태 유지
        document.getElementById('currentPhone').textContent = user.phone ?? '미등록';
        document.getElementById('job').value = user.job ?? '';
        const emailEl = document.getElementById('currentEmail');
        if (emailEl) emailEl.textContent = user.email ?? '';

        document.getElementById('incomeLevelCode').value = user.incomeLevelCode ?? '';
        const pushEl = document.getElementById('pushEnabled');
        if (pushEl) pushEl.checked = user.pushEnabled === 'Y' || user.pushEnabled === true;
        const mktEl = document.getElementById('marketingAgree');
        if (mktEl) mktEl.checked = user.marketingAgree === 'Y' || user.marketingAgree === true;

        _original = {
            name: user.name ?? '',
            job: user.job ?? '',
            incomeLevelCode: user.incomeLevelCode ?? '',
            pushEnabled: pushEl?.checked ?? false,
            marketingAgree: mktEl?.checked ?? false,
        };
    } catch (err) {
        if (err.status > 0 && err.status !== 403 && err.status < 500) Toast.error('내 정보를 불러오지 못했습니다.');
    }

    /* ② 변경 감지 — 개인정보 / 알림설정 분리 */
    function hasPersonalInfoChange() {
        return (
            document.getElementById('name').value.trim() !== _original.name ||
            _phoneChanged ||   // phone은 input 이벤트 플래그로 감지 (마스킹 값 비교 불가)
            document.getElementById('job').value !== _original.job ||
            document.getElementById('incomeLevelCode').value !== _original.incomeLevelCode
        );
    }

    function hasNotificationChange() {
        const pushEl = document.getElementById('pushEnabled');
        const mktEl = document.getElementById('marketingAgree');
        return (
            (pushEl?.checked ?? false) !== _original.pushEnabled ||
            (mktEl?.checked ?? false) !== _original.marketingAgree
        );
    }

    function hasChanges() {
        return hasPersonalInfoChange() || hasNotificationChange();
    }

    /* ③ 전송 바디 수집
       - 개인정보 변경 시: currentPassword 포함
       - 알림 설정만 변경 시: currentPassword null (서버에서 검증 생략)
       - null 이면 서버 MyBatis <if test="... != null"> 에 의해 UPDATE 생략
    */
    function collectBody(currentPassword) {
        const pushEl = document.getElementById('pushEnabled');
        const mktEl = document.getElementById('marketingAgree');

        const body = {};

        if (currentPassword) body.currentPassword = currentPassword;

        const name = document.getElementById('name').value.trim();
        const phone = document.getElementById('phone').value.trim();
        const job = document.getElementById('job').value;
        const incomeCode = document.getElementById('incomeLevelCode').value;

        if (name !== _original.name) body.name = name;
        // phone은 플래그로만 판단 — 빈 값이면 phone 필드 미포함
        if (_phoneChanged && phone) body.phone = phone;
        if (job !== _original.job) body.job = job;
        if (incomeCode !== _original.incomeLevelCode) body.incomeLevelCode = incomeCode;

        // 알림 설정은 항상 현재 상태 전송
        body.pushEnabled = pushEl?.checked ?? false;
        body.marketingAgree = mktEl?.checked ?? false;

        return body;
    }

    /* ④ 실제 수정 요청 */
    async function doUpdate(currentPassword) {
        btnLoading(submitBtn, true);
        try {
            await API.put('/api/users/me', collectBody(currentPassword));
            Toast.success('정보가 수정되었습니다.');
            setTimeout(() => { window.location.href = '/mypage'; }, 1000);
        } catch (err) {
            if (err instanceof ApiError && err.applyFieldErrors(V.setErr)) {
                // 필드별 에러 표시됨
            } else if (err.code === 'U003' || err.code === 'C001' || err.message?.includes('비밀번호')) {
                // 비밀번호 불일치 또는 누락 → 모달 재오픈
                if (pwInput) pwInput.value = '';
                if (pwErr) { pwErr.textContent = '비밀번호가 올바르지 않습니다. 다시 입력해주세요.'; pwErr.classList.add('show'); }
                modal?.classList.add('open');
                setTimeout(() => pwInput?.focus(), 150);
            } else if (err.status > 0 && err.status !== 403 && err.status < 500) {
                Toast.error(err.message || '수정 중 오류가 발생했습니다.');
            }
        } finally {
            btnLoading(submitBtn, false);
        }
    }

    /* ⑤ 폼 제출
       - 개인정보(name/phone/job/incomeLevelCode/creditScore) 변경 → 비밀번호 확인 모달
       - 알림 설정(pushEnabled/marketingAgree)만 변경 → 비밀번호 없이 바로 저장
    */
    form?.addEventListener('submit', async e => {
        e.preventDefault();
        if (!hasChanges()) { Toast.warning('변경된 내용이 없습니다.'); return; }

        // 전화번호 형식 유효성 검사
        const phoneVal = document.getElementById('phone').value.trim();
        if (phoneVal) {
            const phoneErr = V.phone(phoneVal);
            if (phoneErr) { V.setErr('phone', phoneErr); return; }
        }
        V.setErr('phone', '');

        if (hasPersonalInfoChange()) {
            // 개인정보 변경 → 비밀번호 확인 필요
            if (pwInput) pwInput.value = '';
            if (pwErr) { pwErr.textContent = ''; pwErr.classList.remove('show'); }
            modal?.classList.add('open');
            setTimeout(() => pwInput?.focus(), 150);
        } else {
            // 알림 설정만 변경 → 비밀번호 없이 바로 저장
            await doUpdate(null);
        }
    });

    /* 모달 취소 */
    document.getElementById('modalCancelBtn')?.addEventListener('click', () => {
        modal?.classList.remove('open');
        if (pwInput) pwInput.value = '';
    });

    /* 모달 확인 */
    confirmBtn?.addEventListener('click', async () => {
        const pw = pwInput?.value?.trim() ?? '';
        if (!pw) {
            if (pwErr) { pwErr.textContent = '비밀번호를 입력해주세요.'; pwErr.classList.add('show'); }
            return;
        }
        pwErr?.classList.remove('show');
        modal?.classList.remove('open');
        await doUpdate(pw);
    });

    pwInput?.addEventListener('keydown', e => { if (e.key === 'Enter') confirmBtn?.click(); });
    document.getElementById('phone')?.addEventListener('input', () => {
        _phoneChanged = true;  // 한 글자라도 입력하면 변경으로 간주
        V.setErr('phone', '');
    });
}

/* ================================================================
   §5. 비밀번호 변경 (mypage-password)
   ================================================================ */
async function initPassword() {
    const form = document.getElementById('pwForm');
    const submitBtn = document.getElementById('submitBtn');
    const newPw = document.getElementById('newPw');
    const confirmPw = document.getElementById('confirmPw');
    const currentPw = document.getElementById('currentPw');

    form?.addEventListener('submit', async e => {
        e.preventDefault();

        let ok = true;
        if (!V.setErr('currentPw', V.required(currentPw?.value))) ok = false;
        if (!V.setErr('newPw', V.password(newPw?.value ?? ''))) ok = false;
        if (ok && currentPw.value === newPw.value) {
            V.setErr('newPw', '현재 비밀번호와 동일한 비밀번호는 사용할 수 없습니다.');
            ok = false;
        }
        if (ok && !V.setErr('confirmPw', V.match(newPw?.value ?? '', confirmPw?.value ?? ''))) ok = false;
        if (!ok) return;

        btnLoading(submitBtn, true);
        try {
            await API.patch('/api/users/me/password', {
                currentPassword: currentPw.value,
                newPassword: newPw.value,
                newPasswordConfirm: confirmPw.value,
            });
            Toast.success('비밀번호가 변경되었습니다.');
            setTimeout(() => {
                document.getElementById('doneModal')?.classList.add('open');
            }, 500);
        } catch (err) {
            if (err instanceof ApiError && err.applyFieldErrors(V.setErr)) {
                // 필드별 에러 표시됨
            } else if (err.code === 'U009') {
                // 새 비밀번호 확인 불일치 (서버 검증)
                V.setErr('confirmPw', '새 비밀번호와 확인이 일치하지 않습니다.');
            } else if (err.code === 'U003' || err.message?.includes('비밀번호') || err.message?.includes('password')) {
                V.setErr('currentPw', err.message);
            } else if (err.status > 0 && err.status !== 403 && err.status < 500) {
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
   §6. 소비 패턴 관리 (mypage-spending)
   ================================================================ */
async function initSpending() {
    const container = document.getElementById('rowContainer');
    const totalEl = document.getElementById('totalAmount');

    /* ① 카테고리 + 기존 소비패턴 병렬 로드 */
    let allCategories = [];
    let existingAmounts = {};

    try {
        const [cats, spending] = await Promise.allSettled([
            API.get('/api/cards/categories'),
            API.get('/api/users/me/spending'),
        ]);

        if (cats.status === 'fulfilled') {
            allCategories = Array.isArray(cats.value) ? cats.value : [];
        } else if (cats.reason?.status > 0 && cats.reason?.status !== 403 && cats.reason?.status < 500) {
            Toast.error('카테고리를 불러오지 못했습니다.');
        }

        if (spending.status === 'fulfilled') {
            const items = Array.isArray(spending.value) ? spending.value : (spending.value?.items ?? []);
            items.forEach(i => { existingAmounts[i.categoryId] = i.monthlyAmount ?? 0; });
        } else if (spending.reason?.status > 0 && spending.reason?.status !== 403 && spending.reason?.status < 500) {
            Toast.warning('기존 소비 패턴을 불러오지 못했습니다. 새로 입력해주세요.');
        }
    } catch {
        // 개별 에러는 위에서 처리됨
    }

    /* ② 카테고리 행 렌더링 */
    if (!container) return;
    if (!allCategories.length) {
        container.innerHTML = '<p class="empty-text">카테고리 데이터를 불러오지 못했습니다.</p>';
        return;
    }

    container.innerHTML = allCategories.map((cat, i) => {
        const existing = existingAmounts[cat.categoryId] ?? 0;
        const formatted = existing ? Number(existing).toLocaleString('ko-KR') : '';
        return `
        <div class="spending-row">
          <span class="spending-dot spending-dot--${i % CHART_COLORS.length}"></span>
          <span class="spending-label">${cat.categoryName}</span>
          <input class="spending-input money-input form-input"
                 type="text"
                 data-id="${cat.categoryId}"
                 data-raw="${existing}"
                 value="${formatted}"
                 placeholder="0">
        </div>`;
    }).join('');

    initMoneyInputs();

    /* ③ 합계 실시간 업데이트 */
    function updateTotal() {
        const sum = [...container.querySelectorAll('.spending-input')]
            .reduce((s, inp) => s + Number(inp.dataset.raw ?? 0), 0);
        if (totalEl) totalEl.textContent = Number(sum).toLocaleString('ko-KR') + '원';
    }
    container.addEventListener('input', updateTotal);
    updateTotal();

    /* ④ 저장 */
    document.getElementById('spendingForm')?.addEventListener('submit', async e => {
        e.preventDefault();
        const patterns = [...container.querySelectorAll('.spending-input')]
            .map(inp => ({ categoryId: Number(inp.dataset.id), monthlyAmount: Number(inp.dataset.raw ?? 0) }))
            .filter(item => item.monthlyAmount > 0);

        if (!patterns.length) {
            Toast.warning('저장할 금액이 없습니다. 1개 이상 입력해주세요.');
            return;
        }

        const submitBtn = document.getElementById('submitBtn');
        btnLoading(submitBtn, true);
        try {
            await API.put('/api/users/me/spending', { patterns });
            Toast.success('소비 패턴이 저장되었습니다.');
            setTimeout(() => { window.location.href = '/mypage'; }, 1000);
        } catch (err) {
            if (err.status > 0 && err.status !== 403 && err.status < 500)
                Toast.error(err.message || '저장 중 오류가 발생했습니다.');
        } finally {
            btnLoading(submitBtn, false);
        }
    });
}
async function initCreditScore() {
    let _scoreData = null;

    document.getElementById('revealBtn')
        ?.addEventListener('click', revealScore);
    document.getElementById('lockScreen')
        ?.querySelector('.btn-hide-score')
        ?.addEventListener('click', hideScore);

    // btn-hide-score 는 revealScreen 안에 있으므로 별도 바인딩
    // (onclick 인라인 속성 대신 이벤트 위임 사용)
    document.addEventListener('click', e => {
        if (e.target.closest('.btn-hide-score')) hideScore();
    });

    async function revealScore() {
        const btn = document.getElementById('revealBtn');
        btnLoading(btn, true);
        try {
            const user = await API.get('/api/users/me');
            _scoreData = user;
            renderScore(user.creditScore ?? 0);
        } catch (err) {
            if (err.status > 0 && err.status !== 403 && err.status < 500)
                Toast.error('점수 조회에 실패했습니다.');
        } finally {
            btnLoading(btn, false);
        }
    }

    function renderScore(score) {
        document.getElementById('lockScreen')?.classList.add('hidden');
        document.getElementById('revealScreen')?.classList.add('show');

        const scoreVal = document.getElementById('scoreVal');
        if (scoreVal) scoreVal.textContent = score;

        const pct = Math.max(0, Math.min(100, ((score - 300) / 600) * 100));
        setTimeout(() => {
            const fill = document.getElementById('gaugeFill');
            if (fill) fill.style.width = pct + '%';
        }, 80);

        let gradeClass, gradeLabel;
        if (score >= 820) { gradeClass = 'score-grade--excellent'; gradeLabel = '🏅 최우수'; }
        else if (score >= 665) { gradeClass = 'score-grade--good'; gradeLabel = '✅ 우수'; }
        else if (score >= 600) { gradeClass = 'score-grade--fair'; gradeLabel = '⚠️ 일반'; }
        else { gradeClass = 'score-grade--poor'; gradeLabel = '❗ 주의'; }

        const gradeEl = document.getElementById('scoreGrade');
        if (gradeEl) { gradeEl.className = 'score-grade ' + gradeClass; gradeEl.textContent = gradeLabel; }

        const pctEl = document.getElementById('scorePercentile');
        if (pctEl) pctEl.textContent = Math.max(1, Math.round(100 - pct)).toString();

        const updEl = document.getElementById('scoreUpdated');
        if (updEl) updEl.textContent = '조회 시각: ' + new Date().toLocaleString('ko-KR');
    }

    function hideScore() {
        document.getElementById('lockScreen')?.classList.remove('hidden');
        document.getElementById('revealScreen')?.classList.remove('show');
        const fill = document.getElementById('gaugeFill');
        if (fill) fill.style.width = '0%';
        _scoreData = null;
    }
}


/* ================================================================
   §8. 신뢰 기기(IP) 관리 (mypage-trusted-ips)
   ================================================================ */
async function initTrustedIps() {
    let _ipList = [];
    let _deleteTargetId = null;

    await loadIpList();
    bindDeleteModal();

    async function loadIpList() {
        try {
            const data = await API.get('/api/users/me/trusted-ips');
            _ipList = Array.isArray(data) ? data : (data?.items ?? data ?? []);
            renderList();
        } catch (err) {
            document.getElementById('ipList').innerHTML = emptyHtml('목록을 불러올 수 없습니다.');
        }
    }

    function renderList() {
        const list = document.getElementById('ipList');
        const countBadge = document.getElementById('ipCountBadge');
        const capacityWrap = document.getElementById('ipCapacityWrap');
        const capacityBar = document.getElementById('ipCapacityBar');
        const capacityLabel = document.getElementById('ipCapacityLabel');

        const count = _ipList.length;
        if (countBadge) countBadge.textContent = count + ' / 10';
        if (capacityWrap) capacityWrap.style.display = 'flex';
        const pct = (count / 10) * 100;
        if (capacityBar) { capacityBar.style.width = pct + '%'; capacityBar.className = 'ip-capacity__bar' + (pct >= 100 ? ' full' : pct >= 70 ? ' warn' : ''); }
        if (capacityLabel) capacityLabel.textContent = count + '/10 사용 중';

        if (!list) return;
        if (count === 0) { list.innerHTML = emptyHtml('등록된 기기가 없습니다.'); return; }
        list.innerHTML = _ipList.map(ip => buildIpItem(ip)).join('');
        attachItemEvents();
    }

    function emptyHtml(msg) {
        return `<li class="ip-empty"><p>${msg}</p></li>`;
    }

    function maskIp(addr) {
        const parts = addr.split('.');
        return parts.length === 4
            ? `${parts[0]}.${parts[1]}.*.${parts[3]}`
            : addr;
    }

    function formatVia(via) {
        return { SIGNUP: '<span class="via-signup">자동 등록</span>', EMAIL_VERIFY: '<span class="via-email">이메일 인증</span>', CI_VERIFY: '<span class="via-ci">CI 인증</span>', ADMIN: '<span class="via-admin">관리자 등록</span>' }[via] ?? via;
    }

    function fmtTs(ts) {
        if (!ts) return '—';
        try { return new Date(ts).toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' }); }
        catch { return ts; }
    }

    function esc(s) {
        return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    function buildIpItem(ip) {
        const isInitial = ip.isInitial === 'Y' || ip.is_initial === 'Y';
        const isDisabled = (ip.statusCode || ip.status_code) === 'DISABLED';
        const iconClass = isInitial ? 'ip-icon--initial' : (isDisabled ? 'ip-icon--disabled' : '');
        const nickname = ip.nickname || '내 기기';
        const addr = ip.ipAddress || ip.ip_address || '';
        const via = ip.registeredVia || ip.registered_via || 'SIGNUP';
        const lastUsed = ip.lastUsedAt || ip.last_used_at;
        const trustId = ip.trustId || ip.trust_id;

        return `<li class="ip-item" data-trust-id="${trustId}">
	  <div class="ip-icon ${iconClass}">🖥</div>
	  <div class="ip-body">
	    <div class="ip-nickname-row">
	      <span class="ip-nickname">${esc(nickname)}</span>
	      <input class="ip-nickname-input" type="text" maxlength="50" value="${esc(nickname)}" placeholder="별명 입력" aria-label="기기 별명" />
	      ${isInitial ? '<span class="ip-badge-initial">최초 가입 기기</span>' : ''}
	      ${isDisabled ? '<span class="ip-badge-disabled">비활성</span>' : ''}
	    </div>
	    <div class="ip-address">${maskIp(addr)}</div>
	    <div class="ip-meta">
	      <span>${lastUsed ? '마지막 접속 ' + fmtTs(lastUsed) : '접속 이력 없음'}</span>
	      <span>등록: ${formatVia(via)}</span>
	    </div>
	  </div>
	  <div class="ip-actions">
	    <button class="btn-ip-edit" data-action="edit">수정</button>
	    <button class="btn-ip-save" data-action="save">저장</button>
	    <button class="btn-ip-delete" data-action="delete" ${isInitial ? 'disabled title="최초 가입 기기는 삭제할 수 없습니다"' : ''}>삭제</button>
	  </div>
	</li>`;
    }

    function exitEditMode(span, input, editBtn, saveBtn) {
        span.classList.remove('editing');
        input.classList.remove('editing');
        editBtn.style.display = '';
        saveBtn.classList.remove('editing');
    }

    function attachItemEvents() {
        document.querySelectorAll('.ip-item').forEach(item => {
            const trustId = item.dataset.trustId;
            const editBtn = item.querySelector('[data-action="edit"]');
            const saveBtn = item.querySelector('[data-action="save"]');
            const deleteBtn = item.querySelector('[data-action="delete"]');
            const nicknameSpan = item.querySelector('.ip-nickname');
            const nicknameInput = item.querySelector('.ip-nickname-input');

            editBtn?.addEventListener('click', () => {
                nicknameSpan.classList.add('editing');
                nicknameInput.classList.add('editing');
                editBtn.style.display = 'none';
                saveBtn.classList.add('editing');
                nicknameInput.focus();
                nicknameInput.select();
            });

            saveBtn?.addEventListener('click', () => saveNickname(trustId, nicknameInput, nicknameSpan, editBtn, saveBtn));

            nicknameInput?.addEventListener('keydown', e => {
                if (e.key === 'Enter') { e.preventDefault(); saveBtn?.click(); }
                if (e.key === 'Escape') { nicknameInput.value = nicknameSpan.textContent; exitEditMode(nicknameSpan, nicknameInput, editBtn, saveBtn); }
            });

            deleteBtn?.addEventListener('click', () => {
                if (deleteBtn.disabled) return;
                _deleteTargetId = trustId;
                const nameEl = document.getElementById('deleteTargetName');
                if (nameEl) nameEl.textContent = nicknameSpan.textContent + '  (' + item.querySelector('.ip-address').textContent + ')';
                document.getElementById('deleteModal')?.classList.add('open');
            });
        });
    }

    async function saveNickname(trustId, input, span, editBtn, saveBtn) {
        const newNickname = input.value.trim();
        if (!newNickname) { Toast.error('별명은 비워둘 수 없습니다.'); input.focus(); return; }

        btnLoading(saveBtn, true);
        try {
            await API.patch('/api/users/me/trusted-ips/' + trustId, { nickname: newNickname });
            span.textContent = newNickname;
            Toast.success('별명이 수정되었습니다.');
        } catch (err) {
            Toast.error(err.message || '수정 중 오류가 발생했습니다.');
            input.value = span.textContent;
        } finally {
            btnLoading(saveBtn, false);
            exitEditMode(span, input, editBtn, saveBtn);
        }
    }

    function bindDeleteModal() {
        document.getElementById('deleteCancelBtn')?.addEventListener('click', () => {
            document.getElementById('deleteModal')?.classList.remove('open');
            _deleteTargetId = null;
        });

        document.getElementById('deleteConfirmBtn')?.addEventListener('click', async () => {
            if (!_deleteTargetId) return;
            const btn = document.getElementById('deleteConfirmBtn');
            btnLoading(btn, true);
            try {
                await API.del('/api/users/me/trusted-ips/' + _deleteTargetId);
                Toast.success('기기가 삭제되었습니다.');
                document.getElementById('deleteModal')?.classList.remove('open');
                _deleteTargetId = null;
                await loadIpList();
            } catch (err) {
                if (err.code === 'IP_INITIAL_DELETE_FORBIDDEN')
                    Toast.error('최초 가입 기기는 삭제할 수 없습니다.');
                else
                    Toast.error(err.message || '삭제 중 오류가 발생했습니다.');
            } finally {
                btnLoading(btn, false);
            }
        });
    }
}