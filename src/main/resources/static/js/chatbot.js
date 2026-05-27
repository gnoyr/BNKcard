(function () {
  const widget = document.getElementById("bnk-ai-chatbot");
  if (!widget) return;

  // data-* 속성에서 동적으로 API URL 및 봇 이미지 로드
  const apiUrl    = widget.dataset.apiUrl   || "/api/chat";
  const botImage  = widget.dataset.botImage || "/images/ai-chatbot.png";

  const toggleButton   = widget.querySelector(".chat-toggle");
  const closeButton    = widget.querySelector(".chat-close");
  const botImageElement= widget.querySelector(".chat-toggle img");
  const form           = widget.querySelector(".chat-input-area");
  const input          = widget.querySelector(".chat-input");
  const sendButton     = widget.querySelector(".chat-send");
  const messages       = widget.querySelector(".chat-messages");

  // 챗봇 토글 버튼 이미지 세팅
  botImageElement.src = botImage;

  const sessionKey = "bnk_ai_chat_session_id";

  // 세션 ID가 없으면 새로 생성하여 유지
  function getSessionId() {
    let sessionId = sessionStorage.getItem(sessionKey);

    if (!sessionId) {
      sessionId = "web-" + Date.now() + "-" + Math.random().toString(36).slice(2);
      sessionStorage.setItem(sessionKey, sessionId);
    }

    return sessionId;
  }

  // 채팅창에 메시지 추가 및 자동 스크롤
  function appendMessage(type, text) {
    const message = document.createElement("div");
    message.className = "message " + type;
    message.textContent = text;
    messages.appendChild(message);
    messages.scrollTop = messages.scrollHeight;
  }

  // 다양한 포맷의 백엔드 JSON 응답 구조를 안전하게 처리
  function extractResponseText(json) {
    if (!json) return "응답이 비어 있습니다.";
    if (json.response)                  return json.response;
    if (json.data && json.data.response)    return json.data.response;
    if (json.result && json.result.response) return json.result.response;
    if (json.body && json.body.response)    return json.body.response;
    return "응답 형식을 확인해 주세요.";
  }

  // Spring Security 등의 CSRF 보호 태그 감지 및 헤더 반환
  function getCsrfHeaders() {
    const token  = document.querySelector('meta[name="_csrf"]')?.content;
    const header = document.querySelector('meta[name="_csrf_header"]')?.content;

    if (token && header) {
      return { [header]: token };
    }

    return {};
  }

  // 메시지 전송 및 API 연동
  async function sendMessage(userInput) {
    appendMessage("user", userInput);

    input.value = "";
    input.disabled = true;
    sendButton.disabled = true;

    // 로딩 상태 표시
    const loadingMessage = document.createElement("div");
    loadingMessage.className = "message bot";
    loadingMessage.textContent = "답변을 생성하고 있습니다...";
    messages.appendChild(loadingMessage);
    messages.scrollTop = messages.scrollHeight;

    try {
      const response = await fetch(apiUrl, {
        method: "POST",
        credentials: "same-origin",
        headers: {
          "Content-Type": "application/json",
          ...getCsrfHeaders()
        },
        body: JSON.stringify({
          sessionId: getSessionId(),
          userInput: userInput
        })
      });

      if (!response.ok) {
        throw new Error("HTTP " + response.status);
      }

      const json = await response.json();
      loadingMessage.textContent = extractResponseText(json);

    } catch (error) {
      console.error(error);
      loadingMessage.textContent = "죄송합니다. 채팅 요청 중 오류가 발생했습니다.";

    } finally {
      input.disabled = false;
      sendButton.disabled = false;
      input.focus();
    }
  }

  // 이벤트 리스너: 챗봇 창 열기/닫기 토글
  toggleButton.addEventListener("click", function () {
    widget.classList.toggle("open");

    if (widget.classList.contains("open")) {
      input.focus();
    }
  });

  // 이벤트 리스너: 챗봇 창 닫기
  closeButton.addEventListener("click", function () {
    widget.classList.remove("open");
  });

  // 이벤트 리스너: 폼 제출 시 메시지 전송
  form.addEventListener("submit", function (event) {
    event.preventDefault();

    const userInput = input.value.trim();
    if (!userInput) return;

    sendMessage(userInput);
  });

})();