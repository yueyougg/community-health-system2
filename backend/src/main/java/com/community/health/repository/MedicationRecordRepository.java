package com.community.health.repository;

import com.community.health.model.MedicationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicationRecordRepository extends JpaRepository<MedicationRecord, Long> {

    List<MedicationRecord> findByResidentIdOrderByStartDateDesc(Long residentId);
}
