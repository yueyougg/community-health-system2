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
@Table(name = "follow_up_plans")
public class FollowUpPlan extends BaseEntity {

    @Column(nullable = false)
    private Long residentId;

    @Column(nullable = false, length = 120)
    private String diseaseType;

    @Column(nullable = false)
    private Integer periodDays;

    private LocalDate nextFollowUpDate;

    @Column(length = 255)
    private String content;

    @Column(nullable = false)
    private Boolean active = true;
}
