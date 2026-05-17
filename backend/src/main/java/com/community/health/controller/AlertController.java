package com.community.health.controller;

import com.community.health.audit.AuditLog;
import com.community.health.common.ApiResponse;
import com.community.health.model.AlertRecord;
import com.community.health.repository.AlertRecordRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertRecordRepository alertRecordRepository;

    public AlertController(AlertRecordRepository alertRecordRepository) {
        this.alertRecordRepository = alertRecordRepository;
    }

    @GetMapping
    public ApiResponse<List<AlertRecord>> list(@RequestParam(required = false) Long residentId,
                                               @RequestParam(required = false) String status) {
        List<AlertRecord> list;
        if (residentId != null) {
            list = alertRecordRepository.findByResidentIdOrderByCreatedAtDesc(residentId);
        } else if (status != null && !status.isBlank()) {
            list = alertRecordRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            list = alertRecordRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        }
        if (status != null && !status.isBlank() && residentId != null) {
            list = list.stream().filter(a -> status.equalsIgnoreCase(a.getStatus())).collect(Collectors.toList());
        }
        return ApiResponse.ok(list);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @AuditLog(module = "预警管理", action = "处理预警")
    public ApiResponse<AlertRecord> updateStatus(@PathVariable Long id, @RequestParam String status) {
        AlertRecord alert = alertRecordRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("预警不存在"));
        alert.setStatus(status);
        return ApiResponse.ok("更新成功", alertRecordRepository.save(alert));
    }
}
