'use strict';
/**
 * mypage.js  |  BNK 마이페이지
 * 의존: utils.js (BnkAPI, BnkToast, BnkDOM)
 * 로드: utils.js → header.js → mypage.js
 */

/* ================================================================
   §1. 공통 유틸
   ================================================================ */
const Toast = BnkToast;
const btnLoading = (btn, on) => BnkDOM.btnLoading(btn, on, '처리 중...');

class ApiError extends Error {
    constructor(data = {}, status = 0) {
        const msg = data.detail ?? data.fieldErrors?.[0]?.message ?? data.message ?? '오류가 발생했습니다.';
        super(msg);
        this.name = 'ApiError';
        this.status = status;
        this.code = data.code ?? null;
        this.fieldErrors = data.fieldErrors ?? [];
    }
    applyFieldErrors(fn) {
        if (!this.fieldErrors.length) return false;
        this.fieldErrors.forEach(fe => { if (fe.field) fn(fe.field, fe.message); });
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
        phone: v => /^01[0-9]{8,9}$/.test(v.replace(/-/g, '')) ? '' : '올바른 휴대폰 번호를 입력해주세요.',
        match: (a, b, msg = '비밀번호가 일치하지 않습니다.') => a === b ? '' : msg,
        password: v => {
            if (!v || v.length < 8) return '8자 이상 입력해주세요.';
            if (v.length > 50) return '50자 이하로 입력해주세요.';
            if (!/[A-Za-z]/.test(v)) return '영문을 포함해주세요.';
            if (!/\d/.test(v)) return '숫자를 포함해주세요.';
            return '';
        },
    };
})();

