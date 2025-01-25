package com.rentalplatform.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ImageDto {
    private Long id;
    private String filename;
    private String url;
}
