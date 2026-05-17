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
@Table(name = "medication_records")
public class MedicationRecord extends BaseEntity {

    @Column(nullable = false)
    private Long residentId;

    @Column(nullable = false, length = 120)
    private String drugName;

    private LocalDate startDate;

    private LocalDate endDate;

    @Column(length = 120)
    private String dosage;

    @Column(length = 120)
    private String usageMethod;

    @Column(length = 255)
    private String reason;
}
