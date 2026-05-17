package com.community.health.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "resident_profiles")
public class ResidentProfile extends BaseEntity {

    @OneToOne
    @JsonIgnore
    @JoinColumn(name = "user_id", unique = true)
    private UserAccount user;

    @Column(nullable = false, unique = true, length = 64)
    private String archiveNo;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 10)
    private String gender;

    private LocalDate birthDate;

    @Column(unique = true, length = 30)
    private String idCard;

    @Column(length = 30)
    private String phone;

    @Column(length = 255)
    private String address;

    @Column(length = 50)
    private String occupation;

    private LocalDate archiveDate;

    @Column(length = 100)
    private String archiveOrg;
}
