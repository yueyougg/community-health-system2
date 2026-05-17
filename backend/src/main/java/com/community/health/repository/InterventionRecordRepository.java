package com.community.health.repository;

import com.community.health.model.InterventionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterventionRecordRepository extends JpaRepository<InterventionRecord, Long> {
    List<InterventionRecord> findByResidentIdOrderByInterventionDateDesc(Long residentId);
}
