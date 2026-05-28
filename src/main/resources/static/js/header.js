/**
 * header.js  |  BNK 부산은행 공통 헤더
 * =====================================================================
 * ▸ JWT 토큰은 HttpOnly 쿠키로만 관리 — JS에서 토큰 직접 접근 불가
 * ▸ GET /api/users/me (credentials:'include') 로 인증 상태 확인
 * ▸ 401 수신 시 POST /api/auth/refresh 자동 시도 후 재확인
 * ▸ 관리자 페이지(/admin/) : GET /api/admin/auth/me 로 인증 확인
 *   전용 me 엔드포인트 사용
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
 *
 * [1] 토큰 재발급 버튼 유지 (사용자 요청)
 * [2] bnk_user_name sessionStorage 캐싱 추가
 *     → 페이지 이동 시 /api/users/me 불필요 재호출 방지
 * [3] markActiveLink — pathname 기반 startsWith 비교
 *     → 쿼리스트링·trailing slash 있어도 active 정상 처리
 * [4] checkAdminAuth — /api/admin/auth/me 전용 엔드포인트 사용
 * [5] Access Token 남은 유효 시간 헤더 표시
 *     → loginAt 기준 2h TTL 카운트다운
 *     → 5분 미만 시 경고색(주황) 표시
 *     → 만료 시 자동 로그인 페이지 이동
 *     → 재발급 버튼 클릭 시 타이머 리셋
 * =====================================================================
 */
