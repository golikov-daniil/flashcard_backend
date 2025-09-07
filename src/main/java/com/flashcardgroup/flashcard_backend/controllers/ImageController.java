package com.flashcardgroup.flashcard_backend.controllers;

import com.flashcardgroup.flashcard_backend.service.GcsImageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@CrossOrigin
public class ImageController {

    private final GcsImageService gcsImageService;

    public ImageController(GcsImageService gcsImageService) {
        this.gcsImageService = gcsImageService;
    }

    @GetMapping("/images/{imageId:.+}")
    public void getImage(@PathVariable String imageId, HttpServletRequest req, HttpServletResponse res) throws IOException {
        System.out.println("Auth header present: " + (req.getHeader("Authorization") != null));
        gcsImageService.streamObject(imageId, res);
    }

}
