/**
 * header.js  |  BNK 부산은행 공통 헤더
 * =====================================================================
 * ▸ JWT 토큰은 HttpOnly 쿠키로만 관리 — JS에서 토큰 직접 접근 불가
 * ▸ GET /api/users/me (credentials:'include') 로 인증 상태 확인
 * ▸ 401 수신 시 POST /api/auth/refresh 자동 시도 후 재확인
 * ▸ 관리자 페이지(/admin/) : GET /api/admin/auth/me 로 인증 확인
 *
 * =====================================================================
 */
(() => {
  /* ─────────────────────────────────────────────────────────────
     페이지 타입 판별
  ──────────────────────────────────────────────────────────────── */
  const path = location.pathname.replace(/\/$/, '') || '/';

  const IS_ADMIN_LOGIN = path === '/admin/login';
  const IS_ADMIN       = path.startsWith('/admin') && !IS_ADMIN_LOGIN;
  const IS_AUTH = [
      '/login', '/signup', '/find-id', '/reset-password', '/admin/login',
      '/auth/login.html', '/auth/signup.html',
      '/auth/find-id.html', '/auth/reset-password.html',
  ].includes(path);
  const IS_MYPAGE = path.startsWith('/mypage');
  const NEED_AUTH = IS_ADMIN || IS_MYPAGE;

  const REDIRECT_IF_LOGGED_IN = ['/login', '/signup'];

  const LOGIN_URL       = '/login';
  const ADMIN_LOGIN_URL = '/admin/login';
  const HOME_URL        = '/';

  const CACHE_KEY_NAME     = 'bnk_user_name';
  const CACHE_KEY_LOGIN_AT = 'bnk_login_at';
  const CACHE_TTL_MS       = 10 * 60 * 1000; // 10분

  const ACCESS_TOKEN_TTL_MS = 2 * 60 * 60 * 1000;
  const WARN_THRESHOLD_MS   = 5 * 60 * 1000;

  let _tokenTimerInterval = null;

  /* ─────────────────────────────────────────────────────────────
     푸터 FOUC 방지 — 헤더 스크립트 로드 즉시 푸터 숨김
     renderFooter() 완료 후 visibility 복원
  ──────────────────────────────────────────────────────────────── */
  function hideFooerNav() {
    const footerNav = document.querySelector('.site-footer__nav');
    if (footerNav) footerNav.style.visibility = 'hidden';
  }

  /* ─────────────────────────────────────────────────────────────
     헤더 HTML 주입  (#app-header 가 있는 페이지만)
  ──────────────────────────────────────────────────────────────── */
  function injectHeader() {
    const mount = document.getElementById('app-header');
    if (!mount) return;

    if (IS_ADMIN) {
      mount.innerHTML = `
        <header class="site-header site-header--admin">
          <div class="logo">
            <span class="logo-badge logo-badge--admin">ADMIN</span>
            <span class="logo-text logo-text--admin">부산은행 관리자</span>
          </div>
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
     세션 캐시 — name + loginAt 반드시 쌍으로 검증
  ──────────────────────────────────────────────────────────────── */
  function readCache() {
    const name    = sessionStorage.getItem(CACHE_KEY_NAME);
    const loginAt = sessionStorage.getItem(CACHE_KEY_LOGIN_AT);
    // 둘 중 하나라도 없으면 오염된 상태 → 캐시 미사용
    if (!name || !loginAt) return null;
    if (Date.now() - Number(loginAt) >= CACHE_TTL_MS) return null;
    return name;
  }

  function writeCache(name) {
    sessionStorage.setItem(CACHE_KEY_NAME, name);
    sessionStorage.setItem(CACHE_KEY_LOGIN_AT, String(Date.now()));
  }

  function clearCache() {
      sessionStorage.removeItem(CACHE_KEY_NAME);
      sessionStorage.removeItem(CACHE_KEY_LOGIN_AT);
      sessionStorage.removeItem('bnk_is_admin');
  }

  /* ─────────────────────────────────────────────────────────────
     Refresh Token 재발급 시도
  ──────────────────────────────────────────────────────────────── */
  async function tryRefresh() {
    const res = await BnkAPI.post('/api/auth/refresh');
    if (res.ok) {
      // loginAt만 갱신 (name은 유지)
      sessionStorage.setItem(CACHE_KEY_LOGIN_AT, String(Date.now()));
    }
    return res.ok;
  }

  /* ─────────────────────────────────────────────────────────────
     인증 상태 확인
  ──────────────────────────────────────────────────────────────── */
  async function checkUserAuth() {
    let name = null;

    // [FIX-1] 쌍 검증 캐시 읽기
    const cached = readCache();
    if (cached) {
      name = cached;
    } else {
      // 캐시가 없거나 오염 → 서버 확인 시도
      // NEED_AUTH 페이지가 아닌 경우, 세션이 없으면 비로그인 처리(불필요한 401 방지)
      const hadLoginAt = !!sessionStorage.getItem(CACHE_KEY_LOGIN_AT);

      if (!hadLoginAt && !NEED_AUTH) {
        // 비인증 페이지에서 이전 세션 흔적 없음 → 비로그인 처리
        // (불필요한 /api/users/me 호출 생략)
      } else {
        try {
          let res = await fetch('/api/users/me', { credentials: 'include' });

          // access_token 만료 → refresh 시도
          if (res.status === 401) {
            const refreshed = await tryRefresh();
            if (refreshed) {
              res = await fetch('/api/users/me', { credentials: 'include' });
            }
          }

          // refresh 후에도 401 → 세션 완전 만료
          if (res.status === 401) {
            clearCache();
            if (NEED_AUTH) {
              const next = encodeURIComponent(location.pathname + location.search);
              location.replace(`${LOGIN_URL}?next=${next}`);
              return;
            }
            // 비인증 페이지는 비로그인 상태로 계속
          } else if (res.ok) {
            const json = await res.json().catch(() => ({}));
            name = json.data?.maskedName ?? json.name ?? '회원';
            writeCache(name); // [FIX-1] 쌍으로 저장
          } else {
            // 5xx 등 서버 오류 → BnkAPI가 Toast 처리함
            clearCache();
          }
        } catch {
          // 네트워크 오류 → 비로그인 처리
          clearCache();
        }
      }
    }

    const loggedIn = name !== null;

    if (IS_AUTH && loggedIn && REDIRECT_IF_LOGGED_IN.includes(path)) {
      location.replace(HOME_URL);
      return;
    }
    if (NEED_AUTH && !loggedIn) {
      const next = encodeURIComponent(location.pathname + location.search);
      location.replace(`${LOGIN_URL}?next=${next}`);
      return;
    }

    renderNav(loggedIn, name);
    renderFooter(loggedIn);
  }

  /* 관리자 인증 확인 */
  async function checkAdminAuth() {
    let authenticated = false;
    try {
      const res = await fetch('/api/admin/auth/me', { credentials: 'include' });
      authenticated = res.ok;
    } catch { /* 네트워크 오류는 미인증으로 처리 */ }

    if (!authenticated) {
      location.replace(ADMIN_LOGIN_URL);
      return;
    }

    const adminName = sessionStorage.getItem(CACHE_KEY_NAME) ?? '관리자';
    sessionStorage.setItem('bnk_is_admin', '1');
    renderAdminNav(adminName);
    renderFooter(true);
  }

  /* 인증 진입점 */
  async function checkAuth() {
    if (IS_ADMIN) {
      await checkAdminAuth();
    } else {
      await checkUserAuth();
    }
  }

  /* ─────────────────────────────────────────────────────────────
     헤더 내비 렌더링
  ──────────────────────────────────────────────────────────────── */
  function renderNav(loggedIn, name) {
    const nav = document.getElementById('headerNav');
    if (!nav) return;

    if (loggedIn) {
      nav.innerHTML = `
        <span class="header-nav__username">${esc(name)}님</span>
        <a href="/mypage/index.html">마이페이지</a>
        <span class="header-nav__timer" id="hdrTokenTimer" title="Access Token 남은 시간">--:--:--</span>
        <button class="header-nav__btn" id="hdrRefresh">토큰 재발급</button>
        <button class="header-nav__btn" id="hdrLogout">로그아웃</button>`;

      document.getElementById('hdrRefresh').addEventListener('click', manualRefresh);
      document.getElementById('hdrLogout').addEventListener('click', logout);
      startTokenTimer();
    } else {
      nav.innerHTML = `
        <a href="/auth/login.html" class="nav-login">로그인</a>
        <a href="/auth/signup.html" class="nav-signup">회원가입</a>`;
    }

    markActiveLink(nav);
  }

  function renderAdminNav(name) {
    const nav = document.getElementById('headerNav');
    if (!nav) return;

    nav.innerHTML = `
      <span class="header-nav__username">${esc(name)}님</span>
      <span class="header-nav__timer" id="hdrTokenTimer" title="Access Token 남은 시간">--:--:--</span>
      <button class="header-nav__btn" id="hdrLogout">로그아웃</button>`;

    document.getElementById('hdrLogout').addEventListener('click', adminLogout);
    markActiveLink(nav);
    startTokenTimer();
  }

  /* ─────────────────────────────────────────────────────────────
     푸터 nav 렌더링
     visibility 복원으로 FOUC 제거
  ──────────────────────────────────────────────────────────────── */
  function renderFooter(loggedIn) {
    const footerNav = document.querySelector('.site-footer__nav');
    if (!footerNav) return;

    if (loggedIn) {
      footerNav.innerHTML = `
        <a href="/">카드 홈</a>
        <a href="/mypage/index.html">마이페이지</a>`;
    } else {
      footerNav.innerHTML = `
        <a href="/">카드 홈</a>
        <a href="/auth/login.html">로그인</a>
        <a href="/auth/signup.html">회원가입</a>
        <a href="/mypage/index.html">마이페이지</a>`;
    }

    // 정확한 상태 렌더링 후 표시
    footerNav.style.visibility = '';
  }

  /* ─────────────────────────────────────────────────────────────
     markActiveLink — clean URL + .html 경로 모두 처리
     예) /mypage 접근 시 href="/mypage/index.html" 도 active 처리
  ──────────────────────────────────────────────────────────────── */
  function markActiveLink(nav) {
    const currentPath = location.pathname;
    nav.querySelectorAll('a[href]').forEach(a => {
      const href = a.getAttribute('href');
      if (!href || href === '#') return;

      const isExact = currentPath === href;

      // /mypage/index.html ↔ /mypage 양방향 매칭
      const hrefClean  = href.replace(/\/index\.html$/, '') || '/';
      const pathClean  = currentPath.replace(/\/index\.html$/, '') || '/';
      const isClean    = hrefClean !== '/' && (pathClean === hrefClean || currentPath === hrefClean);

      // 디렉토리 prefix 매칭 (예: /mypage/* → /mypage/index.html 활성)
      const isParent = href !== '/' &&
          !href.endsWith('.html') &&
          currentPath.startsWith(href + '/');

      a.classList.toggle('active', isExact || isClean || isParent);
    });
  }

  /* ─────────────────────────────────────────────────────────────
     Access Token 유효 시간 타이머
  ──────────────────────────────────────────────────────────────── */
  function startTokenTimer() {
    stopTokenTimer();

    function tick() {
      const timerEl = document.getElementById('hdrTokenTimer');
      if (!timerEl) { stopTokenTimer(); return; }

      const loginAt   = Number(sessionStorage.getItem(CACHE_KEY_LOGIN_AT) ?? 0);
      const elapsed   = Date.now() - loginAt;
      const remaining = ACCESS_TOKEN_TTL_MS - elapsed;

      if (remaining <= 0) {
        stopTokenTimer();
        BnkToast.error('세션이 만료되었습니다. 다시 로그인해 주세요.');
        clearCache();
        setTimeout(() => location.replace(LOGIN_URL), 1500);
        return;
      }

      const h = Math.floor(remaining / 3_600_000);
      const m = Math.floor((remaining % 3_600_000) / 60_000);
      const s = Math.floor((remaining % 60_000) / 1_000);
      timerEl.textContent =
        `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;

      if (remaining < WARN_THRESHOLD_MS) {
        timerEl.style.color      = '#ea580c';
        timerEl.style.background = '#fff7ed';
      } else {
        timerEl.style.color      = '';
        timerEl.style.background = '';
      }
    }

    tick();
    _tokenTimerInterval = setInterval(tick, 1_000);
  }

  function stopTokenTimer() {
    if (_tokenTimerInterval) {
      clearInterval(_tokenTimerInterval);
      _tokenTimerInterval = null;
    }
  }

  /* ─────────────────────────────────────────────────────────────
     토큰 수동 재발급
  ──────────────────────────────────────────────────────────────── */
  async function manualRefresh() {
    const btn = document.getElementById('hdrRefresh');
    if (btn) { btn.disabled = true; btn.textContent = '재발급 중...'; }

    const res = await BnkAPI.post('/api/auth/refresh');

    if (btn) { btn.disabled = false; btn.textContent = '토큰 재발급'; }

    if (res.ok) {
      sessionStorage.setItem(CACHE_KEY_LOGIN_AT, String(Date.now()));
      BnkToast.success('토큰이 재발급되었습니다.');
    } else if (res.status !== 0) {
      const msg = BnkError.extract(res.data, '재발급 실패. 다시 로그인해 주세요.');
      BnkToast.error(msg);
      setTimeout(() => location.replace(LOGIN_URL), 1500);
    }
  }

  /* ─────────────────────────────────────────────────────────────
     로그아웃
  ──────────────────────────────────────────────────────────────── */
  async function logout() {
    stopTokenTimer();
    try {
      await fetch('/api/auth/logout', { method: 'POST', credentials: 'include' });
    } finally {
      clearCache();
      location.replace(LOGIN_URL);
    }
  }

  async function adminLogout() {
      stopTokenTimer();
      try {
          await fetch('/api/admin/auth/logout', { method: 'POST', credentials: 'include' });
      } finally {
          clearCache();
          sessionStorage.removeItem('bnk_is_admin');
          location.replace(ADMIN_LOGIN_URL);
      }
  }
  /* ─────────────────────────────────────────────────────────────
     XSS 방어용 이스케이프
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
     초기화
  ──────────────────────────────────────────────────────────────── */
  hideFooerNav();     // 푸터 즉시 숨김 (FOUC 방지)
  injectHeader();     // 헤더 뼈대 즉시 삽입
  checkAuth();        // 인증 확인 → renderNav + renderFooter

  // 하위 호환
  window.showToast = (msg, type = 'info') => BnkToast[type]?.(msg) ?? BnkToast.info(msg);
})();
