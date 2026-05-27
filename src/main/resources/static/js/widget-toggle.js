/**
 * widget-toggle.js  |  BNK 부산은행
 * ─────────────────────────────────────────────────────
 * ▸ 챗봇을 열면 → 카트 토글 버튼 / 카트 패널 숨김
 * ▸ 카트를 열면 → 챗봇 버튼 숨김
 * ▸ 둘 다 닫으면 → 모두 복원
 * ▸ 원본 main.js 의 toggleCart() 를 래핑해서 동작
 * ─────────────────────────────────────────────────────
 */
(function () {
  'use strict';

  const HIDDEN = 'fab--hidden';

  /* DOM 참조 — DOMContentLoaded 후 안전하게 수집 */
  function getEls() {
    return {
      chatbot:    document.getElementById('bnk-ai-chatbot'),
      cartToggle: document.querySelector('.cart-toggle'),
      cart:       document.getElementById('compare-cart'),
      chatToggle: document.querySelector('#bnk-ai-chatbot .chat-toggle'),
    };
  }

  /* 챗봇 열림 여부 */
  function isChatOpen() {
    return document.getElementById('bnk-ai-chatbot')?.classList.contains('open') ?? false;
  }

  /* 카트 열림 여부 */
  function isCartOpen() {
    return document.getElementById('compare-cart')?.classList.contains('open') ?? false;
  }

  /* 상태에 따라 FAB 표시/숨김 갱신 */
  function syncVisibility() {
    const { chatbot, cartToggle, cart } = getEls();
    const chatOpen = isChatOpen();
    const cartOpen = isCartOpen();

    /* 챗봇이 열려 있으면 카트 버튼·패널 숨김 */
    if (chatbot) {
      cartToggle?.classList.toggle(HIDDEN, chatOpen);
      cart?.classList.toggle(HIDDEN, chatOpen && !cartOpen);
      /* 카트가 열려 있었다면 함께 닫기 */
      if (chatOpen && cartOpen) {
        cart?.classList.remove('open');
        cartToggle?.classList.remove(HIDDEN);
      }
    }

    /* 카트가 열려 있으면 챗봇 버튼 숨김 */
    if (cartOpen && !chatOpen) {
      chatbot?.classList.add(HIDDEN);
    } else if (!chatOpen) {
      chatbot?.classList.remove(HIDDEN);
    }
  }

  /* ── 챗봇 토글 버튼 가로채기 ── */
  function patchChatbot() {
    const chatbot    = document.getElementById('bnk-ai-chatbot');
    const chatToggle = chatbot?.querySelector('.chat-toggle');
    const chatClose  = chatbot?.querySelector('.chat-close');
    if (!chatToggle) return;

    chatToggle.addEventListener('click', () => {
      /* 원본 chatbot.js 가 .open 클래스를 붙이기 직전이므로
         requestAnimationFrame으로 한 틱 뒤에 syncVisibility 호출 */
      requestAnimationFrame(syncVisibility);
    });

    chatClose?.addEventListener('click', () => {
      requestAnimationFrame(syncVisibility);
    });
  }

  /* ── 카트 토글 버튼 가로채기 ── */
  function patchCartToggle() {
    const cartToggle = document.querySelector('.cart-toggle');
    if (!cartToggle) return;

    /* onclick="toggleCart()" 이 인라인으로 걸려 있으므로
       capture 단계에서 syncVisibility 를 추가 실행 */
    cartToggle.addEventListener('click', () => {
      requestAnimationFrame(syncVisibility);
    }, true);
  }

  /* ── 초기화 ── */
  function init() {
    patchChatbot();
    patchCartToggle();

    /* 카트 내부 compare 버튼 클릭 후 카트가 닫힐 때도 복원 */
    document.addEventListener('click', (e) => {
      if (e.target.closest('.compare-modal-close') || e.target === document.getElementById('compare-modal-overlay')) {
        requestAnimationFrame(syncVisibility);
      }
    });

    /* ESC 키로 챗봇 닫힐 경우 복원 */
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') requestAnimationFrame(syncVisibility);
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
