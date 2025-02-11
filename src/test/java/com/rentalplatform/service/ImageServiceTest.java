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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private AwsS3Config awsS3Config;

    @Mock
    private ImageDtoMapper imageDtoMapper;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private ListingRepository listingRepository;

    @InjectMocks
    private ImageService imageService;

    @Test
    void testGetImageForListing_Success() {
        Long listingId = 1L;
        Long imageId = 1L;

        ListingEntity listing = ListingEntity.builder()
                .id(1L)
                .title("Test Title")
                .build();

        ImageEntity image = ImageEntity.builder()
                .id(1L)
                .listing(listing)
                .filename("Test Filename")
                .build();

        ImageDto imageDto = ImageDto.builder()
                .id(1L)
                .filename("Test filename")
                .build();

        when(imageRepository.findById(imageId)).thenReturn(Optional.ofNullable(image));
        when(listingRepository.findById(listingId)).thenReturn(Optional.ofNullable(listing));
        when(imageDtoMapper.makeImageDto(image)).thenReturn(imageDto);

        ImageDto result = imageService.getImageForListing(listingId, imageId);

        assertNotNull(result);
        assertEquals(imageDto.getFilename(), result.getFilename());
        verify(imageRepository, times(1)).findById(imageId);
        verify(listingRepository, times(1)).findById(listingId);
        verify(imageDtoMapper, times(1)).makeImageDto(image);
    }

    @Test
    void testGetImageForListing_WhenListingNotFound_ShouldThrowException() {
        Long listingId = 1L;
        Long imageId = 10L;

        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        Exception exception = assertThrows(NotFoundException.class,
                () -> imageService.getImageForListing(listingId, imageId));

        assertEquals("Listing with id '1' not found", exception.getMessage());
    }

    @Test
    void testGetImageForListing_ImageNotFound() {
        Long listingId = 1L;
        Long imageId = 10L;

        ListingEntity listing = ListingEntity.builder()
                .id(listingId)
                .build();

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(imageRepository.findById(imageId)).thenReturn(Optional.empty());

        Exception exception = assertThrows(NotFoundException.class,
                () -> imageService.getImageForListing(listingId, imageId));

        assertEquals("Image with id '10' not found", exception.getMessage());
    }

    @Test
    void testGetImageForListing_ImageNotBelonging() {
        Long listingId = 1L;
        Long imageId = 10L;

        ListingEntity listing = ListingEntity.builder()
                .id(listingId)
                .build();

        ListingEntity anotherListing = ListingEntity.builder()
                .id(2L)
                .build();

        ImageEntity image = ImageEntity.builder()
                .id(imageId)
                .listing(anotherListing)
                .build();

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(imageRepository.findById(imageId)).thenReturn(Optional.of(image));


        Exception exception = assertThrows(BadRequestException.class,
                () -> imageService.getImageForListing(listingId, imageId));

        assertEquals("The image with id '10' doesn't below to listing with id '1'", exception.getMessage());
    }

    @Test
    void testGetImagesForListing_Success() {
        Long listingId = 1L;

        ListingEntity listing = ListingEntity.builder()
                .id(1L)
                .build();

        ImageEntity image = ImageEntity.builder()
                .id(1L)
                .filename("Test Filename")
                .listing(listing)
                .build();

        ImageDto imageDto = ImageDto.builder()
                .id(1L)
                .filename("Test Filename")
                .build();

        List<ImageEntity> images = new ArrayList<>(List.of(image));
        List<ImageDto> imageDtos = new ArrayList<>(List.of(imageDto));

        when(listingRepository.findById(listingId)).thenReturn(Optional.ofNullable(listing));
        when(imageRepository.findAllByListingId(listingId)).thenReturn(images);
        when(imageDtoMapper.makeImageDto(images)).thenReturn(imageDtos);

        List<ImageDto> result = imageService.getImagesForListing(listingId);

        assertNotNull(result);
        assertEquals(imageDto.getFilename(), result.get(0).getFilename());
        verify(listingRepository, times(1)).findById(listingId);
        verify(imageRepository, times(1)).findAllByListingId(listingId);
        verify(imageDtoMapper, times(1)).makeImageDto(images);
    }

    @Test
    void testGetImagesForListing_WhenListingNotFound_ShouldThrowException() {
        Long listingId = 1L;

        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> imageService.getImagesForListing(listingId));
    }

    @Test
    void testUploadFile_Success() throws IOException {
        Long listingId = 1L;
        String filename = "test-image.jpg";
        byte[] fileBytes = "image content".getBytes();

        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenReturn(fileBytes);

        ListingEntity listing = ListingEntity.builder()
                .id(listingId)
                .build();

        when(s3Client.putObject(any(Consumer.class), any(RequestBody.class))).thenReturn(null);
        when(awsS3Config.getBucketName()).thenReturn("test-bucket");
        when(awsS3Config.getRegion()).thenReturn("us-east-1");
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        String expectedUrl = "https://" + awsS3Config.getBucketName() +
                ".s3." + awsS3Config.getRegion() + ".amazonaws.com/" + filename;

        String resultUrl = imageService.uploadFile(listingId, file, filename);

        assertEquals(expectedUrl, resultUrl);
        verify(s3Client, times(1)).putObject(any(Consumer.class), any(RequestBody.class));
        verify(imageRepository, times(1)).save(any(ImageEntity.class));
    }

    @Test
    void testUploadFile_ListingNotFound() {
        Long listingId = 1L;
        String filename = "test-image.jpg";

        MultipartFile file = mock(MultipartFile.class);

        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> imageService.uploadFile(listingId, file, filename));
    }

    @Test
    void testDeleteFile_Success() {
        Long listingId = 1L;
        String filename = "test-image.jpg";

        ImageEntity image = ImageEntity.builder()
                .id(1L)
                .filename(filename)
                .build();

        ListingEntity listing = ListingEntity.builder()
                .id(listingId)
                .images(new ArrayList<>(List.of(image)))
                .build();

        when(listingRepository.findById(listingId)).thenReturn(Optional.ofNullable(listing));
        when(imageRepository.findByFilename(filename)).thenReturn(Optional.of(image));

        imageService.deleteFile(listingId, filename);

        verify(imageRepository, times(1)).delete(image);
        verify(s3Client, times(1)).deleteObject(any(Consumer.class));
    }

    @Test
    void testDeleteFile_WhenListingNotFound_ShouldThrowException() {
        Long listingId = 1L;
        String filename = "test-image.jpg";

        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> imageService.deleteFile(listingId, filename));

        assertEquals("Listing with id '1' not found", exception.getMessage());
    }

    @Test
    void testDeleteFile_ImageNotFound() {
        Long listingId = 1L;
        String filename = "test-image.jpg";

        ListingEntity listing = ListingEntity.builder()
                .id(listingId)
                .build();

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(imageRepository.findByFilename(filename)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> imageService.deleteFile(listingId, filename));

        assertEquals("Image with filename 'test-image.jpg' not found", exception.getMessage());
    }

    @Test
    void testDeleteFile_WhenImageNotInListing_ShouldThrowException() {
        Long listingId = 1L;
        String filename = "test-image.jpg";

        ListingEntity listing = ListingEntity.builder()
                .id(listingId)
                .images(new ArrayList<>())
                .build();

        ImageEntity image = ImageEntity.builder()
                .id(1L)
                .filename(filename)
                .build();

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(imageRepository.findByFilename(filename)).thenReturn(Optional.of(image));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> imageService.deleteFile(listingId, filename));

        assertEquals("Listing with id '1' doesn't contain image with filename 'test-image.jpg'", exception.getMessage());
    }
}