# BNK 부산은행 금융 상품 플랫폼

![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.x-6DB33F?logo=springboot&logoColor=white)
![HTML5](https://img.shields.io/badge/HTML5-E34F26?logo=html5&logoColor=white)
![CSS3](https://img.shields.io/badge/CSS3-1572B6?logo=css3&logoColor=white)
![Oracle](https://img.shields.io/badge/Oracle-DB-F80000?logo=oracle&logoColor=white)
![MyBatis](https://img.shields.io/badge/MyBatis-000000)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?logo=springsecurity&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?logo=jsonwebtokens&logoColor=white)
![OCI](https://img.shields.io/badge/Oracle_Cloud-F80000?logo=oracle&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white)
![Nginx](https://img.shields.io/badge/Nginx-009639?logo=nginx&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?logo=githubactions&logoColor=white)
![Google AI](https://img.shields.io/badge/Google_AI-4285F4?logo=google&logoColor=white)
![Ollama](https://img.shields.io/badge/Ollama-000000?logo=ollama&logoColor=white)
![Qdrant](https://img.shields.io/badge/Qdrant-DC244C?logo=qdrant&logoColor=white)
 
> BNK 부산은행 카드 상품 조회·신청·심사 통합 플랫폼  
> Java 21 · Spring Boot 4.x · MyBatis · Oracle · OCI · Docker · Nginx · GitHub Actions · Google AI · Ollama · Qdrant
 
---

## 프로젝트 도메인

www.bnkcard.store

---

## 프로젝트 소개

BNK 부산은행의 금융 상품(카드) 플랫폼으로, 사용자 회원 인증부터 카드 상품 조회·검색·비교  
관리자 카드 등록 신청·결재·사용자 노출 프로세스 설계  
소비패턴 기반 맞춤형 카드 추천 기능 제공  
AI 챗봇 카드 안내

---

## 주요 기능

#### 회원 / 인증
- 회원가입, 이메일 인증, 로그인 / 로그아웃
- JWT 액세스·리프레시 토큰 발급 및 갱신
- 아이디 찾기 (이름 + 전화번호), 비밀번호 재설정
- 개인정보 AES 암호화 · 마스킹 처리

#### 카드 상품
- 카드 목록 조회 / 상세 조회 / 조회수 집계
- 카드 비교(연회비, 카드 유형, 혜택 수, 주요 혜택)
- 비회원 → 조회수 기반, 회원 → 소비패턴 기반 TOP3 맞춤 추천

#### AI 추천 · 소비패턴 분석
- 카테고리별 월 소비금액 입력 및 자동 분석
- Google AI + Ollama + Qdrant VectorStore 기반 카드 임베딩
- AI 채팅봇을 통한 카드 상품 안내 및 추천 (비로그인 허용)

#### 검색
- 키워드 기반 카드 검색
- 인기 검색어 TOP 10
- 관리자 등록 추천 검색어 표시

#### 관리자
- 카드 상품 등록 · 수정 신청 (3단계 입력: 기본정보 → 혜택 → 이미지)
- 카드 버전 관리 · 상태 변경 이력
- 약관 등록 · 버전 관리 · 상태 변경 이력
- 카드 · 약관 등록 결재
- 회원 다중 조건 검색 및 상세 조회
- 대시보드: 인기 카드 TOP3, 최근 가입 회원, 결재 대기 현황
 
---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Backend | Java 21, Spring Boot 4.x |
| Frontend | HTML5, CSS3 |
| Database | Oracle Database, Redis |
| ORM | MyBatis Framework |
| 인증·보안 | Spring Security, JWT |
| 인프라 | Oracle Cloud Infrastructure (OCI), OCI Object Storage |
| 배포 | Docker, Nginx, GitHub Actions |
| AI | Google AI, Ollama, Qdrant VectorStore |

---

## 시스템 아키텍처

<img width="903" height="457" alt="시스템 아키텍처" src="https://github.com/user-attachments/assets/6bd0fdd1-89bb-4a5c-b291-d540c9fef0ad" />

---

## ERD

<img width="800" alt="erd" src="https://github.com/user-attachments/assets/2f0cdcd9-578b-4e2c-9625-ec76cc6e0de8" />

테이블 (총 약 40개)

| 도메인 | 주요 테이블 |
|--------|------------|
| 공통 코드 | COMMON_CODE_GROUPS, COMMON_CODES |
| 회원·인증 | USERS, USER_SESSIONS, LOGIN_HISTORIES |
| 카드 상품 | CARDS, CARD_BENEFITS, CARD_IMAGES, CARD_CONTENTS, CARD_CATEGORIES |
| 약관 | TERMS_MASTER, TERMS, TERMS_FILES |
| 카드 등록 신청·결재 | APPROVAL_REQUESTS, APPROVAL_LINES, CARD_VERSIONS |
| 소비패턴·AI | USER_SPENDING_PATTERNS, AI_CHAT_LOGS |
| 검색 | SEARCH_KEYWORDS, SEARCH_LOGS |
| 관리자 | ADMIN_USERS, ADMIN_ROLES |

---

## 팀원 소개

| 이름 | 역할 | GitHub |
|------|------|--------|
| 김현길 | 약관 / 상품 페이지 | [@gusrlf114](https://github.com/gusrlf114) |
| 김성룡 | 인프라 / 형상관리 / AI | [@gnoyr](https://github.com/gnoyr) |
| 박소현 | 카드 등록 /  결재  / 관리 | [@sohyeon59](https://github.com/sohyeon59) |
| 허윤 | DB / 회원 / 인증 | [@heoheoyun](https://github.com/heoheoyun) |

---

## 프로젝트 보고서 및 발표자료

[최종 보고서](https://drive.google.com/file/d/13p_4iO5GwN-gU1dv0EqyotF48-BJtDcC/view?usp=sharing)  
[발표 자료](https://canva.link/s4c4zdfyxeipdk0)
