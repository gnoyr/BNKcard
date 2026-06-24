/**
 * admin-admins.js  |  BNK 관리자 계정 관리
 * 관리자 목록 조회 / 등록 / 역할·상태 변경 (SUPER_ADMIN 전용)
 * 의존: utils.js, header.js
 */

'use strict';

/* ================================================================
   1. 상태 변수
================================================================ */

let adminListCache = [];
let sortRoleAsc    = true;

/* 모달 상태 */
let modalTarget = null;
let modalMode   = null;

/* ================================================================
   2. API 헬퍼
================================================================ */

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

/* ================================================================
   3. 탭 전환
================================================================ */

function switchTab(tab, btn) {
    document.querySelectorAll('[id^="tab-"]').forEach(el => {
        el.hidden = el.id !== 'tab-' + tab;
    });
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    if (tab === 'list') loadAdminList();
}

/* ================================================================
   4. 배지 헬퍼
================================================================ */

function roleBadge(code, name) {
    const cls = code === 'SUPER_ADMIN' ? 'badge-super'
              : code === 'MANAGER'     ? 'badge-manager'
              : 'badge-operator';
    return `<span class="badge ${cls}">${name ?? code}</span>`;
}

function statusBadge(code) {
    const cls   = code === 'ACTIVE' ? 'status-active'
                : code === 'LOCKED' ? 'status-locked'
                : 'status-inactive';
    const label = code === 'ACTIVE' ? '정상'
                : code === 'LOCKED' ? '잠금'
                : '비활성';
    return `<span class="${cls}">${label}</span>`;
}

/* ================================================================
   5. 목록 조회
================================================================ */

async function loadAdminList() {
    const tbody = document.getElementById('admin-tbody');
    tbody.innerHTML = '<tr class="empty-row"><td colspan="8">불러오는 중...</td></tr>';

    const r = await adminApi('GET', '/api/admin/admins');
    if (!r.ok) {
        tbody.innerHTML = `<tr class="empty-row"><td colspan="8" style="color:#c62828;">
            조회 실패: ${r.json?.message ?? '오류'}</td></tr>`;
        return;
    }

    const list = r.json.data ?? [];
    adminListCache = list;

    if (!list.length) {
        tbody.innerHTML = '<tr class="empty-row"><td colspan="8">등록된 관리자가 없습니다.</td></tr>';
        return;
    }

    renderAdminRows(list, tbody);
}

function renderAdminRows(list, tbody) {
    tbody.innerHTML = list.map(a => `
        <tr>
            <td>${a.adminId}</td>
            <td><b>${a.username}</b></td>
            <td>${a.name}</td>
            <td>${roleBadge(a.roleCode, a.roleName)}</td>
            <td>${statusBadge(a.statusCode)}</td>
            <td class="cell-muted">${a.lastLoginAt ? a.lastLoginAt.substring(0, 16) : '-'}</td>
            <td class="cell-muted">${a.createdAt  ? a.createdAt.substring(0, 10)   : '-'}</td>
            <td>
                ${a.roleCode !== 'SUPER_ADMIN' ? `
                    <button class="btn-sm btn-role"
                            onclick="openRoleModal(${a.adminId}, '${a.roleCode}')">역할변경</button>
                    <button class="btn-sm btn-status"
                            onclick="openStatusModal(${a.adminId}, '${a.statusCode}')">상태변경</button>
                ` : '<span class="cell-muted">보호됨</span>'}
            </td>
        </tr>`).join('');
}

/* ================================================================
   6. 역할 정렬
================================================================ */

function sortByRole() {
    sortRoleAsc = !sortRoleAsc;
    document.getElementById('sort-role-icon').textContent = sortRoleAsc ? '▲' : '▼';

    const roleOrder = { SUPER_ADMIN: 1, MANAGER: 2, OPERATOR: 3 };
    const sorted = [...adminListCache].sort((a, b) => {
        const ra = roleOrder[a.roleCode] ?? 9;
        const rb = roleOrder[b.roleCode] ?? 9;
        return sortRoleAsc ? ra - rb : rb - ra;
    });

    renderAdminRows(sorted, document.getElementById('admin-tbody'));
}

