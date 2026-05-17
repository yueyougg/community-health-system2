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
@Table(name = "medical_histories")
public class MedicalHistory extends BaseEntity {

    @Column(nullable = false)
    private Long residentId;

    @Column(length = 80)
    private String diseaseName;

    private LocalDate diagnosedDate;

    @Column(length = 255)
    private String treatmentStatus;

    @Column(length = 120)
    private String familyDisease;

    @Column(length = 120)
    private String familyRelation;

    @Column(length = 120)
    private String allergen;

    @Column(length = 255)
    private String allergicReaction;

    @Column(length = 120)
    private String smokingHabit;

    @Column(length = 120)
    private String drinkingHabit;

    @Column(length = 255)
    private String dietHabit;

    @Column(length = 255)
    private String exerciseHabit;

    @Column(length = 30)
    private String sourceType;
}
