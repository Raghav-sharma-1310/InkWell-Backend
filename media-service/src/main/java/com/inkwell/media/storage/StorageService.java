/*
 * This source file contains application startup and module wiring for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.media.storage;

import org.springframework.web.multipart.MultipartFile;

/* This interface groups storage service behavior so the module keeps a clear responsibility. */
public interface StorageService {
    StoredFile store(MultipartFile file);
    void delete(String filename);
}
