package com.community.health.repository;

import com.community.health.model.MedicalHistoryAllergyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicalHistoryAllergyRecordRepository extends JpaRepository<MedicalHistoryAllergyRecord, Long> {
    List<MedicalHistoryAllergyRecord> findByMedicalHistoryIdOrderByIdAsc(Long medicalHistoryId);
    void deleteByMedicalHistoryId(Long medicalHistoryId);
}
