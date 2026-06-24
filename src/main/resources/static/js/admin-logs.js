/**
 * admin-logs.js  |  BNK 관리자 로그 조회
 * 관리자 활동 로그 / 감사 로그 / 회원 활동 로그 — 탭 전환 + 필터 + 페이지네이션
 * 의존: utils.js, header.js
 */

'use strict';

/* ================================================================
   1. 상태 변수
================================================================ */

let currentTab  = 'admin';
let currentPage = 0;
const PAGE_SIZE = 20;

/* ================================================================
   2. API 헬퍼
================================================================ */

async function adminApi(method, url) {
    const res  = await fetch(url, { method, credentials: 'include' });
    const json = await res.json().catch(() => ({}));
    return { ok: res.ok, json };
}

/* ================================================================
   3. 탭 전환
================================================================ */

function switchTab(tab, btn) {
    currentTab  = tab;
    currentPage = 0;
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    renderFilter();
    loadLogs();
}

/* ================================================================
   4. 필터 렌더링 (탭별로 다름)
================================================================ */

function renderFilter() {
    const row = document.getElementById('filter-row');

    if (currentTab === 'admin') {
        row.innerHTML = `
            <div class="filter-group">
                <label>관리자명</label>
                <input type="text" id="f-admin-name" placeholder="이름 검색">
            </div>
            <div class="filter-group">
                <label>역할</label>
                <select id="f-role">
                    <option value="">전체</option>
                    <option value="SUPER_ADMIN">SUPER_ADMIN</option>
                    <option value="MANAGER">MANAGER</option>
                    <option value="OPERATOR">OPERATOR</option>
                </select>
            </div>
            <div class="filter-group">
                <label>결과</label>
                <select id="f-result">
                    <option value="">전체</option>
                    <option value="S">성공</option>
                    <option value="F">실패</option>
                </select>
            </div>
            <div class="filter-group">
                <label>시작일</label>
                <input type="date" id="f-from">
            </div>
            <div class="filter-group">
                <label>종료일</label>
                <input type="date" id="f-to">
            </div>
            <div class="filter-group" style="justify-content:flex-end;">
                <button class="btn-primary" style="margin-top:0;padding:8px 20px;"
                        onclick="currentPage=0; loadLogs();">조회</button>
            </div>`;

    } else if (currentTab === 'audit') {
        row.innerHTML = `
            <div class="filter-group">
                <label>액션</label>
                <input type="text" id="f-action" placeholder="액션 검색">
            </div>
            <div class="filter-group">
                <label>결과</label>
                <select id="f-result">
                    <option value="">전체</option>
                    <option value="S">성공</option>
                    <option value="F">실패</option>
                </select>
            </div>
            <div class="filter-group">
                <label>IP</label>
                <input type="text" id="f-ip" placeholder="IP 검색">
            </div>
            <div class="filter-group">
                <label>시작일</label>
                <input type="date" id="f-from">
            </div>
            <div class="filter-group">
                <label>종료일</label>
                <input type="date" id="f-to">
            </div>
            <div class="filter-group" style="justify-content:flex-end;">
                <button class="btn-primary" style="margin-top:0;padding:8px 20px;"
                        onclick="currentPage=0; loadLogs();">조회</button>
            </div>`;

    } else {
        row.innerHTML = `
            <div class="filter-group">
                <label>액션</label>
                <input type="text" id="f-action" placeholder="액션 검색">
            </div>
            <div class="filter-group">
                <label>결과</label>
                <select id="f-result">
                    <option value="">전체</option>
                    <option value="S">성공</option>
                    <option value="F">실패</option>
                </select>
            </div>
            <div class="filter-group">
                <label>시작일</label>
                <input type="date" id="f-from">
            </div>
            <div class="filter-group">
                <label>종료일</label>
                <input type="date" id="f-to">
            </div>
            <div class="filter-group" style="justify-content:flex-end;">
                <button class="btn-primary" style="margin-top:0;padding:8px 20px;"
                        onclick="currentPage=0; loadLogs();">조회</button>
            </div>`;
    }
}

