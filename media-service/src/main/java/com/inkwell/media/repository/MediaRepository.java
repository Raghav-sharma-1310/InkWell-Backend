/*
 * This source file contains database access contracts for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.media.repository;

import com.inkwell.media.entity.MediaFile;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/* This interface groups media repository behavior so the module keeps a clear responsibility. */
public interface MediaRepository extends JpaRepository<MediaFile, UUID> {
    List<MediaFile> findByUploaderIdAndDeletedFalse(UUID uploaderId);
    List<MediaFile> findByLinkedPostIdAndDeletedFalse(UUID linkedPostId);
    List<MediaFile> findByDeletedFalse();
}
