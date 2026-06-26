package com.flowiq.profile.repository;

import com.flowiq.profile.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, String> {

    Optional<UserSession> findByIdAndRevokedAtIsNull(String id);

    Optional<UserSession> findByRefreshTokenHashAndRevokedAtIsNull(String refreshTokenHash);

    List<UserSession> findByUserIdAndRevokedAtIsNullOrderByLastActivityAtDesc(Long userId);

    @Modifying
    @Query("UPDATE UserSession s SET s.revokedAt = :revokedAt WHERE s.user.id = :userId AND s.revokedAt IS NULL")
    int revokeAllForUser(@Param("userId") Long userId, @Param("revokedAt") LocalDateTime revokedAt);

    @Modifying
    @Query("UPDATE UserSession s SET s.revokedAt = :revokedAt WHERE s.user.id = :userId AND s.id <> :sessionId AND s.revokedAt IS NULL")
    int revokeAllExcept(@Param("userId") Long userId, @Param("sessionId") String sessionId, @Param("revokedAt") LocalDateTime revokedAt);

    @Modifying
    @Query("UPDATE UserSession s SET s.revokedAt = :revokedAt WHERE s.id = :sessionId AND s.revokedAt IS NULL")
    int revokeById(@Param("sessionId") String sessionId, @Param("revokedAt") LocalDateTime revokedAt);
}
