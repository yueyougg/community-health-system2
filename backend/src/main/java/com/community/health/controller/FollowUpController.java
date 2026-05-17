package com.community.health.controller;

import com.community.health.audit.AuditLog;
import com.community.health.common.ApiResponse;
import com.community.health.model.FollowUpPlan;
import com.community.health.model.FollowUpRecord;
import com.community.health.repository.FollowUpPlanRepository;
import com.community.health.repository.FollowUpRecordRepository;
import com.community.health.service.FollowUpService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/follow-ups")
public class FollowUpController {

    private final FollowUpPlanRepository followUpPlanRepository;
    private final FollowUpRecordRepository followUpRecordRepository;
    private final FollowUpService followUpService;

    public FollowUpController(FollowUpPlanRepository followUpPlanRepository,
                              FollowUpRecordRepository followUpRecordRepository,
                              FollowUpService followUpService) {
        this.followUpPlanRepository = followUpPlanRepository;
        this.followUpRecordRepository = followUpRecordRepository;
        this.followUpService = followUpService;
    }

    @GetMapping("/plans")
    public ApiResponse<List<FollowUpPlan>> listPlans(@RequestParam(required = false) Long residentId,
                                                     @RequestParam(required = false) String residentName,
                                                     @RequestParam(required = false) String diseaseType,
                                                     @RequestParam(required = false) LocalDate date) {
        String normalizedResidentName = residentName == null ? null : residentName.trim();
        String normalizedDiseaseType = diseaseType == null ? null : diseaseType.trim();
        if (normalizedResidentName != null && normalizedResidentName.isEmpty()) {
            normalizedResidentName = null;
        }
        if (normalizedDiseaseType != null && normalizedDiseaseType.isEmpty()) {
            normalizedDiseaseType = null;
        }
        if (normalizedResidentName != null || normalizedDiseaseType != null || date != null) {
            return ApiResponse.ok(followUpPlanRepository.searchPlans(normalizedResidentName, normalizedDiseaseType, date));
        }
        List<FollowUpPlan> list = residentId == null
                ? followUpPlanRepository.findAll(Sort.by(Sort.Direction.ASC, "nextFollowUpDate"))
                : followUpPlanRepository.findByResidentIdOrderByNextFollowUpDateAsc(residentId);
        return ApiResponse.ok(list);
    }

    @PostMapping("/plans")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @AuditLog(module = "随访管理", action = "创建随访计划")
    public ApiResponse<FollowUpPlan> createPlan(@RequestBody FollowUpPlan request) {
        return ApiResponse.ok("创建成功", followUpPlanRepository.save(request));
    }

    @PutMapping("/plans/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @AuditLog(module = "随访管理", action = "更新随访计划")
    public ApiResponse<FollowUpPlan> updatePlan(@PathVariable Long id, @RequestBody FollowUpPlan request) {
        FollowUpPlan db = followUpPlanRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("随访计划不存在"));
        db.setResidentId(request.getResidentId());
        db.setDiseaseType(request.getDiseaseType());
        db.setPeriodDays(request.getPeriodDays());
        db.setNextFollowUpDate(request.getNextFollowUpDate());
        db.setContent(request.getContent());
        db.setActive(request.getActive());
        return ApiResponse.ok("更新成功", followUpPlanRepository.save(db));
    }

    @DeleteMapping("/plans/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @AuditLog(module = "随访管理", action = "删除随访计划")
    public ApiResponse<String> deletePlan(@PathVariable Long id) {
        followUpPlanRepository.deleteById(id);
        return ApiResponse.ok("删除成功", "ok");
    }

    @GetMapping("/records")
    public ApiResponse<List<FollowUpRecord>> listRecords(@RequestParam(required = false) Long residentId,
                                                         @RequestParam(required = false) Long planId,
                                                         @RequestParam(required = false) LocalDateTime startTime,
                                                         @RequestParam(required = false) LocalDateTime endTime) {
        if (planId != null || startTime != null || endTime != null) {
            return ApiResponse.ok(followUpRecordRepository.searchRecords(planId, startTime, endTime));
        }
        if (residentId != null) {
            return ApiResponse.ok(followUpRecordRepository.findByResidentIdOrderByFollowUpTimeDesc(residentId));
        }
        return ApiResponse.ok(followUpRecordRepository.findAll(Sort.by(Sort.Direction.DESC, "followUpTime")));
    }

    @PostMapping("/records")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @AuditLog(module = "随访管理", action = "记录随访")
    public ApiResponse<FollowUpRecord> createRecord(@RequestBody FollowUpRecord request) {
        return ApiResponse.ok("创建成功", followUpService.saveRecord(request));
    }
}