function esc(s) {
    return String(s ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function bindPwToggles(root = document) {
    root.querySelectorAll('.pw-toggle').forEach(btn => {
        btn.addEventListener('click', () => {
            const input = document.getElementById(btn.dataset.target);
            if (!input) return;
            const show = input.type === 'password';
            input.type = show ? 'text' : 'password';
            btn.textContent = show ? '숨김' : '표시';
        });
    });
}

function emptyState(msg = '데이터가 없습니다.') {
    return `<p class="empty-state">${msg}</p>`;
}

/* modal open/close — hidden attribute 기반 */
function openModal(id) { const m = document.getElementById(id); if (m) m.removeAttribute('hidden'); }
function closeModal(id) { const m = document.getElementById(id); if (m) m.setAttribute('hidden', ''); }

/* ================================================================
   §2. 페이지 감지 + 초기화
   ================================================================ */
document.addEventListener('DOMContentLoaded', () => {
    bindPwToggles();
    document.querySelector('.sub-nav__back')?.addEventListener('click', () => history.back());

    const page = document.body.dataset.page;
    if (page === 'mypage-main') initMain();
    else if (page === 'mypage-edit') initEdit();
    else if (page === 'mypage-password') initPassword();
    else if (page === 'mypage-spending') initSpending();
    else if (page === 'mypage-credit-score') initCreditScore();
    else if (page === 'mypage-trusted-ips') initTrustedIps();
});

/* ================================================================
   §3. 메인 대시보드
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

    const tabOwned = document.getElementById('tab-owned');
    const tabApplied = document.getElementById('tab-applied');
    const cardSection = document.getElementById('cardSection');

    let _myCards = null;

    const SKELETON =
        '<span class="skeleton skeleton-line"></span>' +
        '<span class="skeleton skeleton-line skeleton-line--short"></span>';

    const APP_STATUS_LABEL = {
        SUBMITTED: '신청완료', REVIEW: '심사중', APPROVED: '승인',
        REJECTED: '반려', ISSUED: '발급완료', CANCELLED: '취소',
    };

	function renderCardItems(items, emptyMsg, statusFn) {
	    if (!items.length) { cardSection.innerHTML = emptyState(emptyMsg); return; }
	    cardSection.innerHTML = items.map(c => `
	        <article class="card-item">
	            <span class="card-item__name">${esc(c.cardName ?? '')}</span>
	            <span class="card-item__status">${esc(statusFn(c))}</span>
	        </article>`).join('');
	}
	
	async function ensureMyCards() {
	    if (_myCards) return _myCards;
	    const data = await API.get('/api/users/me/cards');   // { ownedCards, applications }
	    _myCards = {
	        ownedCards:   data?.ownedCards   ?? [],
	        applications: data?.applications ?? [],
	    };
	    return _myCards;
	}
	
	function setActiveTab(active) {
	    tabOwned.classList.toggle('active', active === 'owned');
	    tabApplied.classList.toggle('active', active === 'applied');
	    tabOwned.setAttribute('aria-selected', String(active === 'owned'));
	    tabApplied.setAttribute('aria-selected', String(active === 'applied'));
	}

	async function loadOwned() {
	    setActiveTab('owned');
	    cardSection.innerHTML = SKELETON;
	    try {
	        const { ownedCards } = await ensureMyCards();
	        renderCardItems(ownedCards, '보유 카드가 없습니다.',
	            c => c.issuedAt ? `${fmtDate(c.issuedAt)} 발급` : '');
	    } catch { cardSection.innerHTML = emptyState('카드 정보를 불러올 수 없습니다.'); }
	}

	async function loadApplied() {
	    setActiveTab('applied');
	    cardSection.innerHTML = SKELETON;
	    try {
	        const { applications } = await ensureMyCards();
	        renderCardItems(applications, '신청 이력이 없습니다.',
	            c => APP_STATUS_LABEL[c.applicationStatus] ?? (c.applicationStatus ?? ''));
	    } catch { cardSection.innerHTML = emptyState('신청 현황을 불러올 수 없습니다.'); }
	}

	tabOwned?.addEventListener('click',   loadOwned);
	tabApplied?.addEventListener('click', loadApplied);
	loadOwned();
}

/* ================================================================
   §4. 내 정보 수정
   ================================================================ */
async function initEdit() {

    /* 탭 전환 */
    const tabs = document.querySelectorAll('.edit-tab');
    const panels = document.querySelectorAll('.tab-panel');

    function switchTab(tab) {
        tabs.forEach(t => { t.classList.remove('active'); t.setAttribute('aria-selected', 'false'); });
        panels.forEach(p => p.classList.remove('active'));
        tab.classList.add('active');
        tab.setAttribute('aria-selected', 'true');
        document.getElementById('panel-' + tab.dataset.tab)?.classList.add('active');
    }
    tabs.forEach(t => t.addEventListener('click', () => switchTab(t)));

    const urlTab = new URLSearchParams(location.search).get('tab');
    if (urlTab) {
        const t = document.querySelector(`.edit-tab[data-tab="${urlTab}"]`);
        if (t) switchTab(t);
    }

    /* 사용자 정보 로드 */
    let _original = {};
    let _phoneChanged = false;

    try {
        const user = await API.get('/api/users/me');
        const get = id => document.getElementById(id);

        if (get('name')) get('name').value = user.name ?? '';
        if (get('currentEmail')) get('currentEmail').textContent = user.email ?? '';
        if (get('currentPhone')) get('currentPhone').textContent = user.maskedPhone ?? user.phone ?? '미등록';
        if (get('currentAddr')) get('currentAddr').textContent = user.address ?? '주소 미등록';
        if (get('job')) get('job').value = user.job ?? '';
        if (get('incomeLevelCode')) get('incomeLevelCode').value = user.incomeLevelCode ?? '';
        if (get('pushEnabled')) get('pushEnabled').checked = user.pushEnabled === 'Y' || user.pushEnabled === true;
        if (get('marketingAgree')) get('marketingAgree').checked = user.marketingAgree === 'Y' || user.marketingAgree === true;

        _original = {
            name: user.name ?? '',
            job: user.job ?? '',
            incomeLevelCode: user.incomeLevelCode ?? '',
            pushEnabled: get('pushEnabled')?.checked ?? false,
            marketingAgree: get('marketingAgree')?.checked ?? false,
        };
    } catch (err) {
        if (err.status > 0 && err.status !== 403 && err.status < 500)
            Toast.error('내 정보를 불러오지 못했습니다.');
    }

    document.getElementById('phone')?.addEventListener('input', () => {
        _phoneChanged = true;
        V.setErr('phone', '');
    });

    /* 비밀번호 확인 모달 */
    const pwInput = document.getElementById('confirmModalPw');
    const pwErrEl = document.getElementById('modalPw-err');

    function openPwModal(onConfirm) {
        if (pwInput) pwInput.value = '';
        if (pwErrEl) { pwErrEl.textContent = ''; pwErrEl.classList.remove('show'); }
        openModal('pwConfirmModal');
        setTimeout(() => pwInput?.focus(), 150);
        document.getElementById('pwConfirmBtn')._onConfirm = onConfirm;
    }

    document.getElementById('pwCancelBtn')?.addEventListener('click', () => closeModal('pwConfirmModal'));

    document.getElementById('pwConfirmBtn')?.addEventListener('click', async function() {
        const pw = pwInput?.value?.trim() ?? '';
        if (!pw) {
            if (pwErrEl) { pwErrEl.textContent = '비밀번호를 입력해주세요.'; pwErrEl.classList.add('show'); }
            return;
        }
        pwErrEl?.classList.remove('show');
        closeModal('pwConfirmModal');
        if (typeof this._onConfirm === 'function') await this._onConfirm(pw);
    });
    pwInput?.addEventListener('keydown', e => { if (e.key === 'Enter') document.getElementById('pwConfirmBtn')?.click(); });

    /* 기본 정보 저장 */
    async function submitBasic(currentPassword) {
        const nameVal = document.getElementById('name')?.value.trim() ?? '';
        const phoneVal = document.getElementById('phone')?.value.trim() ?? '';
        let ok = true;
        if (!V.setErr('name', V.required(nameVal))) ok = false;
        if (phoneVal && !V.setErr('phone', V.phone(phoneVal))) ok = false;
        if (!ok) return;
        const changed = nameVal !== _original.name || _phoneChanged;
        if (!changed) { Toast.warning('변경된 내용이 없습니다.'); return; }

        const body = {};
        if (nameVal !== _original.name) body.name = nameVal;
        if (_phoneChanged && phoneVal) body.phone = phoneVal;
        if (currentPassword) body.currentPassword = currentPassword;

        const btn = document.getElementById('basicSubmitBtn');
        btnLoading(btn, true);
        try {
            await API.put('/api/users/me', body);
            Toast.success('기본 정보가 수정되었습니다.');
            setTimeout(() => { location.href = '/mypage'; }, 1000);
        } catch (err) {
            if (err instanceof ApiError && err.applyFieldErrors(V.setErr)) return;
            if (err.code === 'U003' || err.message?.includes('비밀번호') || err.message?.includes('password')) {
                openPwModal(submitBasic);
            } else if (err.status > 0 && err.status !== 403 && err.status < 500) {
                Toast.error(err.message || '수정 중 오류가 발생했습니다.');
            }
        } finally { btnLoading(btn, false); }
    }

    document.getElementById('basicSubmitBtn')?.addEventListener('click', () => {
        const phoneVal = document.getElementById('phone')?.value.trim() ?? '';
        if (_phoneChanged && phoneVal) openPwModal(pw => submitBasic(pw));
        else submitBasic(null);
    });

    /* 주소 검색 */
    document.getElementById('addrSearchBtn')?.addEventListener('click', searchAddress);

    function searchAddress() {
        function load(cb) {
            if (window.daum?.Postcode) { cb(); return; }
            const s = document.createElement('script');
            s.src = 'https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js';
            s.onload = cb;
            s.onerror = () => Toast.error('주소 검색 서비스를 불러오지 못했습니다.');
            document.head.appendChild(s);
        }
        load(() => {
            document.getElementById('addrSearchEmbed')?.remove();
            const overlay = document.createElement('div');
            overlay.id = 'addrSearchEmbed';
            overlay.className = 'addr-embed-overlay';
            const inner = document.createElement('div');
            inner.className = 'addr-embed-inner';
            const closeBtn = document.createElement('button');
            closeBtn.type = 'button';
            closeBtn.className = 'addr-embed-close';
            closeBtn.textContent = '닫기';
            closeBtn.addEventListener('click', () => overlay.remove());
            overlay.addEventListener('click', e => { if (e.target === overlay) overlay.remove(); });
            inner.appendChild(closeBtn);
            overlay.appendChild(inner);
            document.body.appendChild(overlay);
            new daum.Postcode({
                oncomplete(data) {
                    document.getElementById('postcode').value = data.zonecode;
                    document.getElementById('addrMain').value = data.roadAddress || data.jibunAddress;
                    document.getElementById('addrDetail').value = '';
                    document.getElementById('addrDetail').focus();
                    V.setErr('addr', '');
                    overlay.remove();
                },
                width: '100%', height: '100%',
            }).embed(inner, { autoClose: false });
        });
    }

    /* 주소 저장 */
    document.getElementById('addrSubmitBtn')?.addEventListener('click', async () => {
        const postcode = document.getElementById('postcode')?.value.trim() ?? '';
        const addrMain = document.getElementById('addrMain')?.value.trim() ?? '';
        const addrDetail = document.getElementById('addrDetail')?.value.trim() ?? '';
        if (!addrMain) { V.setErr('addr', '주소를 검색해주세요.'); return; }
        V.setErr('addr', '');
        const btn = document.getElementById('addrSubmitBtn');
        btnLoading(btn, true);
        try {
            await API.put('/api/users/me', { postcode, address: addrMain, addressDetail: addrDetail });
            document.getElementById('ciRefreshBadge')?.classList.add('show');
            Toast.success('주소가 변경되었습니다.');
            setTimeout(() => { location.href = '/mypage'; }, 1200);
        } catch (err) {
            if (err instanceof ApiError && err.applyFieldErrors(V.setErr)) return;
            if (err.status > 0 && err.status !== 403 && err.status < 500)
                Toast.error(err.message || '주소 변경 중 오류가 발생했습니다.');
        } finally { btnLoading(btn, false); }
    });

    /* 소득 정보 저장 */
    document.getElementById('incomeSubmitBtn')?.addEventListener('click', async () => {
        const job = document.getElementById('job')?.value ?? '';
        const income = document.getElementById('incomeLevelCode')?.value ?? '';
        if (job === _original.job && income === _original.incomeLevelCode) {
            Toast.warning('변경된 내용이 없습니다.'); return;
        }
        const btn = document.getElementById('incomeSubmitBtn');
        btnLoading(btn, true);
        try {
            await API.put('/api/users/me', { job, incomeLevelCode: income });
            Toast.success('소득 정보가 수정되었습니다.');
            setTimeout(() => { location.href = '/mypage'; }, 1000);
        } catch (err) {
            if (err.status > 0 && err.status !== 403 && err.status < 500)
                Toast.error(err.message || '수정 중 오류가 발생했습니다.');
        } finally { btnLoading(btn, false); }
    });

    /* 알림 설정 저장 */
    document.getElementById('notifySubmitBtn')?.addEventListener('click', async () => {
        const push = document.getElementById('pushEnabled')?.checked ?? false;
        const mkt = document.getElementById('marketingAgree')?.checked ?? false;
        if (push === _original.pushEnabled && mkt === _original.marketingAgree) {
            Toast.warning('변경된 내용이 없습니다.'); return;
        }
        const btn = document.getElementById('notifySubmitBtn');
        btnLoading(btn, true);
        try {
            await API.put('/api/users/me', { pushEnabled: push, marketingAgree: mkt });
            Toast.success('알림 설정이 저장되었습니다.');
        } catch (err) {
            if (err.status > 0 && err.status !== 403 && err.status < 500)
                Toast.error(err.message || '저장 중 오류가 발생했습니다.');
        } finally { btnLoading(btn, false); }
    });
}

/* ================================================================
   §5. 비밀번호 변경
   ================================================================ */
async function initPassword() {
    const form = document.getElementById('pwForm');
    const submitBtn = document.getElementById('submitBtn');
    const currentPwEl = document.getElementById('currentPw');
    const newPwEl = document.getElementById('newPw');
    const confirmPwEl = document.getElementById('confirmPw');

    /* 강도 + 규칙 */
    newPwEl?.addEventListener('input', () => {
        const val = newPwEl.value;
        const wrap = document.getElementById('strengthWrap');
        const fill = document.getElementById('strengthFill');
        const label = document.getElementById('strengthLabel');
        const rules = document.getElementById('pwRules');
        if (!val) { if (wrap) wrap.hidden = true; return; }
        if (wrap) wrap.hidden = false;
        const c = {
            length: val.length >= 8 && val.length <= 50,
            letter: /[A-Za-z]/.test(val),
            number: /\d/.test(val),
            special: /[^A-Za-z0-9]/.test(val),
        };
        const score = Object.values(c).filter(Boolean).length;
        if (fill) {
            fill.style.width = (score * 25) + '%';
            fill.style.background = score <= 1 ? '#EF4444' : score === 2 ? '#F59E0B' : score === 3 ? '#3B82F6' : '#22C55E';
        }
        if (label) {
            label.textContent = ['', '취약', '보통', '강함', '매우 강함'][score] ?? '';
            label.style.color = fill?.style.background ?? '';
        }
        rules?.querySelectorAll('li[data-rule]').forEach(li => li.classList.toggle('ok', c[li.dataset.rule] === true));
    });

    form?.addEventListener('submit', async e => {
        e.preventDefault();
        let ok = true;
        if (!V.setErr('currentPw', V.required(currentPwEl?.value))) ok = false;
        if (!V.setErr('newPw', V.password(newPwEl?.value ?? ''))) ok = false;
        if (ok && currentPwEl.value === newPwEl.value) {
            V.setErr('newPw', '현재 비밀번호와 동일한 비밀번호는 사용할 수 없습니다.'); ok = false;
        }
        if (ok && !V.setErr('confirmPw', V.match(newPwEl?.value ?? '', confirmPwEl?.value ?? ''))) ok = false;
        if (!ok) return;

        btnLoading(submitBtn, true);
        try {
            await API.patch('/api/users/me/password', {
                currentPassword: currentPwEl.value,
                newPassword: newPwEl.value,
                newPasswordConfirm: confirmPwEl.value,
            });
            Toast.success('비밀번호가 변경되었습니다.');
            setTimeout(() => openModal('doneModal'), 500);
        } catch (err) {
            if (err instanceof ApiError && err.applyFieldErrors(V.setErr)) return;
            if (err.code === 'U009') V.setErr('confirmPw', '새 비밀번호와 확인이 일치하지 않습니다.');
            else if (err.code === 'U003' || err.message?.includes('비밀번호')) V.setErr('currentPw', err.message);
            else if (err.status > 0 && err.status !== 403 && err.status < 500) Toast.error(err.message || '변경 중 오류가 발생했습니다.');
        } finally { btnLoading(submitBtn, false); }
    });

    document.getElementById('doneOk')?.addEventListener('click', () => { location.href = '/mypage'; });
}

/* ================================================================
   §6. 소비 패턴 관리
   ================================================================ */
async function initSpending() {
    const CATS = [
        { key: 'FOOD', label: '식비', color: '#EF4444' },
        { key: 'TRANSPORT', label: '교통', color: '#3B82F6' },
        { key: 'SHOPPING', label: '쇼핑', color: '#8B5CF6' },
        { key: 'CULTURE', label: '문화/여가', color: '#F59E0B' },
        { key: 'HEALTH', label: '의료/건강', color: '#10B981' },
        { key: 'EDUCATION', label: '교육', color: '#06B6D4' },
        { key: 'COMMUNICATION', label: '통신', color: '#6366F1' },
        { key: 'INSURANCE', label: '보험', color: '#EC4899' },
        { key: 'HOUSING', label: '주거/관리비', color: '#84CC16' },
        { key: 'ETC', label: '기타', color: '#94A3B8' },
    ];

    const container = document.getElementById('rowContainer');
    const totalEl = document.getElementById('totalAmount');
    const submitBtn = document.getElementById('submitBtn');
    const backBtn = document.getElementById('backBtn');

    backBtn?.addEventListener('click', () => history.back());

    let _existing = {};
    try {
        const data = await API.get('/api/users/me/spending-patterns');
        const items = Array.isArray(data) ? data : (data?.items ?? []);
        items.forEach(i => { _existing[i.categoryCode ?? i.category] = Number(i.monthlyAmount ?? 0); });
    } catch { /* 빈 값으로 렌더 */ }

    container.innerHTML = CATS.map(cat => `
        <li class="spending-row">
            <span class="spending-dot" data-color="${cat.color}" aria-hidden="true"></span>
            <label class="spending-label" for="sp-${cat.key}">${cat.label}</label>
            <input class="spending-input" type="number" min="0" step="1000"
                   id="sp-${cat.key}" name="${cat.key}"
                   value="${_existing[cat.key] ?? 0}" placeholder="0">
            <span class="spending-unit" aria-hidden="true">원</span>
        </li>`).join('');

    function updateTotal() {
        const total = CATS.reduce((s, c) => s + Number(document.getElementById(`sp-${c.key}`)?.value ?? 0), 0);
        if (totalEl) totalEl.value = total.toLocaleString() + '원';
    }
    container.addEventListener('input', updateTotal);
    updateTotal();

    document.getElementById('spendingForm')?.addEventListener('submit', async e => {
        e.preventDefault();
        btnLoading(submitBtn, true);
        try {
            const items = CATS.map(c => ({
                categoryCode: c.key,
                monthlyAmount: Number(document.getElementById(`sp-${c.key}`)?.value ?? 0),
                source: 'MANUAL',
            }));
            await API.post('/api/users/me/spending-patterns', { items });
            Toast.success('소비 패턴이 저장되었습니다.');
            setTimeout(() => { location.href = '/mypage'; }, 1000);
        } catch (err) {
            Toast.error(err.message || '저장 중 오류가 발생했습니다.');
        } finally { btnLoading(submitBtn, false); }
    });
}

/* ================================================================
   §7. 신용점수 조회
   ================================================================ */
async function initCreditScore() {
    const lockScreen = document.getElementById('lockScreen');
    const revealScreen = document.getElementById('revealScreen');

    document.getElementById('revealBtn')?.addEventListener('click', async () => {
        try {
            const user = await API.get('/api/users/me');
            const score = user.creditScore;
            if (score == null) { Toast.warning('신용점수 정보가 없습니다.'); return; }

            document.getElementById('scoreVal').textContent = score;
            lockScreen?.classList.add('hidden');
            revealScreen?.classList.add('show');

            /* meter 태그 값 업데이트 */
            const meter = document.getElementById('scoreMeter');
            if (meter) { meter.value = score; meter.textContent = score + '점'; }

            let grade = '일반';
            if (score >= 820) grade = '최우수';
            else if (score >= 665) grade = '우수';
            else if (score < 600) grade = '주의';
            document.getElementById('scoreGrade').textContent = grade;

            const pct = Math.round(((score - 300) / 600) * 100);
            document.getElementById('scorePercentile').textContent = Math.max(1, 100 - pct);

            const updatedEl = document.getElementById('scoreUpdated');
            if (updatedEl && user.creditScoreUpdatedAt)
                updatedEl.textContent = '최종 갱신: ' + new Date(user.creditScoreUpdatedAt).toLocaleDateString('ko-KR');
        } catch {
            Toast.error('신용점수를 불러올 수 없습니다.');
        }
    });

    document.getElementById('hideScoreBtn')?.addEventListener('click', () => {
        lockScreen?.classList.remove('hidden');
        revealScreen?.classList.remove('show');
    });
}

/* ================================================================
   §8. 신뢰 기기(IP) 관리
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
        } catch {
            document.getElementById('ipList').innerHTML = emptyState('목록을 불러올 수 없습니다.');
        }
    }

    function renderList() {
        const list = document.getElementById('ipList');
        const badge = document.getElementById('ipCountBadge');
        const meter = document.getElementById('ipCapacityMeter');
        const lbl = document.getElementById('ipCapacityLabel');
        const count = _ipList.length;

        if (badge) badge.textContent = `${count} / 10`;
        if (meter) { meter.value = count; meter.removeAttribute('hidden'); }
        if (lbl) { lbl.textContent = `${count}/10 사용 중`; lbl.removeAttribute('hidden'); }

        if (!count) { list.innerHTML = emptyState('등록된 기기가 없습니다.'); return; }

        list.innerHTML = _ipList.map(item => {
            const { id: tid, ipAddress, nickname, via, isInitial, isDisabled, createdAt, lastUsedAt } = item;
            return `
	            <li class="ip-item">
	                <span class="ip-body">
	                    <span class="ip-nickname-row">
	                        <span class="ip-nickname" id="nn-${tid}">${esc(nickname ?? '이름 없음')}</span>
	                        <input class="ip-nickname-input" id="nn-input-${tid}"
	                               value="${esc(nickname ?? '')}" maxlength="20">
	                        ${isInitial ? '<span class="ip-badge-initial">최초 기기</span>' : ''}
	                        ${isDisabled ? '<span class="ip-badge-disabled">비활성</span>' : ''}
	                    </span>
	                    <span class="ip-address">${esc(maskIp(ipAddress))}</span>
	                    <span class="ip-meta">
	                        <span>${esc(fmtVia(via))}</span>
	                        <span>등록 ${fmtDate(createdAt)}</span>
	                        ${lastUsedAt ? `<span>최근 ${fmtDate(lastUsedAt)}</span>` : ''}
	                    </span>
	                </span>
	                <span class="ip-actions">
	                    ${!isInitial ? `
	                    <button class="btn-ip-edit" data-id="${tid}" type="button">수정</button>
	                    <button class="btn-ip-save" data-id="${tid}" type="button" hidden>저장</button>
	                    <button class="btn-ip-del"  data-id="${tid}" type="button">삭제</button>
	                    ` : ''}
	                </span>
	            </li>`;
        }).join('');

        container.querySelectorAll('.spending-dot[data-color]').forEach(el => {
            el.style.setProperty('background-color', el.dataset.color);
        });

        list.querySelectorAll('.btn-ip-edit').forEach(b => b.addEventListener('click', () => enterEdit(b.dataset.id)));
        list.querySelectorAll('.btn-ip-save').forEach(b => b.addEventListener('click', () => saveNickname(b.dataset.id, b)));
        list.querySelectorAll('.btn-ip-del').forEach(b => b.addEventListener('click', () => {
            _deleteTargetId = Number(b.dataset.id);
            openModal('deleteModal');
        }));
    }

    function maskIp(ip) {
        if (!ip) return '';
        const p = ip.split('.');
        if (p.length === 4) p[3] = '***';
        return p.join('.');
    }
    function fmtVia(v) {
        return { EMAIL: '이메일 인증', CI: 'CI 인증', MANUAL: '직접 등록', AUTO: '자동 등록' }[v] ?? v ?? '';
    }
	
	function fmtDate(v) {
	    return v ? new Date(v).toLocaleDateString('ko-KR') : '';
	}

    function enterEdit(id) {
        const span = document.getElementById(`nn-${id}`);
        const input = document.getElementById(`nn-input-${id}`);
        const editBtn = document.querySelector(`.btn-ip-edit[data-id="${id}"]`);
        const saveBtn = document.querySelector(`.btn-ip-save[data-id="${id}"]`);
        if (!span || !input) return;
        span.classList.add('editing');
        input.classList.add('editing');
        if (editBtn) editBtn.hidden = true;
        if (saveBtn) saveBtn.hidden = false;
        input.focus(); input.select();
    }
    function exitEdit(id) {
        const span = document.getElementById(`nn-${id}`);
        const input = document.getElementById(`nn-input-${id}`);
        const editBtn = document.querySelector(`.btn-ip-edit[data-id="${id}"]`);
        const saveBtn = document.querySelector(`.btn-ip-save[data-id="${id}"]`);
        span?.classList.remove('editing');
        input?.classList.remove('editing');
        if (editBtn) editBtn.hidden = false;
        if (saveBtn) saveBtn.hidden = true;
    }
    async function saveNickname(tid, saveBtn) {
        const span = document.getElementById(`nn-${tid}`);
        const input = document.getElementById(`nn-input-${tid}`);
        const name = input?.value?.trim() ?? '';
        if (!name) { Toast.warning('별명을 입력해주세요.'); input?.focus(); return; }
        btnLoading(saveBtn, true);
        try {
            await API.patch(`/api/users/me/trusted-ips/${tid}`, { nickname: name });
            if (span) span.textContent = name;
            Toast.success('별명이 수정되었습니다.');
            exitEdit(tid);
        } catch (err) {
            Toast.error(err.message || '수정 중 오류가 발생했습니다.');
            if (input && span) input.value = span.textContent;
        } finally { btnLoading(saveBtn, false); }
    }

    function bindDeleteModal() {
        document.getElementById('deleteCancelBtn')?.addEventListener('click', () => {
            closeModal('deleteModal'); _deleteTargetId = null;
        });
        document.getElementById('deleteConfirmBtn')?.addEventListener('click', async () => {
            if (!_deleteTargetId) return;
            const btn = document.getElementById('deleteConfirmBtn');
            btnLoading(btn, true);
            try {
                await API.del(`/api/users/me/trusted-ips/${_deleteTargetId}`);
                Toast.success('기기가 삭제되었습니다.');
                closeModal('deleteModal'); _deleteTargetId = null;
                await loadIpList();
            } catch (err) {
                if (err.code === 'IP004') Toast.error('최초 가입 기기는 삭제할 수 없습니다.');
                else if (err.code === 'IP008') Toast.error('기기를 찾을 수 없습니다.');
                else Toast.error(err.message || '삭제 중 오류가 발생했습니다.');
            } finally { btnLoading(btn, false); }
        });
    }
}
/* ================================================================
   §9. 내 계좌  (data-page="mypage-accounts")
   ================================================================ */
async function initAccounts() {
    const listEl = document.getElementById('accountList');
    const totalBalEl = document.getElementById('totalBalance');
    const accountCntEl = document.getElementById('accountCount');

    const TYPE_LABEL = { CHECKING: '입출금', SAVINGS: '적금', DEPOSIT: '예금' };
    const STATUS_LABEL = { DORMANT: '휴면', CLOSED: '해지' };

    function fmtBalance(amount) {
        if (amount == null) return '-';
        return Number(amount).toLocaleString('ko-KR') + '원';
    }

    function buildItem(acc) {
        const typeLabel = TYPE_LABEL[acc.accountType] ?? acc.accountType;
        const statusLabel = STATUS_LABEL[acc.accountStatus] ?? '';
        const alias = esc(acc.accountAlias ?? acc.accountNumber ?? '-');
        const number = esc(acc.accountNumber ?? '');
        const isZero = Number(acc.balance ?? 0) === 0;

        return `
        <li class="account-item">
          <span class="account-item__type-badge account-item__type-badge--${esc(acc.accountType)}">
            ${esc(typeLabel)}
          </span>
          <span class="account-item__body">
            <span class="account-item__alias">${alias}</span>
            <span class="account-item__number">${number}</span>
          </span>
          <span class="account-item__balance${isZero ? ' account-item__balance--zero' : ''}">
            ${fmtBalance(acc.balance)}
          </span>
          ${statusLabel
                ? `<span class="account-item__status account-item__status--${esc(acc.accountStatus)}">${esc(statusLabel)}</span>`
                : ''}
        </li>`;
    }

    try {
        const raw = await API.get('/api/accounts/me');
        const accounts = Array.isArray(raw) ? raw : (raw?.data ?? []);

        const total = accounts.reduce((sum, a) => sum + Number(a.balance ?? 0), 0);
        totalBalEl.textContent = fmtBalance(total);
        accountCntEl.textContent = `계좌 ${accounts.length}개`;

        listEl.innerHTML = accounts.length
            ? accounts.map(buildItem).join('')
            : `<li>${emptyState('보유 계좌가 없습니다.')}</li>`;

    } catch (err) {
        totalBalEl.textContent = '-';
        listEl.innerHTML = `<li>${emptyState('계좌 정보를 불러올 수 없습니다.')}</li>`;
        if (err?.status > 0 && err.status !== 403 && err.status < 500)
            Toast.error('계좌 정보를 불러오지 못했습니다.');
    }
}