/* ================================================================
   5. 쿼리 파라미터 빌드
================================================================ */

function buildParams() {
    const p   = new URLSearchParams({ page: currentPage, size: PAGE_SIZE });
    const get = id => {
        const el = document.getElementById(id);
        return el ? el.value.trim() : '';
    };

    if (currentTab === 'admin') {
        if (get('f-admin-name')) p.set('adminName', get('f-admin-name'));
        if (get('f-role'))       p.set('roleCode',  get('f-role'));
        if (get('f-result'))     p.set('result',    get('f-result'));
        if (get('f-from'))       p.set('from',      get('f-from'));
        if (get('f-to'))         p.set('to',        get('f-to'));

    } else if (currentTab === 'audit') {
        if (get('f-action')) p.set('action', get('f-action'));
        if (get('f-result')) p.set('result', get('f-result'));
        if (get('f-ip'))     p.set('ip',     get('f-ip'));
        if (get('f-from'))   p.set('from',   get('f-from'));
        if (get('f-to'))     p.set('to',     get('f-to'));

    } else {
        if (get('f-action')) p.set('action', get('f-action'));
        if (get('f-result')) p.set('result', get('f-result'));
        if (get('f-from'))   p.set('from',   get('f-from'));
        if (get('f-to'))     p.set('to',     get('f-to'));
    }
    return p;
}

/* ================================================================
   6. 로그 조회
================================================================ */

async function loadLogs() {
    const tbody = document.getElementById('table-body');
    tbody.innerHTML = '<tr class="empty-row"><td colspan="7">불러오는 중...</td></tr>';

    const params = buildParams();
    const urlMap = {
        admin: `/api/admin/logs/admin-activity?${params}`,
        audit: `/api/admin/logs/audit?${params}`,
        user:  `/api/admin/logs/user-activity?${params}`,
    };

    const r = await adminApi('GET', urlMap[currentTab]);
    if (!r.ok) {
        tbody.innerHTML = '<tr class="empty-row"><td colspan="7">조회 실패</td></tr>';
        return;
    }

    const data       = r.json.data ?? {};
    const items      = data.content ?? data ?? [];
    const totalCount = data.totalCount ?? items.length;

    document.getElementById('table-count').textContent = `총 ${totalCount.toLocaleString()}건`;

    if (!items.length) {
        renderHead();
        tbody.innerHTML = '<tr class="empty-row"><td colspan="7">조회된 데이터가 없습니다.</td></tr>';
        renderPagination(0);
        return;
    }

    renderHead();
    renderRows(items);
    renderPagination(totalCount);
}

/* ================================================================
   7. 테이블 헤더
================================================================ */

function renderHead() {
    const head = document.getElementById('table-head');
    const titleMap = {
        admin: '관리자 활동 로그',
        audit: '감사 로그',
        user:  '회원 활동 로그',
    };
    document.getElementById('table-title').textContent = titleMap[currentTab];

    const headers = {
        admin: ['로그 ID', '관리자', '역할', '액션', '결과', '대상', '일시'],
        audit: ['로그 ID', '액션', '결과', 'IP', 'URI', '일시', ''],
        user:  ['로그 ID', '액션', '결과', '대상', '상세', 'IP', '일시'],
    };
    head.innerHTML = `<tr>${headers[currentTab].map(h => `<th>${h}</th>`).join('')}</tr>`;
}

/* ================================================================
   8. 테이블 행
================================================================ */

