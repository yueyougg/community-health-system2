package com.community.health.repository;

import com.community.health.model.LoginRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface LoginRecordRepository extends JpaRepository<LoginRecord, Long> {
    long countByCreatedAtAfter(LocalDateTime dateTime);
}