(() => {
  /* ─────────────────────────────────────────────────────────────
     페이지 타입 판별
  ──────────────────────────────────────────────────────────────── */
  const path = location.pathname.replace(/\/$/, '') || '/';

  const IS_ADMIN_LOGIN = path === '/admin/login';
  const IS_ADMIN       = path.startsWith('/admin') && !IS_ADMIN_LOGIN;
  const IS_AUTH        = ['/login', '/signup', '/find-id', '/reset-password', '/admin/login'].includes(path);
  const IS_MYPAGE      = path.startsWith('/mypage');
  const NEED_AUTH      = IS_ADMIN || IS_MYPAGE;

  const REDIRECT_IF_LOGGED_IN = ['/login', '/signup'];

  const LOGIN_URL       = '/login';
  const ADMIN_LOGIN_URL = '/admin/login';
  const HOME_URL        = '/';

  // sessionStorage 캐시 키
  const CACHE_KEY_NAME     = 'bnk_user_name';
  const CACHE_KEY_LOGIN_AT = 'bnk_login_at';

  // Access Token 만료 시간 — application.properties jwt.access-expiration=7200000 (2h)
  const ACCESS_TOKEN_TTL_MS = 2 * 60 * 60 * 1000;
  // 만료 5분 전부터 경고색 표시
  const WARN_THRESHOLD_MS   = 5 * 60 * 1000;

  // 타이머 인터벌 참조 (stopTokenTimer 용)
  let _tokenTimerInterval = null;

  /* ─────────────────────────────────────────────────────────────
     1. 헤더 HTML 주입  (#app-header 가 있는 페이지만)
  ──────────────────────────────────────────────────────────────── */
  function injectHeader() {
    const mount = document.getElementById('app-header');
    if (!mount) return;   // auth 페이지는 static header 사용

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
       2. 인증 상태 확인
    ──────────────────────────────────────────────────────────────── */
    async function checkUserAuth() {
        let name = null;

        const cachedName = sessionStorage.getItem(CACHE_KEY_NAME);
        const cachedLoginAt = sessionStorage.getItem(CACHE_KEY_LOGIN_AT);
        const cacheValid = cachedName && cachedLoginAt &&
            (Date.now() - Number(cachedLoginAt)) < 10 * 60 * 1000;

        if (cacheValid) {
            name = cachedName;
        } else {
            const hadSession = !!cachedLoginAt;

            // ① 완전 신규 방문자: /api/users/me 불필요 호출 차단
            if (!hadSession) {
                if (!NEED_AUTH) {
                    // ② index.html: 관리자 세션 플래그가 있을 때만 admin 확인
                    //    → 플래그 없는 일반 방문자는 /api/admin/auth/me 호출 자체를 생략
                    //      (호출 시 백엔드가 500 반환하는 문제 방어)
                    //    → 플래그는 checkAdminAuth 성공 시 set, adminLogout 시 remove
                    if ((path === '/' || path === '/index.html') &&
                            sessionStorage.getItem('bnk_is_admin') === '1') {
                        try {
                            const ar = await fetch('/api/admin/auth/me', { credentials: 'include' });
                            if (ar.ok) { location.replace('/admin/cardManage.html'); return; }
                        } catch { /* 무시 */ }
                        // admin 확인 실패(토큰 만료 등) → 플래그 정리 후 비로그인 처리
                        sessionStorage.removeItem('bnk_is_admin');
                    }
                    renderNav(false, null);
                    return;
                }
                const next = encodeURIComponent(location.pathname + location.search);
                location.replace(`${LOGIN_URL}?next=${next}`);
                return;
            }

            try {
                const res = await fetch('/api/users/me', { credentials: 'include' });

                if (res.status === 401) {
                    if (hadSession) {
                        const refreshed = await tryRefresh();
                        if (refreshed) {
                            const retryRes = await fetch('/api/users/me', { credentials: 'include' });
                            if (retryRes.ok) {
                                const json = await retryRes.json().catch(() => ({}));
                                name = json.data?.name ?? '회원';
                                sessionStorage.setItem(CACHE_KEY_NAME, name);
                                sessionStorage.setItem(CACHE_KEY_LOGIN_AT, String(Date.now()));
                            }
                        } else {
                            sessionStorage.removeItem(CACHE_KEY_NAME);
                            sessionStorage.removeItem(CACHE_KEY_LOGIN_AT);
                        }
                    }
                } else if (res.ok) {
                    const json = await res.json().catch(() => ({}));
                    name = json.data?.name ?? '회원';
                    sessionStorage.setItem(CACHE_KEY_NAME, name);
                    sessionStorage.setItem(CACHE_KEY_LOGIN_AT, String(Date.now()));
                }
            } catch { /* 네트워크 오류 → 비로그인 처리 */ }
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
    }
    /* 관리자 인증 확인
     * /api/admin/auth/me 전용 엔드포인트 사용
     * → 대시보드 API 변경·지연의 영향 차단
     */
    async function checkAdminAuth() {
        let authenticated = false;

        try {
            const res = await fetch('/api/admin/auth/me', { credentials: 'include' });
            authenticated = res.ok;
        } catch {
            /* 네트워크 오류는 미인증으로 처리 */
        }

        if (!authenticated) {
            location.replace(ADMIN_LOGIN_URL);
            return;
        }

        const adminName = sessionStorage.getItem(CACHE_KEY_NAME) ?? '관리자';
        sessionStorage.setItem('bnk_is_admin', '1');   // ← 관리자 세션 플래그 설정
        renderAdminNav(adminName);
    }

    /* ─────────────────────────────────────────────────────────────
       2-c. 인증 진입점 — IS_ADMIN 여부에 따라 분기
    ──────────────────────────────────────────────────────────────── */
    async function checkAuth() {
        if (IS_ADMIN) {
            await checkAdminAuth();
        } else {
            await checkUserAuth();
        }
    }

    /* ─────────────────────────────────────────────────────────────
       3. Refresh Token 재발급 시도 (자동)
    ──────────────────────────────────────────────────────────────── */
    async function tryRefresh() {
        try {
            const res = await fetch('/api/auth/refresh', {
                method: 'POST',
                credentials: 'include',
            });
            if (res.ok) {
                sessionStorage.setItem(CACHE_KEY_LOGIN_AT, String(Date.now()));
            }
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
        <span class="header-nav__timer" id="hdrTokenTimer" title="Access Token 남은 시간">--:--:--</span>
        <button class="header-nav__btn" id="hdrRefresh">토큰 재발급</button>
        <button class="header-nav__btn" id="hdrLogout">로그아웃</button>`;

            document.getElementById('hdrRefresh').addEventListener('click', manualRefresh);
            document.getElementById('hdrLogout').addEventListener('click', logout);

      // 타이머 시작
      startTokenTimer();
    } else {
      nav.innerHTML = `
		  <a href="/login">로그인</a>
		  <a href="/signup">회원가입</a>`;
    }

        markActiveLink(nav);
    }

    /** 관리자 내비 */
    function renderAdminNav(name) {
        const nav = document.getElementById('headerNav');
        if (!nav) return;

        nav.innerHTML = `
      <span class="header-nav__username">${esc(name)}님</span>
      <span class="header-nav__timer" id="hdrTokenTimer" title="Access Token 남은 시간">--:--:--</span>
      <button class="header-nav__btn" id="hdrLogout">로그아웃</button>`;

        document.getElementById('hdrLogout').addEventListener('click', adminLogout);
        markActiveLink(nav);

        // 관리자도 타이머 시작
        startTokenTimer();
    }

    /** 현재 경로 active 표시
     * 정확한 경로 일치(===) → pathname 기반 비교로 보완
     *   - 쿼리스트링이 있어도 (/mypage/index.html?tab=info) active 정상 처리
     *   - trailing slash 있어도 (/mypage/) active 정상 처리
     *   - 하위 경로 포함 (/admin/cardManage.html → /admin/ active 처리)
     */
    function markActiveLink(nav) {
        const currentPath = location.pathname;

        nav.querySelectorAll('a[href]').forEach(a => {
            const href = a.getAttribute('href');
            if (!href || href === '#') return;

            const isExact = currentPath === href;
            const isParent = href !== '/' && currentPath.startsWith(href.replace(/\/[^/]+\.html$/, '/'));

            a.classList.toggle('active', isExact || isParent);
        });
    }

    /* ─────────────────────────────────────────────────────────────
       5. Access Token 유효 시간 타이머
       ─────────────────────────────────────────────────────────────
       HttpOnly 쿠키라 JS에서 JWT 직접 파싱 불가.
       sessionStorage의 bnk_login_at(마지막 인증 시각) 기준으로
       ACCESS_TOKEN_TTL_MS(2h) 를 역산하여 남은 시간 표시.
    ──────────────────────────────────────────────────────────────── */
    function startTokenTimer() {
        stopTokenTimer(); // 기존 타이머 정리

        function tick() {
            const timerEl = document.getElementById('hdrTokenTimer');
            if (!timerEl) { stopTokenTimer(); return; }

            const loginAt = Number(sessionStorage.getItem(CACHE_KEY_LOGIN_AT) ?? 0);
            const elapsed = Date.now() - loginAt;
            const remaining = ACCESS_TOKEN_TTL_MS - elapsed;

            if (remaining <= 0) {
                // 만료 → 로그인 페이지로 이동
                stopTokenTimer();
                showToast('세션이 만료되었습니다. 다시 로그인해 주세요.', 'error');
                setTimeout(() => {
                    sessionStorage.removeItem(CACHE_KEY_NAME);
                    sessionStorage.removeItem(CACHE_KEY_LOGIN_AT);
                    location.replace(LOGIN_URL);
                }, 1500);
                return;
            }

            // 남은 시간 포맷: HH:MM:SS
            const totalSec = Math.floor(remaining / 1000);
            const h = Math.floor(totalSec / 3600);
            const m = Math.floor((totalSec % 3600) / 60);
            const s = totalSec % 60;
            const formatted = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;

            timerEl.textContent = formatted;

            // 5분 미만 → 경고색
            if (remaining < WARN_THRESHOLD_MS) {
                timerEl.classList.add('header-nav__timer--warn');
            } else {
                timerEl.classList.remove('header-nav__timer--warn');
            }
        }

        tick(); // 즉시 1회 실행
        _tokenTimerInterval = setInterval(tick, 1000);
    }

    function stopTokenTimer() {
        if (_tokenTimerInterval) {
            clearInterval(_tokenTimerInterval);
            _tokenTimerInterval = null;
        }
    }

    /* ─────────────────────────────────────────────────────────────
       6. 토큰 재발급 (수동)
    ──────────────────────────────────────────────────────────────── */
    async function manualRefresh() {
        try {
            const res = await fetch('/api/auth/refresh', {
                method: 'POST',
                credentials: 'include',
            });
            if (res.ok) {
                // refresh 성공 시 캐시 타임스탬프 갱신 → 타이머 리셋
                sessionStorage.setItem(CACHE_KEY_LOGIN_AT, String(Date.now()));
                startTokenTimer(); // 2h 타이머 재시작
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
       7. 로그아웃
    ──────────────────────────────────────────────────────────────── */
    async function logout() {
        stopTokenTimer(); // 타이머 정리
        try {
            await fetch('/api/auth/logout', {
                method: 'POST',
                credentials: 'include',
            });
        } finally {
            sessionStorage.removeItem(CACHE_KEY_NAME);
            sessionStorage.removeItem(CACHE_KEY_LOGIN_AT);
            location.replace(LOGIN_URL);
        }
    }

    async function adminLogout() {
        stopTokenTimer(); // 타이머 정리
        try {
            await fetch('/api/auth/logout', {
                method: 'POST',
                credentials: 'include',
            });
        } finally {
            sessionStorage.removeItem(CACHE_KEY_NAME);
            sessionStorage.removeItem(CACHE_KEY_LOGIN_AT);
            sessionStorage.removeItem('bnk_is_admin');   // ← 관리자 플래그 정리
            location.replace(ADMIN_LOGIN_URL);
        }
    }

    /* ─────────────────────────────────────────────────────────────
       8. 토스트 알림
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

        requestAnimationFrame(() => toast.classList.add('toast--show'));
        setTimeout(() => {
            toast.classList.remove('toast--show');
            setTimeout(() => toast.remove(), 300);
        }, 3000);
    }

    /* ─────────────────────────────────────────────────────────────
       9. XSS 방어용 이스케이프
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
    window.showToast = showToast;
})();