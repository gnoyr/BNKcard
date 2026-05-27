const cardId = new URLSearchParams(location.search).get('cardId');

if (!cardId) {
  document.getElementById('main-wrap').innerHTML =
    '<div class="loading-wrap"><p>잘못된 접근입니다. cardId가 없습니다.</p><br><a href="/card-main.html">카드 목록으로</a></div>';
  throw new Error('no cardId');
}

async function api(url) {
  try {
    const res = await fetch(url, { credentials: 'include' });
    if (!res.ok) return null;
    return (await res.json())?.data ?? null;
  } catch { return null; }
}

function fmtFee(v) {
  if (!v && v !== 0) return '-';
  return Number(v) === 0 ? '연회비 없음' : Number(v).toLocaleString() + '원';
}
function fmtDate(v) {
  if (!v) return '-';
  return new Date(v).toLocaleDateString('ko-KR');
}
function cardTypeName(t) {
  return { CREDIT:'신용카드', CHECK:'체크카드', PREPAID:'선불카드', HYBRID:'하이브리드' }[t] ?? t;
}
function benefitTypeLabel(t) {
  return { RATE_DISCOUNT:'할인율', FIXED_DISCOUNT:'고정할인', POINT:'포인트', CASHBACK:'캐시백', FREE:'무료' }[t] ?? t;
}

// ══════════════════════════════════════════════
//  카드 상세 데이터 로드
// ══════════════════════════════════════════════
async function loadCard() {
  const card = await api(`/api/cards/${cardId}`);

  if (!card) {
    document.getElementById('main-wrap').innerHTML =
      '<div class="loading-wrap"><p>카드 정보를 불러올 수 없습니다.</p><br><a href="/card-main.html">카드 목록으로</a></div>';
    return;
  }

  const termsWithFiles = await loadTermsFiles(card.termsFiles ?? []);
  renderPage(card, termsWithFiles);
  
  initStickyObserver(card);
  initImageSlider();
}

async function loadTermsFiles(termsList) {
  if (!termsList?.length) return [];
  return await Promise.all(
    termsList.map(async t => {
      if (!t.termsId) return { ...t, files: [] };
      const files = await api(`/api/terms/${t.termsId}/files`);
      return { ...t, files: files ?? [] };
    })
  );
}

// ══════════════════════════════════════════════
//  전체 페이지 렌더링
// ══════════════════════════════════════════════
function renderPage(card, termsWithFiles) {
  const validImages = card.images?.filter(i => ['FRONT', 'THUMBNAIL', 'DETAIL'].includes(i.imageType)) ?? [];

  document.getElementById('main-wrap').innerHTML = `
    <section class="card-hero">
      <div class="card-hero-inner">
        <div class="card-image-container">
          <div class="card-image-wrap" id="card-slider-wrap">
            ${validImages.length > 0 ? `
              <div class="slider-track" id="slider-track">
                ${validImages.map(img => `
                  <div class="slider-slide">
                    <img src="${img.imageUrl}" alt="${card.cardName}">
                  </div>
                `).join('')}
              </div>
              ${validImages.length > 1 ? `
                <button class="slider-btn prev" onclick="moveSlider(-1)">‹</button>
                <button class="slider-btn next" onclick="moveSlider(1)">›</button>
              ` : ''}
            ` : `<div class="card-image-placeholder">${card.cardName}</div>`}
          </div>
          ${validImages.length > 1 ? `
            <div class="slider-dots" id="slider-dots">
              ${validImages.map((_, idx) => `<div class="slider-dot ${idx === 0 ? 'active' : ''}"></div>`).join('')}
            </div>
          ` : ''}
        </div>

        <div class="card-info">
          <div class="card-badge-row">
            <span class="badge badge-type">${cardTypeName(card.cardType)}</span>
            ${card.cardStatus === 'PUBLISHED' ? '<span class="badge badge-status">게시중</span>' : ''}
            ${card.brandName ? `<span class="badge badge-brand">${card.brandName}</span>` : ''}
          </div>
          <h1 class="card-title" id="hero-apply-target">${card.cardName}</h1>
          <p class="card-subtitle">${card.companyName ?? 'BNK부산은행'}</p>
          <p class="card-desc">${card.summaryDescription ?? ''}</p>
          <div class="card-fee-row">
            <div class="fee-item">
              <div class="fee-label">국내 연회비</div>
              <div class="fee-value">${fmtFee(card.annualFeeDomestic)}</div>
            </div>
            <div class="fee-item">
              <div class="fee-label">해외 연회비</div>
              <div class="fee-value">${fmtFee(card.annualFeeOverseas)}</div>
            </div>
            ${card.previousMonthSpend ? `
            <div class="fee-item">
              <div class="fee-label">전월 실적</div>
              <div class="fee-value">${Number(card.previousMonthSpend).toLocaleString()}원</div>
            </div>` : ''}
          </div>
          <button class="btn-apply" id="main-apply-btn" onclick="applyCard(${card.cardId})">
            발급 신청
          </button>
        </div>
      </div>
    </section>

    <section class="benefit-section">
      <div class="section-heading">💳 상세 혜택 정보</div>
      ${renderBenefitsRaw(card.benefits ?? [])}
    </section>

    <div class="tab-container">
      <div class="tab-bar">
        <button class="tab-btn active" onclick="switchTab(this,'tab-product')">상품안내</button>
        <button class="tab-btn" onclick="switchTab(this,'tab-service')">서비스안내</button>
        <button class="tab-btn" onclick="switchTab(this,'tab-fee')">연회비/수수료</button>
        <button class="tab-btn" onclick="switchTab(this,'tab-etc')">기타</button>
        <button class="tab-btn" onclick="switchTab(this,'tab-terms')">상품약관</button>
        <button class="tab-btn" onclick="switchTab(this,'tab-notice')">유의사항</button>
      </div>

      <div class="tab-content active" id="tab-product">${renderTabProduct(card)}</div>
      <div class="tab-content" id="tab-service">${renderTabService(card)}</div>
      <div class="tab-content" id="tab-fee">${renderTabFee(card)}</div>
      <div class="tab-content" id="tab-etc">${renderTabEtc(card)}</div>
      <div class="tab-content" id="tab-terms">${renderTabTerms(termsWithFiles)}</div>
      <div class="tab-content" id="tab-notice">${renderTabNotice(card)}</div>
    </div>
  `;
}

