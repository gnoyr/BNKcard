/**
 * admin-dashboard.js  |  BNK 관리자 대시보드
 * 역할(SUPER_ADMIN / MANAGER / OPERATOR)에 따라 대시보드 동적 렌더링
 * 의존: Chart.js 4.x, utils.js, header.js
 */

'use strict';

/* ================================================================
   1. 공통 유틸
================================================================ */

/** 관리자 API 호출 헬퍼 */
async function adminApi(method, url, body) {
    const opts = {
        method,
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
    };
    if (body) opts.body = JSON.stringify(body);
    const res  = await fetch(url, opts);
    const json = await res.json().catch(() => ({}));
    return { ok: res.ok, status: res.status, json };
}

/* ── 색상 팔레트 ── */
const COLORS = {
    teal:   '#4DB6C8',
    green:  '#81C995',
    blue:   '#7EB3E8',
    orange: '#FFAB76',
    purple: '#B39DDB',
    pink:   '#F48FB1',
    yellow: '#FFF176',
    gray:   '#B0BEC5',
};
const PALETTE = Object.values(COLORS);

/* ── 차트 공통 옵션 ── */
const CHART_DEFAULTS = {
    plugins: { legend: { labels: { font: { size: 12 } } } },
    responsive: true,
    maintainAspectRatio: true,
};

/* ── 배지 헬퍼 ── */
function roleBadge(code) {
    const cls = code === 'SUPER_ADMIN' ? 'badge-super'
              : code === 'MANAGER'     ? 'badge-manager'
              : 'badge-operator';
    return `<span class="badge ${cls}">${code ?? '-'}</span>`;
}

function resultBadge(result) {
    const cls   = result === 'S' ? 'badge-success' : 'badge-fail';
    const label = result === 'S' ? '성공' : '실패';
    return `<span class="badge ${cls}">${label}</span>`;
}

/* ── 잠금 해제 ── */
async function unlockUser(userId) {
    if (!confirm('잠금을 해제하시겠습니까?')) return;
    const r = await adminApi('PATCH', `/api/admin/users/${userId}/unlock`, {});
    if (!r.ok) { alert('처리 실패: ' + (r.json?.message ?? '오류')); return; }
    alert('잠금이 해제되었습니다.');
    initDashboard();
}

/* ── 차트 헬퍼 ── */
function drawDoughnut(canvasId, labels, data) {
    const ctx = document.getElementById(canvasId);
    if (!ctx) return;
    new Chart(ctx, {
        type: 'doughnut',
        data: { labels, datasets: [{ data, backgroundColor: PALETTE }] },
        options: CHART_DEFAULTS,
    });
}

function drawBar(canvasId, labels, datasets) {
    const ctx = document.getElementById(canvasId);
    if (!ctx) return;
    new Chart(ctx, {
        type: 'bar',
        data: { labels, datasets },
        options: { ...CHART_DEFAULTS, scales: { y: { beginAtZero: true } } },
    });
}

function drawLine(canvasId, labels, datasets) {
    const ctx = document.getElementById(canvasId);
    if (!ctx) return;
    new Chart(ctx, {
        type: 'line',
        data: { labels, datasets },
        options: {
            ...CHART_DEFAULTS,
            scales: { y: { beginAtZero: true } },
            elements: { line: { tension: 0.3 } },
        },
    });
}

/** 공통 활동 로그 테이블 렌더링 */
function renderActivityTable(tbodyId, items) {
    const tbody = document.getElementById(tbodyId);
    if (!tbody) return;
    if (!items.length) {
        tbody.innerHTML = '<tr class="empty-row"><td colspan="4">데이터가 없습니다.</td></tr>';
        return;
    }
    tbody.innerHTML = items.map(a => `
        <tr>
            <td>${a.ACTION     ?? a.action     ?? '-'}</td>
            <td>${resultBadge(a.RESULT ?? a.result)}</td>
            <td class="cell-muted">${a.TARGETID ?? a.targetId ?? '-'}</td>
            <td class="cell-muted">${a.OCCURREDAT ?? a.occurredAt ?? '-'}</td>
        </tr>`).join('');
}

/* ================================================================
   2. 진입점 — 역할별 분기
================================================================ */

