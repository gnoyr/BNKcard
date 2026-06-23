/**
 * admin-approval.js  |  BNK 결재 관리
 * 결재 목록 조회 / 상세 조회 / 승인 / 반려
 * 의존: utils.js (showToast), header.js
 */

'use strict';

/* ================================================================
   1. 상수 및 상태 변수
================================================================ */

function getApprovalId() {
    const match = location.pathname.match(/^\/admin\/approvals\/(\d+)$/);
    if (match) return match[1];
    return new URLSearchParams(location.search).get('approvalId');
}

let approvalId  = getApprovalId();
let currentPage = 0;
const PAGE_SIZE = 20;

const TYPE_LABEL = {
    CARD_PUBLISH:  '카드 등록',
    CARD_UPDATE:   '카드 수정',
    TERMS_PUBLISH: '약관 등록',
};

const TYPE_CLASS = {
    CARD_PUBLISH:  'type-card-publish',
    CARD_UPDATE:   'type-card-update',
    TERMS_PUBLISH: 'type-terms-publish',
};

const STATUS_CLASS = {
    PENDING:  'badge-pending',
    APPROVED: 'badge-approved',
    REJECTED: 'badge-rejected',
};

const LINE_CLASS = {
    PENDING:  'line-node-pending',
    APPROVED: 'line-node-approved',
    REJECTED: 'line-node-rejected',
};

/* ================================================================
   2. 공통 유틸
================================================================ */

function notify(msg, type = 'info') {
    if (typeof window.showToast === 'function') {
        window.showToast(msg, type);
    } else {
        console[type === 'error' ? 'error' : 'log'](msg);
    }
}

/** GlobalExceptionHandler 응답에서 사용자 메시지 추출 */
function extractApiMsg(data, fallback = '처리 중 오류가 발생했습니다.') {
    if (data?.fieldErrors?.length) {
        const fe = data.fieldErrors[0];
        return fe.field ? `${fe.field}: ${fe.message}` : fe.message;
    }
    return data?.detail ?? data?.message ?? data?.data?.message ?? fallback;
}

/* ================================================================
   3. API 공통 함수
================================================================ */

async function api(method, url, body) {
    const opts = {
        method,
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
    };
    if (body) opts.body = JSON.stringify(body);

    const res = await fetch(url, opts);

    if (res.status === 401) {
        location.href = '/admin/login';
        return { ok: false, status: 401, data: null };
    }

    const data = await res.json().catch(() => null);

    if (res.status === 403) {
        notify(data?.message ?? data?.detail ?? '접근 권한이 없습니다.', 'error');
        return { ok: false, status: 403, data };
    }

    if (res.status >= 500) {
        notify('서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.', 'error');
        return { ok: false, status: res.status, data };
    }

    return { ok: res.ok, status: res.status, data };
}

/* ================================================================
   4. 목록 조회
================================================================ */

function searchApprovals() { loadApprovals(0); }

function resetFilters() {
    document.getElementById('statusFilter').value = '';
    document.getElementById('typeFilter').value   = '';
    loadApprovals(0);
}

