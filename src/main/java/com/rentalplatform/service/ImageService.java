package com.rentalplatform.service;

import com.rentalplatform.config.AwsS3Config;
import com.rentalplatform.dto.ImageDto;
import com.rentalplatform.entity.ImageEntity;
import com.rentalplatform.entity.ListingEntity;
import com.rentalplatform.exception.BadRequestException;
import com.rentalplatform.exception.NotFoundException;
import com.rentalplatform.mapper.ImageDtoMapper;
import com.rentalplatform.repository.ImageRepository;
import com.rentalplatform.repository.ListingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ImageService {

    private final S3Client s3Client;
    private final AwsS3Config awsS3Config;
    private final ImageDtoMapper imageDtoMapper;
    private final ImageRepository imageRepository;
    private final ListingRepository listingRepository;

    public ImageDto getImageForListing(Long listingId, Long imageId) {
        ListingEntity listing = findListingByIdOrThrowException(listingId);
        ImageEntity image = findImageByIdOrThrowException(imageId);

        if(!image.getListing().getId().equals(listing.getId())) {
            throw new BadRequestException("The image with id '%d' doesn't below to listing with id '%d'"
                    .formatted(imageId, listingId));
        }

        return imageDtoMapper.makeImageDto(image);
    }

    public List<ImageDto> getImagesForListing(Long listingId) {
        findListingByIdOrThrowException(listingId);
        return imageDtoMapper.makeImageDto(imageRepository.findAllByListingId(listingId));
    }

    @Transactional
    public String uploadFile(Long listingId, MultipartFile file, String filename) throws IOException {
        ListingEntity listing = findListingByIdOrThrowException(listingId);

        s3Client.putObject(builder -> builder
                        .bucket(awsS3Config.getBucketName())
                        .key(filename)
                        .acl("public-read")
                        .build(),
                software.amazon.awssdk.core.sync.RequestBody.fromBytes(file.getBytes()));

        String url = "https://" + awsS3Config.getBucketName() + ".s3." + awsS3Config.getRegion() + ".amazonaws.com/" + filename;

        imageRepository.save(ImageEntity.builder()
                .filename(filename)
                .url(url)
                .listing(listing)
                .build());

        return url;
    }

    @Transactional
    public void deleteFile(Long listingId, String filename, String username) {
        ListingEntity listing = findListingByIdOrThrowException(listingId);
        ImageEntity imageToDelete = findImageByFilenameOrThrowException(filename);

        if(!listing.getLandlord().getUsername().equals(username)) {
            throw new BadRequestException("You are not authorized to delete this file");
        }

        if(!listing.getImages().contains(imageToDelete)) {
            throw new BadRequestException("Listing with id '%d' doesn't contain image with filename '%s'"
                    .formatted(listingId, filename));
        }

        imageRepository.delete(imageToDelete);
        s3Client.deleteObject(builder -> builder.bucket(awsS3Config.getBucketName()).key(filename).build());
    }

    private ImageEntity findImageByFilenameOrThrowException(String filename) {
        return imageRepository.findByFilename(filename)
                .orElseThrow(() -> new NotFoundException("Image with filename '%s' not found".formatted(filename)));
    }

    private ImageEntity findImageByIdOrThrowException(Long imageId) {
        return imageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("Image with id '%d' not found".formatted(imageId)));
    }

    private ListingEntity findListingByIdOrThrowException(Long listingId) {
        return listingRepository.findById(listingId)
                .orElseThrow(() -> new NotFoundException("Listing with id '%d' not found".formatted(listingId)));
    }
}