async function initDashboard() {
    const r = await adminApi('GET', '/api/admin/dashboard/stats');
    if (!r.ok) {
        document.getElementById('dashboard-root').innerHTML =
            '<p class="text-error">데이터를 불러오지 못했습니다.</p>';
        return;
    }

    const data     = r.json.data ?? {};
    const roleCode = data.roleCode ?? (sessionStorage.getItem('bnk_admin_role') ?? 'OPERATOR');

    if      (roleCode === 'SUPER_ADMIN') renderSuperAdmin(data);
    else if (roleCode === 'MANAGER')     renderManager(data);
    else                                 renderOperator(data);
}

/* ================================================================
   3. SUPER_ADMIN 대시보드
================================================================ */

function renderSuperAdmin(data) {
    document.getElementById('page-title').textContent = '대시보드 — 최상위 관리자';
    const root = document.getElementById('dashboard-root');

    root.innerHTML = `
        <div class="summary-grid">
            <div class="summary-card">
                <div class="label">전체 회원</div>
                <div class="value">${(data.totalUsers ?? 0).toLocaleString()}</div>
                <div class="sub">오늘 가입 ${data.todaySignup ?? 0}명</div>
            </div>
            <div class="summary-card">
                <div class="label">정지 계정</div>
                <div class="value">${(data.suspendedUsers ?? 0).toLocaleString()}</div>
                <div class="sub">관리 필요</div>
            </div>
            <div class="summary-card">
                <div class="label">게시중 카드</div>
                <div class="value" id="sa-published-card">-</div>
                <div class="sub">전체 카드 현황</div>
            </div>
            <div class="summary-card">
                <div class="label">전체 관리자</div>
                <div class="value" id="sa-total-admin">-</div>
                <div class="sub">역할별 현황</div>
            </div>
        </div>

        <div class="shortcut-grid">
            <a href="/admin/cardManage.html" class="shortcut-btn">
                카드 / 약관 관리<div class="sc-label">카드 등록, 약관 관리</div>
            </a>
            <a href="/admin/admins.html" class="shortcut-btn">
                관리자 계정 관리<div class="sc-label">계정 등록, 역할 변경</div>
            </a>
            <a href="/admin/logs.html" class="shortcut-btn">
                상세 로그 조회<div class="sc-label">감사 로그, 활동 로그</div>
            </a>
        </div>

        <div class="chart-grid">
            <div class="chart-card"><h3>카드 상태 분포</h3><canvas id="chart-cards"></canvas></div>
            <div class="chart-card"><h3>관리자 역할 분포</h3><canvas id="chart-admins"></canvas></div>
            <div class="chart-card"><h3>최근 7일 결재 처리 현황</h3><canvas id="chart-approvals"></canvas></div>
            <div class="chart-card"><h3>최근 7일 회원 가입 추이</h3><canvas id="chart-signup"></canvas></div>
        </div>

        <div class="table-card">
            <h3>최근 관리자 활동 <a href="/admin/logs.html">전체 보기</a></h3>
            <table class="data-table">
                <thead><tr><th>관리자</th><th>역할</th><th>액션</th><th>결과</th><th>대상</th><th>일시</th></tr></thead>
                <tbody id="sa-activity-tbody">
                    <tr class="empty-row"><td colspan="6">불러오는 중...</td></tr>
                </tbody>
            </table>
        </div>`;

    /* 카드 상태 도넛 */
    const cardStatus = data.cardsByStatus ?? [];
    drawDoughnut('chart-cards',
        cardStatus.map(d => d.STATUS ?? d.status),
        cardStatus.map(d => d.CNT    ?? d.cnt));

    const published = cardStatus.find(d => (d.STATUS ?? d.status) === 'PUBLISHED');
    const el = document.getElementById('sa-published-card');
    if (el) el.textContent = (published?.CNT ?? published?.cnt ?? 0).toLocaleString();

    /* 관리자 역할 도넛 */
    const adminByRole = data.adminsByRole ?? [];
    drawDoughnut('chart-admins',
        adminByRole.map(d => d.ROLECODE ?? d.roleCode),
        adminByRole.map(d => d.CNT      ?? d.cnt));

    const totalAdmin = adminByRole.reduce((s, d) => s + (d.CNT ?? d.cnt ?? 0), 0);
    const elAdmin = document.getElementById('sa-total-admin');
    if (elAdmin) elAdmin.textContent = totalAdmin.toLocaleString();

    /* 결재 처리 현황 바 */
    const approvalTrend = data.approvalTrend ?? [];
    drawBar('chart-approvals',
        approvalTrend.map(d => d.DATE ?? d.date),
        [
            { label: '승인', backgroundColor: COLORS.green,
              data: approvalTrend.map(d => d.APPROVED ?? d.approved ?? 0) },
            { label: '반려', backgroundColor: COLORS.red,
              data: approvalTrend.map(d => d.REJECTED ?? d.rejected ?? 0) },
        ]);

    /* 회원 가입 추이 라인 */
    const signupTrend = data.signupTrend ?? [];
    drawLine('chart-signup',
        signupTrend.map(d => d.DATE ?? d.date),
        [{ label: '가입', borderColor: COLORS.blue,
           backgroundColor: 'rgba(21,101,192,.1)',
           data: signupTrend.map(d => d.CNT ?? d.cnt ?? 0) }]);

    /* 최근 관리자 활동 */
    const tbody = document.getElementById('sa-activity-tbody');
    const acts  = data.recentActivity ?? [];
    if (!acts.length) {
        tbody.innerHTML = '<tr class="empty-row"><td colspan="6">최근 활동이 없습니다.</td></tr>';
    } else {
        tbody.innerHTML = acts.map(a => `
            <tr>
                <td><b>${a.ADMINNAME ?? a.adminName ?? '-'}</b></td>
                <td>${roleBadge(a.ROLECODE ?? a.roleCode)}</td>
                <td>${a.ACTION    ?? a.action    ?? '-'}</td>
                <td>${resultBadge(a.RESULT ?? a.result)}</td>
                <td class="cell-muted">${a.TARGETID   ?? a.targetId   ?? '-'}</td>
                <td class="cell-muted">${a.OCCURREDAT ?? a.occurredAt ?? '-'}</td>
            </tr>`).join('');
    }
}

