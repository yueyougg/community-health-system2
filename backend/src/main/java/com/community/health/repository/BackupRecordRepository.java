package com.community.health.repository;

import com.community.health.model.BackupRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BackupRecordRepository extends JpaRepository<BackupRecord, Long> {
    List<BackupRecord> findAllByOrderByCreatedAtDesc();
}
