package com.flashcardgroup.flashcard_backend.service;

import java.io.InputStream;

public interface ImageStorageService {

    String uploadImage(String logicalKeyPrefix,
                       String originalFilename,
                       String contentType,
                       InputStream data,
                       long contentLength);

    record ImageData(byte[] bytes, String contentType, long contentLength) {}

    ImageData getImage(String key);
}