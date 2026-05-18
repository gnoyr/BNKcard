package com.bnk.domain.card.dto.request2;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImageCreateRequest {
	
	private String  imageType;      // FRONT / BACK / THUMBNAIL / DETAIL
    private String  imageUrl;
    private String  originalName;
    private String  storedName;
    private Long    fileSize;
    private String  mimeType;
    private Integer imageWidth;
    private Integer imageHeight;
    private Integer sortOrder;
}
