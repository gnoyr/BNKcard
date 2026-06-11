/**
 * main.js  |  BNK 부산은행 카드 메인 페이지
 * (구 card-main.js → static/main.js 로 이관)
 *
 * ▸ 카드 상세 이동 경로 변경:
 *   /card-detail.html?cardId=X  →  /card/?cardId=X
 */

const state = {
    compareCart: [],
    cartOpen: false,
    currentPage: 0,
    currentCategoryIds: [],
    modalCategoryIds: [],
    currentCardType: '',
    PAGE_SIZE: 12,
    categories: [],
};

const CAT_EMOJI = {
    TRAVEL: '✈️', DINING: '🍽️', SHOPPING: '🛍️', OIL: '⛽', TRANSPORT: '🚇',
    LEISURE: '🎭', MEDICAL: '💊', TELECOM: '📱', ONLINE: '💻', DELIVERY: '🛵',
    EDUCATION: '📚', LIVING: '🏠', INSURANCE: '🛡️', CASHBACK: '💰', SPORT: '⚽',
    CONVENIENCE: '🏪', TOLL: '🛣️', PARKING: '🅿️', BEAUTY: '💄', MART: '🏬',
    LOCAL: '🏘️', BUSINESS: '💼', ETC: '✨',
};

function toast(msg, dur = 2200) {
    const el = document.getElementById('toast');
    el.textContent = msg; el.classList.add('show');
    setTimeout(() => el.classList.remove('show'), dur);
}
function fmtFee(v) {
    if (v === null || v === undefined) return '-';
    return Number(v) === 0 ? '연회비 없음' : Number(v).toLocaleString() + '원';
}
function cardTypeBadge(t) {
    return { CREDIT: '신용', CHECK: '체크', PREPAID: '선불', HYBRID: '하이브리드' }[t] ?? t;
}
function safeImgHtml(url, alt) {
    if (!url) return `<div class="card-img-placeholder">${alt}</div>`;
    const e = alt.replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    return `<img src="${url}" alt="${e}" onerror="this.style.display='none';this.parentElement.innerHTML='<div class=\\'card-img-placeholder\\'>${e}</div>'">`;
}
async function api(url) {
    try {
        const res = await fetch(url, { credentials: 'include' });
        if (!res.ok) return null;
        return (await res.json())?.data ?? null;
    } catch { return null; }
}

// ── 카테고리 로드 ──
async function loadCategories() {
    const data = await api('/api/cards/categories');
    if (!data?.length) return;
    state.categories = data;
    document.getElementById('category-bar').innerHTML = data.map(c => `
    <button class="cat-btn" data-id="${c.categoryId}" data-name="${(c.categoryName ?? '').replace(/"/g, '&quot;')}"
            onclick="toggleCategory(this,'${c.categoryId}')">
      ${CAT_EMOJI[c.categoryCode] ?? '🔹'} ${c.categoryName}
    </button>`).join('');
    document.getElementById('modal-cat-grid').innerHTML = data.slice(0, 12).map(c => `
    <button class="modal-cat-btn" data-id="${c.categoryId}" data-name="${(c.categoryName ?? '').replace(/"/g, '&quot;')}"
            onclick="toggleModalCategory(this,'${c.categoryId}')">
      <span class="cat-em">${CAT_EMOJI[c.categoryCode] ?? '🔹'}</span>${c.categoryName}
    </button>`).join('');
}

function toggleCategory(btn, catId) {
    const idx = state.currentCategoryIds.indexOf(catId);
    if (idx === -1) { state.currentCategoryIds.push(catId); btn.classList.add('active'); }
    else { state.currentCategoryIds.splice(idx, 1); btn.classList.remove('active'); }
    loadCards(0);
}
function updateCatInfo(elId, ids) {
    const el = document.getElementById(elId);
    if (!ids.length) { el.textContent = ''; return; }
    const names = ids.map(id => state.categories.find(c => String(c.categoryId) === String(id))?.categoryName ?? id);
    el.textContent = '선택된 혜택 : ' + names.join(' , ');
}
function selectCardType(btn, cardType) {
    document.querySelectorAll('.type-tab').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    state.currentCardType = cardType;
    loadCards(0);
}

