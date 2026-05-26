package com.bnk.domain.card.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import com.bnk.domain.card.model.CardBenefit;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class CardDetailResponse {
    private Long cardId;
    private String cardCode;
    private String cardType;
    private String cardName;
    private String companyName;
    private String companyCode;
    private String brandName;
    private Long annualFeeDomestic;
    private Long annualFeeOverseas;
    private Long previousMonthSpend;
    private Integer minimumAge;
    private Integer maximumAge;
    private Long creditLimitMin;
    private Long creditLimitMax;
    private String targetUser;
    private String summaryDescription;
    private String searchableYn;
    private String visibleYn;
    private String approvalRequiredYn;
    private String cardStatus;
    private LocalDateTime publishStartAt;
    private LocalDateTime publishEndAt;
    private Long  applicationCount;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updateBy;
    private LocalDateTime updateAt;
    private String deletedYn;
    private LocalDateTime deleteAt;

    private List<CardBenefit> benefits;
    private List<ImageDto> images;
    private List<ContentDto> contents;
    private List<TermsFileDto> termsFiles;

    @Getter @Builder
    public static class ImageDto {
        private Long imageId;
        private String imageType;
        private String imageUrl;
        private String originalName;
        private String storedName;
        private Long fileSize;
        private String mimeType;
        private Integer imageWidth;
        private Integer imageHeight;
        private Integer sortOrder;
        private LocalDateTime createdAt;
    }
    @Getter @Builder
    public static class ContentDto {
        private Long contentId;        // ← 추가
        private Long cardId;           // ← 추가
        private String contentType;
        private String title;
        private String contentHtml;
        private String mobileContentHtml;
        private Integer displayOrder;
        private String visibleYn;      // ← 추가
        private Long createdBy;        // ← 추가
        private LocalDateTime createdAt;  // ← 추가
        private LocalDateTime updatedAt;  // ← 추가
    }
    @Getter @Builder
    public static class TermsFileDto {
        private Long termsId;
        private String title;
        private String filePath;
        private String fileType;
    }
}