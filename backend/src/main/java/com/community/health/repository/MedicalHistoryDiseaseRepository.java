package com.community.health.repository;

import com.community.health.model.MedicalHistoryDisease;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicalHistoryDiseaseRepository extends JpaRepository<MedicalHistoryDisease, Long> {
    List<MedicalHistoryDisease> findByMedicalHistoryIdOrderByIdAsc(Long medicalHistoryId);
    void deleteByMedicalHistoryId(Long medicalHistoryId);
}
