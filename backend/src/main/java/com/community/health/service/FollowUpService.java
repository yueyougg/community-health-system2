package com.community.health.service;

import com.community.health.model.FollowUpPlan;
import com.community.health.model.FollowUpRecord;
import com.community.health.repository.FollowUpPlanRepository;
import com.community.health.repository.FollowUpRecordRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class FollowUpService {

    private final FollowUpPlanRepository followUpPlanRepository;
    private final FollowUpRecordRepository followUpRecordRepository;
    private final AlertService alertService;

    public FollowUpService(FollowUpPlanRepository followUpPlanRepository,
                           FollowUpRecordRepository followUpRecordRepository,
                           AlertService alertService) {
        this.followUpPlanRepository = followUpPlanRepository;
        this.followUpRecordRepository = followUpRecordRepository;
        this.alertService = alertService;
    }

    public FollowUpRecord saveRecord(FollowUpRecord record) {
        FollowUpPlan plan = followUpPlanRepository.findById(record.getPlanId())
                .orElseThrow(() -> new EntityNotFoundException("随访计划不存在"));
        FollowUpRecord saved = followUpRecordRepository.save(record);

        LocalDate nextDate = record.getNextReminderDate();
        if (nextDate == null && plan.getPeriodDays() != null) {
            nextDate = LocalDate.now().plusDays(plan.getPeriodDays());
        }
        plan.setNextFollowUpDate(nextDate);
        followUpPlanRepository.save(plan);

        if (nextDate != null && !nextDate.isAfter(LocalDate.now().plusDays(3))) {
            alertService.createAlert(
                    record.getResidentId(),
                    "FOLLOW_UP",
                    "MEDIUM",
                    "随访提醒: 请在 " + nextDate + " 前完成下一次随访",
                    "FOLLOW_UP_PLAN",
                    plan.getId()
            );
        }
        return saved;
    }
}
