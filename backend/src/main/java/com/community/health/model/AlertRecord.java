package com.community.health.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "alert_records")
public class AlertRecord extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "resident_id", insertable = false, updatable = false)
    private ResidentProfile resident;

    @Column(nullable = false, name = "resident_id")
    private Long residentId;

    @Column(nullable = false, length = 80)
    private String alertType;

    @Column(nullable = false, length = 30)
    private String level;

    @Column(nullable = false, length = 255)
    private String message;

    @Column(nullable = false, length = 30)
    private String status = "NEW";

    @Column(length = 80)
    private String relatedType;

    private Long relatedId;

    @Column(nullable = false)
    private Boolean notifiedResident = false;

    @Column(nullable = false)
    private Boolean notifiedDoctor = false;
}
