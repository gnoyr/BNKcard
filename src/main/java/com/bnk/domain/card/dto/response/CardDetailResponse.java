package com.bnk.domain.card.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter @Builder
public class CardDetailResponse {
    private Long cardId;
    private String cardName;
    private String companyName;
    private String cardType;
    private Long annualFeeDomestic;
    private Long annualFeeOverseas;
    private String summaryDescription;
    private List<BenefitDto> benefits;
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
