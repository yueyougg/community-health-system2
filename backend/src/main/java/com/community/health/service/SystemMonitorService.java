package com.community.health.service;

import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class SystemMonitorService {

    private final Instant startTime;
    private final RuntimeMXBean runtimeMXBean;
    private final OperatingSystemMXBean osMXBean;
    private final com.sun.management.OperatingSystemMXBean sunOsMXBean;

    // 日志抑制机制
    private long lastCpuLogTime = 0;
    private long lastMemoryLogTime = 0;
    private static final long LOG_INTERVAL_MS = 30000; // 30秒记录一次详细日志

    public SystemMonitorService() {
        this.startTime = Instant.now();
        this.runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        this.osMXBean = ManagementFactory.getOperatingSystemMXBean();
        this.sunOsMXBean = (com.sun.management.OperatingSystemMXBean) osMXBean;
    }

    /**
     * 获取系统运行时间（实时）
     */
    public Map<String, Object> getUptimeInfo() {
        Map<String, Object> info = new HashMap<>();

        Instant now = Instant.now();
        Duration uptime = Duration.between(startTime, now);

        long days = uptime.toDays();
        long hours = uptime.toHoursPart();
        long minutes = uptime.toMinutesPart();
        long seconds = uptime.toSecondsPart();

        // 格式化显示
        StringBuilder uptimeStr = new StringBuilder();
        if (days > 0) {
            uptimeStr.append(days).append("天 ");
        }
        uptimeStr.append(String.format("%02d:%02d:%02d", hours, minutes, seconds));

        info.put("startTime", LocalDateTime.ofInstant(startTime, ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        info.put("uptimeSeconds", uptime.getSeconds());
        info.put("uptimeFormatted", uptimeStr.toString());
        info.put("days", days);
        info.put("hours", hours);
        info.put("minutes", minutes);
        info.put("seconds", seconds);

        return info;
    }

    /**
     * 获取CPU使用率（实时）
     */
    public Map<String, Object> getCpuInfo() {
        Map<String, Object> info = new HashMap<>();

        // 获取CPU使用率（0.0 - 1.0）
        double cpuLoad = sunOsMXBean.getCpuLoad();
        if (cpuLoad < 0) {
            cpuLoad = 0;
        }

        double cpuPercentage = cpuLoad * 100;

        info.put("cpuUsage", String.format("%.1f%%", cpuPercentage));
        info.put("cpuUsageValue", cpuPercentage);
        info.put("availableProcessors", osMXBean.getAvailableProcessors());
        info.put("systemLoadAverage", osMXBean.getSystemLoadAverage());

        // 日志抑制：每30秒记录一次详细日志
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCpuLogTime > LOG_INTERVAL_MS) {
            System.out.println(String.format(
                    "[系统监控] CPU使用率: %.1f%% (负载: %.2f), 核心数: %d",
                    cpuPercentage, osMXBean.getSystemLoadAverage(), osMXBean.getAvailableProcessors()));
            lastCpuLogTime = currentTime;
        }

        return info;
    }

    /**
     * 获取内存使用情况（实时）
     */
    public Map<String, Object> getMemoryInfo() {
        Map<String, Object> info = new HashMap<>();

        Runtime runtime = Runtime.getRuntime();

        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        double usedPercentage = (double) usedMemory / maxMemory * 100;

        info.put("usedMemory", formatBytes(usedMemory));
        info.put("freeMemory", formatBytes(freeMemory));
        info.put("totalMemory", formatBytes(totalMemory));
        info.put("maxMemory", formatBytes(maxMemory));
        info.put("usedMemoryBytes", usedMemory);
        info.put("maxMemoryBytes", maxMemory);
        info.put("memoryUsage", String.format("%.1f%%", usedPercentage));
        info.put("memoryUsageValue", usedPercentage);

        // 系统级内存信息
        long totalPhysicalMemory = sunOsMXBean.getTotalMemorySize();
        long freePhysicalMemory = sunOsMXBean.getFreeMemorySize();
        long usedPhysicalMemory = totalPhysicalMemory - freePhysicalMemory;

        info.put("systemTotalMemory", formatBytes(totalPhysicalMemory));
        info.put("systemFreeMemory", formatBytes(freePhysicalMemory));
        info.put("systemUsedMemory", formatBytes(usedPhysicalMemory));
        info.put("systemMemoryUsage", String.format("%.1f%%",
                (double) usedPhysicalMemory / totalPhysicalMemory * 100));

        // 日志抑制：每30秒记录一次详细日志
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMemoryLogTime > LOG_INTERVAL_MS) {
            System.out.println(String.format(
                    "[系统监控] 内存使用率: %.1f%% (已用: %s / %s), 系统内存: %s / %s",
                    usedPercentage, formatBytes(usedMemory), formatBytes(maxMemory),
                    formatBytes(usedPhysicalMemory), formatBytes(totalPhysicalMemory)));
            lastMemoryLogTime = currentTime;
        }

        return info;
    }

    /**
     * 获取完整的系统监控信息
     */
    public Map<String, Object> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("uptime", getUptimeInfo());
        stats.put("cpu", getCpuInfo());
        stats.put("memory", getMemoryInfo());
        stats.put("timestamp", System.currentTimeMillis());
        return stats;
    }

    private String formatBytes(long bytes) {
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
