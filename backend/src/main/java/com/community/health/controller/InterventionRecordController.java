package com.community.health.controller;

import com.community.health.common.ApiResponse;
import com.community.health.model.InterventionRecord;
import com.community.health.model.ResidentProfile;
import com.community.health.model.UserAccount;
import com.community.health.repository.InterventionRecordRepository;
import com.community.health.repository.ResidentProfileRepository;
import com.community.health.repository.UserAccountRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/interventions")
public class InterventionRecordController {

    private final InterventionRecordRepository interventionRecordRepository;
    private final UserAccountRepository userAccountRepository;
    private final ResidentProfileRepository residentProfileRepository;

    public InterventionRecordController(InterventionRecordRepository interventionRecordRepository,
                                        UserAccountRepository userAccountRepository,
                                        ResidentProfileRepository residentProfileRepository) {
        this.interventionRecordRepository = interventionRecordRepository;
        this.userAccountRepository = userAccountRepository;
        this.residentProfileRepository = residentProfileRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN','PUBLIC_HEALTH_MANAGER','RESIDENT')")
    public ApiResponse<List<InterventionRecord>> list(@RequestParam(required = false) Long residentId) {
        List<InterventionRecord> list = residentId == null
                ? interventionRecordRepository.findAll(Sort.by(Sort.Direction.DESC, "interventionDate"))
                : interventionRecordRepository.findByResidentIdOrderByInterventionDateDesc(residentId);
        return ApiResponse.ok(list);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN','PUBLIC_HEALTH_MANAGER')")
    public ApiResponse<InterventionRecord> create(@RequestBody InterventionRecord request, Authentication authentication) {
        if (request.getInterventionDate() == null) {
            request.setInterventionDate(LocalDateTime.now());
        }
        if (authentication != null) {
            UserAccount user = userAccountRepository.findByUsername(authentication.getName()).orElse(null);
            if (user != null) {
                request.setDoctorId(user.getId());
                if (request.getDoctorName() == null || request.getDoctorName().isBlank()) {
                    request.setDoctorName(user.getUsername());
                }
            }
        }
        
        if (request.getResidentId() != null) {
            ResidentProfile resident = residentProfileRepository.findById(request.getResidentId()).orElse(null);
            if (resident != null) {
                request.setResidentName(resident.getName());
            }
        }

        BigDecimal before = request.getBeforeValue() == null ? BigDecimal.ZERO : request.getBeforeValue();
        BigDecimal after = request.getAfterValue() == null ? BigDecimal.ZERO : request.getAfterValue();
        BigDecimal delta = after.subtract(before);
        String level;
        if (delta.compareTo(BigDecimal.ZERO) < 0) {
            level = "IMPROVED";
        } else if (delta.compareTo(BigDecimal.ZERO) > 0) {
            level = "WORSE";
        } else {
            level = "STABLE";
        }
        request.setEffectLevel(level);
        request.setAutoEvaluation(buildEvaluation(request.getTargetMetric(), before, after, level));

        return ApiResponse.ok("创建成功", interventionRecordRepository.save(request));
    }

    private String buildEvaluation(String metric, BigDecimal before, BigDecimal after, String level) {
        String trend = switch (level) {
            case "IMPROVED" -> "干预后指标改善";
            case "WORSE" -> "干预后指标上升，建议复评";
            default -> "干预后指标基本稳定";
        };
        return String.format("目标指标[%s]：干预前 %s，干预后 %s。%s。", metric, before.toPlainString(), after.toPlainString(), trend);
    }
}
