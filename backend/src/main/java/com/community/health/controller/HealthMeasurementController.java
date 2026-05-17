package com.community.health.controller;

import com.community.health.audit.AuditLog;
import com.community.health.common.ApiResponse;
import com.community.health.model.HealthMeasurement;
import com.community.health.model.ResidentProfile;
import com.community.health.model.UserAccount;
import com.community.health.repository.HealthMeasurementRepository;
import com.community.health.repository.ResidentProfileRepository;
import com.community.health.repository.UserAccountRepository;
import com.community.health.service.HealthMeasurementService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/measurements")
public class HealthMeasurementController {

    private final HealthMeasurementRepository healthMeasurementRepository;
    private final HealthMeasurementService healthMeasurementService;
    private final ResidentProfileRepository residentProfileRepository;
    private final UserAccountRepository userAccountRepository;

    public HealthMeasurementController(HealthMeasurementRepository healthMeasurementRepository,
                                       HealthMeasurementService healthMeasurementService,
                                       ResidentProfileRepository residentProfileRepository,
                                       UserAccountRepository userAccountRepository) {
        this.healthMeasurementRepository = healthMeasurementRepository;
        this.healthMeasurementService = healthMeasurementService;
        this.residentProfileRepository = residentProfileRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @GetMapping
    public ApiResponse<List<HealthMeasurement>> list(@RequestParam(required = false) Long residentId) {
        List<HealthMeasurement> list = residentId == null
                ? healthMeasurementRepository.findAll(Sort.by(Sort.Direction.DESC, "measuredAt"))
                : healthMeasurementRepository.findByResidentIdOrderByMeasuredAtDesc(residentId);
        list.forEach(healthMeasurementService::refreshAlertFlag);
        return ApiResponse.ok(list);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('RESIDENT')")
    public ApiResponse<List<HealthMeasurement>> myMeasurements() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserAccount user = userAccountRepository.findByUsername(auth.getName()).orElseThrow();
        ResidentProfile profile = residentProfileRepository.findByUser(user)
                .orElseThrow(() -> new EntityNotFoundException("未找到您的健康档案"));
        List<HealthMeasurement> list = healthMeasurementRepository.findByResidentIdOrderByMeasuredAtDesc(profile.getId());
        list.forEach(healthMeasurementService::refreshAlertFlag);
        return ApiResponse.ok(list);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN','RESIDENT')")
    @AuditLog(module = "健康检测", action = "录入体征")
    public ApiResponse<HealthMeasurement> create(@RequestBody HealthMeasurement request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // 流程：居民录入时强制绑定本人档案并标记来源；医生录入则标记医生来源
        boolean isResident = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_RESIDENT"));
        if (isResident) {
            UserAccount user = userAccountRepository.findByUsername(auth.getName()).orElseThrow();
            ResidentProfile profile = residentProfileRepository.findByUser(user).orElseThrow();
            request.setResidentId(profile.getId());
            request.setSourceType("RESIDENT_SELF");
        } else if (request.getSourceType() == null || request.getSourceType().isBlank()) {
            request.setSourceType("DOCTOR_ENTRY");
        }
        return ApiResponse.ok("录入成功", healthMeasurementService.saveMeasurement(request));
    }

    @GetMapping("/trend/me")
    @PreAuthorize("hasRole('RESIDENT')")
    public ApiResponse<List<HealthMeasurement>> myTrend() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserAccount user = userAccountRepository.findByUsername(auth.getName()).orElseThrow();
        ResidentProfile profile = residentProfileRepository.findByUser(user).orElseThrow();
        List<HealthMeasurement> list = healthMeasurementRepository.findTop20ByResidentIdOrderByMeasuredAtAsc(profile.getId());
        list.forEach(healthMeasurementService::refreshAlertFlag);
        return ApiResponse.ok(list);
    }

    @GetMapping("/trend/{residentId}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ApiResponse<List<HealthMeasurement>> trend(@PathVariable Long residentId) {
        List<HealthMeasurement> list = healthMeasurementRepository.findTop20ByResidentIdOrderByMeasuredAtAsc(residentId);
        list.forEach(healthMeasurementService::refreshAlertFlag);
        return ApiResponse.ok(list);
    }
}
