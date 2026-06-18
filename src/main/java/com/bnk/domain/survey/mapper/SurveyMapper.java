package com.bnk.domain.survey.mapper;

import com.bnk.domain.survey.model.CardSurvey;
import com.bnk.domain.survey.model.CardSurveyAnswer;
import com.bnk.domain.survey.model.CardSurveyComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SurveyMapper {

    /** 설문 저장 */
    void insertSurvey(CardSurvey survey);

    /** 설문 응답 저장 */
    void insertAnswer(CardSurveyAnswer answer);

    /** 자유 의견 저장 */
    void insertComment(CardSurveyComment comment);

    /** 이번 달 이미 제출했는지 확인 */
    int countThisMonth(@Param("userId") Long userId);

    /** 마지막 설문 조회 */
    CardSurvey findLastByUserId(@Param("userId") Long userId);

    /** 감정분석 안 된 댓글 목록 (배치용) */
    List<CardSurveyComment> findUnanalyzedComments();

    /** 감정분석 결과 업데이트 */
    void updateSentiment(@Param("commentId")      Long commentId,
                         @Param("sentiment")      String sentiment,
                         @Param("sentimentScore") Double sentimentScore);
}