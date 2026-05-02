/*
 * This source file contains application startup and module wiring for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.media.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.mode", havingValue = "s3")
/* This class groups s3 storage service behavior so the module keeps a clear responsibility. */
public class S3StorageService implements StorageService {

    private final S3Client s3Client;

    @Value("${app.storage.s3.bucket}")
    private String bucket;

    @Value("${app.storage.s3.public-base-url}")
    private String publicBaseUrl;

    @Override
    // Defines store so related behavior stays grouped in one place.
    public StoredFile store(MultipartFile file) {
        try {
            String originalName = file.getOriginalFilename();
            String safeName = (originalName != null) ? originalName.replaceAll("[^a-zA-Z0-9._-]", "_") : "upload";
            String filename = java.util.UUID.randomUUID() + "-" + safeName;
            s3Client.putObject(PutObjectRequest.builder().bucket(bucket).key(filename).contentType(file.getContentType()).build(), RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return new StoredFile(filename, publicBaseUrl + "/" + filename, Math.max(1, file.getSize() / 1024));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to upload file to S3", ex);
        }
    }

    @Override
    // Performs the delete workflow so callers do not duplicate this logic.
    public void delete(String filename) {
        s3Client.deleteObject(builder -> builder.bucket(bucket).key(filename));
    }
}
