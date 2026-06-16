/**
 * identity-verify.js
 * 가상 본인인증 모달 — BNK 부산은행
 *
 * [수정 내역]
 *  1. 카카오 주소 API: open() 팝업 → embed 방식 (팝업 차단 / CSP 우회)
 *  2. 주민번호 뒷자리: 7자리 전체 → 성별코드 1자리만 입력
 *     - dot 표시: 7개 → 1개
 *     - 검증: _back.length !== 7 → !== 1
 *     - 키패드: 1자리 입력 즉시 자동 닫힘
 *  3. 모달 재사용 시 _back 초기화 누락 버그 수정
 *  4. 카카오 스크립트: //t1... → https://t1... (mixed-content 차단 방지)
 *  5. 스크립트 로드 실패 시 onerror 처리 추가
 */
const IdentityVerify = (() => {
    'use strict';

    let _onSuccess = null;
    let _back = '';         // 주민번호 뒷자리 성별코드 (1자리만)
    let _addrData = null;   // 카카오 주소 결과

    // ── 카카오 우편번호 API 로드 ──────────────────────────────────
    function _loadKakaoPost(callback) {
        if (window.daum && window.daum.Postcode) {
            callback();
            return;
        }
        const script = document.createElement('script');
        // ① https 명시 — 로컬 http 환경에서 mixed-content 차단 방지
        script.src = 'https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js';
        script.onload = callback;
        // ⑤ 로드 실패 시 사용자 안내
        script.onerror = () => {
            alert('주소 검색 서비스를 불러오지 못했습니다.\n네트워크 연결을 확인해 주세요.');
        };
        document.head.appendChild(script);
    }

    // ── 주소 검색 (② embed 방식 — 팝업 차단 / CSP 우회) ─────────
    function _openAddrSearch() {
        _loadKakaoPost(() => {
            // 기존 embed 컨테이너 제거 후 재생성
            document.getElementById('iv-addr-embed')?.remove();

            const wrap = document.createElement('div');
            wrap.id = 'iv-addr-embed';
            wrap.style.cssText = [
                'position:fixed', 'inset:0', 'z-index:99999',
                'background:rgba(0,0,0,.55)',
                'display:flex', 'align-items:center', 'justify-content:center',
            ].join(';');

            const inner = document.createElement('div');
            inner.style.cssText = [
                'width:min(500px,95vw)', 'height:530px',
                'background:#fff', 'border-radius:12px', 'overflow:hidden',
                'position:relative',
            ].join(';');

            // 닫기 버튼
            const closeBtn = document.createElement('button');
            closeBtn.textContent = '✕';
            closeBtn.type = 'button';
            closeBtn.style.cssText = [
                'position:absolute', 'top:8px', 'right:12px', 'z-index:1',
                'background:none', 'border:none', 'font-size:22px',
                'cursor:pointer', 'color:#52525b', 'line-height:1',
            ].join(';');
            closeBtn.addEventListener('click', () => wrap.remove());

            // 오버레이 클릭 닫기
            wrap.addEventListener('click', e => { if (e.target === wrap) wrap.remove(); });

            inner.appendChild(closeBtn);
            wrap.appendChild(inner);
            document.body.appendChild(wrap);

            new daum.Postcode({
                oncomplete: (data) => {
                    wrap.remove();
                    const road = data.roadAddress || data.jibunAddress;
                    document.getElementById('iv-addr').value = `[${data.zonecode}] ${road}`;
                    document.getElementById('iv-detail').value = '';
                    document.getElementById('iv-detail').focus();
                    _addrData = {
                        address: road,
                        zipCode: data.zonecode,
                    };
                    _showErr('err-addr', false);
                },
                // ② embed: 팝업 대신 inner 컨테이너에 렌더링
                embed: true,
                autoClose: false,
                width: '100%',
                height: '100%',
            }).embed(inner);
        });
    }

    // ── 키패드 (셔플) ─────────────────────────────────────────────
    function _buildKeypad() {
        const nums = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9'];
        for (let i = nums.length - 1; i > 0; i--) {
            const j = Math.floor(Math.random() * (i + 1));
            [nums[i], nums[j]] = [nums[j], nums[i]];
        }
        const keys = [...nums, '', 'DEL'];
        const grid = document.getElementById('iv-kgrid');
        if (!grid) return;
        grid.innerHTML = '';
        keys.forEach(k => {
            const btn = document.createElement('button');
            btn.type = 'button';
            if (k === '') {
                btn.className = 'iv-key blank';
            } else if (k === 'DEL') {
                btn.className = 'iv-key del';
                btn.textContent = '⌫';
                btn.addEventListener('click', () => {
                    if (_back.length > 0) {
                        _back = _back.slice(0, -1);
                        _updateDots();
                    }
                });
            } else {
                btn.className = 'iv-key';
                btn.textContent = k;
                btn.addEventListener('click', () => {
                    // ③ 1자리만 받음 (성별코드)
                    if (_back.length < 1) {
                        _back += k;
                        _updateDots();
                        // 1자리 입력 즉시 키패드 닫기
                        if (_back.length === 1) _closeKeypad();
                    }
                });
            }
            grid.appendChild(btn);
        });
    }

    // ③ dot 업데이트 — 1자리 기준
    function _updateDots() {
        const d = document.getElementById('iv-d0');
        if (d) d.classList.toggle('filled', _back.length >= 1);
    }

    function _closeKeypad() {
        document.getElementById('iv-keypad')?.classList.remove('open');
        document.getElementById('iv-res-back')?.classList.remove('on');
    }

    // ── 에러 표시 ─────────────────────────────────────────────────
    function _showErr(id, show) {
        const el = document.getElementById(id);
        if (el) el.classList.toggle('show', show);
    }

    // ── 모달 HTML (주민번호 dot 1개로 축소) ───────────────────────
    function _buildHTML() {
        return `
<div class="iv-overlay" id="iv-overlay" role="dialog" aria-modal="true" aria-label="본인인증">
  <div class="iv-modal">

    <div class="iv-head">
      <div class="iv-head-icon">🔐</div>
      <div>
        <div class="iv-head-title">본인인증</div>
        <div class="iv-head-sub">안전한 서비스 이용을 위한 신원 확인</div>
      </div>
    </div>

    <!-- 입력 패널 -->
    <div id="iv-panel-form">
      <div class="iv-body">
        <div class="iv-notice">
          입력하신 정보는 본인 확인 목적으로만 사용되며 별도 저장되지 않습니다.
        </div>

        <!-- 이름 -->
        <div class="iv-group">
          <label class="iv-label">이름 <span class="req">*</span></label>
          <input class="iv-input" type="text" id="iv-name" placeholder="실명 입력" autocomplete="name">
          <div class="iv-err" id="err-name">이름을 입력해주세요.</div>
        </div>

        <!-- 주민번호 — 앞 6자리 + 성별코드 1자리 -->
        <div class="iv-group">
          <label class="iv-label">주민등록번호 <span class="req">*</span></label>
          <div class="iv-res-wrap">
            <input class="iv-input iv-res-front" type="text" id="iv-front"
              placeholder="앞 6자리" maxlength="6" inputmode="numeric">
            <span class="iv-res-dash">-</span>
            <div class="iv-res-back-wrap">
              <!-- ③ dot 1개만 표시 (성별코드) + 나머지는 ● 고정 텍스트 -->
              <div class="iv-res-back" id="iv-res-back" tabindex="0" role="button" aria-label="뒷자리 성별코드 입력">
                <span class="iv-dot" id="iv-d0"></span>
                <span style="color:#a1a1aa;font-size:13px;letter-spacing:3px">●●●●●●</span>
              </div>
              <div class="iv-keypad" id="iv-keypad">
                <div class="iv-kgrid" id="iv-kgrid"></div>
              </div>
            </div>
          </div>
          <div class="iv-err" id="err-res">주민번호 앞 6자리와 성별코드(뒷자리 첫 번째)를 입력해주세요.</div>
        </div>

        <!-- 주소 -->
        <div class="iv-group">
          <label class="iv-label">주소 <span class="req">*</span></label>
          <div class="iv-addr-row">
            <input class="iv-input" type="text" id="iv-addr"
              placeholder="주소 검색 버튼을 눌러주세요" readonly>
            <button class="iv-btn-search" type="button" id="btn-addr-search">
              🔍 검색
            </button>
          </div>
          <input class="iv-input" type="text" id="iv-detail"
            placeholder="상세 주소 입력 (동/호수 등)" style="margin-top:8px">
          <div class="iv-err" id="err-addr">주소를 검색해주세요.</div>
        </div>
      </div>

      <div class="iv-foot">
        <button class="iv-btn-cancel" type="button" id="btn-iv-cancel">취소</button>
        <button class="iv-btn-ok" type="button" id="btn-iv-ok">인증하기</button>
      </div>
    </div>

    <!-- 완료 패널 -->
    <div id="iv-panel-done" style="display:none">
      <div class="iv-body">
        <div class="iv-done">
          <div class="iv-done-icon">✅</div>
          <div class="iv-done-title">본인인증 완료</div>
          <div class="iv-done-desc">아래 정보로 인증이 완료되었습니다.</div>
          <div class="iv-done-info" id="iv-done-info"></div>
        </div>
      </div>
      <div class="iv-foot">
        <button class="iv-btn-ok" type="button" id="btn-iv-confirm" style="flex:1">
          확인 및 적용
        </button>
      </div>
    </div>

  </div>
</div>`;
    }

    // ── 이벤트 바인딩 ─────────────────────────────────────────────
    function _bindEvents() {
        // 주민번호 앞자리 숫자만
        document.getElementById('iv-front')?.addEventListener('input', e => {
            e.target.value = e.target.value.replace(/\D/g, '');
        });

        // 뒷자리 클릭 → 키패드 토글
        document.getElementById('iv-res-back')?.addEventListener('click', e => {
            e.stopPropagation();
            const kp = document.getElementById('iv-keypad');
            const isOpen = kp?.classList.contains('open');
            if (isOpen) {
                _closeKeypad();
            } else {
                kp?.classList.add('open');
                document.getElementById('iv-res-back')?.classList.add('on');
                _buildKeypad(); // 클릭마다 셔플
            }
        });

        // 키패드 외부 클릭 닫기
        document.addEventListener('click', e => {
            if (!e.target.closest('#iv-res-back') && !e.target.closest('#iv-keypad')) {
                _closeKeypad();
            }
        });

        // 주소 검색
        document.getElementById('btn-addr-search')?.addEventListener('click', _openAddrSearch);

        // 인증하기
        document.getElementById('btn-iv-ok')?.addEventListener('click', () => {
            const name = document.getElementById('iv-name')?.value.trim();
            const front = document.getElementById('iv-front')?.value.trim();
            const addr = document.getElementById('iv-addr')?.value.trim();
            let ok = true;

            ['err-name', 'err-res', 'err-addr'].forEach(id => _showErr(id, false));

            if (!name) { _showErr('err-name', true); ok = false; }
            // ③ 뒷자리: 7자리 전체 → 1자리(성별코드)만 검증
            if (front.length !== 6 || _back.length !== 1) { _showErr('err-res', true); ok = false; }
            if (!addr) { _showErr('err-addr', true); ok = false; }
            if (!ok) return;

            // 생년월일 계산 (주민번호 앞 6자리 + 성별코드)
            const genderCode = _back[0];
            const century = ['3', '4', '7', '8'].includes(genderCode) ? '20' : '19';
            const birthDate = `${century}${front.slice(0, 2)}-${front.slice(2, 4)}-${front.slice(4, 6)}`;
            const detail = document.getElementById('iv-detail')?.value.trim();

            // 완료 화면 표시
            const info = document.getElementById('iv-done-info');
            if (info) {
                info.innerHTML = [
                    ['이름', name],
                    ['생년월일', birthDate],
                    ['주민번호', `${front}-${genderCode}●●●●●●`],
                    ['주소', (_addrData?.address || addr) + (detail ? ` ${detail}` : '')],
                ].map(([l, v]) => `
                    <div class="iv-done-row">
                        <span class="iv-done-lbl">${l}</span>
                        <span class="iv-done-val">${v}</span>
                    </div>`).join('');
            }

            // 결과 저장
			_pendingResult = {
			    name,
			    birthDate,
			    residentFront: front,
			    genderCode, 
			    address: _addrData?.address || addr,
			    addressDetail: detail,
			    zipCode: _addrData?.zipCode || '',
			};

            document.getElementById('iv-panel-form').style.display = 'none';
            document.getElementById('iv-panel-done').style.display = 'block';
        });

        // 확인 및 적용
        document.getElementById('btn-iv-confirm')?.addEventListener('click', () => {
            _close();
            if (_onSuccess && _pendingResult) {
                _onSuccess(_pendingResult);
            }
        });

        // 취소
        document.getElementById('btn-iv-cancel')?.addEventListener('click', _close);

        // 오버레이 클릭 닫기
        document.getElementById('iv-overlay')?.addEventListener('click', e => {
            if (e.target.id === 'iv-overlay') _close();
        });

        // ESC 닫기
        document.addEventListener('keydown', e => {
            if (e.key === 'Escape') _close();
        });
    }

    let _pendingResult = null;

    // ── 열기 ─────────────────────────────────────────────────────
    function _open(opts = {}) {
        _onSuccess = opts.onSuccess || null;
        // ④ 재사용 시 상태 완전 초기화
        _back = '';
        _addrData = null;
        _pendingResult = null;

        if (!document.getElementById('iv-overlay')) {
            const div = document.createElement('div');
            div.innerHTML = _buildHTML();
            document.body.appendChild(div.firstElementChild);
            _buildKeypad();
            _bindEvents();
        } else {
            // 재사용 시 폼 필드 초기화
            document.getElementById('iv-name').value = '';
            document.getElementById('iv-front').value = '';
            document.getElementById('iv-addr').value = '';
            document.getElementById('iv-detail').value = '';
            _updateDots();
            document.getElementById('iv-panel-form').style.display = 'block';
            document.getElementById('iv-panel-done').style.display = 'none';
            ['err-name', 'err-res', 'err-addr'].forEach(id => _showErr(id, false));
        }

        requestAnimationFrame(() => {
            document.getElementById('iv-overlay')?.classList.add('is-open');
        });
    }

    // ── 닫기 ─────────────────────────────────────────────────────
    function _close() {
        _closeKeypad();
        document.getElementById('iv-overlay')?.classList.remove('is-open');
    }

    return { open: _open, close: _close };
})();