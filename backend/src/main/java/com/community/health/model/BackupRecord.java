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
@Table(name = "backup_records")
public class BackupRecord extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String fileName;

    @Column(length = 255)
    private String filePath;

    @Column(nullable = false)
    private Long fileSize;

    @Column(length = 20)
    private String status; // SUCCESS, FAILED

    @Column(length = 50)
    private String operator;

    @Column(length = 255)
    private String remark;
}
