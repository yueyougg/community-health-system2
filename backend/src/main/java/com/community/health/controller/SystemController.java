package com.community.health.controller;

import com.community.health.common.ApiResponse;
import com.community.health.model.OperationLog;
import com.community.health.model.LoginRecord;
import com.community.health.repository.OperationLogRepository;
import com.community.health.repository.UserAccountRepository;
import com.community.health.repository.BackupRecordRepository;
import com.community.health.repository.LoginRecordRepository;
import com.community.health.service.SystemMonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final OperationLogRepository operationLogRepository;
    private final UserAccountRepository userAccountRepository;
    private final BackupRecordRepository backupRecordRepository;
    private final LoginRecordRepository loginRecordRepository;
    private final DataSource dataSource;
    private final SystemMonitorService systemMonitorService;

    private static final Map<String, String> ROLE_CHINESE_MAP = Map.of(
            "RESIDENT", "社区居民",
            "DOCTOR", "社区医生",
            "ADMIN", "系统管理员",
            "PUBLIC_HEALTH_MANAGER", "公共卫生管理员"
    );

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public SystemController(OperationLogRepository operationLogRepository,
                            UserAccountRepository userAccountRepository,
                            BackupRecordRepository backupRecordRepository,
                            LoginRecordRepository loginRecordRepository,
                            DataSource dataSource,
                            SystemMonitorService systemMonitorService) {
        this.operationLogRepository = operationLogRepository;
        this.userAccountRepository = userAccountRepository;
        this.backupRecordRepository = backupRecordRepository;
        this.loginRecordRepository = loginRecordRepository;
        this.dataSource = dataSource;
        this.systemMonitorService = systemMonitorService;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> systemStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userAccountRepository.count());
        stats.put("totalLogs", operationLogRepository.count());
        stats.put("totalBackups", backupRecordRepository.count());

        // 获取当天登录数
        long todayLogins = loginRecordRepository.countByCreatedAtAfter(LocalDate.now().atStartOfDay());
        stats.put("todayLogins", todayLogins);

        // 获取CPU使用率（兼容旧版字符串格式）
        Map<String, Object> cpuInfo = systemMonitorService.getCpuInfo();
        stats.put("cpuUsage", cpuInfo.get("cpuUsage"));
        stats.put("cpuUsageDetail", cpuInfo);

        // 获取内存使用情况（兼容旧版字符串格式）
        Map<String, Object> memoryInfo = systemMonitorService.getMemoryInfo();
        stats.put("memoryUsage", memoryInfo.get("memoryUsage"));
        stats.put("memoryUsageDetail", memoryInfo);

        // 获取系统运行时间（实时）
        stats.put("uptime", systemMonitorService.getUptimeInfo());

        // 获取数据库大小
        stats.put("dbSize", getDatabaseSize());

        // 获取角色登录统计
        stats.put("roleLoginStats", getRoleLoginStats());

        // 获取最近登录记录
        stats.put("recentLogins", getRecentLogins());

        return ApiResponse.ok(stats);
    }

    @GetMapping("/monitor")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> getMonitorData() {
        // 只返回实时监控数据（运行时间、CPU、内存）
        return ApiResponse.ok(systemMonitorService.getSystemStats());
    }

    private String getDatabaseSize() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String databaseProductName = metaData.getDatabaseProductName().toLowerCase();

            if (databaseProductName.contains("mysql")) {
                return getMySQLDatabaseSize(connection);
            } else if (databaseProductName.contains("postgresql")) {
                return getPostgreSQLDatabaseSize(connection);
            } else {
                return "N/A";
            }
        } catch (Exception e) {
            return "N/A";
        }
    }

    private String getMySQLDatabaseSize(Connection connection) throws SQLException {
        try (ResultSet rs = connection.createStatement().executeQuery(
            "SELECT table_schema, SUM(data_length + index_length) AS size FROM information_schema.tables GROUP BY table_schema"
        )) {
            long totalSize = 0;
            while (rs.next()) {
                String schema = rs.getString("table_schema");
                if (!schema.equals("information_schema") && !schema.equals("mysql") && !schema.equals("performance_schema")) {
                    totalSize += rs.getLong("size");
                }
            }
            return String.format("%.2f MB", totalSize / 1024.0 / 1024.0);
        }
    }

    private String getPostgreSQLDatabaseSize(Connection connection) throws SQLException {
        try (ResultSet rs = connection.createStatement().executeQuery(
            "SELECT pg_size_pretty(pg_database_size(current_database()))"
        )) {
            if (rs.next()) {
                return rs.getString(1);
            }
            return "N/A";
        }
    }

    private List<Map<String, Object>> getRoleLoginStats() {
        List<LoginRecord> records = loginRecordRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        Map<String, Long> roleCount = records.stream()
            .collect(Collectors.groupingBy(LoginRecord::getLoginRole, Collectors.counting()));

        List<Map<String, Object>> result = new ArrayList<>();
        roleCount.forEach((role, count) -> {
            Map<String, Object> item = new HashMap<>();
            String chineseRole = ROLE_CHINESE_MAP.getOrDefault(role, role);
            item.put("name", chineseRole);
            item.put("value", count);
            result.add(item);
        });
        return result;
    }

    private List<Map<String, Object>> getRecentLogins() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));
        List<LoginRecord> records = loginRecordRepository.findAll(pageable).getContent();

        List<Map<String, Object>> result = new ArrayList<>();
        for (LoginRecord record : records) {
            Map<String, Object> item = new HashMap<>();
            item.put("username", record.getUser().getUsername());
            item.put("role", ROLE_CHINESE_MAP.getOrDefault(record.getLoginRole(), record.getLoginRole()));
            item.put("loginTime", record.getCreatedAt().format(DATE_TIME_FORMATTER));
            item.put("ipAddress", record.getIpAddress());
            result.add(item);
        }
        return result;
    }

    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<OperationLog>> logs() {
        return ApiResponse.ok(operationLogRepository.findAll(Sort.by(Sort.Direction.DESC, "id")));
    }

    @GetMapping("/backup-guide")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, String>> backupGuide() {
        return ApiResponse.ok(Map.of(
                "backup", "系统自动每14天备份一次，也可手动触发备份",
                "restore", "在备份列表中选择成功的备份文件进行恢复"
        ));
    }
}
