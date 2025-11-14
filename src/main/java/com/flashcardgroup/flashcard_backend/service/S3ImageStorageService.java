package com.flashcardgroup.flashcard_backend.service;

import com.flashcardgroup.flashcard_backend.exception.ImageTooLargeException;
import com.flashcardgroup.flashcard_backend.exception.ImageUploadException;
import com.flashcardgroup.flashcard_backend.exception.ImageRetrievalException;
import com.flashcardgroup.flashcard_backend.exception.UnsupportedImageTypeException;
import com.flashcardgroup.flashcard_backend.exception.ImageNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Service
public class S3ImageStorageService implements ImageStorageService {

    private static final Logger log = LoggerFactory.getLogger(S3ImageStorageService.class);

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.max-file-size-bytes}")
    private long maxFileSizeBytes;

    @Value("${aws.s3.allowed-content-types}")
    private List<String> allowedContentTypes;

    public S3ImageStorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public String uploadImage(String logicalKeyPrefix, String originalFilename, String contentType, InputStream data, long contentLength) {
        validate(contentType, contentLength);

        String extension = extractExtension(originalFilename);
        String key = buildKey(logicalKeyPrefix, extension);

        try {
            log.info("Uploading image to S3 bucket='{}', key='{}', contentType='{}', size={} bytes", bucketName, key, contentType, contentLength);

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(data, contentLength));

            log.info("Successfully uploaded image to S3: key='{}'", key);
            return key;
        } catch (Exception e) {
            log.error("Failed to upload image to S3", e);
            throw new ImageUploadException("Failed to upload image to S3", e);
        }
    }

    @Override
    public ImageData getImage(String key) {
        try {
            GetObjectRequest req = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(req)) {
                byte[] bytes = response.readAllBytes();
                String contentType = response.response().contentType();
                long length = response.response().contentLength();
                return new ImageData(bytes, contentType != null ? contentType : "application/octet-stream", length);
            }
        } catch (NoSuchKeyException e) {
            log.warn("S3 image not found: key='{}'", key);
            throw new ImageNotFoundException("Image not found: " + key);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                log.warn("S3 404 for key='{}'", key);
                throw new ImageNotFoundException("Image not found: " + key);
            }
            log.error("S3 error retrieving image: key='{}' status={} message={}", key, e.statusCode(), e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage());
            throw new ImageRetrievalException("Failed to retrieve image from S3", e);
        } catch (IOException e) {
            log.error("IO error reading S3 object: key='{}'", key, e);
            throw new ImageRetrievalException("Failed to read image stream", e);
        } catch (Exception e) {
            log.error("Unexpected error retrieving image: key='{}'", key, e);
            throw new ImageRetrievalException("Failed to retrieve image from S3", e);
        }
    }

    private void validate(String contentType, long contentLength) {
        if (contentLength > maxFileSizeBytes) {
            throw new ImageTooLargeException("Image exceeds maximum size of " + (maxFileSizeBytes / 1024 / 1024) + "MB");
        }
        if (contentType == null || !allowedContentTypes.contains(contentType)) {
            throw new UnsupportedImageTypeException("Unsupported image content type: " + contentType);
        }
    }

    private String extractExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot == -1 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot);
    }

    private String buildKey(String logicalKeyPrefix, String extension) {
        String base = logicalKeyPrefix != null && !logicalKeyPrefix.isEmpty()
                ? logicalKeyPrefix.replaceAll("^/+|/+$", "") + "/"
                : "";
        return base + UUID.randomUUID() + extension;
    }
}
