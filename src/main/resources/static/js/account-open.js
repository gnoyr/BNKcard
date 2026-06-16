/**
 * account-open.js | 계좌 개설 프로세스
 */

// ── 상태 ──────────────────────────────────────────────────────────
const state = {
    currentStep: 1,
    agreedTerms: [],   // 동의한 약관 ID 목록
    accountType: '',
    accountAlias: '',
    password: '',
};

// ── 단계 전환 ─────────────────────────────────────────────────────
function goStep(step) {
    [1, 2, 3, 4].forEach(n => {
        document.getElementById(`step-${n}`).style.display = n === step ? '' : 'none';
        const si = document.getElementById(`si-${n}`);
        si.style.fontWeight = n === step ? 'bold' : 'normal';
        si.style.color      = n === step ? '#003087' : n < step ? '#28a745' : '#ccc';
    });
    state.currentStep = step;
}

// ── 1단계: 약관 동의 ──────────────────────────────────────────────
async function initStep1() {
    const container = document.getElementById('step-1');
    container.innerHTML = `<p style="color:#999;">약관을 불러오는 중...</p>`;

    // ACCOUNT_OPEN 패키지 약관 조회
    const res = await fetch('/api/terms/packages/ACCOUNT_OPEN', {
        credentials: 'include'
    });
    const json = await res.json();
    const terms = json?.data?.terms ?? [];

    if (!terms.length) {
        container.innerHTML = `<p style="color:red;">약관을 불러오지 못했습니다.</p>`;
        return;
    }

    container.innerHTML = `
        <h2 style="font-size:20px; margin-bottom:8px;">약관 동의</h2>
        <p style="color:#666; font-size:14px; margin-bottom:24px;">
            계좌 개설에 필요한 약관에 동의해 주세요.
        </p>

        <!-- 전체 동의 -->
        <label style="display:flex; align-items:center; gap:10px;
                      padding:14px; background:#f0f4fb; border-radius:8px;
                      cursor:pointer; margin-bottom:16px;">
            <input type="checkbox" id="chk-all" onchange="toggleAll(this.checked)"
                   style="width:18px; height:18px;">
            <span style="font-weight:600; font-size:15px;">전체 동의</span>
        </label>

        <!-- 개별 약관 목록 -->
        <div id="terms-list">
            ${terms.map(t => `
                <div style="display:flex; align-items:center; justify-content:space-between;
                            padding:12px 0; border-bottom:1px solid #eee;">
                    <label style="display:flex; align-items:center; gap:10px; cursor:pointer;">
                        <input type="checkbox" class="chk-term"
                               data-terms-id="${t.termsId}"
                               data-required="${t.requiredYn}"
                               onchange="onTermChange()"
                               style="width:16px; height:16px;">
                        <span style="font-size:14px;">
                            ${t.requiredYn === 'Y'
                                ? '<span style="color:#e65100;">[필수]</span>'
                                : '<span style="color:#888;">[선택]</span>'}
                            ${t.title}
                        </span>
                    </label>
                    <a href="/terms/view.html?termsId=${t.termsId}" target="_blank"
                       style="font-size:12px; color:#003087; white-space:nowrap;">
                        보기 &gt;
                    </a>
                </div>
            `).join('')}
        </div>

        <button onclick="submitStep1()"
                style="width:100%; margin-top:24px; padding:14px;
                       background:#003087; color:#fff; border:none;
                       border-radius:8px; font-size:16px; font-weight:600;
                       cursor:pointer;">
            다음
        </button>
    `;
}

function toggleAll(checked) {
    document.querySelectorAll('.chk-term').forEach(c => c.checked = checked);
}

function onTermChange() {
    const all   = document.querySelectorAll('.chk-term');
    const checked = document.querySelectorAll('.chk-term:checked');
    document.getElementById('chk-all').checked = all.length === checked.length;
}

function submitStep1() {
    const required = document.querySelectorAll('.chk-term[data-required="Y"]');
    for (const chk of required) {
        if (!chk.checked) {
            alert('필수 약관에 모두 동의해 주세요.');
            return;
        }
    }
    // 동의한 약관 ID 수집
    state.agreedTerms = [...document.querySelectorAll('.chk-term:checked')]
        .map(c => Number(c.dataset.termsId));

    goStep(2);
    initStep2();
}