async function loadApprovals(page = 0) {
    currentPage = page;

    const status = document.getElementById('statusFilter').value;
    const type   = document.getElementById('typeFilter').value;

    let url = `/api/admin/approvals?page=${page}&size=${PAGE_SIZE}`;
    if (status) url += '&statusCode='        + encodeURIComponent(status);
    if (type)   url += '&requestTypeCode='   + encodeURIComponent(type);

    const tbody = document.getElementById('approvalTbody');
    tbody.innerHTML = '<tr><td colspan="6" class="empty-cell">로딩 중...</td></tr>';

    try {
        const r = await api('GET', url);

        if (!r.ok || !r.data?.data) {
            const msg = r.data
                ? extractApiMsg(r.data, '목록 조회에 실패했습니다.')
                : '목록 조회에 실패했습니다.';
            tbody.innerHTML = `<tr><td colspan="6" class="empty-cell">${msg}</td></tr>`;
            if (r.status && r.status !== 401 && r.status !== 403 && r.status < 500) {
                notify(msg, 'error');
            }
            return;
        }

        const { content, totalCount } = r.data.data;
        document.getElementById('totalCount').textContent = totalCount ?? 0;

        if (!content?.length) {
            tbody.innerHTML = '<tr><td colspan="6" class="empty-cell">결재 건이 없습니다.</td></tr>';
            renderPagination(0);
            return;
        }

        tbody.innerHTML = content.map(a => {
            const typeLabel = TYPE_LABEL[a.requestTypeCode] ?? a.requestTypeCode ?? '-';
            const typeCls   = TYPE_CLASS[a.requestTypeCode] ?? '';
            const statusCls = STATUS_CLASS[a.statusCode] ?? '';
            const requestedAt = a.requestedAt
                ? a.requestedAt.substring(0, 16).replace('T', ' ')
                : '-';

            return `
                <tr class="clickable-row"
                    onclick="location.href='/admin/approvals/${a.approvalId}'"
                    style="cursor:pointer;">
                    <td class="cell-id">${a.approvalId}</td>
                    <td><span class="type-badge badge ${typeCls}">${typeLabel}</span></td>
                    <td class="cell-name">${a.targetName ?? '-'}</td>
                    <td>${a.requesterName ?? '-'}</td>
                    <td><span class="badge ${statusCls}">${a.statusCode ?? '-'}</span></td>
                    <td class="cell-date">${requestedAt}</td>
                </tr>`;
        }).join('');

        renderPagination(totalCount);

    } catch (e) {
        tbody.innerHTML = '<tr><td colspan="6" class="empty-cell">네트워크 오류</td></tr>';
        notify('네트워크 오류가 발생했습니다.', 'error');
    }
}

/* ================================================================
   5. 상세 조회
================================================================ */

