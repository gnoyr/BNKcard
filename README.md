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

BNK 부산은행의 금융 상품(카드) 플랫폼으로, 회원 인증부터 카드 상품 조회·비교·신청·심사까지 전 과정을 지원
소비패턴 기반 맞춤형 카드 추천 기능 제공

---

## 주요 기능

| 분류 | 기술 |
|------|------|
| Backend | Java 21, Spring Boot 4.x |
| Frontend | HTML5, CSS3 |
| Database | Oracle Database |
| ORM | MyBatis Framework |
| 인증·보안 | Spring Security, JWT |
| 인프라 | Oracle Cloud Infrastructure (OCI), OCI Object Storage |
| 배포 | Docker, Nginx, GitHub Actions |
| AI | Google AI, Ollama, Qdrant VectorStore |
 
---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 4.0.6, Spring Security, Spring Validation |
| ORM | MyBatis 4.0.1 |
| Database | Oracle, Redis |
| AI | Spring AI (Google GenAI, Embedding), Ollama |
| 인증 | JWT, 이메일 인증 (Spring Mail) |
| 빌드 | Gradle |
| 테스트 | JUnit 5, Mockito, H2 (인메모리) |
| 문서 | Swagger (SpringDoc) |

---

## ERD

<img width="800" alt="erd" src="https://github.com/user-attachments/assets/2f0cdcd9-578b-4e2c-9625-ec76cc6e0de8" />

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
| 김현길 | 약관 / 상품 페이지 | [@gusrlf114](https://github.com/gusrlf114) |
| 김성룡 | 인프라 / 형상관리 / AI | [@gnoyr](https://github.com/gnoyr) |
| 박소현 | 카드 등록 /  결재  / 관리 | [@sohyeon59](https://github.com/sohyeon59) |
| 허윤 | DB / 회원 / 인증 | [@heoheoyun](https://github.com/heoheoyun) |

---

## 📋 프로젝트 보고서

[📄 최종 보고서 보기](file:///C:/Users/GGG/Downloads/1%ED%8C%80%20%EA%B2%B0%EA%B3%BC%EB%B3%B4%EA%B3%A0%EC%84%9C.pdf)
