package com.bnk.domain.survey.service;

import com.bnk.domain.survey.dto.request.SurveySubmitRequest;
import com.bnk.domain.survey.dto.response.SurveyResultResponse;
import com.bnk.domain.survey.dto.response.SurveyStatusResponse;
import com.bnk.domain.survey.mapper.SurveyMapper;
import com.bnk.domain.survey.model.CardSurvey;
import com.bnk.domain.survey.model.CardSurveyAnswer;
import com.bnk.domain.survey.model.CardSurveyComment;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SurveyService {

    private final SurveyMapper surveyMapper;

    /**
     * 이번 달 설문 완료 여부 확인
     */
    @Transactional(readOnly = true)
    public SurveyStatusResponse getStatus(Long userId) {
        int count = surveyMapper.countThisMonth(userId);
        CardSurvey last = surveyMapper.findLastByUserId(userId);

        return SurveyStatusResponse.builder()
                .completed(count > 0)
                .lastSurveyAt(last != null && last.getSubmittedAt() != null
                        ? last.getSubmittedAt().toString() : null)
                .build();
    }

    /**
     * 설문 제출
     */
    @Transactional
    public SurveyResultResponse submit(SurveySubmitRequest request, Long userId) {

        // 이번 달 이미 제출했으면 차단
        if (surveyMapper.countThisMonth(userId) > 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "이번 달 설문은 이미 완료하셨습니다.");
        }

        // 1. 설문 마스터 저장
        CardSurvey survey = CardSurvey.builder()
                .userId(userId)
                .rewardGivenYn("N")
                .build();
        surveyMapper.insertSurvey(survey);

        // 2. 응답 저장
        for (SurveySubmitRequest.AnswerItem item : request.getAnswers()) {
            CardSurveyAnswer answer = CardSurveyAnswer.builder()
                    .surveyId(survey.getSurveyId())
                    .questionCode(item.getQuestionCode())
                    .answerValue(item.getAnswerValue())
                    .build();
            surveyMapper.insertAnswer(answer);
        }

        // 3. 자유 의견 저장
        String commentText = request.getCommentText();
        if (commentText != null && !commentText.isBlank()) {
            CardSurveyComment comment = CardSurveyComment.builder()
                    .surveyId(survey.getSurveyId())
                    .userId(userId)
                    .commentText(commentText.trim())
                    .build();
            surveyMapper.insertComment(comment);
        }

        log.info("[Survey] 설문 제출 완료: userId={}, surveyId={}",
                userId, survey.getSurveyId());

        return SurveyResultResponse.builder()
                .surveyId(survey.getSurveyId())
                .rewardGiven(false)
                .message("설문에 참여해 주셔서 감사합니다.")
                .build();
    }
}