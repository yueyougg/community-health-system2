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
@Table(name = "vaccination_records")
public class VaccinationRecord extends BaseEntity {

    @Column(nullable = false)
    private Long residentId;

    @Column(nullable = false, length = 120)
    private String vaccineName;

    private LocalDate vaccinatedAt;

    @Column(length = 120)
    private String institution;

    @Column(length = 80)
    private String batchNo;
}
