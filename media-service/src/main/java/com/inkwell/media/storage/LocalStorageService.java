/*
 * This source file contains application startup and module wiring for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.media.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.mode", havingValue = "local", matchIfMissing = true)
/* This class groups local storage service behavior so the module keeps a clear responsibility. */
public class LocalStorageService implements StorageService {

    @Value("${app.storage.local-dir:uploads/media}")
    private String localDir;

    @Value("${app.storage.public-base-url:http://localhost:8085/api/media/public/files}")
    private String publicBaseUrl;

    @Override
    // Defines store so related behavior stays grouped in one place.
    public StoredFile store(MultipartFile file) {
        try {
            Files.createDirectories(Path.of(localDir));
            String originalName = file.getOriginalFilename();
            String safeName = (originalName != null) ? originalName.replaceAll("[^a-zA-Z0-9._-]", "_") : "upload";
            String filename = UUID.randomUUID() + "-" + safeName;
            Path target = Path.of(localDir, filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredFile(filename, publicBaseUrl + "/" + filename, Math.max(1, file.getSize() / 1024));
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to store file", ex);
        }
    }

    @Override
    // Performs the delete workflow so callers do not duplicate this logic.
    public void delete(String filename) {
        try {
            Files.deleteIfExists(Path.of(localDir, filename));
        } catch (IOException ex) {
            // Silently ignore delete failures – the file may not exist on disk
            java.util.logging.Logger.getLogger(getClass().getName()).fine("Delete ignored: " + ex.getMessage());
        }
    }
}
