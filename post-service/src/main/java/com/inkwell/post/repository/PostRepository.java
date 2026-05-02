/*
 * This source file contains database access contracts for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.repository;

import com.inkwell.post.entity.Post;
import com.inkwell.post.enumtype.PostStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/* This interface groups post repository behavior so the module keeps a clear responsibility. */
public interface PostRepository extends JpaRepository<Post, UUID> {

    Optional<Post> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Post> findByStatusOrderByPinnedDescPublishedAtDesc(PostStatus status, Pageable pageable);

    Page<Post> findByAuthorId(UUID authorId, Pageable pageable);

    @Query(value = """
        select distinct p from Post p left join p.tagSlugs t
        where (:status is null or p.status = :status)
          and (:categorySlug = '' or p.categorySlug = :categorySlug)
          and (:tagSlug = '' or t = :tagSlug)
          and (:query = '' or lower(p.title) like lower(concat('%', :query, '%'))
               or lower(p.content) like lower(concat('%', :query, '%')))
        order by p.pinned desc, p.publishedAt desc, p.createdAt desc
        """,
        countQuery = """
        select count(distinct p) from Post p left join p.tagSlugs t
        where (:status is null or p.status = :status)
          and (:categorySlug = '' or p.categorySlug = :categorySlug)
          and (:tagSlug = '' or t = :tagSlug)
          and (:query = '' or lower(p.title) like lower(concat('%', :query, '%'))
               or lower(p.content) like lower(concat('%', :query, '%')))
        """)
    Page<Post> search(
        @Param("status") PostStatus status,
        @Param("categorySlug") String categorySlug,
        @Param("tagSlug") String tagSlug,
        @Param("query") String query,
        Pageable pageable
    );

    long countByStatus(PostStatus status);

    @Query("select coalesce(sum(p.viewCount), 0) from Post p where p.status = :status")
    Long sumViewCountByStatus(@Param("status") PostStatus status);

    List<Post> findByStatusAndScheduledAtBefore(PostStatus status, LocalDateTime time);
}