// ── 인기 검색어 ──
async function loadPopularKeywords() {
    const data = await api('/api/search/keywords/popular');
    const wrap = document.getElementById('popular-keywords');
    if (!data?.length) return;
    data.slice(0, 8).forEach(kw => {
        const keyword = kw.keyword ?? kw.keywordRaw ?? (typeof kw === 'string' ? kw : '');
        if (!keyword) return;
        const tag = document.createElement('button');
        tag.className = 'kw-tag'; tag.textContent = keyword;
        tag.onclick = () => openSearchModal(keyword);
        wrap.appendChild(tag);
    });
}

// ── TOP3 포디움 ──
async function loadTop3() {
    const container = document.getElementById('top3-podium');
    const data = await api('/api/cards/top3');
    if (!data?.length) {
        container.innerHTML = '<div class="empty-wrap">추천 카드를 불러올 수 없습니다.</div>';
        return;
    }
    const ordered = [
        { card: data[1], cls: 'left', rank: 2 },
        { card: data[0], cls: 'center', rank: 1 },
        { card: data[2], cls: 'right', rank: 3 },
    ].filter(o => o.card);
    container.innerHTML = ordered.map(o => `
    <div class="podium-item ${o.cls}">
      <div class="podium-flip">${buildFlipCard(o.card, o.rank)}</div>
    </div>`).join('');
    container.addEventListener('click', handleCardClick);
}

// ── 카드 목록 ──
async function loadCards(page = 0) {
    state.currentPage = page;
    const grid = document.getElementById('card-grid');
    grid.innerHTML = `<div class="loading-wrap" style="grid-column:1/-1">
    <div class="loading-spinner"></div><p>불러오는 중...</p></div>`;

    const q = (document.getElementById('filter-q')?.value ?? '').trim();
    const type = state.currentCardType;
    const catIds = state.currentCategoryIds;

    const params = new URLSearchParams();
    params.append('page', page); params.append('size', state.PAGE_SIZE);
    if (q) params.append('q', q);
    if (type) params.append('cardType', type);
    if (catIds.length === 1) params.append('categoryId', catIds[0]);
    else if (catIds.length > 1) catIds.forEach(id => params.append('categoryIds', id));

    const data = await api('/api/cards?' + params.toString());

    if (!data?.content?.length) {
        grid.innerHTML = `<div class="empty-wrap" style="grid-column:1/-1">🔍 조건에 맞는 카드가 없습니다.</div>`;
        document.getElementById('filter-count').textContent = '';
        renderPagination(0); return;
    }
    const typeLabel = { CREDIT: '신용카드', CHECK: '체크카드', PREPAID: '선불카드' }[type] ?? '';
    document.getElementById('filter-count').textContent =
        `${typeLabel ? typeLabel + ' ' : ''}총 ${(data.totalCount ?? 0).toLocaleString()}개`;
    grid.innerHTML = data.content.map(c => buildFlipCard(c)).join('');
    renderPagination(data.totalCount ?? 0);
}

// ── 카드 버튼 공통 클릭 핸들러 (이벤트 위임용) ──
function handleCardClick(e) {
    const detailBtn = e.target.closest('.btn-detail');
    const compareBtn = e.target.closest('.btn-compare');
    const applyBtn = e.target.closest('.btn-apply');
    if (detailBtn || applyBtn) {
        e.stopPropagation();
        const id = (detailBtn ?? applyBtn).dataset.cardId;
        if (id) location.href = `/card/${id}`;
    }
    if (compareBtn) {
        e.stopPropagation();
        addToCart(
            Number(compareBtn.dataset.cardId),
            compareBtn.dataset.cardName ?? '',
            compareBtn.dataset.companyName ?? ''
        );
    }
}

