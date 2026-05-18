package com.bnk.domain.card.model2;

import java.util.Date;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CardCategory {

	private Long categoryId;

    @NotBlank(message = "카테고리 코드는 필수입니다.")
    @Size(max = 50, message = "카테고리 코드는 50자 이하여야 합니다.")
    private String categoryCode;            // VARCHAR2(50) UQ NN

    @NotBlank(message = "카테고리명은 필수입니다.")
    @Size(max = 100, message = "카테고리명은 100자 이하여야 합니다.")
    private String categoryName;            // VARCHAR2(100) NN

    @Size(max = 100, message = "아이콘 코드는 100자 이하여야 합니다.")
    private String iconCode;                // VARCHAR2(100) NULL

    private Integer displayOrder;           // NUMBER(5) NULL

    @NotBlank(message = "사용 여부는 필수입니다.")
    @Pattern(regexp = "Y|N", message = "사용 여부는 Y 또는 N이어야 합니다.")
    private String useYn = "Y";             // CHAR(1) DEFAULT 'Y' NN

    private Date createdAt;     
    
    
}
