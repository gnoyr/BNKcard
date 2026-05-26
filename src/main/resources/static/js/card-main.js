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
  TRAVEL:'✈️', DINING:'🍽️', SHOPPING:'🛍️', OIL:'⛽', TRANSPORT:'🚇',
  LEISURE:'🎭', MEDICAL:'💊', TELECOM:'📱', ONLINE:'💻', DELIVERY:'🛵',
  EDUCATION:'📚', LIVING:'🏠', INSURANCE:'🛡️', CASHBACK:'💰', SPORT:'⚽',
  CONVENIENCE:'🏪', TOLL:'🛣️', PARKING:'🅿️', BEAUTY:'💄', MART:'🏬',
  LOCAL:'🏘️', BUSINESS:'💼', ETC:'✨',
};

function toast(msg, dur=2200) {
  const el=document.getElementById('toast');
  el.textContent=msg; el.classList.add('show');
  setTimeout(()=>el.classList.remove('show'),dur);
}
function fmtFee(v) {
  if(v===null||v===undefined) return '-';
  return Number(v)===0?'연회비 없음':Number(v).toLocaleString()+'원';
}
function cardTypeBadge(t) {
  return {CREDIT:'신용',CHECK:'체크',PREPAID:'선불',HYBRID:'하이브리드'}[t]??t;
}
function safeImgHtml(url,alt) {
  if(!url) return `<div class="card-img-placeholder">${alt}</div>`;
  const e=alt.replace(/"/g,'&quot;').replace(/'/g,'&#39;');
  return `<img src="${url}" alt="${e}" onerror="this.style.display='none';this.parentElement.innerHTML='<div class=\\'card-img-placeholder\\'>${e}</div>'">`;
}
async function api(url) {
  try {
    const res=await fetch(url,{credentials:'include'});
    if(!res.ok) return null;
    return (await res.json())?.data??null;
  } catch { return null; }
}

// 카테고리 로드
async function loadCategories() {
  const data=await api('/api/cards/categories');
  if(!data?.length) return;
  state.categories=data;
  document.getElementById('category-bar').innerHTML=data.map(c=>`
    <button class="cat-btn" data-id="${c.categoryId}" data-name="${c.categoryName}"
            onclick="toggleCategory(this,'${c.categoryId}','${c.categoryName}')">
      ${CAT_EMOJI[c.categoryCode]??'🔹'} ${c.categoryName}
    </button>`).join('');
  document.getElementById('modal-cat-grid').innerHTML=data.slice(0,12).map(c=>`
    <button class="modal-cat-btn" data-id="${c.categoryId}" data-name="${c.categoryName}"
            onclick="toggleModalCategory(this,'${c.categoryId}','${c.categoryName}')">
      <span class="cat-em">${CAT_EMOJI[c.categoryCode]??'🔹'}</span>${c.categoryName}
    </button>`).join('');
}

function toggleCategory(btn,catId,catName) {
  const idx=state.currentCategoryIds.indexOf(catId);
  if(idx===-1){state.currentCategoryIds.push(catId);btn.classList.add('active');}
  else{state.currentCategoryIds.splice(idx,1);btn.classList.remove('active');}
  updateCatInfo('cat-selected-info',state.currentCategoryIds);
  loadCards(0);
}
function updateCatInfo(elId,ids) {
  const el=document.getElementById(elId);
  if(!ids.length){el.textContent='';return;}
  const names=ids.map(id=>state.categories.find(c=>String(c.categoryId)===String(id))?.categoryName??id);
  el.textContent='선택된 혜택 (AND): '+names.join(' + ');
}
function selectCardType(btn,cardType) {
  document.querySelectorAll('.type-tab').forEach(b=>b.classList.remove('active'));
  btn.classList.add('active');
  state.currentCardType=cardType;
  loadCards(0);
}

// 인기 검색어
async function loadPopularKeywords() {
  const data=await api('/api/search/keywords/popular');
  const wrap=document.getElementById('popular-keywords');
  if(!data?.length) return;
  data.slice(0,8).forEach(kw=>{
    const keyword=kw.keyword??kw.keywordRaw??(typeof kw==='string'?kw:'');
    if(!keyword) return;
    const tag=document.createElement('button');
    tag.className='kw-tag'; tag.textContent=keyword;
    tag.onclick=()=>openSearchModal(keyword);
    wrap.appendChild(tag);
  });
}

// TOP3 포디움
async function loadTop3() {
  const container=document.getElementById('top3-podium');
  const data=await api('/api/cards/top3?surveyResult=');
  if(!data?.length){
    container.innerHTML='<div class="empty-wrap">추천 카드를 불러올 수 없습니다.</div>';
    return;
  }
  const ordered=[
    {card:data[1],cls:'left',rank:2},
    {card:data[0],cls:'center',rank:1},
    {card:data[2],cls:'right',rank:3},
  ].filter(o=>o.card);
  container.innerHTML=ordered.map(o=>`
    <div class="podium-item ${o.cls}">
      <div class="podium-flip">${buildFlipCard(o.card,o.rank)}</div>
    </div>`).join('');
}

// 카드 목록
async function loadCards(page=0) {
  state.currentPage=page;
  const grid=document.getElementById('card-grid');
  grid.innerHTML=`<div class="loading-wrap" style="grid-column:1/-1">
    <div class="loading-spinner"></div><p>불러오는 중...</p></div>`;

  const q=( document.getElementById('filter-q')?.value??'').trim();
  const type=state.currentCardType;
  const catIds=state.currentCategoryIds;

  const params=new URLSearchParams();
  params.append('page',page); params.append('size',state.PAGE_SIZE);
  if(q) params.append('q',q);
  if(type) params.append('cardType',type);
  if(catIds.length===1) params.append('categoryId',catIds[0]);
  else if(catIds.length>1) catIds.forEach(id=>params.append('categoryIds',id));

  const data=await api('/api/cards?'+params.toString());

  if(!data?.content?.length){
    grid.innerHTML=`<div class="empty-wrap" style="grid-column:1/-1">🔍 조건에 맞는 카드가 없습니다.</div>`;
    document.getElementById('filter-count').textContent='';
    renderPagination(0); return;
  }
  const typeLabel={CREDIT:'신용카드',CHECK:'체크카드',PREPAID:'선불카드'}[type]??'';
  document.getElementById('filter-count').textContent=
    `${typeLabel?typeLabel+' ':''}총 ${(data.totalCount??0).toLocaleString()}개`;
  grid.innerHTML=data.content.map(c=>buildFlipCard(c)).join('');
  renderPagination(data.totalCount??0);
}

// 카드 플립 빌더 (정보 강화)
function buildFlipCard(card,rank=null) {
  const rankBadge=rank?`<div class="top3-rank rank-${rank}">${rank}</div>`:'';
  const imgHtml=safeImgHtml(card.thumbnailUrl,card.cardName??'');
  const benefitText=card.topBenefit??'';
  const benefitTags=benefitText
    ?benefitText.split(/[,\/]\s*/).slice(0,2).map(t=>t.trim()).filter(Boolean)
      .map(t=>`<span style="display:inline-block;background:#e8f0fe;color:#003087;border-radius:10px;padding:1px 7px;font-size:10px;margin:1px;">${t}</span>`)
      .join(''):'';
  const feeDisplay=card.annualFeeDomestic===0||card.annualFeeDomestic===null
    ?'<span style="color:#00875a;font-weight:700;">연회비 없음</span>'
    :Number(card.annualFeeDomestic).toLocaleString()+'원';
  const backText=card.topBenefit??card.summaryDescription??'다양한 혜택 제공';
  const lines=backText.split(/[,\.\n]\s*/).slice(0,3).map(b=>b.trim()).filter(Boolean)
    .map(b=>`<div class="benefit-item"><span class="benefit-dot"></span>${b}</div>`).join('');
  const ne=(card.cardName??'').replace(/'/g,"\\'").replace(/"/g,'&quot;');
  const ce=(card.companyName??'').replace(/'/g,"\\'").replace(/"/g,'&quot;');
  return `
    <div class="flip-wrapper">
      <div class="flip-inner">
        <div class="flip-front">
          ${rankBadge}
          <div class="card-img-wrap">${imgHtml}</div>
          <div class="card-front-info">
            <span class="card-type-badge">${cardTypeBadge(card.cardType)}</span>
            <div class="card-name">${card.cardName??''}</div>
            <div class="card-company">${card.companyName??'BNK부산은행'}</div>
            <div class="card-fee">${feeDisplay}</div>
            ${benefitTags?`<div style="margin-top:5px;line-height:1.8;">${benefitTags}</div>`:''}
          </div>
        </div>
        <div class="flip-back">
          <div class="back-card-name">${card.cardName??''}</div>
          <div class="back-benefit">${lines||backText}</div>
          <div class="back-actions">
            <button class="btn-detail" onclick="event.stopPropagation();location.href='/card-detail.html?cardId=${card.cardId}'">자세히</button>
            <button class="btn-compare" onclick="event.stopPropagation();addToCart(${card.cardId},'${ne}','${ce}')">비교</button>
            <button class="btn-apply" onclick="event.stopPropagation();location.href='/card-detail.html?cardId=${card.cardId}'">신청</button>
          </div>
        </div>
      </div>
    </div>`;
}

// 페이지네이션
function renderPagination(total) {
  const tp=Math.ceil(total/state.PAGE_SIZE);
  const el=document.getElementById('pagination');
  if(tp<=1){el.innerHTML='';return;}
  const cur=state.currentPage,s=Math.max(0,cur-2),e=Math.min(tp-1,s+4);
  let h=`<button class="page-btn" onclick="loadCards(${cur-1})" ${cur===0?'disabled':''}>‹</button>`;
  for(let i=s;i<=e;i++) h+=`<button class="page-btn ${i===cur?'active':''}" onclick="loadCards(${i})">${i+1}</button>`;
  h+=`<button class="page-btn" onclick="loadCards(${cur+1})" ${cur>=tp-1?'disabled':''}>›</button>`;
  el.innerHTML=h;
}

// 필터 초기화
function resetFilter() {
  document.getElementById('filter-q').value='';
  state.currentCardType=''; state.currentCategoryIds=[];
  document.querySelectorAll('.type-tab').forEach(b=>b.classList.remove('active'));
  document.querySelector('.type-tab[data-type=""]')?.classList.add('active');
  document.querySelectorAll('#category-bar .cat-btn').forEach(b=>b.classList.remove('active'));
  document.getElementById('cat-selected-info').textContent='';
  loadCards(0);
}

// 비교 카트
function toggleCart() {
  state.cartOpen=!state.cartOpen;
  document.getElementById('compare-cart').classList.toggle('open',state.cartOpen);
}
function addToCart(cardId,cardName,companyName) {
  if(state.compareCart.find(c=>c.cardId===cardId)){toast('이미 비교 카트에 있는 카드입니다.');return;}
  if(state.compareCart.length>=3){toast('최대 3개까지 비교할 수 있습니다.');return;}
  state.compareCart.push({cardId,cardName,companyName});
  renderCart();
  if(!state.cartOpen) toggleCart();
  toast(`"${cardName}" 비교 카트에 추가됐습니다.`);
}
function removeFromCart(cardId) {
  state.compareCart=state.compareCart.filter(c=>c.cardId!==cardId);
  renderCart();
}
function renderCart() {
  const cnt=state.compareCart.length;
  document.getElementById('cart-count').textContent=`(${cnt}/3)`;
  document.getElementById('btn-do-compare').disabled=cnt<2;
  const w=document.getElementById('cart-items');
  if(!cnt){w.innerHTML='<div class="cart-empty">카드를 추가하세요</div>';return;}
  w.innerHTML=state.compareCart.map(c=>`
    <div class="cart-item">
      <div class="cart-item-name">${c.cardName}<br>
        <span style="color:#999;font-size:11px">${c.companyName}</span></div>
      <button class="cart-remove" onclick="removeFromCart(${c.cardId})">✕</button>
    </div>`).join('');
}

// 비교 모달
function openCompareModal() {
  document.getElementById('compare-modal-overlay').classList.add('open');
  document.body.style.overflow='hidden';
}
function closeCompareModal() {
  document.getElementById('compare-modal-overlay').classList.remove('open');
  document.body.style.overflow='';
}
function handleCompareOverlayClick(e) {
  if(e.target===document.getElementById('compare-modal-overlay')) closeCompareModal();
}

async function doCompare() {
  const cardIds=state.compareCart.map(c=>c.cardId);
  if(cardIds.length<2){toast('2개 이상 선택하세요.');return;}
  openCompareModal();
  document.getElementById('compare-modal-body').innerHTML=
    '<div class="compare-empty"><div class="loading-spinner" style="margin:0 auto 12px"></div>불러오는 중...</div>';
  const res=await fetch('/api/cards/compare',{
    method:'POST',headers:{'Content-Type':'application/json'},
    credentials:'include',body:JSON.stringify({cardIds})
  });
  const json=await res.json().catch(()=>null);
  if(!json?.data?.length){
    document.getElementById('compare-modal-body').innerHTML='<div class="compare-empty">비교 데이터를 불러올 수 없습니다.</div>';
    return;
  }
  renderCompareModal(json.data);
  if(state.cartOpen) toggleCart();
}

function renderCompareModal(cards) {
  const fees=cards.map(c=>Number(c.annualFeeDomestic)||0);
  const minFee=Math.min(...fees);
  const oFees=cards.map(c=>Number(c.annualFeeOverseas)||0);
  const minO=Math.min(...oFees);
  const bCnts=cards.map(c=>(c.benefits??[]).length);
  const maxB=Math.max(...bCnts);

  const headerCells=cards.map((c,i)=>`
    <th style="position:relative;">
      ${bCnts[i]===maxB&&maxB>0?'<div style="position:absolute;top:8px;right:8px;background:#ffd700;color:#003087;border-radius:12px;padding:2px 8px;font-size:10px;font-weight:700;">★ 혜택 최다</div>':''}
      ${c.thumbnailUrl?`<img class="compare-card-img" src="${c.thumbnailUrl}" alt="${c.cardName}" onerror="this.style.display='none'">`:`<span class="compare-card-img-placeholder"></span>`}
      <br><span style="font-size:13px;font-weight:700;">${c.cardName}</span>
      <button class="compare-remove-btn" onclick="removeFromCartAndRefresh(${c.cardId})">✕ 제거</button>
    </th>`).join('');

  function feeCell(val,minVal){
    const n=Number(val)||0,d=n===0?'연회비 없음':n.toLocaleString()+'원';
    return n===minVal
      ?`<td style="color:#00875a;font-weight:700;">${d}<br><span style="font-size:10px;background:#e8f8f0;color:#00875a;border-radius:4px;padding:1px 6px;">최저</span></td>`
      :`<td>${d}</td>`;
  }

  const rows=[
    `<tr><td>카드사</td>${cards.map(c=>`<td>${c.companyName??'-'}</td>`).join('')}</tr>`,
    `<tr><td>유형</td>${cards.map(c=>`<td>${cardTypeBadge(c.cardType)}</td>`).join('')}</tr>`,
    `<tr><td>국내 연회비</td>${cards.map(c=>feeCell(c.annualFeeDomestic,minFee)).join('')}</tr>`,
    `<tr><td>해외 연회비</td>${cards.map(c=>feeCell(c.annualFeeOverseas,minO)).join('')}</tr>`,
    `<tr><td>혜택 수</td>${cards.map((c,i)=>`<td style="${bCnts[i]===maxB&&maxB>0?'color:#003087;font-weight:700;':''}">${bCnts[i]}개${bCnts[i]===maxB&&maxB>0?' ★':''}</td>`).join('')}</tr>`,
    `<tr><td>주요 혜택</td>${cards.map((c,i)=>`<td style="${bCnts[i]===maxB&&maxB>0?'background:#fffde7;':''}text-align:left;">${(c.benefits??[]).slice(0,3).map(b=>`• ${b.displayText??b.benefitTitle??''}`).join('<br>')||'-'}</td>`).join('')}</tr>`,
  ].join('');

  document.getElementById('compare-modal-body').innerHTML=`
    <div style="margin-bottom:12px;font-size:12px;color:#888;">
      💡 <span style="color:#00875a;font-weight:600;">초록색</span> = 연회비 최저 &nbsp;|&nbsp;
         <span style="color:#003087;font-weight:600;">★</span> = 혜택 최다
    </div>
    <table class="compare-table">
      <thead><tr><th>항목</th>${headerCells}</tr></thead>
      <tbody>${rows}</tbody>
    </table>
    <p style="margin-top:16px;font-size:12px;color:#999;text-align:right;">카드를 제거하면 다시 비교하기 버튼을 눌러주세요.</p>`;
}

function removeFromCartAndRefresh(cardId) {
  removeFromCart(cardId);
  if(state.compareCart.length<2){
    document.getElementById('compare-modal-body').innerHTML=
      '<div class="compare-empty">비교할 카드가 2개 이상 필요합니다.<br>카드를 더 추가해주세요.</div>';
    return;
  }
  doCompare();
}

// 검색 모달
async function openSearchModal(keyword='') {
  document.getElementById('search-overlay').classList.add('open');
  const input=document.getElementById('modal-search-input');
  input.value=keyword; input.focus();
  document.body.style.overflow='hidden';
  const suggest=await api('/api/search/keywords/suggest');
  const suggestEl=document.getElementById('suggest-list');
  if(suggest?.length){
    suggestEl.innerHTML=suggest.slice(0,12).map(kw=>`
      <button class="suggest-tag" onclick="modalSetKeyword('${(kw.keyword??kw).replace(/'/g,"\\'")}')">
        ${kw.keyword??kw}</button>`).join('');
  } else {
    suggestEl.innerHTML='<span style="color:#bbb;font-size:13px">추천 검색어가 없습니다.</span>';
  }
  if(keyword) runModalSearch();
}
function closeSearchModal() {
  document.getElementById('search-overlay').classList.remove('open');
  document.body.style.overflow='';
  state.modalCategoryIds=[];
  document.querySelectorAll('.modal-cat-btn').forEach(b=>b.classList.remove('active'));
  document.getElementById('modal-cat-selected').textContent='';
}
function handleOverlayClick(e) {
  if(e.target===document.getElementById('search-overlay')) closeSearchModal();
}
function modalSetKeyword(kw) {
  document.getElementById('modal-search-input').value=kw;
  runModalSearch();
}
function toggleModalCategory(btn,catId,catName) {
  const idx=state.modalCategoryIds.indexOf(catId);
  if(idx===-1){state.modalCategoryIds.push(catId);btn.classList.add('active');}
  else{state.modalCategoryIds.splice(idx,1);btn.classList.remove('active');}
  updateCatInfo('modal-cat-selected',state.modalCategoryIds);
  runModalSearch();
}

async function runModalSearch() {
  const q=(document.getElementById('modal-search-input')?.value??'').trim();
  const cats=state.modalCategoryIds??[];
  if(!q&&cats.length===0){
    document.getElementById('modal-results-wrap').style.display='none'; return;
  }
  document.getElementById('modal-results-wrap').style.display='';
  document.getElementById('modal-result-title').textContent='검색 중...';
  document.getElementById('modal-results').innerHTML=
    '<div style="text-align:center;padding:20px;color:#999"><div class="loading-spinner" style="margin:0 auto 8px"></div>불러오는 중...</div>';

  const params=new URLSearchParams();
  params.append('page',0); params.append('size',10);
  if(q) params.append('q',q);
  if(cats.length===1) params.append('categoryId',cats[0]);
  else if(cats.length>1) cats.forEach(id=>params.append('categoryIds',id));

  const data=await api('/api/cards?'+params.toString());
  const results=data?.content??[];
  document.getElementById('modal-result-title').textContent=`검색 결과 (${data?.totalCount??0}건)`;

  if(!results.length){
    document.getElementById('modal-results').innerHTML='<div style="text-align:center;color:#999;padding:20px">결과가 없습니다.</div>';
    return;
  }
  document.getElementById('modal-results').innerHTML=results.map(card=>{
    const ne=(card.cardName??'').replace(/'/g,"\\'");
    const ce=(card.companyName??'').replace(/'/g,"\\'");
    const th=card.thumbnailUrl?`<img src="${card.thumbnailUrl}" alt="${card.cardName}" onerror="this.style.display='none'" style="width:100%;height:100%;object-fit:cover;">`:'';
    return `
      <div class="modal-result-item" onclick="goDetail(${card.cardId})">
        <div class="modal-result-thumb">${th}</div>
        <div class="modal-result-info">
          <div class="modal-result-name">${card.cardName??''}</div>
          <div class="modal-result-company">${card.companyName??''} · ${cardTypeBadge(card.cardType)}</div>
          ${card.topBenefit?`<div class="modal-result-benefit">${card.topBenefit}</div>`:''}
        </div>
        <div class="modal-result-actions" onclick="event.stopPropagation()">
          <button class="btn-m-detail" onclick="goDetail(${card.cardId})">상세</button>
          <button class="btn-m-compare" onclick="addToCart(${card.cardId},'${ne}','${ce}');closeSearchModal()">비교</button>
        </div>
      </div>`;
  }).join('');
}

function goDetail(cardId){closeSearchModal();location.href=`/card-detail.html?cardId=${cardId}`;}

// 설문 기반 추천
const SURVEY=[
  {q:'한 달 지출 중 가장 많은 항목은?',options:[
    {label:'🚗 교통/주유',scores:{OIL:2,TRANSPORT:2}},
    {label:'🛍️ 쇼핑/마트',scores:{SHOPPING:2,MART:1}},
    {label:'🍽️ 식음료/카페',scores:{DINING:2,CONVENIENCE:1}},
    {label:'✈️ 여행/레저',scores:{TRAVEL:2,LEISURE:1}},
  ]},
  {q:'가장 원하는 혜택은?',options:[
    {label:'💰 현금처럼 쓰는 캐시백',scores:{CASHBACK:2}},
    {label:'✈️ 마일리지/포인트 적립',scores:{TRAVEL:2}},
    {label:'🏷️ 바로 청구할인',scores:{SHOPPING:1,DINING:1,OIL:1}},
    {label:'🆓 연회비 없이 사용',scores:{CASHBACK:1,CONVENIENCE:1}},
  ]},
  {q:'주로 어디서 카드를 쓰나요?',options:[
    {label:'🏪 편의점/배달앱',scores:{CONVENIENCE:2,DELIVERY:2}},
    {label:'⛽ 주유소',scores:{OIL:3,TOLL:1}},
    {label:'🏬 대형마트/온라인',scores:{MART:2,ONLINE:1}},
    {label:'🎭 문화/여가',scores:{LEISURE:2,SPORT:1}},
  ]},
];
const surveyState={step:0,scores:{}};
const catIdMap={TRAVEL:1,DINING:2,SHOPPING:3,OIL:4,TRANSPORT:5,LEISURE:6,CASHBACK:14,CONVENIENCE:16,DELIVERY:10,MART:20,ONLINE:9,SPORT:15,TOLL:17};
const codeLabel={TRAVEL:'여행/항공',DINING:'식음료/카페',SHOPPING:'쇼핑',OIL:'주유',TRANSPORT:'교통',CASHBACK:'캐시백/포인트',CONVENIENCE:'편의점',DELIVERY:'배달',MART:'마트',LEISURE:'레저',ONLINE:'온라인',SPORT:'스포츠',TOLL:'하이패스'};

function openSurvey(){
  surveyState.step=0; surveyState.scores={};
  document.getElementById('survey-overlay').classList.add('open');
  document.body.style.overflow='hidden';
  renderSurveyStep();
}
function closeSurvey(){
  document.getElementById('survey-overlay').classList.remove('open');
  document.body.style.overflow='';
}
function handleSurveyOverlay(e){
  if(e.target===document.getElementById('survey-overlay')) closeSurvey();
}
function renderSurveyStep(){
  const q=SURVEY[surveyState.step];
  const prog=((surveyState.step+1)/SURVEY.length*100)+'%';
  document.getElementById('survey-step-label').textContent=`${surveyState.step+1} / ${SURVEY.length}`;
  document.getElementById('survey-question').textContent=q.q;
  document.getElementById('survey-progress').style.width=prog;
  document.getElementById('survey-options').innerHTML=q.options.map((opt,i)=>`
    <button onclick="selectSurveyOption(${i})"
      style="padding:14px 20px;border:1.5px solid #dde3ef;border-radius:12px;
             font-size:14px;text-align:left;cursor:pointer;background:#fff;
             transition:all .15s;font-family:inherit;width:100%;"
      onmouseover="this.style.borderColor='#003087';this.style.background='#e8f0fe'"
      onmouseout="this.style.borderColor='#dde3ef';this.style.background='#fff'">
      ${opt.label}
    </button>`).join('');
}
function selectSurveyOption(optIdx){
  const opt=SURVEY[surveyState.step].options[optIdx];
  Object.entries(opt.scores).forEach(([code,score])=>{
    surveyState.scores[code]=(surveyState.scores[code]??0)+score;
  });
  surveyState.step++;
  if(surveyState.step<SURVEY.length){renderSurveyStep();return;}
  const topCode=Object.entries(surveyState.scores).sort((a,b)=>b[1]-a[1])[0]?.[0];
  const categoryId=catIdMap[topCode]??null;
  closeSurvey();
  showSurveyResult(topCode,categoryId);
}
async function showSurveyResult(topCode,categoryId){
  const label=codeLabel[topCode]??topCode;
  toast(`🎯 "${label}" 혜택 카드를 추천해드립니다!`);
  if(categoryId){
    state.currentCategoryIds=[String(categoryId)];
    document.querySelectorAll('#category-bar .cat-btn').forEach(b=>{
      b.classList.toggle('active',b.dataset.id===String(categoryId));
    });
    document.getElementById('cat-selected-info').textContent=`🎯 설문 추천: ${label} 혜택 카드`;
  }
  document.getElementById('card-list').scrollIntoView({behavior:'smooth'});
  await loadCards(0);
}

document.addEventListener('keydown',e=>{
  if(e.key==='Escape'){closeSearchModal();closeCompareModal();closeSurvey();}
});
document.getElementById('hero-search').addEventListener('keydown',e=>{
  if(e.key==='Enter') openSearchModal(e.target.value.trim());
});

(async function init(){
  await loadCategories();
  await loadPopularKeywords();
  await loadTop3();
  await loadCards(0);
})();