package com.community.health.repository;

import com.community.health.model.FollowUpPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FollowUpPlanRepository extends JpaRepository<FollowUpPlan, Long> {

    List<FollowUpPlan> findByResidentIdOrderByNextFollowUpDateAsc(Long residentId);

    @Query("SELECT p FROM FollowUpPlan p JOIN ResidentProfile r ON p.residentId = r.id " +
           "WHERE (:residentName IS NULL OR r.name LIKE %:residentName%) " +
           "AND (:diseaseType IS NULL OR p.diseaseType LIKE %:diseaseType%) " +
           "AND (:date IS NULL OR p.nextFollowUpDate = :date) " +
           "ORDER BY p.nextFollowUpDate ASC")
    List<FollowUpPlan> searchPlans(@Param("residentName") String residentName,
                                   @Param("diseaseType") String diseaseType,
                                   @Param("date") LocalDate date);
}
