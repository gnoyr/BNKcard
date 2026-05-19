package com.bnk.domain.card.dto.request2;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryCreateRequest {
	
	private String  categoryCode;   // TRANSPORT / SHOPPING / DINING 등
    private String  categoryName;
    private String  iconCode;
    private Integer displayOrder;
    private String  useYn;
}
