/**
 * survey-widget.js | 카드 소비 성향 설문 위젯
 * 로그인한 사용자에게 이번 달 설문 미완료 시 자동 표시
 * 위치: 오른쪽 카드 목록 옆 고정 패널
 */

(function () {

    // ── 질문 정의 ────────────────────────────────────────────────────
    const QUESTIONS = [
        {
            code: 'Q1',
            text: '주로 어디서 결제하시나요?',
            type: 'multi',
            options: [
                { label: '편의점',     value: 'CONVENIENCE' },
                { label: '마트/슈퍼',  value: 'MART' },
                { label: '온라인쇼핑', value: 'ONLINE' },
                { label: '외식',       value: 'RESTAURANT' },
                { label: '주유',       value: 'GAS' },
                { label: '여행',       value: 'TRAVEL' },
                { label: '구독서비스', value: 'SUBSCRIPTION' },
            ],
        },
        {
            code: 'Q2',
            text: '월 평균 카드 지출은?',
            type: 'single',
            options: [
                { label: '30만원 미만',   value: 'UNDER_30' },
                { label: '30 ~ 50만원',  value: '30_50' },
                { label: '50 ~ 100만원', value: '50_100' },
                { label: '100만원 이상',  value: 'OVER_100' },
            ],
        },
        {
            code: 'Q3',
            text: '가장 중요하게 생각하는 혜택은?',
            type: 'single',
            options: [
                { label: '할인',     value: 'DISCOUNT' },
                { label: '포인트',   value: 'POINT' },
                { label: '캐시백',   value: 'CASHBACK' },
                { label: '마일리지', value: 'MILEAGE' },
                { label: '무료혜택', value: 'FREE' },
            ],
        },
        {
            code: 'Q4',
            text: '연회비에 대한 생각은?',
            type: 'single',
            options: [
                { label: '혜택이 좋으면 낸다',   value: 'WORTH_IT' },
                { label: '최대한 낮을수록 좋다', value: 'LOW_FEE' },
                { label: '무료 카드만 쓴다',     value: 'FREE_ONLY' },
            ],
        },
        {
            code: 'Q5',
            text: '카드 선택 시 가장 중요한 기준은?',
            type: 'single',
            options: [
                { label: '혜택 종류',   value: 'BENEFIT_TYPE' },
                { label: '연회비',      value: 'ANNUAL_FEE' },
                { label: '카드사 신뢰', value: 'BRAND' },
                { label: '디자인',      value: 'DESIGN' },
                { label: '지인 추천',   value: 'REFERRAL' },
            ],
        },
    ];

    // ── 상태 ─────────────────────────────────────────────────────────
    let currentStep   = 0;
    let answers       = {};
    let multiSelected = {};

    // ── 초기화 ───────────────────────────────────────────────────────
    async function init() {
        const loginAt = sessionStorage.getItem('bnk_login_at');
        if (!loginAt) return;

        try {
            const res  = await fetch('/api/surveys/status', { credentials: 'include' });
            const json = await res.json();
            if (json?.data?.completed) return;
        } catch {
            return;
        }

        setTimeout(render, 3000);
    }

    // ── 스타일 주입 ──────────────────────────────────────────────────
    function injectStyle() {
        if (document.getElementById('survey-widget-style')) return;
        const style = document.createElement('style');
        style.id = 'survey-widget-style';
        style.textContent = `
            #survey-widget {
                position: fixed;
                top: 50%;
                left: 24px;
                transform: translateY(-50%);
                width: 280px;
                background: #fff;
                border-radius: 14px;
                box-shadow: 0 8px 32px rgba(0,103,127,.18);
                z-index: 900;
                font-family: 'Pretendard', sans-serif;
                border: 1px solid var(--teal-100);
                animation: sw-slideIn .3s ease;
            }
			@keyframes sw-slideIn {
			    from { opacity:0; transform:translateY(-50%) translateX(-20px); }
			    to   { opacity:1; transform:translateY(-50%) translateX(0); }
			}
            .sw-header {
                background: linear-gradient(135deg, var(--teal-900) 0%, var(--teal-600) 100%);
                padding: 12px 14px;
                border-radius: 14px 14px 0 0;
                display: flex;
                justify-content: space-between;
                align-items: center;
            }
            .sw-header-title {
                color: #fff;
                font-size: 12px;
                font-weight: 600;
                letter-spacing: -.2px;
            }
            .sw-header-step {
                color: rgba(255,255,255,.65);
                font-size: 11px;
            }
            .sw-close {
                background: none;
                border: none;
                color: rgba(255,255,255,.7);
                font-size: 16px;
                cursor: pointer;
                line-height: 1;
                padding: 0;
                margin-left: 8px;
            }
            .sw-close:hover { color: #fff; }
            .sw-body { padding: 14px; }
            .sw-progress {
                height: 3px;
                background: var(--teal-50);
                border-radius: 2px;
                margin-bottom: 12px;
            }
            .sw-progress-bar {
                height: 100%;
                background: var(--teal-600);
                border-radius: 2px;
                transition: width .3s ease;
            }
            .sw-question {
                font-size: 13px;
                font-weight: 600;
                color: var(--teal-800);
                margin-bottom: 10px;
                line-height: 1.5;
            }
            .sw-option {
                display: block;
                width: 100%;
                text-align: left;
                padding: 8px 10px;
                margin-bottom: 5px;
                border: 1px solid var(--teal-100);
                border-radius: 8px;
                background: #fff;
                cursor: pointer;
                font-size: 12px;
                color: #444;
                transition: all .15s;
                box-sizing: border-box;
            }
            .sw-option:hover {
                border-color: var(--teal-400);
                color: var(--teal-800);
                background: var(--teal-50);
            }
            .sw-option.active {
                border-color: var(--teal-600);
                background: var(--teal-50);
                color: var(--teal-800);
                font-weight: 600;
            }
            .sw-btn-row {
                display: flex;
                gap: 6px;
                margin-top: 10px;
            }
            .sw-btn-prev {
                flex: 1;
                padding: 8px;
                background: var(--teal-50);
                color: var(--teal-800);
                border: 1px solid var(--teal-100);
                border-radius: 8px;
                font-size: 12px;
                cursor: pointer;
            }
            .sw-btn-prev:hover { background: var(--teal-100); }
            .sw-btn-next {
                flex: 2;
                padding: 8px;
                background: var(--teal-600);
                color: #fff;
                border: none;
                border-radius: 8px;
                font-size: 12px;
                font-weight: 600;
                cursor: pointer;
            }
            .sw-btn-next:hover { background: var(--teal-800); }
            .sw-hint {
                font-size: 10px;
                color: #aaa;
                text-align: center;
                margin-top: 8px;
            }
            .sw-textarea {
                width: 100%;
                height: 80px;
                padding: 8px 10px;
                border: 1px solid var(--teal-100);
                border-radius: 8px;
                font-size: 12px;
                resize: none;
                outline: none;
                font-family: 'Pretendard', sans-serif;
                box-sizing: border-box;
                color: #333;
            }
            .sw-textarea:focus { border-color: var(--teal-400); }
            .sw-char-count {
                font-size: 10px;
                color: #aaa;
                text-align: right;
                margin-top: 2px;
            }
            .sw-complete {
                padding: 24px 16px;
                text-align: center;
            }
            .sw-complete-icon {
                width: 48px;
                height: 48px;
                background: var(--teal-50);
                border-radius: 50%;
                display: flex;
                align-items: center;
                justify-content: center;
                margin: 0 auto 12px;
            }
            .sw-complete-title {
                font-size: 15px;
                font-weight: 700;
                color: var(--teal-800);
                margin-bottom: 6px;
            }
            .sw-complete-desc {
                font-size: 12px;
                color: #888;
                line-height: 1.6;
                margin-bottom: 16px;
            }
            .sw-btn-close-final {
                padding: 9px 20px;
                background: var(--teal-600);
                color: #fff;
                border: none;
                border-radius: 8px;
                font-size: 13px;
                font-weight: 600;
                cursor: pointer;
            }
            .sw-btn-close-final:hover { background: var(--teal-800); }
        `;
        document.head.appendChild(style);
    }

    // ── 위젯 렌더링 ──────────────────────────────────────────────────
    function render() {
        if (document.getElementById('survey-widget')) return;
        injectStyle();

        const widget = document.createElement('div');
        widget.id = 'survey-widget';
        document.body.appendChild(widget);
        renderStep();
    }

    // ── 단계 렌더링 ──────────────────────────────────────────────────
    function renderStep() {
        const widget = document.getElementById('survey-widget');
        if (!widget) return;

        const totalSteps = QUESTIONS.length + 1;
        const progress   = Math.round((currentStep / totalSteps) * 100);

        if (currentStep < QUESTIONS.length) {
            const q = QUESTIONS[currentStep];
            widget.innerHTML = `
                <div class="sw-header">
                    <div>
                        <div class="sw-header-title">소비 성향 설문</div>
                        <div class="sw-header-step">${currentStep + 1} / ${totalSteps}</div>
                    </div>
                    <button class="sw-close" onclick="document.getElementById('survey-widget').remove()">×</button>
                </div>
                <div class="sw-body">
                    <div class="sw-progress">
                        <div class="sw-progress-bar" style="width:${progress}%"></div>
                    </div>
                    <div class="sw-question">${q.text}</div>
                    <div id="sw-options">
                        ${q.options.map(opt => `
                            <button class="sw-option ${isSelected(q.code, opt.value) ? 'active' : ''}"
                                    onclick="swSelect('${q.code}','${opt.value}','${q.type}',this)">
                                ${opt.label}
                            </button>
                        `).join('')}
                    </div>
                    <div class="sw-btn-row">
                        ${currentStep > 0
                            ? `<button class="sw-btn-prev" onclick="swPrev()">이전</button>`
                            : ''}
                        <button class="sw-btn-next" onclick="swNext()">
                            ${currentStep < QUESTIONS.length - 1 ? '다음' : '마지막 단계'}
                        </button>
                    </div>
                    <div class="sw-hint">월 1회 참여 가능합니다.</div>
                </div>
            `;
        } else {
            // 자유 의견 단계
            widget.innerHTML = `
                <div class="sw-header">
                    <div>
                        <div class="sw-header-title">소비 성향 설문</div>
                        <div class="sw-header-step">${totalSteps} / ${totalSteps}</div>
                    </div>
                    <button class="sw-close" onclick="document.getElementById('survey-widget').remove()">×</button>
                </div>
                <div class="sw-body">
                    <div class="sw-progress">
                        <div class="sw-progress-bar" style="width:95%"></div>
                    </div>
                    <div class="sw-question">카드 서비스에 대한 의견을 자유롭게 남겨주세요.</div>
                    <div style="font-size:11px;color:#999;margin-bottom:8px;">선택 사항입니다.</div>
                    <textarea id="sw-comment" class="sw-textarea"
                              placeholder="예: 주유 혜택이 더 다양했으면 좋겠어요."
                              maxlength="500"></textarea>
                    <div class="sw-char-count" id="sw-char-count">0 / 500</div>
                    <div class="sw-btn-row">
                        <button class="sw-btn-prev" onclick="swPrev()">이전</button>
                        <button class="sw-btn-next" id="sw-submit-btn" onclick="swSubmit()">제출하기</button>
                    </div>
                </div>
            `;
            document.getElementById('sw-comment')
                    .addEventListener('input', function () {
                document.getElementById('sw-char-count').textContent =
                    `${this.value.length} / 500`;
            });
        }
    }

    // ── 선택 처리 ────────────────────────────────────────────────────
    window.swSelect = function (code, value, type, btn) {
        if (type === 'single') {
            answers[code] = value;
            document.querySelectorAll('.sw-option').forEach(b =>
                b.classList.remove('active'));
            btn.classList.add('active');
        } else {
            if (!multiSelected[code]) multiSelected[code] = new Set();
            if (multiSelected[code].has(value)) {
                multiSelected[code].delete(value);
                btn.classList.remove('active');
            } else {
                multiSelected[code].add(value);
                btn.classList.add('active');
            }
            answers[code] = [...multiSelected[code]].join(',');
        }
    };

    function isSelected(code, value) {
        if (!answers[code]) return false;
        return answers[code].split(',').includes(value);
    }

    window.swNext = function () {
        const q = QUESTIONS[currentStep];
        if (!answers[q.code] || answers[q.code] === '') {
            alert('항목을 선택해 주세요.');
            return;
        }
        currentStep++;
        renderStep();
    };

    window.swPrev = function () {
        if (currentStep > 0) { currentStep--; renderStep(); }
    };

    // ── 제출 ─────────────────────────────────────────────────────────
    window.swSubmit = async function () {
        const btn     = document.getElementById('sw-submit-btn');
        const comment = document.getElementById('sw-comment').value.trim();

        btn.disabled    = true;
        btn.textContent = '제출 중...';

        const answerList = Object.entries(answers).map(([code, val]) => ({
            questionCode: code,
            answerValue:  val,
        }));

        try {
            const res = await fetch('/api/surveys', {
                method:      'POST',
                credentials: 'include',
                headers:     { 'Content-Type': 'application/json' },
                body:        JSON.stringify({
                    answers:     answerList,
                    commentText: comment || null,
                }),
            });

            const json = await res.json();
            if (!res.ok) {
                alert(json?.message ?? '제출에 실패했습니다.');
                btn.disabled    = false;
                btn.textContent = '제출하기';
                return;
            }
            renderComplete();

        } catch {
            alert('서버 오류가 발생했습니다.');
            btn.disabled    = false;
            btn.textContent = '제출하기';
        }
    };

    // ── 완료 화면 ────────────────────────────────────────────────────
    function renderComplete() {
        const widget = document.getElementById('survey-widget');
        if (!widget) return;

        widget.innerHTML = `
            <div class="sw-header" style="border-radius:14px 14px 0 0;">
                <div class="sw-header-title">소비 성향 설문</div>
                <button class="sw-close"
                        onclick="document.getElementById('survey-widget').remove()">×</button>
            </div>
            <div class="sw-complete">
                <div class="sw-complete-icon">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none"
                         stroke="var(--teal-600)" stroke-width="2.5"
                         stroke-linecap="round" stroke-linejoin="round">
                        <polyline points="20 6 9 17 4 12"/>
                    </svg>
                </div>
                <div class="sw-complete-title">설문 완료</div>
                <div class="sw-complete-desc">
                    소중한 의견 감사합니다.<br>더 나은 카드 서비스를 위해 활용하겠습니다.
                </div>
                <button class="sw-btn-close-final"
                        onclick="document.getElementById('survey-widget').remove()">
                    닫기
                </button>
            </div>
        `;

        setTimeout(() => {
            const w = document.getElementById('survey-widget');
            if (w) w.remove();
        }, 3000);
    }

    // ── 실행 ─────────────────────────────────────────────────────────
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();