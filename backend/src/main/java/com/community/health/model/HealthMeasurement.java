package com.community.health.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "health_measurements")
public class HealthMeasurement extends BaseEntity {

    @Column(nullable = false)
    private Long residentId;

    @Column(nullable = false)
    private LocalDateTime measuredAt;

    @Column(length = 120)
    private String location;

    @Column(precision = 6, scale = 2)
    private BigDecimal heightCm;

    @Column(precision = 6, scale = 2)
    private BigDecimal weightKg;

    private Integer systolicBp;

    private Integer diastolicBp;

    @Column(precision = 6, scale = 2)
    private BigDecimal bloodSugar;

    @Column(precision = 6, scale = 2)
    private BigDecimal bloodLipid;

    private Integer heartRate;

    @Column(length = 30)
    private String sourceType;

    @Column(nullable = false)
    private Boolean alertFlag = false;
}
