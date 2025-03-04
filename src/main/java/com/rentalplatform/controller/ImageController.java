package com.rentalplatform.controller;

import com.rentalplatform.dto.ImageDto;
import com.rentalplatform.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/listings/{listingId}/images")
@RestController
public class ImageController {

    private final ImageService imageService;

    @GetMapping("/{imageId}")
    public ResponseEntity<ImageDto> getImageForListing(@PathVariable Long listingId, @PathVariable Long imageId) {
        return ResponseEntity.ok(imageService.getImageForListing(listingId, imageId));
    }

    @GetMapping
    public ResponseEntity<List<ImageDto>> getImagesForListing(@PathVariable Long listingId) {
        return ResponseEntity.ok(imageService.getImagesForListing(listingId));
    }

    @PreAuthorize("hasRole('ROLE_LANDLORD')")
    @PostMapping("/upload")
    public ResponseEntity<String> uploadImage(@PathVariable Long listingId, @RequestParam("file") MultipartFile file)
            throws IOException {
        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        String fileUrl = imageService.uploadFile(listingId, file, filename);
        return ResponseEntity.ok("Image uploaded successfully: " + fileUrl);
    }

    @PreAuthorize("hasRole('ROLE_LANDLORD')")
    @DeleteMapping("/{filename}")
    public ResponseEntity<String> deleteImage(@PathVariable Long listingId, @PathVariable String filename, Principal principal) {
        imageService.deleteFile(listingId, filename, principal.getName());
        return ResponseEntity.ok("Image deleted successfully");
    }
}
