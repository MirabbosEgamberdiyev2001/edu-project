package uz.eduplatform.modules.subscription.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.eduplatform.modules.subscription.domain.UsageRecord;
import uz.eduplatform.modules.subscription.domain.UsageType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsageRecordRepository extends JpaRepository<UsageRecord, UUID> {

    Optional<UsageRecord> findByUserIdAndUsageTypeAndUsageDate(UUID userId, UsageType usageType, LocalDate date);

    List<UsageRecord> findByUserIdAndUsageDateBetween(UUID userId, LocalDate from, LocalDate to);

    @Query("SELECT COALESCE(SUM(u.count), 0) FROM UsageRecord u " +
            "WHERE u.userId = :userId AND u.usageType = :type " +
            "AND u.usageDate BETWEEN :from AND :to")
    int sumCountByUserIdAndTypeAndDateRange(
            @Param("userId") UUID userId,
            @Param("type") UsageType type,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
