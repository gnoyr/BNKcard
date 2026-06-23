/// <reference path="./global.d.ts" />
/**
 * header.js  |  BNK 부산은행 공통 헤더
 * =====================================================================
 * ▸ JWT 토큰은 HttpOnly 쿠키로만 관리 — JS에서 토큰 직접 접근 불가
 * ▸ GET /api/users/me (credentials:'include') 로 인증 상태 확인
 * ▸ 401 수신 시 POST /api/auth/refresh 자동 시도 후 재확인
 * ▸ 관리자 페이지(/admin/) : GET /api/admin/auth/me 로 인증 확인
 *
 * [변경] renderAdminNav() — 헤더 렌더 후 관리자 서브 네비 자동 주입
 * =====================================================================
 */
(() => {

    const BnkAPI   = window.BnkAPI;
    const BnkError = window.BnkError;
    const BnkToast = window.BnkToast;

    /* ─────────────────────────────────────────────────────────────
       페이지 타입 판별
    ──────────────────────────────────────────────────────────────── */
    const path = location.pathname.replace(/\/$/, '') || '/';

    const IS_ADMIN_LOGIN = path === '/admin/login';
    const IS_ADMIN = path.startsWith('/admin') && !IS_ADMIN_LOGIN;
    const IS_AUTH = [
        '/login', '/signup', '/find-id', '/reset-password',
        '/admin/login',
    ].includes(path);
    const IS_MYPAGE  = path.startsWith('/mypage');
    const NEED_AUTH  = IS_ADMIN || IS_MYPAGE;

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
       관리자 서브 네비 메뉴 목록
       href 와 현재 pathname 을 prefix 매칭해 active 클래스 부여
    ──────────────────────────────────────────────────────────────── */
    const ADMIN_NAV_ITEMS = [
        { label: '대시보드',      href: '/admin/dashboard.html'          },
        { label: '카드/약관 관리', href: '/admin/cardManage.html'          },
        { label: '결재 관리',     href: '/admin/approvals'               },
        { label: '회원 관리',     href: '/admin/users'                   },
        { label: '관리자 계정',   href: '/admin/admins.html'             },
        { label: '알림 관리',     href: '/admin/notificationManage.html' },
        { label: '로그 조회',     href: '/admin/logs.html'               },
    ];

    /* ─────────────────────────────────────────────────────────────
       푸터 FOUC 방지
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
       세션 캐시
    ──────────────────────────────────────────────────────────────── */
    function readCache() {
        const name    = sessionStorage.getItem(CACHE_KEY_NAME);
        const loginAt = Number(sessionStorage.getItem(CACHE_KEY_LOGIN_AT) ?? 0);
        if (!name || !loginAt) return null;
        if (Date.now() - loginAt > CACHE_TTL_MS) { clearCache(); return null; }
        return name;
    }

    function writeCache(name) {
        sessionStorage.setItem(CACHE_KEY_NAME, name);
        sessionStorage.setItem(CACHE_KEY_LOGIN_AT, String(Date.now()));
    }

    function clearCache() {
        sessionStorage.removeItem(CACHE_KEY_NAME);
        sessionStorage.removeItem(CACHE_KEY_LOGIN_AT);
    }

    /* ─────────────────────────────────────────────────────────────
       일반 사용자 인증 확인
    ──────────────────────────────────────────────────────────────── */
    async function checkUserAuth() {
        let name = readCache();

        if (name === null) {
            try {
                const res = await fetch('/api/users/me', { credentials: 'include' });

                if (res.ok) {
                    const json = await res.json().catch(() => ({}));
                    name = json.data?.name ?? json.name ?? '회원';
                    writeCache(name);
                } else if (res.status === 401) {
                    // 자동 토큰 재발급 시도
                    const rr = await fetch('/api/auth/refresh', {
                        method: 'POST', credentials: 'include',
                    });
                    if (rr.ok) {
                        writeCache(CACHE_KEY_LOGIN_AT, String(Date.now()));
                        const rr2 = await fetch('/api/users/me', { credentials: 'include' });
                        if (rr2.ok) {
                            const json2 = await rr2.json().catch(() => ({}));
                            name = json2.data?.name ?? json2.name ?? '회원';
                            writeCache(name);
                        }
                    }
                } else {
                    clearCache();
                }
            } catch {
                clearCache();
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

    /* ─────────────────────────────────────────────────────────────
       관리자 인증 확인
    ──────────────────────────────────────────────────────────────── */
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
       헤더 내비 렌더링 (일반 사용자)
    ──────────────────────────────────────────────────────────────── */
    function renderNav(loggedIn, name) {
        const nav = document.getElementById('headerNav');
        if (!nav) return;

        if (loggedIn) {
            nav.innerHTML = `
        <span class="header-nav__username">${esc(name)}님</span>
        <a href="/mypage">마이페이지</a>
        <span class="header-nav__timer" id="hdrTokenTimer" title="Access Token 남은 시간">--:--:--</span>
        <button class="header-nav__btn" id="hdrRefresh">토큰 재발급</button>
        <button class="header-nav__btn" id="hdrLogout">로그아웃</button>`;

            document.getElementById('hdrRefresh').addEventListener('click', manualRefresh);
            document.getElementById('hdrLogout').addEventListener('click', logout);
            startTokenTimer();
        } else {
            nav.innerHTML = `
        <a href="/login" class="nav-login">로그인</a>
        <a href="/signup" class="nav-signup">회원가입</a>`;
        }

        markActiveLink(nav);
    }

    /* ─────────────────────────────────────────────────────────────
       관리자 헤더 + 서브 네비 렌더링
       [변경] 헤더 nav 렌더 후 site-header 바로 아래에 서브 네비 주입
    ──────────────────────────────────────────────────────────────── */
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

        /* ── 서브 네비 주입 ── */
        injectAdminSubNav();
    }

    /* ─────────────────────────────────────────────────────────────
       관리자 서브 네비 주입
       기존에 이미 #admin-sub-nav 가 있으면 덮어쓰지 않음 (중복 방지)
    ──────────────────────────────────────────────────────────────── */
    /* ─────────────────────────────────────────────────────────────
       서브 네비를 표시하지 않는 페이지
       자체 사이드바/네비가 있어 상단 서브 네비가 불필요한 페이지
    ──────────────────────────────────────────────────────────────── */
    const ADMIN_SUB_NAV_EXCLUDE = [
        '/admin/cardManage.html',  // 좌측 사이드바 자체 보유
        '/admin/cards',            // CleanUrl 매핑
    ];

    function injectAdminSubNav() {
        /* 자체 사이드바가 있는 페이지는 상단 서브 네비 주입 안 함 */
        const currentPath = location.pathname.replace(/\/$/, '');
        const excluded = ADMIN_SUB_NAV_EXCLUDE.some(p =>
            currentPath === p || currentPath === p.replace(/\.html$/, ''));
        if (excluded) return;

        /* 이미 존재하면 active 클래스만 갱신 */
        let subNav = document.getElementById('admin-sub-nav');
        if (!subNav) {
            subNav = document.createElement('nav');
            subNav.id = 'admin-sub-nav';
            subNav.className = 'admin-sub-nav';
            subNav.setAttribute('aria-label', '관리자 메뉴');

            /* app-header div 바로 뒤에 삽입 */
            const appHeader = document.getElementById('app-header');
            if (appHeader && appHeader.nextSibling) {
                appHeader.parentNode.insertBefore(subNav, appHeader.nextSibling);
            } else if (appHeader) {
                appHeader.insertAdjacentElement('afterend', subNav);
            } else {
                document.body.prepend(subNav);
            }
        }

        subNav.innerHTML = ADMIN_NAV_ITEMS.map(item => {
            const itemPath  = item.href.replace(/\.html$/, '');
            const isActive  = currentPath === item.href
                           || currentPath === itemPath
                           || currentPath.startsWith(itemPath + '/');
            return `<a href="${item.href}"
                       class="admin-sub-nav__link${isActive ? ' admin-sub-nav__link--active' : ''}"
                       ${isActive ? 'aria-current="page"' : ''}>${item.label}</a>`;
        }).join('');
    }

    /* ─────────────────────────────────────────────────────────────
       푸터 nav 렌더링
    ──────────────────────────────────────────────────────────────── */
    function renderFooter(loggedIn) {
        const footerNav = document.querySelector('.site-footer__nav');
        if (!footerNav) return;

        if (loggedIn) {
            footerNav.innerHTML = `
        <a href="/">카드 홈</a>
        <a href="/mypage">마이페이지</a>`;
        } else {
            footerNav.innerHTML = `
        <a href="/">카드 홈</a>
        <a href="/login">로그인</a>
        <a href="/signup">회원가입</a>
        <a href="/mypage">마이페이지</a>`;
        }

        footerNav.style.visibility = '';
    }

    /* ─────────────────────────────────────────────────────────────
       markActiveLink
    ──────────────────────────────────────────────────────────────── */
    function markActiveLink(nav) {
        const currentPath = location.pathname;
        nav.querySelectorAll('a[href]').forEach(a => {
            const href = a.getAttribute('href');
            if (!href || href === '#') return;

            const isExact = currentPath === href;

            const hrefClean = href.replace(/\/index\.html$/, '') || '/';
            const pathClean = currentPath.replace(/\/index\.html$/, '') || '/';
            const isClean = hrefClean !== '/' && (pathClean === hrefClean || currentPath === hrefClean);

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

            const loginAt = Number(sessionStorage.getItem(CACHE_KEY_LOGIN_AT) ?? 0);
            if (!loginAt) { timerEl.textContent = '--:--:--'; return; }

            const elapsed   = Date.now() - loginAt;
            const remaining = ACCESS_TOKEN_TTL_MS - elapsed;

            if (remaining <= 0) {
                timerEl.textContent = '00:00:00';
                timerEl.style.color = '#ef4444';
                stopTokenTimer();
                return;
            }

            const h = Math.floor(remaining / 3_600_000);
            const m = Math.floor((remaining % 3_600_000) / 60_000);
            const s = Math.floor((remaining % 60_000) / 1_000);

            timerEl.textContent = `${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`;
            timerEl.style.color = remaining < WARN_THRESHOLD_MS ? '#f97316' : '';
        }

        tick();
        _tokenTimerInterval = setInterval(tick, 1000);
    }

    function stopTokenTimer() {
        if (_tokenTimerInterval) { clearInterval(_tokenTimerInterval); _tokenTimerInterval = null; }
    }

    /* ─────────────────────────────────────────────────────────────
       토큰 재발급 (수동)
    ──────────────────────────────────────────────────────────────── */
    async function manualRefresh() {
        const btn = document.getElementById('hdrRefresh');
        if (btn) { btn.disabled = true; btn.textContent = '재발급 중...'; }

        const refreshUrl  = IS_ADMIN ? '/api/admin/auth/refresh' : '/api/auth/refresh';
        const redirectUrl = IS_ADMIN ? ADMIN_LOGIN_URL : LOGIN_URL;

        const res = await BnkAPI.post(refreshUrl);

        if (btn) { btn.disabled = false; btn.textContent = '토큰 재발급'; }

        if (res.ok) {
            sessionStorage.setItem(CACHE_KEY_LOGIN_AT, String(Date.now()));
            startTokenTimer();
            BnkToast.success('토큰이 재발급되었습니다.');
        } else {
            const msg = BnkError.extract(res.data, '세션이 만료되었습니다. 다시 로그인해 주세요.');
            BnkToast.error(msg);
            clearCache();
            stopTokenTimer();
            setTimeout(() => location.replace(redirectUrl), 1500);
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
       XSS 방어
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
    hideFooerNav();
    injectHeader();
    checkAuth();

    window.showToast = (msg, type = 'info') => BnkToast[type]?.(msg) ?? BnkToast.info(msg);
})();