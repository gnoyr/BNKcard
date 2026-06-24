package com.bnk.domain.terms.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class TermsMaster {
    private Long termsMasterId;
    private String termsType;       // COMMON / PRIVACY / CARD_SERVICE
    private String title;
    private String description;
    private LocalDateTime createdAt;
    private Integer displayOrder;  
    private String requiredYn;     
}
