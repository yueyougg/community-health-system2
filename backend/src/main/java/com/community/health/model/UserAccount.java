package com.community.health.model;

import com.community.health.model.enums.Role;
import com.community.health.model.converter.RoleConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sys_users")
public class UserAccount extends BaseEntity {
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @JsonIgnore
    @Column(nullable = false, length = 120)
    private String password;

    @Convert(converter = RoleConverter.class)
    @Column(nullable = false, length = 50)
    private Role role;

    @Column(nullable = false)
    private Boolean enabled = true;

}
