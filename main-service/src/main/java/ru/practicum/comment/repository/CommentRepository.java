package ru.practicum.comment.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.comment.model.Comment;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("""
            SELECT c FROM Comment c
            LEFT JOIN FETCH c.user
            LEFT JOIN FETCH c.event
            WHERE c.id = :id AND c.user.id = :userId AND c.event.id = :eventId"""
    )
    Optional<Comment> findByIdAndUserIdAndEventId(@Param("id") Long id, @Param("userId") Long userId, @Param("eventId") Long eventId);

    @Query("""
            SELECT c FROM Comment c
            LEFT JOIN FETCH c.user
            LEFT JOIN FETCH c.event
            WHERE c.id = :id AND c.user.id = :userId"""
    )
    Optional<Comment> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query("""
            SELECT c FROM Comment c
            LEFT JOIN FETCH c.user
            LEFT JOIN FETCH c.event
            WHERE c.event.id = :eventId
            ORDER BY c.createdDate DESC"""
    )
    List<Comment> findByEventId(@Param("eventId") Long eventId, Pageable pageable);

    @Query("""
            SELECT c FROM Comment c
            LEFT JOIN FETCH c.user
            LEFT JOIN FETCH c.event
            WHERE c.user.id = :userId
            ORDER BY c.createdDate DESC"""
    )
    List<Comment> findByUserId(@Param("userId") Long userId, Pageable pageable);
}
