package com.community.health.controller;

import com.community.health.common.ApiResponse;
import com.community.health.model.HealthKnowledge;
import com.community.health.repository.HealthKnowledgeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
public class HealthKnowledgeController {

    private final HealthKnowledgeRepository healthKnowledgeRepository;

    public HealthKnowledgeController(HealthKnowledgeRepository healthKnowledgeRepository) {
        this.healthKnowledgeRepository = healthKnowledgeRepository;
    }

    @GetMapping
    public ApiResponse<List<HealthKnowledge>> list(@RequestParam(required = false) String category,
                                                   @RequestParam(required = false) String keyword) {
        if (category != null && !category.isBlank()) {
            return ApiResponse.ok(healthKnowledgeRepository.findByCategory(category));
        }
        if (keyword != null && !keyword.isBlank()) {
            return ApiResponse.ok(healthKnowledgeRepository.findByTitleContaining(keyword));
        }
        return ApiResponse.ok(healthKnowledgeRepository.findAll(Sort.by(Sort.Direction.DESC, "id")));
    }

    @GetMapping("/{id}")
    public ApiResponse<HealthKnowledge> detail(@PathVariable Long id) {
        return ApiResponse.ok(healthKnowledgeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("知识条目不存在")));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ApiResponse<HealthKnowledge> create(@RequestBody HealthKnowledge request) {
        return ApiResponse.ok("创建成功", healthKnowledgeRepository.save(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ApiResponse<HealthKnowledge> update(@PathVariable Long id, @RequestBody HealthKnowledge request) {
        HealthKnowledge db = healthKnowledgeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("知识条目不存在"));
        db.setTitle(request.getTitle());
        db.setCategory(request.getCategory());
        db.setContent(request.getContent());
        db.setTags(request.getTags());
        return ApiResponse.ok("更新成功", healthKnowledgeRepository.save(db));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ApiResponse<String> delete(@PathVariable Long id) {
        healthKnowledgeRepository.deleteById(id);
        return ApiResponse.ok("删除成功", "ok");
    }
}
