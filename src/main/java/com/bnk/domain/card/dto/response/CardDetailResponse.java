package com.bnk.domain.card.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.bnk.domain.card.model.CardBenefit;

import lombok.Builder;
import lombok.Getter;

/**
 * 카드 상세 조회 응답 DTO (관리자용)
 */
@Getter
@Builder
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
    private Long applicationCount;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;          // 변경: updateBy → updatedBy
    private LocalDateTime updatedAt; // 변경: updateAt → updatedAt
    private String deletedYn;
    private LocalDateTime deletedAt; // 변경: deleteAt → deletedAt

    private List<CardBenefit> benefits;
    private List<ImageDto> images;
    private List<ContentDto> contents;
    private List<TermsFileDto> termsFiles;

    @Getter
    @Builder
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

    @Getter
    @Builder
    public static class ContentDto {
        private Long contentId;
        private Long cardId;
        private String contentType;
        private String title;
        private String contentHtml;
        private String mobileContentHtml;
        private Integer displayOrder;
        private String visibleYn;
        private Long createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Getter
    @Builder
    public static class TermsFileDto {
        private Long termsId;
        private String title;
        private String filePath;
        private String fileType;
    }
}
