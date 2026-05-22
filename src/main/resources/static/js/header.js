/**
 * header.js  |  BNK 부산은행 공통 헤더
 * =====================================================================
 * ▸ JWT 토큰은 HttpOnly 쿠키로만 관리 — JS에서 토큰 직접 접근 불가
 * ▸ GET /api/users/me (credentials:'include') 로 인증 상태 확인
 * ▸ 401 수신 시 POST /api/auth/refresh 자동 시도 후 재확인
 * ▸ 관리자 페이지(/admin/) : GET /api/admin/dashboard 로 인증 확인
 *
 * ── 페이지별 동작 ──────────────────────────────────────────────────
 * [공통 헤더 주입 대상]
 *   • /mypage/**   : #app-header 에 site-header 주입 (비로그인 시 로그인 페이지 이동)
 *   • /admin/**    : #app-header 에 site-header--admin 주입 (미인증 시 admin-login 이동)
 *   • /index.html  : #app-header 에 site-header 주입
 *
 * [auth 페이지 — 이미 site-header 존재, headerNav 만 채움]
 *   • /auth/login.html    : 로그인 상태면 / 로 리다이렉트
 *   • /auth/signup.html   : 로그인 상태면 / 로 리다이렉트
 *   • /auth/find-id.html  : 리다이렉트 없음 (항상 접근 허용)
 *   • /auth/reset-*.html  : 리다이렉트 없음
 * =====================================================================
 */