function renderRows(items) {
    const tbody = document.getElementById('table-body');

    if (currentTab === 'admin') {
        tbody.innerHTML = items.map(d => `
            <tr>
                <td class="cell-id">${d.LOGID      ?? d.logId      ?? '-'}</td>
                <td><b>${d.ADMINNAME  ?? d.adminName  ?? '-'}</b></td>
                <td>${roleBadge(d.ROLECODE ?? d.roleCode)}</td>
                <td>${d.ACTION        ?? d.action     ?? '-'}</td>
                <td>${resultBadge(d.RESULT ?? d.result)}</td>
                <td class="cell-muted">${d.TARGETID   ?? d.targetId   ?? '-'}</td>
                <td class="cell-muted">${d.OCCURREDAT ?? d.occurredAt ?? '-'}</td>
            </tr>`).join('');

    } else if (currentTab === 'audit') {
        tbody.innerHTML = items.map(d => `
            <tr>
                <td class="cell-id">${d.LOGID      ?? d.logId      ?? '-'}</td>
                <td>${d.ACTION       ?? d.action    ?? '-'}</td>
                <td>${d.RESULT       ?? d.result    ?? '-'}</td>
                <td class="cell-muted">${d.CLIENTIP   ?? d.clientIp  ?? '-'}</td>
                <td class="cell-truncate">${d.REQUESTURI ?? d.requestUri ?? '-'}</td>
                <td class="cell-muted">${d.OCCURREDAT ?? d.occurredAt ?? '-'}</td>
                <td></td>
            </tr>`).join('');

    } else {
        tbody.innerHTML = items.map(d => `
            <tr>
                <td class="cell-id">${d.LOGID     ?? d.logId     ?? '-'}</td>
                <td>${d.ACTION      ?? d.action    ?? '-'}</td>
                <td>${resultBadge(d.RESULT ?? d.result)}</td>
                <td class="cell-muted">${d.TARGETID  ?? d.targetId  ?? '-'}</td>
                <td class="cell-truncate">${d.DETAIL  ?? d.detail    ?? '-'}</td>
                <td class="cell-muted">${d.CLIENTIP  ?? d.clientIp  ?? '-'}</td>
                <td class="cell-muted">${d.OCCURREDAT ?? d.occurredAt ?? '-'}</td>
            </tr>`).join('');
    }
}

/* ================================================================
   9. 페이지네이션
================================================================ */

function renderPagination(totalCount) {
    const totalPages = Math.ceil(totalCount / PAGE_SIZE);
    const pg = document.getElementById('pagination');
    if (totalPages <= 1) { pg.innerHTML = ''; return; }

    const start = Math.max(0, currentPage - 2);
    const end   = Math.min(totalPages - 1, start + 4);

    let html = `<button class="page-btn" ${currentPage === 0 ? 'disabled' : ''}
                        onclick="goPage(${currentPage - 1})">이전</button>`;
    for (let i = start; i <= end; i++) {
        html += `<button class="page-btn ${i === currentPage ? 'active' : ''}"
                         onclick="goPage(${i})">${i + 1}</button>`;
    }
    html += `<button class="page-btn" ${currentPage === totalPages - 1 ? 'disabled' : ''}
                     onclick="goPage(${currentPage + 1})">다음</button>`;
    pg.innerHTML = html;
}

function goPage(page) { currentPage = page; loadLogs(); }

/* ================================================================
   10. 배지 헬퍼
================================================================ */

function roleBadge(code) {
    const cls = code === 'SUPER_ADMIN' ? 'badge-super'
              : code === 'MANAGER'     ? 'badge-manager'
              : 'badge-operator';
    return `<span class="badge ${cls}">${code ?? '-'}</span>`;
}

function resultBadge(result) {
    if (!result || result === '-') {
        return `<span class="badge" style="background:#f5f5f5;color:#888;">-</span>`;
    }
    const cls   = result === 'S' ? 'badge-success' : 'badge-fail';
    const label = result === 'S' ? '성공' : '실패';
    return `<span class="badge ${cls}">${label}</span>`;
}

/* ================================================================
   11. 초기화
================================================================ */

document.addEventListener('DOMContentLoaded', () => {
    renderFilter();
    loadLogs();
});
