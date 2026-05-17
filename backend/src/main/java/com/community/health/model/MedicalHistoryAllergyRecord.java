package com.community.health.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "medical_history_allergy_records")
public class MedicalHistoryAllergyRecord extends BaseEntity {

    @Column(nullable = false)
    private Long medicalHistoryId;

    @Column(nullable = false, length = 120)
    private String allergen;

    @Column(length = 255)
    private String allergicReaction;
}