async function loadDetail(id) {
    const aid = id ?? approvalId;
    if (!aid) return;
    approvalId = aid;

    document.getElementById('listPage').style.display   = 'none';
    document.getElementById('detailPage').style.display = 'block';

    const infoTable      = document.getElementById('infoTable');
    const lineTbody      = document.getElementById('linesTable');
    const actionArea     = document.getElementById('actionArea');
    const snapshotSection = document.getElementById('snapshotSection');

    if (infoTable)  infoTable.innerHTML  = '<tr><td colspan="2">불러오는 중...</td></tr>';
    if (lineTbody)  lineTbody.innerHTML  = '<tr><td colspan="5">불러오는 중...</td></tr>';
    if (actionArea) actionArea.style.display = 'none';

    const r = await api('GET', `/api/admin/approvals/${aid}`);

    if (!r.ok || !r.data?.data) {
        const msg = r.data
            ? extractApiMsg(r.data, '상세 조회에 실패했습니다.')
            : '상세 조회에 실패했습니다.';
        if (infoTable) infoTable.innerHTML = `<tr><td colspan="2">${msg}</td></tr>`;
        if (r.status && r.status !== 401 && r.status !== 403 && r.status < 500) {
            notify(msg, 'error');
        }
        return;
    }

    const a = r.data.data;

    /* 기본 정보 */
    if (infoTable) {
        infoTable.innerHTML = `
            <tr><th>결재 ID</th>   <td>${a.approvalId}</td></tr>
            <tr><th>유형</th>      <td>${TYPE_LABEL[a.requestTypeCode] ?? a.requestTypeCode ?? '-'}</td></tr>
            <tr><th>대상</th>      <td>${a.targetName ?? '-'}</td></tr>
            <tr><th>요청자</th>    <td>${a.requesterName ?? '-'}</td></tr>
            <tr><th>요청 의견</th> <td>${a.requestComment ?? '-'}</td></tr>
            <tr><th>상태</th>
                <td><span class="badge ${STATUS_CLASS[a.statusCode] ?? ''}">${a.statusCode ?? '-'}</span></td>
            </tr>
            <tr><th>요청일시</th>
                <td>${a.requestedAt ? a.requestedAt.substring(0, 16).replace('T', ' ') : '-'}</td>
            </tr>
            <tr><th>완료일시</th>
                <td>${a.completedAt ? a.completedAt.substring(0, 16).replace('T', ' ') : '-'}</td>
            </tr>`;
    }

    /* 결재 라인 */
    if (lineTbody) {
        let canProcess = false;
        lineTbody.innerHTML = (a.lines ?? []).map((line, idx) => {
            const cls = LINE_CLASS[line.statusCode] ?? '';
            if (line.isMine && line.statusCode === 'PENDING') canProcess = true;
            return `
                <tr>
                    <td>${idx + 1}</td>
                    <td>${line.approverName ?? '-'}</td>
                    <td><span class="badge ${cls}">${line.statusCode ?? '-'}</span></td>
                    <td>${line.comment ?? '-'}</td>
                    <td class="cell-date">
                        ${line.processedAt ? line.processedAt.substring(0, 16).replace('T', ' ') : '-'}
                    </td>
                </tr>`;
        }).join('') || '<tr><td colspan="5" class="empty-cell">결재 라인 없음</td></tr>';

        /* 처리 버튼 노출 조건: 내 차례이고 결재 전체 상태가 PENDING */
        if (actionArea && canProcess && a.statusCode === 'PENDING') {
            actionArea.style.display = 'block';
            document.getElementById('commentInput').value = '';
        }
    }

    /* 카드 스냅샷 */
    if (snapshotSection) {
        if (a.snapshot) {
            snapshotSection.style.display = 'block';

            const snapshotTable = document.getElementById('snapshotTable');
            const s = a.snapshot;
            if (snapshotTable) {
                snapshotTable.innerHTML = `
                    <tr><th>카드 ID</th>   <td>${s.cardId ?? '-'}</td></tr>
                    <tr><th>카드명</th>    <td>${s.cardName ?? '-'}</td></tr>
                    <tr><th>카드사</th>    <td>${s.companyName ?? '-'}</td></tr>
                    <tr><th>카드 유형</th> <td>${s.cardType ?? '-'}</td></tr>
                    <tr><th>국내 연회비</th><td>${s.annualFeeDomestic != null ? s.annualFeeDomestic.toLocaleString() + '원' : '-'}</td></tr>
                    <tr><th>해외 연회비</th><td>${s.annualFeeOverseas != null ? s.annualFeeOverseas.toLocaleString() + '원' : '-'}</td></tr>`;
            }

            /* 혜택 목록 */
            const benefitTbody = document.getElementById('benefitTbody');
            if (benefitTbody) {
                benefitTbody.innerHTML = s.benefits?.length
                    ? s.benefits.map(b => `
                        <tr>
                            <td>${b.benefitId ?? '-'}</td>
                            <td>${b.categoryName ?? '-'}</td>
                            <td>${b.benefitTitle ?? '-'}</td>
                            <td>${b.benefitType ?? '-'}</td>
                            <td>${b.displayText ?? '-'}</td>
                            <td>${b.discountRate ?? '-'}</td>
                            <td>${b.discountAmount != null ? b.discountAmount.toLocaleString() : '-'}</td>
                            <td>${b.pointRate ?? '-'}</td>
                            <td>${b.cashbackRate ?? '-'}</td>
                            <td>${b.monthlyLimitAmount != null ? b.monthlyLimitAmount.toLocaleString() : '-'}</td>
                            <td>${b.dailyLimitAmount != null ? b.dailyLimitAmount.toLocaleString() : '-'}</td>
                            <td>${b.minPaymentAmount != null ? b.minPaymentAmount.toLocaleString() : '-'}</td>
                            <td>${b.conditionText ?? '-'}</td>
                            <td>${b.sortOrder ?? '-'}</td>
                            <td>${b.visibleYn ?? '-'}</td>
                            <td class="cell-date">${b.createdAt ? b.createdAt.substring(0, 16) : '-'}</td>
                            <td class="cell-date">${b.updatedAt ? b.updatedAt.substring(0, 16) : '-'}</td>
                        </tr>`).join('')
                    : '<tr><td colspan="17" class="empty-cell">혜택 없음</td></tr>';
            }

            /* 이미지 목록 */
            const imageTbody = document.getElementById('imageTbody');
            if (imageTbody) {
                imageTbody.innerHTML = s.images?.length
                    ? s.images.map(i => `
                        <tr>
                            <td>${i.imageId ?? '-'}</td>
                            <td>${i.imageType ?? '-'}</td>
                            <td>${i.imageUrl
                                ? `<img src="${i.imageUrl}" alt="${i.imageType ?? ''}">`
                                : '-'}</td>
                            <td>${i.originalName ?? '-'}</td>
                            <td>${i.storedName ?? '-'}</td>
                            <td>${i.mimeType ?? '-'}</td>
                            <td>${i.imageWidth ?? '-'}</td>
                            <td>${i.imageHeight ?? '-'}</td>
                            <td>${i.sortOrder ?? '-'}</td>
                            <td class="cell-date">${i.createdAt ? i.createdAt.substring(0, 16) : '-'}</td>
                            <td>${i.imageUrl
                                ? `<a href="${i.imageUrl}" target="_blank" class="link-url">URL 열기</a>`
                                : '-'}</td>
                        </tr>`).join('')
                    : '<tr><td colspan="11" class="empty-cell">이미지 없음</td></tr>';
            }
        } else {
            snapshotSection.style.display = 'none';
        }
    }
}

