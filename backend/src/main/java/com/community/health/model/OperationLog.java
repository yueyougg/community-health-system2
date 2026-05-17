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
@Table(name = "operation_logs")
public class OperationLog extends BaseEntity {

    @Column(length = 50)
    private String username;

    @Column(length = 80)
    private String moduleName;

    @Column(length = 80)
    private String actionName;

    @Column(length = 20)
    private String httpMethod;

    @Column(length = 255)
    private String requestPath;

    @Column(length = 80)
    private String ipAddress;
}
