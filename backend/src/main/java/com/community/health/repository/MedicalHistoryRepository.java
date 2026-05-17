package com.community.health.repository;

import com.community.health.model.MedicalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MedicalHistoryRepository extends JpaRepository<MedicalHistory, Long> {

    List<MedicalHistory> findByResidentIdOrderByCreatedAtDesc(Long residentId);
    Optional<MedicalHistory> findFirstByResidentIdOrderByIdDesc(Long residentId);

    @Query("select m.diseaseName, count(m.id) from MedicalHistory m where m.diseaseName is not null and m.diseaseName <> '' group by m.diseaseName")
    List<Object[]> countByDisease();
}