// ── 카드 플립 빌더 ──
function buildFlipCard(card, rank = null) {
    // data-* 속성용: 큰따옴표만 이스케이프 (작은따옴표 그대로 둬도 안전)
    const ne = (card.cardName ?? '').replace(/"/g, '&quot;');
    const ce = (card.companyName ?? '').replace(/"/g, '&quot;');
    const imgHtml = safeImgHtml(card.thumbnailUrl, card.cardName ?? '');
    const backText = card.topBenefit ?? card.summaryDescription ?? '다양한 혜택 제공';
    const lines = backText.split(/[,\.\n]\s*/).slice(0, 3).map(b => b.trim()).filter(Boolean)
        .map(b => `<div class="benefit-item"><span class="benefit-dot"></span>${b}</div>`).join('');

    // ★ TOP3
    if (rank) {
        const rankBadge = `<div class="top3-rank rank-${rank}">${rank}</div>`;
        const feeText = card.annualFeeDomestic === 0 || card.annualFeeDomestic === null
            ? '연회비 없음'
            : Number(card.annualFeeDomestic).toLocaleString() + '원';
        const benefitText = card.topBenefit ?? '';
        const benefitTags = benefitText
            ? benefitText.split(/[,\/]\s*/).slice(0, 2).map(t => t.trim()).filter(Boolean)
                .map(t => `<span style="display:inline-block;background:rgba(255,255,255,.18);color:#fff;border-radius:10px;padding:2px 9px;font-size:10px;margin:1px;">${t}</span>`)
                .join('') : '';
        return `
        <div class="flip-wrapper">
          <div class="flip-inner">
            <!-- 앞면: 이미지 + 이름만 -->
            <div class="flip-front top3-front">
              ${rankBadge}
              <div class="card-img-wrap top3-img-wrap">${imgHtml}</div>
              <div class="top3-name-bar">
                <div class="top3-card-label">${card.cardName ?? ''}</div>
              </div>
            </div>
            <!-- 뒷면: 기존 앞면 정보 + 액션 버튼 -->
            <div class="flip-back">
              <div class="back-card-name">${card.cardName ?? ''}</div>
              <div class="back-meta">
                <span class="back-type-badge">${cardTypeBadge(card.cardType)}</span>
                <span class="back-company">${card.companyName ?? 'BNK부산은행'}</span>
              </div>
              <div class="back-fee">💳 ${feeText}</div>
              <div class="back-benefit">${lines || backText}</div>
              ${benefitTags ? `<div style="margin:6px 0 2px;">${benefitTags}</div>` : ''}
              <div class="back-actions">
                <button class="btn-detail"  data-card-id="${card.cardId}">자세히</button>
                <button class="btn-compare" data-card-id="${card.cardId}" data-card-name="${ne}" data-company-name="${ce}">비교</button>
                <button class="btn-apply"   data-card-id="${card.cardId}">신청</button>
              </div>
            </div>
          </div>
        </div>`;
    }

    // ★ 일반 그리드 카드 (기존 그대로)
    const rankBadge = '';
    const benefitText = card.topBenefit ?? '';
    const benefitTags = benefitText
        ? benefitText.split(/[,\/]\s*/).slice(0, 2).map(t => t.trim()).filter(Boolean)
            .map(t => `<span style="display:inline-block;background:#e8f0fe;color:#003087;border-radius:10px;padding:1px 7px;font-size:10px;margin:1px;">${t}</span>`)
            .join('') : '';
    const feeDisplay = card.annualFeeDomestic === 0 || card.annualFeeDomestic === null
        ? '<span style="color:#00875a;font-weight:700;">연회비 없음</span>'
        : Number(card.annualFeeDomestic).toLocaleString() + '원';
    return `
    <div class="flip-wrapper">
      <div class="flip-inner">
        <div class="flip-front">
          ${rankBadge}
          <div class="card-img-wrap">${imgHtml}</div>
          <div class="card-front-info">
            <span class="card-type-badge">${cardTypeBadge(card.cardType)}</span>
            <div class="card-name">${card.cardName ?? ''}</div>
            <div class="card-company">${card.companyName ?? 'BNK부산은행'}</div>
            <div class="card-fee">${feeDisplay}</div>
            ${benefitTags ? `<div style="margin-top:5px;line-height:1.8;">${benefitTags}</div>` : ''}
          </div>
        </div>
        <div class="flip-back">
          <div class="back-card-name">${card.cardName ?? ''}</div>
          <div class="back-benefit">${lines || backText}</div>
          <div class="back-actions">
            <button class="btn-detail"  data-card-id="${card.cardId}">자세히</button>
            <button class="btn-compare" data-card-id="${card.cardId}" data-card-name="${ne}" data-company-name="${ce}">비교</button>
            <button class="btn-apply"   data-card-id="${card.cardId}">신청</button>
          </div>
        </div>
      </div>
    </div>`;
}
// ── 페이지네이션 ──
function renderPagination(total) {
    const tp = Math.ceil(total / state.PAGE_SIZE);
    const el = document.getElementById('pagination');
    if (tp <= 1) { el.innerHTML = ''; return; }
    const cur = state.currentPage, s = Math.max(0, cur - 2), e = Math.min(tp - 1, s + 4);
    let h = `<button class="page-btn" onclick="loadCards(${cur - 1})" ${cur === 0 ? 'disabled' : ''}>‹</button>`;
    for (let i = s;i <= e;i++) h += `<button class="page-btn ${i === cur ? 'active' : ''}" onclick="loadCards(${i})">${i + 1}</button>`;
    h += `<button class="page-btn" onclick="loadCards(${cur + 1})" ${cur >= tp - 1 ? 'disabled' : ''}>›</button>`;
    el.innerHTML = h;
}

// ── 필터 초기화 ──
function resetFilter() {
    document.getElementById('filter-q').value = '';
    state.currentCardType = ''; state.currentCategoryIds = [];
    document.querySelectorAll('.type-tab').forEach(b => b.classList.remove('active'));
    document.querySelector('.type-tab[data-type=""]')?.classList.add('active');
    document.querySelectorAll('#category-bar .cat-btn').forEach(b => b.classList.remove('active'));
    document.getElementById('cat-selected-info').textContent = '';
    loadCards(0);
}

// ── 비교 카트 ──
function toggleCart() {
    state.cartOpen = !state.cartOpen;
    document.getElementById('compare-cart').classList.toggle('open', state.cartOpen);
}
function addToCart(cardId, cardName, companyName) {
    if (state.compareCart.find(c => c.cardId === cardId)) { toast('이미 비교 카트에 있는 카드입니다.'); return; }
    if (state.compareCart.length >= 3) { toast('최대 3개까지 비교할 수 있습니다.'); return; }
    state.compareCart.push({ cardId, cardName, companyName });
    renderCart();
    if (!state.cartOpen) toggleCart();
    toast(`"${cardName}" 비교 카트에 추가됐습니다.`);
}
function removeFromCart(cardId) {
    state.compareCart = state.compareCart.filter(c => c.cardId !== cardId);
    renderCart();
}
function renderCart() {
    const cnt = state.compareCart.length;
    document.getElementById('cart-count').textContent = `(${cnt}/3)`;
    document.getElementById('btn-do-compare').disabled = cnt < 2;
    const w = document.getElementById('cart-items');
    if (!cnt) { w.innerHTML = '<div class="cart-empty">카드를 추가하세요</div>'; return; }
    w.innerHTML = state.compareCart.map(c => `
    <div class="cart-item">
      <div class="cart-item-name">${c.cardName}<br>
        <span style="color:#999;font-size:11px">${c.companyName}</span></div>
      <button class="cart-remove" onclick="removeFromCart(${c.cardId})">✕</button>
    </div>`).join('');
}

// ── 비교 모달 ──
function openCompareModal() {
    document.getElementById('compare-modal-overlay').classList.add('open');
    document.body.style.overflow = 'hidden';
}
function closeCompareModal() {
    document.getElementById('compare-modal-overlay').classList.remove('open');
    document.body.style.overflow = '';
}
function handleCompareOverlayClick(e) {
    if (e.target === document.getElementById('compare-modal-overlay')) closeCompareModal();
}

async function doCompare(skipOpen = false) {
    const cardIds = state.compareCart.map(c => c.cardId);
    if (cardIds.length < 2) { toast('2개 이상 선택하세요.'); return; }

    if (!skipOpen) openCompareModal();

    const body = document.getElementById('compare-modal-body');
    if (body) body.innerHTML =
        '<div class="compare-empty"><div class="loading-spinner" style="margin:0 auto 16px"></div>카드 정보를 불러오는 중...</div>';

    let json = null;
    try {
        const res = await fetch('/api/cards/compare', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ cardIds })
        });
        json = await res.json().catch(() => null);

        if (!res.ok) {
            toast(json?.message ?? '비교 요청에 실패했습니다.');
            if (body) body.innerHTML = '<div class="compare-empty">비교 데이터를 불러올 수 없습니다.</div>';
            return;
        }
    } catch (err) {
        if (body) body.innerHTML = '<div class="compare-empty">서버에 연결할 수 없습니다.</div>';
        return;
    }

    // 서버 응답 구조: { data: [...] } 또는 배열 직접 반환 모두 대응
    const cards = Array.isArray(json) ? json
        : Array.isArray(json?.data) ? json.data
            : null;

    if (!cards?.length) {
        if (body) body.innerHTML = '<div class="compare-empty">비교 데이터를 불러올 수 없습니다.</div>';
        return;
    }
    renderCompareModal(cards);
    if (state.cartOpen) toggleCart();
}

