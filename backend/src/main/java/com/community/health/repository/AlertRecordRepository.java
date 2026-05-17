package com.community.health.repository;

import com.community.health.model.AlertRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRecordRepository extends JpaRepository<AlertRecord, Long> {

    List<AlertRecord> findByResidentIdOrderByCreatedAtDesc(Long residentId);

    List<AlertRecord> findByStatusOrderByCreatedAtDesc(String status);
}
