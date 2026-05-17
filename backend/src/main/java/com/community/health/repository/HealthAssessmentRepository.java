package com.community.health.repository;

import com.community.health.model.HealthAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface HealthAssessmentRepository extends JpaRepository<HealthAssessment, Long> {
    List<HealthAssessment> findByResidentIdOrderByAssessmentDateDesc(Long residentId);

    @Query("select h.diseaseType, count(h.id) from HealthAssessment h where h.diseaseType is not null and h.diseaseType <> '' group by h.diseaseType")
    List<Object[]> countByDiseaseType();
}
