/**
 * chatbot.js  |  BNK 부산은행 AI 챗봇 위젯
 * ─────────────────────────────────────────────────────────────────
 */
(function () {
  'use strict';

  // ── 위젯 루트 ──────────────────────────────────────────────────
  const widget = document.getElementById("bnk-ai-chatbot");
  if (!widget) return;

  // ── data-* 속성에서 API URL · 봇 이미지 경로 로드 ─────────────
  const apiUrl   = widget.dataset.apiUrl   || "/api/chat";
  const botImage = widget.dataset.botImage || "/images/ai-chatbot.png";

  // ── DOM 요소 참조 ──────────────────────────────────────────────
  const toggleButton    = widget.querySelector(".chat-toggle");
  const closeButton     = widget.querySelector(".chat-close");
  const botImageElement = widget.querySelector(".chat-toggle img"); // null 가능
  const inputArea       = widget.querySelector(".chat-input-area"); // <div> — submit 이벤트 없음
  const input           = widget.querySelector(".chat-input");
  const sendButton      = widget.querySelector(".chat-send");
  const messages        = widget.querySelector(".chat-messages");

  // 필수 요소 누락 시 조기 반환 ────────────────────────
  if (!toggleButton || !closeButton || !inputArea || !input || !sendButton || !messages) {
    console.warn("[BNK Chatbot] 필수 DOM 요소를 찾을 수 없습니다. 챗봇 초기화를 중단합니다.");
    return;
  }

  // 이미지 null guard + onerror 폴백 ───────────────────
  if (botImageElement) {
    botImageElement.src = botImage;
    botImageElement.onerror = function () {
      // 이미지 로드 실패 시 alt 텍스트 표시 (버튼 기능은 정상 유지)
      this.style.display = "none";
      console.warn("[BNK Chatbot] 챗봇 이미지를 불러올 수 없습니다:", botImage);
    };
  }

  // ── 세션 ID 관리 ───────────────────────────────────────────────
  const SESSION_KEY = "bnk_ai_chat_session_id";

  function getSessionId() {
    let sessionId = sessionStorage.getItem(SESSION_KEY);
    if (!sessionId) {
      sessionId = "web-" + Date.now() + "-" + Math.random().toString(36).slice(2);
      sessionStorage.setItem(SESSION_KEY, sessionId);
    }
    return sessionId;
  }

  // ── 채팅창 메시지 추가 + 자동 스크롤 ──────────────────────────
  function appendMessage(type, text) {
    const div = document.createElement("div");
    div.className = "message " + type;
    div.textContent = text;
    messages.appendChild(div);
    messages.scrollTop = messages.scrollHeight;
    return div;
  }

  // ── 다양한 백엔드 JSON 응답 구조 정규화 ────────────────────────
  function extractResponseText(json) {
    if (!json)                                return "응답이 비어 있습니다.";
    if (json.response)                        return json.response;
    if (json.data?.response)                  return json.data.response;
    if (json.result?.response)                return json.result.response;
    if (json.body?.response)                  return json.body.response;
    return "응답 형식을 확인해 주세요.";
  }

  // ── CSRF 헤더 (Spring Security meta 태그 감지) ─────────────────
  function getCsrfHeaders() {
    const token  = document.querySelector('meta[name="_csrf"]')?.content;
    const header = document.querySelector('meta[name="_csrf_header"]')?.content;
    return (token && header) ? { [header]: token } : {};
  }

  // ── 입력 잠금 토글 ─────────────────────────────────────────────
  function setInputLocked(locked) {
    input.disabled      = locked;
    sendButton.disabled = locked;
  }

  // ── 메시지 전송 + API 호출 ─────────────────────────────────────
  async function sendMessage(userInput) {
    const trimmed = userInput.trim();
    if (!trimmed) return;

    appendMessage("user", trimmed);
    input.value = "";
    setInputLocked(true);

    // 로딩 버블 (서버 응답 수신 전 자리 표시자)
    const loadingEl = appendMessage("bot", "답변을 생성하고 있습니다...");

    try {
      const response = await fetch(apiUrl, {
        method:      "POST",
        credentials: "omit",
        headers: {
          "Content-Type": "application/json",
          ...getCsrfHeaders()
        },
        body: JSON.stringify({
          sessionId: getSessionId(),
          userInput: trimmed
        })
      });

      if (!response.ok) {
        throw new Error("HTTP " + response.status);
      }

      const json = await response.json();
      loadingEl.textContent = extractResponseText(json);

    } catch (err) {
      console.error("[BNK Chatbot] API 오류:", err);
      loadingEl.textContent = "죄송합니다. 채팅 요청 중 오류가 발생했습니다.";
    } finally {
      setInputLocked(false);
      input.focus();
    }
  }
  
  function showWelcomeMessage() {
      const welcomeEl = appendMessage("bot",
        "안녕하세요! BNK 카드 전속 AI 상담원입니다.\n무엇을 도와드릴까요?\nBNK 카드에 대해 궁금한 점이 있으시면 언제든지 물어봐 주세요."
      );
      welcomeEl.style.whiteSpace = "pre-line"; // \n 줄바꿈 적용
    }

    // 챗봇 열릴 때 최초 1회만 실행
    let welcomed = false;
    toggleButton.addEventListener("click", function () {
      widget.classList.toggle("open");
      if (widget.classList.contains("open")) {
        if (!welcomed) {
          showWelcomeMessage();
          welcomed = true;
        }
        input.focus();
      }
    });

  // 전송 버튼 click 이벤트 ──────────────────────────
  // sendButton click 직접 바인딩
  sendButton.addEventListener("click", function () {
    sendMessage(input.value);
  });

  // Enter 키 전송 (Shift+Enter는 허용) ───────────────
  input.addEventListener("keydown", function (e) {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      sendMessage(input.value);
    }
  });

  // ── 챗봇 닫기 ─────────────────────────────────────────────────
  closeButton.addEventListener("click", function () {
    widget.classList.remove("open");
  });

})();