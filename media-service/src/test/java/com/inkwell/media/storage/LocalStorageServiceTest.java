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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
/* This class groups local storage service test behavior so the module keeps a clear responsibility. */
class LocalStorageServiceTest {

    @InjectMocks
    private LocalStorageService localStorageService;

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("inkwell-media-test");
        ReflectionTestUtils.setField(localStorageService, "localDir", tempDir.toString());
        ReflectionTestUtils.setField(localStorageService, "publicBaseUrl", "http://localhost/public");
    }

    @Test
    @DisplayName("Should upload file successfully")
    void uploadFileSuccess() {
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test data".getBytes());
        StoredFile storedFile = localStorageService.store(file);

        assertThat(storedFile.url()).startsWith("http://localhost/public/");
        assertThat(storedFile.url()).endsWith(".jpg");
    }

    @Test
    @DisplayName("Should handle file with null original filename")
    void uploadFileNullFilename() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        org.mockito.Mockito.when(file.getOriginalFilename()).thenReturn(null);
        try {
            org.mockito.Mockito.when(file.getInputStream()).thenReturn(new java.io.ByteArrayInputStream("test data".getBytes()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        org.mockito.Mockito.when(file.getSize()).thenReturn(10L);
        
        StoredFile storedFile = localStorageService.store(file);
        
        assertThat(storedFile.url()).endsWith("upload");
    }


    @Test
    @DisplayName("Should delete file successfully")
    void deleteFile() throws IOException {
        Path target = tempDir.resolve("test.jpg");
        Files.write(target, "test".getBytes());
        assertThat(Files.exists(target)).isTrue();

        localStorageService.delete("test.jpg");
        
        assertThat(Files.exists(target)).isFalse();
    }

    @Test
    @DisplayName("Should handle IOException on delete gracefully")
    void deleteFileIOException() {
        try (org.mockito.MockedStatic<Files> filesMock = org.mockito.Mockito.mockStatic(Files.class)) {
            filesMock.when(() -> Files.deleteIfExists(any(Path.class))).thenThrow(new IOException("Access denied"));
            
            assertDoesNotThrow(() -> localStorageService.delete("test.jpg"));
        }
    }
}