// ── 초기화 ────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    initStep1();
});


// ── 2단계: 계좌 정보 입력 ─────────────────────────────────────────
function initStep2() {
    const container = document.getElementById('step-2');
    container.innerHTML = `
        <h2 style="font-size:20px; margin-bottom:8px;">계좌 정보 입력</h2>
        <p style="color:#666; font-size:14px; margin-bottom:24px;">
            개설할 계좌 종류를 선택해 주세요.
        </p>

        <!-- 계좌 종류 선택 -->
        <div style="margin-bottom:24px;">
            <p style="font-weight:600; margin-bottom:12px;">
                계좌 종류 <span style="color:#e65100;">*</span>
            </p>
            <div style="display:flex; flex-direction:column; gap:10px;">
                ${[
                    { value: 'CHECKING', label: '입출금 통장',
                      desc: '자유롭게 입출금할 수 있는 기본 통장' },
                    { value: 'SAVINGS',  label: '적금',
                      desc: '매월 일정 금액을 저축하는 상품' },
                    { value: 'DEPOSIT',  label: '예금',
                      desc: '목돈을 맡기고 이자를 받는 상품' },
                ].map(t => `
                    <label style="display:flex; align-items:center; gap:14px;
                                  padding:16px; border:2px solid #eee;
                                  border-radius:10px; cursor:pointer;"
                           id="label-${t.value}"
                           onclick="selectAccountType('${t.value}')">
                        <input type="radio" name="accountType" value="${t.value}"
                               style="width:18px; height:18px;">
                        <div>
                            <div style="font-weight:600; font-size:15px;">
                                ${t.label}
                            </div>
                            <div style="font-size:13px; color:#888; margin-top:2px;">
                                ${t.desc}
                            </div>
                        </div>
                    </label>
                `).join('')}
            </div>
        </div>

        <!-- 계좌 별명 -->
        <div style="margin-bottom:24px;">
            <label style="font-weight:600; display:block; margin-bottom:8px;">
                계좌 별명 <span style="color:#888; font-weight:normal;">(선택)</span>
            </label>
            <input type="text" id="input-alias"
                   placeholder="예: 생활비 통장"
                   maxlength="20"
                   style="width:100%; padding:12px 14px; border:1px solid #ddd;
                          border-radius:8px; font-size:15px; box-sizing:border-box;">
            <p style="font-size:12px; color:#aaa; margin-top:4px;">
                최대 20자
            </p>
        </div>

        <div style="display:flex; gap:10px;">
            <button onclick="goStep(1); initStep1();"
                    style="flex:1; padding:14px; background:#f0f2f6; color:#333;
                           border:none; border-radius:8px; font-size:15px;
                           cursor:pointer;">
                이전
            </button>
            <button onclick="submitStep2()"
                    style="flex:2; padding:14px; background:#003087; color:#fff;
                           border:none; border-radius:8px; font-size:16px;
                           font-weight:600; cursor:pointer;">
                다음
            </button>
        </div>
    `;
}

function selectAccountType(value) {
    // 선택된 항목 스타일 강조
    ['CHECKING', 'SAVINGS', 'DEPOSIT'].forEach(v => {
        const label = document.getElementById(`label-${v}`);
        if (label) {
            label.style.borderColor = v === value ? '#003087' : '#eee';
            label.style.background  = v === value ? '#f0f4fb' : '#fff';
        }
    });
    // 라디오 체크
    const radio = document.querySelector(`input[name="accountType"][value="${value}"]`);
    if (radio) radio.checked = true;
}

function submitStep2() {
    const selected = document.querySelector('input[name="accountType"]:checked');
    if (!selected) {
        alert('계좌 종류를 선택해 주세요.');
        return;
    }
    state.accountType  = selected.value;
    state.accountAlias = document.getElementById('input-alias').value.trim();

    goStep(3);
    initStep3();
}

