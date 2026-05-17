# 社区健康档案管理系统（Spring Boot + Vue3 + MySQL）

基于图片《社区健康档案管理系统需求说明书》生成的完整项目模板，包含：

- 后端：`Spring Boot 3 + Spring Security + JWT + JPA`
- 前端：`Vue3 + Vite + Pinia + Vue Router + Element Plus + ECharts`
- 数据库：`MySQL`（提供初始化 SQL 脚本）

## 1. 项目结构

```text
code
├─ backend                    # Spring Boot 后端
├─ frontend                   # Vue3 前端
└─ sql
   └─ schema.sql              # MySQL 建表脚本
```

## 2. 已实现模块

- 用户与权限管理：登录、注册、JWT 鉴权、角色权限（居民/医生/管理员）
- 居民档案管理：基本信息、建档信息
- 健康信息管理：既往病史、家族病史、过敏史、生活习惯
- 健康检测记录：血压/血糖/血脂/心率录入、趋势图展示、异常预警
- 就诊记录管理
- 用药记录管理
- 疫苗接种记录管理
- 随访计划与随访记录管理（提醒预警）
- 统计报表：居民分布、疾病谱、总览指标
- 系统日志：操作审计日志、备份恢复指引接口

## 3. 数据库初始化

在 MySQL 中执行：

```sql
source sql/schema.sql;
```

或手动导入 `sql/schema.sql`。

## 4. 后端启动

1. 修改配置文件 [application.yml](/d:/学习资料/数据库/数据库课程设计文件/code/backend/src/main/resources/application.yml) 中数据库账号密码。
2. 进入后端目录运行：

```bash
cd backend
mvn spring-boot:run
```

默认端口：`8080`

默认账号（首次启动自动创建）：

- 管理员：`admin / admin123`
- 医生：`doctor / doctor123`
- 居民：`resident / resident123`

## 5. 前端启动

```bash
cd frontend
npm install
npm run dev
```

默认端口：`5173`，已代理后端 `/api -> http://localhost:8080`。

## 6. 关键接口示例

- 认证：`/api/auth/login`、`/api/auth/register`
- 居民档案：`/api/residents`
- 健康信息：`/api/medical-histories`
- 体征检测：`/api/measurements`、`/api/measurements/trend/{residentId}`
- 随访：`/api/followups/plans`、`/api/followups/records`
- 预警：`/api/alerts`
- 统计：`/api/stats/overview`、`/api/stats/gender-distribution`、`/api/stats/disease-distribution`
- 系统：`/api/system/logs`、`/api/system/backup-guide`

## 7. 说明

- 本项目为课程设计可运行基础版，适合继续扩展 PDF 导出、消息推送、设备数据自动导入等高级功能。
- 当前环境编译校验时提示仅有 JRE（缺少 JDK 编译器），若需本地编译请安装 JDK 17+。

## 8. 注册登录邀请码说明

为了提高系统安全性，非居民角色在注册和登录时需要提供系统邀请码：

| 角色名称 | 角色代码 | 邀请码 |
| :--- | :--- | :--- |
| **系统管理员** | `ADMIN` | **`ADMIN888`** |
| **社区医生** | `DOCTOR` | **`DOC666`** |
| **公共卫生管理者** | `PUBLIC_HEALTH_MANAGER` | **`PHM777`** |
| **社区居民** | `RESIDENT` | *无需邀请码* |

*注：登录时如果选择非居民角色，也必须填写对应的邀请码。*
