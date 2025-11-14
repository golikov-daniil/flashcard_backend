package com.flashcardgroup.flashcard_backend.controllers;

import com.flashcardgroup.flashcard_backend.exception.ImageTooLargeException;
import com.flashcardgroup.flashcard_backend.exception.UnsupportedImageTypeException;
import com.flashcardgroup.flashcard_backend.exception.ImageUploadException;
import com.flashcardgroup.flashcard_backend.exception.ImageNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ImageTooLargeException.class)
    public ResponseEntity<Map<String, String>> handleImageTooLarge(ImageTooLargeException ex) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage());
    }

    @ExceptionHandler(UnsupportedImageTypeException.class)
    public ResponseEntity<Map<String, String>> handleUnsupportedType(UnsupportedImageTypeException ex) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage());
    }

    @ExceptionHandler(ImageUploadException.class)
    public ResponseEntity<Map<String, String>> handleUpload(ImageUploadException ex) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(ImageNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ImageNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "Image not found");
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        Map<String, String> body = new HashMap<>();
        body.put("error", message);
        return ResponseEntity.status(status).body(body);
    }
}
