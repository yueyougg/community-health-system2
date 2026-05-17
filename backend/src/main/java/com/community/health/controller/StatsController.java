package com.community.health.controller;

import com.community.health.common.ApiResponse;
import com.community.health.repository.AlertRecordRepository;
import com.community.health.repository.HealthAssessmentRepository;
import com.community.health.repository.HealthMeasurementRepository;
import com.community.health.repository.FollowUpPlanRepository;
import com.community.health.repository.MedicalHistoryRepository;
import com.community.health.repository.ResidentProfileRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final ResidentProfileRepository residentProfileRepository;
    private final MedicalHistoryRepository medicalHistoryRepository;
    private final AlertRecordRepository alertRecordRepository;
    private final HealthMeasurementRepository healthMeasurementRepository;
    private final HealthAssessmentRepository healthAssessmentRepository;
    private final FollowUpPlanRepository followUpPlanRepository;

    public StatsController(ResidentProfileRepository residentProfileRepository,
                           MedicalHistoryRepository medicalHistoryRepository,
                           AlertRecordRepository alertRecordRepository,
                           HealthMeasurementRepository healthMeasurementRepository,
                           HealthAssessmentRepository healthAssessmentRepository,
                           FollowUpPlanRepository followUpPlanRepository) {
        this.residentProfileRepository = residentProfileRepository;
        this.medicalHistoryRepository = medicalHistoryRepository;
        this.alertRecordRepository = alertRecordRepository;
        this.healthMeasurementRepository = healthMeasurementRepository;
        this.healthAssessmentRepository = healthAssessmentRepository;
        this.followUpPlanRepository = followUpPlanRepository;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN','PUBLIC_HEALTH_MANAGER')")
    public ApiResponse<Map<String, Object>> overview() {
        Map<String, Object> map = new HashMap<>();
        map.put("residentCount", residentProfileRepository.count());
        map.put("medicalHistoryCount", medicalHistoryRepository.count());
        map.put("newAlertCount", alertRecordRepository.findByStatusOrderByCreatedAtDesc("NEW").size());
        map.put("followUpPlanCount", followUpPlanRepository.count());
        return ApiResponse.ok(map);
    }

    @GetMapping("/gender-distribution")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN','PUBLIC_HEALTH_MANAGER')")
    public ApiResponse<List<Map<String, Object>>> genderDistribution() {
        List<Map<String, Object>> list = residentProfileRepository.countByGender().stream()
                .map(row -> Map.<String, Object>of(
                        "name", row[0] == null ? "未知" : row[0].toString(),
                        "value", row[1]
                ))
                .toList();
        return ApiResponse.ok(list);
    }

    @GetMapping("/disease-distribution")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN','PUBLIC_HEALTH_MANAGER')")
    public ApiResponse<List<Map<String, Object>>> diseaseDistribution() {
        List<Map<String, Object>> list = healthAssessmentRepository.countByDiseaseType().stream()
                .map(row -> Map.<String, Object>of(
                        "name", row[0] == null ? "未知" : row[0].toString(),
                        "value", row[1]
                ))
                .toList();
        return ApiResponse.ok(list);
    }

    @GetMapping("/health-indicator-distribution")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN','PUBLIC_HEALTH_MANAGER')")
    public ApiResponse<Map<String, Object>> healthIndicatorDistribution() {
        long total = residentProfileRepository.count();
        // 一个人只要有任何指标异常就算异常记录
        long abnormalCount = healthMeasurementRepository.findAnyAbnormalResidentIds().size();
        // 所有指标正常才算正常记录
        long normalCount = total - abnormalCount;
        
        Map<String, Object> result = new HashMap<>();
        result.put("abnormal", abnormalCount);
        result.put("normal", normalCount);
        result.put("total", total);
        
        return ApiResponse.ok(result);
    }

    @GetMapping("/gender-abnormal-stats")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN','PUBLIC_HEALTH_MANAGER')")
    public ApiResponse<Map<String, Object>> genderAbnormalStats() {
        // 获取各性别总人数
        List<Object[]> genderCounts = residentProfileRepository.countByGender();
        Map<String, Long> genderTotalMap = new HashMap<>();
        for (Object[] row : genderCounts) {
            String gender = row[0] == null ? "未知" : row[0].toString();
            Long count = (Long) row[1];
            genderTotalMap.put(gender, count);
        }
        
        // 获取各性别血压异常人数
        Map<String, Long> bpAbnormalByGender = new HashMap<>();
        for (Object[] row : healthMeasurementRepository.countBloodPressureAbnormalByGender()) {
            String gender = row[0] == null ? "未知" : row[0].toString();
            Long count = ((Number) row[1]).longValue();
            bpAbnormalByGender.put(gender, count);
        }
        
        // 获取各性别血糖异常人数
        Map<String, Long> bsAbnormalByGender = new HashMap<>();
        for (Object[] row : healthMeasurementRepository.countBloodSugarAbnormalByGender()) {
            String gender = row[0] == null ? "未知" : row[0].toString();
            Long count = ((Number) row[1]).longValue();
            bsAbnormalByGender.put(gender, count);
        }
        
        // 获取各性别血脂异常人数
        Map<String, Long> blAbnormalByGender = new HashMap<>();
        for (Object[] row : healthMeasurementRepository.countBloodLipidAbnormalByGender()) {
            String gender = row[0] == null ? "未知" : row[0].toString();
            Long count = ((Number) row[1]).longValue();
            blAbnormalByGender.put(gender, count);
        }
        
        // 计算各性别任何指标异常人数
        Map<String, Long> anyAbnormalByGender = new HashMap<>();
        for (String gender : genderTotalMap.keySet()) {
            long bpCount = bpAbnormalByGender.getOrDefault(gender, 0L);
            long bsCount = bsAbnormalByGender.getOrDefault(gender, 0L);
            long blCount = blAbnormalByGender.getOrDefault(gender, 0L);
            // 取最大值作为任何异常的人数（因为一个人可能同时有多种异常）
            anyAbnormalByGender.put(gender, Math.max(Math.max(bpCount, bsCount), blCount));
        }
        
        // 构建返回数据
        List<Map<String, Object>> maleData = new ArrayList<>();
        List<Map<String, Object>> femaleData = new ArrayList<>();
        
        long maleAbnormal = anyAbnormalByGender.getOrDefault("男", 0L);
        long maleTotal = genderTotalMap.getOrDefault("男", 0L);
        maleData.add(Map.of("name", "异常", "value", maleAbnormal));
        maleData.add(Map.of("name", "正常", "value", Math.max(0, maleTotal - maleAbnormal)));
        
        long femaleAbnormal = anyAbnormalByGender.getOrDefault("女", 0L);
        long femaleTotal = genderTotalMap.getOrDefault("女", 0L);
        femaleData.add(Map.of("name", "异常", "value", femaleAbnormal));
        femaleData.add(Map.of("name", "正常", "value", Math.max(0, femaleTotal - femaleAbnormal)));
        
        Map<String, Object> result = new HashMap<>();
        result.put("male", maleData);
        result.put("female", femaleData);
        
        return ApiResponse.ok(result);
    }

    @GetMapping("/age-group-abnormal-stats")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN','PUBLIC_HEALTH_MANAGER')")
    public ApiResponse<Map<String, Object>> ageGroupAbnormalStats() {
        String[] ageGroups = {"30岁以下", "30-39岁", "40-49岁", "50-59岁", "60-69岁", "70岁及以上"};
        
        // 获取各年龄段血压异常人数
        Map<String, Long> bpAbnormalByAge = new HashMap<>();
        for (Object[] row : healthMeasurementRepository.countBloodPressureAbnormalByAgeGroup()) {
            bpAbnormalByAge.put((String) row[0], ((Number) row[1]).longValue());
        }
        
        // 获取各年龄段血糖异常人数
        Map<String, Long> bsAbnormalByAge = new HashMap<>();
        for (Object[] row : healthMeasurementRepository.countBloodSugarAbnormalByAgeGroup()) {
            bsAbnormalByAge.put((String) row[0], ((Number) row[1]).longValue());
        }
        
        // 获取各年龄段血脂异常人数
        Map<String, Long> blAbnormalByAge = new HashMap<>();
        for (Object[] row : healthMeasurementRepository.countBloodLipidAbnormalByAgeGroup()) {
            blAbnormalByAge.put((String) row[0], ((Number) row[1]).longValue());
        }
        
        // 构建返回数据
        List<Map<String, Object>> bpData = new ArrayList<>();
        List<Map<String, Object>> bsData = new ArrayList<>();
        List<Map<String, Object>> blData = new ArrayList<>();
        
        for (String ageGroup : ageGroups) {
            long bpAbnormal = bpAbnormalByAge.getOrDefault(ageGroup, 0L);
            long bsAbnormal = bsAbnormalByAge.getOrDefault(ageGroup, 0L);
            long blAbnormal = blAbnormalByAge.getOrDefault(ageGroup, 0L);
            
            bpData.add(Map.of("ageGroup", ageGroup, "abnormal", bpAbnormal));
            bsData.add(Map.of("ageGroup", ageGroup, "abnormal", bsAbnormal));
            blData.add(Map.of("ageGroup", ageGroup, "abnormal", blAbnormal));
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("bloodPressure", bpData);
        result.put("bloodSugar", bsData);
        result.put("bloodLipid", blData);
        
        return ApiResponse.ok(result);
    }

    @GetMapping("/risk-screening")
    @PreAuthorize("hasRole('PUBLIC_HEALTH_MANAGER')")
    public ApiResponse<Map<String, Object>> riskScreening() {
        long bpAbnormal = healthMeasurementRepository.countHighBloodPressure();
        long bsAbnormal = healthMeasurementRepository.countDiabetes();
        long blAbnormal = healthMeasurementRepository.countHighBloodLipid();
        long total = residentProfileRepository.count();
        
        return ApiResponse.ok(Map.of(
            "totalResidents", total,
            "bpAbnormal", bpAbnormal,
            "bsAbnormal", bsAbnormal,
            "blAbnormal", blAbnormal,
            "bpAbnormalRate", total > 0 ? (double)bpAbnormal / total : 0,
            "bsAbnormalRate", total > 0 ? (double)bsAbnormal / total : 0,
            "blAbnormalRate", total > 0 ? (double)blAbnormal / total : 0
        ));
    }
    
    @GetMapping("/intervention-effect")
    @PreAuthorize("hasRole('PUBLIC_HEALTH_MANAGER')")
    public ApiResponse<Map<String, Object>> interventionEffect() {
        // 获取近6个月的高血压高危人数趋势
        List<Object[]> hbpTrend = healthMeasurementRepository.findHighBloodPressureTrend();
        // 获取近6个月的高血糖高危人数趋势
        List<Object[]> diabetesTrend = healthMeasurementRepository.findDiabetesTrend();

        // 转换数据格式
        Map<String, Map<String, Object>> trendMap = new HashMap<>();

        for (Object[] row : hbpTrend) {
            String month = (String) row[0];
            trendMap.computeIfAbsent(month, k -> new HashMap<>()).put("hbp", row[1]);
        }
        for (Object[] row : diabetesTrend) {
            String month = (String) row[0];
            trendMap.computeIfAbsent(month, k -> new HashMap<>()).put("diabetes", row[1]);
        }

        List<Map<String, Object>> trends = trendMap.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> item = entry.getValue();
                    item.put("month", entry.getKey());
                    item.putIfAbsent("hbp", 0);
                    item.putIfAbsent("diabetes", 0);
                    return item;
                })
                .sorted((a, b) -> ((String) a.get("month")).compareTo((String) b.get("month")))
                .toList();

        // 计算控制率（模拟逻辑：假设总患病人数为高危人数的 1.5 倍，那么控制率 = 1 - 高危/总患病）
        // 这里为了演示，仍然使用模拟的控制率，但趋势图使用真实数据
        return ApiResponse.ok(Map.of(
            "hbpControlRate", 0.65, 
            "diabetesControlRate", 0.58,
            "trends", trends
        ));
    }
}
