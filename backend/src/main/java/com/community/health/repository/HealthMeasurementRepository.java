package com.community.health.repository;

import com.community.health.model.HealthMeasurement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HealthMeasurementRepository extends JpaRepository<HealthMeasurement, Long> {

    List<HealthMeasurement> findByResidentIdOrderByMeasuredAtDesc(Long residentId);

    List<HealthMeasurement> findTop20ByResidentIdOrderByMeasuredAtAsc(Long residentId);

    // 统计高血压人群（收缩压>=140 或 舒张压>=90）
    @org.springframework.data.jpa.repository.Query("select count(distinct h.residentId) from HealthMeasurement h where h.systolicBp >= 140 or h.diastolicBp >= 90")
    long countHighBloodPressure();

    // 统计高血糖人群（空腹血糖>=7.0）
    @org.springframework.data.jpa.repository.Query("select count(distinct h.residentId) from HealthMeasurement h where h.bloodSugar >= 7.0")
    long countDiabetes();
    
    // 查询高血压高危人群详情
    @org.springframework.data.jpa.repository.Query("select distinct h.residentId from HealthMeasurement h where h.systolicBp >= 140 or h.diastolicBp >= 90")
    List<Long> findHighBloodPressureRiskIds();
    
    // 查询糖尿病高危人群详情
    @org.springframework.data.jpa.repository.Query("select distinct h.residentId from HealthMeasurement h where h.bloodSugar >= 7.0")
    List<Long> findDiabetesRiskIds();
    
    // 按月统计高血压人数
    @org.springframework.data.jpa.repository.Query(value = "SELECT DATE_FORMAT(measured_at, '%Y-%m') as month, COUNT(DISTINCT resident_id) FROM health_measurements WHERE systolic_bp >= 140 OR diastolic_bp >= 90 GROUP BY month ORDER BY month DESC LIMIT 6", nativeQuery = true)
    List<Object[]> findHighBloodPressureTrend();

    // 按月统计高血糖人数
    @org.springframework.data.jpa.repository.Query(value = "SELECT DATE_FORMAT(measured_at, '%Y-%m') as month, COUNT(DISTINCT resident_id) FROM health_measurements WHERE blood_sugar >= 7.0 GROUP BY month ORDER BY month DESC LIMIT 6", nativeQuery = true)
    List<Object[]> findDiabetesTrend();

    @org.springframework.data.jpa.repository.Query("select count(distinct h.residentId) from HealthMeasurement h where h.bloodLipid >= 5.2")
    long countHighBloodLipid();

    @org.springframework.data.jpa.repository.Query("select count(h.id) from HealthMeasurement h where h.systolicBp >= 140 or h.diastolicBp >= 90")
    long countAbnormalBloodPressureRecords();

    @org.springframework.data.jpa.repository.Query("select count(h.id) from HealthMeasurement h where h.bloodSugar >= 7.0")
    long countAbnormalBloodSugarRecords();

    @org.springframework.data.jpa.repository.Query("select count(h.id) from HealthMeasurement h where h.bloodLipid >= 5.2")
    long countAbnormalBloodLipidRecords();

    // 查询有任何指标异常的居民ID列表
    @org.springframework.data.jpa.repository.Query("select distinct h.residentId from HealthMeasurement h where h.systolicBp >= 140 or h.diastolicBp >= 90 or h.bloodSugar >= 7.0 or h.bloodLipid >= 5.2")
    List<Long> findAnyAbnormalResidentIds();

    // 按性别和指标异常统计（血压）
    @org.springframework.data.jpa.repository.Query(value = "SELECT r.gender, COUNT(DISTINCT h.resident_id) FROM health_measurements h JOIN resident_profiles r ON h.resident_id = r.id WHERE h.systolic_bp >= 140 OR h.diastolic_bp >= 90 GROUP BY r.gender", nativeQuery = true)
    List<Object[]> countBloodPressureAbnormalByGender();

    // 按性别和指标异常统计（血糖）
    @org.springframework.data.jpa.repository.Query(value = "SELECT r.gender, COUNT(DISTINCT h.resident_id) FROM health_measurements h JOIN resident_profiles r ON h.resident_id = r.id WHERE h.blood_sugar >= 7.0 GROUP BY r.gender", nativeQuery = true)
    List<Object[]> countBloodSugarAbnormalByGender();

    // 按性别和指标异常统计（血脂）
    @org.springframework.data.jpa.repository.Query(value = "SELECT r.gender, COUNT(DISTINCT h.resident_id) FROM health_measurements h JOIN resident_profiles r ON h.resident_id = r.id WHERE h.blood_lipid >= 5.2 GROUP BY r.gender", nativeQuery = true)
    List<Object[]> countBloodLipidAbnormalByGender();

    // 按年龄段统计血压异常人数
    @org.springframework.data.jpa.repository.Query(value = "SELECT " +
            "CASE " +
            "  WHEN TIMESTAMPDIFF(YEAR, r.birth_date, CURDATE()) < 30 THEN '30岁以下' " +
            "  WHEN TIMESTAMPDIFF(YEAR, r.birth_date, CURDATE()) BETWEEN 30 AND 39 THEN '30-39岁' " +
            "  WHEN TIMESTAMPDIFF(YEAR, r.birth_date, CURDATE()) BETWEEN 40 AND 49 THEN '40-49岁' " +
            "  WHEN TIMESTAMPDIFF(YEAR, r.birth_date, CURDATE()) BETWEEN 50 AND 59 THEN '50-59岁' " +
            "  WHEN TIMESTAMPDIFF(YEAR, r.birth_date, CURDATE()) BETWEEN 60 AND 69 THEN '60-69岁' " +
            "  ELSE '70岁及以上' " +
            "END as age_group, " +
            "COUNT(DISTINCT h.resident_id) as abnormal_count " +
            "FROM health_measurements h " +
            "JOIN resident_profiles r ON h.resident_id = r.id " +
            "WHERE h.systolic_bp >= 140 OR h.diastolic_bp >= 90 " +
            "GROUP BY age_group " +
            "ORDER BY FIELD(age_group, '30岁以下', '30-39岁', '40-49岁', '50-59岁', '60-69岁', '70岁及以上')", nativeQuery = true)
    List<Object[]> countBloodPressureAbnormalByAgeGroup();

    // 按年龄段统计血糖异常人数
    @org.springframework.data.jpa.repository.Query(value = "SELECT " +
            "CASE " +
            "  WHEN TIMESTAMPDIFF(YEAR, r.birth_date, CURDATE()) < 30 THEN '30岁以下' " +
            "  WHEN TIMESTAMPDIFF(YEAR, r.birth_date, CURDATE()) BETWEEN 30 AND 39 THEN '30-39岁' " +
            "  WHEN TIMESTAMPDIFF(YEAR, r.birth_date, CURDATE()) BETWEEN 40 AND 49 THEN '40-49岁' " +
            "  WHEN TIMESTAMPDIFF(YEAR, r.birth_date, CURDATE()) BETWEEN 50 AND 59 THEN '50-59岁' " +
            "  WHEN TIMESTAMPDIFF(YEAR, r.birth_date, CURDATE()) BETWEEN 60 AND 69 THEN '60-69岁' " +
            "  ELSE '70岁及以上' " +
            "END as age_group, " +
            "COUNT(DISTINCT h.resident_id) as abnormal_count " +
            "FROM health_measurements h " +
            "JOIN resident_profiles r ON h.resident_id = r.id " +
            "WHERE h.blood_sugar >= 7.0 " +
            "GROUP BY age_group " +
            "ORDER BY FIELD(age_group, '30岁以下', '30-39岁', '40-49岁', '50-59岁', '60-69岁', '70岁及以上')", nativeQuery = true)
    List<Object[]> countBloodSugarAbnormalByAgeGroup();

    // 按年龄段统计血脂异常人数
    @org.springframework.data.jpa.repository.Query(value = "SELECT " +
            "CASE " +
            "  WHEN TIMESTAMPDIFF(YEAR, r.birth_date, CURDATE()) < 30 THEN '30岁以下' " +
            "  WHEN TIMESTAMPDIFF(YEAR, r.birth_date, CURDATE()) BETWEEN 30 AND 39 THEN '30-39岁' " +
            "  WHEN TIMESTAMPDIFF(YEAR, r.birth_date, CURDATE()) BETWEEN 40 AND 49 THEN '40-49岁' " +
            "  WHEN TIMESTAMPDIFF(YEAR, r.birth_date, CURDATE()) BETWEEN 50 AND 59 THEN '50-59岁' " +
            "  WHEN TIMESTAMPDIFF(YEAR, r.birth_date, CURDATE()) BETWEEN 60 AND 69 THEN '60-69岁' " +
            "  ELSE '70岁及以上' " +
            "END as age_group, " +
            "COUNT(DISTINCT h.resident_id) as abnormal_count " +
            "FROM health_measurements h " +
            "JOIN resident_profiles r ON h.resident_id = r.id " +
            "WHERE h.blood_lipid >= 5.2 " +
            "GROUP BY age_group " +
            "ORDER BY FIELD(age_group, '30岁以下', '30-39岁', '40-49岁', '50-59岁', '60-69岁', '70岁及以上')", nativeQuery = true)
    List<Object[]> countBloodLipidAbnormalByAgeGroup();
}
