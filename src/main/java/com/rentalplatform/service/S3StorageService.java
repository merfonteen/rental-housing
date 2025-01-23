package com.rentalplatform.service;

import com.rentalplatform.config.AwsS3Config;
import com.rentalplatform.entity.ImageEntity;
import com.rentalplatform.entity.ListingEntity;
import com.rentalplatform.exception.BadRequestException;
import com.rentalplatform.exception.NotFoundException;
import com.rentalplatform.repository.ImageRepository;
import com.rentalplatform.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;

@RequiredArgsConstructor
@Service
public class S3StorageService {

    private final S3Client s3Client;
    private final AwsS3Config awsS3Config;
    private final ImageRepository imageRepository;
    private final ListingRepository listingRepository;

    public String uploadFile(Long listingId, MultipartFile file, String filename) throws IOException {
        ListingEntity listing = findListingById(listingId);

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

    public void deleteFile(Long listingId, String filename) {
        ListingEntity listing = findListingById(listingId);
        ImageEntity imageToDelete = findImageByFilename(filename);

        if(!listing.getImages().contains(imageToDelete)) {
            throw new BadRequestException("Listing with id '%d' doesn't contain image with filename '%s'"
                    .formatted(listingId, filename));
        }

        imageRepository.delete(imageToDelete);
        s3Client.deleteObject(builder -> builder.bucket(awsS3Config.getBucketName()).key(filename).build());
    }

    private ImageEntity findImageByFilename(String filename) {
        return imageRepository.findByFilename(filename)
                .orElseThrow(() -> new NotFoundException("Image with filename '%s' not found".formatted(filename)));
    }

    private ListingEntity findListingById(Long listingId) {
        return listingRepository.findById(listingId)
                .orElseThrow(() -> new NotFoundException("Listing with id '%d' not found".formatted(listingId)));
    }
}
