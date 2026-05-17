package com.community.health.repository;

import com.community.health.model.HealthKnowledge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HealthKnowledgeRepository extends JpaRepository<HealthKnowledge, Long> {
    List<HealthKnowledge> findByCategory(String category);
    List<HealthKnowledge> findByTitleContaining(String keyword);
}
