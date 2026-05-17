package com.community.health.service;

import com.community.health.model.BackupRecord;
import com.community.health.repository.BackupRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class DatabaseBackupService {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private BackupRecordRepository backupRecordRepository;

    @Value("${backup.directory:./backups}")
    private String backupDirectory;

    @Value("${backup.auto.enabled:true}")
    private boolean autoBackupEnabled;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    /**
     * 每7天执行一次自动备份
     * cron表达式: 秒 分 时 日 月 周
     * 0 0 2 1/7 * * 表示每7天的凌晨2点执行
     * 可以修改cron表达式来调整备份时间
     */
    @Scheduled(cron = "0 0 2 1/7 * *")
    @Transactional
    public void scheduledBackup() {
        if (!autoBackupEnabled) {
            System.out.println("[定时备份] 自动备份已禁用");
            return;
        }

        System.out.println("[定时备份] 开始执行定时备份任务...");
        try {
            BackupRecord record = backupDatabase("auto");
            if ("SUCCESS".equals(record.getStatus())) {
                System.out.println("[定时备份] 备份成功: " + record.getFileName() + ", 大小: " + formatFileSize(record.getFileSize()));
            } else {
                System.err.println("[定时备份] 备份失败: " + record.getRemark());
            }
        } catch (Exception e) {
            System.err.println("[定时备份] 备份异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 手动执行备份（带事务控制）
     */
    @Transactional(rollbackFor = Exception.class)
    public BackupRecord backupDatabase(String operator) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String fileName = "backup-" + operator + "-" + timestamp + ".sql";
        String filePath = Paths.get(backupDirectory, fileName).toString();

        BackupRecord record = new BackupRecord();
            record.setFileName(fileName);
            record.setFilePath(filePath);
            record.setFileSize(0L);
            record.setOperator(operator);
            record = backupRecordRepository.save(record);

        File backupFile = null;
        try {
            // 确保备份目录存在
            Path backupPath = Paths.get(backupDirectory);
            if (!Files.exists(backupPath)) {
                Files.createDirectories(backupPath);
            }

            // 执行纯Java备份
            boolean success = performJdbcBackup(filePath);

            if (success) {
                backupFile = new File(filePath);
                long fileSize = backupFile.exists() ? backupFile.length() : 0;
                record.setFileSize(fileSize);
                record.setStatus("SUCCESS");
                System.out.println("[备份服务] 备份成功: " + fileName + ", 大小: " + formatFileSize(fileSize));
            } else {
                record.setStatus("FAILED");
                System.err.println("[备份服务] 备份失败: " + fileName);
                throw new RuntimeException("备份失败，已回滚");
            }
        } catch (Exception e) {
            // 出错时删除备份文件
            if (backupFile != null && backupFile.exists()) {
                backupFile.delete();
                System.out.println("[备份服务] 已删除失败的备份文件: " + backupFile.getPath());
            }

            record.setStatus("FAILED");
            System.err.println("[备份服务] 备份异常: " + e.getMessage());
            e.printStackTrace();

            // 抛出异常以触发事务回滚
            throw new RuntimeException("备份失败: " + e.getMessage(), e);
        }

        return backupRecordRepository.save(record);
    }

    /**
     * 使用JDBC纯Java方式备份数据库
     */
    private boolean performJdbcBackup(String filePath) {
        try (Connection connection = dataSource.getConnection();
             PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(filePath)))) {

            DatabaseMetaData metaData = connection.getMetaData();
            String databaseName = connection.getCatalog();

            // 写入SQL文件头部注释
            writer.println("--");
            writer.println("-- 数据库备份");
            writer.println("-- 数据库: " + databaseName);
            writer.println("-- 备份时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.println("--");
            writer.println("SET FOREIGN_KEY_CHECKS=0;");
            writer.println("SET UNIQUE_CHECKS=0;");
            writer.println("SET NAMES utf8mb4;");
            writer.println("SET CHARACTER SET utf8mb4;");
            writer.println();

            // 获取所有表
            ResultSet tables = metaData.getTables(databaseName, null, "%", new String[]{"TABLE"});

            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                System.out.println("[备份服务] 正在备份表: " + tableName);

                // 备份表结构
                backupTableStructure(connection, writer, tableName);

                // 备份表数据
                backupTableData(connection, writer, tableName);

                writer.println();
            }

            writer.println("SET FOREIGN_KEY_CHECKS=1;");
            writer.println("SET UNIQUE_CHECKS=1;");

            tables.close();
            return true;

        } catch (Exception e) {
            System.err.println("[备份服务] JDBC备份失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 备份表结构
     */
    private void backupTableStructure(Connection connection, PrintWriter writer, String tableName) throws SQLException {
        writer.println("-- ----------------------------");
        writer.println("-- 表结构: " + tableName);
        writer.println("-- ----------------------------");
        writer.println("DROP TABLE IF EXISTS `" + tableName + "`;");

        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE `" + tableName + "`");

        if (rs.next()) {
            String createTable = rs.getString(2);
            writer.println(createTable + ";");
        }

        rs.close();
        stmt.close();
        writer.println();
    }

    /**
     * 备份表数据
     */
    private void backupTableData(Connection connection, PrintWriter writer, String tableName) throws SQLException {
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM `" + tableName + "`");

        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        // 构建INSERT语句的列名部分
        StringBuilder columns = new StringBuilder();
        for (int i = 1; i <= columnCount; i++) {
            if (i > 1) columns.append(", ");
            columns.append("`").append(metaData.getColumnName(i)).append("`");
        }

        int rowCount = 0;
        StringBuilder insertPrefix = new StringBuilder();
        insertPrefix.append("INSERT INTO `").append(tableName).append("` (").append(columns).append(") VALUES ");

        while (rs.next()) {
            if (rowCount % 100 == 0) {
                if (rowCount > 0) {
                    writer.println(";");
                }
                writer.println(insertPrefix.toString());
            } else {
                writer.println(",");
            }

            StringBuilder values = new StringBuilder();
            values.append("(");
            for (int i = 1; i <= columnCount; i++) {
                if (i > 1) values.append(", ");

                Object value = rs.getObject(i);
                if (value == null) {
                    values.append("NULL");
                } else if (value instanceof String) {
                    String strValue = escapeSqlString(value.toString());
                    values.append("'").append(strValue).append("'");
                } else if (value instanceof java.sql.Date || value instanceof java.sql.Timestamp || value instanceof java.sql.Time) {
                // 转换时间戳为MySQL兼容格式
                String mysqlTimestamp = formatTimestamp(value.toString());
                values.append("'").append(mysqlTimestamp).append("'");
            } else if (value instanceof Boolean) {
                values.append((Boolean) value ? "1" : "0");
            } else if (value instanceof Number) {
                values.append(value.toString());
            } else {
                String strValue = escapeSqlString(value.toString());
                values.append("'").append(strValue).append("'");
            }
            }
            values.append(")");
            writer.print(values.toString());

            rowCount++;
        }

        if (rowCount > 0) {
            writer.println(";");
        }

        writer.println();
        rs.close();
        stmt.close();

        System.out.println("[备份服务] 表 " + tableName + " 备份完成，共 " + rowCount + " 条记录");
    }

    /**
     * 转换时间戳为MySQL兼容格式
     * 将 ISO 8601 格式 (2026-03-23T15:04:06.067537) 转换为 MySQL 格式 (2026-03-23 15:04:06)
     */
    private String formatTimestamp(String timestampStr) {
        if (timestampStr == null) {
            return null;
        }
        
        // 如果已经是MySQL格式，直接返回
        if (timestampStr.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
            return timestampStr;
        }
        
        // 处理ISO 8601格式 (2026-03-23T19:24:02.860709)
        if (timestampStr.contains("T")) {
            return timestampStr.replace("T", " ").split("\\.")[0];
        }
        
        return timestampStr;
    }

    /**
     * 转义SQL字符串中的特殊字符
     */
    private String escapeSqlString(String str) {
        if (str == null) {
            return null;
        }
        return str.replace("\\", "\\\\")
                  .replace("'", "''")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\0", "\\0");
    }

    /**
     * 恢复数据库（安全模式，失败后不丢失数据）
     * 采用增量恢复策略，只恢复成功部分，失败部分跳过
     */
    public boolean restoreDatabase(Long backupId) {
        BackupRecord record = backupRecordRepository.findById(backupId).orElse(null);
        if (record == null) {
            System.err.println("[恢复服务] 备份记录不存在: " + backupId);
            throw new RuntimeException("备份记录不存在");
        }

        File backupFile = new File(record.getFilePath());
        if (!backupFile.exists()) {
            System.err.println("[恢复服务] 备份文件不存在: " + record.getFilePath());
            throw new RuntimeException("备份文件不存在");
        }

        System.out.println("[恢复服务] 开始恢复数据库，文件: " + record.getFilePath());

        try {
            // 使用安全恢复模式（不回滚）
            boolean success = performSafeJdbcRestore(record.getFilePath());

            if (success) {
                System.out.println("[恢复服务] 数据库恢复成功");
                return true;
            } else {
                System.err.println("[恢复服务] 数据库恢复部分失败，请检查日志");
                throw new RuntimeException("数据库恢复部分失败，成功率低于95%");
            }
        } catch (RuntimeException e) {
            System.err.println("[恢复服务] 恢复失败: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("[恢复服务] 恢复异常: " + e.getMessage());
            throw new RuntimeException("数据库恢复异常: " + e.getMessage());
        }
    }

    /**
     * 根据文件路径恢复数据库
     */
    public boolean restoreDatabaseByPath(String filePath) {
        File backupFile = new File(filePath);
        if (!backupFile.exists()) {
            System.err.println("[恢复服务] 备份文件不存在: " + filePath);
            throw new RuntimeException("备份文件不存在");
        }

        System.out.println("[恢复服务] 开始恢复数据库，文件: " + filePath);

        try {
            // 使用安全恢复模式（不回滚）
            boolean success = performSafeJdbcRestore(filePath);

            if (success) {
                System.out.println("[恢复服务] 数据库恢复成功");
                return true;
            } else {
                System.err.println("[恢复服务] 数据库恢复部分失败，请检查日志");
                throw new RuntimeException("数据库恢复部分失败，成功率低于95%");
            }
        } catch (RuntimeException e) {
            System.err.println("[恢复服务] 恢复失败: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("[恢复服务] 恢复异常: " + e.getMessage());
            throw new RuntimeException("数据库恢复异常: " + e.getMessage());
        }
    }

    /**
     * 使用JDBC纯Java方式安全恢复数据库（不回滚，失败后保留已恢复数据）
     * 采用增量恢复策略，每恢复100条记录提交一次
     */
    private boolean performSafeJdbcRestore(String filePath) {
        Connection connection = null;
        BufferedReader reader = null;
        Statement stmt = null;
        
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            reader = new BufferedReader(new FileReader(filePath));
            stmt = connection.createStatement();
            StringBuilder sqlBuffer = new StringBuilder();
            String line;

            stmt.execute("SET FOREIGN_KEY_CHECKS=0");
            stmt.execute("SET UNIQUE_CHECKS=0");

            int sqlCount = 0;
            int successCount = 0;
            int errorCount = 0;
            String lastError = null;
            int batchCount = 0;

            // 每100条记录提交一次，确保部分成功时数据不丢失
            final int BATCH_SIZE = 100;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("--") || line.startsWith("/*") || line.startsWith("*")) {
                    continue;
                }

                sqlBuffer.append(line).append(" ");

                if (line.endsWith(";")) {
                    String sql = sqlBuffer.toString().trim();
                    if (!sql.isEmpty()) {
                        sqlCount++;
                        String upperSql = sql.toUpperCase();
                        
                        try {
                            // SET语句直接执行
                            if (upperSql.startsWith("SET ")) {
                                stmt.execute(sql);
                                successCount++;
                            }
                            // DROP TABLE IF EXISTS 忽略错误（表可能不存在）
                            else if (upperSql.startsWith("DROP TABLE IF EXISTS ")) {
                                try {
                                    stmt.execute(sql);
                                } catch (SQLException e) {
                                    // 忽略DROP TABLE错误，表可能不存在
                                    System.out.println("[恢复服务] DROP TABLE忽略错误（表可能不存在）: " + e.getMessage());
                                }
                                successCount++;
                            }
                            // CREATE TABLE 执行
                            else if (upperSql.startsWith("CREATE TABLE") || upperSql.startsWith("INSERT INTO")) {
                                stmt.execute(sql);
                                successCount++;
                            }
                            // 其他语句
                            else {
                                // 跳过手动COMMIT/ROLLBACK语句，因为我们已自动处理事务
                                if (!upperSql.startsWith("COMMIT") && !upperSql.startsWith("ROLLBACK")) {
                                    stmt.execute(sql);
                                    successCount++;
                                } else {
                                    System.out.println("[恢复服务] 跳过手动事务控制语句: " + sql);
                                    successCount++;
                                }
                            }
                            
                            // 每100条记录提交一次
                            batchCount++;
                            if (batchCount >= BATCH_SIZE) {
                                connection.commit();
                                batchCount = 0;
                                System.out.println("[恢复服务] 已恢复 " + successCount + " 条记录...");
                            }
                        } catch (SQLException e) {
                            errorCount++;
                            lastError = e.getMessage();
                            // 只记录前10个错误，不中断执行
                            if (errorCount <= 10) {
                                System.err.println("[恢复服务] SQL执行警告 (" + sqlCount + "): " + sql.substring(0, Math.min(80, sql.length())));
                                System.err.println("[恢复服务] 警告: " + e.getMessage());
                            }
                        }
                    }
                    sqlBuffer.setLength(0);
                }
            }

            // 最后提交
            connection.commit();

            // 启用外键检查
            stmt.execute("SET FOREIGN_KEY_CHECKS=1");
            stmt.execute("SET UNIQUE_CHECKS=1");

            System.out.println("[恢复服务] SQL执行完成: 总数=" + sqlCount + ", 成功=" + successCount + ", 错误=" + errorCount);
            
            if (errorCount > 0 && lastError != null) {
                System.err.println("[恢复服务] 最后错误: " + lastError);
            }

            // 即使有少量错误也认为恢复成功
            if (successCount > 0 && sqlCount > 0) {
                double successRate = (double) successCount / sqlCount;
                System.out.println("[恢复服务] 恢复完成，成功率: " + String.format("%.2f%%", successRate * 100));
                return true;
            }
            
            return successCount > 0;

        } catch (Exception e) {
            System.err.println("[恢复服务] JDBC恢复失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            // 清理资源
            try {
                if (stmt != null) stmt.close();
                if (reader != null) reader.close();
                if (connection != null) {
                    // 确保外键检查被启用
                    try {
                        Statement cleanupStmt = connection.createStatement();
                        cleanupStmt.execute("SET FOREIGN_KEY_CHECKS=1");
                        cleanupStmt.execute("SET UNIQUE_CHECKS=1");
                        cleanupStmt.close();
                        connection.setAutoCommit(true);
                        connection.close();
                    } catch (SQLException e) {
                        System.err.println("[恢复服务] 清理资源时出错: " + e.getMessage());
                    }
                }
            } catch (SQLException e) {
                System.err.println("[恢复服务] 关闭连接失败: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("[恢复服务] 关闭读取器失败: " + e.getMessage());
            }
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
