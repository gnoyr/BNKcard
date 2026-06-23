'use strict';
/* ================================================================
   notification.js  |  BNK 알림 시스템 통합
   §1. 헤더 알림 벨 + 드롭다운  — 로그인 사용자에게만 표시
   §2. 마이페이지 최근 알림 섹션 — #mp-notif-section 있을 때만
   §3. 관리자 알림 발송 관리    — #batchTbody 있을 때만
================================================================ */


/* ================================================================
   §1. 헤더 알림 벨 드롭다운
   header.js 의 renderNav(loggedIn) 가 완료된 뒤 실행되어야 하므로
   DOMContentLoaded 가 아닌 window load + MutationObserver 로 감지
================================================================ */
(function HeaderNotif() {
  const POLL_MS = 60_000;

  const CAT_ICON = {
    TERMS_CHANGED: '&#128203;',  // 📋
    CARD_UPDATED:  '&#128179;',  // 💳
    EVENT:         '&#127873;',  // 🎁
    NOTICE:        '&#128226;',  // 📢
    SYSTEM:        '&#9881;',    // ⚙
  };

  /* ── 벨 버튼 주입 (로그인 사용자 nav 에만) ── */
  function injectBtn() {
    const nav = document.getElementById('headerNav');
    if (!nav || nav.querySelector('#notif-btn')) return;

    // 로그인 상태 판별: 비로그인 시 nav 에는 로그인/회원가입 링크만 있음
    // header.js 가 renderNav(loggedIn=true) 시 username span 을 주입함
    const isLoggedIn = !!nav.querySelector('.header-nav__username');
    if (!isLoggedIn) return;

    const btn = document.createElement('button');
    btn.id        = 'notif-btn';
    btn.className = 'notif-btn';
    btn.type      = 'button';
    btn.setAttribute('aria-label', '알림');
    // SVG 없이 유니코드 벨 문자 사용
    btn.innerHTML = `
      <span class="notif-bell" aria-hidden="true">&#128276;</span>
      <span class="notif-hdr-badge" id="notif-hdr-badge" aria-live="polite" style="display:none">0</span>`;
    btn.addEventListener('click', toggleDd);

    // username 바로 뒤에 삽입
    const username = nav.querySelector('.header-nav__username');
    username.insertAdjacentElement('afterend', btn);
  }

  /* ── 드롭다운 ── */
  function injectDd() {
    if (document.getElementById('notif-dropdown')) return;
    const el = document.createElement('div');
    el.id        = 'notif-dropdown';
    el.className = 'notif-dropdown';
    el.style.display = 'none';
    el.setAttribute('role', 'dialog');
    el.setAttribute('aria-label', '알림 목록');
    el.innerHTML = `
      <div class="notif-dd-head">
        <span class="notif-dd-head__title">알림</span>
        <button type="button" class="notif-dd-head__all" onclick="window.__notif.markAll()">모두 읽음</button>
      </div>
      <ul class="notif-dd-list" id="notif-dd-list" role="list">
        <li class="notif-dd-empty">불러오는 중...</li>
      </ul>
      <div class="notif-dd-foot">
        <a href="/mypage/notifications" class="notif-dd-foot__link">전체 알림 보기</a>
      </div>`;
    document.body.appendChild(el);

    document.addEventListener('click', e => {
      const dd  = document.getElementById('notif-dropdown');
      const btn = document.getElementById('notif-btn');
      if (dd && !dd.contains(e.target) && btn && !btn.contains(e.target))
        dd.style.display = 'none';
    });
  }

  /* ── 토글 ── */
  function toggleDd() {
    const dd = document.getElementById('notif-dropdown');
    if (!dd) return;
    const opening = dd.style.display === 'none';
    dd.style.display = opening ? 'flex' : 'none';
    if (opening) { positionDd(); loadDdList(); }
  }

  function positionDd() {
    const dd  = document.getElementById('notif-dropdown');
    const btn = document.getElementById('notif-btn');
    if (!dd || !btn) return;
    const r = btn.getBoundingClientRect();
    dd.style.top   = (r.bottom + 6) + 'px';
    dd.style.right = Math.max(8, window.innerWidth - r.right) + 'px';
  }

  /* ── 목록 로드 ── */
  async function loadDdList() {
    const ul = document.getElementById('notif-dd-list');
    if (!ul) return;
    ul.innerHTML = '<li class="notif-dd-empty">불러오는 중...</li>';
    try {
      const res  = await fetch('/api/notifications', { credentials: 'include' });
      if (!res.ok) { ul.innerHTML = '<li class="notif-dd-empty">불러오기 실패</li>'; return; }
      const body = await res.json();
      const { unreadCount = 0, notifications: items = [] } = body?.data ?? {};
      setBadge(unreadCount);
      if (!items.length) { ul.innerHTML = '<li class="notif-dd-empty">알림이 없습니다.</li>'; return; }
      ul.innerHTML = items.slice(0, 8).map(n => {
        const unread = n.readYn === 'N';
        const catCls = 'notif-cat--' + (n.notificationCategory ?? '').toLowerCase();
        const icon   = CAT_ICON[n.notificationCategory] ?? '&#128276;';
        const msg    = esc((n.message ?? '').slice(0, 40) + ((n.message ?? '').length > 40 ? '…' : ''));
        return `
          <li class="notif-dd-item${unread ? ' notif-dd-item--unread' : ''}"
              role="listitem"
              onclick="window.__notif.read(${n.notificationId},'${esc(n.linkUrl ?? '')}')">
            <span class="notif-dd-dot ${catCls}" aria-hidden="true"></span>
            <div class="notif-dd-body">
              <p class="notif-dd-title">${esc(n.title)}</p>
              <p class="notif-dd-msg">${msg}</p>
              <p class="notif-dd-time">${fmtAgo(n.createdAt)}</p>
            </div>
            ${unread ? '<span class="notif-dd-unread-dot" aria-hidden="true"></span>' : ''}
          </li>`;
      }).join('');
    } catch { ul.innerHTML = '<li class="notif-dd-empty">불러오기 실패</li>'; }
  }

  /* ── 뱃지 폴링 (로그인 확인 후에만 실행) ── */
  async function pollBadge() {
    const nav = document.getElementById('headerNav');
    if (!nav || !nav.querySelector('.header-nav__username')) return; // 비로그인 스킵
    try {
      const res  = await fetch('/api/notifications/unread-count', { credentials: 'include' });
      if (!res.ok) return;
      const body = await res.json();
      setBadge(body?.data ?? 0);
    } catch {}
  }

  function setBadge(n) {
    const el = document.getElementById('notif-hdr-badge');
    if (!el) return;
    el.textContent   = n > 99 ? '99+' : String(n);
    el.style.display = n > 0 ? 'inline-flex' : 'none';
  }

  function fmtAgo(iso) {
    if (!iso) return '';
    const d = Math.floor((Date.now() - new Date(iso)) / 1000);
    if (d < 60)     return '방금';
    if (d < 3600)   return Math.floor(d / 60) + '분 전';
    if (d < 86400)  return Math.floor(d / 3600) + '시간 전';
    if (d < 604800) return Math.floor(d / 86400) + '일 전';
    return iso.slice(0, 10);
  }
  function esc(s) {
    return String(s ?? '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
  }

  /* ── 전역 공개 API ── */
  window.__notif = {
    read: async function(id, url) {
      try {
        await fetch('/api/notifications/' + id + '/read', { method: 'PATCH', credentials: 'include' });
        // 드롭다운 즉시 반영
        const ddItem = document.querySelector('#notif-dd-list .notif-dd-item--unread[onclick*="' + id + '"]');
        if (ddItem) { ddItem.classList.remove('notif-dd-item--unread'); ddItem.querySelector('.notif-dd-unread-dot')?.remove(); }
        // 마이페이지 즉시 반영
        const mpItem = document.querySelector('.mp-notif-item--unread[data-id="' + id + '"]');
        if (mpItem) { mpItem.classList.remove('mp-notif-item--unread'); mpItem.querySelector('.mp-notif-unread-dot')?.remove(); }
        pollBadge();
        if (url) location.href = url;
      } catch {}
    },
    markAll: async function() {
      try {
        await fetch('/api/notifications/read-all', { method: 'PATCH', credentials: 'include' });
        document.querySelectorAll('.notif-dd-item--unread, .mp-notif-item--unread').forEach(el => {
          el.classList.remove('notif-dd-item--unread', 'mp-notif-item--unread');
          el.querySelector('.notif-dd-unread-dot, .mp-notif-unread-dot')?.remove();
        });
        setBadge(0);
        const mpBadge = document.getElementById('mp-notif-badge');
        if (mpBadge) mpBadge.style.display = 'none';
      } catch {}
    },
  };

  /* ── 초기화: header.js 가 nav 를 렌더한 뒤 실행되어야 함 ──
     header.js 는 async checkAuth() → renderNav() 순서로 동작.
     MutationObserver 로 headerNav 에 username 이 추가되는 시점을 감지. */
  function waitForLogin() {
    const nav = document.getElementById('headerNav');
    if (!nav) return;

    const observer = new MutationObserver(() => {
      if (nav.querySelector('.header-nav__username')) {
        observer.disconnect();
        injectBtn();
        injectDd();
        pollBadge();
        setInterval(pollBadge, POLL_MS);
      }
    });
    observer.observe(nav, { childList: true, subtree: true });

    // 이미 렌더된 경우(캐시 히트) 즉시 처리
    if (nav.querySelector('.header-nav__username')) {
      observer.disconnect();
      injectBtn();
      injectDd();
      pollBadge();
      setInterval(pollBadge, POLL_MS);
    }
  }

  document.readyState === 'loading'
    ? document.addEventListener('DOMContentLoaded', waitForLogin)
    : waitForLogin();
})();


/* ================================================================
   §2. 마이페이지 최근 알림 섹션  (#mp-notif-section 있을 때만)
================================================================ */
(function MypageNotif() {
  if (!document.getElementById('mp-notif-section')) return;

  const CAT_ICON = {
    TERMS_CHANGED: '&#128203;', CARD_UPDATED: '&#128179;',
    EVENT: '&#127873;', NOTICE: '&#128226;', SYSTEM: '&#9881;',
  };

  function esc(s) {
    return String(s ?? '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
  }
  function fmtAgo(iso) {
    if (!iso) return '';
    const d = Math.floor((Date.now() - new Date(iso)) / 1000);
    if (d < 60)     return '방금';
    if (d < 3600)   return Math.floor(d / 60) + '분 전';
    if (d < 86400)  return Math.floor(d / 3600) + '시간 전';
    if (d < 604800) return Math.floor(d / 86400) + '일 전';
    return iso.slice(0, 10);
  }

  function skeleton(n) {
    return Array.from({ length: n }, () => `
      <li class="mp-notif-skeleton">
        <div class="mp-sk mp-sk-circle"></div>
        <div class="mp-sk-lines">
          <div class="mp-sk mp-sk-line mp-sk-line--long"></div>
          <div class="mp-sk mp-sk-line mp-sk-line--short"></div>
        </div>
      </li>`).join('');
  }

  async function load() {
    const ul    = document.getElementById('mp-notif-list');
    const badge = document.getElementById('mp-notif-badge');
    if (!ul) return;

    ul.innerHTML = skeleton(3);

    try {
      const res  = await fetch('/api/notifications', { credentials: 'include' });
      if (!res.ok) { ul.innerHTML = '<li class="mp-notif-empty">알림을 불러오지 못했습니다.</li>'; return; }
      const body = await res.json();
      const { unreadCount = 0, notifications: items = [] } = body?.data ?? {};

      if (badge) {
        badge.textContent   = unreadCount > 99 ? '99+' : String(unreadCount);
        badge.style.display = unreadCount > 0 ? 'inline-flex' : 'none';
      }

      if (!items.length) { ul.innerHTML = '<li class="mp-notif-empty">새 알림이 없습니다.</li>'; return; }

      ul.innerHTML = items.slice(0, 5).map(n => {
        const unread  = n.readYn === 'N';
        const catKey  = (n.notificationCategory ?? '').toLowerCase();
        const icon    = CAT_ICON[n.notificationCategory] ?? '&#128276;';
        const preview = esc((n.message ?? '').slice(0, 55) + ((n.message ?? '').length > 55 ? '…' : ''));
        return `
          <li class="mp-notif-item${unread ? ' mp-notif-item--unread' : ''}"
              data-id="${n.notificationId}"
              onclick="window.__notif.read(${n.notificationId},'${esc(n.linkUrl ?? '')}')">
            <span class="mp-notif-icon mp-notif-icon--${catKey}" aria-hidden="true">${icon}</span>
            <div class="mp-notif-text">
              <p class="mp-notif-title">${esc(n.title)}</p>
              <p class="mp-notif-msg">${preview}</p>
              <p class="mp-notif-time">${fmtAgo(n.createdAt)}</p>
            </div>
            ${unread ? '<span class="mp-notif-unread-dot" aria-hidden="true"></span>' : ''}
          </li>`;
      }).join('');
    } catch {
      ul.innerHTML = '<li class="mp-notif-empty">알림을 불러오지 못했습니다.</li>';
    }
  }

  document.readyState === 'loading'
    ? document.addEventListener('DOMContentLoaded', load)
    : load();
})();


/* ================================================================
   §3. 관리자 알림 발송 관리  (#batchTbody 있을 때만)
================================================================ */
(function AdminNotif() {
  if (!document.getElementById('batchTbody')) return;

  const LOGIN_URL = '/admin/login';
  let curPage = 0, curBatchId = null;
  const PAGE_SIZE = 20;

  async function api(method, url, body) {
    const opts = { method, credentials: 'include', headers: { 'Content-Type': 'application/json' } };
    if (body) opts.body = JSON.stringify(body);
    const res  = await fetch(url, opts);
    if (res.status === 401) { location.href = LOGIN_URL; return { ok: false, data: null }; }
    const data = await res.json().catch(() => ({}));
    if (res.status === 403) { toast('error', data?.message ?? '접근 권한이 없습니다.'); return { ok: false, data }; }
    if (res.status >= 500) { toast('error', '서버 내부 오류가 발생했습니다.'); return { ok: false, data }; }
    return { ok: res.ok, data };
  }

  function extractMsg(d, fb = '처리 중 오류가 발생했습니다.') {
    if (d?.fieldErrors?.length) { const f = d.fieldErrors[0]; return f.field ? `[${f.field}] ${f.reason}` : f.reason; }
    return d?.message ?? d?.detail ?? fb;
  }
  function toast(type, msg) {
    if (typeof window.Toast !== 'undefined') return window.Toast[type]?.(msg);
    if (typeof window.BnkToast !== 'undefined') return window.BnkToast[type]?.(msg);
    alert((type === 'error' ? '오류: ' : '') + msg);
  }

  const CAT_L = { TERMS_CHANGED:'약관 변경', CARD_UPDATED:'카드 변경', EVENT:'이벤트', NOTICE:'공지사항', SYSTEM:'시스템' };
  const CAT_C = { TERMS_CHANGED:'cat-terms', CARD_UPDATED:'cat-card', EVENT:'cat-event', NOTICE:'cat-notice', SYSTEM:'cat-system' };
  const ST_L  = { DRAFT:'초안', SCHEDULED:'예약됨', SENDING:'발송중', DONE:'완료', FAILED:'실패' };
  const ST_C  = { DRAFT:'badge-draft', SCHEDULED:'badge-scheduled', SENDING:'badge-sending', DONE:'badge-done', FAILED:'badge-failed' };
  const TG_L  = { ALL:'전체 회원', TERMS_AGREED:'약관 동의자', CARD_OWNER:'카드 보유자', MARKETING_AGREE:'마케팅 동의' };

  function fmtDt(s) { return s ? s.replace('T',' ').slice(0,16) : '-'; }
  function g(id) { return document.getElementById(id)?.value ?? ''; }
  function esc(s) { return String(s ?? '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); }

  /* ── 탭 (전역) ── */
  window.switchTab = function(t) {
    document.getElementById('tabList').style.display   = t === 'list'   ? '' : 'none';
    document.getElementById('tabCreate').style.display = t === 'create' ? '' : 'none';
    document.getElementById('tabBtnList').classList.toggle('active',   t === 'list');
    document.getElementById('tabBtnCreate').classList.toggle('active', t === 'create');
    if (t === 'list') loadBatches(0);
  };

  /* ── 목록 ── */
  async function loadBatches(page) {
    curPage = page;
    const { ok, data } = await api('GET', `/api/admin/notifications?page=${page}&size=${PAGE_SIZE}`);
    if (!ok) return;
    const items = data?.data?.content ?? [], total = data?.data?.totalCount ?? 0;
    document.getElementById('totalCount').textContent = total;
    const tb = document.getElementById('batchTbody');
    if (!items.length) {
      tb.innerHTML = '<tr><td colspan="10" class="empty-cell">발송 이력이 없습니다.</td></tr>';
      document.getElementById('pagination').innerHTML = '';
      return;
    }
    tb.innerHTML = items.map(b => `
      <tr>
        <td style="font-size:12px;color:#00677F;font-weight:700">#${b.batchId}</td>
        <td><span class="cat-badge ${CAT_C[b.notificationCategory]??''}">${CAT_L[b.notificationCategory]??esc(b.notificationCategory)}</span></td>
        <td style="font-weight:600;max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${esc(b.title)}">${esc(b.title)}</td>
        <td>${TG_L[b.targetType]??esc(b.targetType)}</td>
        <td>${esc((b.channels??'').replace(/,/g,' / '))}</td>
        <td style="text-align:right">${b.recipientCount>0?b.recipientCount.toLocaleString()+'명':'-'}</td>
        <td><span class="badge ${ST_C[b.status]??''}">${ST_L[b.status]??esc(b.status)}</span></td>
        <td style="font-size:12px;color:#9BB4BB">${b.sentAt?fmtDt(b.sentAt):(b.scheduledAt?'예약: '+fmtDt(b.scheduledAt):'-')}</td>
        <td style="font-size:12px">${esc(b.createdByName??'-')}</td>
        <td style="white-space:nowrap">
          <button type="button" class="btn-detail-link" onclick="openDetail(${b.batchId})">상세</button>
          ${(b.status==='DRAFT'||b.status==='SCHEDULED')?`<button type="button" class="btn-send-now" onclick="confirmSend(${b.batchId})" style="margin-left:4px">발송</button>`:''}
        </td>
      </tr>`).join('');
    renderPaging(total, page);
  }

  function renderPaging(total, page) {
    const pages = Math.ceil(total / PAGE_SIZE), pg = document.getElementById('pagination');
    if (pages <= 1) { pg.innerHTML = ''; return; }
    const s = Math.max(0,page-2), e = Math.min(pages-1,s+4);
    let h = `<button type="button" onclick="loadBatches(${page-1})" ${page===0?'disabled':''}>&#8249;</button>`;
    for (let i=s;i<=e;i++) h += `<button type="button" class="${i===page?'active':''}" onclick="loadBatches(${i})">${i+1}</button>`;
    h += `<button type="button" onclick="loadBatches(${page+1})" ${page>=pages-1?'disabled':''}>&#8250;</button>`;
    pg.innerHTML = h;
  }

  /* ── 상세 모달 (전역) ── */
  window.openDetail = async function(id) {
    curBatchId = id;
    const { ok, data } = await api('GET', `/api/admin/notifications/${id}`);
    if (!ok) return;
    const b = data?.data, can = b.status==='DRAFT'||b.status==='SCHEDULED';
    document.getElementById('detailBody').innerHTML = `
      <div class="stat-row">
        <div class="stat-card"><div class="stat-num">${b.recipientCount?.toLocaleString()??0}</div><div class="stat-lbl">총 수신자</div></div>
        <div class="stat-card"><div class="stat-num" style="color:#0A6B55">${b.successCount?.toLocaleString()??0}</div><div class="stat-lbl">성공</div></div>
        <div class="stat-card"><div class="stat-num" style="color:#C0392B">${b.failCount?.toLocaleString()??0}</div><div class="stat-lbl">실패</div></div>
      </div>
      ${[
        ['카테고리', `<span class="cat-badge ${CAT_C[b.notificationCategory]??''}">${CAT_L[b.notificationCategory]??esc(b.notificationCategory)}</span>`],
        ['제목',     esc(b.title)],
        ['내용',     esc(b.message??'-')],
        ['링크 URL', esc(b.linkUrl??'-')],
        ['발송 채널', esc((b.channels??'').replace(/,/g,' / '))],
        ['수신 대상', TG_L[b.targetType]??esc(b.targetType)],
        ['상태',     `<span class="badge ${ST_C[b.status]??''}">${ST_L[b.status]??esc(b.status)}</span>`],
        ['작성자',   esc(b.createdByName??'-')],
        ['발송자',   esc(b.sentByName??'-')],
        ['예약 시각', b.scheduledAt?fmtDt(b.scheduledAt):'-'],
        ['발송 시각', b.sentAt?fmtDt(b.sentAt):'-'],
        ['작성일',   fmtDt(b.createdAt)],
      ].map(([k,v])=>`<div class="detail-row"><span class="detail-key">${k}</span><span class="detail-val">${v}</span></div>`).join('')}`;
    document.getElementById('btnModalSend').style.display = can ? '' : 'none';
    document.getElementById('detailModal').style.display  = 'flex';
  };

  window.closeModal = function() { document.getElementById('detailModal').style.display='none'; curBatchId=null; };
  window.sendBatchFromModal = async function() {
    if (!curBatchId || !confirm('즉시 발송하시겠습니까?\n이 작업은 되돌릴 수 없습니다.')) return;
    await execSend(curBatchId); window.closeModal();
  };
  window.confirmSend = async function(id) {
    if (!confirm('즉시 발송하시겠습니까?\n이 작업은 되돌릴 수 없습니다.')) return;
    await execSend(id);
  };
  async function execSend(id) {
    const { ok, data } = await api('POST', `/api/admin/notifications/${id}/send`);
    ok ? toast('success', `발송 완료! 수신자 ${data?.data?.recipientCount??0}명`)
       : toast('error', extractMsg(data));
    loadBatches(curPage);
  }

  /* ── 폼 (전역) ── */
  window.onTargetTypeChange = function() {
    const t = g('fTargetType');
    const rw = document.getElementById('refIdWrap'), pw = document.getElementById('recipientPreview'), h = document.getElementById('refIdHelp');
    if (t==='TERMS_AGREED') {
      rw.style.display = pw.style.display = '';
      h.textContent = '약관 ID를 입력하세요.';
      document.getElementById('fTargetRefId').placeholder = '약관 ID';
    } else if (t==='CARD_OWNER') {
      rw.style.display = pw.style.display = '';
      h.textContent = '카드 ID를 입력하세요.';
      document.getElementById('fTargetRefId').placeholder = '카드 ID';
    } else {
      rw.style.display = 'none';
      pw.style.display = t ? '' : 'none';
    }
  };
  window.onSendModeChange = function() {
    document.getElementById('scheduledAtWrap').style.display =
      document.querySelector('input[name="sendMode"]:checked')?.value === 'scheduled' ? '' : 'none';
  };
  window.previewRecipients = async function() {
    const t = g('fTargetType'), ref = g('fTargetRefId'), el = document.getElementById('recipientCount');
    el.textContent = '조회 중...';
    const urls = { ALL: '/api/admin/notifications/preview?type=ALL', MARKETING_AGREE: '/api/admin/notifications/preview?type=MARKETING_AGREE' };
    const url = urls[t]
      ?? (ref && t==='TERMS_AGREED' ? `/api/admin/notifications/preview?type=TERMS_AGREED&refId=${ref}` : null)
      ?? (ref && t==='CARD_OWNER'   ? `/api/admin/notifications/preview?type=CARD_OWNER&refId=${ref}`   : null);
    if (!url) { el.textContent = '대상 유형을 먼저 선택하세요'; return; }
    const { ok, data } = await api('GET', url);
    el.textContent = ok ? `예상 수신자: ${(data?.data??0).toLocaleString()}명` : '조회 실패';
  };
  window.togglePreview = function() {
    const p = document.getElementById('previewPanel');
    if (p.style.display === 'none') { updatePreview(); p.style.display = ''; } else p.style.display = 'none';
  };
  function updatePreview() {
    const chk = id => document.getElementById(id)?.checked;
    document.getElementById('prevCategory').textContent = CAT_L[g('fCategory')] ?? g('fCategory') ?? '-';
    document.getElementById('prevTitle').textContent    = g('fTitle') || '(제목 없음)';
    document.getElementById('prevMessage').textContent  = g('fMessage') || '(내용 없음)';
    document.getElementById('prevLink').textContent     = g('fLinkUrl') ? '링크: ' + g('fLinkUrl') : '';
    document.getElementById('prevChannels').textContent =
      ['chInapp','chPush','chEmail'].filter(chk).map(id => document.getElementById(id).value).join(' / ') || '-';
    document.getElementById('prevTarget').textContent = TG_L[g('fTargetType')] ?? g('fTargetType') ?? '-';
  }
  function getChannels() {
    return ['chInapp','chPush','chEmail'].filter(id => document.getElementById(id)?.checked)
      .map(id => document.getElementById(id).value).join(',');
  }
  function validate() {
    let ok = true;
    const err = (id, msg) => { const el = document.getElementById(id); if (el) el.textContent = msg; if (msg) ok = false; };
    err('err-category', !g('fCategory')        ? '카테고리를 선택하세요.'            : '');
    err('err-title',    !g('fTitle').trim()     ? '제목을 입력하세요.'              : '');
    err('err-message',  !g('fMessage').trim()   ? '내용을 입력하세요.'              : '');
    err('err-channels', !getChannels()          ? '발송 채널을 하나 이상 선택하세요.' : '');
    err('err-target',   !g('fTargetType')       ? '수신 대상을 선택하세요.'          : '');
    return ok;
  }
  window.submitBatch = async function() {
    if (!validate()) return;
    const mode = document.querySelector('input[name="sendMode"]:checked')?.value;
    const body = {
      notificationCategory: g('fCategory'),
      title:       g('fTitle').trim(),
      message:     g('fMessage').trim(),
      linkUrl:     g('fLinkUrl').trim() || null,
      channels:    getChannels(),
      targetType:  g('fTargetType'),
      targetRefId: g('fTargetRefId') ? Number(g('fTargetRefId')) : null,
      scheduledAt: mode === 'scheduled' ? g('fScheduledAt') || null : null,
    };
    const btn = document.getElementById('btnSend');
    btn.disabled = true; btn.textContent = '처리 중...';
    const { ok, data } = await api('POST', '/api/admin/notifications', body);
    if (!ok) { toast('error', extractMsg(data)); btn.disabled=false; btn.textContent='발송하기'; return; }
    if (mode === 'immediate' && data?.data?.batchId) {
      const r = await api('POST', `/api/admin/notifications/${data.data.batchId}/send`);
      r.ok ? toast('success', `발송 완료! 수신자 ${r.data?.data?.recipientCount??0}명`)
           : toast('error', '저장됐으나 발송 오류가 발생했습니다.');
    } else {
      toast('success', '예약 알림이 등록되었습니다.');
    }
    resetForm();
    window.switchTab('list');
    btn.disabled = false; btn.textContent = '발송하기';
  };
  function resetForm() {
    ['fCategory','fTitle','fMessage','fLinkUrl','fTargetType','fTargetRefId','fScheduledAt']
      .forEach(id => { const e = document.getElementById(id); if (e) e.value = ''; });
    document.getElementById('chInapp').checked = true;
    document.getElementById('chPush').checked  = false;
    document.getElementById('chEmail').checked = false;
    document.getElementById('titleLen').textContent = '0';
    ['previewPanel','refIdWrap','recipientPreview','scheduledAtWrap']
      .forEach(id => { const e = document.getElementById(id); if (e) e.style.display = 'none'; });
    document.querySelector('input[name="sendMode"][value="immediate"]').checked = true;
    ['err-category','err-title','err-message','err-channels','err-target']
      .forEach(id => { const e = document.getElementById(id); if (e) e.textContent = ''; });
  }
  document.getElementById('fTitle')?.addEventListener('input', function() {
    document.getElementById('titleLen').textContent = this.value.length;
  });

  document.readyState === 'loading'
    ? document.addEventListener('DOMContentLoaded', () => loadBatches(0))
    : loadBatches(0);
})();