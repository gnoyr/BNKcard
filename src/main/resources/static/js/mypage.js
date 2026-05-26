/**
 * mypage.js  |  BNK 마이페이지 통합 스크립트
 *
 * 구성:
 *   §1. 공통 유틸  — API, Toast, V, Fmt, btnLoading, initPwToggles, renderDonut, initMoneyInputs
 *   §2. 페이지 감지 — DOM 요소 존재 여부로 현재 페이지 판별
 *   §3. 메인 대시보드 (index.html)
 *   §4. 내 정보 수정 (edit.html)
 *   §5. 비밀번호 변경 (password.html)
 *   §6. 소비 패턴 관리 (spending.html)
 *
 * [변경 이력]
 *   - initMain  : 카드 이미지(cardImageUrl) 렌더링 추가
 *   - initMain  : infoList 이메일·전화번호 원본(user.email / user.phone) 표시
 *   - initMain  : [변경] 이메일 인증 여부 항목 제거
 *   - initMain  : [변경] 카드 섹션을 보유카드 / 신청카드 탭으로 분리
 *   - initMain  : [변경] 신청카드에도 카드 이미지 렌더링 적용
 *   - initEdit  : currentPhone 원본(user.phone) 표시
 *   - initEdit  : 제출 시 항상 비밀번호 확인 모달 표시
 *   - initSpending : GET /api/cards/categories 로 전체 카테고리 로드 후 기존 패턴 merge
 *   - MemoryTokenStore → RedisTokenStore 전환 (백엔드 반영, JS 영향 없음)
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
function renderDonut(canvasId, items, totalAmount) {
    const canvas = document.getElementById(canvasId);
    if (!canvas || !window.Chart) return;

    const colors = items.map((_, i) => CHART_COLORS[i % CHART_COLORS.length]);

    new Chart(canvas.getContext('2d'), {
        type: 'doughnut',
        data: {
            labels: items.map(i => i.categoryName),
            datasets: [{
                data: items.map(i => Number(i.monthlyAmount ?? 0)),
                backgroundColor: colors,
                borderWidth: 0,
            }],
        },
        options: {
            cutout: '72%',
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        label: ctx => {
                            const pct = items[ctx.dataIndex]?.percentage
                                ?? items[ctx.dataIndex]?.ratio
                                ?? Math.round(ctx.parsed / totalAmount * 100);
                            return ` ${ctx.label}: ${Fmt.money(ctx.parsed)} (${pct}%)`;
                        },
                    },
                },
            },
        },
    });
}

/* ── 차트 팔레트 ── */
const CHART_COLORS = [
    '#C8102E', '#E8374F', '#F28B82', '#FF9800',
    '#4CAF50', '#2196F3', '#9C27B0', '#00BCD4',
    '#FF5722', '#795548', '#607D8B', '#E91E63',
];

function chartColor(categoryId) {
    return CHART_COLORS[(Number(categoryId) - 1) % CHART_COLORS.length];
}

/* ── 카테고리 이모지 ── */
const CAT_EMOJI = {
    FOOD: '🍽️',
    TRANSPORT: '🚌',
    SHOPPING: '🛍️',
    CULTURE: '🎭',
    TRAVEL: '✈️',
    HEALTH: '💊',
    EDUCATION: '📚',
    CAFE: '☕',
    CONVENIENCE: '🏪',
    BEAUTY: '💄',
    SPORTS: '⚽',
    PET: '🐾',
    HOUSING: '🏠',
    COMMUNICATION: '📱',
    INSURANCE: '🛡️',
    GAS: '⛽',
    PARKING: '🅿️',
    MART: '🛒',
    OTT: '📺',
    GAME: '🎮',
    DONATION: '💝',
    TAX: '🏛️',
    ETC: '💳',
};

/* ── 금액 입력 콤마 포맷 ── */
function initMoneyInputs(selector) {
    document.querySelectorAll(selector).forEach(inp => {
        inp.addEventListener('input', () => {
            const raw = inp.value.replace(/\D/g, '');
            inp.value = raw ? Number(raw).toLocaleString('ko-KR') : '';
            inp.dataset.raw = raw || '0';
        });
    });
}

/* ── 날짜 포맷 ── */
function fmtDate(str) {
    if (!str) return '—';
    return str.slice(0, 10);
}

/* ── 금액 포맷 (만원 단위) ── */
function fmtMoney(n) {
    if (n == null) return '—';
    const num = Number(n);
    if (num >= 10000) return (num / 10000).toFixed(num % 10000 === 0 ? 0 : 1) + '만원';
    return num.toLocaleString('ko-KR') + '원';
}

/* ── 이름 이니셜 ── */
function nameInitial(name) {
    if (!name) return '?';
    return name.charAt(0);
}

