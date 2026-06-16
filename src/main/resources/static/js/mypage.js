'use strict';
/**
 * mypage.js  |  BNK 마이페이지 통합 스크립트 (버그 수정본)
 *
 * 의존: utils.js (BnkAPI, BnkToast, BnkDOM)
 * 로드 순서: utils.js → header.js → mypage.js
 *
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

/* ================================================================
   §1. 로컬 별칭 + 마이페이지 전용 유틸
   ================================================================ */

const Toast = BnkToast;
const btnLoading = (btn, on) => BnkDOM.btnLoading(btn, on, '처리 중…');

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
        match: (a, b, msg = '비밀번호가 일치하지 않습니다.') => (a === b) ? '' : msg,
        password: v => {
            if (!v || v.length < 8) return '8자 이상 입력해주세요.';
            if (v.length > 50) return '50자 이하로 입력해주세요.';
            if (!/[A-Za-z]/.test(v)) return '영문을 포함해주세요.';
            if (!/\d/.test(v)) return '숫자를 포함해주세요.';
            return '';
        },
    };
})();

/* ================================================================
   §2. 페이지 감지 + 초기화 진입
   ================================================================ */
document.addEventListener('DOMContentLoaded', () => {
    const page = document.body.dataset.page;
    if (page === 'mypage-main')         initMain();
    else if (page === 'mypage-edit')    initEdit();
    else if (page === 'mypage-password') initPassword();
    else if (page === 'mypage-spending') initSpending();
    else if (page === 'mypage-credit-score') initCreditScore();
    else if (page === 'mypage-trusted-ips')  initTrustedIps();
});

/* ================================================================
   §3. 메인 대시보드 (mypage-main)
   ================================================================ */
async function initMain() {
    try {
        const user = await API.get('/api/users/me');
        const name = user.name ?? '';
        document.getElementById('profileName').textContent = name;
        document.getElementById('profileInitial').textContent = name.charAt(0) || '?';
        const meta = [];
        if (user.email) meta.push(user.maskedEmail ?? user.email);
        if (user.maskedPhone) meta.push(user.maskedPhone);
        document.getElementById('profileMeta').textContent = meta.join(' · ');
    } catch (err) {
        if (err.status > 0 && err.status !== 403 && err.status < 500)
            Toast.error('내 정보를 불러오지 못했습니다.');
    }
}

/* ================================================================
   §4. 내 정보 수정 (mypage-edit)
   ================================================================ */

let _editSubmitHandler = null; // initEdit()에서 등록한 제출 핸들러 참조 보관

function handleBasicSubmit() {
    if (typeof _editSubmitHandler === 'function') {
        _editSubmitHandler();
    }
}

