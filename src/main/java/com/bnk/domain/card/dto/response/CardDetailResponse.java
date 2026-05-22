package com.bnk.domain.card.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.bnk.domain.card.model.CardBenefit;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class CardDetailResponse {
    private Long cardId;
    private String cardName;
    private String companyName;
    private String cardType;
    private String cardStatus;            // 추가
    private Long annualFeeDomestic;
    private Long annualFeeOverseas;
    private String summaryDescription;
    private LocalDateTime publishStartAt; // 추가
    private LocalDateTime publishEndAt;   // 추가
    private List<CardBenefit> benefits;
    private List<ImageDto> images;
    private List<ContentDto> contents;  // display_order ASC
    private List<TermsFileDto> termsFiles;

    @Getter @Builder
    public static class ImageDto {
        private String imageType;
        private String imageUrl;
        private Integer sortOrder;
    }

    @Getter @Builder
    public static class ContentDto {
        private String contentType;
        private String title;
        private String contentHtml;
        private String mobileContentHtml;
        private Integer displayOrder;
    }

    @Getter @Builder
    public static class TermsFileDto {
        private Long termsId;
        private String title;
        private String filePath;
        private String fileType;
    }
}
