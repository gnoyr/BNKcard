package com.bnk.domain.terms.dto.response;

import com.bnk.domain.terms.model.Terms;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TermsPackageResponse {
    private Long packageId;
    private String packageName;
    private String packageType;
    private List<TermsItem> terms;

    @Getter
    @Builder
    public static class TermsItem {
        private Long termsId;
        private String title;
        private String termsType;
        private String version;
        private String requiredYn;
        private String contentHtml;
    }

    public static TermsItem fromTerms(Terms t) {
        return TermsItem.builder()
                .termsId(t.getTermsId())
                .title(t.getTitle())
                .termsType(t.getTermsType())
                .version(t.getVersion())
                .requiredYn(t.getRequiredYn())
                .contentHtml(t.getContentHtml())
                .build();
    }
}
