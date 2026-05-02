/*
 * This source file contains automated verification for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.media.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
/* This class groups s3 storage service test behavior so the module keeps a clear responsibility. */
class S3StorageServiceTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private S3StorageService storageService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(storageService, "bucket", "test-bucket");
        ReflectionTestUtils.setField(storageService, "publicBaseUrl", "https://s3.test");
    }

    @Test
    @DisplayName("Should upload file successfully")
    void uploadFileSuccess() {
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test data".getBytes());
        StoredFile storedFile = storageService.store(file);

        assertThat(storedFile.url()).startsWith("https://s3.test/");
        assertThat(storedFile.url()).endsWith(".jpg");
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("Should handle file with null original filename")
    void uploadFileNullFilename() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        org.mockito.Mockito.when(file.getOriginalFilename()).thenReturn(null);
        try {
            org.mockito.Mockito.when(file.getInputStream()).thenReturn(new java.io.ByteArrayInputStream("test data".getBytes()));
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        org.mockito.Mockito.when(file.getSize()).thenReturn(10L);

        StoredFile storedFile = storageService.store(file);

        assertThat(storedFile.url()).endsWith("upload");
    }

    @Test
    @DisplayName("Should delete file successfully")
    void deleteFile() {
        storageService.delete("test.jpg");
        verify(s3Client).deleteObject(any(java.util.function.Consumer.class));
    }
}
