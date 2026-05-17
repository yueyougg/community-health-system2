package com.community.health.controller;

import com.community.health.audit.AuditLog;
import com.community.health.common.ApiResponse;
import com.community.health.model.MedicationRecord;
import com.community.health.repository.MedicationRecordRepository;
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
@RequestMapping("/api/medications")
public class MedicationRecordController {

    private final MedicationRecordRepository medicationRecordRepository;

    public MedicationRecordController(MedicationRecordRepository medicationRecordRepository) {
        this.medicationRecordRepository = medicationRecordRepository;
    }

    @GetMapping
    public ApiResponse<List<MedicationRecord>> list(@RequestParam(required = false) Long residentId) {
        List<MedicationRecord> list = residentId == null
                ? medicationRecordRepository.findAll(Sort.by(Sort.Direction.DESC, "startDate"))
                : medicationRecordRepository.findByResidentIdOrderByStartDateDesc(residentId);
        return ApiResponse.ok(list);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @AuditLog(module = "用药管理", action = "新增用药")
    public ApiResponse<MedicationRecord> create(@RequestBody MedicationRecord request) {
        return ApiResponse.ok("创建成功", medicationRecordRepository.save(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @AuditLog(module = "用药管理", action = "更新用药")
    public ApiResponse<MedicationRecord> update(@PathVariable Long id, @RequestBody MedicationRecord request) {
        MedicationRecord db = medicationRecordRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("用药记录不存在"));
        db.setResidentId(request.getResidentId());
        db.setDrugName(request.getDrugName());
        db.setStartDate(request.getStartDate());
        db.setEndDate(request.getEndDate());
        db.setDosage(request.getDosage());
        db.setUsageMethod(request.getUsageMethod());
        db.setReason(request.getReason());
        return ApiResponse.ok("更新成功", medicationRecordRepository.save(db));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @AuditLog(module = "用药管理", action = "删除用药")
    public ApiResponse<String> delete(@PathVariable Long id) {
        medicationRecordRepository.deleteById(id);
        return ApiResponse.ok("删除成功", "ok");
    }
}
