/**
 * survey-widget.js | 카드 소비 성향 설문 위젯
 * 로그인한 사용자에게 이번 달 설문 미완료 시 자동 표시
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
                { label: '30만원 미만',    value: 'UNDER_30' },
                { label: '30 ~ 50만원',   value: '30_50' },
                { label: '50 ~ 100만원',  value: '50_100' },
                { label: '100만원 이상',   value: 'OVER_100' },
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
                { label: '혜택이 좋으면 낸다',    value: 'WORTH_IT' },
                { label: '최대한 낮을수록 좋다',  value: 'LOW_FEE' },
                { label: '무료 카드만 쓴다',      value: 'FREE_ONLY' },
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
    let currentStep  = 0;
    let answers      = {};   // { Q1: 'val', Q2: 'val', ... }
    let multiSelected = {};  // 복수 선택용 { Q1: Set }

    // ── 초기화 ───────────────────────────────────────────────────────
    async function init() {
        // 비로그인이면 위젯 표시 안 함
        const loginAt = sessionStorage.getItem('bnk_login_at');
        if (!loginAt) return;

        // 이번 달 완료 여부 확인
        try {
            const res  = await fetch('/api/surveys/status', { credentials: 'include' });
            const json = await res.json();
            if (json?.data?.completed) return; // 이미 완료
        } catch {
            return;
        }

        // 3초 후 위젯 표시
        setTimeout(render, 3000);
    }

    // ── 위젯 렌더링 ──────────────────────────────────────────────────
    function render() {
        if (document.getElementById('survey-widget')) return;

        const widget = document.createElement('div');
        widget.id = 'survey-widget';
        widget.style.cssText = `
            position: fixed;
            bottom: 24px;
            right: 24px;
            width: 320px;
            background: #fff;
            border-radius: 14px;
            box-shadow: 0 8px 32px rgba(0,0,0,.15);
            z-index: 9999;
            font-family: 'Pretendard', sans-serif;
            overflow: hidden;
            animation: slideUp .3s ease;
        `;

        // 애니메이션 스타일
        if (!document.getElementById('survey-style')) {
            const style = document.createElement('style');
            style.id = 'survey-style';
            style.textContent = `
                @keyframes slideUp {
                    from { transform: translateY(20px); opacity: 0; }
                    to   { transform: translateY(0);    opacity: 1; }
                }
                .sw-option {
                    display: block; width: 100%; text-align: left;
                    padding: 9px 12px; margin-bottom: 6px;
                    border: 1px solid #e0e4ef; border-radius: 8px;
                    background: #fff; cursor: pointer; font-size: 13px;
                    color: #333; transition: all .15s;
                }
                .sw-option:hover  { border-color: #003087; color: #003087; }
                .sw-option.active { border-color: #003087; background: #f0f4fb; color: #003087; font-weight: 600; }
                .sw-progress {
                    height: 3px; background: #e8eaf0;
                    border-radius: 2px; margin-bottom: 16px;
                }
                .sw-progress-bar {
                    height: 100%; background: #003087;
                    border-radius: 2px; transition: width .3s;
                }
            `;
            document.head.appendChild(style);
        }

        document.body.appendChild(widget);
        renderStep();
    }

    // ── 단계 렌더링 ──────────────────────────────────────────────────
    function renderStep() {
        const widget = document.getElementById('survey-widget');
        if (!widget) return;

        const totalSteps = QUESTIONS.length + 1; // 질문 + 자유 의견
        const progress   = Math.round((currentStep / totalSteps) * 100);

        if (currentStep < QUESTIONS.length) {
            const q = QUESTIONS[currentStep];
            widget.innerHTML = `
                <div style="background:#003087; padding:14px 16px;
                            display:flex; justify-content:space-between; align-items:center;">
                    <span style="color:#fff; font-size:13px; font-weight:600;">
                        소비 성향 설문 (${currentStep + 1}/${QUESTIONS.length + 1})
                    </span>
                    <button onclick="document.getElementById('survey-widget').remove()"
                            style="background:none; border:none; color:rgba(255,255,255,.7);
                                   font-size:18px; cursor:pointer; line-height:1;">×</button>
                </div>
                <div style="padding:16px;">
                    <div class="sw-progress">
                        <div class="sw-progress-bar" style="width:${progress}%"></div>
                    </div>
                    <p style="font-size:14px; font-weight:600; color:#1a2340; margin-bottom:14px;">
                        ${q.text}
                    </p>
                    <div id="sw-options">
                        ${q.options.map(opt => `
                            <button class="sw-option ${isSelected(q.code, opt.value) ? 'active' : ''}"
                                    onclick="swSelect('${q.code}', '${opt.value}', '${q.type}', this)">
                                ${opt.label}
                            </button>
                        `).join('')}
                    </div>
                    <div style="display:flex; gap:8px; margin-top:14px;">
                        ${currentStep > 0 ? `
                            <button onclick="swPrev()"
                                    style="flex:1; padding:10px; background:#f0f2f6;
                                           color:#333; border:none; border-radius:8px;
                                           font-size:13px; cursor:pointer;">
                                이전
                            </button>` : ''}
                        <button onclick="swNext()"
                                style="flex:2; padding:10px; background:#003087;
                                       color:#fff; border:none; border-radius:8px;
                                       font-size:14px; font-weight:600; cursor:pointer;">
                            ${currentStep < QUESTIONS.length - 1 ? '다음' : '마지막 단계'}
                        </button>
                    </div>
                    <p style="font-size:11px; color:#aaa; text-align:center; margin-top:10px;">
                        월 1회 참여 가능합니다.
                    </p>
                </div>
            `;

        } else {
            // 자유 의견 단계
            widget.innerHTML = `
                <div style="background:#003087; padding:14px 16px;
                            display:flex; justify-content:space-between; align-items:center;">
                    <span style="color:#fff; font-size:13px; font-weight:600;">
                        소비 성향 설문 (${QUESTIONS.length + 1}/${QUESTIONS.length + 1})
                    </span>
                    <button onclick="document.getElementById('survey-widget').remove()"
                            style="background:none; border:none; color:rgba(255,255,255,.7);
                                   font-size:18px; cursor:pointer; line-height:1;">×</button>
                </div>
                <div style="padding:16px;">
                    <div class="sw-progress">
                        <div class="sw-progress-bar" style="width:95%"></div>
                    </div>
                    <p style="font-size:14px; font-weight:600; color:#1a2340; margin-bottom:6px;">
                        카드 서비스에 대한 의견을 자유롭게 남겨주세요.
                    </p>
                    <p style="font-size:12px; color:#888; margin-bottom:12px;">선택 사항입니다.</p>
                    <textarea id="sw-comment"
                              placeholder="예: 주유 혜택이 많은 카드가 더 다양했으면 좋겠어요."
                              maxlength="500"
                              style="width:100%; height:90px; padding:10px; border:1px solid #dde3ef;
                                     border-radius:8px; font-size:13px; resize:none;
                                     box-sizing:border-box; outline:none;
                                     font-family:'Pretendard',sans-serif;"></textarea>
                    <p id="sw-char-count"
                       style="font-size:11px; color:#aaa; text-align:right; margin-top:2px;">
                        0 / 500
                    </p>
                    <div style="display:flex; gap:8px; margin-top:10px;">
                        <button onclick="swPrev()"
                                style="flex:1; padding:10px; background:#f0f2f6;
                                       color:#333; border:none; border-radius:8px;
                                       font-size:13px; cursor:pointer;">
                            이전
                        </button>
                        <button onclick="swSubmit()"
                                id="sw-submit-btn"
                                style="flex:2; padding:10px; background:#003087;
                                       color:#fff; border:none; border-radius:8px;
                                       font-size:14px; font-weight:600; cursor:pointer;">
                            제출하기
                        </button>
                    </div>
                </div>
            `;

            // 글자 수 카운터
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
            // 복수 선택
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

    // ── 이전 / 다음 ──────────────────────────────────────────────────
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
        if (currentStep > 0) {
            currentStep--;
            renderStep();
        }
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

            // 완료 화면
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
            <div style="padding:28px 20px; text-align:center;">
                <div style="width:56px; height:56px; background:#e8f5e9;
                            border-radius:50%; display:flex; align-items:center;
                            justify-content:center; margin:0 auto 16px;">
                    <svg width="28" height="28" viewBox="0 0 24 24" fill="none"
                         stroke="#2e7d32" stroke-width="2.5"
                         stroke-linecap="round" stroke-linejoin="round">
                        <polyline points="20 6 9 17 4 12"/>
                    </svg>
                </div>
                <p style="font-size:16px; font-weight:700; color:#1a2340; margin-bottom:6px;">
                    설문 완료
                </p>
                <p style="font-size:13px; color:#666; margin-bottom:20px; line-height:1.6;">
                    소중한 의견 감사합니다.<br>더 나은 카드 서비스를 위해 활용하겠습니다.
                </p>
                <button onclick="document.getElementById('survey-widget').remove()"
                        style="padding:10px 24px; background:#003087; color:#fff;
                               border:none; border-radius:8px; font-size:14px;
                               font-weight:600; cursor:pointer;">
                    닫기
                </button>
            </div>
        `;

        // 3초 후 자동 닫기
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