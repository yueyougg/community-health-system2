package com.community.health.service;

import com.community.health.model.HealthMeasurement;
import com.community.health.repository.HealthMeasurementRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class HealthMeasurementService {
    private static final int SYSTOLIC_BP_LOW = 90;
    private static final int SYSTOLIC_BP_HIGH = 140;
    private static final int DIASTOLIC_BP_LOW = 60;
    private static final int DIASTOLIC_BP_HIGH = 90;
    private static final BigDecimal BLOOD_SUGAR_LOW = new BigDecimal("3.9");
    private static final BigDecimal BLOOD_SUGAR_HIGH = new BigDecimal("6.1");
    private static final BigDecimal BLOOD_LIPID_HIGH = new BigDecimal("5.2");
    private static final int HEART_RATE_LOW = 60;
    private static final int HEART_RATE_HIGH = 100;

    private final HealthMeasurementRepository healthMeasurementRepository;
    private final AlertService alertService;

    public HealthMeasurementService(HealthMeasurementRepository healthMeasurementRepository, AlertService alertService) {
        this.healthMeasurementRepository = healthMeasurementRepository;
        this.alertService = alertService;
    }

    public HealthMeasurement saveMeasurement(HealthMeasurement measurement) {
        if (measurement.getMeasuredAt() == null) {
            measurement.setMeasuredAt(LocalDateTime.now());
        }
        if (measurement.getSourceType() == null || measurement.getSourceType().isBlank()) {
            measurement.setSourceType("MANUAL");
        }

        List<String> abnormalReasons = findAbnormalReasons(measurement);
        measurement.setAlertFlag(!abnormalReasons.isEmpty());
        HealthMeasurement saved = healthMeasurementRepository.save(measurement);

        if (!abnormalReasons.isEmpty()) {
            String message = "检测指标异常: " + String.join("；", abnormalReasons);
            alertService.createAlert(saved.getResidentId(), "HEALTH_METRIC", "HIGH", message, "MEASUREMENT", saved.getId());
        }
        return saved;
    }

    public void refreshAlertFlag(HealthMeasurement measurement) {
        measurement.setAlertFlag(isAbnormal(measurement));
    }

    public boolean isAbnormal(HealthMeasurement measurement) {
        return !findAbnormalReasons(measurement).isEmpty();
    }

    public List<String> findAbnormalReasons(HealthMeasurement measurement) {
        List<String> reasons = new ArrayList<>();

        if (measurement.getSystolicBp() != null
                && (measurement.getSystolicBp() > SYSTOLIC_BP_HIGH || measurement.getSystolicBp() < SYSTOLIC_BP_LOW)) {
            reasons.add("收缩压异常(" + measurement.getSystolicBp() + ")");
        }
        if (measurement.getDiastolicBp() != null
                && (measurement.getDiastolicBp() > DIASTOLIC_BP_HIGH || measurement.getDiastolicBp() < DIASTOLIC_BP_LOW)) {
            reasons.add("舒张压异常(" + measurement.getDiastolicBp() + ")");
        }
        if (measurement.getBloodSugar() != null &&
                (measurement.getBloodSugar().compareTo(BLOOD_SUGAR_HIGH) > 0
                        || measurement.getBloodSugar().compareTo(BLOOD_SUGAR_LOW) < 0)) {
            reasons.add("血糖异常(" + measurement.getBloodSugar() + ")");
        }
        if (measurement.getBloodLipid() != null && measurement.getBloodLipid().compareTo(BLOOD_LIPID_HIGH) > 0) {
            reasons.add("血脂偏高(" + measurement.getBloodLipid() + ")");
        }
        if (measurement.getHeartRate() != null
                && (measurement.getHeartRate() > HEART_RATE_HIGH || measurement.getHeartRate() < HEART_RATE_LOW)) {
            reasons.add("心率异常(" + measurement.getHeartRate() + ")");
        }
        return reasons;
    }
}