/* ================================================================
   4. MANAGER 대시보드
================================================================ */

function renderManager(data) {
    document.getElementById('page-title').textContent = '대시보드 — 중간 관리자';
    const root   = document.getElementById('dashboard-root');
    const pending = data.pendingApprovals ?? [];

    root.innerHTML = `
        <div class="summary-grid">
            <div class="summary-card">
                <div class="label">대기중 결재</div>
                <div class="value">${pending.length}</div>
                <div class="sub">처리 필요</div>
            </div>
            <div class="summary-card">
                <div class="label">게시중 카드</div>
                <div class="value" id="mg-published-card">-</div>
                <div class="sub">카드 현황</div>
            </div>
            <div class="summary-card">
                <div class="label">게시중 약관</div>
                <div class="value" id="mg-published-terms">-</div>
                <div class="sub">약관 현황</div>
            </div>
            <div class="summary-card">
                <div class="label">이번달 처리</div>
                <div class="value" id="mg-my-total">-</div>
                <div class="sub">내 결재 처리 건수</div>
            </div>
        </div>

        <div class="shortcut-grid">
            <a href="/admin/cardManage.html" class="shortcut-btn">
                카드 / 약관 관리<div class="sc-label">카드 등록, 약관 관리</div>
            </a>
            <a href="/admin/cardManage.html#approvals" class="shortcut-btn">
                결재 처리<div class="sc-label">대기중 결재 목록</div>
            </a>
            <a href="/admin/logs.html" class="shortcut-btn">
                상세 로그 조회<div class="sc-label">내 활동 로그</div>
            </a>
        </div>

        <div class="chart-grid">
            <div class="chart-card"><h3>카드 상태 분포</h3><canvas id="chart-cards"></canvas></div>
            <div class="chart-card"><h3>약관 상태 분포</h3><canvas id="chart-terms"></canvas></div>
            <div class="chart-card chart-card--full"><h3>이번달 내 결재 처리 현황</h3><canvas id="chart-my-approvals"></canvas></div>
        </div>

        <div class="table-card">
            <h3>대기중 결재 목록 <a href="/admin/approvalManage.html">전체 보기</a></h3>
            <table class="data-table">
                <thead><tr><th>결재 ID</th><th>유형</th><th>대상</th><th>요청자</th><th>일시</th></tr></thead>
                <tbody id="mg-pending-tbody">
                    <tr class="empty-row"><td colspan="5">불러오는 중...</td></tr>
                </tbody>
            </table>
        </div>

        <div class="table-card">
            <h3>내 최근 활동 <a href="/admin/logs.html">전체 보기</a></h3>
            <table class="data-table">
                <thead><tr><th>액션</th><th>결과</th><th>대상</th><th>일시</th></tr></thead>
                <tbody id="mg-activity-tbody"></tbody>
            </table>
        </div>`;

    /* 카드 상태 도넛 */
    const cardStatus = data.cardsByStatus ?? [];
    drawDoughnut('chart-cards',
        cardStatus.map(d => d.STATUS ?? d.status),
        cardStatus.map(d => d.CNT    ?? d.cnt));
    const pub = cardStatus.find(d => (d.STATUS ?? d.status) === 'PUBLISHED');
    const elCard = document.getElementById('mg-published-card');
    if (elCard) elCard.textContent = (pub?.CNT ?? pub?.cnt ?? 0).toLocaleString();

    /* 약관 상태 도넛 */
    const termsStatus = data.termsByStatus ?? [];
    drawDoughnut('chart-terms',
        termsStatus.map(d => d.STATUS ?? d.status),
        termsStatus.map(d => d.CNT    ?? d.cnt));
    const pubTerms = termsStatus.find(d => (d.STATUS ?? d.status) === 'PUBLISHED');
    const elTerms = document.getElementById('mg-published-terms');
    if (elTerms) elTerms.textContent = (pubTerms?.CNT ?? pubTerms?.cnt ?? 0).toLocaleString();

    /* 내 결재 처리 현황 바 */
    const myApprovals = data.myApprovalTrend ?? [];
    drawBar('chart-my-approvals',
        myApprovals.map(d => d.DATE ?? d.date),
        [
            { label: '승인', backgroundColor: COLORS.green,
              data: myApprovals.map(d => d.APPROVED ?? d.approved ?? 0) },
            { label: '반려', backgroundColor: COLORS.red,
              data: myApprovals.map(d => d.REJECTED ?? d.rejected ?? 0) },
        ]);
    const myTotal = myApprovals.reduce((s, d) =>
        s + (d.APPROVED ?? d.approved ?? 0) + (d.REJECTED ?? d.rejected ?? 0), 0);
    const elTotal = document.getElementById('mg-my-total');
    if (elTotal) elTotal.textContent = myTotal.toLocaleString();

    /* 대기중 결재 목록 */
    const pendingTbody = document.getElementById('mg-pending-tbody');
    if (!pending.length) {
        pendingTbody.innerHTML = '<tr class="empty-row"><td colspan="5">대기중인 결재가 없습니다.</td></tr>';
    } else {
        pendingTbody.innerHTML = pending.map(a => `
            <tr>
                <td class="cell-muted">${a.APPROVALID   ?? a.approvalId   ?? '-'}</td>
                <td>${a.APPROVALTYPE  ?? a.approvalType  ?? '-'}</td>
                <td>${a.TARGETNAME    ?? a.targetName    ?? '-'}</td>
                <td>${a.REQUESTERNAME ?? a.requesterName ?? '-'}</td>
                <td class="cell-muted">${a.CREATEDAT ?? a.createdAt ?? '-'}</td>
            </tr>`).join('');
    }

    renderActivityTable('mg-activity-tbody', data.myRecentActivity ?? []);
}

