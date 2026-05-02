-- =============================================================================
-- InkWell Post Service: EARLY_ACCESS → PUBLIC Migration Script
-- =============================================================================
-- PROBLEM: The Java enum PostVisibility was changed from {PUBLIC, EARLY_ACCESS, PREMIUM}
-- to {PUBLIC, PREMIUM}, but the MySQL database still has rows with 'EARLY_ACCESS'
-- and the column's MySQL ENUM definition still includes 'EARLY_ACCESS'.
-- This causes: "Data truncated for column 'visibility'" when Hibernate tries to
-- ALTER the column, and "No enum constant" when reading rows with EARLY_ACCESS.
--
-- RUN THIS SCRIPT AGAINST THE post_db DATABASE BEFORE RESTARTING THE POST SERVICE.
-- =============================================================================

-- Step 1: Convert all EARLY_ACCESS posts to PUBLIC (safe migration)
UPDATE posts SET visibility = 'PUBLIC' WHERE visibility = 'EARLY_ACCESS';

-- Step 2: Convert the column from MySQL ENUM to VARCHAR(20)
-- This avoids future issues where MySQL ENUM and Java enum get out of sync.
-- Hibernate with @Convert + @Column(length=20) expects VARCHAR, not ENUM.
ALTER TABLE posts MODIFY COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC';

-- Step 3: Verify the migration
SELECT visibility, COUNT(*) as count FROM posts GROUP BY visibility;

-- =============================================================================
-- Done! You can now restart post-service safely.
-- Hibernate ddl-auto:update will NOT try to change this column anymore.
-- =============================================================================
