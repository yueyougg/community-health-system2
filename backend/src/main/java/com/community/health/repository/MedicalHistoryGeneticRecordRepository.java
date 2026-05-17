package com.community.health.repository;

import com.community.health.model.MedicalHistoryGeneticRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicalHistoryGeneticRecordRepository extends JpaRepository<MedicalHistoryGeneticRecord, Long> {
    List<MedicalHistoryGeneticRecord> findByMedicalHistoryIdOrderByIdAsc(Long medicalHistoryId);
    void deleteByMedicalHistoryId(Long medicalHistoryId);
}
