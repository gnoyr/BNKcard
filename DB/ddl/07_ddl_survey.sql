-- 설문 마스터
CREATE SEQUENCE SEQ_CARD_SURVEYS START WITH 1 INCREMENT BY 1;

CREATE TABLE CARD_SURVEYS (
    survey_id       NUMBER PRIMARY KEY,
    user_id         NUMBER NOT NULL,
    submitted_at    TIMESTAMP DEFAULT SYSTIMESTAMP,
    reward_given_yn CHAR(1) DEFAULT 'N',
    CONSTRAINT fk_survey_user FOREIGN KEY (user_id)
        REFERENCES USERS(user_id)
);

-- 설문 응답
CREATE SEQUENCE SEQ_CARD_SURVEY_ANSWERS START WITH 1 INCREMENT BY 1;

CREATE TABLE CARD_SURVEY_ANSWERS (
    answer_id       NUMBER PRIMARY KEY,
    survey_id       NUMBER NOT NULL,
    question_code   VARCHAR2(20) NOT NULL,  -- Q1, Q2, Q3...
    answer_value    VARCHAR2(500),           -- 선택값 or 텍스트
    CONSTRAINT fk_answer_survey FOREIGN KEY (survey_id)
        REFERENCES CARD_SURVEYS(survey_id)
);

-- 자유 의견 (감정분석 대상)
CREATE SEQUENCE SEQ_CARD_SURVEY_COMMENTS START WITH 1 INCREMENT BY 1;

CREATE TABLE CARD_SURVEY_COMMENTS (
    comment_id      NUMBER PRIMARY KEY,
    survey_id       NUMBER NOT NULL,
    user_id         NUMBER NOT NULL,
    comment_text    VARCHAR2(2000),
    sentiment       VARCHAR2(20),      -- POSITIVE / NEGATIVE / NEUTRAL
    sentiment_score NUMBER(4,3),       -- -1.0 ~ 1.0
    analyzed_at     TIMESTAMP,
    created_at      TIMESTAMP DEFAULT SYSTIMESTAMP,
    CONSTRAINT fk_comment_survey FOREIGN KEY (survey_id)
        REFERENCES CARD_SURVEYS(survey_id)
);

COMMIT;