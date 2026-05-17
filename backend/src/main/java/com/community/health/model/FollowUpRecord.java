package com.community.health.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "follow_up_records")
public class FollowUpRecord extends BaseEntity {

    @Column(nullable = false)
    private Long planId;

    @Column(nullable = false)
    private Long residentId;

    @Column(nullable = false)
    private LocalDateTime followUpTime;

    @Column(length = 50)
    private String doctorName;

    @Column(length = 255)
    private String symptoms;

    @Column(length = 255)
    private String physicalSigns;

    @Column(length = 255)
    private String guidance;

    @Column(length = 255)
    private String medicationAdjustment;

    private LocalDate nextReminderDate;
}
