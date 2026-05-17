package com.community.health.controller;

import com.community.health.audit.AuditLog;
import com.community.health.common.ApiResponse;
import com.community.health.model.BackupRecord;
import com.community.health.repository.BackupRecordRepository;
import com.community.health.service.DatabaseBackupService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/backups")
@PreAuthorize("hasRole('ADMIN')")
public class BackupController {

    private final BackupRecordRepository backupRecordRepository;
    private final DatabaseBackupService databaseBackupService;

    @Value("${backup.directory:./backups}")
    private String backupDirectory;

    @Autowired
    public BackupController(BackupRecordRepository backupRecordRepository,
                           DatabaseBackupService databaseBackupService) {
        this.backupRecordRepository = backupRecordRepository;
        this.databaseBackupService = databaseBackupService;
    }

    @PostConstruct
    public void init() {
        try {
            Path backupPath = Paths.get(backupDirectory);
            if (!Files.exists(backupPath)) {
                Files.createDirectories(backupPath);
                System.out.println("[备份系统] 创建备份目录: " + backupPath.toAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("[备份系统] 创建备份目录失败: " + e.getMessage());
        }
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        // 直接从backups文件夹获取sql文件信息
        List<Map<String, Object>> result = new ArrayList<>();
        File backupDir = new File(backupDirectory);
        if (backupDir.exists() && backupDir.isDirectory()) {
            File[] files = backupDir.listFiles((dir, name) -> name.endsWith(".sql"));
            if (files != null) {
                // 按修改时间排序
                Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                
                for (File file : files) {
                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("fileName", file.getName());
                    fileInfo.put("filePath", file.getAbsolutePath());
                    fileInfo.put("fileSize", file.length());
                    fileInfo.put("backupTime", new Date(file.lastModified()));
                    
                    // 从文件名解析操作员信息
                    // 文件名格式: backup-{operator}-{timestamp}.sql 或 backup-java-{timestamp}.sql 或 backup-auto-{timestamp}.sql
                    String fileName = file.getName();
                    if (fileName.startsWith("backup-auto-")) {
                        fileInfo.put("operator", "自动备份");
                    } else if (fileName.startsWith("backup-java-")) {
                        fileInfo.put("operator", "管理员");
                    } else if (fileName.startsWith("backup-")) {
                        // 格式: backup-{operator}-{timestamp}.sql
                        String[] parts = fileName.split("-");
                        if (parts.length >= 3) {
                            fileInfo.put("operator", parts[1]);
                        } else {
                            fileInfo.put("operator", "未知");
                        }
                    } else {
                        fileInfo.put("operator", "未知");
                    }
                    result.add(fileInfo);
                }
            }
        }
        
        return ApiResponse.ok(result);
    }

    @GetMapping("/next-backup")
    public ApiResponse<Map<String, Object>> getNextBackupTime() {
        Map<String, Object> result = new HashMap<>();
        
        // 计算下次备份时间
        // cron表达式: 0 0 2 1/7 * * 表示每7天的凌晨2点执行
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextBackup = calculateNextBackupTime(now);
        
        long daysRemaining = ChronoUnit.DAYS.between(now, nextBackup);
        long hoursRemaining = ChronoUnit.HOURS.between(now, nextBackup) % 24;
        long minutesRemaining = ChronoUnit.MINUTES.between(now, nextBackup) % 60;
        
        result.put("nextBackupTime", nextBackup.toString());
        result.put("daysRemaining", daysRemaining);
        result.put("hoursRemaining", hoursRemaining);
        result.put("minutesRemaining", minutesRemaining);
        result.put("totalHoursRemaining", ChronoUnit.HOURS.between(now, nextBackup));
        
        return ApiResponse.ok(result);
    }
    
    private LocalDateTime calculateNextBackupTime(LocalDateTime now) {
        // 每7天的凌晨2点执行
        // 计算从本月1号开始，每7天的凌晨2点
        LocalDateTime firstOfMonth = now.withDayOfMonth(1).withHour(2).withMinute(0).withSecond(0).withNano(0);
        
        LocalDateTime nextBackup = firstOfMonth;
        while (nextBackup.isBefore(now) || nextBackup.isEqual(now)) {
            nextBackup = nextBackup.plusDays(7);
        }
        
        return nextBackup;
    }

    @GetMapping("/test")
    public ApiResponse<Map<String, Object>> testEnvironment() {
        Map<String, Object> result = new HashMap<>();

        try {
            Path backupPath = Paths.get(backupDirectory);
            result.put("backupDirectory", backupDirectory);
            result.put("directoryExists", Files.exists(backupPath));
            result.put("directoryWritable", Files.isWritable(backupPath));
            result.put("absolutePath", backupPath.toAbsolutePath().toString());
            result.put("jdbcBackup", "可用（纯Java实现，无需外部命令）");
        } catch (Exception e) {
            result.put("error", "检查失败: " + e.getMessage());
        }

        return ApiResponse.ok(result);
    }

    @PostMapping
    @AuditLog(module = "系统维护", action = "数据备份")
    public ApiResponse<BackupRecord> backup(Authentication authentication) {
        try {
            String currentUser = authentication.getName();
            System.out.println("[备份接口] 开始执行手动备份，操作员: " + currentUser);
            BackupRecord record = databaseBackupService.backupDatabase(currentUser);

            if ("SUCCESS".equals(record.getStatus())) {
                return ApiResponse.ok("备份成功，文件大小: " + formatFileSize(record.getFileSize()), record);
            } else {
                return ApiResponse.fail("备份失败: " + record.getRemark());
            }
        } catch (Exception e) {
            System.err.println("[备份接口] 备份异常: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.fail("备份失败: " + e.getMessage());
        }
    }

    @PostMapping("/restore")
    @AuditLog(module = "系统维护", action = "数据库恢复")
    public ApiResponse<Void> restore(@RequestParam String filePath) {
        try {
            File backupFile = new File(filePath);
            if (!backupFile.exists()) {
                return ApiResponse.fail("备份文件不存在: " + filePath);
            }

            System.out.println("[恢复接口] 开始恢复数据库，文件: " + filePath);

            // 直接根据文件路径恢复
            boolean success = databaseBackupService.restoreDatabaseByPath(filePath);

            if (success) {
                return ApiResponse.ok("数据恢复成功", null);
            } else {
                return ApiResponse.fail("数据恢复失败，请查看日志了解详情");
            }
        } catch (RuntimeException e) {
            System.err.println("[恢复接口] 恢复异常: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.fail("数据恢复失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    @AuditLog(module = "系统维护", action = "删除备份")
    public ApiResponse<String> delete(@RequestParam String filePath) {
        try {
            File backupFile = new File(filePath);
            if (backupFile.exists()) {
                backupFile.delete();
            }
            return ApiResponse.ok("备份文件已删除", "ok");
        } catch (Exception e) {
            return ApiResponse.fail("删除失败: " + e.getMessage());
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}
