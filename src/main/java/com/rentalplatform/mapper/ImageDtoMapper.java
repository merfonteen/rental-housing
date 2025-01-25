package com.rentalplatform.mapper;

import com.rentalplatform.dto.ImageDto;
import com.rentalplatform.entity.ImageEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ImageDtoMapper {
    public ImageDto makeImageDto(ImageEntity image) {
        return ImageDto.builder()
                .id(image.getId())
                .filename(image.getFilename())
                .url(image.getUrl())
                .build();
    }

    public List<ImageDto> makeImageDto(List<ImageEntity> images) {
        return images.stream()
                .map(this::makeImageDto)
                .collect(Collectors.toList());
    }
}