// ── 3단계: 비밀번호 설정 ──────────────────────────────────────────
function initStep3() {
    const container = document.getElementById('step-3');
    container.innerHTML = `
        <h2 style="font-size:20px; margin-bottom:8px;">출금 비밀번호 설정</h2>
        <p style="color:#666; font-size:14px; margin-bottom:24px;">
            출금 시 사용할 4~6자리 숫자 비밀번호를 설정해 주세요.
        </p>

        <!-- 비밀번호 입력 -->
        <div style="margin-bottom:20px;">
            <label style="font-weight:600; display:block; margin-bottom:8px;">
                비밀번호 <span style="color:#e65100;">*</span>
            </label>
            <input type="password" id="input-pw"
                   placeholder="4~6자리 숫자"
                   maxlength="6"
                   inputmode="numeric"
                   pattern="[0-9]*"
                   style="width:100%; padding:12px 14px; border:1px solid #ddd;
                          border-radius:8px; font-size:15px; box-sizing:border-box;
                          letter-spacing:6px;">
        </div>

        <!-- 비밀번호 확인 -->
        <div style="margin-bottom:8px;">
            <label style="font-weight:600; display:block; margin-bottom:8px;">
                비밀번호 확인 <span style="color:#e65100;">*</span>
            </label>
            <input type="password" id="input-pw-confirm"
                   placeholder="비밀번호 재입력"
                   maxlength="6"
                   inputmode="numeric"
                   pattern="[0-9]*"
                   style="width:100%; padding:12px 14px; border:1px solid #ddd;
                          border-radius:8px; font-size:15px; box-sizing:border-box;
                          letter-spacing:6px;">
        </div>

        <!-- 일치 여부 메시지 -->
        <p id="pw-match-msg" style="font-size:13px; height:18px; margin-bottom:20px;"></p>

        <!-- 주의사항 -->
        <div style="background:#fff8e8; border:1px solid #ffe082;
                    border-radius:8px; padding:12px 14px; margin-bottom:24px;">
            <p style="font-size:13px; color:#795548; margin:0; line-height:1.6;">
                ⚠ 비밀번호는 연속된 숫자(1234), 동일한 숫자(1111)는 사용할 수 없습니다.<br>
                ⚠ 5회 연속 오류 시 계좌가 잠깁니다.
            </p>
        </div>

        <div style="display:flex; gap:10px;">
            <button onclick="goStep(2); initStep2();"
                    style="flex:1; padding:14px; background:#f0f2f6; color:#333;
                           border:none; border-radius:8px; font-size:15px;
                           cursor:pointer;">
                이전
            </button>
            <button onclick="submitStep3()"
                    style="flex:2; padding:14px; background:#003087; color:#fff;
                           border:none; border-radius:8px; font-size:16px;
                           font-weight:600; cursor:pointer;">
                계좌 개설 신청
            </button>
        </div>
    `;

    // 비밀번호 일치 실시간 체크
    ['input-pw', 'input-pw-confirm'].forEach(id => {
        document.getElementById(id).addEventListener('input', checkPwMatch);
    });
}

function checkPwMatch() {
    const pw      = document.getElementById('input-pw').value;
    const confirm = document.getElementById('input-pw-confirm').value;
    const msg     = document.getElementById('pw-match-msg');
    if (!confirm) { msg.textContent = ''; return; }
    if (pw === confirm) {
        msg.textContent = '✅ 비밀번호가 일치합니다.';
        msg.style.color = '#28a745';
    } else {
        msg.textContent = '❌ 비밀번호가 일치하지 않습니다.';
        msg.style.color = '#e65100';
    }
}

function validatePassword(pw) {
    // 4~6자리 숫자
    if (!/^\d{4,6}$/.test(pw)) return '4~6자리 숫자만 입력해 주세요.';
    // 연속된 숫자 (1234, 2345 등)
    const seq = '0123456789';
    for (let i = 0; i <= seq.length - pw.length; i++) {
        if (seq.substring(i, i + pw.length) === pw) return '연속된 숫자는 사용할 수 없습니다.';
    }
    // 동일한 숫자 (1111, 2222 등)
    if (/^(\d)\1+$/.test(pw)) return '동일한 숫자만으로 된 비밀번호는 사용할 수 없습니다.';
    return null;
}

