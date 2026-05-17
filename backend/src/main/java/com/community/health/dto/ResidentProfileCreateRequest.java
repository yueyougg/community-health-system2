package com.community.health.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ResidentProfileCreateRequest {

    private Long userId;

    private String name;

    private String gender;

    private LocalDate birthDate;

    private String idCard;

    private String phone;

    private String address;

    private String occupation;

    private LocalDate archiveDate;

    private String archiveOrg;
}
