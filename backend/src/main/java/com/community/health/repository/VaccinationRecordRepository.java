package com.community.health.repository;

import com.community.health.model.VaccinationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VaccinationRecordRepository extends JpaRepository<VaccinationRecord, Long> {

    List<VaccinationRecord> findByResidentIdOrderByVaccinatedAtDesc(Long residentId);
}
