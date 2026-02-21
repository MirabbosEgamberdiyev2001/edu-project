package uz.eduplatform.modules.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.eduplatform.modules.auth.domain.UserSession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findByRefreshTokenAndIsActiveTrue(String refreshToken);

    Optional<UserSession> findByRefreshJtiAndIsActiveTrue(String refreshJti);

    List<UserSession> findByUserIdAndIsActiveTrue(UUID userId);

    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.userId = :userId")
    void deactivateAllByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.refreshToken = :refreshToken")
    void deactivateByRefreshToken(@Param("refreshToken") String refreshToken);

    @Query("SELECT COUNT(DISTINCT s.userId) FROM UserSession s WHERE s.lastUsedAt > :since AND s.isActive = true")
    long countDistinctActiveUsersSince(@Param("since") LocalDateTime since);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);

    @Query(value = "SELECT last_used_at::date AS activity_date, COUNT(DISTINCT user_id) " +
            "FROM user_sessions WHERE last_used_at >= :since AND is_active = true " +
            "GROUP BY activity_date ORDER BY activity_date", nativeQuery = true)
    List<Object[]> countDailyActiveUsers(@Param("since") LocalDateTime since);
}
