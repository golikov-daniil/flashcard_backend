package com.flashcardgroup.flashcard_backend.config;

import com.flashcardgroup.flashcard_backend.service.GeminiService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.s3.S3Client;

import static org.mockito.Mockito.mock;

@Configuration
@Profile("test")
public class TestS3Config {

    @Bean
    public S3Client s3Client() {
        // Lightweight mock S3Client for tests; avoids real AWS calls and satisfies S3ImageStorageService dependency
        return mock(S3Client.class);
    }

    @Bean
    public GeminiService geminiService() {
        // Mock GeminiService to avoid real external calls during tests
        return mock(GeminiService.class);
    }
}
