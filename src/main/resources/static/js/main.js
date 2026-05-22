/* 로그인 상태면 hero 회원가입 버튼을 마이페이지로 교체 */
document.addEventListener('DOMContentLoaded', () => {
    const nav = document.getElementById('headerNav');
    if (nav) {
        const observer = new MutationObserver(() => {
            const isLoggedIn = !!nav.querySelector('.header-nav__username');
            const btn = document.getElementById('heroSignup');
            if (btn && isLoggedIn) {
                btn.textContent = '마이페이지';
                btn.href = '/mypage/index.html';
            }
        });
        observer.observe(nav, { childList: true, subtree: true });
    }
});