/* ================================================================
   5. OPERATOR 대시보드
================================================================ */

function renderOperator(data) {
    document.getElementById('page-title').textContent = '대시보드 — 하위 운영자';
    const root        = document.getElementById('dashboard-root');
    const lockedUsers = data.lockedUsers ?? [];

    root.innerHTML = `
        <div class="summary-grid">
            <div class="summary-card">
                <div class="label">전체 회원</div>
                <div class="value">${(data.totalUsers ?? 0).toLocaleString()}</div>
                <div class="sub">오늘 가입 ${data.todaySignup ?? 0}명</div>
            </div>
            <div class="summary-card">
                <div class="label">정지 계정</div>
                <div class="value">${(data.suspendedUsers ?? 0).toLocaleString()}</div>
                <div class="sub">관리 필요</div>
            </div>
            <div class="summary-card">
                <div class="label">잠금 계정</div>
                <div class="value">${lockedUsers.length}</div>
                <div class="sub">해제 필요</div>
            </div>
            <div class="summary-card">
                <div class="label">오늘 검색어</div>
                <div class="value" id="op-keyword-cnt">-</div>
                <div class="sub">TOP 5 집계</div>
            </div>
        </div>

        <div class="shortcut-grid">
            <a href="/admin/userManage.html" class="shortcut-btn">
                회원 관리<div class="sc-label">회원 조회, 잠금 해제</div>
            </a>
            <a href="/admin/logs.html" class="shortcut-btn">
                상세 로그 조회<div class="sc-label">로그인 로그, 내 활동</div>
            </a>
            <a href="/admin/cardManage.html" class="shortcut-btn">
                카드 조회<div class="sc-label">카드 현황 조회</div>
            </a>
        </div>

        <div class="chart-grid">
            <div class="chart-card"><h3>회원 상태 분포</h3><canvas id="chart-users"></canvas></div>
            <div class="chart-card"><h3>오늘 검색 키워드 TOP 5</h3><canvas id="chart-keywords"></canvas></div>
            <div class="chart-card chart-card--full"><h3>최근 7일 로그인 성공 / 실패 추이</h3><canvas id="chart-login"></canvas></div>
        </div>

        <div class="table-card">
            <h3>잠금 회원 목록 <a href="/admin/users.html">회원 관리로</a></h3>
            <table class="data-table">
                <thead><tr><th>회원 ID</th><th>이름</th><th>실패 횟수</th><th>잠금 해제일</th><th>처리</th></tr></thead>
                <tbody id="op-locked-tbody"></tbody>
            </table>
        </div>

        <div class="table-card">
            <h3>내 최근 활동 <a href="/admin/logs.html">전체 보기</a></h3>
            <table class="data-table">
                <thead><tr><th>액션</th><th>결과</th><th>대상</th><th>일시</th></tr></thead>
                <tbody id="op-activity-tbody"></tbody>
            </table>
        </div>`;

    /* 회원 상태 도넛 */
    const userStatus = data.usersByStatus ?? [];
    drawDoughnut('chart-users',
        userStatus.map(d => d.STATUS ?? d.status),
        userStatus.map(d => d.CNT    ?? d.cnt));

    /* 키워드 바 */
    const keywords = data.topKeywords ?? [];
    drawBar('chart-keywords',
        keywords.map(d => d.KEYWORD ?? d.keyword),
        [{ label: '검색 수', backgroundColor: COLORS.teal,
           data: keywords.map(d => d.CNT ?? d.cnt ?? 0) }]);
    const elKw = document.getElementById('op-keyword-cnt');
    if (elKw) elKw.textContent = keywords.length;

    /* 로그인 추이 라인 */
    const loginTrend = data.loginTrend ?? [];
    drawLine('chart-login',
        loginTrend.map(d => d.DATE ?? d.date),
        [
            { label: '성공', borderColor: COLORS.green,
              backgroundColor: 'rgba(46,125,50,.1)',
              data: loginTrend.map(d => d.SUCCESS ?? d.success ?? 0) },
            { label: '실패', borderColor: COLORS.red,
              backgroundColor: 'rgba(198,40,40,.08)',
              data: loginTrend.map(d => d.FAIL ?? d.fail ?? 0) },
        ]);

    /* 잠금 회원 목록 */
    const lockedTbody = document.getElementById('op-locked-tbody');
    if (!lockedUsers.length) {
        lockedTbody.innerHTML = '<tr class="empty-row"><td colspan="5">잠금된 회원이 없습니다.</td></tr>';
    } else {
        lockedTbody.innerHTML = lockedUsers.map(u => `
            <tr>
                <td class="cell-muted">${u.userId ?? '-'}</td>
                <td>${u.name ?? '-'}</td>
                <td>${u.failCount ?? '-'}</td>
                <td class="cell-muted">${u.lockedUntil ?? '-'}</td>
                <td><button class="btn-unlock" onclick="unlockUser(${u.userId})">해제</button></td>
            </tr>`).join('');
    }

    renderActivityTable('op-activity-tbody', data.myRecentActivity ?? []);
}

/* ================================================================
   6. 초기화
================================================================ */

document.addEventListener('DOMContentLoaded', () => initDashboard());