/* ================================================================
   7. 모달 — 역할 변경 / 상태 변경
================================================================ */

function openRoleModal(adminId, currentRole) {
    modalTarget = adminId;
    modalMode   = 'role';
    document.getElementById('modal-role').value = currentRole;
    document.getElementById('modal-title').textContent = '역할 변경';
    document.getElementById('modal-role-group').hidden   = false;
    document.getElementById('modal-status-group').hidden = true;
    document.getElementById('modal-overlay').hidden = false;
}

function openStatusModal(adminId, currentStatus) {
    modalTarget = adminId;
    modalMode   = 'status';
    document.getElementById('modal-status').value = currentStatus;
    document.getElementById('modal-title').textContent = '상태 변경';
    document.getElementById('modal-role-group').hidden   = true;
    document.getElementById('modal-status-group').hidden = false;
    document.getElementById('modal-overlay').hidden = false;
}

function closeModal() {
    document.getElementById('modal-overlay').hidden = true;
    modalTarget = null;
    modalMode   = null;
}

async function submitModal() {
    if (modalMode === 'role') {
        const roleCode = document.getElementById('modal-role').value;
        const r = await adminApi('PATCH', `/api/admin/admins/${modalTarget}/role`, { roleCode });
        if (!r.ok) { alert('변경 실패: ' + (r.json?.detail ?? '오류')); return; }
        alert('역할이 변경되었습니다.');
    } else if (modalMode === 'status') {
        const statusCode = document.getElementById('modal-status').value;
        const r = await adminApi('PATCH', `/api/admin/admins/${modalTarget}/status`, { statusCode });
        if (!r.ok) { alert('변경 실패: ' + (r.json?.detail ?? '오류')); return; }
        alert('상태가 변경되었습니다.');
    }
    closeModal();
    loadAdminList();
}

/* ================================================================
   8. 관리자 등록 (MANAGER / OPERATOR)
================================================================ */

async function createAdmin() {
    const username = document.getElementById('c-username').value.trim();
    const name     = document.getElementById('c-name').value.trim();
    const password = document.getElementById('c-password').value;
    const roleCode = document.getElementById('c-role').value;

    if (!username || !name || !password || !roleCode) {
        alert('모든 항목을 입력해주세요.');
        return;
    }

    const r = await adminApi('POST', '/api/admin/admins', { username, name, password, roleCode });
    if (!r.ok) {
        alert('등록 실패: ' + (r.json?.detail ?? r.json?.message ?? '오류'));
        return;
    }
    alert('관리자가 등록되었습니다.');
    document.getElementById('c-username').value = '';
    document.getElementById('c-name').value     = '';
    document.getElementById('c-password').value = '';
    document.getElementById('c-role').value     = '';
}

/* ================================================================
   9. SUPER_ADMIN 등록
================================================================ */

async function createSuperAdmin() {
    const username = document.getElementById('s-username').value.trim();
    const name     = document.getElementById('s-name').value.trim();
    const password = document.getElementById('s-password').value;
    const secret   = document.getElementById('s-secret').value;

    if (!username || !name || !password || !secret) {
        alert('모든 항목을 입력해주세요.');
        return;
    }

    const r = await adminApi('POST', '/api/admin/admins/super', { username, name, password, secret });
    if (!r.ok) {
        alert('등록 실패: ' + (r.json?.detail ?? r.json?.message ?? '오류'));
        return;
    }
    alert('SUPER_ADMIN이 등록되었습니다.');
}

/* ================================================================
   10. 초기화
================================================================ */

document.addEventListener('DOMContentLoaded', () => loadAdminList());
