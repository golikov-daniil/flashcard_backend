package com.flashcardgroup.flashcard_backend.exception;

public class ImageTooLargeException extends RuntimeException {
    public ImageTooLargeException(String message) {
        super(message);
    }
}

