package com.rentalplatform.controller;

import com.rentalplatform.service.S3StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RequiredArgsConstructor
@RequestMapping("/api/listings/{listingId}/images")
@RestController
public class ImageController {

    private final S3StorageService s3StorageService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadImage(@PathVariable Long listingId, @RequestParam("file") MultipartFile file)
            throws IOException {
        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        String fileUrl = s3StorageService.uploadFile(listingId, file, filename);
        return ResponseEntity.ok("Image uploaded successfully: " + fileUrl);
    }

    @DeleteMapping("/{filename}")
    public ResponseEntity<String> deleteImage(@PathVariable Long listingId, @PathVariable String filename) {
        s3StorageService.deleteFile(listingId, filename);
        return ResponseEntity.ok("Image deleted successfully");
    }
}