// ── [복원] 원래 방식대로 전체 리스트를 그대로 격자 노출하는 함수 ──
function renderBenefitsRaw(benefits) {
  if (!benefits.length) {
    return '<div class="no-benefit">등록된 혜택 정보가 없습니다.</div>';
  }

  return `
    <div class="benefit-grid">
      ${benefits.map(b => {
        const rateVal = b.discountRate ? `${b.discountRate}%` :
                       b.cashbackRate ? `${b.cashbackRate}%` :
                       b.pointRate    ? `${b.pointRate}%`    : null;
        return `
          <div class="benefit-card">
            ${b.categoryName ? `<span class="benefit-category">${b.categoryName}</span>` : ''}
            <div class="benefit-title">${b.benefitTitle ?? ''}</div>
            ${b.benefitType ? `<span class="benefit-type-badge type-${b.benefitType}">${benefitTypeLabel(b.benefitType)}</span>` : ''}
            ${rateVal ? `<div class="benefit-rate">${rateVal}</div>` : ''}
            ${b.displayText ? `<div class="benefit-display">${b.displayText}</div>` : ''}
            ${b.monthlyLimitAmount ? `<div class="benefit-limit">월 한도 ${Number(b.monthlyLimitAmount).toLocaleString()}원</div>` : ''}
          </div>`;
      }).join('')}
    </div>
  `;
}

// ── 이미지 슬라이더 내부 구동 제어 엔진 ──
let currentSlide = 0;
function initImageSlider() {
  currentSlide = 0;
}

function moveSlider(direction) {
  const track = document.getElementById('slider-track');
  const wrapper = document.querySelector('.card-image-wrap'); // 기준이 되는 부모 뷰포트
  const slides = document.querySelectorAll('.slider-slide');
  const dots = document.querySelectorAll('.slider-dot');
  
  if (!track || !wrapper || slides.length === 0) return;

  currentSlide += direction;
  if (currentSlide < 0) currentSlide = slides.length - 1;
  if (currentSlide >= slides.length) currentSlide = 0;

  // 감싸고 있는 뷰포트 너비를 기준으로 정확하게 이동 거리를 계산합니다.
  const slideWidth = wrapper.clientWidth;
  track.style.transform = `translateX(${-currentSlide * slideWidth}px)`;

  dots.forEach((dot, idx) => {
    dot.classList.toggle('active', idx === currentSlide);
  });
}

// ── 스티키 바 IntersectionObserver 핵심 엔진 설계 ──
function initStickyObserver(card) {
  const mainApplyBtn = document.getElementById('main-apply-btn');
  const stickyBar = document.getElementById('sticky-apply-bar');
  
  if (!mainApplyBtn || !stickyBar) return;

  document.getElementById('sticky-card-name').innerText = card.cardName;
  document.getElementById('sticky-card-fee').innerText = `국내 연회비: ${fmtFee(card.annualFeeDomestic)}`;
  document.getElementById('btn-sticky-trigger').onclick = () => applyCard(card.cardId);

  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (!entry.isIntersecting) {
        stickyBar.classList.add('show');
      } else {
        stickyBar.classList.remove('show');
      }
    });
  }, { threshold: 0 });

  observer.observe(mainApplyBtn);
}

// ── 기존 탭 렌더러 함수군 ──
function renderTabProduct(card) {
  const introContents = (card.contents ?? []).filter(c => ['INTRO','GUIDE'].includes(c.contentType));
  if (!introContents.length) {
    return `
      <div class="tab-content-title">商品 개요</div>
      <table class="info-table">
        <tr><th>카드명</th><td>${card.cardName}</td></tr>
        <tr><th>카드사</th><td>${card.companyName ?? 'BNK부산은행'}</td></tr>
        <tr><th>카드 유형</th><td>${cardTypeName(card.cardType)}</td></tr>
        <tr><th>브랜드</th><td>${card.brandName ?? '-'}</td></tr>
      </table>`;
  }
  return introContents.map(c => `
    <div class="content-block">
      <h4>${c.title ?? '상품 안내'}</h4>
      <div class="content-html">${c.contentHtml ?? ''}</div>
    </div>
  `).join('');
}

