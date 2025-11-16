package com.flashcardgroup.flashcard_backend.exception;

public class ImageRetrievalException extends RuntimeException {

    public ImageRetrievalException(String message) {
        super(message);
    }

    public ImageRetrievalException(String message, Throwable cause) {
        super(message, cause);
    }
}