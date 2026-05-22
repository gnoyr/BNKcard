/**
 * header.js  |  BNK 부산은행 공통 헤더 컴포넌트 로더 + 세션 관리
 *
 * 사용법: 각 HTML에서 </body> 직전에 포함
 *   <div id="app-header"></div>
 *   <script src="/js/header.js"></script>
 *
 * 인증: HttpOnly 쿠키 (access_token / refresh_token)
 *   credentials:'include' 만 사용 — JS 쿠키 직접 읽기 없음
 */

'use strict';

(async () => {

  /* ── 상수 ─────────────────────────────────────────── */
  const ACCESS_TTL_MS = 2 * 60 * 60 * 1000;  // 2시간
  const WARN_MS       = 5 * 60 * 1000;         // 5분 전 경고
  const KEY_LOGIN_AT  = 'bnk_login_at';
  const KEY_USERNAME  = 'bnk_user_name';
  const LOGIN_URL     = '/auth/login.html';

  /* ── 상태 ─────────────────────────────────────────── */
  let loggedIn  = false;
  let userName  = '';
  let ticker    = null;
  let warnShown = false;

  /* ─────────────────────────────────────────────────────
     1. 헤더 컴포넌트 HTML 로드 후 #app-header에 삽입
  ───────────────────────────────────────────────────── */
  await loadComponent();

  /* ─────────────────────────────────────────────────────
     2. 인증 상태 확인
  ───────────────────────────────────────────────────── */
  await checkAuth();

  /* ─────────────────────────────────────────────────────
     3. 헤더 렌더 + 세션 바 처리
  ───────────────────────────────────────────────────── */
  renderNav();

  if (loggedIn) {
    showSessionBar();
    startTicker();
    redirectIfAlreadyLoggedIn();
  }

  /* ═══════════════════════════════════════════════════
     컴포넌트 로드
  ═══════════════════════════════════════════════════ */
  async function loadComponent() {
    const mount = document.getElementById('app-header');
    if (!mount) return;

    try {
      const res  = await fetch('/components/header.html');
      const html = await res.text();
      mount.outerHTML = html;
    } catch {
      // fallback: 최소 헤더 직접 생성
      mount.outerHTML = `
        <header class="site-header">
          <a href="/" class="logo">
            <span class="logo-badge">BNK</span>
            <span class="logo-text">부산은행</span>
          </a>
          <nav class="header-nav" id="headerNav"></nav>
        </header>`;
    }
  }

  /* ═══════════════════════════════════════════════════
     인증 상태 확인
     GET /api/users/me — credentials:'include' 로 쿠키 자동 전송
  ═══════════════════════════════════════════════════ */
  async function checkAuth() {
    try {
      const res = await fetch('/api/users/me', { credentials: 'include' });

      if (res.ok) {
        const json = await res.json().catch(() => ({}));
        setLoggedIn(json.data?.name ?? sessionStorage.getItem(KEY_USERNAME) ?? '회원');

      } else if (res.status === 401) {
        // access_token 만료 → refresh 시도
        if (await tryRefresh()) {
          const res2 = await fetch('/api/users/me', { credentials: 'include' });
          if (res2.ok) {
            const json2 = await res2.json().catch(() => ({}));
            setLoggedIn(json2.data?.name ?? '회원');
          }
        }
      }
    } catch {
      // 네트워크 오류: sessionStorage 캐시 유지
      const cached = sessionStorage.getItem(KEY_USERNAME);
      if (cached) setLoggedIn(cached);
    }
  }

  function setLoggedIn(name) {
    loggedIn  = true;
    userName  = name;
    sessionStorage.setItem(KEY_USERNAME, name);
    if (!sessionStorage.getItem(KEY_LOGIN_AT)) {
      sessionStorage.setItem(KEY_LOGIN_AT, String(Date.now()));
    }
  }

  /* ═══════════════════════════════════════════════════
     헤더 내비 렌더
  ═══════════════════════════════════════════════════ */
  function renderNav() {
    const nav = document.getElementById('headerNav');
    if (!nav) return;

    if (loggedIn) {
      nav.innerHTML = `
        <span class="header-nav__username">${esc(userName)}님</span>
        <a href="/mypage/index.html">마이페이지</a>
        <button class="header-nav__btn" id="hdrLogout">로그아웃</button>
      `;
      document.getElementById('hdrLogout').addEventListener('click', logout);
    } else {
      nav.innerHTML = `
        <a href="/auth/signup.html">회원가입</a>
        <a href="/auth/login.html">로그인</a>
      `;
    }

    // 현재 페이지 active
    nav.querySelectorAll('a[href]').forEach(a => {
      a.classList.toggle('active', a.getAttribute('href') === location.pathname);
    });

    // mp-header 유저명 반영
    const mpTitle = document.querySelector('.mp-header__title');
    if (mpTitle && loggedIn) mpTitle.textContent = esc(userName) + '님의 마이페이지';
  }

  /* ═══════════════════════════════════════════════════
     세션 바 표시
  ═══════════════════════════════════════════════════ */
  function showSessionBar() {
    const bar  = document.getElementById('sessionBar');
    const name = document.getElementById('sessionName');
    if (!bar) return;

    if (name) name.textContent = userName + '님';

    // mp-header 사용 페이지면 top 조정
    if (document.querySelector('.mp-header')) bar.classList.add('mp-offset');

    bar.hidden = false;

    document.getElementById('sessionRefresh')
            ?.addEventListener('click', refresh);
    document.getElementById('sessionLogout')
            ?.addEventListener('click', logout);
  }

  /* ═══════════════════════════════════════════════════
     카운트다운 타이머
  ═══════════════════════════════════════════════════ */
  function startTicker() {
    if (ticker) clearInterval(ticker);
    ticker = setInterval(tick, 1000);
    tick();
  }

  function tick() {
    const loginAt   = Number(sessionStorage.getItem(KEY_LOGIN_AT)) || Date.now();
    const remaining = Math.max(0, ACCESS_TTL_MS - (Date.now() - loginAt));

    const timerEl = document.getElementById('sessionTimer');
    const dotEl   = document.getElementById('sessionDot');
    if (!timerEl) return;

    const h = Math.floor(remaining / 3600000);
    const m = Math.floor((remaining % 3600000) / 60000);
    const s = Math.floor((remaining % 60000)   / 1000);
    timerEl.textContent = `${pad(h)}:${pad(m)}:${pad(s)}`;

    // 상태 클래스 토글
    timerEl.classList.toggle('session-bar__timer--warn',    remaining <= WARN_MS && remaining > 0);
    timerEl.classList.toggle('session-bar__timer--expired', remaining === 0);
    dotEl.classList.toggle('session-bar__dot--warn',    remaining <= WARN_MS && remaining > 0);
    dotEl.classList.toggle('session-bar__dot--expired', remaining === 0);

    if (remaining === 0) {
      showToast('세션이 만료되었습니다. 재발급 버튼을 눌러주세요.');
    } else if (remaining <= WARN_MS && !warnShown) {
      warnShown = true;
      showToast(`${Math.ceil(remaining / 60000)}분 후 세션이 만료됩니다.`, true);
    }
  }

  /* ═══════════════════════════════════════════════════
     토큰 재발급
     refresh_token 쿠키: path=/api/auth/refresh → 이 URL에만 자동 전송
  ═══════════════════════════════════════════════════ */
  async function refresh() {
    const btn = document.getElementById('sessionRefresh');
    if (btn) { btn.disabled = true; btn.textContent = '재발급 중...'; }

    if (await tryRefresh()) {
      warnShown = false;
      hideToast();
      showToast('토큰이 재발급되었습니다.', false, 2500);

      const timerEl = document.getElementById('sessionTimer');
      const dotEl   = document.getElementById('sessionDot');
      timerEl?.classList.remove('session-bar__timer--warn', 'session-bar__timer--expired');
      dotEl?.classList.remove('session-bar__dot--warn', 'session-bar__dot--expired');
    } else {
      showToast('재발급에 실패했습니다. 다시 로그인해주세요.');
      setTimeout(logout, 2000);
    }

    if (btn) { btn.disabled = false; btn.textContent = '↻ 재발급'; }
  }

  async function tryRefresh() {
    try {
      const res = await fetch('/api/auth/refresh', {
        method:      'POST',
        credentials: 'include',
      });
      if (res.ok) {
        sessionStorage.setItem(KEY_LOGIN_AT, String(Date.now()));
        return true;
      }
      return false;
    } catch { return false; }
  }

  /* ═══════════════════════════════════════════════════
     로그아웃
  ═══════════════════════════════════════════════════ */
  async function logout() {
    try {
      await fetch('/api/auth/logout', { method: 'POST', credentials: 'include' });
    } catch { /* 무시 */ }
    sessionStorage.removeItem(KEY_LOGIN_AT);
    sessionStorage.removeItem(KEY_USERNAME);
    if (ticker) clearInterval(ticker);
    window.location.href = LOGIN_URL;
  }

  /* ═══════════════════════════════════════════════════
     로그인 페이지 리다이렉트 (이미 로그인 상태)
  ═══════════════════════════════════════════════════ */
  function redirectIfAlreadyLoggedIn() {
    const p = location.pathname;
    if (p.endsWith('login.html') && !p.includes('admin')) {
      window.location.href = '/';
    }
  }

  /* ═══════════════════════════════════════════════════
     세션 토스트
  ═══════════════════════════════════════════════════ */
  function showToast(msg, withBtn = false, autoMs = 0) {
    const el = document.getElementById('sessionToast');
    if (!el) return;
    el.innerHTML = esc(msg)
      + (withBtn
          ? `<button class="session-toast__refresh-btn"
               onclick="document.getElementById('sessionRefresh').click()">
               ↻ 지금 재발급
             </button>`
          : '');
    el.hidden = false;
    if (autoMs > 0) setTimeout(hideToast, autoMs);
  }

  function hideToast() {
    const el = document.getElementById('sessionToast');
    if (el) el.hidden = true;
  }

  /* ═══════════════════════════════════════════════════
     유틸
  ═══════════════════════════════════════════════════ */
  function pad(n) { return String(n).padStart(2, '0'); }
  function esc(s) {
    return String(s)
      .replace(/&/g,'&amp;').replace(/</g,'&lt;')
      .replace(/>/g,'&gt;').replace(/"/g,'&quot;');
  }

  /* ── 전역 노출 (admin-login.html 등에서 logout 호출 용도) ── */
  window._BNKHeader = { logout, refresh, isLoggedIn: () => loggedIn };

})();