function renderTabService(card) {
  const serviceContents = (card.contents ?? []).filter(c => c.contentType === 'GUIDE');
  if (!serviceContents.length) return '<p style="color:#bbb;text-align:center;padding:40px">등록된 서비스 안내가 없습니다.</p>';
  return serviceContents.map(c => `<div class="content-block"><h4>${c.title ?? '서비스 안내'}</h4><div class="content-html">${c.contentHtml ?? ''}</div></div>`).join('');
}

function renderTabFee(card) {
  return `
    <div class="tab-content-title">연회비 및 수수료 안내</div>
    <table class="info-table">
      <tr><th>국내 연회비</th><td>${fmtFee(card.annualFeeDomestic)}</td></tr>
      <tr><th>해외 연회비</th><td>${fmtFee(card.annualFeeOverseas)}</td></tr>
    </table>`;
}

function renderTabEtc(card) {
  const etcContents = (card.contents ?? []).filter(c => ['NOTICE','FAQ','EVENT'].includes(c.contentType));
  if (!etcContents.length) return '<p style="color:#888;line-height:1.8;">• 본 카드는 BNK부산은행에서 발급합니다.<br>• 문의: 1588-0079</p>';
  return etcContents.map(c => `<div class="content-block"><h4>${c.title ?? '기타'}</h4><div class="content-html">${c.contentHtml ?? ''}</div></div>`).join('');
}

function renderTabTerms(termsWithFiles) {
  if (!termsWithFiles.length) return '<p style="color:#bbb;text-align:center;padding:40px">연결된 약관 정보가 없습니다.</p>';
  
  return termsWithFiles.map(t => {
    // PDF 파일과 이미지 파일들을 각각 분류합니다.
    const pdfFile = t.files?.find(f => f.fileType === 'PDF');
    const imgFiles = t.files?.filter(f => f.fileType === 'IMAGE') ?? [];
    
    // 대표로 노출할 썸네일 이미지 (이미지가 있다면 첫 번째 이미지를 사용, 없으면 기본 배경)
    const thumbSrc = imgFiles.length > 0 ? imgFiles[0].filePath : '';

    return `
      <div class="terms-item">
        ${thumbSrc ? `
          <div class="terms-thumb">
            <img src="${thumbSrc}" alt="${t.title ?? '약관 썸네일'}">
          </div>
        ` : `<div class="terms-thumb" style="background:#e0e7f5;"></div>`}
        
        <div class="terms-info">
          <div class="terms-name">${t.title ?? '약관'}</div>
          <div class="terms-version">개정일: ${fmtDate(t.createdAt)} (v${t.version ?? '1.0'})</div>
          
          <div class="terms-actions">
            ${pdfFile ? `<a class="btn-terms-pdf" href="${pdfFile.filePath}" target="_blank" download>📄 PDF 다운로드</a>` : ''}
            ${imgFiles.length > 0 ? `<button class="btn-terms-view" onclick="this.parentElement.nextElementSibling.style.display = (this.parentElement.nextElementSibling.style.display === 'flex' ? 'none' : 'flex')">🔍 이미지 보기</button>` : ''}
          </div>
          
          ${imgFiles.length > 0 ? `
            <div class="terms-preview" style="display: none; margin-top: 12px; gap: 8px; flex-wrap: wrap;">
              ${imgFiles.map(img => `
                <img src="${img.filePath}" alt="약관 이미지" onclick="openImgModal('${img.filePath}')">
              `).join('')}
            </div>
          ` : ''}
        </div>
      </div>`;
  }).join('');
}

function renderTabNotice(card) {
  return `
    <div class="tab-content-title">📢 유의사항</div>
    <table class="notice-table">
      <tbody>
        <tr><td>1</td><td>혜택은 전월 실적 조건 충족 시 적용됩니다.</td></tr>
        <tr><td>2</td><td>연회비는 카드 발급 시 부과되며, 해지 시 일할 환급됩니다.</td></tr>
        <tr><td>3</td><td>본 카드 상품 정보는 변경될 수 있으니 최신 정보를 확인하세요.</td></tr>
        <tr><td>4</td><td>문의: BNK부산은행 고객센터 1588-6200</td></tr>
      </tbody>
    </table>
  `;
}
function switchTab(btn, tabId) {
  document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
  document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
  btn.classList.add('active');
  document.getElementById(tabId)?.classList.add('active');
}

function openImgModal(src) {
  document.getElementById('img-modal-src').src = src;
  document.getElementById('img-modal').classList.add('open');
}
function closeImgModal() {
  document.getElementById('img-modal').classList.remove('open');
}

function applyCard(cardId) {
  const ok = confirm('카드 발급을 신청하시겠습니까?\n로그인이 필요합니다.');
  if (ok) location.href = `/auth-test.html?redirect=/apply?cardId=${cardId}`;
}

loadCard();