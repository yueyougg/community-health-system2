package com.community.health.controller;

import com.community.health.common.ApiResponse;
import com.community.health.model.HealthAssessment;
import com.community.health.model.UserAccount;
import com.community.health.repository.HealthAssessmentRepository;
import com.community.health.repository.UserAccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@RestController
@RequestMapping("/api/assessments")
public class HealthAssessmentController {

    private final HealthAssessmentRepository healthAssessmentRepository;
    private final UserAccountRepository userAccountRepository;

    public HealthAssessmentController(HealthAssessmentRepository healthAssessmentRepository,
                                      UserAccountRepository userAccountRepository) {
        this.healthAssessmentRepository = healthAssessmentRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN','RESIDENT')")
    public ApiResponse<List<HealthAssessment>> list(@RequestParam(required = false) Long residentId) {
        if (residentId != null) {
            return ApiResponse.ok(healthAssessmentRepository.findByResidentIdOrderByAssessmentDateDesc(residentId));
        }
        return ApiResponse.ok(healthAssessmentRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ApiResponse<HealthAssessment> create(@RequestBody HealthAssessment assessment, Authentication authentication) {
        if (authentication != null) {
            UserAccount user = userAccountRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
            assessment.setDoctorId(user.getId());
            String name = (assessment.getDoctorName() != null && !assessment.getDoctorName().isBlank())
                    ? assessment.getDoctorName()
                    : user.getUsername();
            assessment.setDoctorName(name);
        }
        assessment.setAssessmentDate(LocalDateTime.now());

        log.info("保存健康评估 {}", assessment.toString());
        return ApiResponse.ok(healthAssessmentRepository.save(assessment));
    }
}
