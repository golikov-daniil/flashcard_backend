package com.flashcardgroup.flashcard_backend.controllers;

import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
public class TestApiController {

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of(
                "status", "ok",
                "time", Instant.now().toString()
        );
    }
}