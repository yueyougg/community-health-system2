package com.community.health.repository;

import com.community.health.model.FollowUpRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FollowUpRecordRepository extends JpaRepository<FollowUpRecord, Long> {

    List<FollowUpRecord> findByPlanIdOrderByFollowUpTimeDesc(Long planId);

    List<FollowUpRecord> findByResidentIdOrderByFollowUpTimeDesc(Long residentId);

    @Query("SELECT r FROM FollowUpRecord r WHERE (:planId IS NULL OR r.planId = :planId) " +
           "AND (:startTime IS NULL OR r.followUpTime >= :startTime) " +
           "AND (:endTime IS NULL OR r.followUpTime <= :endTime) " +
           "ORDER BY r.followUpTime DESC")
    List<FollowUpRecord> searchRecords(@Param("planId") Long planId,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);
}
