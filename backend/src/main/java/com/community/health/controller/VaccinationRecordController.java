package com.community.health.controller;

import com.community.health.audit.AuditLog;
import com.community.health.common.ApiResponse;
import com.community.health.model.VaccinationRecord;
import com.community.health.repository.VaccinationRecordRepository;
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
@RequestMapping("/api/vaccinations")
public class VaccinationRecordController {

    private final VaccinationRecordRepository vaccinationRecordRepository;

    public VaccinationRecordController(VaccinationRecordRepository vaccinationRecordRepository) {
        this.vaccinationRecordRepository = vaccinationRecordRepository;
    }

    @GetMapping
    public ApiResponse<List<VaccinationRecord>> list(@RequestParam(required = false) Long residentId) {
        List<VaccinationRecord> list = residentId == null
                ? vaccinationRecordRepository.findAll(Sort.by(Sort.Direction.DESC, "vaccinatedAt"))
                : vaccinationRecordRepository.findByResidentIdOrderByVaccinatedAtDesc(residentId);
        return ApiResponse.ok(list);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @AuditLog(module = "疫苗管理", action = "新增接种")
    public ApiResponse<VaccinationRecord> create(@RequestBody VaccinationRecord request) {
        return ApiResponse.ok("创建成功", vaccinationRecordRepository.save(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @AuditLog(module = "疫苗管理", action = "更新接种")
    public ApiResponse<VaccinationRecord> update(@PathVariable Long id, @RequestBody VaccinationRecord request) {
        VaccinationRecord db = vaccinationRecordRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("接种记录不存在"));
        db.setResidentId(request.getResidentId());
        db.setVaccineName(request.getVaccineName());
        db.setVaccinatedAt(request.getVaccinatedAt());
        db.setInstitution(request.getInstitution());
        db.setBatchNo(request.getBatchNo());
        return ApiResponse.ok("更新成功", vaccinationRecordRepository.save(db));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @AuditLog(module = "疫苗管理", action = "删除接种")
    public ApiResponse<String> delete(@PathVariable Long id) {
        vaccinationRecordRepository.deleteById(id);
        return ApiResponse.ok("删除成功", "ok");
    }
}