async function submitStep3() {
    const pw      = document.getElementById('input-pw').value;
    const confirm = document.getElementById('input-pw-confirm').value;

    // 유효성 검사
    const pwError = validatePassword(pw);
    if (pwError) { alert(pwError); return; }
    if (pw !== confirm) { alert('비밀번호가 일치하지 않습니다.'); return; }

    state.password = pw;

    // API 호출
    const btn = document.querySelector('#step-3 button:last-child');
    btn.disabled     = true;
    btn.textContent  = '처리 중...';

    try {
        const res = await fetch('/api/accounts', {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                accountType:  state.accountType,
                accountAlias: state.accountAlias || null,
                password:     state.password,
            })
        });

        const json = await res.json();

        if (!res.ok) {
            alert(json?.message ?? '계좌 개설에 실패했습니다.');
            btn.disabled    = false;
            btn.textContent = '계좌 개설 신청';
            return;
        }

        // 성공 → 4단계
        state.result = json.data;
        goStep(4);
        initStep4();

    } catch (e) {
        alert('서버 오류가 발생했습니다.');
        btn.disabled    = false;
        btn.textContent = '계좌 개설 신청';
    }
}

// ── 4단계: 완료 ───────────────────────────────────────────────────
function initStep4() {
    const data      = state.result ?? {};
    const typeLabel = { CHECKING: '입출금 통장', SAVINGS: '적금', DEPOSIT: '예금' };
    const container = document.getElementById('step-4');

    container.innerHTML = `
        <div style="text-align:center; padding:20px 0;">

            <!-- 성공 아이콘 -->
            <div style="width:72px; height:72px; background:#e8f5e9;
                        border-radius:50%; display:flex; align-items:center;
                        justify-content:center; margin:0 auto 20px;">
                <span style="font-size:36px;">✅</span>
            </div>

            <h2 style="font-size:22px; margin-bottom:8px;">계좌 개설 완료!</h2>
            <p style="color:#666; font-size:14px; margin-bottom:32px;">
                새 계좌가 성공적으로 개설되었습니다.
            </p>

            <!-- 계좌 정보 카드 -->
            <div style="background:#f0f4fb; border-radius:12px;
                        padding:24px; text-align:left; margin-bottom:32px;">
                <p style="font-size:12px; color:#888; margin-bottom:4px;">
                    계좌번호
                </p>
                <p style="font-size:22px; font-weight:700; color:#003087;
                          letter-spacing:2px; margin-bottom:16px;">
                    ${data.accountNumber ?? '-'}
                </p>
                <div style="display:flex; flex-direction:column; gap:10px;">
                    <div style="display:flex; justify-content:space-between;
                                font-size:14px;">
                        <span style="color:#888;">계좌 종류</span>
                        <span style="font-weight:600;">
                            ${typeLabel[data.accountType] ?? data.accountType ?? '-'}
                        </span>
                    </div>
                    <div style="display:flex; justify-content:space-between;
                                font-size:14px;">
                        <span style="color:#888;">계좌 별명</span>
                        <span style="font-weight:600;">
                            ${data.accountAlias ?? '(없음)'}
                        </span>
                    </div>
                    <div style="display:flex; justify-content:space-between;
                                font-size:14px;">
                        <span style="color:#888;">계좌 상태</span>
                        <span style="font-weight:600; color:#28a745;">
                            정상
                        </span>
                    </div>
                    <div style="display:flex; justify-content:space-between;
                                font-size:14px;">
                        <span style="color:#888;">잔액</span>
                        <span style="font-weight:600;">0원</span>
                    </div>
                </div>
            </div>

            <!-- 버튼 -->
            <div style="display:flex; flex-direction:column; gap:10px;">
                <button onclick="location.href='/mypage'"
                        style="width:100%; padding:14px; background:#003087;
                               color:#fff; border:none; border-radius:8px;
                               font-size:16px; font-weight:600; cursor:pointer;">
                    마이페이지에서 확인
                </button>
                <button onclick="location.href='/'"
                        style="width:100%; padding:14px; background:#f0f2f6;
                               color:#333; border:none; border-radius:8px;
                               font-size:15px; cursor:pointer;">
                    홈으로
                </button>
            </div>
        </div>
    `;
}