(() => {
  /* ─────────────────────────────────────────────────────────────
     페이지 타입 판별
  ──────────────────────────────────────────────────────────────── */
  const path        = location.pathname;
  const IS_ADMIN    = path.startsWith('/admin');
  const IS_AUTH     = path.startsWith('/auth/');
  const IS_MYPAGE   = path.startsWith('/mypage/');
  const NEED_AUTH   = IS_ADMIN || IS_MYPAGE;  // 미인증 시 리다이렉트 대상

  // auth 페이지 중 로그인 상태이면 메인으로 보낼 페이지
  const REDIRECT_IF_LOGGED_IN = ['/auth/login.html', '/auth/signup.html'];

  const LOGIN_URL       = '/auth/login.html';
  const ADMIN_LOGIN_URL = '/auth/admin-login.html';
  const HOME_URL        = '/';

  /* ─────────────────────────────────────────────────────────────
     1. 헤더 HTML 주입  (#app-header 가 있는 페이지만)
  ──────────────────────────────────────────────────────────────── */
  function injectHeader() {
    const mount = document.getElementById('app-header');
    if (!mount) return;   // auth 페이지는 static header 사용

    if (IS_ADMIN) {
      mount.innerHTML = `
        <header class="site-header site-header--admin">
          <a href="/admin/index.html" class="logo">
            <span class="logo-badge logo-badge--admin">ADMIN</span>
            <span class="logo-text logo-text--admin">부산은행 관리자</span>
          </a>
          <nav class="header-nav" id="headerNav"></nav>
        </header>`;
    } else {
      mount.innerHTML = `
        <header class="site-header">
          <a href="${HOME_URL}" class="logo">
            <span class="logo-badge">BNK</span>
            <span class="logo-text">부산은행</span>
          </a>
          <nav class="header-nav" id="headerNav"></nav>
        </header>`;
    }
  }

  /* ─────────────────────────────────────────────────────────────
     2. 인증 상태 확인
        - 일반 페이지 : GET /api/users/me
        - 관리자 페이지: GET /api/admin/dashboard
  ──────────────────────────────────────────────────────────────── */
  async function checkAuth() {
    if (IS_ADMIN) {
      await checkAdminAuth();
    } else {
      await checkUserAuth();
    }
  }

  /* 일반 사용자 인증 확인 */
  async function checkUserAuth() {
    let name = null;

    try {
      let res = await fetch('/api/users/me', { credentials: 'include' });

      // 401 → Refresh Token으로 자동 재발급 시도
      if (res.status === 401) {
        const refreshed = await tryRefresh();
        if (refreshed) {
          res = await fetch('/api/users/me', { credentials: 'include' });
        }
      }

      if (res.ok) {
        const json = await res.json().catch(() => ({}));
        name = json.data?.name ?? '회원';
      }
    } catch {
      /* 네트워크 오류는 비로그인으로 처리 */
    }

    const loggedIn = name !== null;

    // auth 페이지: 이미 로그인 → 메인으로
    if (IS_AUTH && loggedIn && REDIRECT_IF_LOGGED_IN.includes(path)) {
      location.replace(HOME_URL);
      return;
    }

    // 보호 페이지: 비로그인 → 로그인으로
    if (NEED_AUTH && !loggedIn) {
      const next = encodeURIComponent(location.pathname + location.search);
      location.replace(`${LOGIN_URL}?next=${next}`);
      return;
    }

    renderNav(loggedIn, name);
  }

  /* 관리자 인증 확인 */
  async function checkAdminAuth() {
    let authenticated = false;

    try {
      const res = await fetch('/api/admin/dashboard', { credentials: 'include' });
      authenticated = res.ok;
    } catch {
      /* 네트워크 오류는 미인증으로 처리 */
    }

    if (!authenticated) {
      location.replace(ADMIN_LOGIN_URL);
      return;
    }

    // 관리자 이름은 sessionStorage (admin-login.html 에서 저장)
    const adminName = sessionStorage.getItem('bnk_user_name') ?? '관리자';
    renderAdminNav(adminName);
  }

  /* ─────────────────────────────────────────────────────────────
     3. Refresh Token 재발급 시도
        refresh_token 쿠키 경로 = /api/auth/refresh → 자동 전송
  ──────────────────────────────────────────────────────────────── */
  async function tryRefresh() {
    try {
      const res = await fetch('/api/auth/refresh', {
        method:      'POST',
        credentials: 'include',
      });
      return res.ok;
    } catch {
      return false;
    }
  }

  /* ─────────────────────────────────────────────────────────────
     4. 헤더 내비 렌더링
  ──────────────────────────────────────────────────────────────── */

  /** 일반 사용자 내비 */
  function renderNav(loggedIn, name) {
    const nav = document.getElementById('headerNav');
    if (!nav) return;

    if (loggedIn) {
      nav.innerHTML = `
        <span class="header-nav__username">${esc(name)}님</span>
        <a href="/mypage/index.html">마이페이지</a>
        <button class="header-nav__btn" id="hdrRefresh">토큰 재발급</button>
        <button class="header-nav__btn" id="hdrLogout">로그아웃</button>`;

      document.getElementById('hdrRefresh').addEventListener('click', manualRefresh);
      document.getElementById('hdrLogout').addEventListener('click', logout);
    } else {
      nav.innerHTML = `
        <a href="/auth/login.html">로그인</a>
        <a href="/auth/signup.html">회원가입</a>`;
    }

    markActiveLink(nav);
  }

  /** 관리자 내비 */
  function renderAdminNav(name) {
    const nav = document.getElementById('headerNav');
    if (!nav) return;

    nav.innerHTML = `
      <span class="header-nav__username">${esc(name)}님</span>
      <a href="/admin/index.html">대시보드</a>
      <a href="/admin/cardManage.html">카드 관리</a>
      <a href="/admin/userManage.html">회원 관리</a>
      <a href="/admin/requestApproval.html">결재 처리</a>
      <button class="header-nav__btn" id="hdrLogout">로그아웃</button>`;

    document.getElementById('hdrLogout').addEventListener('click', adminLogout);
    markActiveLink(nav);
  }

  /** 현재 경로 active 표시 */
  function markActiveLink(nav) {
    nav.querySelectorAll('a[href]').forEach(a => {
      a.classList.toggle('active', a.getAttribute('href') === path);
    });
  }

  /* ─────────────────────────────────────────────────────────────
     5. 토큰 재발급 (수동)
  ──────────────────────────────────────────────────────────────── */
  async function manualRefresh() {
    try {
      const res = await fetch('/api/auth/refresh', {
        method:      'POST',
        credentials: 'include',
      });
      if (res.ok) {
        showToast('토큰이 재발급되었습니다.', 'success');
      } else {
        showToast('재발급 실패. 다시 로그인해 주세요.', 'error');
        setTimeout(() => location.replace(LOGIN_URL), 1500);
      }
    } catch {
      showToast('서버에 연결할 수 없습니다.', 'error');
    }
  }

  /* ─────────────────────────────────────────────────────────────
     6. 로그아웃
  ──────────────────────────────────────────────────────────────── */
  async function logout() {
    try {
      await fetch('/api/auth/logout', {
        method:      'POST',
        credentials: 'include',
      });
    } finally {
      sessionStorage.removeItem('bnk_user_name');
      sessionStorage.removeItem('bnk_login_at');
      location.replace(LOGIN_URL);
    }
  }

  async function adminLogout() {
    try {
      // 관리자 세션도 동일 쿠키 삭제 엔드포인트 사용
      await fetch('/api/auth/logout', {
        method:      'POST',
        credentials: 'include',
      });
    } finally {
      sessionStorage.removeItem('bnk_user_name');
      sessionStorage.removeItem('bnk_login_at');
      location.replace(ADMIN_LOGIN_URL);
    }
  }

  /* ─────────────────────────────────────────────────────────────
     7. 토스트 알림
  ──────────────────────────────────────────────────────────────── */
  function showToast(msg, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
      container = document.createElement('div');
      container.id = 'toast-container';
      document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast--${type}`;
    toast.textContent = msg;
    container.appendChild(toast);

    // 진입 애니메이션 후 3초 제거
    requestAnimationFrame(() => toast.classList.add('toast--show'));
    setTimeout(() => {
      toast.classList.remove('toast--show');
      setTimeout(() => toast.remove(), 300);
    }, 3000);
  }

  /* ─────────────────────────────────────────────────────────────
     8. XSS 방어용 이스케이프
  ──────────────────────────────────────────────────────────────── */
  function esc(str) {
    return String(str ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  /* ─────────────────────────────────────────────────────────────
     초기화 — DOM 준비 즉시 실행
  ──────────────────────────────────────────────────────────────── */
  injectHeader();   // 헤더 뼈대 즉시 삽입 (FOUC 방지)
  checkAuth();      // 인증 확인 (비동기)
})();
