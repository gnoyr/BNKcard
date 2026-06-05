/**
 * utils.js  |  BNK 부산은행 공통 유틸리티
 * ─────────────────────────────────────────────────────────────
 * 로드 순서: utils.js → header.js → (auth|main|card|mypage).js
 *
 * 제공 API:
 *   window.BNK.API      — fetch 래퍼 (타임아웃·5xx 자동 Toast)
 *   window.BNK.Error    — HTTP 상태별 메시지 + 필드 에러 추출
 *   window.BNK.Toast    — 경량 Toast
 *   window.BNK.DOM      — DOM 헬퍼
 *
 * 하위 호환 전역 별칭 (기존 코드 무수정 사용 가능):
 *   BnkAPI, BnkError, BnkToast, BnkDOM
 *
 * ─────────────────────────────────────────────────────────────
 */
'use strict';

(() => {

/* ════════════════════════════════════════════════════════════
   1. HTTP 상태코드 → 사용자 메시지 테이블
════════════════════════════════════════════════════════════ */
const HTTP_MSG = Object.freeze({
    0:   '서버에 연결할 수 없습니다. 네트워크를 확인해 주세요.',
    400: '입력 정보를 다시 확인해 주세요.',
    401: '로그인이 필요합니다.',
    403: '접근 권한이 없습니다.',
    404: '요청한 정보를 찾을 수 없습니다.',
    409: '이미 사용 중인 정보입니다.',
    423: '계정이 잠겨 있습니다. 잠시 후 다시 시도해 주세요.',
    429: '요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.',
    500: '서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.',
    503: '서비스가 일시적으로 중단되었습니다. 잠시 후 다시 시도해 주세요.',
});

/* ════════════════════════════════════════════════════════════
   2. BnkError — 에러 메시지 추출 + 상태코드 핸들러
════════════════════════════════════════════════════════════ */
const _BnkError = {
    extract(data, fallback = '오류가 발생했습니다. 다시 시도해 주세요.') {
        if (!data) return fallback;
        const fields = data.errors ?? data.fieldErrors ?? [];
        if (Array.isArray(fields) && fields.length) {
            return fields
                .map(fe => {
                    const msg = fe.reason ?? fe.message ?? '';
                    return fe.field ? `[${fe.field}] ${msg}` : msg;
                })
                .join('\n');
        }
        return data.detail ?? data.message ?? data.data?.message ?? fallback;
    },

    handle(res, errEl = null, custom = {}) {
        if (res.ok) return false;
        const s = res.status;
        if (s === 0 || s >= 500) return true;
        const extracted = _BnkError.extract(res.data, '');
        const msg = custom[s]
            ?? (s === 400 ? _BnkError.extract(res.data, HTTP_MSG[400]) : null)
            ?? (extracted || HTTP_MSG[s] || '오류가 발생했습니다.');
        _BnkDOM.showError(errEl, msg);
        return true;
    },
};

/* ════════════════════════════════════════════════════════════
   3. BnkToast — 경량 Toast 알림
════════════════════════════════════════════════════════════ */
const _BnkToast = (() => {
    function _container() {
        let el = document.getElementById('toast-container');
        if (!el) {
            el = document.createElement('div');
            el.id = 'toast-container';
            document.body.appendChild(el);
        }
        return el;
    }
    function show(msg, type = 'info', ms = 3200) {
        const c = _container();
        const el = document.createElement('div');
        el.className = `toast toast--${type}`;
        el.textContent = msg;
        c.appendChild(el);
        requestAnimationFrame(() => el.classList.add('toast--show'));
        setTimeout(() => {
            el.classList.remove('toast--show');
            setTimeout(() => el.remove(), 300);
        }, ms);
    }
    return {
        success: (m, ms) => show(m, 'success', ms),
        error:   (m, ms) => show(m, 'error',   ms),
        warning: (m, ms) => show(m, 'warning', ms),
        info:    (m, ms) => show(m, 'info',    ms),
    };
})();

/* ════════════════════════════════════════════════════════════
   4. BnkAPI — fetch 래퍼 (타임아웃 · 5xx 자동 Toast)
════════════════════════════════════════════════════════════ */
const _BnkAPI = (() => {
    const TIMEOUT = 15_000;

    async function request(method, url, body, ms = TIMEOUT) {
        const ctrl  = new AbortController();
        const timer = setTimeout(() => ctrl.abort(), ms);
        const opts  = {
            method,
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            signal: ctrl.signal,
        };
        if (body !== undefined && body !== null) opts.body = JSON.stringify(body);

        try {
            const res  = await fetch(url, opts);
            clearTimeout(timer);
            const data = await res.json().catch(() => ({}));
            if (res.status >= 500) _BnkToast.error(HTTP_MSG[500]);
            return { ok: res.ok, status: res.status, data };
        } catch (err) {
            clearTimeout(timer);
            const msg = err.name === 'AbortError'
                ? '요청 시간이 초과되었습니다. 다시 시도해 주세요.'
                : HTTP_MSG[0];
            _BnkToast.error(msg);
            return { ok: false, status: 0, data: {} };
        }
    }

    return {
        get:   (url, t)    => request('GET',    url, null, t),
        post:  (url, b, t) => request('POST',   url, b,    t),
        put:   (url, b, t) => request('PUT',    url, b,    t),
        patch: (url, b, t) => request('PATCH',  url, b,    t),
        del:   (url, t)    => request('DELETE', url, null, t),
    };
})();

/* ════════════════════════════════════════════════════════════
   5. BnkDOM — DOM 헬퍼
════════════════════════════════════════════════════════════ */
const _BnkDOM = {
    $id: (id) => document.getElementById(id),

    showError(el, msg) {
        if (!el) return;
        el.textContent  = msg ?? '';
        el.style.display = 'block';
        el.classList.add('show');
    },

    hideError(el) {
        if (!el) return;
        el.textContent  = '';
        el.style.display = '';
        el.classList.remove('show');
    },

    btnLoading(btn, loading, loadingText = '처리 중...') {
        if (!btn) return;
        if (loading) {
            btn.dataset.origText = btn.textContent;
            btn.textContent      = loadingText;
            btn.disabled         = true;
        } else {
            btn.textContent = btn.dataset.origText ?? btn.textContent;
            btn.disabled    = false;
        }
    },

    on(id, event, handler) {
        document.getElementById(id)?.addEventListener(event, handler);
    },
};

/* ════════════════════════════════════════════════════════════
   6. window.BNK 단일 네임스페이스 등록
════════════════════════════════════════════════════════════ */
window.BNK = Object.freeze({
    API:   _BnkAPI,
    Error: _BnkError,
    Toast: _BnkToast,
    DOM:   _BnkDOM,
});

/* ════════════════════════════════════════════════════════════
   7. 하위 호환 전역 별칭 — 기존 코드 무수정 동작 보장
      (auth.js / mypage.js / header.js 에서 BnkAPI 등 직접 참조)
════════════════════════════════════════════════════════════ */
window.BnkAPI   = _BnkAPI;
window._BnkError = _BnkError;
window.BnkToast = _BnkToast;
window.BnkDOM   = _BnkDOM;

/* 하위 호환: 외부에서 window.showToast(msg, type) 사용 가능 */
window.showToast = (msg, type = 'info') =>
    (_BnkToast[type] ?? _BnkToast.info)(msg);

/* ════════════════════════════════════════════════════════════
   8. 전역 미처리 Promise 에러 캐치
════════════════════════════════════════════════════════════ */
window.addEventListener('unhandledrejection', (e) => {
    if (e.reason?.name === 'ApiError') return;
    console.warn('[BNK] Unhandled rejection:', e.reason);
});

})();