/* ================================================================
   6. 승인 / 반려 처리
================================================================ */

async function doApprove() {
    if (!confirm('승인 처리하시겠습니까?')) return;
    const comment = document.getElementById('commentInput').value.trim();
    const r = await api('POST', `/api/admin/approvals/${approvalId}/approve`, { comment });
    if (r.ok) {
        alert('승인 처리 완료');
        location.href = '/admin/approvalManage.html';
    } else {
        alert(extractApiMsg(r.data, '승인 처리 실패'));
    }
}

async function doReject() {
    const comment = document.getElementById('commentInput').value.trim();
    if (!comment) { alert('반려 시 의견은 필수입니다.'); return; }
    if (!confirm('반려 처리하시겠습니까?')) return;
    const r = await api('POST', `/api/admin/approvals/${approvalId}/reject`, { comment });
    if (r.ok) {
        alert('반려 처리 완료');
        location.href = '/admin/approvalManage.html';
    } else {
        alert(extractApiMsg(r.data, '반려 처리 실패'));
    }
}

/* ================================================================
   7. 페이지네이션
================================================================ */

function renderPagination(totalCount) {
    const totalPages = Math.ceil(totalCount / PAGE_SIZE);
    const el = document.getElementById('pagination');
    if (totalPages <= 1) { el.innerHTML = ''; return; }

    const startPage = Math.max(0, currentPage - 2);
    const endPage   = Math.min(totalPages - 1, startPage + 4);

    let html = `<button class="page-btn" onclick="loadApprovals(${currentPage - 1})"
                        ${currentPage === 0 ? 'disabled' : ''}>이전</button>`;
    for (let i = startPage; i <= endPage; i++) {
        html += `<button class="page-btn ${i === currentPage ? 'active' : ''}"
                         onclick="loadApprovals(${i})">${i + 1}</button>`;
    }
    html += `<button class="page-btn" onclick="loadApprovals(${currentPage + 1})"
                     ${currentPage === totalPages - 1 ? 'disabled' : ''}>다음</button>`;
    el.innerHTML = html;
}

/* ================================================================
   8. 초기화
================================================================ */

document.addEventListener('DOMContentLoaded', () => {
    if (approvalId) {
        loadDetail(approvalId);
    } else {
        document.getElementById('listPage').style.display   = 'block';
        document.getElementById('detailPage').style.display = 'none';
        loadApprovals();
    }
});
