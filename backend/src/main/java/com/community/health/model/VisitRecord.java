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
@Table(name = "visit_records")
public class VisitRecord extends BaseEntity {

    @Column(nullable = false)
    private Long residentId;

    @Column(nullable = false)
    private LocalDateTime visitTime;

    @Column(length = 120)
    private String organization;

    @Column(length = 255)
    private String diagnosis;

    @Column(columnDefinition = "TEXT")
    private String prescription;

    @Column(columnDefinition = "TEXT")
    private String examReport;
}