async function initEdit() {
    const form      = document.getElementById('editForm');
    const submitBtn = document.getElementById('basicSubmitBtn') ?? document.getElementById('submitBtn');
    const modal     = document.getElementById('pwConfirmModal');
    const pwInput   = document.getElementById('confirmPwInput');
    const pwErr     = document.getElementById('confirmPwErr');
    const confirmBtn = document.getElementById('modalConfirmBtn');

    let _original     = {};
    let _phoneChanged = false;

    /* ① 사용자 정보 로드 */
    try {
        const user = await API.get('/api/users/me');

        document.getElementById('name').value = user.name ?? '';

        document.getElementById('currentPhone').textContent = user.maskedPhone ?? user.phone ?? '미등록';

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
        if (err.status > 0 && err.status !== 403 && err.status < 500)
            Toast.error('내 정보를 불러오지 못했습니다.');
    }

    /* ② 변경 감지 */
    function hasPersonalInfoChange() {
        return (
            document.getElementById('name').value.trim() !== _original.name ||
            _phoneChanged ||
            document.getElementById('job').value !== _original.job ||
            document.getElementById('incomeLevelCode').value !== _original.incomeLevelCode
        );
    }

    function hasNotificationChange() {
        const pushEl = document.getElementById('pushEnabled');
        const mktEl  = document.getElementById('marketingAgree');
        return (
            (pushEl?.checked ?? false) !== _original.pushEnabled ||
            (mktEl?.checked  ?? false) !== _original.marketingAgree
        );
    }

    function hasChanges() {
        return hasPersonalInfoChange() || hasNotificationChange();
    }

    /* ③ 전송 바디 수집 */
    function collectBody(currentPassword) {
        const pushEl = document.getElementById('pushEnabled');
        const mktEl  = document.getElementById('marketingAgree');
        const body   = {};

        if (currentPassword) body.currentPassword = currentPassword;

        const name      = document.getElementById('name').value.trim();
        const phone     = document.getElementById('phone').value.trim();
        const job       = document.getElementById('job').value;
        const incomeCode = document.getElementById('incomeLevelCode').value;

        if (name !== _original.name) body.name = name;
        if (_phoneChanged && phone)   body.phone = phone;
        if (job !== _original.job)    body.job = job;
        if (incomeCode !== _original.incomeLevelCode) body.incomeLevelCode = incomeCode;

        body.pushEnabled    = pushEl?.checked ?? false;
        body.marketingAgree = mktEl?.checked  ?? false;
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

    /* ⑤ 핵심 제출 로직 (폼 이벤트 & 버튼 onclick 양쪽에서 호출)
     */
    async function submitLogic() {
        if (!hasChanges()) { Toast.warning('변경된 내용이 없습니다.'); return; }

        const phoneVal = document.getElementById('phone').value.trim();
        if (phoneVal) {
            const phoneErrMsg = V.phone(phoneVal);
            if (phoneErrMsg) { V.setErr('phone', phoneErrMsg); return; }
        }
        V.setErr('phone', '');

        if (hasPersonalInfoChange()) {
            if (pwInput) pwInput.value = '';
            if (pwErr) { pwErr.textContent = ''; pwErr.classList.remove('show'); }
            modal?.classList.add('open');
            setTimeout(() => pwInput?.focus(), 150);
        } else {
            await doUpdate(null);
        }
    }

    _editSubmitHandler = submitLogic;

    // form submit 이벤트도 유지 (form 내 Enter 키 제출 대비)
    form?.addEventListener('submit', async e => { e.preventDefault(); await submitLogic(); });

    document.getElementById('phone')?.addEventListener('input', () => {
        _phoneChanged = true;
        V.setErr('phone', '');
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
}

/* ================================================================
   §5. 비밀번호 변경 (mypage-password)
   ================================================================ */
async function initPassword() {
    const form      = document.getElementById('pwForm');
    const submitBtn = document.getElementById('submitBtn');
    const newPw     = document.getElementById('newPw');
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
                currentPassword:    currentPw.value,
                newPassword:        newPw.value,
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
    // 기존 코드 유지 (버그 없음)
    try {
        const spending = await API.get('/api/users/me/spending-patterns');
        const items = Array.isArray(spending) ? spending : (spending?.items ?? spending ?? []);
        const total = items.reduce((s, i) => s + Number(i.monthlyAmount ?? 0), 0);
        const totalEl = document.getElementById('chart-amount');
        if (totalEl) totalEl.textContent = total.toLocaleString() + '원';
        // renderDonut 등 기존 로직 유지
    } catch (err) {
        if (err.status > 0 && err.status !== 403 && err.status < 500)
            Toast.warning('소비 패턴 데이터를 불러오지 못했습니다.');
    }
}

/* ================================================================
   §7. 신용점수 조회 (mypage-credit-score)
   ================================================================ */
async function initCreditScore() {
    // 기존 코드 유지 (버그 없음)
    document.getElementById('revealBtn')?.addEventListener('click', async () => {
        try {
            const user = await API.get('/api/users/me');
            const score = user.creditScore;
            if (score == null) { Toast.warning('신용점수 정보가 없습니다.'); return; }

            document.getElementById('scoreVal').textContent = score;
            document.getElementById('lockScreen')?.classList.add('hidden');
            document.getElementById('revealScreen')?.classList.add('show');

            const pct = Math.max(0, Math.min(100, ((score - 300) / 600) * 100));
            document.getElementById('gaugeFill').style.width = pct + '%';

            let grade = '일반';
            if (score >= 820) grade = '최우수';
            else if (score >= 665) grade = '우수';
            else if (score < 600) grade = '주의';
            document.getElementById('scoreGrade').textContent = grade;

            const percentile = Math.round(((score - 300) / 600) * 100);
            document.getElementById('scorePercentile').textContent = Math.max(1, 100 - percentile);
        } catch (err) {
            Toast.error('신용점수를 불러올 수 없습니다.');
        }
    });
}

/* ================================================================
   §8. 신뢰 기기(IP) 관리 (mypage-trusted-ips)
   ================================================================ */
async function initTrustedIps() {
    let _ipList          = [];
    let _deleteTargetId  = null;

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
        const list         = document.getElementById('ipList');
        const countBadge   = document.getElementById('ipCountBadge');
        const capacityWrap = document.getElementById('ipCapacityWrap');
        const capacityBar  = document.getElementById('ipCapacityBar');
        const capacityLabel = document.getElementById('ipCapacityLabel');

        const count = _ipList.length;
        if (countBadge) countBadge.textContent = count + ' / 10';
        if (capacityWrap) capacityWrap.style.display = 'flex';
        const pct = (count / 10) * 100;
        if (capacityBar) {
            capacityBar.style.width = pct + '%';
            capacityBar.className = 'ip-capacity__bar' + (pct >= 100 ? ' full' : pct >= 70 ? ' warn' : '');
        }
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
        return {
            SIGNUP:       '<span class="via-signup">자동 등록</span>',
            EMAIL_VERIFY: '<span class="via-email">이메일 인증</span>',
            CI_VERIFY:    '<span class="via-ci">CI 인증</span>',
            ADMIN:        '<span class="via-admin">관리자 등록</span>',
        }[via] ?? via;
    }

    function fmtTs(ts) {
        if (!ts) return '—';
        try { return new Date(ts).toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' }); }
        catch { return ts; }
    }

    function esc(s) {
        return String(s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;')
            .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    function buildIpItem(ip) {
        const isInitial  = ip.isInitial === 'Y' || ip.is_initial === 'Y';
        const isDisabled = (ip.statusCode || ip.status_code) === 'DISABLED';
        const trustId    = ip.trustId ?? ip.trust_id;
        const nickname   = ip.nickname ?? '내 기기';
        const ipAddr     = ip.ipAddress ?? ip.ip_address ?? '';
        const via        = ip.registeredVia ?? ip.registered_via ?? '';
        const createdAt  = ip.createdAt ?? ip.created_at;
        const lastUsedAt = ip.lastUsedAt ?? ip.last_used_at;

        return `
        <li class="ip-item${isDisabled ? ' ip-item--disabled' : ''}" data-trust-id="${trustId}">
          <span class="ip-icon${isInitial ? ' ip-icon--initial' : ''}">💻</span>
          <div class="ip-body">
            <div class="ip-row">
              <span class="ip-nickname" id="nn-${trustId}">${esc(nickname)}</span>
              ${isInitial ? '<span class="ip-initial-badge">최초 기기</span>' : ''}
              <input class="ip-nickname-input" id="nn-input-${trustId}" value="${esc(nickname)}" maxlength="20" style="display:none;" />
            </div>
            <div class="ip-addr">${esc(maskIp(ipAddr))}</div>
            <div class="ip-meta">${formatVia(via)} · 등록일 ${fmtTs(createdAt)}${lastUsedAt ? ' · 최근 사용 ' + fmtTs(lastUsedAt) : ''}</div>
          </div>
          <div class="ip-actions">
            ${!isInitial ? `
            <button class="btn-ip-edit" data-id="${trustId}" title="별명 수정">✏️</button>
            <button class="btn-ip-save" data-id="${trustId}" style="display:none;">저장</button>
            <button class="btn-ip-del"  data-id="${trustId}" title="삭제">🗑</button>
            ` : ''}
          </div>
        </li>`;
    }

    function attachItemEvents() {
        document.querySelectorAll('.btn-ip-edit').forEach(btn => {
            btn.addEventListener('click', () => enterEditMode(btn.dataset.id));
        });
        document.querySelectorAll('.btn-ip-save').forEach(btn => {
            btn.addEventListener('click', () => saveNickname(btn.dataset.id, btn));
        });
        document.querySelectorAll('.btn-ip-del').forEach(btn => {
            btn.addEventListener('click', () => {
                _deleteTargetId = Number(btn.dataset.id);
                document.getElementById('deleteModal')?.classList.add('open');
            });
        });
    }

    function enterEditMode(id) {
        const span  = document.getElementById(`nn-${id}`);
        const input = document.getElementById(`nn-input-${id}`);
        const editBtn = document.querySelector(`.btn-ip-edit[data-id="${id}"]`);
        const saveBtn = document.querySelector(`.btn-ip-save[data-id="${id}"]`);
        if (!span || !input) return;
        span.style.display  = 'none';
        input.style.display = '';
        editBtn && (editBtn.style.display = 'none');
        saveBtn && (saveBtn.style.display = '');
        input.focus();
        input.select();
    }

    function exitEditMode(span, input, editBtn, saveBtn) {
        span.style.display  = '';
        input.style.display = 'none';
        editBtn && (editBtn.style.display = '');
        saveBtn && (saveBtn.style.display = 'none');
    }

    async function saveNickname(trustId, saveBtn) {
        const span  = document.getElementById(`nn-${trustId}`);
        const input = document.getElementById(`nn-input-${trustId}`);
        const editBtn = document.querySelector(`.btn-ip-edit[data-id="${trustId}"]`);
        const newNickname = input?.value?.trim() ?? '';

        if (!newNickname) { Toast.warning('별명을 입력해주세요.'); input.focus(); return; }

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

                if (err.code === 'IP004') {
                    Toast.error('최초 가입 기기는 삭제할 수 없습니다.');
                } else if (err.code === 'IP008') {
                    Toast.error('기기를 찾을 수 없습니다.');
                } else {
                    Toast.error(err.message || '삭제 중 오류가 발생했습니다.');
                }
            } finally {
                btnLoading(btn, false);
            }
        });
    }
}