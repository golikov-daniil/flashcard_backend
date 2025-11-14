package com.flashcardgroup.flashcard_backend.exception;

public class UnsupportedImageTypeException extends RuntimeException {
    public UnsupportedImageTypeException(String message) {
        super(message);
    }
}