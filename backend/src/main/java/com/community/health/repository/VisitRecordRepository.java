package com.community.health.repository;

import com.community.health.model.VisitRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VisitRecordRepository extends JpaRepository<VisitRecord, Long> {

    List<VisitRecord> findByResidentIdOrderByVisitTimeDesc(Long residentId);
}
