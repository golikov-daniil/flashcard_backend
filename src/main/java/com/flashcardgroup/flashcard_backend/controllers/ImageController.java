package com.flashcardgroup.flashcard_backend.controllers;

import com.flashcardgroup.flashcard_backend.service.ImageStorageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
public class ImageController {

    private final ImageStorageService imageStorageService;

    public ImageController(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    @GetMapping("/images/{*imageId}")
    public ResponseEntity<byte[]> getImage(@PathVariable String imageId) {
        // Strip leading slash if present (Spring captures it in the path variable)
        String key = imageId.startsWith("/") ? imageId.substring(1) : imageId;
        var data = imageStorageService.getImage(key);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(data.contentType()))
                .contentLength(data.contentLength())
                .body(data.bytes());
    }
}
