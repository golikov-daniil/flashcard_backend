package com.flashcardgroup.flashcard_backend.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class GcsImageService {

    private final Storage storage;
    private final String bucket;

    public GcsImageService(
            @Value("${gcp.bucket.name}") String bucketName,
            @Value("${gcp.credentials.path:}") String credsPath
    ) throws IOException {
        this.bucket = bucketName;

        GoogleCredentials creds;
        if (credsPath != null && !credsPath.isBlank() && Files.exists(Path.of(credsPath))) {
            creds = GoogleCredentials.fromStream(new FileInputStream(credsPath));
            System.out.println("Using explicit GCP credentials file: " + credsPath);
            this.storage = StorageOptions.newBuilder().setCredentials(creds).build().getService();
        } else {
            creds = GoogleCredentials.getApplicationDefault();
            System.out.println("Using Application Default Credentials: " + creds.getClass().getName());
            this.storage = StorageOptions.newBuilder().setCredentials(creds).build().getService();
        }
    }

    // ---------- Upload ----------

    /**
     * Mirrors Node handleImage.
     * Stores to object name: safeName(targetFilename) + extension.
     * Returns the object name to save in ImageNo.
     */
    public String uploadImage(MultipartFile file, String targetFilename) throws IOException {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("Bucket name not configured");
        }
        if (file == null || file.isEmpty()) {
            return null;
        }

        String ext = extractExtension(file);
        String objectName = safeName(targetFilename) + ext;

        BlobInfo info = BlobInfo.newBuilder(bucket, objectName)
                .setContentType(file.getContentType() != null ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .setCacheControl("public, max-age=31536000")
                .build();

        storage.createFrom(info, file.getInputStream());
        return objectName;
    }

    private static String extractExtension(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name != null) {
            int dot = name.lastIndexOf('.');
            if (dot >= 0 && dot < name.length() - 1) {
                return name.substring(dot);
            }
        }
        String ct = file.getContentType();
        if ("image/jpeg".equalsIgnoreCase(ct)) return ".jpg";
        if ("image/png".equalsIgnoreCase(ct)) return ".png";
        if ("image/webp".equalsIgnoreCase(ct)) return ".webp";
        if ("image/gif".equalsIgnoreCase(ct)) return ".gif";
        return "";
    }

    private static String safeName(String s) {
        if (s == null || s.isBlank()) return "image";
        return s.trim().toLowerCase().replaceAll("[^a-z0-9-_]+", "-");
    }

    // ---------- Fetch ----------

    public void streamObject(String objectName, HttpServletResponse response) throws IOException {
        if (bucket == null || bucket.isBlank()) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Bucket name not configured");
            return;
        }

        Blob blob = storage.get(BlobId.of(bucket, objectName));
        if (blob == null || !blob.exists()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
            return;
        }

        String contentType = blob.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(contentType);
        if (blob.getSize() != null) {
            response.setContentLengthLong(blob.getSize());
        }
        response.setHeader("Cache-Control", "public, max-age=3600");
        if (blob.getEtag() != null) {
            response.setHeader("ETag", blob.getEtag());
        }
        response.setHeader("Content-Disposition", "inline; filename=\"" + blob.getName() + "\"");

        try (ReadChannel reader = blob.reader();
             WritableByteChannel out = Channels.newChannel(response.getOutputStream())) {
            ByteBuffer buffer = ByteBuffer.allocate(64 * 1024);
            while (reader.read(buffer) >= 0) {
                buffer.flip();
                out.write(buffer);
                buffer.clear();
            }
        }
    }
}
