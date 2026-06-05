# BNK 부산은행 금융 상품 플랫폼

![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.6-6DB33F?logo=springboot&logoColor=white)
![Oracle](https://img.shields.io/badge/Oracle-DB-F80000?logo=oracle&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?logo=redis&logoColor=white)
![MyBatis](https://img.shields.io/badge/MyBatis-4.0.1-000000)
![Spring AI](https://img.shields.io/badge/Spring_AI-Google_GenAI-4285F4?logo=google&logoColor=white)

> BNK 부산은행 카드 상품 조회·신청·심사 통합 플랫폼  
> Java 21 · Spring Boot 4.0.6 · MyBatis · Oracle · Redis · Spring AI

---

## 프로젝트 소개

BNK 부산은행의 금융 상품(카드) 플랫폼으로, 회원 인증부터 카드 상품 조회·비교·신청·심사까지 전 과정을 지원합니다.  
소비패턴 기반 맞춤형 카드 추천 기능 제공

---

## 주요 기능

| 도메인 | 기능 |
|--------|------|
| 인증 | 회원가입, 이메일 인증, 로그인, JWT 토큰 발급, 비밀번호 재설정 |
| 카드 상품 | 카드 목록 조회, 상세 조회, 카드 비교, 혜택 시뮬레이션 |
| 카드 신청 | 약관 동의, 카드 신청서 제출, 신청 현황 조회 |
| AI 분석 | 소비패턴 분석, AI 채팅 기반 카드 추천 |
| 관리자 | 카드 상품 등록·수정, 신청 심사, 회원 관리, 통계 조회 |
| 검색 | 키워드 검색, 검색어 자동완성, 검색 통계 |
| 알림 | 신청 상태 변경 알림, 푸시 설정 |

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 4.0.6, Spring Security, Spring Validation |
| ORM | MyBatis 4.0.1 |
| Database | Oracle, Redis |
| AI | Spring AI (Google GenAI, Embedding) |
| 인증 | JWT, 이메일 인증 (Spring Mail) |
| 빌드 | Gradle |
| 테스트 | JUnit 5, Mockito, H2 (인메모리) |
| 문서 | Swagger (SpringDoc) |

---

## ERD

```
<img width="5306" height="5104" alt="erd" src="https://github.com/user-attachments/assets/2f0cdcd9-578b-4e2c-9625-ec76cc6e0de8" />

```

주요 테이블 (총 약 40개)

| 도메인 | 주요 테이블 |
|--------|------------|
| 공통 코드 | COMMON_CODE_GROUPS, COMMON_CODES |
| 회원·인증 | USERS, EMAIL_VERIFY, TOKEN_STORE |
| 카드 상품 | CARDS, CARD_BENEFITS, CARD_IMAGES, CARD_CATEGORIES |
| 약관 | TERMS_MASTER, TERMS, USER_TERMS_AGREEMENT |
| 신청·심사 | CARD_APPLICATIONS, APPROVAL |
| 소비패턴·AI | SPENDING_PATTERNS, AI_CHAT_LOGS |
| 검색 | SEARCH_KEYWORDS, SEARCH_LOGS |
| 관리자 | ADMIN_USERS |
| 알림 | NOTIFICATIONS |

---

## 👥 팀원 소개

| 이름 | 역할 | GitHub |
|------|------|--------|
| 김현길 | 약관 / 상품 페이지 | [@username](https://github.com/username) |
| 김성룡 | 인프라 / 형상관리 / AI | [@username](https://github.com/username) |
| 박소현 | 카드 등록 /  결재  / 관리 | [@username](https://github.com/username) |
| 허윤 | DB / 회원 / 인증 | [@username](https://github.com/username) |

---
