package com.community.health.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "medical_history_diseases")
public class MedicalHistoryDisease extends BaseEntity {

    @Column(nullable = false)
    private Long medicalHistoryId;

    @Column(nullable = false, length = 80)
    private String diseaseName;

    @Column(length = 255)
    private String treatmentStatus;

    private LocalDate checkedAt;
}