function renderCompareModal(cards) {
    const fees  = cards.map(c => (c.annualFeeDomestic  === null ? Infinity : Number(c.annualFeeDomestic)  || 0));
    const oFees = cards.map(c => (c.annualFeeOverseas   === null ? Infinity : Number(c.annualFeeOverseas)  || 0));
    const minFee = Math.min(...fees);
    const minO   = Math.min(...oFees);
    const bCnts  = cards.map(c => (c.benefits ?? []).length);
    const maxB   = Math.max(...bCnts);

    const typeLabel = { CREDIT: '신용카드', CHECK: '체크카드', PREPAID: '선불카드', HYBRID: '하이브리드' };

    const headerCells = cards.map((c, i) => {
        const isBest = bCnts[i] === maxB && maxB > 0;
        const imgHtml = c.thumbnailUrl
            ? `<img src="${c.thumbnailUrl}" class="compare-card-img" alt="${(c.cardName ?? '').replace(/"/g,'&quot;')}" onerror="this.style.display='none'">`
            : '<div class="compare-card-img-placeholder">이미지 없음</div>';
        return `<th>
      <div class="compare-th-inner">
        ${isBest ? '<span class="compare-best-badge">★ 혜택 최다</span>' : '<span style="display:block;height:24px;margin-bottom:10px;"></span>'}
        <div class="compare-img-box">${imgHtml}</div>
        <div class="compare-th-name">${c.cardName ?? ''}</div>
        <div class="compare-th-company">${c.companyName ?? 'BNK부산은행'}</div>
        <div class="compare-th-actions">
          <button class="compare-apply-btn" onclick="location.href='/card/${c.cardId}'">신청하기</button>
          <button class="compare-remove-btn" onclick="removeFromCartAndRefresh(${c.cardId})">제거</button>
        </div>
      </div>
    </th>`;
    }).join('');

    const feeRow = `<tr>
      <td>국내 연회비</td>
      ${cards.map((c, i) => `<td class="${fees[i] === minFee ? 'compare-val--best-fee' : ''}">${fmtFee(c.annualFeeDomestic)}</td>`).join('')}
    </tr>`;
    const oFeeRow = `<tr>
      <td>해외 연회비</td>
      ${cards.map((c, i) => `<td class="${oFees[i] === minO ? 'compare-val--best-fee' : ''}">${fmtFee(c.annualFeeOverseas)}</td>`).join('')}
    </tr>`;
    const typeRow = `<tr>
      <td>카드 유형</td>
      ${cards.map(c => `<td>${typeLabel[c.cardType] ?? c.cardType ?? '-'}</td>`).join('')}
    </tr>`;
    const cntRow = `<tr>
      <td>혜택 수</td>
      ${cards.map((_, i) => `<td class="${bCnts[i] === maxB && maxB > 0 ? 'compare-val--best-benefit' : ''}">${bCnts[i]}개</td>`).join('')}
    </tr>`;
    const benefitRow = `<tr>
      <td>주요 혜택</td>
      ${cards.map((c, i) => {
          const lines = (c.benefits ?? []).slice(0, 4)
              .map(b => `<div>• ${b.displayText ?? b.benefitTitle ?? ''}</div>`).join('') || '-';
          const isBest = bCnts[i] === maxB && maxB > 0;
          return `<td class="compare-val--benefit-cell${isBest ? ' best' : ''}">${lines}</td>`;
      }).join('')}
    </tr>`;

    document.getElementById('compare-modal-body').innerHTML = `
    <div class="compare-legend">
      <span class="compare-legend-item">
        <span class="compare-legend-dot compare-legend-dot--green"></span> 연회비 최저
      </span>
      <span class="compare-legend-item">
        <span class="compare-legend-dot compare-legend-dot--gold"></span> 혜택 최다
      </span>
    </div>
    <div class="compare-scroll-wrap">
      <table class="compare-table">
        <thead><tr><th>항목</th>${headerCells}</tr></thead>
        <tbody>${feeRow}${oFeeRow}${typeRow}${cntRow}${benefitRow}</tbody>
      </table>
    </div>
    <p style="margin-top:14px;font-size:11px;color:#bbb;text-align:right;">카드를 제거한 뒤 비교하기를 다시 눌러주세요.</p>`;
}

