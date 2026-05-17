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
@Table(name = "intervention_records")
public class InterventionRecord extends BaseEntity {

    private Long residentId;

    @Column(length = 50)
    private String residentName;

    private Long doctorId;

    @Column(length = 50)
    private String doctorName;

    @Column(nullable = false, length = 80)
    private String interventionType;

    @Column(nullable = false, length = 80)
    private String targetMetric;

    @Column(precision = 10, scale = 2)
    private BigDecimal beforeValue;

    @Column(precision = 10, scale = 2)
    private BigDecimal afterValue;

    @Column(nullable = false)
    private LocalDateTime interventionDate;

    @Column(length = 1000)
    private String notes;

    @Column(length = 30)
    private String effectLevel;

    @Column(length = 500)
    private String autoEvaluation;
}
