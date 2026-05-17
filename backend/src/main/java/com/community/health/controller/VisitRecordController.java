package com.community.health.controller;

import com.community.health.audit.AuditLog;
import com.community.health.common.ApiResponse;
import com.community.health.model.VisitRecord;
import com.community.health.repository.VisitRecordRepository;
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
@RequestMapping("/api/visits")
public class VisitRecordController {

    private final VisitRecordRepository visitRecordRepository;

    public VisitRecordController(VisitRecordRepository visitRecordRepository) {
        this.visitRecordRepository = visitRecordRepository;
    }

    @GetMapping
    public ApiResponse<List<VisitRecord>> list(@RequestParam(required = false) Long residentId) {
        List<VisitRecord> list = residentId == null
                ? visitRecordRepository.findAll(Sort.by(Sort.Direction.DESC, "visitTime"))
                : visitRecordRepository.findByResidentIdOrderByVisitTimeDesc(residentId);
        return ApiResponse.ok(list);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @AuditLog(module = "就诊管理", action = "新增就诊")
    public ApiResponse<VisitRecord> create(@RequestBody VisitRecord request) {
        return ApiResponse.ok("创建成功", visitRecordRepository.save(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @AuditLog(module = "就诊管理", action = "更新就诊")
    public ApiResponse<VisitRecord> update(@PathVariable Long id, @RequestBody VisitRecord request) {
        VisitRecord db = visitRecordRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("就诊记录不存在"));
        db.setResidentId(request.getResidentId());
        db.setVisitTime(request.getVisitTime());
        db.setOrganization(request.getOrganization());
        db.setDiagnosis(request.getDiagnosis());
        db.setPrescription(request.getPrescription());
        db.setExamReport(request.getExamReport());
        return ApiResponse.ok("更新成功", visitRecordRepository.save(db));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @AuditLog(module = "就诊管理", action = "删除就诊")
    public ApiResponse<String> delete(@PathVariable Long id) {
        visitRecordRepository.deleteById(id);
        return ApiResponse.ok("删除成功", "ok");
    }
}
