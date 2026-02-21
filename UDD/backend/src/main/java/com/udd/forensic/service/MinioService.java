package com.udd.forensic.service;

import io.minio.*;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Service for interacting with MinIO Object Storage.
 * Handles upload, download, and management of forensic report PDF files.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    /**
     * Uploads a file to MinIO and returns the object path.
     *
     * @param file the uploaded multipart file
     * @return the MinIO object path (e.g., "forensic-reports/2026/02/abc123.pdf")
     */
    public String uploadFile(MultipartFile file) {
        try {
            ensureBucketExists();

            String objectName = generateObjectPath(file.getOriginalFilename());

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            log.info("File uploaded to MinIO: {}/{}", bucketName, objectName);
            return bucketName + "/" + objectName;

        } catch (Exception e) {
            log.error("Failed to upload file to MinIO", e);
            throw new RuntimeException("Greška prilikom uploada fajla na MinIO: " + e.getMessage(), e);
        }
    }

    /**
     * Downloads a file from MinIO.
     *
     * @param objectPath full path including bucket (e.g., "forensic-reports/2026/02/abc.pdf")
     * @return InputStream of the file content
     */
    public InputStream downloadFile(String objectPath) {
        try {
            // Remove bucket name prefix if present
            String objectName = objectPath;
            if (objectPath.startsWith(bucketName + "/")) {
                objectName = objectPath.substring(bucketName.length() + 1);
            }

            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());

        } catch (Exception e) {
            log.error("Failed to download file from MinIO: {}", objectPath, e);
            throw new RuntimeException("Greška prilikom preuzimanja fajla: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the raw bytes of an uploaded MultipartFile for forwarding to Tika.
     */
    public byte[] getFileBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (Exception e) {
            throw new RuntimeException("Greška prilikom čitanja fajla: " + e.getMessage(), e);
        }
    }

    // ==================== Private Helpers ====================

    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucketName)
                .build());

        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
            log.info("Created MinIO bucket: {}", bucketName);
        }
    }

    private String generateObjectPath(String originalFilename) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        return date + "/" + UUID.randomUUID() + extension;
    }
}