function removeFromCartAndRefresh(cardId) {
    removeFromCart(cardId);
    const body = document.getElementById('compare-modal-body');
    if (state.compareCart.length < 2) {
        if (body) body.innerHTML =
            '<div class="compare-empty"><div class="compare-empty-icon">📊</div>비교할 카드가 2개 이상 필요합니다.<br>카드를 추가해주세요.</div>';
        return;
    }
    doCompare(true); // 모달이 이미 열려 있으므로 skipOpen=true
}

// ── 검색 모달 ──
async function openSearchModal(keyword = '') {
    document.getElementById('search-overlay').classList.add('open');
    const input = document.getElementById('modal-search-input');
    input.value = keyword; input.focus();
    document.body.style.overflow = 'hidden';
    const suggest = await api('/api/search/keywords/suggest');
    const suggestEl = document.getElementById('suggest-list');
    if (suggest?.length) {
        suggestEl.innerHTML = suggest.slice(0, 12).map(kw => {
            const word = (kw.keyword ?? kw ?? '').replace(/"/g, '&quot;');
            return `<button class="suggest-tag" data-keyword="${word}">${word}</button>`;
        }).join('');
        suggestEl.addEventListener('click', (e) => {
            const btn = e.target.closest('.suggest-tag');
            if (btn) modalSetKeyword(btn.dataset.keyword);
        });
    } else {
        suggestEl.innerHTML = '<span style="color:#bbb;font-size:13px">추천 검색어가 없습니다.</span>';
    }
    if (keyword) runModalSearch();
}
function closeSearchModal() {
    document.getElementById('search-overlay').classList.remove('open');
    document.body.style.overflow = '';
    state.modalCategoryIds = [];
    document.querySelectorAll('.modal-cat-btn').forEach(b => b.classList.remove('active'));
    document.getElementById('modal-cat-selected').textContent = '';
}
function handleOverlayClick(e) {
    if (e.target === document.getElementById('search-overlay')) closeSearchModal();
}
function modalSetKeyword(kw) {
    document.getElementById('modal-search-input').value = kw;
    runModalSearch();
}
function toggleModalCategory(btn, catId) {
    const idx = state.modalCategoryIds.indexOf(catId);
    if (idx === -1) { state.modalCategoryIds.push(catId); btn.classList.add('active'); }
    else { state.modalCategoryIds.splice(idx, 1); btn.classList.remove('active'); }
    updateCatInfo('modal-cat-selected', state.modalCategoryIds);
    runModalSearch();
}

async function runModalSearch() {
    const q = (document.getElementById('modal-search-input')?.value ?? '').trim();
    const cats = state.modalCategoryIds ?? [];
    if (!q && cats.length === 0) {
        document.getElementById('modal-results-wrap').style.display = 'none'; return;
    }
    document.getElementById('modal-results-wrap').style.display = '';
    document.getElementById('modal-result-title').textContent = '검색 중...';
    document.getElementById('modal-results').innerHTML =
        '<div style="text-align:center;padding:20px;color:#999"><div class="loading-spinner" style="margin:0 auto 8px"></div>불러오는 중...</div>';

    const params = new URLSearchParams();
    params.append('page', 0); params.append('size', 10);
    if (q) params.append('q', q);
    if (cats.length === 1) params.append('categoryId', cats[0]);
    else if (cats.length > 1) cats.forEach(id => params.append('categoryIds', id));

    const data = await api('/api/cards?' + params.toString());
    const results = data?.content ?? [];
    document.getElementById('modal-result-title').textContent = `검색 결과 (${data?.totalCount ?? 0}건)`;

    if (!results.length) {
        document.getElementById('modal-results').innerHTML = '<div style="text-align:center;color:#999;padding:20px">결과가 없습니다.</div>';
        return;
    }
    const modalResultsEl = document.getElementById('modal-results');
    modalResultsEl.innerHTML = results.map(card => {
        const ne = (card.cardName ?? '').replace(/"/g, '&quot;');
        const ce = (card.companyName ?? '').replace(/"/g, '&quot;');
        const th = card.thumbnailUrl ? `<img src="${card.thumbnailUrl}" alt="${card.cardName}" onerror="this.style.display='none'" style="width:100%;height:100%;object-fit:cover;">` : '';
        return `
      <div class="modal-result-item" data-card-id="${card.cardId}">
        <div class="modal-result-thumb">${th}</div>
        <div class="modal-result-info">
          <div class="modal-result-name">${card.cardName ?? ''}</div>
          <div class="modal-result-company">${card.companyName ?? ''} · ${cardTypeBadge(card.cardType)}</div>
          ${card.topBenefit ? `<div class="modal-result-benefit">${card.topBenefit}</div>` : ''}
        </div>
        <div class="modal-result-actions">
          <button class="btn-m-detail"  data-card-id="${card.cardId}">상세</button>
          <button class="btn-m-compare" data-card-id="${card.cardId}" data-card-name="${ne}" data-company-name="${ce}">비교</button>
        </div>
      </div>`;
    }).join('');
    // 이벤트 위임
    modalResultsEl.addEventListener('click', (e) => {
        const detailBtn = e.target.closest('.btn-m-detail');
        const compareBtn = e.target.closest('.btn-m-compare');
        const row = e.target.closest('.modal-result-item');
        if (compareBtn) {
            e.stopPropagation();
            addToCart(
                Number(compareBtn.dataset.cardId),
                compareBtn.dataset.cardName ?? '',
                compareBtn.dataset.companyName ?? ''
            );
            closeSearchModal();
            return;
        }
        if (detailBtn || row) {
            const id = (detailBtn ?? row).dataset.cardId;
            if (id) { closeSearchModal(); location.href = `/card/${id}`; }
        }
    });
}

// ★ 상세 이동: /card-detail.html → /card/
function goDetail(cardId) {
    closeSearchModal();
    location.href = `/card/${cardId}`;
}

// ── 설문 기반 추천 ──
const SURVEY = [
    {
        q: '한 달 지출 중 가장 많은 항목은?', options: [
            { label: '🚗 교통/주유', scores: { OIL: 2, TRANSPORT: 2 } },
            { label: '🛍️ 쇼핑/마트', scores: { SHOPPING: 2, MART: 1 } },
            { label: '🍽️ 식음료/카페', scores: { DINING: 2, CONVENIENCE: 1 } },
            { label: '✈️ 여행/레저', scores: { TRAVEL: 2, LEISURE: 1 } },
        ]
    },
    {
        q: '가장 원하는 혜택은?', options: [
            { label: '💰 현금처럼 쓰는 캐시백', scores: { CASHBACK: 2 } },
            { label: '✈️ 마일리지/포인트 적립', scores: { TRAVEL: 2 } },
            { label: '🏷️ 바로 청구할인', scores: { SHOPPING: 1, DINING: 1, OIL: 1 } },
            { label: '🆓 연회비 없이 사용', scores: { CASHBACK: 1, CONVENIENCE: 1 } },
        ]
    },
    {
        q: '주로 어디서 카드를 쓰나요?', options: [
            { label: '🏙️ 오프라인 매장 위주', scores: { SHOPPING: 1, DINING: 1, MART: 1 } },
            { label: '🖥️ 온라인/앱 결제 위주', scores: { ONLINE: 2, DELIVERY: 1 } },
            { label: '🚌 대중교통 자주 이용', scores: { TRANSPORT: 2, TOLL: 1 } },
            { label: '⚕️ 의료/보험 자주 이용', scores: { MEDICAL: 2, INSURANCE: 1 } },
        ]
    },
];
const codeLabel = {
    OIL: '주유', TRANSPORT: '교통', SHOPPING: '쇼핑', MART: '마트', DINING: '식음료',
    CONVENIENCE: '편의점', TRAVEL: '여행', LEISURE: '레저', CASHBACK: '캐시백',
    ONLINE: '온라인', DELIVERY: '배달', TOLL: '통행료', MEDICAL: '의료', INSURANCE: '보험',
};
let surveyState = { step: 0, scores: {} };
let catIdMap = {};

async function openSurvey() {
    surveyState = { step: 0, scores: {} };
    catIdMap = {};
    const cats = state.categories.length ? state.categories : await api('/api/cards/categories') ?? [];
    cats.forEach(c => { if (c.categoryCode && c.categoryId) catIdMap[c.categoryCode] = c.categoryId; });
    document.getElementById('survey-overlay').classList.add('open');
    document.body.style.overflow = 'hidden';
    renderSurveyStep();
}
function closeSurvey() {
    document.getElementById('survey-overlay').classList.remove('open');
    document.body.style.overflow = '';
}
function handleSurveyOverlay(e) {
    if (e.target === document.getElementById('survey-overlay')) closeSurvey();
}
function renderSurveyStep() {
    const step = SURVEY[surveyState.step];
    const total = SURVEY.length;
    document.getElementById('survey-step-label').textContent = `${surveyState.step + 1} / ${total}`;
    document.getElementById('survey-question').textContent = step.q;
    document.getElementById('survey-progress').style.width = `${((surveyState.step + 1) / total) * 100}%`;
    document.getElementById('survey-options').innerHTML = step.options.map((o, i) =>
        `<button class="survey-option" onclick="answerSurvey(${i})">${o.label}</button>`
    ).join('');
}
function answerSurvey(idx) {
    const step = SURVEY[surveyState.step];
    const scores = step.options[idx].scores;
    Object.entries(scores).forEach(([code, score]) => {
        surveyState.scores[code] = (surveyState.scores[code] ?? 0) + score;
    });
    surveyState.step++;
    if (surveyState.step < SURVEY.length) { renderSurveyStep(); return; }
    const topCode = Object.entries(surveyState.scores).sort((a, b) => b[1] - a[1])[0]?.[0];
    const categoryId = catIdMap[topCode] ?? null;
    closeSurvey();
    showSurveyResult(topCode, categoryId);
}
async function showSurveyResult(topCode, categoryId) {
    const label = codeLabel[topCode] ?? topCode;
    toast(`🎯 "${label}" 혜택 카드를 추천해드립니다!`);
    if (categoryId) {
        state.currentCategoryIds = [String(categoryId)];
        document.querySelectorAll('#category-bar .cat-btn').forEach(b => {
            b.classList.toggle('active', b.dataset.id === String(categoryId));
        });
        document.getElementById('cat-selected-info').textContent = `🎯 설문 추천: ${label} 혜택 카드`;
    }
    document.getElementById('card-list').scrollIntoView({ behavior: 'smooth' });
    await loadCards(0);
}

// ── 키보드 단축키 ──
document.addEventListener('keydown', e => {
    if (e.key === 'Escape') { closeSearchModal(); closeCompareModal(); closeSurvey(); }
});
document.getElementById('hero-search').addEventListener('keydown', e => {
    if (e.key === 'Enter') openSearchModal(e.target.value.trim());
});

// ── 초기화 ──
(async function init() {
    await loadCategories();
    await loadPopularKeywords();
    await loadTop3();
    await loadCards(0);

    // 카드 그리드 이벤트 위임 — 1회만 등록
    document.getElementById('card-grid')?.addEventListener('click', handleCardClick);

    // 비교 카트
    document.getElementById('btn-do-compare').addEventListener('click', () => doCompare());
    document.getElementById('btn-cart-toggle').addEventListener('click', toggleCart);
    document.getElementById('btn-close-compare').addEventListener('click', closeCompareModal);
    document.getElementById('compare-modal-overlay').addEventListener('click', handleCompareOverlayClick);

    // 검색
    document.getElementById('btn-hero-search').addEventListener('click', () =>
        openSearchModal(document.getElementById('hero-search').value.trim()));
    document.getElementById('btn-close-search').addEventListener('click', closeSearchModal);
    document.getElementById('search-overlay').addEventListener('click', handleOverlayClick);
    document.getElementById('btn-modal-search').addEventListener('click', runModalSearch);
    document.getElementById('modal-search-input').addEventListener('keydown', e => {
        if (e.key === 'Enter') runModalSearch();
    });

    // 필터
    document.getElementById('btn-filter-search').addEventListener('click', () => loadCards(0));
    document.getElementById('btn-filter-reset').addEventListener('click', resetFilter);
    document.getElementById('filter-q').addEventListener('keydown', e => {
        if (e.key === 'Enter') loadCards(0);
    });

    // 설문
    document.getElementById('btn-open-survey').addEventListener('click', openSurvey);
    document.getElementById('btn-close-survey').addEventListener('click', closeSurvey);
    document.getElementById('survey-overlay').addEventListener('click', handleSurveyOverlay);

    // 카드 타입 탭
    document.querySelectorAll('.type-tab').forEach(btn =>
        btn.addEventListener('click', () => selectCardType(btn, btn.dataset.type)));
})();