/* ── 한글 라벨 맵 ── */
const JOB_LABEL = {
    EMPLOYED: '직장인',
    SELF_EMPLOYED: '자영업자',
    STUDENT: '학생',
    UNEMPLOYED: '무직',
    OTHER: '기타',
};

const INCOME_LABEL = {
    LV1: 'LV1 (3천만 미만)',
    LV2: 'LV2 (3천~5천만)',
    LV3: 'LV3 (5천만~1억)',
    LV4: 'LV4 (1억 이상)',
};

const APP_STATUS_LABEL = {
    REQUESTED: '신청 접수',
    REVIEWING: '심사 중',
    APPROVED: '승인 완료',
    REJECTED: '신청 거절',
    ISSUED: '발급 완료',
};

const APP_STATUS_CLASS = {
    REQUESTED: 'badge--requested',
    REVIEWING: 'badge--reviewing',
    APPROVED: 'badge--approved',
    REJECTED: 'badge--rejected',
    ISSUED: 'badge--issued',
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

    /* ──────────────────────────────────────────
       [1] 내 정보 — GET /api/users/me
       ✅ 이메일 인증 여부 항목 제거
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
                { label: '이메일', value: user.email },
                { label: '휴대폰', value: user.phone },
                { label: '생년월일', value: user.birthDate },
                { label: '직업', value: JOB_LABEL[user.job] ?? user.job },
                { label: '소득 등급', value: INCOME_LABEL[user.incomeLevelCode] ?? user.incomeLevelCode },
                // ✅ 이메일 인증 여부 제거 (이전: { label: '이메일 인증', value: user.isEmailVerified })
                { label: '푸시 알림', value: user.pushEnabled },
                { label: '마케팅 동의', value: user.marketingAgree },
            ];

            infoList.innerHTML = rows.map(({ label, value }) => {
                const isBool = value === 'Y' || value === 'N' || typeof value === 'boolean';
                const on = value === 'Y' || value === true;

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
       [2] 카드 섹션 — GET /api/users/me/cards
       ✅ 보유카드 / 신청카드 탭 분리
       ✅ 신청카드에도 카드 이미지 렌더링
    ────────────────────────────────────────── */
    const cardSection = document.getElementById('cardSection');

    try {
        const data = await API.get('/api/users/me/cards');

        const owned = (data.ownedCards ?? []).map(c => ({
            type: 'owned',
            name: c.cardName,
            imageUrl: c.cardImageUrl,
            sub: '발급일 ' + fmtDate(c.issuedAt),
            status: 'ISSUED',
        }));

        const applied = (data.applications ?? []).map(a => ({
            type: 'applied',
            name: a.cardName,
            imageUrl: a.cardImageUrl,   // ✅ 신청카드도 이미지 사용
            sub: '신청일 ' + fmtDate(a.appliedAt),
            status: a.applicationStatus,
        }));

        /* ── 카드 아이템 HTML 생성 (owned/applied 공통 렌더러) ── */
        function renderCardItem(item) {
            const badgeClass = APP_STATUS_CLASS[item.status] ?? 'badge--requested';
            const badgeLabel = APP_STATUS_LABEL[item.status] ?? item.status;

            /* 이미지 — URL 있으면 img, 없으면 첫 글자 placeholder */
            const imgHtml = item.imageUrl
                ? `<img src="${item.imageUrl}" alt="${item.name ?? ''}" class="card-item__img"
                onerror="this.style.display='none';this.nextElementSibling.style.display='flex'">`
                : '';
            const placeholderStyle = item.imageUrl ? 'display:none' : '';

            return `<div class="card-item">
        <div class="card-item__img-wrap">
          ${imgHtml}
          <div class="card-item__img-placeholder" style="${placeholderStyle}">
            ${(item.name ?? '카').charAt(0)}
          </div>
        </div>
        <div class="card-info">
          <div class="card-info__name">${item.name ?? '—'}</div>
          <div class="card-info__sub">${item.sub}</div>
        </div>
        <span class="badge ${badgeClass}">${badgeLabel}</span>
      </div>`;
        }

        /* ── 탭 전환 함수 ── */
        function switchCardTab(tab) {
            /* 탭 버튼 active 처리 */
            cardSection.querySelectorAll('.card-tab-btn').forEach(btn => {
                btn.classList.toggle('card-tab-btn--active', btn.dataset.tab === tab);
            });

            /* 패널 렌더링 */
            const list = tab === 'owned' ? owned : applied;
            const emptyMsg = tab === 'owned' ? '보유 카드가 없습니다.' : '신청 내역이 없습니다.';
            const panelEl = document.getElementById('cardTabPanel');

            panelEl.innerHTML = list.length
                ? list.map(renderCardItem).join('')
                : `<div class="empty-state">${emptyMsg}</div>`;
        }

        /* ── 탭 헤더 + 패널 마운트 ── */
        cardSection.innerHTML = `
      <div class="card-tab-bar">
        <button class="card-tab-btn card-tab-btn--active" data-tab="owned">
          보유카드 <span class="card-tab-count">${owned.length}</span>
        </button>
        <button class="card-tab-btn" data-tab="applied">
          신청카드 <span class="card-tab-count">${applied.length}</span>
        </button>
      </div>
      <div id="cardTabPanel"></div>`;

        /* ── 탭 클릭 이벤트 바인딩 ── */
        cardSection.querySelectorAll('.card-tab-btn').forEach(btn => {
            btn.addEventListener('click', () => switchCardTab(btn.dataset.tab));
        });

        /* ── 초기 렌더: 보유카드 탭 ── */
        switchCardTab('owned');

    } catch {
        cardSection.innerHTML = `<div class="empty-state">카드 정보를 불러오지 못했습니다.</div>`;
    }


    /* ──────────────────────────────────────────
       [3] 소비 패턴 도넛 차트 — GET /api/users/me/spending
    ────────────────────────────────────────── */
    const spendingSection = document.getElementById('spendingSection');

    try {
        const data = await API.get('/api/users/me/spending');
        const items = Array.isArray(data) ? data : (data.items ?? []);
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

        const ctx = document.getElementById('donutChart').getContext('2d');
        const colors = active.map((_, i) => CHART_COLORS[i % CHART_COLORS.length]);

        new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: active.map(i => i.categoryName),
                datasets: [{
                    data: active.map(i => Number(i.monthlyAmount)),
                    backgroundColor: colors,
                    borderWidth: 0,
                }],
            },
            options: {
                cutout: '72%',
                plugins: {
                    legend: { display: false },
                    tooltip: {
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
                const pct = item.percentage ?? item.ratio
                    ?? Math.round(Number(item.monthlyAmount) / total * 100);
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
        document.getElementById('name').value = user.name ?? '';
        /* 원본 phone 사용 (마스킹 제거) */
        document.getElementById('currentPhone').textContent = user.phone ?? '미등록';
        document.getElementById('job').value = user.job ?? '';
        document.getElementById('incomeLevelCode').value = user.incomeLevelCode ?? '';
        document.getElementById('pushEnabled').checked = user.pushEnabled === 'Y' || user.pushEnabled === true;
        document.getElementById('marketingAgree').checked = user.marketingAgree === 'Y' || user.marketingAgree === true;
    } catch {
        Toast.error('정보를 불러오지 못했습니다.');
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

    /* 제출 시 항상 비밀번호 확인 모달 표시 */
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
    } catch {
        Toast.error('카테고리를 불러오지 못했습니다.');
    }

    /* ② 기존 소비패턴 로드 → categoryId → monthlyAmount 맵 */
    const existingAmounts = {};
    try {
        const data = await API.get('/api/users/me/spending');
        const items = Array.isArray(data) ? data : (data.items ?? []);
        items.forEach(i => {
            existingAmounts[String(i.categoryId)] = Number(i.monthlyAmount || 0);
        });
    } catch {
        /* 패턴 없어도 카테고리는 표시 */
    }

    if (allCategories.length === 0) {
        container.innerHTML = '<div class="empty-state">등록된 카테고리가 없습니다.</div>';
        return;
    }

    /* ③ 전체 카테고리 렌더링 (기존 금액 있으면 채움, 없으면 0) */
    container.innerHTML = allCategories.map(cat => {
        const color = chartColor(cat.categoryId);
        const amt = existingAmounts[String(cat.categoryId)] ?? 0;
        const display = amt > 0 ? amt.toLocaleString('ko-KR') : '';
        const emoji = CAT_EMOJI[cat.categoryCode] ?? '💳';

        return `
      <div class="spending-row" data-category-id="${cat.categoryId}">
        <span class="spending-dot" style="background:${color}"></span>
        <span class="spending-emoji" aria-hidden="true">${emoji}</span>
        <span class="spending-label">${cat.categoryName}</span>
        <input class="form-control spending-input money-input"
               type="text" inputmode="numeric"
               placeholder="0" value="${display}"
               data-raw="${amt}"
               aria-label="${cat.categoryName} 월 지출액"/>
        <span class="spending-unit">원</span>
      </div>`;
    }).join('');

    initMoneyInputs('.money-input');
    recalcTotal();
    container.addEventListener('input', recalcTotal);

    function recalcTotal() {
        const sum = [...document.querySelectorAll('.money-input')]
            .reduce((acc, inp) => acc + Number(inp.dataset.raw || 0), 0);
        if (totalEl) totalEl.textContent = Fmt.money(sum);
    }

    /* ④ 저장 */
    document.getElementById('spendingForm').addEventListener('submit', async e => {
        e.preventDefault();
        const btn = document.getElementById('submitBtn');

        const patterns = [...document.querySelectorAll('#rowContainer .spending-row')]
            .map(row => ({
                categoryId: Number(row.dataset.categoryId),
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