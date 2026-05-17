package com.community.health.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "health_assessments")
public class HealthAssessment extends BaseEntity {

    @Column(nullable = false)
    private Long residentId;

    @Column(nullable = false)
    private Long doctorId;

    @Column(nullable = false)
    private LocalDateTime assessmentDate;

    private Integer healthScore;

    @Column(length = 20)
    private String healthLevel; // e.g. EXCELLENT, GOOD, FAIR, POOR

    @Column(length = 80)
    private String diseaseType;

    @Column(length = 1000)
    private String evaluation;

    @Column(length = 1000)
    private String guidance;

    @Column(length = 50)
    private String doctorName;

    @Override
    public String toString() {
        return "HealthAssessment{" +
                "residentId=" + residentId +
                ", doctorId=" + doctorId +
                ", assessmentDate=" + assessmentDate +
                ", healthScore=" + healthScore +
                ", healthLevel='" + healthLevel + '\'' +
                ", diseaseType='" + diseaseType + '\'' +
                ", evaluation='" + evaluation + '\'' +
                ", guidance='" + guidance + '\'' +
                ", doctorName='" + doctorName + '\'' +
                '}';
    }
}
